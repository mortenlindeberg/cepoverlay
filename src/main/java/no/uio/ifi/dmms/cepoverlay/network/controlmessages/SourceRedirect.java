package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class SourceRedirect implements Serializable {
    private String address;
    private int port;

    public SourceRedirect(String address, int port) {
        this.address = address;
        this.port = port;
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
}
