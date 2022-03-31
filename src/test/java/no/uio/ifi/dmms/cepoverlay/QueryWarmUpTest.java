package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.queryengine.EngineWarmUp;
import org.junit.Test;

public class QueryWarmUpTest {
    @Test
    public void testWarmUp() throws InterruptedException {
        EngineWarmUp engineWarmUp = new EngineWarmUp();
        System.out.println("Warming up");
        engineWarmUp.warmUp();
        System.out.println("Done");
    }
}
