package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.network.topology.NetworkGraph;
import no.uio.ifi.dmms.cepoverlay.network.topology.PlacementModule;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.PAMAPSource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries.*;

public class PAMAPPlacementTest {
    private static final Logger log = Logger.getLogger(PAMAPPlacementTest.class.getName());
    private static final int LOG_OFFSET = 41656; // The timestamps does not correspond with the logger timestamps, and offset is around this value for C1 nodes.. I.e., 42 seconds



    @Test
    public void testOptimalPlacement() throws IOException {
        FileWriter fileWriter = new FileWriter("teoretic-optimum.txt");
        List<PamapSourceTest> sourcesJ1 = new ArrayList<>();
        List<PamapSourceTest> sourcesJ2 = new ArrayList<>();

        // Create all the sources
        for (int i : Arrays.asList(10,12,14,16,18,20,22,24)) {
            PamapSourceTest t = new PamapSourceTest(i);

            if (i < 18) {
                sourcesJ1.add(t);
            } else {
                sourcesJ2.add(t);
            }
        }

        int timestamp = 0;
        boolean keepRunning = true;
        String lastPlacement = "C1";
        while (keepRunning) {
            try {
                int j1Sum = 0, j2Sum = 0;
                for (PamapSourceTest s : sourcesJ1) {
                    Object[] t = s.getNextTuple(s.streamId(), timestamp);

                    if (t == null) continue;

                    double hr = (double) t[1];
                    if (hr < Main.PAMAP_LIMIT_HR)
                        j1Sum++;
                }
                for (PamapSourceTest s : sourcesJ2) {
                    Object[] t = s.getNextTuple(s.streamId(), timestamp);
                    if (t == null) continue;

                    double hr = (double) t[1];
                    if (hr < Main.PAMAP_LIMIT_HR)
                        j2Sum++;
                }

                String placement = "NA";
                if (j1Sum > j2Sum)
                    placement = "J1";
                else if (j1Sum < j2Sum)
                    placement = "J2";
                if (placement != lastPlacement) {
                    fileWriter.write(LOG_OFFSET + (timestamp/10) + " " + placement + " " + j1Sum + " " + j2Sum + "\n");
                    lastPlacement = placement;
                }

                timestamp = timestamp + 100;
            } catch (RuntimeException e) {
               keepRunning = false;
            }
        }
        fileWriter.close();
    }

    @Test
    public void testPlacement() {
        List<Instance> sources = new ArrayList();
        List<Instance> nodes = new ArrayList();

        for (int i = 1; i <= 8; i++) {
            sources.add(new Instance("P" + i, "10.0.0." + i, 1080, 1, "1Mbps", 100));
        }

        nodes.addAll(sources);

        Instance j1 = new Instance("J1", "10.0.0.9", 1080, 1, "1Mbps", 100);
        Instance j2 = new Instance("J2", "10.0.0.10", 1080, 1, "1Mbps", 100);

        nodes.add(j1);
        nodes.add(j2);

        Instance d = new Instance("D", "10.0.0.11", 1080, 1, "1Mbps", 100);
        nodes.add(d);

        NetworkGraph graph = new NetworkGraph(nodes);

        /* Create the links */
        for (int i = 0; i < 4; i++) {
            graph.addEdge(sources.get(i), j1);
        }

        for (int i = 4; i < 8; i++) {
            graph.addEdge(sources.get(i), j2);
        }

        graph.addEdge(j1, d);
        graph.addEdge(j2, d);
        graph.addEdge(j1, j2);


        List<Instance> allButJs = new ArrayList<>();
        allButJs.addAll(sources);
        allButJs.add(d);

        long dur = 0;
        // For each rate chart
        for (HashMap<Instance, Boolean> rateChart : generateRateCharts(sources)) {
            printRateChart(rateChart, sources);
            long start = System.currentTimeMillis();
            String placementString = getBestPlacements(rateChart, graph, sources, nodes, d);
            long stop = System.currentTimeMillis();
            dur += (stop - start);
            System.out.println(placementString + ". ");
        }
        System.out.println("Average dur: " + dur);

    }

