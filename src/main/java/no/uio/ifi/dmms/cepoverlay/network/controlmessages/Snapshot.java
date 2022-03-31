package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import no.uio.ifi.dmms.cepoverlay.queryengine.Query;

import java.io.Serializable;

public class Snapshot implements Serializable {
    private Query query;
    private byte[] snapshot;
    private String masterAddress;
    private int masterPort;

    public Snapshot(Query query, byte[] snapshot, String masterAddress, int masterPort) {
        this.query = query;
        this.snapshot = snapshot;
        this.masterAddress = masterAddress;
        this.masterPort = masterPort;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public byte[] getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public int getSize() { return snapshot.length;  }

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
}
