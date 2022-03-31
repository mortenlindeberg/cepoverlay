package no.uio.ifi.dmms.cepoverlay.prediction;

public interface Adapter {
    void adaptCallback(boolean modeA) throws InterruptedException;

    void analyze(Object[] tuple);

    void endStream();

    
    void migrationFinished();
}
