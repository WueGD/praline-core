package de.uniwue.informatik.praline.datastructure.graphs;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

import java.util.*;

public class Vertex implements ShapedObject, LabeledObject {

    /*==========
     * Instance variables
     *==========*/

    /**
     * contains only top level {@link PortGroup}s and {@link Port}s (and in them possibly contained elements)
     */
    private final List<PortComposition> portCompositions;
    private final HashSet<Port> ports;
    private VertexGroup vertexGroup;
    private final LabelManager labelManager;
    private Shape shape;


    /*==========
     * Constructors
     *==========*/

    public Vertex() {
        this(null, null, null, null);
    }

    public Vertex(Collection<PortComposition> portCompositions) {
        this(portCompositions, null, null, null);
    }

    public Vertex(Collection<PortComposition> portCompositions, Shape shape) {
        this(portCompositions, null, null, shape);
    }

    public Vertex(Collection<PortComposition> portCompositions, Collection<Label> labels) {
        this(portCompositions, labels, null, null);
    }

    public Vertex(Collection<PortComposition> portCompositions, Collection<Label> labels, Shape shape) {
        this(portCompositions, labels, null, shape);
    }

    /**
     * leave value as null if it should be empty initially (e.g. no labels)
     *
     * @param portCompositions
     *      It suffices to only have the top-level {@link PortGroup}s and {@link Port}s in this {@link Collection}
     * @param labels
     * @param mainLabel
     * @param shape
     */
    public Vertex(Collection<PortComposition> portCompositions, Collection<Label> labels, Label mainLabel,
                  Shape shape) {
        this.ports = new HashSet<>();
        this.portCompositions = new ArrayList<>();
        if (portCompositions != null) {
            //find top level PortCompositions and lower level ones
            //moreover find all "real" ports
            HashSet<PortComposition> allLowerLevelPortCompositions = new HashSet<>();
            for (PortComposition portComposition : portCompositions) {
                getContainedPortCompositionsAndAllPorts(allLowerLevelPortCompositions, this.ports, portComposition);
            }
            //add only the top level ones to our list + reference this vertex at each port composition
            for (PortComposition portComposition : portCompositions) {
                if (!allLowerLevelPortCompositions.contains(portComposition)) {
                    portCompositions.add(portComposition);
                    assignPortCompositionRecursivelyToVertex(portComposition, this);
                }
            }
        }
        this.labelManager = new LabelManager(this, labels, mainLabel);
        this.shape = shape;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<PortComposition> getPortCompositions() {
        return Collections.unmodifiableList(portCompositions);
    }

    public Set<Port> getPorts() {
        return Collections.unmodifiableSet(ports);
    }

    public VertexGroup getVertexGroup() {
        return vertexGroup;
    }

    protected void setVertexGroup(VertexGroup vertexGroup) {
        this.vertexGroup = vertexGroup;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public LabelManager getLabelManager() {
        return labelManager;
    }


    /*==========
     * Modifiers
     *==========*/

    /**
     * Adds {@link PortComposition} if it is not already contained (on a top or lower level)
     *
     * @param pc
     * @return
     */
    public boolean addPortComposition(PortComposition pc) {
        //check if already contained
        HashSet<PortComposition> allAlreadyContainedPortCompositions = new HashSet<>();
        for (PortComposition currPortComposition : portCompositions) {
            getContainedPortCompositionsAndAllPorts(allAlreadyContainedPortCompositions, new HashSet<>(),
                    currPortComposition);
            allAlreadyContainedPortCompositions.add(currPortComposition);
        }

        //is already contained -> do nothing
        if (allAlreadyContainedPortCompositions.contains(pc)) {
            return false;
        }
        //not yet contained -> add it
        portCompositions.add(pc);
        assignPortCompositionRecursivelyToVertex(pc, this);
        //find ports of newly added PortComposition
        HashSet<Port> newPorts = new HashSet<>();
        getContainedPortCompositionsAndAllPorts(new HashSet<>(), newPorts, pc);
        this.ports.addAll(newPorts);
        return true;
    }

    /**
     *
     * @param pc
     * @return
     *      if false is returned there is no such {@link PortComposition} or something else went wrong (e. g. failed
     *      by removing {@link Port}s contained in the passed {@link PortComposition})
     */
    public boolean removePortComposition(PortComposition pc) {
        boolean success = false;
        //check if top-level-composition
        if (portCompositions.contains(pc)) {
            success = portCompositions.remove(pc);
        }
        //check if lower-level-composition contains it
        for (PortComposition topLevelPortComposition : portCompositions) {
            success = success | removeIfContained(pc, topLevelPortComposition);
        }

        //remove contained ports if necessary
        if (success) {
            //un-link from this vertex
            assignPortCompositionRecursivelyToVertex(pc, null);
            //find ports that are now alive after the previous removal
            HashSet<Port> currentPorts = new HashSet<>();
            for (PortComposition topLevelPortComposition : portCompositions) {
                getContainedPortCompositionsAndAllPorts(new HashSet<>(), currentPorts, topLevelPortComposition);
            }
            //compare this newly alive ports with the previously alive ports
            for (Port oldPort : new ArrayList<>(ports)) {
                if (!currentPorts.contains(oldPort)) {
                    success = success & this.ports.remove(oldPort);
                }
            }
        }

        return  success;
    }


    /*==========
     * Internal
     *==========*/

    private void getContainedPortCompositionsAndAllPorts(HashSet<PortComposition> allLowerLevelPortCompositions,
                                                         HashSet<Port> allPorts,
                                                         PortComposition portComposition) {
        //add Port if possible
        if (portComposition instanceof Port) {
            if (!allPorts.contains(portComposition)) {
                allPorts.add((Port) portComposition);
            }
        }
        //add lower level PortCompositions and go into recursion
        if (portComposition instanceof PortGroup) {
            for (PortComposition lowerLevelComposition : ((PortGroup) portComposition).getPortCompositions()) {
                allLowerLevelPortCompositions.add(lowerLevelComposition);
                getContainedPortCompositionsAndAllPorts(allLowerLevelPortCompositions, allPorts, lowerLevelComposition);
            }
        }
    }

    private boolean removeIfContained(PortComposition removeThis, PortComposition fromThis) {
        if (fromThis instanceof PortGroup) {
            boolean success = false;
            //is contained
            if (((PortGroup) fromThis).getPortCompositions().contains(removeThis)) {
                return ((PortGroup) fromThis).getPortCompositions().remove(removeThis);
            }
            //not on this level contained but possibly on a lower level -> go into recursion
            for (PortComposition portComposition : ((PortGroup) fromThis).getPortCompositions()) {
                success = success | removeIfContained(removeThis, portComposition);
            }
            return success;
        }
        return false;
    }

    private static void assignPortCompositionRecursivelyToVertex(PortComposition topLevelPortComposition,
                                                                    Vertex vertex) {
        topLevelPortComposition.setVertex(vertex);
        if (topLevelPortComposition instanceof PortGroup) {
            for (PortComposition childPortComposition : ((PortGroup) topLevelPortComposition).getPortCompositions()) {
                assignPortCompositionRecursivelyToVertex(childPortComposition, vertex);
            }
        }
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }
}
