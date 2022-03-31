package no.uio.ifi.dmms.cepoverlay.source;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class FireSource extends SimpleSourceThread {
    private static final String ADAPT_FILENAME = "adapt.res";
    private final int sleep;

    private BufferedWriter writer;
    private NormalDistribution norm;
    private double stDev;
    double max;

    private long lowerLimit;
    private long upperLimit;
    private long upperProlongement;
    private long modeDuration;
    private long mid;

    public FireSource(String address, int port, String sendAddress, int sendPort, int streamId, int sleep, double stDev, long upperProlongement) {
        super(address, port, sendAddress, sendPort, streamId, sleep, new SourceFilter(true));
        if (port == 2080) { //Note: This only but is a hack to plot stuff
            try {
                writer = new BufferedWriter(new FileWriter("original.res", true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.norm = new NormalDistribution(null, 20000, 6000);
        this.max = norm.density(20000);
        this.stDev = stDev;

        this.upperProlongement = upperProlongement;
        lowerLimit = 140000;
        upperLimit = 180000 + upperProlongement;
        modeDuration = upperLimit - lowerLimit;
        mid = lowerLimit + (modeDuration / 2);
        this.sleep = sleep;

        super.log.debug("> Source started on port "+port+" with ID "+streamId+" variance "+ this.stDev + " and sleep " +sleep);
    }

    public Object[] getNextTuple(int stream, long timestamp) {
        Object[] out = null;

        if (stream == 1) {
            if ((timestamp > 40000 && timestamp < 80000)) {
                double step = (double) (timestamp - 40000);
                double prob = norm.density(step);
                double factor = 0;
                if (prob > 0)
                    factor = (prob / max);
                double temp = 5 + (factor) * 25;
                out = new Object[]{timestamp, 1, addNoise(temp)};
            }
            else if ((timestamp > lowerLimit && timestamp < upperLimit + upperProlongement)) {
                double step;
                if (upperProlongement > 0) {
                    if ((timestamp > mid - (upperProlongement / 2)) && timestamp < mid + (upperProlongement / 2))
                        step = (double) (mid - (upperProlongement / 2) - lowerLimit);
                    else if (timestamp >= mid + (upperProlongement / 2))
                        step = (double) (timestamp - lowerLimit - (upperProlongement));
                    else
                        step = (double) (timestamp - lowerLimit);
                }
                else
                    step = (double) (timestamp - lowerLimit);
                double prob = norm.density(step);
                double factor = 0;
                if (prob > 0)
                    factor = (prob / max);
                double temp = 5 + (factor) * 25;
                out = new Object[]{timestamp, 1, addNoise(temp)};

            }
            else {
                out = new Object[]{timestamp, 1, addNoise(5.0)};
            }

            if (adapter != null) {
                adapter.analyze(out);
            }
            if (writer != null) {
                try {
                    writer.write(new BigDecimal((double) timestamp / 1000).setScale(5, RoundingMode.HALF_EVEN).toPlainString() + " " + out[2] + "\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (stream == 2) {
            out = new Object[]{timestamp, 1, false};
        }

        return out;
    }

    public double addNoise(double value) {
        Random random = new Random();
        return (random.nextGaussian() * stDev + value);
    }
}
