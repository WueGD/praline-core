package de.uniwue.informatik.praline.datastructure.graphs;

public class PortPairing {

    /*==========
     * Instance variables
     *==========*/

    private Port port0;
    private Port port1;

    /*==========
     * Constructors
     *==========*/

    public PortPairing(Port port0, Port port1) {
        this.port0 = port0;
        this.port1 = port1;
    }

    /*==========
     * Getters & Setters
     *==========*/

    public Port getPort0() {
        return port0;
    }

    public void setPort0(Port port0) {
        this.port0 = port0;
    }

    public Port getPort1() {
        return port1;
    }

    public void setPort1(Port port1) {
        this.port1 = port1;
    }
}
