package no.uio.ifi.dmms.cepoverlay.queryengine;

import java.io.Serializable;
import java.util.Arrays;

public class Tuple implements Comparable<Tuple>, Serializable {
    private int streamId;
    private Object[] tuple;

    public Tuple(int streamId, Object[] tuple) {
        this.streamId = streamId;
        this.tuple = tuple;
    }

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

    @Override
    public String toString() {
        return "Tuple{" +
                "streamId=" + streamId +
                ", tuple=" + Arrays.toString(tuple) +
                '}';
    }

    @Override
    public int compareTo(Tuple tuple) {
        Object[] thisTuple = getTuple();
        long otherTimestamp = (long)tuple.getTuple()[0];
        long thisTimestamp = (long)thisTuple[0];

        if (thisTimestamp == otherTimestamp) return 0;
        else if (thisTimestamp > otherTimestamp) return 1;
        else return -1;
    }
}
