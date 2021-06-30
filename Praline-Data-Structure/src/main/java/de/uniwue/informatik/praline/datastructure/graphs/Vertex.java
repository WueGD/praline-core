package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.PropertyObject;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

/**
 * Represents a {@link Vertex} in the graph.
 * In the application of circuit plans or network plans this is typically a device.
 * Note that vertices (devices) can be combined via {@link VertexGroup}s. See there for more.
 * A vertex should have {@link Port}s -- in particular it accesses its edges always via {@link Port}s.
 * Such a port can be set to size zero, which gives the effect as if there were no {@link Port}s.
 * Via a {@link PortGroup} several {@link Port}s and {@link PortGroup}s can be grouped together.
 * Note that the {@link PortGroup}s should build a tree-structure (and not something more complicated).
 *
 * A {@link Vertex} can be labeled.
 * In particular, if you want to assign a name or an ID to a {@link Vertex} you should use a
 * {@link de.uniwue.informatik.praline.datastructure.labels.TextLabel} attached to this {@link Vertex} and make it
 * the main label of this {@link Vertex}.
 *
 * A {@link Vertex} should have a {@link Shape} in the end -- typically a
 * {@link de.uniwue.informatik.praline.datastructure.shapes.Rectangle}.
 * The idea is that a layouting algorithm will take a {@link Graph} and will set the coordinates and sizes of this
 * {@link Shape}, so there is no need to set this in the forehand.
 */
@JsonIgnoreProperties({ "ports", "vertexGroup", "containedPortCompositionsAndAllPorts" })
@JsonPropertyOrder({ "shape", "labelManager", "portCompositions" })
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class Vertex implements ShapedObject, LabeledObject, ReferenceObject, PropertyObject {

    /*==========
     * Instance variables
     *==========*/

    /**
     * contains only top level {@link PortGroup}s and {@link Port}s (and in them possibly contained elements)
     */
    private final List<PortComposition> portCompositions;
    private final LinkedHashSet<Port> ports;
    private VertexGroup vertexGroup;
    protected final LabelManager labelManager;
    protected Shape shape;
    protected String reference;
    private final Map<String, String> properties = new LinkedHashMap<>();


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

    @JsonCreator
    private Vertex(
            @JsonProperty("portCompositions") final Collection<PortComposition> portCompositions,
            @JsonProperty("labelManager") final LabelManager labelManager,
            @JsonProperty("shape") final Shape shape
    ) {
        this(portCompositions, labelManager.getLabels(), labelManager.getMainLabel(), shape);
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
        this.ports = new LinkedHashSet<>();
        this.portCompositions = new ArrayList<>();
        if (portCompositions != null) {
            //find top level PortCompositions and lower level ones
            //moreover find all "real" ports
            LinkedHashSet<PortComposition> allLowerLevelPortCompositions = new LinkedHashSet<>();
            for (PortComposition portComposition : portCompositions) {
                getContainedPortCompositionsAndAllPorts(allLowerLevelPortCompositions, this.ports, portComposition);
            }
            //add only the top level ones to our list + reference this vertex at each port composition
            for (PortComposition portComposition : portCompositions) {
                if (!allLowerLevelPortCompositions.contains(portComposition) &&
                        !this.portCompositions.contains(portComposition)) {
                    this.portCompositions.add(portComposition);
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

    @Override
    public String getReference()
    {
        return this.reference;
    }

    @Override
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
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
        LinkedHashSet<PortComposition> allAlreadyContainedPortCompositions = new LinkedHashSet<>();
        for (PortComposition currPortComposition : portCompositions) {
            getContainedPortCompositionsAndAllPorts(allAlreadyContainedPortCompositions, new LinkedHashSet<>(),
                    currPortComposition);
            allAlreadyContainedPortCompositions.add(currPortComposition);
        }

        //is already contained -> do nothing
        if (allAlreadyContainedPortCompositions.contains(pc)) {
            return false;
        }
        //not yet contained -> add it

        //but first remove it as direct child of a port group of a vertex if it was there before
        if (pc.getPortGroup() != null) {
            pc.getPortGroup().removePortComposition(pc);
        }
        else if (pc.getVertex() != null) {
            pc.getVertex().removePortComposition(pc);
        }

        portCompositions.add(pc);
        assignPortCompositionRecursivelyToVertex(pc, this);

        //find ports of newly added PortComposition
        updatePortsOfVertex(pc);

        return true;
    }

    /**
     *
     * @param pc
     *      the passed {@link PortComposition} does not need to be on the top level -- it can also be contained
     *      somewhere in the hierarchy tree of {@link PortGroup}s of this {@link Vertex}.
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
            LinkedHashSet<Port> currentPorts = new LinkedHashSet<>();
            for (PortComposition topLevelPortComposition : portCompositions) {
                getContainedPortCompositionsAndAllPorts(new LinkedHashSet<>(), currentPorts, topLevelPortComposition);
            }
            //compare this newly alive ports with the previously alive ports
            for (Port oldPort : new ArrayList<>(ports)) {
                if (!currentPorts.contains(oldPort)) {
                    success = success & removePortCleanly(oldPort);
                }
            }
        }

        return  success;
    }


    /*==========
     * Internal
     *==========*/

    protected void updatePortsOfVertex(PortComposition pc) {
        LinkedHashSet<Port> newPorts = new LinkedHashSet<>();
        getContainedPortCompositionsAndAllPorts(new LinkedHashSet<>(), newPorts, pc);
        this.ports.addAll(newPorts);
    }

    protected void removePortFromPortSet(Port p) {
        ports.remove(p);
    }

    private void getContainedPortCompositionsAndAllPorts(LinkedHashSet<PortComposition> allLowerLevelPortCompositions,
                                                         LinkedHashSet<Port> allPorts,
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
                return ((PortGroup) fromThis).removePortComposition(removeThis);
            }
            //not on this level contained but possibly on a lower level -> go into recursion
            for (PortComposition portComposition : ((PortGroup) fromThis).getPortCompositions()) {
                success = success | removeIfContained(removeThis, portComposition);
            }
            return success;
        }
        return false;
    }

    private boolean removePortCleanly(Port port) {
//        //remove it from possibly existing PortPairings
//        if (this.getVertexGroup() != null) {
//            for (PortPairing portPairing : new ArrayList<>(this.getVertexGroup().getPortPairings())) {
//                if (portPairing.getPorts().contains(port)) {
//                    this.getVertexGroup().removePortPairing(portPairing);
//                }
//            }
//        }
//
//        //remove references at edges
//        for (Edge edge : new ArrayList<>(port.getEdges())) {
//            edge.removePort(port);
//        }

        //remove it from the hash set storing the ports for convenience (easier querying)
        return this.ports.remove(port);
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


    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(LabeledObject o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return EqualLabeling.equalLabelingLists(new ArrayList<>(portCompositions),
                new ArrayList<>(vertex.portCompositions)) && labelManager.equalLabeling(vertex.labelManager) &&
                Objects.equals(reference, vertex.reference);
    }
}
