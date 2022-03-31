package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryStop;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.Redirect;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.WindowAwareQueryMigrate;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static no.uio.ifi.dmms.cepoverlay.queries.PAMAPQueries.*;

public class WindowAwareMigrationTest {

    @Test
    public void testWindowAware() throws InterruptedException, IOException {
        windowAwareMigrateQuery(true, activityJoinApp);
    }

    @Test
    public void testNonWindowAware() throws InterruptedException, IOException {
        windowAwareMigrateQuery(false, activityJoinApp);
    }

    @Test
    public void testWindowAwareExt() throws InterruptedException, IOException {
        windowAwareMigrateQuery(true, activityJoinAppExt);
    }

    @Test
    public void testNonWindowAwareExt() throws InterruptedException, IOException {
        windowAwareMigrateQuery(false, activityJoinAppExt);
    }


    public void windowAwareMigrateQuery(boolean aware, String migrateQuery) throws InterruptedException {
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

        List<QuerySink> joinSinkTemp;
        if (aware) joinSinkTemp = asList(new QuerySink(12, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS));
        else joinSinkTemp = asList(new QuerySink(12, "localhost", 1085, QueryControl.SINK_TYPE_FORWARD));

        QuerySource hPJSource = new QuerySource("tempSource", 10);
        QuerySource aPJSource = new QuerySource("actSource", 11);
        List<QuerySource> joinSources = asList(hPJSource, aPJSource);
        Query joinQuery = new Query(3, migrateQuery, joinSources, joinSinkTemp);
        QueryControl.sendQuery(joinQuery, "localhost", 1084);


        List<QuerySource> postJoinSource = asList(new QuerySource("postJoinSource", 12));
        List<QuerySink> writeSink = asList(new QuerySink(13, null, -1, QueryControl.SINK_TYPE_WRITE));
        Query postJoinQuery = new Query(4, activityPostJoinApp, postJoinSource, writeSink);
        QueryControl.sendQuery(postJoinQuery, "localhost", 1084);

        Thread.sleep(500);

        /* Source stream 1, the temperature readings in each city */
        new PAMAPSource("localhost", 2080, "localhost", 1081, 8, 1).start();
        new PAMAPSource("localhost", 2082, "localhost", 1083, 9, 1).start();

        /* Let the experiment run for 10 seconds before we migrate w window awareness */
        Thread.sleep(10000);

        List<Redirect> redirects = asList(new Redirect(1, 10, "localhost", 1080),new Redirect(2, 11, "localhost", 1082));
        for (int i = 0; i < 20; i++) {
            /* Migrate join, query 3 from C to A */

            WindowAwareQueryMigrate windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, "localhost", 1080, redirects, joinSources, joinSinkTemp, "localhost", 1084);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, "localhost", 1084);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

            /* Migrate join, query 3 from A to B */
            windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, "localhost", 1082, redirects, joinSources, joinSinkTemp, "localhost", 1084);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, "localhost", 1080);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

            /* Migrate join, query 3 from B to C */
            windowAwareQueryMigrate = new WindowAwareQueryMigrate(3, "localhost", 1084, redirects, joinSources, joinSinkTemp, "localhost", 1084);
            QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, "localhost", 1082);

            /* Let the experiment run for 5.1 seconds post migration. */
            Thread.sleep(5100);

        }

        /* Stop the source threads */
        QueryControl.sendOverlayStop("localhost", 2080);
        QueryControl.sendOverlayStop("localhost", 2082);

        Thread.sleep(1000);

        /* Stop the queries */
        QueryControl.sendQueryStop(new QueryStop(1), "localhost", 1080);
        QueryControl.sendQueryStop(new QueryStop(2), "localhost", 1082);
        QueryControl.sendQueryStop(new QueryStop(3), "localhost", 1080);
        Thread.sleep(1000);

        /* Stop the overlay nodes */
        QueryControl.sendOverlayStop("localhost", 1080);
        QueryControl.sendOverlayStop("localhost", 1082);
        QueryControl.sendOverlayStop("localhost", 1084);
        Thread.sleep(2000);
    }
}
