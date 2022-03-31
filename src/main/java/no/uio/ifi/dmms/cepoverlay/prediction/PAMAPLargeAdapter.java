package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.RateUpdate;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PAMAPLargeAdapter implements Adapter {
    private static final Logger log = Logger.getLogger(PAMAPLargeAdapter.class.getName());
    private BufferedWriter writer;
    private DerivativeBasedPrediction dbp;
    private long start;
    private String placementAddress;
    private String adaptMaster;
    private boolean modeA;

    private double predLimit;
    private int predIndex;

    public PAMAPLargeAdapter(String adaptFilename, boolean modeA, String placementAddress, String adaptMaster, double predLimit, int predIndex) {
        log.debug("> Initializing PAMAPLargeAdapter callback in mode A: "+modeA+" "+predLimit+" "+predIndex);
        start = Runners.readStartTime(Main.START_FILENAME);
        dbp = new DerivativeBasedPrediction(this, modeA);

        try {
            writer = new BufferedWriter(new FileWriter(adaptFilename, true));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.modeA = modeA;
        this.placementAddress = placementAddress;
        this.adaptMaster = adaptMaster;
        this.predLimit = predLimit;
        this.predIndex = predIndex;
    }

    @Override
    public void adaptCallback(boolean modeA) throws InterruptedException {
        this.modeA = modeA;
        QueryControl.sendRateUpdate(new RateUpdate(placementAddress,1080, modeA), adaptMaster, 1080);
        log.debug("> Sending rate update from here: "+placementAddress +" "+modeA);
    }

    @Override
    public void analyze(Object[] tuple) {
        try {
            long now = System.currentTimeMillis() - start;
            String adaptString = null;
            try {
                adaptString = dbp.addData(now, (double)tuple[predIndex], predLimit);
            } catch (Exception e) {
                log.debug("Error:  " +e.getMessage());
                e.printStackTrace();
            }

            if (adaptString != null) {
                writer.write(adaptString + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void endStream() {
        log.debug("-> Stream ending so we need to send a rate update!");
        try {
            QueryControl.sendRateUpdate(new RateUpdate(placementAddress,1080, true), adaptMaster, 1080);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void migrationFinished() {
        // Not used
    }
}
