package no.uio.ifi.dmms.cepoverlay.overlay;

import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;

public class OverlayInstanceThread extends Thread {
    private Instance instance;

    public OverlayInstanceThread(Instance instance) {
        this.instance = instance;
    }

    @Override
    public void run() {
        new OverlayInstance(instance);
    }
}
