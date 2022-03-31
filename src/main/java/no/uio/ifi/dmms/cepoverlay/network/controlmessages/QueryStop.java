package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class QueryStop implements Serializable {
    private int queryId;

    public QueryStop(int queryId) {
        this.queryId = queryId;
    }

    public int getQueryId() {
        return queryId;
    }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
    }
}
