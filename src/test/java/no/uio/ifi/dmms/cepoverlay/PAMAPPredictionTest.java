package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.prediction.Adapter;
import no.uio.ifi.dmms.cepoverlay.prediction.DerivativeBasedPrediction;
import no.uio.ifi.dmms.cepoverlay.source.PAMAPSource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PAMAPPredictionTest {
    private TestAdapter ta;

    @Test
    public void runParamTest() throws InterruptedException {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        List<Integer> fws = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        List<Integer> lws = Arrays.asList(120,160,180);
        List<Integer> eps = Arrays.asList(3, 6, 9,12);

        List<Integer> streams = Arrays.asList(10, 12, 14, 16, 18, 20, 22, 24);

//        List<Integer> lws = Arrays.asList(20);
//        List<Integer> eps = Arrays.asList(3);


        for (int streamId : streams)
            for (int fw : fws)
                for (int lw : lws) {
                    for (int ep : eps) {
                        if (ep < lw) {
                            ta = new TestAdapter();
                            //String line = testStreamParamsHr(streamId, lw, ep, fw);
                            String line = testStreamParamsHr(streamId, lw, ep, fw);
                            System.out.println(streamId + " " + lw + " " + ep + " " + ta.getAdaptations() + " " + line);
                        }
                    }
                }
    }


    public String testStreamParams(int streamId, int lw, int ep, int fw) throws InterruptedException {
        boolean mode = false;

        switch (streamId) {
            case 10: // 1
            case 14: // 3
            case 16: // 4
            case 22: // 7
                mode = false;
                break;
            case 12: // 2
            case 18: // 5
            case 20: // 6
            case 24: // 8
                mode = true;
                break;
        }

        DerivativeBasedPrediction dbp = new DerivativeBasedPrediction(lw, ep, fw, ta, mode, false);

        int predicateLimit = 32;

        PAMAPSource pamapSource = new PAMAPSource(null, -1, null, -1, streamId, (int) Math.pow(10, 10));

        Object[] t;
        String outcome = null;
        while ((t = pamapSource.getNextTuple(streamId, 0)) != null) {
            double value = (double) t[2];
            long timestamp = (long) t[0];
            dbp.addData(timestamp, value, predicateLimit);

        }
        return fw + " " + dbp.getTruePositiveCount() + " " + dbp.getFalseNegativeCount() + " " + dbp.getFalsePositiveCount() + " " + dbp.getTrueNegativeCount();
    }

    public String testStreamParamsHr(int streamId, int lw, int ep, int fw) throws InterruptedException {
        boolean mode = false;

        switch (streamId) {
            case 10: // 1
                mode = false;
                break;
            case 12: // 2
            case 14: // 3
            case 16: // 4
            case 18: // 5
            case 20: // 6
            case 22: // 7
            case 24: // 8
                mode = true;
                break;
        }

        DerivativeBasedPrediction dbp = new DerivativeBasedPrediction(lw, ep, fw, ta, mode, false);

        int predicateLimit = 100;

        PAMAPSource pamapSource = new PAMAPSource(null, -1, null, -1, streamId, (int) Math.pow(10, 10));

        Object[] t;
        while ((t = pamapSource.getNextTuple(streamId, 0)) != null) {
            double value = (double) t[1];
            long timestamp = (long) t[0];
            dbp.addData(timestamp, value, predicateLimit);

        }
        return fw + " " + dbp.getTruePositiveCount() + " " + dbp.getFalseNegativeCount() + " " + dbp.getFalsePositiveCount() + " " + dbp.getTrueNegativeCount();
    }


    @Test
    public void testStream1() throws InterruptedException {
        testStream(2);
    }

    @Test
    public void testStream2() throws InterruptedException {
        testStream(4);
    }

    @Test
    public void testStream3() throws InterruptedException {
        testStream(6);
    }

    @Test
    public void testStream4() throws InterruptedException {
        testStream(8);
    }

    public void testStream(int streamId) throws InterruptedException {
        boolean mode;
        if (streamId == 4)
            mode = true;
        else mode = false;
        DerivativeBasedPrediction dbp = new DerivativeBasedPrediction(20, 3, 50, new TestAdapter(), mode, true);

        int predicateLimit = 32;

        PAMAPSource pamapSource = new PAMAPSource(null, -1, null, -1, streamId, (int) Math.pow(10, 10));

        Object[] t;
        while ((t = pamapSource.getNextTuple(streamId, 0)) != null) {
            double value = (double) t[2];
            long timestamp = (long) t[0];
            String outcome = dbp.addData(timestamp, value, predicateLimit);
            System.out.println(outcome);
        }
    }

    @Test
    public void testIfPredicateIsFalseEverywhere() {
        List<Integer> streams = Arrays.asList(10, 12, 14, 16, 18, 20, 22, 24);
        CopyOnWriteArrayList<PAMAPSource> pamapSources = new CopyOnWriteArrayList<>();
        for (Integer stream : streams)
            pamapSources.add(new PAMAPSource(null, -1, null, -1, stream, (int) Math.pow(10, 10)));
        int hits = 0;
        int misses = 0;
        boolean keepGoing = true;
        while (keepGoing) {
            boolean outcome = true;
            int ended = 0;
            for (PAMAPSource p : pamapSources) {
                Object[] o = p.getNextTuple(streams.get(pamapSources.indexOf(p)), 0); // It is really ugly the way I get this number..
                if (o != null) {
                    double val = (double) o[1];

                    if (val >= Main.PAMAP_LIMIT_HR)
                        outcome = false;
                } else {
                    ended++;
                }
            }
            if (outcome) hits++;
            else misses++;
            if (ended == streams.size())
                keepGoing = false;
        }
        System.out.println(" Hits: " + hits + ". Misses: " + misses);
    }


public class TestAdapter implements Adapter {

    int adaptations = 0;

    @Override
    public void adaptCallback(boolean modeA) {
        adaptations++;
    }

    @Override
    public void analyze(Object[] tuple) {

    }

    @Override
    public void endStream() {
    }

    public void migrationFinished() {
    }

    public int getAdaptations() {
        return adaptations;
    }
}
}