    private String getBestPlacements(HashMap<Instance, Boolean> rateChart, NetworkGraph graph, List<Instance> sources, List<Instance> nodes, Instance destination) {
        int shortest = Integer.MAX_VALUE;
        List<Instance> candidates = new ArrayList<>();

        for (Instance i : nodes) {
            int rate = getRate(rateChart, graph, sources, i, destination);
            if (rate < shortest) {
                candidates = new ArrayList<>();
                candidates.add(i);
                shortest = rate;
            } else if (rate == shortest)
                candidates.add(i);
        }

        String candidateString = "";

        for (Instance candidate : candidates)
            candidateString += " " + candidate.getInstanceName();

        return " " + shortest + " : " + candidateString;
    }

    private int getRate(HashMap<Instance, Boolean> rateChart, NetworkGraph graph, List<Instance> sources, Instance placement, Instance destination) {
        int linkCount = 0;
        /* For each with value = false, calculate hop count to placement node */
        /* Calculate number of hops to the placement node */
        boolean active = false;
        for (Instance from : sources)
            if (rateChart.get(from)) {
                active = true;
                int numLink = graph.getShortestRoute(from, placement).size();
                linkCount += numLink;
            }

        /* calculate remaining hops to destination */
        if (active) {
            linkCount += graph.getShortestRoute(placement, destination).size();

            return linkCount;
        }
        return Integer.MAX_VALUE;
    }


    public List<HashMap<Instance, Boolean>> generateRateCharts(List<Instance> sources) {
        List<HashMap<Instance, Boolean>> rateCharts = new ArrayList<>(256);

        HashMap<Instance, Boolean> rateChart = generateFalseHashmap(sources);
        // For each instance of a rateChart
        for (int i = 0; i < 256; i++) {
            rateCharts.add(generateFalseHashmap(sources));
            String s = Integer.toBinaryString(i);
            //System.out.print("\nInt " + i + "(+" + s + "): ");
            for (int j = 0; j < s.length(); j++) {
                if (s.charAt(j) == '1') {
                    int index = 8 - s.length() + j;
                    rateCharts.get(i).put(sources.get(index), true);
                }
            }
        }
        return rateCharts;
    }

    private void printRateChart(HashMap<Instance, Boolean> rateChart, List<Instance> sources) {
        for (Instance i : sources) {
            boolean b = rateChart.get(i);
            int v = 0;
            if (b) v = 1;
            System.out.print(v);
            //System.out.print(i.getInstanceName()+" " + v + " ");

        }
    }

    public HashMap<Instance, Boolean> generateFalseHashmap(List<Instance> sources) {
        HashMap<Instance, Boolean> out = new HashMap<>();
        for (Instance source : sources)
            out.put(source, false);

        return out;
    }

    @Test
    public void testPlacementModule() {
        ConcurrentHashMap rateMap = new ConcurrentHashMap<>();
        rateMap.put("10.0.0.3", false); // 1
        rateMap.put("10.0.0.6", true); // 2
        rateMap.put("10.0.0.9", false); // 3
        rateMap.put("10.0.0.12", false); //4
        rateMap.put("10.0.0.15", true); // 5
        rateMap.put("10.0.0.18", true); // 6
        rateMap.put("10.0.0.21", false); // 7
        rateMap.put("10.0.0.24", true); // 8

        PlacementModule placementModule = new PlacementModule();
        System.out.println(placementModule.findOptimalPlacements(rateMap));

    }

