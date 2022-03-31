package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import org.apache.commons.lang3.SerializationUtils;

public class ControlRequest {
    private int type;
    private byte[] payload;

    public ControlRequest(Query q) {
        this.type = QueryControl.CONTROL_NEW_QUERY;
        this.payload = SerializationUtils.serialize(q);
    }

    public ControlRequest(int type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public ControlRequest(QueryMigrate r) {
        this.type = QueryControl.CONTROL_MIGRATE;
        this.payload = SerializationUtils.serialize(r);
    }

    public ControlRequest(QueryRedirect r)
    {
        this.type = QueryControl.CONTROL_REDIRECT;
        this.payload = SerializationUtils.serialize(r);
    }

    public ControlRequest(Snapshot snapshot) {
        this.type = QueryControl.CONTROL_SNAPSHOT;
        this.payload = SerializationUtils.serialize(snapshot);
    }

    public ControlRequest(OverlayStop s) {
        this.type = QueryControl.CONTROL_OVERLAY_STOP;
        this.payload = SerializationUtils.serialize(s);
    }

    public ControlRequest(QueryStop queryStop) {
        this.type = QueryControl.CONTROL_QUERY_STOP;
        this.payload = SerializationUtils.serialize(queryStop);
    }

    public ControlRequest(SourceRedirect sourceRedirect) {
        this.type = QueryControl.SOURCE_REDIRECT;
        this.payload = SerializationUtils.serialize(sourceRedirect);
    }

    public ControlRequest(AbortLateArrival abortLateArrival) {
        this.type = QueryControl.ABORT_LATE_ARRIVAL;
        this.payload = SerializationUtils.serialize(abortLateArrival);
    }

    public ControlRequest(PartialSnapshot partialSnapshot) {
        this.type = QueryControl.CONTROL_PARTIAL_SNAPSHOT;
        this.payload = SerializationUtils.serialize(partialSnapshot);
    }

    public ControlRequest(RateUpdate rateUpdate) {
        this.type = QueryControl.RATE_UPDATE;
        this.payload = SerializationUtils.serialize(rateUpdate);
    }

    public ControlRequest(QueryMigrationFinished queryMigrationFinished) {
        this.type = QueryControl.QUERY_MIGRATION_FINISHED;
        this.payload = SerializationUtils.serialize(queryMigrationFinished);
    }

    public ControlRequest(WindowAwareQueryMigrate windowAwareQueryMigrate) {
        this.type = QueryControl.WINDOW_AWARE_QUERY_MIGRATE;
        this.payload = SerializationUtils.serialize(windowAwareQueryMigrate);
    }

    public Query getQuery() {
        return SerializationUtils.deserialize(payload);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public QueryMigrate getQueryMigrate() { return SerializationUtils.deserialize(payload); }

    public QueryRedirect getQueryRedirect() { return SerializationUtils.deserialize(payload); }

    public Snapshot getQuerySnapshot() { return SerializationUtils.deserialize(payload); }

    public QueryStop getQueryStop() { return SerializationUtils.deserialize(payload); }

    public SourceRedirect getSourceRedirect() { return SerializationUtils.deserialize(payload); }

    public AbortLateArrival getAbortLateArrival() { return SerializationUtils.deserialize(payload); }

    public PartialSnapshot getQueryPartialSnapshot() { return SerializationUtils.deserialize(payload); }

    public RateUpdate getRateUpdate() { return SerializationUtils.deserialize(payload); }

    public WindowAwareQueryMigrate getWindowAwareQueryMigrate() { return SerializationUtils.deserialize(payload); }
}
