package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;

import java.io.Serializable;
import java.util.List;

public class QueryMigrate implements Serializable {
    private int queryId;
    private String address;
    private int port;
    private List<QuerySource> sources;
    private List<QuerySink> sinks;

    private String masterAddress;
    private int masterPort;

    public QueryMigrate(int queryId, String address, int port, List<QuerySource> sources, List<QuerySink> sinks, String masterAddress, int masterPort) {
        this.queryId =  queryId;
        this.address = address;
        this.port = port;
        this.sources = sources;
        this.sinks = sinks;
        this.masterAddress = masterAddress;
        this.masterPort = masterPort;
    }

    public int getQueryId() { return queryId; }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
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

    public List<QuerySource> getSources() {
        return sources;
    }

    public void setSources(List<QuerySource> sources) {
        this.sources = sources;
    }

    public List<QuerySink> getSinks() {
        return sinks;
    }

    public void setSinks(List<QuerySink> sinks) {
        this.sinks = sinks;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    @Override
    public String toString() {
        return "QueryMigrate{" +
                "queryId=" + queryId +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", sources=" + sources +
                ", sinks=" + sinks +
                '}';
    }
}