    @Test
    public void queryTest() throws InterruptedException {
        BasicConfigurator.configure();
        Runners.cleanOutputFiles(Arrays.asList(Main.START_FILENAME));
        Runners.writeStartTime(Main.START_FILENAME);

        log.debug("Working Directory = " + System.getProperty("user.dir"));

        int port = 1080;
        int streamId = 10;

        // Start the overlay nodes
        List<OverlayInstanceThread> overlayThreads = new ArrayList<>();
        List<String> nodeNames = Arrays.asList("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "C1");
        for (String name : nodeNames) {
            OverlayInstanceThread t = new OverlayInstanceThread(new Instance(name, "localhost", port++));
            port++;
            overlayThreads.add(t);
            t.start();
        }
        log.debug("> Ordinary nodes created..");

        List<QuerySource> p1Sources = Arrays.asList(new QuerySource("tempSource", 10), new QuerySource("actSource", 11));
        List<QuerySource> p2Sources = Arrays.asList(new QuerySource("tempSource", 12), new QuerySource("actSource", 13));
        List<QuerySource> p3Sources = Arrays.asList(new QuerySource("tempSource", 14), new QuerySource("actSource", 15));
        List<QuerySource> p4Sources = Arrays.asList(new QuerySource("tempSource", 16), new QuerySource("actSource", 17));
        List<QuerySource> p5Sources = Arrays.asList(new QuerySource("tempSource", 18), new QuerySource("actSource", 19));
        List<QuerySource> p6Sources = Arrays.asList(new QuerySource("tempSource", 20), new QuerySource("actSource", 21));
        List<QuerySource> p7Sources = Arrays.asList(new QuerySource("tempSource", 22), new QuerySource("actSource", 23));
        List<QuerySource> p8Sources = Arrays.asList(new QuerySource("tempSource", 23), new QuerySource("actSource", 25));

        List<QuerySource> joinSources = Arrays.asList(
                new QuerySource("OutputStreamP1", 25),
                new QuerySource("OutputStreamP2", 26),
                new QuerySource("OutputStreamP3", 27),
                new QuerySource("OutputStreamP4", 28),
                new QuerySource("OutputStreamP5", 29),
                new QuerySource("OutputStreamP6", 30),
                new QuerySource("OutputStreamP7", 31),
                new QuerySource("OutputStreamP8", 32));

        QuerySource lastSource = new QuerySource("InputStream", 33);


        /* Create the sinks */
        List<QuerySink> forwardSinkP1 = asList(new QuerySink(25, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP2 = asList(new QuerySink(26, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP3 = asList(new QuerySink(27, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP4 = asList(new QuerySink(28, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP5 = asList(new QuerySink(29, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP6 = asList(new QuerySink(30, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP7 = asList(new QuerySink(31, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP8 = asList(new QuerySink(32, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));

        List<QuerySink> forwardSink = asList(new QuerySink(33, "localhost", 1097, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> writerSink = asList(new QuerySink(34, null, -1, QueryControl.SINK_TYPE_WRITE));

        /* The queries.. */
        Query p1Query = new Query(1, largePAMAPTempFilter, p1Sources, forwardSinkP1);
        Query p2Query = new Query(2, largePAMAPTempFilter, p2Sources, forwardSinkP2);
        Query p3Query = new Query(3, largePAMAPTempFilter, p3Sources, forwardSinkP3);
        Query p4Query = new Query(4, largePAMAPTempFilter, p4Sources, forwardSinkP4);
        Query p5Query = new Query(5, largePAMAPTempFilter, p5Sources, forwardSinkP5);
        Query p6Query = new Query(6, largePAMAPTempFilter, p6Sources, forwardSinkP6);
        Query p7Query = new Query(7, largePAMAPTempFilter, p7Sources, forwardSinkP7);
        Query p8Query = new Query(8, largePAMAPTempFilter, p8Sources, forwardSinkP8);


        log.debug("> Queries have been set up and ready for sending.. waiting");
        Thread.sleep(20000);

        QueryControl.sendQuery(p1Query, "localhost", 1080);
        QueryControl.sendQuery(p2Query, "localhost", 1082);
        QueryControl.sendQuery(p3Query, "localhost", 1084);
        QueryControl.sendQuery(p4Query, "localhost", 1086);
        QueryControl.sendQuery(p5Query, "localhost", 1088);
        QueryControl.sendQuery(p6Query, "localhost", 1090);
        QueryControl.sendQuery(p7Query, "localhost", 1092);
        QueryControl.sendQuery(p8Query, "localhost", 1094);

        Query joinQuery = new Query(9, extraLargePAMAPTempJoinPostPerson, joinSources, forwardSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1096);

        Query lastQuery = new Query(10, postJoinPAMAPTempQuery, Arrays.asList(lastSource), writerSink);
        QueryControl.sendQuery(lastQuery, "localhost", 1096);


        log.debug("> Queries sent.. starting threads..");

        Thread.sleep(6000);

        port = 1080;
        streamId = 10;
        // Start the threads
        for (int i = 1; i <= 8; i++) {
            new PAMAPSource("localhost", port + 1000, "localhost", ++port, streamId++, 2).start();
            new PAMAPSource("localhost", port + 2000, "localhost", port, streamId++, 2).start();
            port++;
        }

        Thread.sleep(200000);

    }
}
