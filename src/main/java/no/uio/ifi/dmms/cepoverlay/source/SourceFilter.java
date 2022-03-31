package no.uio.ifi.dmms.cepoverlay.source;

import no.uio.ifi.dmms.cepoverlay.Main;

public class SourceFilter {
    private boolean active = false;

    public SourceFilter(boolean active) {
        this.active = active;
    }

    public boolean allowed(Object[] tuple) {
        if (active)
            return (Double) tuple[2] >= Main.LIMIT;
        else return true;
    }
}
