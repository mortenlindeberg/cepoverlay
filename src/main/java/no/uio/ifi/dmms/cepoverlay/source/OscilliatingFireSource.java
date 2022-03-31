package no.uio.ifi.dmms.cepoverlay.source;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class OscilliatingFireSource extends SimpleSourceThread {
    private BufferedWriter writer;
    private double stDev;
    private double frequency;
    private long time_offset = 100000; /* This to allow warm-up period*/
    private NormalDistribution norm;
    double max;

    public OscilliatingFireSource(String address, int port, String sendAddress, int sendPort, int streamId, int sleep, double noise, double frequency) {
        super(address, port, sendAddress, sendPort, streamId, sleep, new SourceFilter(true));
        this.stDev = noise;
        this.frequency = frequency;
        if (port == 2080) { //Note: This is only but a hack to plot stuff
            try {
                writer = new BufferedWriter(new FileWriter("original.res", true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.norm = new NormalDistribution(null, 20000, 6000);
        this.max = norm.density(20000);
    }

    public Object[] getNextTuple(int stream, long timestamp) {
        Object[] out;
        if (stream == 1) {

            if (timestamp > time_offset) {
                out = new Object[]{timestamp, 1, addNoise(calculate(timestamp - time_offset))};
            } else if (timestamp > 40000 && timestamp < 80000) { /* The initial warm-up period.. */
                double step = (double) (timestamp - 40000);
                double prob = norm.density(step);
                double factor = 0;
                if (prob > 0)
                    factor = (prob / max);
                double temp = 5 + (factor) * 25;
                out = new Object[]{timestamp, 1, addNoise(temp)};
            } else {
                out = new Object[]{timestamp, 1, addNoise(5.0)};
            }

            if (writer != null) {
                try {
                    writer.write(new BigDecimal((double) timestamp / 1000).setScale(5, RoundingMode.HALF_EVEN).toPlainString() + " " + out[2] + "\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (adapter != null) {
                adapter.analyze(out);
            }

            return out;
        } else if (stream == 2) {
            return new Object[]{timestamp, 1, false};
        }
        super.log.error("Unknown stream in source: " + stream);
        return null;
    }

    public double calculate(long timestamp) {
        double xAdjusted = Math.cos((timestamp * 2 * Math.PI) / 1000 * frequency / 60) * -1;
        double yAdjusted = 5 + ((xAdjusted + 1) / 2) * 25;

        return yAdjusted;
    }

    public double addNoise(double value) {
        Random random = new Random();
        return (random.nextGaussian() * stDev + value);
    }
}
