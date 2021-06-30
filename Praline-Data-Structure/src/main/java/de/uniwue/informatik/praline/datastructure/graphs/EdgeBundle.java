package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.PropertyObject;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * Via an {@link EdgeBundle} you may group {@link Edge}s and further {@link EdgeBundle}s together.
 * {@link EdgeBundle}s should build a tree-structure (and not something more complicated).
 * In typical applications they may represent bundles of wires or cables.
 *
 * {@link EdgeBundle}s do not have an own course via {@link de.uniwue.informatik.praline.datastructure.paths.Path}s,
 * but a layouting algorithm should place the {@link de.uniwue.informatik.praline.datastructure.paths.Path}s of all
 * {@link Edge}s of an {@link EdgeBundle} close together to obtain the effect of a bundled set of edges.
 *
 * An {@link EdgeBundle} may have labels, but just for the whole thing -- for placing {@link Label}s close to
 * {@link Port}s use the labeling of its contained {@link Edge}s.
 */
@JsonIgnoreProperties({ "allRecursivelyContainedEdges", "allRecursivelyContainedEdgeBundles", "edgeBundle" })
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class EdgeBundle implements LabeledObject, ReferenceObject, PropertyObject {

    /*==========
     * Instance variables
     *==========*/

    private final List<Edge> containedEdges;
    private final List<EdgeBundle> containedEdgeBundles;
    private EdgeBundle edgeBundle;
    private final LabelManager labelManager;
    private String reference;
    private final Map<String, String> properties = new HashMap<>();


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
        for (Edge e : this.containedEdges) {
            e.setEdgeBundle(this);
        }
        this.containedEdgeBundles = newArrayListNullSafe(containedEdgeBundles);
        for (EdgeBundle eb : this.containedEdgeBundles) {
            eb.setEdgeBundle(this);
        }
        this.labelManager = new LabelManager(this, labels, mainlabel);
    }


    /*==========
     * Getters & Setters
     *==========*/

    /**
     * Differs from {@link EdgeBundle#getAllRecursivelyContainedEdges()}
     *
     * @return
     *      {@link Edge}es contained directly in this {@link EdgeBundle}. Note that {@link Edge}s contained in an
     *      {@link EdgeBundle} of this {@link EdgeBundle} are not returned
     */
    public List<Edge> getContainedEdges() {
        return Collections.unmodifiableList(containedEdges);
    }

    /**
     * Differs from {@link EdgeBundle#getContainedEdges()}
     *
     * @return
     *      {@link Edge}es contained directly in this {@link EdgeBundle} and contained in any {@link EdgeBundle}
     *      contained in this {@link EdgeBundle} or even deeper (with arbitrary depth)
     */
    public List<Edge> getAllRecursivelyContainedEdges() {
        List<Edge> allEdges = new ArrayList<>(containedEdges);
        for (EdgeBundle containedEdgeBundle : containedEdgeBundles) {
            allEdges.addAll(containedEdgeBundle.getAllRecursivelyContainedEdges());
        }
        return allEdges;
    }

    /**
     * Differs from {@link EdgeBundle#getAllRecursivelyContainedEdgeBundles()}
     *
     * @return
     *      {@link EdgeBundle}es contained directly in this {@link EdgeBundle}. Note that {@link EdgeBundle}s
     *      contained in an {@link EdgeBundle} of this {@link EdgeBundle} are not returned
     */
    public List<EdgeBundle> getContainedEdgeBundles() {
        return Collections.unmodifiableList(containedEdgeBundles);
    }

    /**
     * Differs from {@link EdgeBundle#getContainedEdgeBundles()}
     *
     * @return
     *      {@link EdgeBundle}es contained directly in this {@link EdgeBundle} and contained in any {@link EdgeBundle}
     *      contained in this {@link EdgeBundle} or even deeper (with arbitrary depth)
     */
    public List<EdgeBundle> getAllRecursivelyContainedEdgeBundles() {
        List<EdgeBundle> allEdgeBundles = new ArrayList<>();
        for (EdgeBundle containedEdgeBundle : containedEdgeBundles) {
            allEdgeBundles.add(containedEdgeBundle);
            allEdgeBundles.addAll(containedEdgeBundle.getAllRecursivelyContainedEdgeBundles());
        }
        return allEdgeBundles;
    }

    public EdgeBundle getEdgeBundle() {
        return edgeBundle;
    }

    protected void setEdgeBundle(EdgeBundle edgeBundle) {
        this.edgeBundle = edgeBundle;
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

    public boolean addEdge(Edge e) {
        if (this.getAllRecursivelyContainedEdges().contains(e)) {
            return false;
        }
        containedEdges.add(e);
        e.setEdgeBundle(this);
        return true;
    }

    public boolean addEdges(Collection<Edge> edges) {
        boolean success = true;
        for (Edge e : edges) {
            success &= addEdge(e);
        }
        return success;
    }

    /**
     * Removes an {@link Edge} from this {@link EdgeBundle} or from some recursively contained
     * {@link EdgeBundle}
     *
     * @param e
     *      to be removed from this {@link EdgeBundle}
     * @return
     *      success
     */
    public boolean removeEdge(Edge e) {
        boolean success = containedEdges.remove(e);
        if (success) {
            e.setEdgeBundle(null);
        }

        //recursive call to edge bundles inside this edge bundle
        for (EdgeBundle containedEdgeBundle : containedEdgeBundles) {
            success |= containedEdgeBundle.removeEdge(e);
        }

        return success;
    }

    public boolean addEdgeBundle(EdgeBundle eb) {
        if (this.getAllRecursivelyContainedEdgeBundles().contains(eb)) {
            return false;
        }
        containedEdgeBundles.add(eb);
        eb.setEdgeBundle(this);
        return true;
    }

    /**
     * Removes an {@link EdgeBundle} from this {@link EdgeBundle} or from some recursively contained
     * {@link EdgeBundle}
     *
     * @param eb
     *      to be removed from this {@link EdgeBundle}
     * @return
     *      success
     */
    public boolean removeEdgeBundle(EdgeBundle eb) {
        boolean success = containedEdgeBundles.remove(eb);
        if (success) {
            eb.setEdgeBundle(null);
        }

        //recursive call to edge bundles inside this edge bundle
        for (EdgeBundle containedEdgeBundle : containedEdgeBundles) {
            success |= containedEdgeBundle.removeEdgeBundle(eb);
        }

        return success;
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
        EdgeBundle that = (EdgeBundle) o;
        return EqualLabeling.equalLabelingLists(new ArrayList<>(containedEdges),
                new ArrayList<>(that.containedEdges)) && EqualLabeling.equalLabelingLists(
                        new ArrayList<>(containedEdgeBundles), new ArrayList<>(that.containedEdgeBundles)) &&
                labelManager.equalLabeling(that.labelManager) && Objects.equals(reference, that.reference);
    }
}
