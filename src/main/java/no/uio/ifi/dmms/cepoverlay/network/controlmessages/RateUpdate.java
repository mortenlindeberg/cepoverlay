package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class RateUpdate implements Serializable  {
    private String address;
    private int port;
    private boolean mode;

    public RateUpdate(String address, int port, boolean mode) {
        this.address = address;
        this.port = port;
        this.mode = mode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isMode() {
        return mode;
    }

    public void setMode(boolean mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "RateUpdate{" +
                "address='" + address + '\'' +
                ", port=" + port +
                ", mode=" + mode +
                '}';
    }
}
