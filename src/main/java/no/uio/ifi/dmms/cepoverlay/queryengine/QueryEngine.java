package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;

import java.util.List;

public class QueryEngine implements Runnable {
    private SiddhiAppRuntime siddhiAppRuntime;

    public QueryEngine(String app, List<StreamCallback> callbacks) {
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(app);

        for (StreamCallback callback : callbacks) {
            siddhiAppRuntime.addCallback("OutputStream", callback);
        }
    }

    public QueryEngine(String app, byte[] snapshot, List<StreamCallback> callbacks) {
        this(app, callbacks);

        try {
            siddhiAppRuntime.restore(snapshot);
        } catch (CannotRestoreSiddhiAppStateException e) {
            e.printStackTrace();
        }
    }

    public InputHandler getInputHandler(String streamName) {
        return siddhiAppRuntime.getInputHandler(streamName);
    }

    @Override
    public void run() {
        synchronized (this) {
            siddhiAppRuntime.start();
            notify();
        }
    }

    public byte[] getSnapshot() {
        return siddhiAppRuntime.snapshot();
    }

    public void stop() {
        siddhiAppRuntime.shutdown();
    }

    public void shutdown() {
        siddhiAppRuntime.shutdown();
        this.stop();
    }
}
