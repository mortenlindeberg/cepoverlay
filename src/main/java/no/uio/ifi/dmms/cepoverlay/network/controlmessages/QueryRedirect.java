package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;

import java.io.Serializable;
import java.util.List;

public class QueryRedirect implements Serializable {
    private int queryId;
    List<QuerySink> querySinks;

    public QueryRedirect(int queryId, List<QuerySink> querySinks) {
        this.queryId = queryId;
        this.querySinks = querySinks;
    }

    public List<QuerySink> getQuerySinks() {
        return querySinks;
    }

    public void setQuerySinks(List<QuerySink> querySinks) {
        this.querySinks = querySinks;
    }

    public int getQueryId() {
        return queryId;
    }

    @Override
    public String toString() {
        return "QueryRedirect{" +
                "queryId=" + queryId +
                ", querySinks=" + querySinks +
                '}';
    }
}
