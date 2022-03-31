package no.uio.ifi.dmms.cepoverlay.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TempLoadSource extends SimpleSourceThread {
    public static final long POINT_ZERO = 1041379200;
    private static final long START_TIME_OFFSET = 16000; // 16 seconds offset

    private final BufferedReader reader;
    private final String city;
    private int cityId;
    private int speedupFactor = 500000;

    public static List<String> files = Arrays.asList("rte-temp-hourly-LFBD.dat", "rte-temp-hourly-LFLY.dat", "rte-temp-hourly-LFPG.dat",
            "rte-temp-hourly-LFQQ.dat", "rte-temp-hourly-LFLL.dat", "rte-temp-hourly-LFML.dat", "rte-temp-hourly-LFPO.dat",
            "rte-temp-hourly-LFRS.dat");

    public TempLoadSource(String address, int port, String sendAddress, int sendPort, int streamId, int sleep, int cityId) {
        super(address, port, sendAddress, sendPort, streamId, sleep, new SourceFilter(false));


        this.cityId = cityId;

        if (streamId == 0) {
            reader = openFile(files.get(cityId));
            city = files.get(cityId).split("-")[3];
        }
        else {
            reader = openFile("rte.dat");
            city = "NA";
        }
        log.debug("-> Files opened at TempLoad source. Stream Id: "+streamId+" City Id: " +cityId);
    }

    public Object[] getNextTuple(int stream, long timestamp) {
        String line = null;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object[] t;

        if (stream == 0)
            t =  new TempTuple(line, city).getEvent();
        else
            t = new LoadTuple(line).getEvent();

        /* Now figure out how long to sleep to "align" actual time to tuple time*/
        long elapsedReal = timestamp - START_TIME_OFFSET;
        long sleepTime = 0;

        if (elapsedReal < 0) {
            sleepTime = elapsedReal * -1;
            log.debug(t[0]+" "+elapsedReal);
        }
        else {
            long elapsedTuples = (long) t[0] - (TempLoadSource.POINT_ZERO * 1000);
            long translatedElapsed = elapsedTuples / speedupFactor;
            sleepTime = translatedElapsed - elapsedReal;
            //log.debug(t[0]+" "+elapsedReal+" "+elapsedTuples+" "+translatedElapsed+" "+sleepTime);
            if (sleepTime < 0) sleepTime = 0;
        }

        if (adapter != null) {
            adapter.analyze(t);
        }

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return t;
    }
}
