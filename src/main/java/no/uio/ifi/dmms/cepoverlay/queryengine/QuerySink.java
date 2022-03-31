package no.uio.ifi.dmms.cepoverlay.queryengine;

import java.io.Serializable;

public class QuerySink implements Serializable {
    private int streamId;
    private String address;
    private int port;
    private int type;

    public QuerySink(int streamId, String address, int port, int type) {
        this.streamId = streamId;
        this.address = address;
        this.port = port;
        this.type = type;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "QuerySink{" +
                "streamId=" + streamId +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", type=" + type +
                '}';
    }
}
