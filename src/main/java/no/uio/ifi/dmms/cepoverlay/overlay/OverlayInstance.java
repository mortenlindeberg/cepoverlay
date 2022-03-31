package no.uio.ifi.dmms.cepoverlay.overlay;

import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import no.uio.ifi.dmms.cepoverlay.network.ControlInput;
import no.uio.ifi.dmms.cepoverlay.network.DataInput;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.*;
import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.network.topology.PlacementModule;
import no.uio.ifi.dmms.cepoverlay.queries.ConfigReader;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Arrays.asList;

public class OverlayInstance {
    public static final int DEFAULT_MIGRATION_TIME = 500; // Note: This is the default value in extra large pamap
    public static final int DEFAULT_WINDOW_SIZE = 1000; // Note: This is the default value in extra large pamap

    private static final int INPUT_DATA_QUEUE_SIZE = 5000;

    private static final Logger log = Logger.getLogger(OverlayInstance.class.getName());
    private static final String OUTPUT_FILENAME = "output.res";

    private int queryId = 0;
    private String address;
    private int port;

    private int migrationSlowDown = 0;

    private boolean windowAware = false;
    private boolean windowWait = false;
    private boolean windowSmart = false;

    private PriorityBlockingQueue dataInputQueue;

    private ReentrantLock mutex = new ReentrantLock();
    private ReentrantLock snapshotMutex = new ReentrantLock();
    private ConcurrentHashMap<Integer, Query> queries;
    private ConcurrentHashMap<Integer, List<InputHandler>> queryInputs;
    private ConcurrentHashMap<Integer, List<StreamCallback>> streamCallbacks;
    private ConcurrentHashMap<Integer, QueryEngine> queryEngines;

    private ControlInput controlInput;
    private DataInput dataInput;
    private InputThread inputThread;
    private LateArrivalHandler lateArrivalHandler;
    private EarlyArrivalHandler earlyArrivalHandler;
    private byte[] snapshotBuf;

    /* Used for hardcoded placement (with adaptation) of the large PAMAP query */
    private ConcurrentHashMap<String, Boolean> rateMap = null;
    private String currentPlacement = "10.0.0.27";
    private AtomicLong lastPlacement = new AtomicLong(-1);
    private ReentrantLock placementLock;
    private PlacementModule placementModule;
    private boolean ongoingMigration = true; /* Note.. This is basically set to true as we are loading the query.
                                                So not exactly an ongoing migration, but close enough.. */
    private int migrationTime;
    private int windowSize;

    private int snapshotCounter = 0;

    AtomicLong lastFlush = new AtomicLong(-1);

    public OverlayInstance() {

    }

