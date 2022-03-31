package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.source.SourceUtility;
import org.junit.Test;

import java.io.IOException;

public class SourceUtilityTest {
    @Test
    public void testSourceUtility() {

        try {
            SourceUtility.shortenSource("stream8.dat", "stream8_short.dat",1500);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            SourceUtility.shortenSource("stream9.dat", "stream9_short.dat",1500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSourceUtilityFixNaN() {
        try {
            SourceUtility.fixNaN("stream8_short.dat", "stream8_NaN.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            SourceUtility.fixNaN("stream9_short.dat", "stream9_NaN.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
