package no.uio.ifi.dmms.cepoverlay.source;

import org.apache.log4j.Logger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TempTuple {
    private final static Logger log = Logger.getLogger(TempTuple.class);
    private long timestamp;
    private String city;
    private double temp;
    private double hum;

    private long prevTimestamp; // Used to fix the occurences of 0 in dataset
    private double prevTemp; // Used to fix the occurrences of NaN in dataset
    private double prevHum; // Used to fix the occurrences of NA in dataset

    //Year,Month,DayOfMonth,HourOfDay,TemperatureC,Humidity
    //0 2008,10,6,22,NaN,NA
    public TempTuple(String csv, String city) {
        this.city = city;
        try {
            String arr[] = csv.split(",");
            int year = Integer.parseInt(arr[0]);
            int month = Integer.parseInt(arr[1]);
            int dayOfMonth = Integer.parseInt(arr[2]);
            int hourOfDay = Integer.parseInt(arr[3]) - 1; // Seems datafile has 00 as 24:00, which Java does not understand
            this.temp = Double.parseDouble(arr[4]);
            this.hum = Double.parseDouble(arr[5]);
            this.timestamp = LocalDateTime.of(year, month, dayOfMonth, hourOfDay, 0).toEpochSecond(ZoneOffset.UTC) * 1000;

        } catch (Exception e) {
            log.debug("> Problem with line: " + csv);
            this.timestamp = prevTimestamp;
            this.temp = prevTemp;
            this.hum = prevHum;
        }

        if (timestamp == 0)
            timestamp = prevTimestamp;
        else prevTimestamp = timestamp;

        if (new Double(temp).isNaN())
            temp = prevTemp;
        else prevTemp = temp;

        if (new Double(hum).isNaN())
            hum = prevHum;
        else prevHum = hum;

        log.debug("> Temp tuple: "+this.toString());
    }

    @Override
    public String toString() {
        return "TempTuple{" +
                "timestamp=" + timestamp +
                ", city=" + city +
                ", temp=" + temp +
                ", hum=" + hum +
                '}';
    }

    public Object[] getEvent() {
        return new Object[]{timestamp, city, temp, hum};
    }
}
