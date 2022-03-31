package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import java.io.Serializable;

public class OverlayStop implements Serializable {
    private int port;

    public OverlayStop(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