    public OverlayInstance(String address, int port) {
        log.debug("> OverlayInstance initiating " + address + " " + port+" "+System.currentTimeMillis());
        placementLock = new ReentrantLock();
        this.migrationSlowDown = ConfigReader.readMigrationSlowdown();
        this.queryId = ConfigReader.readQueryId();
        this.windowAware = ConfigReader.readWindowAware();
        this.windowWait = ConfigReader.readWindowWait();
        this.windowSmart = ConfigReader.readWindowSmart();

        this.migrationTime = ConfigReader.readMigrationTime();
        this.windowSize = ConfigReader.readWindowSize();


        if (address.compareTo("10.0.0.27") == 0) {/* Hardcoded way of finding out that we are running the extra large experiment */
            placementModule = new PlacementModule();

            if (queryId == 2 || queryId == 4) {
                rateMap = new ConcurrentHashMap(Runners.getHRRateMap());
                log.debug("> Query "+queryId+": "+rateMap.toString() + " window aware: " +windowAware);
            }
            else if (queryId == 1 || queryId == 3) {
                rateMap = new ConcurrentHashMap(Runners.getTempRateMap());
                log.debug("> Query "+queryId+": "+rateMap.toString() + " window aware: " +windowAware);
            }
            else
                log.debug("> QueryId unknown "+queryId);
        }

        this.address = address;
        this.port = port;

        log.debug("-> Warming up Siddhi engine");
        try {
            new EngineWarmUp().warmUp();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("-> Engine warm-up complete");


        dataInputQueue = new PriorityBlockingQueue(INPUT_DATA_QUEUE_SIZE);

        controlInput = new ControlInput();
        controlInput.listenForData(address, port, this);

        dataInput = new DataInput();
        dataInput.listenForData(address, port + 1, dataInputQueue);

        queryInputs = new ConcurrentHashMap<>();
        queryEngines = new ConcurrentHashMap<>();
        streamCallbacks = new ConcurrentHashMap<>();
        queries = new ConcurrentHashMap<>();
        lateArrivalHandler = new LateArrivalHandler();
        earlyArrivalHandler = new EarlyArrivalHandler(queryInputs);
        inputThread = new InputThread(dataInputQueue, queryInputs, earlyArrivalHandler, lateArrivalHandler);

        inputThread.start();
        try {
            inputThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.debug("> OverlayInstance finished..");
    }

    public OverlayInstance(Instance instance) {
        this(instance.getAddress(), instance.getControlPort());
    }

    public void handleControlRequest(ControlRequest request) throws InterruptedException {
        //log.debug("QR> " + request.getType() + " " + request.toString() + " " + request.getPayload().length);
        switch (request.getType()) {
            case QueryControl.CONTROL_NEW_QUERY:
                //log.debug("Received new query ");
                Query query = request.getQuery();
                runQuery(query);
                break;
            case QueryControl.CONTROL_MIGRATE:
                //log.debug("Received migrate message ");
                QueryMigrate qm = request.getQueryMigrate();
                sendSnapshot(qm);
                break;
            case QueryControl.CONTROL_REDIRECT:
                //log.debug("Received redirect ");
                QueryRedirect qr = request.getQueryRedirect();
                queryRedirect(qr);
                break;
            case QueryControl.CONTROL_SNAPSHOT:
                //log.debug("Received snapshot");
                Snapshot s = request.getQuerySnapshot();
                runSnapshot(s);
                break;
            case QueryControl.CONTROL_PARTIAL_SNAPSHOT:
                //log.debug("Received snapshot");
                PartialSnapshot ps = request.getQueryPartialSnapshot();
                handlePartialSnapshot(ps);
                break;
            case QueryControl.CONTROL_QUERY_STOP:
                QueryStop qs = request.getQueryStop();
                stopQuery(qs);
                break;
            case QueryControl.CONTROL_OVERLAY_STOP:
                shutdown();
                break;
            case QueryControl.ABORT_LATE_ARRIVAL:
                //log.debug("Received late arrival");
                AbortLateArrival abortLateArrivalRequest = request.getAbortLateArrival();
                abortLateArrival(abortLateArrivalRequest);
                break;
            case QueryControl.RATE_UPDATE:
                //log.debug("Received rate update");
                updateRate(request.getRateUpdate());
                break;
            case QueryControl.QUERY_MIGRATION_FINISHED:
                log.info("> Received notification of migration completion. Duration: " +(System.currentTimeMillis() - lastPlacement.get()));
                ongoingMigration = false;
                break;
            case QueryControl.WINDOW_AWARE_QUERY_MIGRATE:
                //log.debug("> Received request to initiate window aware query migration");
                WindowAwareQueryMigrate windowAwareQueryMigrate = request.getWindowAwareQueryMigrate();
                doWindowAwareQueryMigrate(windowAwareQueryMigrate);
                break;
            default:
                log.error("Overlay not sure what to do with " + request + " " + request.getType());
        }
    }



    // This method for now implements placement on the large PAMAP query. Again hardcoded but needs its own function because of complexity
    private void updateRate(RateUpdate rateUpdate) throws InterruptedException {
        /* Update the map with the update */
        rateMap.put(rateUpdate.getAddress(), rateUpdate.isMode());
        doMigration();
    }

    private boolean doMigration() throws InterruptedException {
        /* Some concern here: So we do not want to run a migration while another migration is ongoing -> crash.
        *  At the same time, we do not want to "miss" a rate update that would trigger an performance raising migration.
        *
        * So what do we do here? Well, we sleep for some time and the try again. So the concern. If there is a flood of
        * rate updates, then there will be many calls to the placement algorithm when they are all released -> overhead
        * at least if placement algorithm is costly. We could elegantly only have a "roof" that only allows suspended thread,
        * but yeah.. Never change something that kinda works.. */
        if (ongoingMigration) {
            /* Schedule a check in the future just to be sure that we do not miss an important migration then.. */
            Runnable run = () -> {
                try {
                    Thread.sleep(200);
                    doMigration();
                } catch (InterruptedException e) {
                    log.error(" Thread interrupted while waiting for ongoing migration!");
                }
            };
            new Thread(run).start();
            return false;
        }

        //log.debug("> Taking placement lock");
        placementLock.lock();
        List<String> optimalPlacements = placementModule.findOptimalPlacements(rateMap);

        if (!optimalPlacements.contains(currentPlacement)) {
            ongoingMigration = true;

            if (windowAware)
                migrateOperatorExtraWindowAware(currentPlacement, optimalPlacements.get(0));
            else
                migrateOperatorExtra(currentPlacement, optimalPlacements.get(0));

            currentPlacement = optimalPlacements.get(0);
            lastPlacement.set(System.currentTimeMillis());
            placementLock.unlock();
            //log.debug("> Releasing placement lock for migration");
            return true;
        }
        placementLock.unlock();
        //log.debug("> Releasing placement lock since no migration");
        return false;
    }

    private void doWindowAwareQueryMigrate(WindowAwareQueryMigrate windowAwareQueryMigrate) throws InterruptedException {
        log.info("> Woohooo we are ready to check the window state and then migrate query "+windowAwareQueryMigrate.getQueryId()+" to "+windowAwareQueryMigrate.getAddress());


        if (!windowSmart) {
            /* 1. Sleep while we wait for flush */
            int waits = 0;
            boolean waitForFlush = true;

            while (waitForFlush) {
                waitForFlush = waitUntilWindowReady();
                if (waitForFlush)
                    waits++;
            }
            log.info("Wait counter: " + waits);
        }
        else {
            // Hardcoded value for future window for now = 30
            int futureWindow = 30;

            /* 1. If window has not been flushed, we cannot know when next flush will be, so act as nothing is expected */
            if (lastFlush.get() == -1)  /* Value is -1 of not activated */
                Thread.sleep(futureWindow*10);
                else if (migrationCollision(futureWindow, migrationTime, lastFlush, windowSize)) /* 2. If conflicting future window near context change, migrate with the goal of minimizing distance to context change */
                    Thread.sleep(collisionFreeOpportunity(futureWindow, migrationTime, lastFlush, windowSize));
                else  /* 3. Else sleep until context change */
                    Thread.sleep(futureWindow);

        }
        /* 2. Notify the new host that it will start to receive new tuples that are not late arrivals */
        List<Integer> streamIds = new ArrayList<>();
        for (QuerySource source : windowAwareQueryMigrate.getSources()) {
            streamIds.add(source.getStreamId());
        }
        QueryControl.sendAbortLateArrival(new AbortLateArrival(streamIds), windowAwareQueryMigrate.getAddress(), windowAwareQueryMigrate.getPort());
        log.debug("> Notified about late arrivals");

        /* 3. Redirect queries to new placements */
        for (Redirect redirect : windowAwareQueryMigrate.getRedirects()) {
            /* When we create a new sink here for the query, we know that the new placement will be where we should redirect the queries to..*/
            List<QuerySink> sinks = asList(new QuerySink(redirect.getStreamId(), windowAwareQueryMigrate.getAddress(), windowAwareQueryMigrate.getPort() +1, QueryControl.SINK_TYPE_FORWARD));
            QueryControl.sendQueryRedirect(new QueryRedirect(redirect.getQueryId(), sinks), redirect.getSourceAddress(), redirect.getSourcePort());
        }
        /* 4. Migrate the query  */
        log.info("> Send snapshot from " +address+" "+port+" to " + windowAwareQueryMigrate.getAddress() +" "+windowAwareQueryMigrate.getPort());
        QueryMigrate qm = new QueryMigrate(windowAwareQueryMigrate.getQueryId(), windowAwareQueryMigrate.getAddress(), windowAwareQueryMigrate.getPort(), windowAwareQueryMigrate.getSources(), windowAwareQueryMigrate.getSinks(), windowAwareQueryMigrate.getMasterAddress(), windowAwareQueryMigrate.getPort());
        sendSnapshot(qm);
    }

    private long collisionFreeOpportunity(int futureWindow, int migrationTime, AtomicLong lastFlush, int windowSize) {
        /* Here we need to choose to do it either before or  after to the context change. We choose the option that gives the best distance. */
        long flush = lastFlush.get();

        long contextChange = System.currentTimeMillis()+futureWindow;
        /* Find the first flush following context change */
        while (flush < contextChange)
            flush += windowSize;

        /* Calculate distance if we do it before */
        flush -= windowSize;
        long beforeOpportunity = flush-migrationTime;
        long beforeDistance = contextChange - beforeOpportunity;

        /* Calculate distance if we do it after */
        long afterOpportunity = flush + windowSize;
        long afterDistance = afterOpportunity - contextChange;

        if (afterDistance > beforeDistance) {
            log.debug("-> Best opportunity is before next flush: "+System.currentTimeMillis()+" "+beforeOpportunity+" "+afterDistance+" "+beforeDistance);
            return beforeOpportunity - System.currentTimeMillis();
        }
        else {
            log.debug("-> Best opportunity is after next flush: "+System.currentTimeMillis()+" "+afterOpportunity+" "+afterDistance+" "+beforeDistance);
            return  afterOpportunity - System.currentTimeMillis();
        }
    }

    private boolean migrationCollision(int futureWindow, int migrationTime, AtomicLong lastFlush, int windowSize) {
        long flush = lastFlush.get();
        long contextChange = System.currentTimeMillis()+futureWindow;
        /* Find the first flush following context change */
        while (flush < contextChange)
            flush += windowSize;

        /* If this flush happens before the migration is completed, return true for communicating to caller that there is a collision */
        if ( (flush - contextChange) <= migrationTime) {
            log.debug("-> Avoiding collision: " +System.currentTimeMillis()+" "+ futureWindow+" " +migrationTime+" "+lastFlush+" "+windowSize);
            return true;
        }

        else return false;
    }

    private boolean waitUntilWindowReady() throws InterruptedException {

        if (lastFlush.get() == -1) return false; /* Value is -1 of not activated */

        if (windowWait)
            migrationTime = windowSize-50; /* What we achieve here is that we only migrate after a freshly flushed window */

        long now = System.currentTimeMillis();
        long sinceLastFlush = (now - lastFlush.get());

        /* Special case where */
        if (sinceLastFlush > (windowSize - migrationTime) && sinceLastFlush < (windowSize + 5)) { // The value of 5 gives a small slack so that we do not actually start a migration just about the time when a flush is somewhat delayed (< 5 ms)
            //log.debug(" > Waiting for window to flush before we start migration: "+sinceLastFlush+" "+now+" "+ lastFlush.get());
            Thread.sleep(10);
            return true;
        } else {
            //log.debug(" > Ready migrate as there is still time: "+sinceLastFlush+" > "+(windowSize - migrationTime));
            return false;
        }
    }

    /* This is only for the extra large PAMAP thingy! Hardcoded.. */
    private void migrateOperatorExtra(String currentPlacement, String newPlacement) throws InterruptedException {
        log.debug(" > Migrate Operator Extra");
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

        QueryMigrate qm = new QueryMigrate(9, newPlacement, 1080, Runners.pamapJoinSources, forwardSink, address, port);
        if (currentPlacement.equals("10.0.0.27")) { // Handle the special occasion where we are running locally the query and will take the snapshot here. No use in sending control message to self..
            sendSnapshot(qm);
        }
        else {
            QueryControl.sendQueryMigrate(qm, currentPlacement, 1080);
        }
    }

    /* This is only for the extra large window aware PAMAP thingy! Hardcoded.. */
    private void migrateOperatorExtraWindowAware(String currentPlacement, String newPlacement) throws InterruptedException {
        log.debug(" > Migrate Operator Extra with window awareness!");
        List<QuerySink> forwardSink;
        if (windowAware) forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS)); //TODO: Hardcoded!
        else forwardSink = asList(new QuerySink(34, "10.0.0.27", 1081, QueryControl.SINK_TYPE_FORWARD)); //TODO: Hardcoded!

        int rQuery = 1; //TODO: Hardcoded value
        int rStream = 26;//TODO: Hardcoded value
        List<Redirect> redirects = new ArrayList();
        List<String> rAddresses = asList("10.0.0.3", "10.0.0.6", "10.0.0.9", "10.0.0.12", "10.0.0.15", "10.0.0.18", "10.0.0.21", "10.0.0.24");

        for (int i = 0; i < rAddresses.size(); i++)
            redirects.add(new Redirect(rQuery++, rStream++, rAddresses.get(i), 1080));

        WindowAwareQueryMigrate windowAwareQueryMigrate = new WindowAwareQueryMigrate(9, newPlacement, 1080, redirects, Runners.pamapJoinSources, forwardSink, "10.0.0.27", 1080); //TODO: Hardcoded value
        QueryControl.sendWindowAwareQueryMigrate(windowAwareQueryMigrate, currentPlacement, 1080);
    }

