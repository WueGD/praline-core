package de.uniwue.informatik.praline.datastructure.graphs;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

public class PortGroup implements PortComposition {

    /*==========
     * Default values
     *==========*/

    private Vertex vertex;
    private PortGroup portGroup;
    public static final boolean DEFAULT_IS_ORDERED = false;


    /*==========
     * Instance variables
     *==========*/

    private final List<PortComposition> portCompositions;
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
        for (PortComposition portComposition : this.portCompositions) {
            portComposition.setPortGroup(this);
        }
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<PortComposition> getPortCompositions() {
        return Collections.unmodifiableList(portCompositions);
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }


    /*==========
     * Modifiers
     *==========*/

    @Override
    public Vertex getVertex() {
        return vertex;
    }

    @Override
    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public PortGroup getPortGroup() {
        return portGroup;
    }

    @Override
    public void setPortGroup(PortGroup portGroup) {
        this.portGroup = portGroup;
    }

    public void addPortComposition(PortComposition pc) {
        portCompositions.add(pc);
        pc.setVertex(this.getVertex());
        pc.setPortGroup(this);
    }

    public void addPortComposition(int position, PortComposition pc) {
        portCompositions.add(position, pc);
        pc.setVertex(this.getVertex());
        pc.setPortGroup(this);
    }

    public boolean removePortComposition(PortComposition pc) {
        pc.setVertex(null);
        pc.setPortGroup(null);
        return portCompositions.remove(pc);
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        Iterator<PortComposition> pcIterator = portCompositions.iterator();
        StringBuilder contained = new StringBuilder(pcIterator.hasNext() ? pcIterator.next().toString() : "");
        while (pcIterator.hasNext()) {
            contained.append(",").append(pcIterator.next().toString());
        }
        if (ordered) {
            return "(" + contained.toString() + ")";
        }
        return "{" + contained.toString() + "}";
    }
}
