package no.uio.ifi.dmms.cepoverlay;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import io.siddhi.core.util.EventPrinter;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.ActivityTuple;
import no.uio.ifi.dmms.cepoverlay.source.PAMAPSource;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries.*;

public class PAMAPTest {

    private static String fileName = "pamap/Optional/subject109.dat";

    @Test
    public void testFileReader() throws IOException, InterruptedException {
        BasicConfigurator.configure();
        SiddhiManager siddhiManager = new SiddhiManager();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(PAMAPQueries.siddhiApp);

        siddhiAppRuntime.addCallback("OutputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                System.out.println(events[0].getData(0)+" "+events[0].getData(2)+" "+events[0].getData(3)+" "+events[0].getData(4));

            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("ActivityStream");

        //Start SiddhiApp runtime
        siddhiAppRuntime.start();

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileName));

        String line = reader.readLine();
        double lastHeartRate = 90;
        int errors = 0;
        while (line != null) {
            line = reader.readLine();
            ActivityTuple tuple = null;
            try {
                tuple = parseActivity(line);
            } catch (NullPointerException e)
            {
                errors++;
            }
            if (tuple != null) {
                if (Double.isNaN(tuple.getHeartRate()))
                    tuple.setHeartRate(lastHeartRate);
                else
                    lastHeartRate = tuple.getHeartRate();
                double avgTemp = (tuple.getTemperatureAnkle() + tuple.getTemperatureChest() + tuple.getTemperatureHand()) / 3;
                //System.out.println(tuple.getTimestamp()+" "+tuple.getActivityId()+" "+tuple.getHeartRate()+" "+avgTemp);

                inputHandler.send(tuple.getEvent());
            }
        }

        //Shutdown SiddhiApp runtime
        siddhiAppRuntime.shutdown();

        //Shutdown Siddhi
        siddhiManager.shutdown();


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
        QuerySource hSource = new QuerySource("tempSource", 8);
        QuerySource aSource = new QuerySource("actSource", 9);

        List<QuerySink> joinSinkAct = asList(new QuerySink(10, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query joinQuery = new Query(1, activityJoinApp, asList(aSource, hSource), joinSinkAct);
            QueryControl.sendQuery(joinQuery, "localhost", 1080);


        List<QuerySink> aSink = asList(new QuerySink(9, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        Query hQuery = new Query(2, activityApp, asList(aSource), aSink);
        QueryControl.sendQuery(hQuery, "localhost", 1082);

        List<QuerySource> postJoinSource = asList(new QuerySource("PostJoinStream", 10));
        List<QuerySink> writerSink = asList(new QuerySink(11, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query postJoinQuery = new Query(3, activityPostJoinApp, postJoinSource, writerSink);
        QueryControl.sendQuery(postJoinQuery, "localhost", 1084);


        Thread.sleep(500);

        /* Source stream 1, the temperature readings  */
        new PAMAPSource("localhost", 2080, "localhost", 1081, 8, 5).start();

        new PAMAPSource("localhost", 2082, "localhost", 1083, 9, 5).start();


        /* Let the experiment run */
        Thread.sleep(1000000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        QueryControl.sendOverlayStop("localhost", 2082);


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
        QuerySource hSource = new QuerySource("tempSource", 8);
        QuerySource aSource = new QuerySource("actSource", 9);

        List<QuerySink> hSink = asList(new QuerySink(8, "localhost", 1083, QueryControl.SINK_TYPE_FORWARD));
        Query aQuery = new Query(1, heartApp, asList(hSource), hSink);
        QueryControl.sendQuery(aQuery, "localhost", 1080);

        List<QuerySink> joinSinkAct = asList(new QuerySink(10, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query joinQuery = new Query(2, activityJoinApp, asList(hSource, aSource), joinSinkAct);
        QueryControl.sendQuery(joinQuery, "localhost", 1082);


        List<QuerySource> postJoinSource = asList(new QuerySource("PostJoinStream", 10));
        List<QuerySink> writerSink = asList(new QuerySink(11, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query postJoinQuery = new Query(3, activityPostJoinApp, postJoinSource, writerSink);
        QueryControl.sendQuery(postJoinQuery, "localhost", 1084);


        Thread.sleep(500);

        new PAMAPSource("localhost", 2080, "localhost", 1081, 8, 5).start();
        new PAMAPSource("localhost", 2082, "localhost", 1083, 9, 5).start();


        /* Let the experiment run */
        Thread.sleep(700000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        QueryControl.sendOverlayStop("localhost", 2082);


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
        QuerySource hSource = new QuerySource("tempSource", 8);
        QuerySource aSource = new QuerySource("actSource", 9);

        List<QuerySink> hSink = asList(new QuerySink(10, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query aQuery = new Query(1, heartApp, asList(hSource), hSink);
        QueryControl.sendQuery(aQuery, "localhost", 1080);

        List<QuerySink> aSink = asList(new QuerySink(11, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query hQuery = new Query(2, activityApp, asList(aSource), aSink);
        QueryControl.sendQuery(hQuery, "localhost", 1082);


        List<QuerySink> joinSinkTemp = asList(new QuerySink(12, "localhost", 1085, QueryControl.SINK_TYPE_WRITE));

        QuerySource hPJSource = new QuerySource("tempSource", 10);
        QuerySource aPJSource = new QuerySource("actSource", 11);
        Query joinQuery = new Query(3, activityJoinApp, asList(hPJSource, aPJSource), joinSinkTemp);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);

        Thread.sleep(500);

        /* Source stream 1, the temperature readings in each city */
        new PAMAPSource("localhost", 2080, "localhost", 1081, 8, 5).start();
        new PAMAPSource("localhost", 2082, "localhost", 1083, 9, 5).start();


        /* Let the experiment run */
        Thread.sleep(700000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        QueryControl.sendOverlayStop("localhost", 2082);


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


    private ActivityTuple parseActivity(String line) {
        String[] lineArr = line.split(" ");
        return new ActivityTuple(
                (long)(Double.parseDouble(lineArr[0])*1000),
                Integer.parseInt(lineArr[1]),
                Double.parseDouble(lineArr[2]),
                Double.parseDouble(lineArr[3]),
                Double.parseDouble(lineArr[20]),
                Double.parseDouble(lineArr[37]));
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
}
