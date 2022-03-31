package no.uio.ifi.dmms.cepoverlay.queries;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.*;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries.*;
import static no.uio.ifi.dmms.cepoverlay.queries.PowerTempQueries.*;
import static no.uio.ifi.dmms.cepoverlay.queries.SyntheticQueries.joinApp;
import static no.uio.ifi.dmms.cepoverlay.queries.SyntheticQueries.postJoinApp;

public class Runners {
    private static final Logger log = Logger.getLogger(Runners.class.getName());
    private static final int WIRESHARK_TIME_OFFSET = 14082; /* Hardcoded value for now - represents the amount of milliseconds from start.res file was created to the wireshark logging happens.
                                                             * This is used for optimal placements based upon analysis of wireshark stuff
                                                             */

    public static List<QuerySource> pamapJoinSources = Arrays.asList(
            new QuerySource("OutputStreamP1", 26),
            new QuerySource("OutputStreamP2", 27),
            new QuerySource("OutputStreamP3", 28),
            new QuerySource("OutputStreamP4", 29),
            new QuerySource("OutputStreamP5", 30),
            new QuerySource("OutputStreamP6", 31),
            new QuerySource("OutputStreamP7", 32),
            new QuerySource("OutputStreamP8", 33));

    private static long startTime = -1;

    public static ConcurrentHashMap<String, Boolean> getHRRateMap() {
        ConcurrentHashMap<String, Boolean> rateMap = new ConcurrentHashMap<>();
        rateMap.put("10.0.0.3", false); // 1
        rateMap.put("10.0.0.6", true); // 2
        rateMap.put("10.0.0.9", true); // 3
        rateMap.put("10.0.0.12", true); //4
        rateMap.put("10.0.0.15", true); // 5
        rateMap.put("10.0.0.18", true); // 6
        rateMap.put("10.0.0.21", true); // 7
        rateMap.put("10.0.0.24", true); // 8

        return rateMap;
    }

    public static ConcurrentHashMap<String, Boolean> getTempRateMap() {
        ConcurrentHashMap<String, Boolean> rateMap = new ConcurrentHashMap<>();
        rateMap.put("10.0.0.3", true); // 1
        rateMap.put("10.0.0.6", false); // 2
        rateMap.put("10.0.0.9", true); // 3
        rateMap.put("10.0.0.12", true); //4
        rateMap.put("10.0.0.15", false); // 5
        rateMap.put("10.0.0.18", false); // 6
        rateMap.put("10.0.0.21", true); // 7
        rateMap.put("10.0.0.24", false); // 8

        return rateMap;
    }


