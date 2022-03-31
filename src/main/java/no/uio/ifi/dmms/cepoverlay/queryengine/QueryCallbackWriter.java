package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.siddhi.core.event.Event;
import io.siddhi.core.stream.output.StreamCallback;
import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class QueryCallbackWriter extends StreamCallback {
    private BufferedWriter writer;
    private volatile int count = 0;
    private int scale = 5;
    private long start;

    public QueryCallbackWriter(String filename) {
        start = Runners.readStartTime(Main.START_FILENAME);
        try {
            writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(Event[] events) {
        try {
            count++;
            Object[] tuple = events[0].getData();

            long now = System.currentTimeMillis() - start;
            String nowStr = new BigDecimal((double) now / 1000).setScale(scale, RoundingMode.HALF_EVEN).toPlainString();

            String out = nowStr + " " + tuple[0].toString();

            for (int i = 1; i < tuple.length; i++)
                out += " " + tuple[i].toString();

            writer.write(out + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return count;
    }

    public void stop() {
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
