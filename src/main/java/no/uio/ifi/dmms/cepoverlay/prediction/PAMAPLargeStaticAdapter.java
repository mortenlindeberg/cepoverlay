package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.RateUpdate;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import org.apache.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

public class PAMAPLargeStaticAdapter implements Adapter {
    private static final Logger log = Logger.getLogger(PAMAPLargeAdapter.class.getName());
    private BufferedWriter writer;
    private DerivativeBasedPrediction dbp;
    private long start;
    private String placementAddress;
    private String adaptMaster;
    private boolean modeA;
    private long adaptationThreshold = 0;
    private long lastAdapted = -1;

    private double predLimit;
    private int predIndex;

    public PAMAPLargeStaticAdapter(String adaptFilename, boolean modeA, String placementAddress, String adaptMaster, double predLimit, int predIndex) {
        log.debug("> Initializing PAMAPLargeAdapter callback in mode A: "+modeA+" "+predLimit+" "+predIndex);
        start = Runners.readStartTime(Main.START_FILENAME);

        //TODO: Use ConfigReader to read these props
        InputStream input = null;
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            // load a properties file

            Properties prop = new Properties();
            prop.load(input);
            if (prop.containsKey("adaptationThreshold"))
                adaptationThreshold = Integer.parseInt(prop.getProperty("adaptationThreshold"));
        } catch (IOException e) {
            log.debug("Error in DBP: " +e.getMessage());
            e.printStackTrace();
        }
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
        lastAdapted = System.currentTimeMillis()-adaptationThreshold; // Explanation: We deduct the adaptation threshold in case the need to adapt happens early on in the run.
    }

    @Override
    public void adaptCallback(boolean modeA) throws InterruptedException {
        this.modeA = modeA;
        QueryControl.sendRateUpdate(new RateUpdate(placementAddress,1080, modeA), adaptMaster, 1080);
        log.debug("> Sending rate update from here: "+placementAddress +" "+modeA);
        lastAdapted = System.currentTimeMillis();
    }

    @Override
    public void analyze(Object[] tuple) {
        try {
            double value = (double) tuple[predIndex];

             if (System.currentTimeMillis() > (lastAdapted + adaptationThreshold)) {
                 //writer.write(tStr +" "+modeA+ " "+predLimit+" "+value+" "+lastAdapted+" "+adaptationThreshold+"\n");
                 if (!modeA && (value <= predLimit)) {
                     modeA = true;
                     adaptCallback(true);
                   //  writer.write(tStr + " " + modeA + " " + value + "\n");
                     String tStr = new BigDecimal((double) (System.currentTimeMillis() - start) / 1000).setScale(5, RoundingMode.HALF_EVEN).toPlainString();
                     writer.write("set arrow from (" + tStr + "),0 to (" + tStr + "),100\n");
                     writer.flush();

                 } else if (modeA && (value > predLimit)) {
                     modeA = false;
                     adaptCallback(false);
                     //writer.write(tStr + " " + modeA + " " + value + "\n");
                     String tStr = new BigDecimal((double) (System.currentTimeMillis() - start) / 1000).setScale(5, RoundingMode.HALF_EVEN).toPlainString();
                     writer.write("set arrow from (" + tStr + "),0 to (" + tStr + "),100\n");
                     writer.flush();
                 }
             }

        } catch (IOException | InterruptedException e) {
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
