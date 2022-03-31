package no.uio.ifi.dmms.cepoverlay.network.topology;

import java.util.Arrays;
import java.util.List;

public class Instance {
    private String instanceName;
    private List<String> address;
    private int controlPort;
    private float cpu;
    private String linkRate;
    private double predictedInputRate;

    public Instance(String instanceName, String address, int controlPort, float cpu, String linkRate, double predictedInputRate) {
        this.instanceName = instanceName;
        this.address = Arrays.asList(address);
        this.controlPort = controlPort;
        this.cpu = cpu;
        this.linkRate = linkRate;
        this.predictedInputRate = predictedInputRate;
    }

    public Instance(String instanceName, List<String> address, int controlPort, float cpu, String linkRate, double predictedInputRate) {
        this.instanceName = instanceName;
        this.address = address;
        this.controlPort = controlPort;
        this.cpu = cpu;
        this.linkRate = linkRate;
        this.predictedInputRate = predictedInputRate;
    }

    public Instance(String instanceName, String address, int controlPort) {
        this (instanceName, address, controlPort, 1,"1Mbps", 1);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getAddress() {
        return address.get(0);
    }

    public String getAddress(int i) {
        return address.get(i);
    }

    public int getControlPort() {
        return controlPort;
    }

    public void setControlPort(int controlPort) {
        this.controlPort = controlPort;
    }

    public float getCpu() {
        return cpu;
    }

    public void setCpu(float cpu) {
        this.cpu = cpu;
    }

    public String getLinkRate() {
        return linkRate;
    }

    public void setLinkRate(String linkRate) {
        this.linkRate = linkRate;
    }

    @Override
    public String toString() {
        return instanceName+":"+getAddress();
    }

    public double getPredictedInputRate() {
        return predictedInputRate;
    }
    public void setPredictedInputRate(double predictedInputRate) {
        this.predictedInputRate = predictedInputRate;
    }
}
