package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.source.FireSource;
import no.uio.ifi.dmms.cepoverlay.source.OscilliatingFireSource;
import org.junit.Test;

import java.util.Arrays;

public class FireSourceTest {
    @Test
    public void testFireSourceTest() {
        Runners.cleanOutputFiles(Arrays.asList(Main.START_FILENAME));
        Runners.writeStartTime(Main.START_FILENAME);

        FireSource f = new FireSource("1",1, "1", 1, 1,1,0.2, 0);

        for (int i = 0; i < 400000; i=i + 100) {
            System.out.println(f.getNextTuple(1, i)[2]);
        }
    }

    @Test
    public void testOscilliatingFireSourceTest() {
        Runners.cleanOutputFiles(Arrays.asList(Main.START_FILENAME));
        Runners.writeStartTime(Main.START_FILENAME);

        OscilliatingFireSource f = new OscilliatingFireSource("1",1, "1", 1, 1,1,0.2, 0);

        for (int i = 0; i < 400000; i=i + 100) {
            System.out.println(i+" "+f.getNextTuple(1, i)[2]);
        }
    }
}
