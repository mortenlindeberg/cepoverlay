package no.uio.ifi.dmms.cepoverlay.overlay;

import io.siddhi.core.event.Event;
import io.siddhi.core.stream.output.StreamCallback;

public class QueryFlushCallback extends StreamCallback {
    private int streamId;
    private OverlayInstance instance;
    public QueryFlushCallback(int streamId, OverlayInstance instance) {
        this.streamId = streamId;
        this.instance = instance;
    }

    @Override
    public void receive(Event[] events) {
        instance.flushJustMade(streamId);
    }
}
