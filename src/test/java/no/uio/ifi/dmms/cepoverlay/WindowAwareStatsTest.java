package no.uio.ifi.dmms.cepoverlay;

import org.junit.Test;

public class WindowAwareStatsTest {
    @Test
    public void windowAwareStatTest() {
        double[] windowSizes = {60000,30000,1000,500};
        for (int i = 1; i <= 20; i++) {
            int mt = i * 50;
            for (double ws : windowSizes)
                System.out.println("PLOT "+mt + " " + ws + " " + calculateLat(mt, ws)+" "+((double)mt/ws));
        }
    }

    public double calculateLat(int mt, double ws) {
        double lat;
        double sumLat = 0;
        int n = 100000;
        for (int i = 0; i < n; i++) {
            int timestamp = (int) (Math.random() * ws);

            if (timestamp < (ws - mt))
                lat = 0;
            else
                lat = timestamp + mt - ws;

            sumLat += lat;

        }
        return (sumLat / 1000);
    }

    @Test
    public void plotLat() {
        int mt = 200;
        double ws = 1000;
        double lat;
        double sumLat = 0;
        int n = 1000;
        for (int i = 0; i < n; i++) {
            int timestamp = i;

            if (timestamp < (ws - mt))
                lat = 0;
            else
                lat = timestamp + mt - ws;

            sumLat += lat;
            System.out.println("PLOT "+i+" "+(sumLat / i)+" "+lat);
        }
    }

    @Test
    public void plotRandLat() {
        int mt = 200;
        double ws = 1000;
        double lat;
        double sumLat = 0;
        int n = 1000;
        for (int i = 0; i < n; i++) {
            int timestamp = (int) (Math.random() * ws);

            if (timestamp < (ws - mt))
                lat = 0;
            else
                lat = timestamp + mt - ws;

            sumLat += lat;
            System.out.println("PLOT "+i+" "+(sumLat / i)+" "+lat);
        }
    }
}
