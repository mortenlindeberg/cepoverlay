package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryMigrate;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstance;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.FireSource;
import no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class TestTriangleAdapt {


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

    SimpleSourceThread src1;
    SimpleSourceThread src2;
    SimpleSourceThread src3;


    @Test
    public void triangleAdapt() throws InterruptedException {
        BasicConfigurator.configure();
        Runners.cleanOutputFiles(Arrays.asList("output.res", "start.res", "original.res"));
        Runners.writeStartTime(Main.START_FILENAME);

        Instance p1 = new Instance("P1","localhost",1080, 1, "1Mbps", 100);
        Instance p2 = new Instance("P2","localhost",1082, 1, "1Mbps", 1);
        Instance i1 = new Instance("I1","localhost",1084);
        Instance s = new Instance("S","localhost",1086);

        /* Start the nodes */
        OverlayNodeThread node1 = new OverlayNodeThread(p1);
        node1.start();
        OverlayNodeThread node2 = new OverlayNodeThread(p2);
        node2.start();
        OverlayNodeThread node3 = new OverlayNodeThread(i1);
        node3.start();
        OverlayNodeThread node4 = new OverlayNodeThread(s);
        node4.start();

        Thread.sleep(500);

        List<QuerySource> writerSource = asList(new QuerySource("PostJoinStream", 3));
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));

        List<QuerySink> joinSink = asList(new QuerySink(3, "localhost", 1087, QueryControl.SINK_TYPE_FORWARD));
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 2);

        /* Execute the adaptation queries on the two "source nodes" */
        Query predictionQuery = new Query(0, predictApp, asList(new QuerySource("PredictStream", 1)), asList(new QuerySink(1, null, -1, QueryControl.SINK_TYPE_PRED_ADAPT)));
        QueryControl.sendQuery(predictionQuery, "localhost", 1086);

        List<QuerySource> joinSource = asList(new QuerySource[]{leftSource, rightSource});

        Query writerQuery = new Query(2, postJoinApp, writerSource, writerSink);
        QueryControl.sendQuery(writerQuery, "localhost", 1086);

        /* Start the query engine on I */
        Query joinQuery = new Query(1, joinApp, joinSource, joinSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);
        Thread.sleep(500);

        /* Start the source threads */
        src1 = new FireSource("localhost", 2080, "localhost", 1085, 1, 20, 0, 0);
        src2 = new FireSource("localhost", 2081, "localhost", 1085, 2, 1000, 0, 0);
        src3 = new FireSource("localhost", 2082, "localhost", 1087, 1, 20, 0, 0);
        src1.start();
        src2.start();
        src3.start();

        Thread.sleep(10000);
        migrateTo(1, i1, p1, joinSource, joinSink);
        Thread.sleep(10000);
        migrateTo(1, p1, p2, joinSource, joinSink);
        Thread.sleep(10000);
        migrateTo(1, p2, p1, joinSource, joinSink);
        Thread.sleep(10000);
        migrateTo(1, p1, p2, joinSource, joinSink);
        Thread.sleep(40000);
        migrateTo(1, p2, p1, joinSource, joinSink);
        Thread.sleep(10000);
        migrateTo(1, p1, p2, joinSource, joinSink);
        Thread.sleep(10000);

        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1086);

        Thread.sleep(4000);
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        QueryControl.sendOverlayStop("localhost", 1086);
        Thread.sleep(4000);
    }

    public void migrateTo(int queryId, Instance from, Instance to, List<QuerySource> sources, List<QuerySink> sinks) throws InterruptedException {
        /* 1. Redirect sources to P1 */
        src1.redirect(to.getAddress(), to.getControlPort()+1);
        src2.redirect(to.getAddress(), to.getControlPort()+1);

        /* 2. Migrate the query engine  */
        QueryControl.sendQueryMigrate(new QueryMigrate(queryId, to.getAddress(), to.getControlPort(), sources, sinks, "localhost", 1080),  from.getAddress(), from.getControlPort());
    }

    private class OverlayNodeThread extends Thread {

        private Instance instance;

        public OverlayNodeThread(Instance instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            new OverlayInstance(instance);
        }
    }
}
