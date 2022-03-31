package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.AbortLateArrival;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryMigrate;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryRedirect;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.SourceRedirect;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

public class PredictiveLoadAdapter implements Adapter {
    private static final Logger log = Logger.getLogger(PredictiveLoadAdapter.class.getName());

    private BufferedWriter writer;
    private DerivativeBasedPrediction dbp;
    private long start;

    public PredictiveLoadAdapter(String filename, boolean modeA) {
        log.debug("> Initializing PredictiveLoadAdapter callback");
        start = Runners.readStartTime(Main.START_FILENAME);
        dbp = new DerivativeBasedPrediction(this, modeA);
        try {
            writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void adaptCallback(boolean modeA) throws InterruptedException {
        QuerySource tempSource = new QuerySource("temperatureStream", 1);
        QuerySource loadSource = new QuerySource("loadStream", 2);
        List<QuerySource> sources = asList(tempSource, loadSource);
        List<QuerySink> sinks = asList(new QuerySink(4, "10.0.0.4", 1081, QueryControl.SINK_TYPE_FORWARD));

        /* Move from B1 to A1 */
        if (modeA) {
            log.debug(System.currentTimeMillis()+" -> Query Adapt from node B to node A");
            /* 0. Notify A1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(1),  "10.0.0.2", 1080);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(2), "10.0.0.2", 1080);

            /* 1. Redirect sources to B1 */
            QuerySink newSink = new QuerySink(1,"10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(0, asList(newSink)), "10.0.0.2", 1080);
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.2", 1081), "10.0.0.3", 2081);

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "10.0.0.2", 1080, sources, sinks, "10.0.0.1", 1080),  "10.0.0.3", 1080);
        }
        else /* Move from A1 to B1 */ {
            log.debug(System.currentTimeMillis()+" -> Query Adapt from node A to node B");
            /* 0. Notify B1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(1), "10.0.0.3", 1080);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(2), "10.0.0.3", 1080);

            /* 1. Redirect sources to B1 */
            QuerySink newSink = new QuerySink(1,"10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(0, asList(newSink)), "10.0.0.2", 1080);
            QueryControl.sendSourceRedirect(new SourceRedirect("10.0.0.3", 1081), "10.0.0.2", 2081);

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(1, "10.0.0.3", 1080, sources, sinks, "10.0.0.1", 1080),  "10.0.0.2", 1080);
        }
    }

    @Override
    public void analyze(Object[] tuple) {
        try {
            // timestamp long, trend double, load double
            long now = System.currentTimeMillis() - start;
            String adaptString = null;
            try {
                adaptString = dbp.addData(now, (double)tuple[2], Main.LOAD_LIMIT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            writer.write(adaptString+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void migrationFinished() {
        // Not in use
    }

    public void endStream() {
        // Not in use
    }
}
