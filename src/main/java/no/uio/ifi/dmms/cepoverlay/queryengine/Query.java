package no.uio.ifi.dmms.cepoverlay.queryengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Query implements Serializable {
    private int queryId;
    private String app;
    private List<QuerySource> querySources;
    private List<QuerySink> querySinks;

    public Query(int queryId, String app, List<QuerySource> sources, List<QuerySink> sinks) {
        this.queryId = queryId;
        this.app = app;
        this.querySources = sources;
        this.querySinks = sinks;
    }
    public Query(int queryId, String app, List<QuerySource> sources) {
        this(queryId, app, sources, new ArrayList<>(0));
    }

    public Query(int queryId, String app, String streamName, int streamId) {
        this(queryId, app, Arrays.asList(new QuerySource[]{new QuerySource(streamName, streamId)}));
    }

    public Query(int queryId, String app,  QuerySource querySource, QuerySink querySink) {
        this(queryId, app, Arrays.asList(querySource), Arrays.asList(querySink));
    }

    public int getQueryId() { return queryId; }

    public void setQueryId(int queryId) {
        this.queryId =queryId;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public List<QuerySource> getQuerySources() {
        return querySources;
    }

    public void setQuerySources(List<QuerySource> querySources) {
        this.querySources = querySources;
    }

    public List<QuerySink> getQuerySinks() {
        return querySinks;
    }

    public void setQuerySinks(List<QuerySink> querySinks) {
        this.querySinks = querySinks;
    }

    @Override
    public String toString() {
        return "Query{" +
                "queryId=" + queryId +
                ", app='" + app + '\'' +
                ", querySources=" + querySources +
                ", querySinks=" + querySinks +
                '}';
    }
}