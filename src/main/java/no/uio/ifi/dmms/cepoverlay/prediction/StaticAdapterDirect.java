package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.AbortLateArrival;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryMigrate;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.SourceRedirect;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.util.Arrays.asList;

public class StaticAdapterDirect implements Adapter {
    private static final Logger log = Logger.getLogger(StaticAdapterDirect.class.getName());

    private BufferedWriter writer;
    private boolean modeA = false;
    private long start;
    private boolean ongoingMigration = false;

    public StaticAdapterDirect(String filename) {
        log.debug("> Initializing QueryCallbackStaticAdapter callback: " + filename);
        start = Runners.readStartTime(Main.START_FILENAME);
        try {
            writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void analyze( Object[] tuple ) {
        try {
            String tStr = new BigDecimal((double) (System.currentTimeMillis() - start) / 1000).setScale(5, RoundingMode.HALF_EVEN).toPlainString();
            double value = (double) tuple[2];

            if (value >= Main.LIMIT) {
                if (!modeA) {
                    modeA = true;
                    adaptCallback(true);
                    writer.write(tStr + " " + modeA + " " + value + "\n");
                    writer.write("set arrow from (" + tStr + "),0 to (" + tStr + "),100\n");
                }
            } else {
                if (modeA) {
                    modeA = false;
                    adaptCallback(false);
                    writer.write(tStr + " " + modeA + " " + value + "\n");
                    writer.write("set arrow from (" + tStr + "),0 to (" + tStr + "),100\n");
                }
            }

            writer.flush();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void endStream() {
        // Not in use
    }

    /* Note: We do not in prototype have a real placement and distribution component, so this function is hardcoded for now */
    public void adaptCallback(boolean modeA) throws InterruptedException {
        if (ongoingMigration) {
            log.debug("> avoiding migration due to ongoing..");
            return;
        }
        log.debug("> Migration started, will not allow any others to start until finished..");
        ongoingMigration = true;

        QuerySource leftSource = new QuerySource("LeftStream", 1);
        QuerySource rightSource = new QuerySource("RightStream", 2);
        List<QuerySource> sources = asList(new QuerySource[]{leftSource, rightSource});
        List<QuerySink> sinks = asList(new QuerySink(3, "10.0.0.6", 1087, QueryControl.SINK_TYPE_FORWARD));


        /* Move from E_2 to E_1 */
        if (modeA) {
            log.debug(System.currentTimeMillis() + " -> Query Adapt from node B to node A");
            /* 0. Notify E_1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(1), "10.0.0.3", 1080);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(2), "10.0.0.3", 1080);


            /* 1. Redirect sources to E_1 */
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.3", 1081), "10.0.0.1", 2080);
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.3", 1081), "10.0.0.2", 2081);

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "10.0.0.3", 1080, sources, sinks, "10.0.0.1", 2080), "10.0.0.4", 1082); //TODO: Check that master address/port is correct for this
        } else /* Move from E_1 to E_2 */ {
            log.debug(System.currentTimeMillis() + " -> Query Adapt from node A to node B");
            /* 0. Notify E_1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(1), "10.0.0.4", 1082);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(2), "10.0.0.4", 1082);

            /* 1. Redirect sources to P1 */
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.4", 1083), "10.0.0.1", 2080);
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.4", 1083), "10.0.0.2", 2081);

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "10.0.0.4", 1082, sources, sinks, "10.0.0.1", 2080), "10.0.0.3", 1080); //TODO: Check that master address/port is correct for this
        }
    }

    public void migrationFinished() {
        log.debug(" > Enabling migration again..");
        ongoingMigration = false;
    }
}