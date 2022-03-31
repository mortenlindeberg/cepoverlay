package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import no.uio.ifi.dmms.cepoverlay.queryengine.Query;

import java.io.Serializable;

public class PartialSnapshot implements Serializable {
    private Query query;
    private int num;
    private int all;
    private byte[] partialSnapshot;
    private String masterAddress;
    private int masterPort;

    public PartialSnapshot(Query query, int num, int all, byte[] partialSnapshot, String masterAddress, int masterPort) {
        this.query = query;
        this.num = num;
        this.all = all;
        this.partialSnapshot = partialSnapshot;
        this.masterAddress = masterAddress;
        this.masterPort = masterPort;
    }


    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getAll() {
        return all;
    }

    public void setAll(int all) {
        this.all = all;
    }

    public byte[] getPartialSnapshot() {
        return partialSnapshot;
    }

    public void setPartialSnapshot(byte[] partialSnapshot) {
        this.partialSnapshot = partialSnapshot;
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
}
