package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.siddhi.core.stream.input.InputHandler;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class EarlyArrivalHandler extends Thread {
    private static final Logger log = Logger.getLogger(EarlyArrivalHandler.class.getName());
    private ConcurrentHashMap<Integer, List<InputHandler>> queryInputs;
    private PriorityBlockingQueue<Tuple> tupleQueue;
    private volatile boolean keepRunning = true;
    private int count;

    public EarlyArrivalHandler(ConcurrentHashMap<Integer, List<InputHandler>> queryInputs) {
        this.queryInputs = queryInputs;
        tupleQueue = new PriorityBlockingQueue<>(1000);
    }

    public void run() {
        List<InputHandler> inputHandlers;
        Tuple tuple = null;
        try {
            while (keepRunning) {
                tuple = tupleQueue.take();
                inputHandlers = queryInputs.get(tuple.getStreamId());
                /* We need to wait until there is an input handler for this tuple */
                while(inputHandlers == null)
                    synchronized (this) {
                        wait();
                }

                /* Now that there is a matching input handler, then send the tuple there */
                for (InputHandler inputHandler : inputHandlers) {
                    Object[] t = tuple.getTuple();
                    try {
                        //log.debug(System.currentTimeMillis()+" > Sending early arrival: " +tuple.getTuple()[0]);
                        inputHandler.send(t);
                        count++;
                    } catch (InterruptedException e) {
                        Thread.sleep(5); // Need to handle the case (happens often) that the engine throws an exception because it is not ready for processing..
                        this.handle(tuple);
                    }
                } // End of send loop
            }
        } catch (InterruptedException e) {
            log.error("-> Tuple caused error in EarlyArrivalThread: " + tuple.getStreamId() + " " + tuple.getTuple()[0]);
        }
    }

    public void handle(Tuple tuple) {
        //log.debug(System.currentTimeMillis() + " -> Early arrival: " + tuple.getStreamId() +" " +tuple.getTuple()[0]);
        tupleQueue.add(tuple);
    }

    public void shutdown() {
        log.debug(System.currentTimeMillis() +" -> Early arrival thread shutting down");
        keepRunning = false;
    }

    public int getQueueSize() {
        return tupleQueue.size();
    }
    public int getCount() {
        return count;
    }

    public Tuple pollTuple() {
        return tupleQueue.poll();
    }

    public void update(ConcurrentHashMap<Integer, List<InputHandler>> queryInputs) {
        synchronized (this) {
            this.queryInputs = queryInputs;
            notify();
        }
    }
}
