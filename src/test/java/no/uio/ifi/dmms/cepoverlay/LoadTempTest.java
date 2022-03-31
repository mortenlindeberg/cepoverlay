package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.TempLoadSource;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;


public class LoadTempTest {



    private static String tempQueryApp =
            "define stream temperatureStream(timestamp long, city string, temperature double, humidity double); " +
                    "from temperatureStream[temperature > -20] select * " +
                    "insert into OutputStream; ";

    private static String loadQueryApp =
            "define stream loadStream(timestamp long, trend double, load double); " +
                    "from loadStream[load > 60000] select * " +
                    "insert into OutputStream; ";

    private static String joinQueryApp =
                    "define stream temperatureStream(timestamp long, city string, temperature double, humidity double); " +
                    "define stream loadStream(timestamp long, trend double, load double); " +
                    "from loadStream[load > 60000]#window.externalTimeBatch(timestamp, 1 hour) as l join " +
                    "temperatureStream[temperature > -20]#window.externalTimeBatch(timestamp, 1 hour) as t on l.timestamp == t.timestamp \n" +
                    "select max(l.timestamp) as timestamp, max(t.timestamp) as timestamp2, l.load as load, t.temperature as temperature, t.humidity as humidity "+
                    "insert into OutputStream; ";

    private static String postJoinApp =
                    "define stream postJoinStream(tempTimestamp long, loadTimestamp long, load double, temperature double, humidity double); " +
                    "from postJoinStream select * insert into OutputStream; ";

    @Test
    public void testMultipleCities() throws InterruptedException {
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);

        OverlayInstanceThread node = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node.start();
        Thread.sleep(1000);

        QuerySource tempSource = new QuerySource("temperatureStream", 0);
        List<QuerySink> writerSink = asList(new QuerySink(1, null, -1, QueryControl.SINK_TYPE_WRITE));

        Query tempQuery = new Query(1, tempQueryApp, asList(tempSource), writerSink);
        QueryControl.sendQuery(tempQuery, "localhost", 1080);

