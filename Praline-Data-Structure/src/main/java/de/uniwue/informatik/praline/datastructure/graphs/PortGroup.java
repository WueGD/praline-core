package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.uniwue.informatik.praline.datastructure.PropertyObject;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * A {@link PortGroup} provides the possibility to arrange the {@link Port}s of a {@link Vertex} combinatorially in
 * only some specific ways.
 * More precisely, a {@link PortGroup} may contain some {@link Port}s and some {@link PortGroup}s (subsumed as
 * {@link PortComposition}) and the {@link Port}s contained in this {@link PortGroup} and all {@link Port}s in all
 * {@link PortGroup}s contained recursively in this {@link PortGroup} must be placed next to each other.
 * Inside a {@link PortGroup} its elements may be arranged freely (if {@link PortGroup#isOrdered()} == false) or
 * arranged as prescribed by the list {@link PortGroup#getPortCompositions()} (if {@link PortGroup#isOrdered()} ==
 * true).
 *
 * A {@link PortGroup} provides the possibility to have {@link PortGroup}s inside {@link PortGroup}s inside
 * {@link PortGroup}s and so on.
 * To keep this manageable, it is assumed that {@link PortGroup}s build trees (and not more complicated graphs) with
 * each element of a tree appearing only once in such a tree.
 * The user giving the input has to take care for this.
 * In such a tree, the {@link PortGroup}s are the inner nodes and the {@link Port}s are the leaves.
 *
 * Different from {@link Port}s, {@link PortGroup}s are *not*
 * {@link de.uniwue.informatik.praline.datastructure.shapes.ShapedObject}s because they represent only the possible
 * logical/combinatorial arrangements of {@link Port}s and not their concrete outline in a drawing.
 * They are also not {@link de.uniwue.informatik.praline.datastructure.labels.LabeledObject}s.
 */
@JsonPropertyOrder({ "ordered", "portCompositions" })
public class PortGroup implements PortComposition, ReferenceObject, PropertyObject {

    /*==========
     * Default values
     *==========*/
    public static final boolean DEFAULT_IS_ORDERED = false;


    /*==========
     * Instance variables
     *==========*/

    private Vertex vertex;
    private PortGroup portGroup;
    private final List<PortComposition> portCompositions;
    private boolean ordered;
    private String reference;
    private final Map<String, String> properties = new LinkedHashMap<>();


    /*==========
     * Constructors
     *==========*/

    public PortGroup() {
        this(null, DEFAULT_IS_ORDERED);
    }

    public PortGroup(boolean isOrdered) {
        this(null, isOrdered);
    }

    public PortGroup(int numberOfPortsPlannedToAdd) {
        portCompositions = new ArrayList<>(numberOfPortsPlannedToAdd);
        ordered = DEFAULT_IS_ORDERED;
    }

    public PortGroup(Collection<PortComposition> portCompositions) {
        this(portCompositions, DEFAULT_IS_ORDERED);
    }

    @JsonCreator
    public PortGroup(
            @JsonProperty("portCompositions") final Collection<PortComposition> portCompositions,
            @JsonProperty("ordered") final boolean ordered
    ) {
        this.ordered = ordered;
        this.portCompositions = new ArrayList<>();
        for (PortComposition portComposition : newArrayListNullSafe(portCompositions)) {
            addPortComposition(portComposition);
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


    /*==========
     * Modifiers
     *==========*/

    public void addPortComposition(PortComposition pc) {
        addPortComposition(this.getPortCompositions().size(), pc); //append to the end of the list
    }

    public void addPortComposition(int position, PortComposition pc) {
        //remove it as direct child of a port group of a vertex if it was there before
        if (pc.getPortGroup() != null) {
            pc.getPortGroup().removePortComposition(pc);
        }
        else if (pc.getVertex() != null) {
            pc.getVertex().removePortComposition(pc);
        }
        portCompositions.add(position, pc);
        setVertexRecursivelyToAllPortCompositions(pc, this.getVertex());
        pc.setPortGroup(this);
    }

    public boolean removePortComposition(PortComposition pc) {
        pc.setPortGroup(null);
        setVertexRecursivelyToAllPortCompositions(pc, null);
        return portCompositions.remove(pc);
    }


    /*==========
     * Internal
     *==========*/

    private static void setVertexRecursivelyToAllPortCompositions(PortComposition pc, Vertex vertex) {
        //potentially remove from port set of vertex
        if (vertex == null && pc.getVertex() != null && pc instanceof Port) {
            pc.getVertex().removePortFromPortSet((Port) pc);
        }

        pc.setVertex(vertex);

        //potentially add to port set of vertex
        if (vertex != null) {
            vertex.updatePortsOfVertex(pc);
        }

        if (pc instanceof PortGroup) {
            for (PortComposition groupMember : ((PortGroup)pc).getPortCompositions()) {
                setVertexRecursivelyToAllPortCompositions(groupMember, vertex);
            }
        }
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

    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(PortComposition o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortGroup portGroup = (PortGroup) o;
        return  ordered == portGroup.ordered && EqualLabeling.equalLabelingLists(new ArrayList<>(portCompositions),
                new ArrayList<>(portGroup.portCompositions)) && Objects.equals(reference, portGroup.reference);
    }
}
