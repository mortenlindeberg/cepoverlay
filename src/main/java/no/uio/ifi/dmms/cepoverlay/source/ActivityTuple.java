package no.uio.ifi.dmms.cepoverlay.source;

public class ActivityTuple {

    private long timestamp;
    private int activityId;
    private double heartRate;
    private double temperatureHand;
    private double temperatureChest;
    private double temperatureAnkle;

    public ActivityTuple(long timestamp, int activityId, double heartRate, double temperatureHand, double temperatureChest, double temperatureAnkle) {
        this.timestamp = timestamp;
        this.activityId = activityId;
        this.heartRate = heartRate;
        this.temperatureHand = temperatureHand;
        this.temperatureChest = temperatureChest;
        this.temperatureAnkle = temperatureAnkle;
    }
    public ActivityTuple(String[] lineArr) {
        this(
                (long) (Double.parseDouble(lineArr[0]) * 1000),
                Integer.parseInt(lineArr[1]),
                Double.parseDouble(lineArr[2]),
                Double.parseDouble(lineArr[3]),
                Double.parseDouble(lineArr[20]),
                Double.parseDouble(lineArr[37]));
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    public double getTemperatureHand() {
        return temperatureHand;
    }

    public void setTemperatureHand(double temperatureHand) {
        this.temperatureHand = temperatureHand;
    }

    public double getTemperatureChest() {
        return temperatureChest;
    }

    public void setTemperatureChest(double temperatureChest) {
        this.temperatureChest = temperatureChest;
    }

    public double getTemperatureAnkle() {
        return temperatureAnkle;
    }

    public void setTemperatureAnkle(double temperatureAnkle) {
        this.temperatureAnkle = temperatureAnkle;
    }

    public Object[] getEvent() {
        return new Object[]{timestamp, activityId, heartRate, temperatureHand, temperatureHand, temperatureAnkle};
    }

    public Object[] getActivityEvent(long realTimestamp) {
        return new Object[]{realTimestamp, activityId};
    }

    public Object[] getHeartRateEvent(long realTimestamp) { return new Object[]{realTimestamp,  heartRate, temperatureHand, temperatureChest, temperatureAnkle}; }

    @Override
    public String toString() {
        return "ActivityTuple{" +
                "timestamp=" + timestamp +
                ", activityId='" + activityId + '\'' +
                ", heartRate=" + heartRate +
                ", temperatureHand=" + temperatureHand +
                ", temperatureChest=" + temperatureHand +
                ", temperatureAnkle=" + temperatureAnkle +
                '}';
    }
}
