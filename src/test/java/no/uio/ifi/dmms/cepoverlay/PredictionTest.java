package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.prediction.Adapter;
import no.uio.ifi.dmms.cepoverlay.prediction.DerivativeBasedPrediction;
import no.uio.ifi.dmms.cepoverlay.prediction.RatePredictor;
import org.junit.Test;

import static org.junit.Assert.*;

public class PredictionTest {

    int inputRate = 500;
    int predicateLimit = 32;


    @Test
    public void testPrediction() throws InterruptedException {
        DerivativeBasedPrediction dbp = new DerivativeBasedPrediction(20, 3, 50, new TestAdapter());
        RatePredictor ratePredictor = new RatePredictor(inputRate, predicateLimit);

        assertNotEquals(ratePredictor.getPredictedRate(dbp), 500);

        for (int i = 0; i < 100; i++) {
            dbp.addData(i, i, 45);
        }
        assertEquals(ratePredictor.getPredictedRate(dbp), 500);

        for (int i = 100; i > 0 ; i--) {
            dbp.addData(i, i, 45);
        }
    }

    public class TestAdapter implements Adapter {

        @Override
        public void adaptCallback(boolean modeA) throws InterruptedException {
            System.out.println("Adapt callback:" +modeA);
        }

        @Override
        public void analyze(Object[] tuple) {

        }

        @Override
        public void endStream() {

        }

        @Override
        public void migrationFinished() {

        }


    }
}
