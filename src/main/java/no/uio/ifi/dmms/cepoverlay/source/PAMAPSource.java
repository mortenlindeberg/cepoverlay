package no.uio.ifi.dmms.cepoverlay.source;

import java.io.BufferedReader;
import java.io.IOException;

public class PAMAPSource extends SimpleSourceThread {

    private final BufferedReader reader;
    private double lastHeartRate = 98;
    private int speedupFactor;

    public PAMAPSource(String address, int port, String sendAddress, int sendPort, int streamId, int speedupFactor) {
        super(address, port, sendAddress, sendPort, streamId, speedupFactor, new SourceFilter(false));
        log.debug("New PAMAP source: "+address+" "+port+" sending to "+sendAddress+" "+sendPort+" streamId: "+ streamId);
        String base = "PAMAPData/";
        String fileName = "";
        switch (streamId) {
            case 1:
            case 10:
            case 11:
            case 2:
                fileName = base+"stream1.dat";
                break;
            case 12:
            case 3:
            case 13:
            case 4:
                fileName = base+"stream2.dat";
                break;
            case 14:
            case 5:
            case 15:
            case 6:
                fileName = base+"stream3.dat";
                break;
            case 16:
            case 7:
            case 17:
            case 8:
                fileName = base+"stream4.dat";
                break;
            case 18:
            case 19:
            case 9:
                fileName = base+"stream5.dat";
                break;
            case 20:
            case 21:
                fileName = base+"stream6.dat";
                break;
            case 22:
            case 23:
                fileName = base+"stream7.dat";
                break;
            case 24:
            case 25:
                fileName = base+"stream8.dat";
                break;
            default:
                log.error("Unknown streamId!");
        }

        reader = openFile(fileName);
        this.speedupFactor = speedupFactor;
    }


    public Object[] getNextTuple(int stream, long realTimestamp) {
        String line = null;
        ActivityTuple t = null;

        int sleepTime = -1;
        double tupleTimestamp = 0;

        while (sleepTime < 0) { // Discard the tuples that are "past" in time following system warmup
            try {
                line = reader.readLine();
                if (line == null) {
                    // When this happens, placement should be reconsidered as this is no longer the dominant source of data!
                    if (adapter != null)
                        adapter.endStream();
                    return null;
                }
                t = new ActivityTuple(line.split(" "));
                // Now figure out how long to sleep to "align" actual time to tuple time
                tupleTimestamp = t.getTimestamp() / speedupFactor;

                sleepTime = (int) (tupleTimestamp - realTimestamp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //log.debug(realTimestamp +" "+t.getTimestamp()+" "+tupleTimestamp+" "+sleepTime+" "+t.toString());
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        t = new ActivityTuple(line.split(" "));
        if (Double.isNaN(t.getHeartRate()))
            t.setHeartRate(lastHeartRate);
        else lastHeartRate = t.getHeartRate();

        if (stream % 2 == 0) {
            if (adapter != null)
                adapter.analyze(t.getHeartRateEvent(realTimestamp));
            return t.getHeartRateEvent(realTimestamp);
        }
        else {
            return t.getActivityEvent(realTimestamp);
        }
    }
}
