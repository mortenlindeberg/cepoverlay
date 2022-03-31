package no.uio.ifi.dmms.cepoverlay.prediction;

public class RatePredictor {
    private int predicateLimit;
    private int inputRate;

    public RatePredictor(int inputRate, int predicateLimit) {
        this.inputRate = inputRate;
        this.predicateLimit = predicateLimit;
    }

    public int getPredictedRate(DerivativeBasedPrediction p) {
        boolean filterTrue = p.getFilterTrue();
        return (filterTrue ? 1 : 0)*inputRate;
    }

}
