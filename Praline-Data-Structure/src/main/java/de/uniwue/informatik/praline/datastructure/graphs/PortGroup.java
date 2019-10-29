package de.uniwue.informatik.praline.datastructure.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

public class PortGroup implements PortComposition {

    /*==========
     * Default values
     *==========*/

    public static final boolean DEFAULT_IS_ORDERED = false;

    /*==========
     * Instance variables
     *==========*/

    private List<PortComposition> portCompositions;
    private boolean ordered;

    /*==========
     * Constructors
     *==========*/

    public PortGroup() {
        this(null, DEFAULT_IS_ORDERED);
    }

    public PortGroup(int numberOfPortsPlannedToAdd) {
        portCompositions = new ArrayList<>(numberOfPortsPlannedToAdd);
        ordered = DEFAULT_IS_ORDERED;
    }

    public PortGroup(Collection<PortComposition> portCompositions) {
        this(portCompositions, DEFAULT_IS_ORDERED);
    }

    public PortGroup(Collection<PortComposition> portCompositions, boolean ordered) {
        this.portCompositions = newArrayListNullSave(portCompositions);
        this.ordered = ordered;
    }

    /*==========
     * Getters & Setters
     *==========*/

    public List<PortComposition> getPortCompositions() {
        return portCompositions;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    /*==========
     * Modifiers
     *
     * Modificiations of the lists currently by List get***()
     * this maybe changed later:
     * make explicit add() and remove() methods and
     * add "Collections.unmodifiableList(...)" to getters
     *==========*/
}
