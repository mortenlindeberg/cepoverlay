package no.uio.ifi.dmms.cepoverlay.source;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.ControlInput;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlRequest;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.SourceRedirect;
import no.uio.ifi.dmms.cepoverlay.prediction.Adapter;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class SimpleSourceThread extends Thread {
    static final Logger log = Logger.getLogger(SimpleSourceThread.class.getName());
    private ControlInput controlInput;
    private String address;
    private int port;

    private String sendAddress;
    private int sendPort;
    private int streamId;
    private int sleep;
    private SourceThread src;
    private volatile int number;
    private long start = 0;

    protected Adapter adapter = null;
    protected SourceFilter sourceFilter = null;


    public SimpleSourceThread(String address, int port, String sendAddress, int sendPort, int streamId, int sleep, SourceFilter sourceFilter) {
        this.address = address;
        this.port = port;
        this.sendAddress = sendAddress;
        this.sendPort = sendPort;
        this.streamId = streamId;
        this.sleep = sleep;
        this.start = Runners.readStartTime(Main.START_FILENAME);
        this.sourceFilter = sourceFilter;
    }

    public void addAdaptation(Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void run() {
        log.debug("> Start: " +start);

        controlInput = new ControlInput();
        controlInput.listenForData(address, port, this);

        src = new SourceThread(sendAddress, sendPort, streamId);
        log.debug("-> Connecting to "+sendAddress+" "+sendPort+" for stream "+streamId+"  at time "+(System.currentTimeMillis() - start));
        src.connect();
        number = 0;
        log.debug("-> Start sending tuples to " +sendPort);


        while (number < Integer.MAX_VALUE) {
            Object[] tuple = getNextTuple(streamId, System.currentTimeMillis() - start);
            if (tuple == null) {
                log.debug("> Reached end of file..");
                break;
            }
            if (streamId < 3) {
                if (streamId == 1) {
                    if (sendPort == 1077 || sourceFilter.allowed(tuple))
                        src.send(tuple);
                }
                else
                    src.send(tuple);

                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else
                src.send(tuple);
        }

        log.debug("> Finished sending tuples");
    }

    public Object[] getNextTuple(int stream, long timestamp) {
        if (stream == 1) {
            return new Object[]{timestamp, 1, Double.valueOf(30)};
        }
        else if (stream == 2) {
            return new Object[]{timestamp, 1, false};
        }
        else
            log.error("Unknown stream called for in getNextTuple()!");
        return null;
    }

    public void redirect(String address, int port) {
        src.redirect(address, port);
        log.debug("> Source redirect to "+address+" "+port);
    }

    public void shutdown() {
        number = Integer.MAX_VALUE;
        src.shutdown();
    }

    public void handleControlRequest(ControlRequest request) {
        log.debug("> Source handles control request type:" +request.getType());
        switch (request.getType()) {
            case QueryControl.SOURCE_REDIRECT:
                SourceRedirect sourceRedirect = request.getSourceRedirect();
                redirect(sourceRedirect.getAddress(), sourceRedirect.getPort());
                break;
            case QueryControl.CONTROL_OVERLAY_STOP:
                shutdown();
                break;
            case QueryControl.QUERY_MIGRATION_FINISHED:
                log.error("Source notified that migration completed..");
                adapter.migrationFinished();
                break;
            default:
                log.error("Source not sure what to do with " + request + " " + request.getType());
        }
    }

    public static BufferedReader openFile(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return reader;
    }
}