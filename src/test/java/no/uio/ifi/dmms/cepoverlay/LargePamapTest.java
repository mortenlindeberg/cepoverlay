package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import no.uio.ifi.dmms.cepoverlay.source.PAMAPSource;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries.*;

public class LargePamapTest {


    @Test
    public void largePamapTest() throws InterruptedException {
        //storeToPcap("largePamap.pcap");
        BasicConfigurator.configure();
        Runners.cleanOutputFiles(Arrays.asList(Main.START_FILENAME));
        Runners.writeStartTime(Main.START_FILENAME);

        /* Create and start the nodes */
        List<String> nodeNames = Arrays.asList("TP1", "AP1", "TP2", "AP2", "TP3", "AP3", "TP4", "AP4", "P1", "P2", "P3", "P4", "A", "B", "C");
        HashMap<String, OverlayInstanceThread> nodes = new HashMap<>();
        int portStart = 1080;

        for (String name : nodeNames) {
            System.out.println("> Creating node: "+name+" at port "+portStart);
            nodes.put(name, new OverlayInstanceThread(new Instance(name, "localhost", portStart)));
            portStart += 2;
            nodes.get(name).start();
        }

        /* Create the sources */
        List<String> sourceNames = Arrays.asList("tempSourceP1", "actSourceP1", "tempSourceP2", "actSourceP2", "tempSourceP3", "actSourceP3", "tempSourceP4", "actSourceP4");
        HashMap<String, QuerySource> querySources = new HashMap<>();
        int i = 1;
        for (String sourceName : sourceNames) {
            QuerySource tempSource = new QuerySource(sourceName, i++);
            querySources.put(sourceName, tempSource);
        }

        Thread.sleep(15000);

        /* Create the sinks */
        List<QuerySink> writerSink = asList(new QuerySink(i, null, -1, QueryControl.SINK_TYPE_WRITE));

        /* Create the queries */
        Query joinQuery = new Query(1, largePAMAPJoin, new ArrayList(querySources.values()), writerSink);

        System.out.println("> Sending query to port "+1108);
        QueryControl.sendQuery(joinQuery, "localhost", 1108);

        Thread.sleep(500);

        /* Activate the SourceThreads */
        for (i = 0; i < 8; i++) {
            System.out.println(" > New PAMAP "+(i+1)+" source on port " +(2080+i) + " to " +1109);
            new PAMAPSource("localhost", 2080+i, "localhost", 1109, i+1, 5).start();
        }


        /* Let the experiment run */
        Thread.sleep(30000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (i = 0; i < 8; i++) {
            QueryControl.sendOverlayStop("localhost", 2080 + i);
        }

        Thread.sleep(1000);

        /* Stop the queries */
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1108);
        Thread.sleep(1000);

        /* Stop the overlay nodes */
       portStart = 1080;

        for (String name : nodeNames) {
            System.out.println("> Stopping" +portStart);
            QueryControl.sendOverlayStop("localhost", portStart);
            portStart += 2;
        }
        Thread.sleep(2000);
        //killPcap();
    }

    @Test
    public void largePamapTestSplit() throws InterruptedException {
        //storeToPcap("largePamap.pcap");
        BasicConfigurator.configure();
        Runners.writeStartTime(Main.START_FILENAME);
        Runners.cleanOutputFiles(Arrays.asList("output.res"));

        /* Create and start the nodes */
        List<String> nodeNames = Arrays.asList("TP1", "AP1", "TP2", "AP2", "TP3", "AP3", "TP4", "AP4", "P1", "P2", "P3", "P4", "A", "B", "C");
        HashMap<String, OverlayInstanceThread> nodes = new HashMap<>();
        int portStart = 1080;

        for (String name : nodeNames) {
            System.out.println("> Creating node: "+name+" at port "+portStart);
            nodes.put(name, new OverlayInstanceThread(new Instance(name, "localhost", portStart)));
            portStart += 2;
            nodes.get(name).start();
        }

        /* Create the sources */
        List<String> sourceNamesLeft = Arrays.asList("tempSourceP1", "actSourceP1", "tempSourceP2", "actSourceP2");
        List<String> sourceNamesRight= Arrays.asList("tempSourceP3", "actSourceP3", "tempSourceP4", "actSourceP4");

        HashMap<String, QuerySource> querySourcesLeft = new HashMap<>();
        int i = 1;
        for (String sourceName : sourceNamesLeft) {
            QuerySource tempSource = new QuerySource(sourceName, i++);
            querySourcesLeft.put(sourceName, tempSource);
        }

        HashMap<String, QuerySource> querySourcesRight = new HashMap<>();
        for (String sourceName : sourceNamesRight) {
            QuerySource tempSource = new QuerySource(sourceName, i++);
            querySourcesRight.put(sourceName, tempSource);
        }

        Thread.sleep(15000);

        /* Create the sinks */
        List<QuerySink> leftSink = asList(new QuerySink(9, "localhost", 1109, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> rightSink = asList(new QuerySink(10, "localhost", 1109, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> writerSink = asList(new QuerySink(11, null, -1, QueryControl.SINK_TYPE_WRITE));


        /* Create the queries */
        Query leftJoinQuery = new Query(1, leftJoinPAMAPQueryApp, new ArrayList(querySourcesLeft.values()), leftSink);
        QueryControl.sendQuery(leftJoinQuery, "localhost", 1104);

        Query rightJoinQuery = new Query(2, rightJoinPAMAPQueryApp, new ArrayList(querySourcesRight.values()), rightSink);
        QueryControl.sendQuery(rightJoinQuery, "localhost", 1106);

        QuerySource leftSource = new QuerySource("OutputStream1", 9);
        QuerySource rightSource = new QuerySource("OutputStream2", 10);

        Query joinQuery = new Query(3, finalJoinPAMAPQueryApp, Arrays.asList(leftSource, rightSource), writerSink);
        QueryControl.sendQuery(joinQuery, "localhost", 1108);

        Thread.sleep(500);

        /* Activate the SourceThreads */
        for (i = 0; i < 4; i++) {
            System.out.println(" > New PAMAP "+(i+1)+" source on port " +(2080+i) + " to " +1105);
            new PAMAPSource("localhost", 2080+i, "localhost", 1105, i+1, 5).start();
        }
        for (i = 4; i < 8; i++) {
            System.out.println(" > New PAMAP "+(i+1)+" source on port " +(2080+i) + " to " +1107);
            new PAMAPSource("localhost", 2080+i, "localhost", 1107, i+1, 5).start();
        }


        /* Let the experiment run */
        Thread.sleep(30000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        for (i = 0; i < 8; i++) {
            QueryControl.sendOverlayStop("localhost", 2080 + i);
        }

        Thread.sleep(1000);

        /* Stop the overlay nodes */
        portStart = 1080;

        for (String name : nodeNames) {
            System.out.println("> Stopping" +portStart);
            QueryControl.sendOverlayStop("localhost", portStart);
            portStart += 2;
        }
        Thread.sleep(2000);
        //killPcap();
    }


}
