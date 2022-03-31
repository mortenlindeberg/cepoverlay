package no.uio.ifi.dmms.cepoverlay.queryengine;

import java.io.Serializable;

public class QuerySource implements Serializable {
    private String streamName;
    private int streamId;

    public QuerySource(String streamName, int streamId) {
        this.streamName = streamName;
        this.streamId = streamId;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    @Override
    public String toString() {
        return "QuerySource{" +
                "streamName='" + streamName + '\'' +
                ", streamId=" + streamId +
                '}';
    }
}