        // Start the multiple threads..
        int numThreads = TempLoadSource.files.size();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            taskExecutor.execute(new TempLoadSource("localhost", 2080 + i, "localhost", 1081, 0, 50, i));

        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(1, TimeUnit.HOURS);
    }

    @Test
    public void SimpleQuery() throws InterruptedException {
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);

        OverlayInstanceThread node = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node.start();
        Thread.sleep(1000);

        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        QuerySource loadSource = new QuerySource("loadStream", 2);

        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query tempQuery = new Query(1, joinQueryApp, asList(tempSource, loadSource), writerSink);
        QueryControl.sendQuery(tempQuery, "localhost", 1080);

        Thread.sleep(1000);

        /* Source stream 1, the temperature readings in each city */
        for (int i = 1; i <= 8; i++) {
            new TempLoadSource("localhost", 2080 + i, "localhost", 1081, 1, 5, i).start();
        }
        /* Source stream 2, the load file */
        new TempLoadSource("localhost", 2080, "localhost",1081,2,5, 0).start();

        /* Let the experiment run */
        Thread.sleep(300000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (int i = 1; i <= 8; i++)
            QueryControl.sendOverlayStop("localhost", 2080 + i);
    }

    @Test
    public void AJQuery() throws InterruptedException {
        storeToPcap("aj.pcap");
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);
        Runners.cleanOutputFiles(Arrays.asList("output.res"));

        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("B","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("C","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Create the queries */
        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        QuerySource loadSource = new QuerySource("loadStream", 2);
        List<QuerySink> joinSinkTemp = asList(new QuerySink(3, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query tempQuery = new Query(1, joinQueryApp, asList(tempSource, loadSource), joinSinkTemp);
        QueryControl.sendQuery(tempQuery, "localhost", 1080);


        List<QuerySink> aLoad = asList(new QuerySink(2, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        Query loadQuery = new Query(2, loadQueryApp, asList(loadSource), aLoad);
        QueryControl.sendQuery(loadQuery, "localhost", 1082);

        List<QuerySource> postJoinSource = asList(new QuerySource("postJoinStream", 3));
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query joinQuery = new Query(3, postJoinApp, postJoinSource, writerSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);


        Thread.sleep(500);

        /* Source stream 1, the temperature readings in each city */
         for (int i = 1; i <= 8; i++) {
            new TempLoadSource("localhost", 2080 + i, "localhost", 1081, 1, 5, i).start();
        }
         /* Source stream 2, the load file */
        new TempLoadSource("localhost", 2080, "localhost",1083,2,5, 0).start();

        /* Let the experiment run */
        Thread.sleep(300000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (int i = 1; i <= 8; i++) {
            QueryControl.sendOverlayStop("localhost", 2080 + i);

        }

        Thread.sleep(1000);

        /* Stop the queries */
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1084);
        Thread.sleep(1000);

        /* Stop the overlay nodes */
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        Thread.sleep(2000);
        killPcap();
    }

    private static void storeToPcap(String s) {
        try {
            Runtime.getRuntime().exec("sudo tcpdump -i lo0 -w "+s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void killPcap() {
        try {
            Runtime.getRuntime().exec("sudo killall tcpdump");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void BJQuery() throws InterruptedException {
        storeToPcap("bj.pcap");
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);
        Runners.cleanOutputFiles(Arrays.asList("output.res"));

        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("B","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("C","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Create the queries */
        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        List<QuerySink> joinSinkTemp = asList(new QuerySink(1, "localhost", 1083, QueryControl.SINK_TYPE_FORWARD));
        Query tempQuery = new Query(1, tempQueryApp, asList(tempSource), joinSinkTemp);
        QueryControl.sendQuery(tempQuery, "localhost", 1080);

        QuerySource loadSource = new QuerySource("loadStream", 2);
        List<QuerySource> joinSources = asList(new QuerySource[]{tempSource, loadSource});
        List<QuerySink> joinSinkLoad = asList(new QuerySink(3, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query loadQuery = new Query(2, joinQueryApp, joinSources, joinSinkLoad);
        QueryControl.sendQuery(loadQuery, "localhost", 1082);

        List<QuerySource> postJoinSource = asList(new QuerySource("postJoinStream", 3));
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query joinQuery = new Query(3, postJoinApp, postJoinSource, writerSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);


        Thread.sleep(500);

        /* Source stream 1, the temperature readings in each city */
        for (int i = 1; i <= 8; i++) {
            new TempLoadSource("localhost", 2080 + i, "localhost", 1081, 1, 5, i).start();
        }
        /* Source stream 2, the load file */
        new TempLoadSource("localhost", 2080, "localhost",1083,2,5, 0).start();

        /* Let the experiment run */
        Thread.sleep(300000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (int i = 1; i <= 8; i++) {
            QueryControl.sendOverlayStop("localhost", 2080 + i);

        }

        Thread.sleep(1000);

        /* Stop the queries */
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1084);
        Thread.sleep(1000);

        /* Stop the overlay nodes */
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        Thread.sleep(2000);
        killPcap();
    }



    @Test
    public void CJQuery() throws InterruptedException {
        storeToPcap("cj.pcap");
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);
        Runners.cleanOutputFiles(Arrays.asList("output.res"));

        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("B","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("C","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Create the queries */
        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        List<QuerySink> joinSinkTemp = asList(new QuerySink(1, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query tempQuery = new Query(1, tempQueryApp, asList(tempSource), joinSinkTemp);
        QueryControl.sendQuery(tempQuery, "localhost", 1080);

        QuerySource loadSource = new QuerySource("loadStream", 2);
        List<QuerySink> joinSinkLoad = asList(new QuerySink(2, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query loadQuery = new Query(2, loadQueryApp, asList(loadSource), joinSinkLoad);
        QueryControl.sendQuery(loadQuery, "localhost", 1082);

        List<QuerySource> joinSources = asList(new QuerySource[]{tempSource, loadSource});
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query joinQuery = new Query(3, joinQueryApp, joinSources, writerSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);


        Thread.sleep(500);

        /* Source stream 1, the temperature readings in each city */
        for (int i = 1; i <= 8; i++) {
            new TempLoadSource("localhost", 2080 + i, "localhost", 1081, 1, 5, i).start();
        }
        /* Source stream 2, the load file */
        new TempLoadSource("localhost", 2080, "localhost",1083,2,5, 0).start();

        /* Let the experiment run */
        Thread.sleep(300000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (int i = 1; i <= 8; i++) {
            QueryControl.sendOverlayStop("localhost", 2080 + i);

        }

        Thread.sleep(1000);

        /* Stop the queries */
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1084);
        Thread.sleep(1000);

        /* Stop the overlay nodes */
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        Thread.sleep(2000);
        killPcap();
    }

    /* PrettyPrint dates code from stack overflow */
    public Map<TimeUnit,Long> computeDiff(Date date1, Date date2) {

        long diffInMillies = date2.getTime() - date1.getTime();

        //create the list
        List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);

        //create the result map of TimeUnit and difference
        Map<TimeUnit,Long> result = new LinkedHashMap<TimeUnit,Long>();
        long milliesRest = diffInMillies;

        for ( TimeUnit unit : units ) {

            //calculate difference in millisecond
            long diff = unit.convert(milliesRest,TimeUnit.MILLISECONDS);
            long diffInMilliesForUnit = unit.toMillis(diff);
            milliesRest = milliesRest - diffInMilliesForUnit;

            //put the result in the map
            result.put(unit,diff);
        }

        return result;
    }
}
