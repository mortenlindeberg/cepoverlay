package no.uio.ifi.dmms.cepoverlay.network.datamessages;

public class DataRequest {
    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public Object[] getTuple() {
        return tuple;
    }

    public void setTuple(Object[] tuple) {
        this.tuple = tuple;
    }

    private int streamId;
    private Object[] tuple;
}