    private void handlePartialSnapshot(PartialSnapshot ps) {
        snapshotMutex.lock();
        snapshotCounter++;

        if (snapshotBuf == null) {
            snapshotBuf = new byte[QueryControl.SNAPSHOT_FRAGMENT_LIMIT*ps.getAll()]; // Remember, this means we need to trim at the end..
        }

        int dstPos = QueryControl.SNAPSHOT_FRAGMENT_LIMIT*(ps.getNum()-1);
        //log.debug(">Handling partial snapshot! " + ps.getNum() + " of " + ps.getAll() + " received " + ps.getPartialSnapshot().length + "  bytes. Counter:  " + snapshotCounter +" writing from " +dstPos);

        System.arraycopy(ps.getPartialSnapshot(), 0, snapshotBuf, dstPos, ps.getPartialSnapshot().length);

        /* If we receive the last one, trim the buffer */
        if (ps.getAll() == ps.getNum()) {

            int newLength = (ps.getNum()*QueryControl.SNAPSHOT_FRAGMENT_LIMIT) - (QueryControl.SNAPSHOT_FRAGMENT_LIMIT - ps.getPartialSnapshot().length);
            //log.debug("> Trimming now size "+ newLength);
            byte[] trimmedBuf = new byte[newLength];
            System.arraycopy(snapshotBuf, 0, trimmedBuf, 0, trimmedBuf.length);
            snapshotBuf = trimmedBuf.clone();
        }

        if (snapshotCounter == ps.getAll()) {
            Snapshot snapshot = new Snapshot(ps.getQuery(), snapshotBuf, ps.getMasterAddress(), ps.getMasterPort());
            try {
                runSnapshot(snapshot);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            snapshotBuf = null;
            snapshotCounter = 0;
        }

        snapshotMutex.unlock();
    }

    public void runQuery(Query q) {
        String siddhiApp = q.getApp();

        log.debug("> " + port + " initializing query engine: " + q.getApp()+" with sinks: "+ q.getQuerySinks());
        List<StreamCallback> callbacks = new ArrayList<>();

        for (QuerySink sink : q.getQuerySinks()) {
            StreamCallback callback;
            switch (sink.getType()) {
                /* The special case where we want to be made aware of when the window is flushing */
                case QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS:
                    callback = new QueryFlushCallback(sink.getStreamId(), this);
                    callbacks.add(callback);
                case QueryControl.SINK_TYPE_FORWARD:
                    callback = new QueryCallbackSend(sink.getStreamId(), sink.getAddress(), sink.getPort());
                    break;
                case QueryControl.SINK_TYPE_WRITE:
                    callback = new QueryCallbackWriter(OUTPUT_FILENAME);
                    break;
                default:
                    throw new IllegalStateException("Unexpected sink type value: " + sink.getType());
            }

            callbacks.add(callback);
        }

        streamCallbacks.put(q.getQueryId(), callbacks);
        QueryEngine engine = new QueryEngine(siddhiApp, callbacks);

        for (QuerySource source : q.getQuerySources()) {
            addInputHandler(engine, source);
        }
        inputThread.update(queryInputs);

        queryEngines.put(q.getQueryId(), engine);
        queries.put(q.getQueryId(), q);

        new Thread(engine).start();
        log.debug("-> " + port + " ..started");

        if (q.getQueryId() == 10) //TODO: This hardcoded value is only for PAMAP experiment - must be set dynamically to support other experiments as welL!
            ongoingMigration = false;
    }

    private void addInputHandler(QueryEngine engine, QuerySource source) {
        List<InputHandler> inputHandlers = queryInputs.get(source.getStreamName());
        InputHandler inputHandler = engine.getInputHandler(source.getStreamName());
        if (inputHandlers == null)
            inputHandlers = Arrays.asList(inputHandler);
        else
            inputHandlers.add(inputHandler);
        queryInputs.put(source.getStreamId(), inputHandlers);
    }

    private void queryRedirect(QueryRedirect qr) {
        //log.debug("> " + port + " Redirecting query " + qr.getQueryId() + " towards " + qr.getQuerySinks().get(0).getAddress() + " " + qr.getQuerySinks().get(0).getPort());

        int index = 0;
        for (StreamCallback c : streamCallbacks.get(qr.getQueryId())) {
            for (int i = 0; i < qr.getQuerySinks().size(); i++) {
                QuerySink sink = qr.getQuerySinks().get(index);

                if (c instanceof QueryCallbackSend)
                    ((QueryCallbackSend) c).redirect(sink.getStreamId(), sink.getAddress(), sink.getPort());

                //log.debug("> " + port + " redirect to sink " + sink.getAddress() + " " + sink.getPort() + " " + sink.getStreamId() +" for stream ID: " + c.getStreamId());
            }
        }
        //log.debug("> " + port + " .. redirected");
    }


    private void sendSnapshot(QueryMigrate queryMigrate) throws InterruptedException {
        mutex.lock();
        /*
        log.debug(System.currentTimeMillis() + " -> Create/Send snapshot of " + queryMigrate.getQueryId() + " from " + port + " " + address + " to " + queryMigrate.getAddress() + " " + queryMigrate.getPort() + ":" +
                " Input Queue: " + dataInputQueue.size() +
                ". Processed: " + inputThread.getCount() +
                ". Late Arrivals: " + lateArrivalHandler.getState() +
                ". Early Arrival queue: " + earlyArrivalHandler.getQueueSize() +
                ". Early Arrival processed: " + earlyArrivalHandler.getCount());
                */

        Query q = queries.get(queryMigrate.getQueryId());
        if (q == null) {
            log.warn("Asked to migrate a snapshot (sendSnapshot) which is not there.. could the prediction / placement algo be out of sync?");
            QueryControl.sendQueryMigrationFinished(new QueryMigrationFinished(1), queryMigrate.getMasterAddress(), queryMigrate.getMasterPort());
            mutex.unlock();
            return;
        }
        /* 0. Get the relevant query engine and update sources */
        QueryEngine engine = queryEngines.get(q.getQueryId());
        q.setQuerySources(queryMigrate.getSources());
        q.setQuerySinks(queryMigrate.getSinks());

        /* 1. Make sure late comers are sent to the new destination */
        for (QuerySource s : q.getQuerySources()) {
            //log.debug("-> Handling late arrival " + s.getStreamId() + " to input handler for transmission to " + queryMigrate.getAddress() + " " + queryMigrate.getPort() + " (dataport: " + (queryMigrate.getPort() + 1) + ")");
            lateArrivalHandler.updateLateArrivalMap(s.getStreamId(), new QuerySink(s.getStreamId(), queryMigrate.getAddress(), queryMigrate.getPort() + 1, -1));
        }

        /* 2. Remove the queryInput handlers */
        for (QuerySource s : queryMigrate.getSources()) {
            queryInputs.remove(s.getStreamId());
        }
        inputThread.update(queryInputs);

        /* 3.  Transfer the snapshot */
        Snapshot snapshot = new Snapshot(q, engine.getSnapshot(), queryMigrate.getMasterAddress(), queryMigrate.getMasterPort());
        QueryControl.sendSnapshot(snapshot, queryMigrate.getAddress(), queryMigrate.getPort());

        /* 4. Stop the query engine */
        queryEngines.get(queryMigrate.getQueryId()).stop();

        /* 5. Clean up the queries data structure */
        queries.remove(queryMigrate.getQueryId());

        log.debug(System.currentTimeMillis() + " -> " + port + " .. snapshot sent to " + queryMigrate.getAddress() + " " + queryMigrate.getPort() + " size " + snapshot.getSize());
        mutex.unlock();
    }

    private void runSnapshot(Snapshot s) throws InterruptedException {
        mutex.lock();
        log.debug(System.currentTimeMillis() + " -> Run snapshot of " + s.getQuery().getQueryId() + " on " + port + " " + address);
        /* Add migration slow down for experimentation*/
        if (migrationSlowDown > 0)
            Thread.sleep(migrationSlowDown);

        Query q = s.getQuery();
        queries.put(q.getQueryId(), q);

        byte[] snapshot = s.getSnapshot();

        List<StreamCallback> callbacks = new ArrayList<>();
        List<QuerySink> sinks = q.getQuerySinks();
        if (sinks != null) {
            for (QuerySink sink : sinks) {
                /* Add one extra callback if we have flush aware migration */
                if (sink.getType() == QueryControl.SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS) {
                    //log.debug("> Note: Migrated a query with flush awareness on sink with stream ID " +sink.getStreamId());
                    callbacks.add(new QueryFlushCallback(sink.getStreamId(), this));
                }
                callbacks.add(new QueryCallbackSend(sink.getStreamId(), sink.getAddress(), sink.getPort())); //Note that we here implicitly state that migration is only supported for forwarding sinks.. but that makes sense..
            }
            streamCallbacks.put(q.getQueryId(), callbacks);
        }

        /* Create new engine for the query */
        QueryEngine engine = new QueryEngine(q.getApp(), snapshot, callbacks);
        queryEngines.put(q.getQueryId(), engine);

        /* Now start, and wait for engine to actually be up and running! */
        Thread engineThread = new Thread(engine);
        synchronized (engineThread) {
            engineThread.start();
            engineThread.wait();
        }

        inputThread.pause();
        Thread.sleep(50); // Avoid that the engine looses tuples because it is not ready..

        Tuple t = earlyArrivalHandler.pollTuple();
        while (t != null) {
            dataInputQueue.add(t);
            t = earlyArrivalHandler.pollTuple();
        }

        /* Enable that the input thread route new tuples to the engine */
        for (QuerySource source : q.getQuerySources()) {
            addInputHandler(engine, source);
        }
        inputThread.update(queryInputs);

        inputThread.unpause();

        mutex.unlock();
        log.debug(System.currentTimeMillis() + " -> " + port + " .. migrated snapshot (operator) started, queue " + dataInputQueue.size());
        QueryControl.sendQueryMigrationFinished(new QueryMigrationFinished(1), s.getMasterAddress(), s.getMasterPort());
    }

    private String printQueue() {
        String out = "Queue: ";

        for (Object o : dataInputQueue) {
            out += ((Tuple) o).getTuple()[0] + ",";
        }

        return out;
    }

    private void abortLateArrival(AbortLateArrival abortLateArrivalRequest) {
        /* Stop the input thread from treating tuples to this query as late arrivals */
        //log.debug(System.currentTimeMillis() + " -> Removing late arrival handler for stream ID" + abortLateArrivalRequest.getStreamId());
        List<Integer> streamIds = abortLateArrivalRequest.getStreamIds();
        for (int streamId : streamIds)
            lateArrivalHandler.removeSink(streamId);
    }

    private void stopQuery(QueryStop qs) {
        log.info("> " + port + " Stopping query engine with queue " + dataInputQueue.size());

        /* 1. Stop the input thread and remove the queryInput handler */
        queryInputs.remove(qs.getQueryId());

        /* 2. Stop the query engine */
        queryEngines.get(qs.getQueryId()).shutdown();

        /* 2. Clean up the queries data structure */
        queries.remove(qs.getQueryId());

        Iterator iteratorValues = dataInputQueue.iterator();
        while (iteratorValues.hasNext()) {
            log.info(">" + port + " " + iteratorValues.next());
        }
        log.info("-> " + port + " .. query engine stopped");
    }


    public void shutdown() {
        log.info("> " + port + " Overlay shutting down: " + inputThread.getCount());
        inputThread.shutdown();
        controlInput.shutdown();
        dataInput.shutdown();
        earlyArrivalHandler.shutdown();

        for (QueryEngine e : queryEngines.values())
            e.stop();

        for (Query q : queries.values())
            for (StreamCallback c : streamCallbacks.get(q.getQueryId())) {
                if (c instanceof QueryCallbackWriter)
                    ((QueryCallbackWriter) c).stop();
                else if (c instanceof QueryCallbackSend)
                    ((QueryCallbackSend) c).stop();
            }
    }

    public void flushJustMade(int streamId) {
        long now = System.currentTimeMillis();
        lastFlush.set(now);
    }
}
