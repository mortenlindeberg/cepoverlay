package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class Redirect implements Serializable {

    private int queryId;
    private int streamId;
    private String sourceAddress;
    private int sourcePort;

    public Redirect(int queryId, int streamId, String sourceAddress, int sourcePort) {
        this.queryId = queryId;
        this.streamId = streamId;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
    }

    public int getQueryId() {
        return queryId;
    }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    @Override
    public String toString() {
        return "Redirect{" +
                "queryId=" + queryId +
                ", streamId=" + streamId +
                ", sourceAddress='" + sourceAddress + '\'' +
                ", sourcePort=" + sourcePort +
                '}';
    }

}