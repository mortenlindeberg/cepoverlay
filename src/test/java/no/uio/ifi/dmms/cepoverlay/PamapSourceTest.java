package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.source.ActivityTuple;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PamapSourceTest {

    private final BufferedReader reader;
    private double lastHeartRate = 98;
    private String line = "";
    ActivityTuple t = null;
    private int streamId;

    public PamapSourceTest(int streamId) {
        this.streamId = streamId;
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
                System.err.println("Unknown streamId!");
        }

        reader = openFile(fileName);
    }


    public Object[] getNextTuple(int stream, long realTimestamp) throws IOException {

        if (t == null) {
            line = reader.readLine();
            if (line == null)
                throw new RuntimeException("EOF");
            t = new ActivityTuple(line.split(" "));
        }


        if (realTimestamp < t.getTimestamp())
            return null;


        if (Double.isNaN(t.getHeartRate()))
            t.setHeartRate(lastHeartRate);
        else lastHeartRate = t.getHeartRate();

        if (stream % 2 == 0) {
            Object[] out = t.getHeartRateEvent(realTimestamp);
            t = null;
            return out;
        }
        else {
            Object[] out =  t.getActivityEvent(realTimestamp);
            t = null;
            return out;
        }
    }

    public static BufferedReader openFile(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return reader;
    }

    public int streamId() {
        return streamId;
    }
}
