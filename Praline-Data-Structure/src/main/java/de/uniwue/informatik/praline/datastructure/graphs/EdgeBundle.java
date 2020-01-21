package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class EdgeBundle implements LabeledObject {

    /*==========
     * Instance variables
     *==========*/

    private final List<Edge> containedEdges;
    private final List<EdgeBundle> containedEdgeBundles;
    private final LabelManager labelManager;


    /*==========
     * Constructors
     *==========*/

    public EdgeBundle() {
        this(null, null, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges) {
        this(containedEdges, null, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles) {
        this(containedEdges, containedEdgeBundles, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles,
                      Collection<Label> labels) {
        this(containedEdges, containedEdgeBundles, labels, null);
    }

    @JsonCreator
    private EdgeBundle(
            @JsonProperty("containedEdges") final Collection<Edge> containedEdges,
            @JsonProperty("containedEdgeBundles") final Collection<EdgeBundle> containedEdgeBundles,
            @JsonProperty("labelManager") final LabelManager labelManager
    ) {
        this(containedEdges, containedEdgeBundles, labelManager.getLabels(), labelManager.getMainLabel());
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles,
                      Collection<Label> labels, Label mainlabel) {
        this.containedEdges = newArrayListNullSafe(containedEdges);
        for (Edge e : containedEdges) {
            e.setEdgeBundle(this);
        }
        this.containedEdgeBundles = newArrayListNullSafe(containedEdgeBundles);
        this.labelManager = new LabelManager(this, labels, mainlabel);
    }


    /*==========
     * Getters
     *==========*/

    public List<Edge> getContainedEdges() {
        return Collections.unmodifiableList(containedEdges);
    }

    public List<EdgeBundle> getContainedEdgeBundles() {
        return Collections.unmodifiableList(containedEdgeBundles);
    }

    @Override
    public LabelManager getLabelManager() {
        return labelManager;
    }


    /*==========
     * Modifiers
     *==========*/

    public void addEdge(Edge e) {
        containedEdges.add(e);
        e.setEdgeBundle(this);
    }

    public boolean removeEdge(Edge e) {
        boolean success = containedEdges.remove(e);
        if (success) {
            e.setEdgeBundle(null);
        }
        return success;
    }

    public void addEdgeBundle(EdgeBundle eb) {
        containedEdgeBundles.add(eb);
    }

    public boolean removeEdgeBundle(EdgeBundle eb) {
        return containedEdgeBundles.remove(eb);
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }
}
