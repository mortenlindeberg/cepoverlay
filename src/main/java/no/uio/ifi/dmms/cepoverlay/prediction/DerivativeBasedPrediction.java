package no.uio.ifi.dmms.cepoverlay.prediction;

import no.uio.ifi.dmms.cepoverlay.Main;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Properties;

public class DerivativeBasedPrediction {
    private static final Logger log = Logger.getLogger(DerivativeBasedPrediction.class.getName());
    private Adapter adapter = null;

    private int edgePointsSize;
    private int learningWindowSize;
    private int futureInterval;

    private ArrayList<Double> learningWindow;
    private ArrayList<Double> predictedWindow;
    private int predictedWindowIndex = 0;
    private long adaptationThreshold = 0;
    private boolean proactivePlus = false;

    private double slope;

    private boolean modeA = false;
    private int truePositiveCount = 0;
    private int falseNegativeCount = 0;
    private int falsePositiveCount = 0;
    private int trueNegativeCount = 0;

    private long adaptTimestamp = -1;
    private long lastAdapted = -1;
    private boolean proactivePAMAPPlus;
    private boolean slopeActive;

    public DerivativeBasedPrediction(Adapter adapter, boolean modeA) {
        this(adapter);
        this.modeA = modeA;
    }
    public DerivativeBasedPrediction(Adapter adapter) {
        this.adapter = adapter;
        InputStream input = null;
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            // load a properties file

            Properties prop = new Properties();
            prop.load(input);

            learningWindowSize = Integer.parseInt(prop.getProperty("learningWindowSize"));
            learningWindow = new ArrayList<>(learningWindowSize);
            edgePointsSize = Integer.parseInt(prop.getProperty("edgePointsSize"));
            futureInterval = Integer.parseInt(prop.getProperty("futureInterval"));
            if (prop.containsKey("adaptationThreshold"))
                adaptationThreshold = Integer.parseInt(prop.getProperty("adaptationThreshold"));

            log.debug("> DBP: " + learningWindowSize + " " + edgePointsSize + " " + adaptationThreshold+" "+futureInterval);

            if (prop.containsKey("proactivePlus")) {
                proactivePlus = Boolean.parseBoolean(prop.getProperty("proactivePlus"));
                log.debug("-> Proactive plus recognized:" + proactivePlus);
            }
            if (prop.containsKey("proactivePAMAPPlus")) {
                proactivePAMAPPlus = Boolean.parseBoolean(prop.getProperty("proactivePAMAPPlus"));
                log.debug("-> Proactive PAMAP plus recognized:" + proactivePAMAPPlus);
            }
            if (prop.containsKey("slopeActive")) {
                slopeActive = Boolean.parseBoolean(prop.getProperty("slopeActive"));
                log.debug("-> Slope active:" + slopeActive);
            }
            else
                slopeActive = false;

            predictedWindow = new ArrayList<>(futureInterval);

        } catch (IOException e) {
            log.debug("Error in DBP: " +e.getMessage());
            e.printStackTrace();
        }
        lastAdapted = System.currentTimeMillis()-adaptationThreshold; // Explanation: We deduct the adaptation threshold in case the need to adapt happens early on in the run.
    }

    public DerivativeBasedPrediction(int learningWindowSize, int edgePointsSize, int futureInterval, Adapter adapter) {
        this.learningWindowSize = learningWindowSize;
        this.edgePointsSize = edgePointsSize;
        this.learningWindow = new ArrayList<>(learningWindowSize);
        this.futureInterval = futureInterval;
        this.predictedWindow = new ArrayList<>(futureInterval);
        this.lastAdapted = System.currentTimeMillis()-adaptationThreshold; // Explanation: We deduct the adaptation threshold in case the need to adapt happens early on in the run.
        this.adapter = adapter;
    }


    public DerivativeBasedPrediction(int learningWindowSize, int edgePointsSize, int futureInterval, Adapter adapter, boolean mode, boolean proactivePAMAPPlus) {
        this(learningWindowSize, edgePointsSize, futureInterval, adapter);
        this.modeA = mode;
        this.proactivePAMAPPlus = proactivePAMAPPlus;
        this.adaptationThreshold = 0;
        this.lastAdapted = System.currentTimeMillis()-adaptationThreshold; // Explanation: We deduct the adaptation threshold in case the need to adapt happens early on in the run.
    }


    public String addData(long timestamp, double value, double limit) throws InterruptedException {
        /* 1. Lets "log" the correctness for statistical purposes */
        boolean truePositive = ((value < limit) && modeA);
        boolean falseNegative = ((value < limit) && !modeA);
        boolean falsePositive = ((value >= limit) && modeA);
        boolean trueNegative = ((value >= limit) && !modeA);


        if (truePositive)
            truePositiveCount++;
        if (falseNegative)
            falseNegativeCount++;
        if (falsePositive)
            falsePositiveCount++;
        if (trueNegative)
            trueNegativeCount++;

        /* 2. Add the value to the learning window */
        learningWindow.add(value);

        if (learningWindow.size() > learningWindowSize) {
            learningWindow.remove(0);
            double start = 0, end = 0;

            for (int i = 0; i <= edgePointsSize; i++) {
                start += learningWindow.get(i);
                end += learningWindow.get(learningWindowSize - 1 - i);
            }
            start = start / edgePointsSize;
            end = end / edgePointsSize;

            if (start == 0)
                slope = 0;
            else
                slope = (end - start) / learningWindowSize;
        }
        else return null;

        double prediction = getPrediction(value, futureInterval);
        boolean adaptUp = false;
        boolean adaptDown = false;

        String arrow = ""; // Used for plotting only..

        /* 3. Adaptation logic
         *   a) see if we have scheduled an adaptation, and if so do it! */
        if (adaptTimestamp != -1) {
            if (timestamp >= adaptTimestamp) {
                adaptTimestamp = -1;
                if (modeA) {
                    modeA = false;
                    adaptDown = true;
                    String tStr = new BigDecimal((double) timestamp / 1000).setScale(3, RoundingMode.HALF_EVEN).toPlainString();
                    arrow = "set arrow from (" + tStr + "),32 to (" + tStr + "),30 \n";

                } else {
                    modeA = true;
                    adaptUp = true;
                    String tStr = new BigDecimal((double) timestamp / 1000).setScale(3, RoundingMode.HALF_EVEN).toPlainString();
                    arrow = "set arrow from (" + tStr + "),30 to (" + tStr + "),32\n";
                }
                adapter.adaptCallback(modeA); /* Carries out the actual adaptation by calling callback function */
                lastAdapted = System.currentTimeMillis();
            }
        }


        /*  b) if no adaptation is scheduled, see if we should schedule now */
        else if (System.currentTimeMillis() > (lastAdapted + adaptationThreshold)) {
            if (!modeA) {
                if (proactivePAMAPPlus) {
                    if (prediction < limit && value < limit) {
                        //printWindow();
                        scheduleAdaptation(timestamp);
                    }
                } else if (prediction < limit) {
                    if (slopeActive) {
                        if (slope < 0) { // Avoid migrating if we are not predicting decline, which indicates we might have already migrated too early
                            scheduleAdaptation(timestamp);
                        }
                    } else
                        scheduleAdaptation(timestamp);
                }
            } else if (modeA) {
                if (proactivePlus) {
                    if (prediction >= limit && value >= limit) {
                        //printWindow();
                        scheduleAdaptation(timestamp);
                    }
                } else if (prediction >= limit) {
                    if (slopeActive) {
                        if (slope > 0) { // Avoid migrating if we are not predicting a decline, which indicates we might have already migrated too early
                            scheduleAdaptation(timestamp);
                        }
                    } else
                        scheduleAdaptation(timestamp);
                }
            }
        }

        /* 4. Log the error of the old predicted value */
        double oldPrediction = -1;
        if (predictedWindowIndex < futureInterval) {
            predictedWindow.add(prediction);
            predictedWindowIndex++;
        } else {
            oldPrediction = predictedWindow.remove(0);
            predictedWindow.add(prediction);
        }

        /*
        String tStr = new BigDecimal((double) timestamp / 1000).setScale(3, RoundingMode.HALF_EVEN).toPlainString();
        int rate = (modeA ? 1 : 0) * 100;
        return arrow + tStr + " " + value + " " + prediction + " " + oldPrediction + " " + slope +" "+ modeA + " " + rate + " " + truePositive + " " + falseNegative + " " + falsePositive + " " + trueNegative + " " + truePositiveCount + " " + falseNegativeCount + " " + falsePositiveCount + " " + trueNegativeCount + " " + adaptUp + " " + adaptDown;
*       */
        if (arrow.isEmpty()) return null;
        return arrow;
    }

    private void printWindow() {
        log.debug("Window: " +learningWindow.toString());

    }

    public void scheduleAdaptation(long timestamp) {
        adaptTimestamp = timestamp;
    }

    private double getPrediction(double value, int x) {
        return value + (slope * x);
    }

    public boolean getFilterTrue() {
        return modeA;
    }

    public int getTruePositiveCount() {
        return truePositiveCount;
    }

    public int getFalseNegativeCount() {
        return falseNegativeCount;
    }

    public int getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public int getTrueNegativeCount() {
        return trueNegativeCount;
    }
}
