package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class QueryMigrationFinished implements Serializable {
    private int id;

    public QueryMigrationFinished(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
