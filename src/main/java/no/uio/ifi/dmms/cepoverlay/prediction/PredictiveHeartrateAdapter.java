package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.AbortLateArrival;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryMigrate;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.QueryRedirect;
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

public class PredictiveHeartrateAdapter implements Adapter {
    private static final Logger log = Logger.getLogger(PredictiveHeartrateAdapter.class.getName());
    private BufferedWriter writer;
    private DerivativeBasedPrediction dbp;
    private long start;

    public PredictiveHeartrateAdapter(String filename, boolean modeA) {
        log.debug("> Initializing PredictiveHeartAdapter callback in mode A: "+modeA);
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
        log.debug("> Adapt from mode: "+modeA);
        QuerySource aFSource = new QuerySource("ActivityStream", 10);
        QuerySource hFSource = new QuerySource("HeartRateStream", 11);

        List<QuerySource> sources = asList(aFSource, hFSource);
        List<QuerySink> joinSink = asList(new QuerySink(12, "10.0.0.3", 1081, QueryControl.SINK_TYPE_FORWARD));


        /* Move from A1 to B1 */
        if (modeA) {
            log.debug(System.currentTimeMillis()+" -> Query Adapt from node A to node B");
            /* 0. Notify B1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(10), "10.0.0.2", 1080);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(11), "10.0.0.2", 1080);

            /* 1. Redirect query 1 to B1 */
            QuerySink actSink = new QuerySink(10,"10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(1, asList(actSink)), "10.0.0.1", 1080);
            log.debug("> QueryRedirect: Q1 at A to sink 8 10.0.0.2");

            /* 1. Redirect query 2 to A1 */
            QuerySink heartSink = new QuerySink(11,"10.0.0.2", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(2, asList(heartSink)), "10.0.0.2", 1080);
            log.debug("> QueryRedirect: Q2 at B to sink 8 10.0.0.2");

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(3, "10.0.0.2", 1080, sources, joinSink, "10.0.0.1", 1080),  "10.0.0.1", 1080);
            log.debug("> Send snapshot to 10.0.0.2 from 10.0.0.1");

        }
        else /* Move from B1 to A1 */ {
            log.debug(System.currentTimeMillis()+" -> Query Adapt from node B to node A");
            /* 0. Notify A1 that it will start to receive new tuples that are not late arrivals */
            QueryControl.sendAbortLateArrival(new AbortLateArrival(10),  "10.0.0.1", 1080);
            QueryControl.sendAbortLateArrival(new AbortLateArrival(11), "10.0.0.1", 1080);

            /* 1. Redirect query 1 to B1 */
            QuerySink actSink = new QuerySink(10,"10.0.0.1", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(1, asList(actSink)), "10.0.0.1", 1080);
            log.debug("> QueryRedirect: Q1 at A to sink 8 10.0.0.1");

            /* 1. Redirect query 2 to A1 */
            QuerySink heartSink = new QuerySink(11,"10.0.0.1", 1081, QueryControl.SINK_TYPE_FORWARD);
            QueryControl.sendQueryRedirect(new QueryRedirect(2, asList(heartSink)), "10.0.0.2", 1080);
            log.debug("> QueryRedirect: Q2 at B to sink 9 10.0.0.1");

            /* 2. Migrate the query engine  */
            QueryControl.sendQueryMigrate(new QueryMigrate(3, "10.0.0.1", 1080, sources, joinSink, "10.0.0.1", 1080),  "10.0.0.2", 1080);
            log.debug("> Send snapshot to 10.0.0.1 from 10.0.0.2");
        }
    }

    @Override
    public void analyze(Object[] tuple) {
        try {
            long now = System.currentTimeMillis() - start;
            //log.debug("Analyzing "+now+" "+ tuple[2]);
            String adaptString = null;
            try {
                adaptString = dbp.addData(now, (double)tuple[2], Main.HEART_LIMIT);
            } catch (Exception e) {
                log.debug("Error:  " +e.getMessage());
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
    @Override
    public void endStream() {
        // Not in use
    }
}
