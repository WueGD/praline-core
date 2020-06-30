package de.uniwue.informatik.praline.layouting.layered.algorithm.restore;

import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.PortGroup;

public class MultiEdgePort implements Restoreable{

    private Port port;
    private PortGroup portGroup;

    public MultiEdgePort (Port port) {
        this.port = port;
        create();
    }

    private void create () {


        if (port.getPortGroup() == null) {

        } else {

        }
    }

    @Override
    public boolean restore () {
        return false;
    }
}