    public static void activityQuery(CommandLine cmd, int strategy) throws InterruptedException {

        /* Create the queries */
        QuerySource aSource = new QuerySource("ActivityStream", 8);
        QuerySource hSource = new QuerySource("HeartRateStream", 9);
        QuerySource postSource = new QuerySource("PostJoinStream", 12);


        if (strategy == 15 || strategy == 18) { //TODO: Might as well remove post join, and add filter query ahead of join on A
            // Strategy represents mode B
            List<QuerySink> aSink = asList(new QuerySink(10, "10.0.0.1", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query aQuery = new Query(1, activityApp, asList(aSource), aSink);
            QueryControl.sendQuery(aQuery, "10.0.0.1", 1080);

            List<QuerySink> hSink = asList(new QuerySink(11, "10.0.0.1", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query hQuery = new Query(2, heartApp, asList(hSource), hSink);
            QueryControl.sendQuery(hQuery, "10.0.0.2", 1080);

            QuerySource aFSource = new QuerySource("ActivityStream", 10);
            QuerySource hFSource = new QuerySource("HeartRateStream", 11);

            List<QuerySink> joinSink = asList(new QuerySink(12, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query joinQuery = new Query(3, activityJoinApp, asList(aFSource, hFSource), joinSink);
            QueryControl.sendQuery(joinQuery, "10.0.0.1", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(13, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query postJoinQuery = new Query(4, activityPostJoinApp, asList(postSource), writerSink);
            QueryControl.sendQuery(postJoinQuery, "10.0.0.3", 1080);
        } else if (strategy == 16) { //TODO: Might as well remove post join, and add filter query ahead of join on B
            // Strategy represents mode A
            List<QuerySink> aSink = asList(new QuerySink(10, "10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query aQuery = new Query(1, activityApp, asList(aSource), aSink);
            QueryControl.sendQuery(aQuery, "10.0.0.1", 1080);

            List<QuerySink> hSink = asList(new QuerySink(11, "10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query hQuery = new Query(2, heartApp, asList(hSource), hSink);
            QueryControl.sendQuery(hQuery, "10.0.0.2", 1080);

            QuerySource aFSource = new QuerySource("ActivityStream", 10);
            QuerySource hFSource = new QuerySource("HeartRateStream", 11);
            List<QuerySink> joinSink = asList(new QuerySink(12, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query joinQuery = new Query(3, activityJoinApp, asList(aFSource, hFSource), joinSink);
            QueryControl.sendQuery(joinQuery, "10.0.0.2", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(13, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query postJoinQuery = new Query(4, activityPostJoinApp, asList(postSource), writerSink);
            QueryControl.sendQuery(postJoinQuery, "10.0.0.3", 1080);
        } else if (strategy == 17) {
            List<QuerySink> aSink = asList(new QuerySink(10, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query aQuery = new Query(1, activityApp, asList(aSource), aSink);
            QueryControl.sendQuery(aQuery, "10.0.0.1", 1080);

            List<QuerySink> hSink = asList(new QuerySink(11, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query hQuery = new Query(2, heartApp, asList(hSource), hSink);
            QueryControl.sendQuery(hQuery, "10.0.0.2", 1080);

            QuerySource aFSource = new QuerySource("ActivityStream", 10);
            QuerySource hFSource = new QuerySource("HeartRateStream", 11);
            List<QuerySink> joinSink = asList(new QuerySink(12, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));
            Query joinQuery = new Query(3, activityJoinApp, asList(aFSource, hFSource), joinSink);
            QueryControl.sendQuery(joinQuery, "10.0.0.3", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(13, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query postJoinQuery = new Query(4, activityPostJoinApp, asList(postSource), writerSink);
            QueryControl.sendQuery(postJoinQuery, "10.0.0.3", 1080);
        }



        /* Let the experiment run */
        int duration = Integer.parseInt(cmd.getOptionValue("duration"));
        Thread.sleep(duration * 1000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("10.0.0.1", 2080);
        QueryControl.sendOverlayStop("10.0.0.2", 2080);
        Thread.sleep(1000);

        QueryControl.sendOverlayStop("10.0.0.1", 1080);
        QueryControl.sendOverlayStop("10.0.0.2", 1080);
        QueryControl.sendOverlayStop("10.0.0.3", 1080);
        Thread.sleep(2000);

    }

    public static void loadTempQuery(CommandLine cmd, int strategy) throws InterruptedException {
        QuerySource tempAggSource = new QuerySource("temperatureStream", 0);
        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        QuerySource loadSource = new QuerySource("loadStream", 2);
        List<QuerySource> postJoinSource = asList(new QuerySource("PostJoinStream", 3));
        List<QuerySink> joinSinkTemp = asList(new QuerySink(3, "10.0.0.4    ", 1081, QueryControl.SINK_TYPE_FORWARD));

        List<QuerySink> tempAggSink = asList(new QuerySink(1, "10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD));
        Query tempAggQuery = new Query(0, tempAggQueryApp, asList(tempAggSource), tempAggSink);
        QueryControl.sendQuery(tempAggQuery, "10.0.0.1", 1080);


        if (strategy == 11 || strategy == 14) { /* Placement of Join at A (with or without Join. Source decides)*/
            /* Create the queries */
            Query joinQuery = new Query(1, tempJoinQueryApp, asList(tempSource, loadSource), joinSinkTemp);
            QueryControl.sendQuery(joinQuery, "10.0.0.2", 1080);

            QuerySink loadSink = new QuerySink(2, "10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD);
            Query loadQuery = new Query(2, loadQueryApp, asList(loadSource), asList(loadSink));
            QueryControl.sendQuery(loadQuery, "10.0.0.3", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(4, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query postQuery = new Query(3, tempPostJoinApp, postJoinSource, writerSink);
            QueryControl.sendQuery(postQuery, "10.0.0.4", 1080);
        } else if (strategy == 12) { /* Placement of Join at B */
            /* Create the queries */
            Query joinQuery = new Query(1, tempJoinQueryApp, asList(tempSource, loadSource), joinSinkTemp);
            QueryControl.sendQuery(joinQuery, "10.0.0.3", 1080);

            QuerySink tempSink = new QuerySink(1, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD);
            Query tempQuery = new Query(2, tempQueryApp, asList(loadSource), asList(tempSink));
            QueryControl.sendQuery(tempQuery, "10.0.0.2", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(4, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query postJoinQuery = new Query(3, tempPostJoinApp, postJoinSource, writerSink);
            QueryControl.sendQuery(postJoinQuery, "10.0.0.4", 1080);
        } else if (strategy == 13) { /* Placement of Join at C */
            /* Create the queries */

            QuerySink tempSink = new QuerySink(1, "10.0.0.4", 1081, QueryControl.SINK_TYPE_FORWARD);
            Query tempQuery = new Query(1, tempQueryApp, asList(tempSource), asList(tempSink));
            QueryControl.sendQuery(tempQuery, "10.0.0.2", 1080);

            QuerySink loadSink = new QuerySink(2, "10.0.0.4", 1081, QueryControl.SINK_TYPE_FORWARD);
            Query loadQuery = new Query(2, loadQueryApp, asList(loadSource), asList(loadSink));
            QueryControl.sendQuery(loadQuery, "10.0.0.3", 1080);

            List<QuerySink> writerSink = asList(new QuerySink(4, null, -1, QueryControl.SINK_TYPE_WRITE));
            Query joinQuery = new Query(3, tempJoinQueryApp, asList(tempSource, loadSource), writerSink);
            QueryControl.sendQuery(joinQuery, "10.0.0.4", 1080);
        } else System.err.println("Don't know what the h*** to do..");

        /* Let the experiment run */
        int duration = Integer.parseInt(cmd.getOptionValue("duration"));
        Thread.sleep(duration * 1000);

        /* Stop the source threads */
        QueryControl.sendOverlayStop("10.0.0.1", 2080);
        QueryControl.sendOverlayStop("10.0.0.2", 2081);
        Thread.sleep(1000);

        QueryControl.sendOverlayStop("10.0.0.1", 1080);
        QueryControl.sendOverlayStop("10.0.0.2", 1080);
        QueryControl.sendOverlayStop("10.0.0.3", 1080);
        Thread.sleep(2000);

    }

    public static void syntheticQuery(CommandLine cmd, int strategy) throws InterruptedException {
        cleanOutputFiles(Arrays.asList("output.res", "original.res"));

        List<QuerySource> writerSource = asList(new QuerySource("PostJoinStream", 3));
        List<QuerySink> writerSink = asList(new QuerySink(3, null, -1, QueryControl.SINK_TYPE_WRITE));

        List<QuerySink> joinSink = asList(new QuerySink(3, "10.0.0.6", 1087, QueryControl.SINK_TYPE_FORWARD));
        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 2);

        Query writerQuery = new Query(2, postJoinApp, writerSource, writerSink);
        List<QuerySource> joinSource = asList(new QuerySource[]{leftSource, rightSource});
        QueryControl.sendQuery(writerQuery, "10.0.0.6", 1086);

        /* Start the query engine on the "join" node (depending on strategy / config) */
        Query joinQuery = new Query(1, joinApp, joinSource, joinSink);

        if (strategy == 3)
            QueryControl.sendQuery(joinQuery, "10.0.0.3", 1080); // At E1

        else if (strategy == 4)
            QueryControl.sendQuery(joinQuery, "10.0.0.5", 1084); // At I1

        else
            QueryControl.sendQuery(joinQuery, "10.0.0.4", 1082); // At E2

        /* Let the experiment run */
        int duration = Integer.parseInt(cmd.getOptionValue("duration"));
        log.debug(" > Let experiment run..");
        Thread.sleep(duration * 1000);
        log.debug(" > Shutting down experiment");
        /* Stop the source threads */
        QueryControl.sendOverlayStop("10.0.0.1", 2080);
        QueryControl.sendOverlayStop("10.0.0.2", 2081);
        QueryControl.sendOverlayStop("10.0.0.1", 1076);
        QueryControl.sendOverlayStop("10.0.0.2", 1078);
        QueryControl.sendOverlayStop("10.0.0.3", 1080);
        QueryControl.sendOverlayStop("10.0.0.4", 1082);
        QueryControl.sendOverlayStop("10.0.0.5", 1084);
        QueryControl.sendOverlayStop("10.0.0.6", 1086);
        Thread.sleep(2000);
    }

    public static void windowAwarePAMAPQuery(CommandLine cmd, int strategy) throws InterruptedException {
        log.debug("SYNC "+System.currentTimeMillis());
        int controlPort = 1080;
        int dataPort = 1081;

        String saIP = "10.0.0.1";
        String sbIP = "10.0.0.2";
        String aIP = "10.0.0.3";
        String bIP = "10.0.0.4";
        String cIP = "10.0.0.5";


        /* Create the queries */
        QuerySource hSource = new QuerySource("tempSource", 8);
        QuerySource aSource = new QuerySource("actSource", 9);

        List<QuerySink> hSink = asList(new QuerySink(10, cIP, dataPort, QueryControl.SINK_TYPE_FORWARD));
        Query aQuery = new Query(1, heartApp, asList(hSource), hSink);
        QueryControl.sendQuery(aQuery, aIP, controlPort);

        List<QuerySink> aSink = asList(new QuerySink(11, cIP, dataPort, QueryControl.SINK_TYPE_FORWARD));
        Query hQuery = new Query(2, activityApp, asList(aSource), aSink);
        QueryControl.sendQuery(hQuery, bIP, controlPort);

        int sinkType;
        if (strategy == 18)
            sinkType = QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS;
        else sinkType = QueryControl.SINK_TYPE_FORWARD;

        List<QuerySink> joinSinkTemp;
        joinSinkTemp = asList(new QuerySink(12, cIP, dataPort, sinkType));

        QuerySource hPJSource = new QuerySource("tempSource", 10);
        QuerySource aPJSource = new QuerySource("actSource", 11);
        List<QuerySource> joinSources = asList(hPJSource, aPJSource);
        Query joinQuery = new Query(3, activityJoinAppExt, joinSources, joinSinkTemp);
        QueryControl.sendQuery(joinQuery, cIP, controlPort);

        List<QuerySource> postJoinSource = asList(new QuerySource("postJoinSource", 12));
        List<QuerySink> writeSink = asList(new QuerySink(13, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query postJoinQuery = new Query(4, activityPostJoinApp, postJoinSource, writeSink);
        QueryControl.sendQuery(postJoinQuery, cIP, controlPort);

        /* Run the experiment where we do multiple migrations */
        Thread.sleep(10000);

        List<Redirect> redirects = asList(new Redirect(1, 10, aIP, controlPort), new Redirect(2,  11, bIP, controlPort));
        for (int i = 0; i < 100; i++) {
            /* Migrate join, query 3 from C to A */
            WindowAwareQueryMigrate windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, aIP, controlPort, redirects, joinSources, joinSinkTemp, cIP, controlPort);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, cIP, controlPort);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

            /* Migrate join, query 3 from A to B */
            windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, bIP, controlPort, redirects, joinSources, joinSinkTemp, cIP, controlPort);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, aIP, controlPort);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

            /* Migrate join, query 3 from B to C */
            windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, cIP, controlPort, redirects, joinSources, joinSinkTemp,  cIP, controlPort);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, bIP, controlPort);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

        }


        /* Stop the nodes */
        for (int i = 1; i <= 3; i++) {
            String address = "10.0.0." + i;
            QueryControl.sendOverlayStop(address, controlPort);
        }

        Thread.sleep(1000);

    }

    //TODO: Unused as of now
    public static void largePAMAPQueryPX(CommandLine cmd, int strategy, String placementIP) throws InterruptedException {
        /* Create the query sources */
        List<QuerySource> p1Sources = Arrays.asList(new QuerySource("tempSource", 10), new QuerySource("actSource", 11));
        List<QuerySource> p2Sources = Arrays.asList(new QuerySource("tempSource", 12), new QuerySource("actSource", 13));
        List<QuerySource> p3Sources = Arrays.asList(new QuerySource("tempSource", 14), new QuerySource("actSource", 15));
        List<QuerySource> p4Sources = Arrays.asList(new QuerySource("tempSource", 16), new QuerySource("actSource", 17));

        List<QuerySource> joinSources = Arrays.asList(
                new QuerySource("OutputStreamP1", 18),
                new QuerySource("OutputStreamP2", 19),
                new QuerySource("OutputStreamP3", 20),
                new QuerySource("OutputStreamP4", 21));

        QuerySource lastSource = new QuerySource("InputStream", 22);


        /* Create the sinks */
        List<QuerySink> forwardSinkP1 = asList(new QuerySink(18, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP2 = asList(new QuerySink(19, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP3 = asList(new QuerySink(20, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP4 = asList(new QuerySink(21, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));

        List<QuerySink> forwardSink = asList(new QuerySink(22, "10.0.0.15", 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> writerSink = asList(new QuerySink(23, null, -1, QueryControl.SINK_TYPE_WRITE));

        /* The queries.. */
        Query p1Query = new Query(1, largePAMAPTempFilter, p1Sources, forwardSinkP1);
        Query p2Query = new Query(2, largePAMAPTempFilter, p2Sources, forwardSinkP2);
        Query p3Query = new Query(3, largePAMAPTempFilter, p3Sources, forwardSinkP3);
        Query p4Query = new Query(4, largePAMAPTempFilter, p4Sources, forwardSinkP4);

        QueryControl.sendQuery(p1Query, "10.0.0.3", 1080);
        QueryControl.sendQuery(p2Query, "10.0.0.6", 1080);
        QueryControl.sendQuery(p3Query, "10.0.0.9", 1080);
        QueryControl.sendQuery(p4Query, "10.0.0.12", 1080);

        Query joinQuery = new Query(5, largePAMAPJoinPostPerson, joinSources, forwardSink);
        QueryControl.sendQuery(joinQuery, placementIP, 1080);

        Query lastQuery = new Query(6, postJoinPAMAPTempQuery, Arrays.asList(lastSource), writerSink);
        QueryControl.sendQuery(lastQuery, "10.0.0.15", 1080);

        /* Let the experiment run */
        int duration = Integer.parseInt(cmd.getOptionValue("duration"));
        Thread.sleep(duration * 1000);


        /* Stop the nodes */
        for (int i = 1; i <= 15; i++) {
            String address = "10.0.0." + i;
            QueryControl.sendOverlayStop(address, 1080);
        }

        Thread.sleep(1000);
    }

    public static void extraLargePAMAPQueryPX(CommandLine cmd, int query, String placementIP) throws InterruptedException {
        /* Create the query sources */
        List<QuerySource> p1Sources = Arrays.asList(new QuerySource("tempSource", 10), new QuerySource("actSource", 11));
        List<QuerySource> p2Sources = Arrays.asList(new QuerySource("tempSource", 12), new QuerySource("actSource", 13));
        List<QuerySource> p3Sources = Arrays.asList(new QuerySource("tempSource", 14), new QuerySource("actSource", 15));
        List<QuerySource> p4Sources = Arrays.asList(new QuerySource("tempSource", 16), new QuerySource("actSource", 17));
        List<QuerySource> p5Sources = Arrays.asList(new QuerySource("tempSource", 18), new QuerySource("actSource", 19));
        List<QuerySource> p6Sources = Arrays.asList(new QuerySource("tempSource", 20), new QuerySource("actSource", 21));
        List<QuerySource> p7Sources = Arrays.asList(new QuerySource("tempSource", 22), new QuerySource("actSource", 23));
        List<QuerySource> p8Sources = Arrays.asList(new QuerySource("tempSource", 24), new QuerySource("actSource", 25));

        List<QuerySource> joinSources = Arrays.asList(
                new QuerySource("OutputStreamP1", 26),
                new QuerySource("OutputStreamP2", 27),
                new QuerySource("OutputStreamP3", 28),
                new QuerySource("OutputStreamP4", 29),
                new QuerySource("OutputStreamP5", 30),
                new QuerySource("OutputStreamP6", 31),
                new QuerySource("OutputStreamP7", 32),
                new QuerySource("OutputStreamP8", 33));

        QuerySource lastSource = new QuerySource("InputStream", 34);


        /* Create the sinks */
        List<QuerySink> forwardSinkP1 = asList(new QuerySink(26, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP2 = asList(new QuerySink(27, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP3 = asList(new QuerySink(28, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP4 = asList(new QuerySink(29, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP5 = asList(new QuerySink(30, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP6 = asList(new QuerySink(31, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP7 = asList(new QuerySink(32, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP8 = asList(new QuerySink(33, placementIP, 1081, QueryControl.SINK_TYPE_FORWARD));

        List<QuerySink> forwardSink;
        if (ConfigReader.readWindowAware()) {
            forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS));
            log.debug("> Forward sink is window aware!");
        }
        else {
            forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD));
            log.debug("> Forward sink is NOT window aware!");
        }

        List<QuerySink> writerSink = asList(new QuerySink(35, null, -1, QueryControl.SINK_TYPE_WRITE));

        String filter;
        String postPerson;
        String postJoin;
        switch (query) {
            case 1:
                filter = largePAMAPTempFilter;
                postPerson = extraLargePAMAPTempJoinPostPerson;
                postJoin = postJoinPAMAPTempQuery;
                break;
            case 2:
                filter = largePAMAPHRFilter;
                postPerson = extraLargePAMAPHRJoinPostPerson;
                postJoin = postJoinPAMAPHRQuery;
                break;
            case 3:
                filter = largePAMAPTempFilter;
                postPerson = extraLargePAMAPTempWindowJoinPostPerson;
                postJoin = postJoinPAMAPTempQuery;
                break;
            case 4:
                filter = largePAMAPHRFilter;
                postPerson = extraLargePAMAPHRWindowJoinPostPerson;
                postJoin = postJoinPAMAPHRQuery;
                break;
            default:
                log.error("Unknown query! "+query);
                return;

        }

        /* The queries.. */
        Query p1Query = new Query(1, filter, p1Sources, forwardSinkP1);
        Query p2Query = new Query(2, filter, p2Sources, forwardSinkP2);
        Query p3Query = new Query(3, filter, p3Sources, forwardSinkP3);
        Query p4Query = new Query(4, filter, p4Sources, forwardSinkP4);
        Query p5Query = new Query(5, filter, p5Sources, forwardSinkP5);
        Query p6Query = new Query(6, filter, p6Sources, forwardSinkP6);
        Query p7Query = new Query(7, filter, p7Sources, forwardSinkP7);
        Query p8Query = new Query(8, filter, p8Sources, forwardSinkP8);

        QueryControl.sendQuery(p1Query, "10.0.0.3", 1080);
        QueryControl.sendQuery(p2Query, "10.0.0.6", 1080);
        QueryControl.sendQuery(p3Query, "10.0.0.9", 1080);
        QueryControl.sendQuery(p4Query, "10.0.0.12", 1080);
        QueryControl.sendQuery(p5Query, "10.0.0.15", 1080);
        QueryControl.sendQuery(p6Query, "10.0.0.18", 1080);
        QueryControl.sendQuery(p7Query, "10.0.0.21", 1080);
        QueryControl.sendQuery(p8Query, "10.0.0.24", 1080);

        Query joinQuery = new Query(9, postPerson, joinSources, forwardSink);
        QueryControl.sendQuery(joinQuery, placementIP, 1080);

        Query lastQuery = new Query(10, postJoin, Arrays.asList(lastSource), writerSink);
        QueryControl.sendQuery(lastQuery, "10.0.0.27", 1080);

        /* Let the experiment run */
        int duration = Integer.parseInt(cmd.getOptionValue("duration"));
        Thread.sleep(duration * 1000);


        /* Stop the nodes */
        for (int i = 1; i <= 27; i++) {
            String address = "10.0.0." + i;
            QueryControl.sendOverlayStop(address, 1080);
        }

        Thread.sleep(1000);
    }

    public static void cleanOutputFiles(List<String> filenames) {
        for (String filename : filenames) {
            File file = new File(filename);
            file.delete();
        }
    }

    public static void writeStartTime(String filename) {
        cleanOutputFiles(Arrays.asList(filename));
        FileWriter f;
        try {
            f = new FileWriter(filename, false);

            PrintWriter p = new PrintWriter(f);
            String s = "" + System.currentTimeMillis();
            p.printf(s + "\n");
            p.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long readStartTime(String filename) {
        File file = new File(filename);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String st;

            if ((st = br.readLine()) != null) {
                startTime = Long.parseLong(st);
                return startTime;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getStartTime(String filename) {
        if (startTime == -1)
            readStartTime(filename);
        return startTime;
    }


    public static void extraLargePAMAPQueryPXHardcode(CommandLine cmd, int queryId, String s) throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                scheduleHardcodedPlacements(Integer.parseInt(cmd.getOptionValue("duration")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        extraLargePAMAPQueryPX(cmd, queryId, s);
    }

    private static void scheduleHardcodedPlacements(int duration) throws InterruptedException {
        getStartTime(Main.START_FILENAME);

        List<PlacementDecision> decisions = Arrays.asList(
                new PlacementDecision(70000, "C1" ,"J2"),
                new PlacementDecision(108000, "J2" ,"J1"),
                new PlacementDecision(133000, "J1" ,"J2"),
                new PlacementDecision(391000, "J2" ,"J1"),
                new PlacementDecision(555000, "J1" ,"J2"),
                new PlacementDecision(675000, "J2" ,"J1"),
                new PlacementDecision(680000, "J1" ,"J2"),
                new PlacementDecision(701000, "J2" ,"J1"),
                new PlacementDecision(706000, "J1" ,"J2"),
                new PlacementDecision(1106000, "J2" ,"J1"),
                new PlacementDecision(1108000, "J1" ,"J2"),
                new PlacementDecision(1476000, "J2" ,"J1"),
                new PlacementDecision(1586000, "J1" ,"J2"),
                new PlacementDecision(1662000, "J2" ,"J1"),
                new PlacementDecision(1667000, "J1" ,"J2"),
                new PlacementDecision(1728000, "J2" ,"J1"),
                new PlacementDecision(1926000, "J1" ,"J2"),
                new PlacementDecision(2049000, "J2" ,"J1"),
                new PlacementDecision(2053000, "J1" ,"J2"),
                new PlacementDecision(2078000, "J2" ,"J1"),
                new PlacementDecision(2080000, "J1" ,"J2"),
                new PlacementDecision(2082000, "J2" ,"J1"),
                new PlacementDecision(2112000, "J1" ,"J2"),
                new PlacementDecision(2129000, "J2" ,"J1"),
                new PlacementDecision(2207000, "J1" ,"J2"),
                new PlacementDecision(2228000, "J2" ,"J1"),
                new PlacementDecision(2232000, "J1" ,"J2"),
                new PlacementDecision(2332000, "J2" ,"J1"),
                new PlacementDecision(2339000, "J1" ,"J2"),
                new PlacementDecision(4570000, "J2" ,"J1"));

        // Correct the timestamps:
        for (PlacementDecision d : decisions) {
            int t = d.timestamp;
            d.timestamp = t + WIRESHARK_TIME_OFFSET;
        }

        long now = 0;
        long current = 0;
        boolean windowAware = ConfigReader.readWindowAware();
        for (PlacementDecision d : decisions) {
            current = System.currentTimeMillis();
            now = current - startTime;
            while (now < d.timestamp) {
                Thread.sleep(100);
                now = System.currentTimeMillis() - startTime;
            }
            log.debug("Init migration at "+now+" "+d.timestamp+" "+d.from+" "+d.to+" "+(d.timestamp - WIRESHARK_TIME_OFFSET)+" "+startTime+" "+current);
            if (windowAware)
                migrateWindowAware(d.from, d.to);
            else
                migrate(d.from, d.to);
        }
        long remains = now - (duration*5000);
        log.debug("> Master going to sleep for the remaining " +remains+" ms.");
        Thread.sleep(remains);


    }

    private static void migrateWindowAware(String from, String to) throws InterruptedException {
        log.debug(" > Migrate Operator Extra with window awareness!");
        List<QuerySink> forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS)); //TODO: Hardcoded!

        int rQuery = 1; //TODO: Hardcoded value
        int rStream = 26;//TODO: Hardcoded value
        List<Redirect> redirects = new ArrayList();
        List<String> rAddresses = asList("10.0.0.3", "10.0.0.6", "10.0.0.9", "10.0.0.12", "10.0.0.15", "10.0.0.18", "10.0.0.21", "10.0.0.24");

        for (int i = 0; i < rAddresses.size(); i++)
            redirects.add(new Redirect(rQuery++, rStream++, rAddresses.get(i), 1080));
        String currentPlacement = toIp(from);
        String newPlacement = toIp(to);
        WindowAwareQueryMigrate windowAwareQueryMigrate = new WindowAwareQueryMigrate(9, newPlacement, 1080, redirects, Runners.pamapJoinSources, forwardSink, "10.0.0.27", 1080); //TODO: Hardcoded value
        QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, currentPlacement, 1080);
    }

    private static void migrate(String from, String to) throws InterruptedException {
        String currentPlacement = toIp(from);
        String newPlacement = toIp(to);

        /* 0. Notify the new host that it will start to receive new tuples that are not late arrivals */
        List<Integer> streamIds = Arrays.asList(26,27,28,29,30,31,32,33);
        QueryControl.sendAbortLateArrival(new AbortLateArrival(streamIds), newPlacement, 1080);


        List<QuerySink> forwardSinkP1 = asList(new QuerySink(26, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP2 = asList(new QuerySink(27, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP3 = asList(new QuerySink(28, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP4 = asList(new QuerySink(29, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP5 = asList(new QuerySink(30, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP6 = asList(new QuerySink(31, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP7 = asList(new QuerySink(32, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));
        List<QuerySink> forwardSinkP8 = asList(new QuerySink(33, newPlacement, 1081, QueryControl.SINK_TYPE_FORWARD));

        /* 1. Redirect queries to new placements */
        log.debug("> Redirecting queries to " + newPlacement);
        QueryControl.sendQueryRedirect(new QueryRedirect(1, forwardSinkP1), "10.0.0.3", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(2, forwardSinkP2), "10.0.0.6", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(3, forwardSinkP3), "10.0.0.9", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(4, forwardSinkP4), "10.0.0.12", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(5, forwardSinkP5), "10.0.0.15", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(6, forwardSinkP6), "10.0.0.18", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(7, forwardSinkP7), "10.0.0.21", 1080);
        QueryControl.sendQueryRedirect(new QueryRedirect(8, forwardSinkP8), "10.0.0.24", 1080);

        /* 2. Migrate the query  */
        log.debug("> Send snapshot from " + currentPlacement + " to " + newPlacement);

        List<QuerySink> forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD));

        QueryMigrate qm = new QueryMigrate(9, newPlacement, 1080, Runners.pamapJoinSources, forwardSink, "10.0.0.27", 1080);
        QueryControl.sendQueryMigrate(qm, currentPlacement, 1080);
    }

    private static String toIp(String from) {
        if (from.equals("C1"))
            return "10.0.0.27";
        else if (from.equals("J2"))
            return "10.0.0.26";
        else if (from.equals("J1"))
            return "10.0.0.25";
        else
            log.error("Hardcoded migration gone wrong, unknown node: " +from);
        return "10.0.0.0";
    }

    private static class PlacementDecision {
        public int timestamp;
        public String from;
        public String to;

        public PlacementDecision(int timestamp, String from, String to) {
            this.timestamp = timestamp;
            this.from = from;
            this.to = to;
        }
    }
}
