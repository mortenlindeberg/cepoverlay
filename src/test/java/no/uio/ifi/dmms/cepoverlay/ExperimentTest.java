package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.FireSource;
import no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;


public class ExperimentTest {

    private String joinApp = "define stream LeftStream(timestamp long, roomNo int, temp double); "+
            "define stream RightStream(timestamp long, roomNo int, isOn bool); " +
            "@info(name = 'queryengine')" +
            "from " +
            "LeftStream[temp > 20.0]#window.length(1) as T unidirectional join " +
            "RightStream[isOn == false]#window.length(1) as R " +
            "on T.roomNo == R.roomNo "+
            "select T.timestamp as ttime, R.timestamp as rtime, T.temp as temp insert into OutputStream;";

    private String postJoinApp = "define stream PostJoinStream(leftTimestamp long, rightTimestamp long, temperature int);" +
            "from PostJoinStream select * " +
            "insert into OutputStream;";

    private String predictApp = "define stream PredictStream(timestamp long, room int, temperature int); "+
            "from PredictStream select * insert into OutputStream";

    SimpleSourceThread src0;
    SimpleSourceThread src1;
    SimpleSourceThread src2;


    @Test
    public void testExperimentOne() throws InterruptedException {
        BasicConfigurator.configure();
        File file = new File("results.txt");
        file.delete();
        file = new File("fasit.txt");
        file.delete();

        file = new File("adapt.txt");
        file.delete();
        Runners.writeStartTime(Main.START_FILENAME);

        Instance a = new Instance("A","localhost",1078); // We create a master that is supposed to run close to the source 1.
        Instance p1 = new Instance("E1","localhost",1080, 1, "1Mbps", 100);
        Instance p2 = new Instance("E2","localhost",1082, 1, "1Mbps", 1);
        Instance i1 = new Instance("I","localhost",1084);
        Instance s = new Instance("D","localhost",1086);


        /* Start the nodes */
        OverlayInstanceThread node0 = new OverlayInstanceThread (a);
        node0.start();
        OverlayInstanceThread  node1 = new OverlayInstanceThread (p1);
        node1.start();
        OverlayInstanceThread  node2 = new OverlayInstanceThread (p2);
        node2.start();
        OverlayInstanceThread  node3 = new OverlayInstanceThread (i1);
        node3.start();
        OverlayInstanceThread  node4 = new OverlayInstanceThread (s);
        node4.start();

        Thread.sleep(15000);

        List<QuerySource> writerSource = asList(new QuerySource("PostJoinStream", 3));
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));

        List<QuerySink> joinSink = asList(new QuerySink(3, "localhost", 1087, QueryControl.SINK_TYPE_FORWARD));
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 2);

        /* Execute the adaptation queries on the two "source nodes" */
        Query predictionQuery = new Query(0, predictApp, asList(new QuerySource("PredictStream", 1)), asList(new QuerySink(1, null, -1, QueryControl.SINK_TYPE_PRED_ADAPT)));
        QueryControl.sendQuery(predictionQuery, "localhost", 1078);

        List<QuerySource> joinSource = asList(new QuerySource[]{leftSource, rightSource});

        Query writerQuery = new Query(2, postJoinApp, writerSource, writerSink);
        QueryControl.sendQuery(writerQuery, "localhost", 1086);

        /* Start the query engine on I */
        Query joinQuery = new Query(1, joinApp, joinSource, joinSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1082);
        Thread.sleep(500);

        /* Start the source threads */
        src0 = new FireSource("localhost", 2079, "localhost",1079, 1, 10, 0, 0);
        src1 = new FireSource("localhost", 2080, "localhost",1083, 1, 10, 0, 0);
        src2 = new FireSource("localhost", 2081, "localhost",1083, 2, 1000, 0, 0);

        src0.start(); // This will only be used for the adaptation, have to do better!!
        src1.start();
        src2.start();

        /* Let the experiment run */
        Thread.sleep(120000);


        // Stop the source threads
        QueryControl.sendOverlayStop("localhost", 2079);
        QueryControl.sendOverlayStop("localhost", 2080);
        QueryControl.sendOverlayStop("localhost", 2081);
        Thread.sleep(1000);

        QueryControl.sendQueryStop(new QueryStop(0), "localhost", 1078);
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1086);

        Thread.sleep(2000);
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        QueryControl.sendOverlayStop("localhost", 1086);
        Thread.sleep(2000);
    }
}
