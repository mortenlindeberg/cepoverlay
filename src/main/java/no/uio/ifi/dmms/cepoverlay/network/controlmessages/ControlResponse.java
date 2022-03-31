package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

public class ControlResponse {
    private int status;

    public ControlResponse(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
