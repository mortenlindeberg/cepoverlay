package no.uio.ifi.dmms.cepoverlay.source;

import org.apache.log4j.Logger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LoadTuple {
    private final static Logger log = Logger.getLogger(LoadTuple.class);
    private long timestamp;
    private double trend;
    private double load;

    private long prevTimestamp;
    private double prevLoad;

    public LoadTuple(String csv) {

        try {
            String[] arr = csv.split(",");
            trend = Double.parseDouble(arr[0]);
            int year = Integer.parseInt(arr[2]);
            int month = Integer.parseInt(arr[3]);
            int dayOfMonth = Integer.parseInt(arr[4]);
            int hourOfDay = Integer.parseInt(arr[5]) - 1; // Seems datafile has 00 as 24:00, which Java does not understand
            load = Double.parseDouble(arr[7]);
            timestamp = LocalDateTime.of(year, month, dayOfMonth, hourOfDay,0).toEpochSecond(ZoneOffset.UTC) * 1000;

        } catch (NumberFormatException e) {
            log.debug("> Problem with line: " +csv);
            timestamp = 0;
            load = Double.NaN;
        }

        if (new Double(load).isNaN())
            load = prevLoad;
        else prevLoad = load;

        if (timestamp == 0)
            timestamp = prevTimestamp;
        else prevTimestamp = timestamp;

        //log.debug("> Load tuple: "+this.toString());
    }

    @Override
    public String toString() {
        return "LoadTuple{" +
                "timestamp=" + timestamp +
                ", trend=" + trend +
                ", load=" + load +
                '}';
    }

    public Object[] getEvent() {
        return new Object[]{timestamp, trend, load};
    }
}
