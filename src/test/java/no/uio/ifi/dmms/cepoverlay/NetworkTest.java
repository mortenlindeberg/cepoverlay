package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryMigrate;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryRedirect;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.FireSource;
import no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread;
import no.uio.ifi.dmms.cepoverlay.source.SourceFilter;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class NetworkTest {
    private String siddhiApp = "define stream MortenStream(timestamp long, roomNo int, temp double); " +
            "" +
            "@info(name = 'queryengine') " +
            "from MortenStream#window.lengthBatch(800) " +
            "select max(timestamp) as timestamp, avg(temp) as temp " +
            "insert into OutputStream;";

    private String preJoinAppLeft = "define stream LeftStream (timestamp long, roomNo int, temp double); " +
            "@info(name = 'queryengine') from LeftStream[temp < 40] select * insert into OutputStream; ";

    private String preJoinAppRight = "define stream RightStream (timestamp long, roomNo int, isOn bool);" +
            "@info(name = 'queryengine') from RightStream[isOn == false] select * insert into OutputStream; ";

    private String joinApp = "define stream LeftStream(timestamp long, roomNo int, temp double); " +
            "define stream RightStream(timestamp long, roomNo int, isOn bool); " +
            "@info(name = 'queryengine')" +
            "from LeftStream[temp < 40.0]#window.length(1) as T join RightStream[isOn == false]#window.length(1) as R " +
            "on T.roomNo == R.roomNo select T.timestamp as ttime, R.timestamp as rtime, T.temp as temp insert into OutputStream;";


    private String postJoinApp = "define stream PostJoinStream(leftTimestamp long, rightTimestamp long, temperature int);" +
            "from PostJoinStream select * " +
            "insert into OutputStream;";


    @Test
    public void leftForwardAdapt_v2() throws InterruptedException {
        BasicConfigurator.configure();
        File file = new File("results.txt");
        file.delete();

        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("A","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Start the queries on P1 (join) and P2 forward */
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 3);
        Query joinQuery = new Query(1, joinApp, Arrays.asList(new QuerySource[]{leftSource, rightSource}),
                Arrays.asList(new QuerySink[]{new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD)}));
        QueryControl.sendQuery(joinQuery, "localhost", 1080);

        Query rightQuery = new Query(2, preJoinAppRight, new QuerySource("RightStream", 2),
                new QuerySink(3, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(rightQuery, "localhost", 1082);


        Query forward = new Query(3, postJoinApp, Arrays.asList(new QuerySource[]{new QuerySource("PostJoinStream", 4)}), new ArrayList<>(0));
        QueryControl.sendQuery(forward, "localhost", 1084);

        /* Start the source threads */
        SimpleSourceThread src1 = new FireSource("localhost", 2080, "localhost", 1081, 1, 10, 0, 0);
        SimpleSourceThread src2 = new FireSource("localhost", 2081, "localhost", 1083, 2, 1000, 0, 0);
        src1.start();
        src2.start();

        Thread.sleep(10000);

        /* Move Join from P1 to D1 */

        /* 1. Start a new forwarding queryengine on P1 */
        Query leftQuery = new Query(6, preJoinAppLeft, new QuerySource("LeftStream", 1), new QuerySink(5, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(leftQuery, "localhost", 1080);

        /* 2. Redirect P2 to D1 */
        QueryControl.sendQueryRedirect(new QueryRedirect(2, Arrays.asList(new QuerySink(3, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD))),
                "localhost", 1082);

        /* 2. Migrate the queryengine from P1 to D1 */
        leftSource = new QuerySource("LeftStream", 5);
        rightSource = new QuerySource("RightStream", 3);
        QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1084,Arrays.asList(leftSource, rightSource), new ArrayList<>(), "localhost", 1080),
                "localhost", 1080);

        /* 2. Stop the old queryengine on D1 */
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1084);


        Thread.sleep(10000);
    }

    @Test
    public void startQueryWithJoinOnLeft() throws InterruptedException {
        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("A","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Start the queries on P1 (join) and P2 forward */
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 3);
        Query joinQuery = new Query(1, joinApp, Arrays.asList(new QuerySource[]{leftSource, rightSource}),
                Arrays.asList(new QuerySink[]{new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD)}));
        QueryControl.sendQuery(joinQuery, "localhost", 1080);

        Query rightQuery = new Query(2, preJoinAppRight, new QuerySource("RightStream", 2),
                new QuerySink(3, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(rightQuery, "localhost", 1082);


        Query forward = new Query(3, postJoinApp, Arrays.asList(new QuerySource[]{new QuerySource("PostJoinStream", 4)}),
                new ArrayList<>(0));
        QueryControl.sendQuery(forward, "localhost", 1084);

        /* Start the source threads */
        SimpleSourceThread src1 = new FireSource("localhost", 2080, "localhost", 1081, 1, 10, 0, 0);
        SimpleSourceThread src2 = new FireSource("localhost", 2080, "localhost", 1083, 2, 1000, 0, 0);
        src1.start();
        src2.start();

    }

    @Test
    public void testLeftRightAdapt() throws InterruptedException {
        BasicConfigurator.configure();
        File file = new File("results.txt");
        file.delete();

        startQueryWithJoinOnLeft();
        Thread.sleep(10000);

        /* Move join from P1 to P2 */

        /* 1. Stop the old queryengine on P2 */
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1082);

        /* 2. Migrate the queryengine from P1 to P2 */
        QuerySource leftSource = new QuerySource("LeftStream", 5);
        QuerySource rightSource = new QuerySource("RightStream", 2);
        QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1082,
                        Arrays.asList(leftSource, rightSource), Arrays.asList(new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD)), "localhost", 1080),
                "localhost", 1080);

        /* 3. Start a new forwarding queryengine on P1 */
        Query leftQuery = new Query(5, preJoinAppLeft, new QuerySource("LeftStream", 1),
                new QuerySink(5, "localhost", 1083, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(leftQuery, "localhost", 1080);


        Thread.sleep(10000);


        /* Move Join from P2 to P1 */
        /* 1. Stop the old queryengine on P1 */
        QueryControl.sendQueryStop(new QueryStop(5), "localhost", 1080);

        /* 2. Migrate the queryengine from P2 to P1 */
        leftSource = new QuerySource("LeftStream", 6);
        rightSource = new QuerySource("RightStream", 1);
        QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1080,
                Arrays.asList(leftSource, rightSource), Arrays.asList(new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD)), "localhost", 1080), "localhost", 1082);

        /* 3. Start a new forwarding queryengine on P2 */
        Query rightQuery = new Query(6, preJoinAppRight, new QuerySource("RightStream", 2), new QuerySink(6, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(rightQuery, "localhost", 1082);

    }


    @Test
    public void leftForwardAdapt() throws InterruptedException {
        BasicConfigurator.configure();
        File file = new File("results.txt");
        file.delete();

        /* Start the nodes */
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("A","localhost", 1084));
        node3.start();
        Thread.sleep(15000);

        /* Start the queries on P1 (join) and P2 forward */
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 3);
        Query joinQuery = new Query(1, joinApp, Arrays.asList(new QuerySource[]{leftSource, rightSource}),
                Arrays.asList(new QuerySink[]{new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD)}));
        QueryControl.sendQuery(joinQuery, "localhost", 1080);

        Query rightQuery = new Query(2, preJoinAppRight, new QuerySource("RightStream", 2),
                new QuerySink(3, "localhost", 1081, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(rightQuery, "localhost", 1082);


        Query forward = new Query(3, postJoinApp, Arrays.asList(new QuerySource[]{new QuerySource("PostJoinStream", 4)}), new ArrayList<>(0));
        QueryControl.sendQuery(forward, "localhost", 1084);

        /* Start the source threads */
        SimpleSourceThread src1 = new FireSource("localhost", 2080, "localhost", 1081, 1, 10, 0, 0);
        SimpleSourceThread src2 = new FireSource("localhost", 2081, "localhost", 1083, 2, 1000, 0, 0);
        src1.start();
        src2.start();

        Thread.sleep(10000);

        /* Move Join from P1 to D1 */
        /* 1. Redirect P2 to D1 */
        QueryControl.sendQueryRedirect(new QueryRedirect(2, Arrays.asList(new QuerySink(3, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD))),
                "localhost", 1082);


        /* 3. Migrate the queryengine from P1 to D1 */
        leftSource = new QuerySource("LeftStream", 5);
        rightSource = new QuerySource("RightStream", 3);
        QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1084, Arrays.asList(leftSource, rightSource), new ArrayList<>(), "localhost", 1080),
                "localhost", 1080);

        /* 2. Stop the old queryengine on D1 */
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1084);


        /* 3. Start a new forwarding queryengine on P1 */
        Query leftQuery = new Query(6, preJoinAppLeft, new QuerySource("LeftStream", 1), new QuerySink(5, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        QueryControl.sendQuery(leftQuery, "localhost", 1080);

        Thread.sleep(10000);
    }




    @Test
    public void testDistributedQuery() throws InterruptedException {
        BasicConfigurator.configure();
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();
        OverlayInstanceThread node3 = new OverlayInstanceThread(new Instance("A","localhost", 1084));
        node3.start();


        Thread.sleep(15000);

        QuerySource leftSource = new QuerySource("LeftStream", 3);
        QuerySource rightSource = new QuerySource("RightStream", 4);


        Query leftQuery = new Query(1, preJoinAppLeft, new QuerySource("LeftStream", 1), new QuerySink(3, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query rightQuery = new Query(2, preJoinAppRight, new QuerySource("RightStream", 2), new QuerySink(4, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));
        Query joinQuery = new Query(3, joinApp, Arrays.asList(new QuerySource[]{leftSource, rightSource}), new ArrayList<>(0));

        QueryControl.sendQuery(leftQuery, "localhost", 1080);
        QueryControl.sendQuery(rightQuery, "localhost", 1082);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);
        Thread.sleep(500);

        SimpleSourceThread src1 = new FireSource("localhost", 2080, "localhost", 1081, 1, 5, 0, 0);
        src1.start();

        SimpleSourceThread src2 = new FireSource("localhost", 2081, "localhost", 1083, 2, 5, 0, 0);
        src2.start();

        Thread.sleep(10000);
    }


    @Test
    public void testMultiSourceQuery() throws InterruptedException {
        BasicConfigurator.configure();
        OverlayInstanceThread node = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node.start();
        Thread.sleep(15000);

        List<QuerySource> sources = Arrays.asList(new QuerySource[]{new QuerySource("LeftStream", 1), new QuerySource("RightStream", 2)});
        Query query = new Query(1, joinApp, sources, new ArrayList<>(0));
        QueryControl.sendQuery(query, "localhost", 1080);

        SimpleSourceThread src1 = new SimpleSourceThread("localhost", 2080, "localhost", 1081, 1, 5, new SourceFilter(true));
        src1.start();

        SimpleSourceThread src2 = new SimpleSourceThread("localhost", 2081, "localhost", 1081, 2, 5, new SourceFilter(true));
        src2.start();

        Thread.sleep(10000);
    }

    @Test
    public void testJoinRedirect() throws InterruptedException {
        BasicConfigurator.configure();
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();
        Thread.sleep(15000);

        List<QuerySource> sources = Arrays.asList(new QuerySource[]{new QuerySource("LeftStream", 1), new QuerySource("RightStream", 2)});
        Query query = new Query(1, joinApp, sources);
        QueryControl.sendQuery(query, "localhost", 1080);

        Thread.sleep(500);

        SimpleSourceThread src1 = new SimpleSourceThread("localhost", 2080, "localhost", 1081, 1, 5, new SourceFilter(true));
        src1.start();

        SimpleSourceThread src2 = new SimpleSourceThread("localhost", 2081, "localhost", 1081, 2, 5, new SourceFilter(true));
        src2.start();
        Thread.sleep(10000);
        src1.redirect("localhost", 1083);
        src2.redirect("localhost", 1083);
        QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1082, query.getQuerySources(), null, "localhost", 1080), "localhost", 1080);
        Thread.sleep(10000);

        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
    }

    @Test
    public void testQueryMigration() throws InterruptedException {
        BasicConfigurator.configure();

        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A","localhost", 1080));
        node1.start();
        OverlayInstanceThread node2 = new OverlayInstanceThread(new Instance("A","localhost", 1082));
        node2.start();

        Thread.sleep(15000);
        QueryControl.sendQuery(new Query(1, siddhiApp, "MortenStream", 1), "localhost", 1080);
        Thread.sleep(500);

        SimpleSourceThread src = new SimpleSourceThread("localhost", 2080, "localhost", 1081, 1, 2, new SourceFilter(true));
        src.start();

        for (int i = 0; i < 5; i++) {
            Thread.sleep(10000);
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1082, Arrays.asList(new QuerySource("MortenStream", 1)), null, "localhost", 1080), "localhost", 1080);
            src.redirect("localhost", 1083);
            Thread.sleep(10000);
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "localhost", 1080, Arrays.asList(new QuerySource("MortenStream", 1)), null, "localhost", 1080), "localhost", 1082);
            src.redirect("localhost", 1081);
        }

        src.shutdown();
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
    }

    @Test
    public void testQueryInitiation() throws InterruptedException {
        BasicConfigurator.configure();
        System.out.println("Starting overlay node on port 1080");
        OverlayInstanceThread node1 = new OverlayInstanceThread(new Instance("A", "localhost", 1080));
        node1.start();

        Thread.sleep(15000);

        QueryControl.sendQuery(new Query(1, siddhiApp, "MortenStream", 1), "localhost", 1080);

        Thread.sleep(500);
        System.out.println("-> Starting source ");
        SimpleSourceThread src1 = new SimpleSourceThread("localhost", 2080, "localhost", 1081, 1, 1,new SourceFilter(true));
        src1.start();

        System.out.println("Starting to send from source ");
        Thread.sleep(5000);

        src1.shutdown();
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1080);
    }
}

