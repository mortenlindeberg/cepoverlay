package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AbortLateArrival implements Serializable {
    private List<Integer> streamIds = new ArrayList<>();

    public AbortLateArrival(int streamId) {
        this.streamIds.add(streamId);
    }

    public AbortLateArrival(List<Integer> streamIds) {
        this.streamIds = streamIds;
    }

    public int getStreamId() {
        return streamIds.get(0);
    }

    public void setStreamId(int streamId) {
        this.streamIds.add(streamId);
    }

    public void addStreamIds(List<Integer> streamIds) { this.streamIds = streamIds;}

    public List<Integer> getStreamIds() { return streamIds; }
}
