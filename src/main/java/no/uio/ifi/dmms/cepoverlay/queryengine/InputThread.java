package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.siddhi.core.stream.input.InputHandler;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class InputThread extends Thread {
    private static final Logger log = Logger.getLogger(InputThread.class.getName());
    private BlockingQueue<Tuple> tuples;
    private ConcurrentHashMap<Integer, List<InputHandler>> queryInputs;
    private volatile boolean keepRunning = true;

    private volatile boolean pause = false;

    private int count;

    private LateArrivalHandler lateArrivalHandler;
    private final EarlyArrivalHandler earlyArrivalHandler;

    public InputThread(PriorityBlockingQueue tuples, ConcurrentHashMap<Integer, List<InputHandler>> queryInputs, EarlyArrivalHandler earlyArrivalHandler, LateArrivalHandler lateArrivalHandler) {
        this.tuples = tuples;
        this.queryInputs = queryInputs;
        this.earlyArrivalHandler = earlyArrivalHandler;
        this.lateArrivalHandler = lateArrivalHandler;
    }

    @Override
    public void run() {
        List<InputHandler> inputHandlers;
        Tuple tuple = null;
        try {
            while (keepRunning) {
                synchronized (this) {
                    if (pause)
                        wait();
                }
                tuple = tuples.take(); /* Block and wait until there is a tuple in the queue */

                /* Check if we the tuple should go to one ore more queries */
                inputHandlers = queryInputs.get(tuple.getStreamId());
                if (inputHandlers != null) {
                    /* Send tuple to all relevant destinations */
                    for (InputHandler inputHandler : inputHandlers) {
                        Object[] t = tuple.getTuple();

                        try {
                            //log.debug(System.currentTimeMillis()+"->"+tuple.toString());
                            inputHandler.send(t);
                            count++;
                        } catch (InterruptedException e) {
                            log.warn("-> Lost tuple as engine was not yet ready! "+tuple.getStreamId() + " " + tuple.getTuple()[0]);
                            earlyArrivalHandler.handle(tuple);
                        }
                    } // End of send loop
                }/* If no engine wants it, check to see if the tuple is a late arrival */

                else if (lateArrivalHandler.handle(tuple)) { /* DO NOTHING */}

                /* Okay, this tuple is likely an early arrival*/
                else
                    earlyArrivalHandler.handle(tuple);
            }
        } catch (InterruptedException e) {
            /* Interrupted exception gets thrown when the thread are awaken and engine is not running */
            log.error("-> Tuple caused error in InputThread: " + tuple.getStreamId() + " " + tuple.getTuple()[0]);
            e.printStackTrace();
        }
        log.debug("> InputThread closing. Sent: " + count);
    }

    public void pause() {
        log.debug(System.currentTimeMillis()+"> input thread pausing");
        pause = true;
    }

    public void unpause() {
        log.debug(System.currentTimeMillis()+"> input thread un-pausing");
        synchronized (this) {
        pause = false;
            notifyAll();
        }
    }

    public void shutdown() {
        log.debug("> Input thread initiating shutdown " + count);
        keepRunning = false;
    }

    public int getCount() {
        return count;
    }

    public void update(ConcurrentHashMap<Integer, List<InputHandler>> queryInputs) {
        this.queryInputs = queryInputs; //TODO: Check if this is really needed..
    }
}
