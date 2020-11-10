package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * A {@link VertexGroup} provides the possibility to arrange several instances of {@link Vertex} together.
 * There are different levels of connectedness.
 * If you use a {@link VertexGroup} just with a list of {@link Vertex}es (vertices) it represents a loose connection of
 * them and
 * a layouting algorithm should take care to place them close to each other.
 * You can have frame around these {@link Vertex}es, which is handled with a {@link Shape}.
 *
 * If you add {@link TouchingPair}s, it pairs two {@link Vertex}es together, i.e., within this group, the algorithm
 * should draw them such that they touch (see there for more).
 *
 * Moreover, you can define a connection between two {@link Port}s (of possibly different {@link Vertex}es) within this
 * {@link VertexGroup}. They should be drawn on the same horizontal or vertical line.
 *
 * A {@link VertexGroup} may have {@link Label}s.
 */
@JsonIgnoreProperties({ "allRecursivelyContainedVertices", "allRecursivelyContainedVertexGroups" })
@JsonPropertyOrder({ "drawnFrame", "labelManager", "shape", "containedVertices", "containedVertexGroups",
        "touchingPairs", "portPairings" })
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class VertexGroup implements ShapedObject, LabeledObject, ReferenceObject {

    /*==========
     * Default values
     *==========*/

    public static final boolean DEFAULT_DRAW_FRAME = false;


    /*==========
     * Instance variables
     *==========*/

    private final List<Vertex> containedVertices;
    private final List<VertexGroup> containedVertexGroups;
    private final List<TouchingPair> touchingPairs;
    private final List<PortPairing> portPairings;
    private final LabelManager labelManager;
    /**
     * null if this group has no specific shape
     */
    private Shape shape;
    private boolean drawnFrame;
    private String reference;


    /*==========
     * Constructors
     *==========*/

    public VertexGroup() {
        this(null, null, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    public VertexGroup(Collection<Vertex> containedVertices) {
        this(containedVertices, null, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    @JsonCreator
    private VertexGroup(
            @JsonProperty("drawnFrame") final boolean drawnFrame,
            @JsonProperty("labelManager") final LabelManager labelManager,
            @JsonProperty("shape") final Shape shape,
            @JsonProperty("containedVertices") final Collection<Vertex> containedVertices,
            @JsonProperty("containedVertexGroups") final Collection<VertexGroup> containedVertexGroups,
            @JsonProperty("touchingPairs") final Collection<TouchingPair> touchingPairs,
            @JsonProperty("portPairings") final Collection<PortPairing> portPairings
    ) {
        this(containedVertices, containedVertexGroups, touchingPairs, portPairings, labelManager.getLabels(),
                labelManager.getMainLabel(), shape, drawnFrame);
    }

    /**
     * Set parameter to null if a {@link VertexGroup} should be initialized without these objects (e.g. without
     * portPairings)
     */
    public VertexGroup(Collection<Vertex> containedVertices, Collection<VertexGroup> containedVertexGroups,
                       Collection<TouchingPair> touchingPairs, Collection<PortPairing> portPairings,
                       Collection<Label> labels, Label mainLabel, Shape shape, boolean drawnFrame) {
        this.containedVertices = newArrayListNullSafe(containedVertices);
        for (Vertex v : this.containedVertices) {
            v.setVertexGroup(this);
        }
        this.containedVertexGroups = newArrayListNullSafe(containedVertexGroups);
        this.touchingPairs = newArrayListNullSafe(touchingPairs);
        this.portPairings = newArrayListNullSafe(portPairings);
        this.labelManager = new LabelManager(this, labels, mainLabel);
        this.shape = shape;
        this.drawnFrame = drawnFrame;
    }


    /*==========
     * Getters & Setters
     *
     * Currently: Modify a list by its getter
     *==========*/

    /**
     * Differs from {@link VertexGroup#getAllRecursivelyContainedVertices()}
     *
     * @return
     *      {@link Vertex}es contained directly in this {@link VertexGroup}. Note that vertices contained in a
     *      {@link VertexGroup} of this {@link VertexGroup} are not returned
     */
    public List<Vertex> getContainedVertices() {
        return Collections.unmodifiableList(containedVertices);
    }

    /**
     * Differs from {@link VertexGroup#getContainedVertices()}
     *
     * @return
     *      {@link Vertex}es contained directly in this {@link VertexGroup} and contained in any {@link VertexGroup}
     *      contained in this {@link VertexGroup} or even deeper (with arbitrary depth)
     */
    public List<Vertex> getAllRecursivelyContainedVertices() {
        List<Vertex> allVertices = new ArrayList<>(containedVertices);
        for (VertexGroup containedVertexGroup : containedVertexGroups) {
            allVertices.addAll(containedVertexGroup.getAllRecursivelyContainedVertices());
        }
        return allVertices;
    }

    /**
     * Differs from {@link VertexGroup#getAllRecursivelyContainedVertexGroups()}
     *
     * @return
     *      {@link VertexGroup}s contained directly in this {@link VertexGroup}. Note that {@link VertexGroup}s
     *      contained in a {@link VertexGroup} of this {@link VertexGroup} are not returned
     */
    public List<VertexGroup> getContainedVertexGroups() {
        return Collections.unmodifiableList(containedVertexGroups);
    }

    /**
     * Differs from {@link VertexGroup#getContainedVertexGroups()}
     *
     * @return
     *      {@link VertexGroup}s contained directly in this {@link VertexGroup} and contained in any {@link VertexGroup}
     *      contained in this {@link VertexGroup} or even deeper (with arbitrary depth)
     */
    public List<VertexGroup> getAllRecursivelyContainedVertexGroups() {
        List<VertexGroup> allVertexGroups = new ArrayList<>();
        for (VertexGroup containedVertexGroup : containedVertexGroups) {
            allVertexGroups.add(containedVertexGroup);
            allVertexGroups.addAll(containedVertexGroup.getAllRecursivelyContainedVertexGroups());
        }
        return allVertexGroups;
    }

    public List<TouchingPair> getTouchingPairs() {
        return Collections.unmodifiableList(touchingPairs);
    }

    public List<PortPairing> getPortPairings() {
        return Collections.unmodifiableList(portPairings);
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public boolean isDrawnFrame() {
        return drawnFrame;
    }

    public void setDrawnFrame(boolean drawnFrame) {
        this.drawnFrame = drawnFrame;
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


    /*==========
     * Modifiers
     *==========*/

    public void addVertex(Vertex v) {
        containedVertices.add(v);
        v.setVertexGroup(this);
    }
    /**
     * Removes a {@link Vertex} from this {@link VertexGroup} or from some recursively contained {@link VertexGroup}.
     * It also removes all {@link TouchingPair}s and {@link PortPairing}s where this {@link Vertex} is involved.
     *
     * @param v
     *      to be removed from this {@link VertexGroup}
     * @return
     *      success
     */
    public boolean removeVertex(Vertex v) {
        boolean success = containedVertices.remove(v);
        if (success) {
            v.setVertexGroup(null);
        }
        //remove from touching pairs and its ports from port pairings
        for (TouchingPair touchingPair : new ArrayList<>(touchingPairs)) {
            if (touchingPair.getVertex0().equals(v) || touchingPair.getVertex1().equals(v)) {
                touchingPairs.remove(touchingPair);
            }
        }
        for (PortPairing portPairing : new ArrayList<>(portPairings)) {
            if (portPairing.getPort0().getVertex().equals(v) || portPairing.getPort1().getVertex().equals(v)) {
                portPairings.remove(portPairing);
            }
        }

        //recursive call to vertex groups inside this vertex group
        for (VertexGroup containedVertexGroup : containedVertexGroups) {
            success |= containedVertexGroup.removeVertex(v);
        }

        return success;
    }

    public void addVertexGroup(VertexGroup vg) {
        containedVertexGroups.add(vg);
    }

    /**
     * Removes a {@link VertexGroup} from this {@link VertexGroup} or from some recursively contained
     * {@link VertexGroup}
     *
     * @param vg
     *      to be removed from this {@link VertexGroup}
     * @return
     *      success
     */
    public boolean removeVertexGroup(VertexGroup vg) {
        boolean success = containedVertexGroups.remove(vg);

        //recursive call to vertex groups inside this vertex group
        for (VertexGroup containedVertexGroup : containedVertexGroups) {
            success |= containedVertexGroup.removeVertexGroup(vg);
        }

        return success;
    }

    public boolean addTouchingPair(TouchingPair tp) {
        if (containedVertices.contains(tp.getVertex0()) && containedVertices.contains(tp.getVertex1())) {
            touchingPairs.add(tp);
            return true;
        }
        return false;
    }

    public boolean removeTouchingPair(TouchingPair tp) {
        return touchingPairs.remove(tp);
    }

    /**
     *
     * @param pp
     *      Each of both ports of this {@link PortPairing} must be contained in a {@link Vertex} of
     *      this {@link VertexGroup}
     * @return
     *      success
     */
    public boolean addPortPairing(PortPairing pp) {
        List<Port> allPortsOfTopLevelVertices = new ArrayList<>();
        for (Vertex v : containedVertices) {
            allPortsOfTopLevelVertices.addAll(v.getPorts());
        }
        if (allPortsOfTopLevelVertices.contains(pp.getPort0()) && allPortsOfTopLevelVertices.contains(pp.getPort1())) {
            portPairings.add(pp);
            return true;
        }
        return false;
    }

    public boolean removePortPairing(PortPairing pp) {
        return portPairings.remove(pp);
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
        VertexGroup that = (VertexGroup) o;
        return drawnFrame == that.drawnFrame && EqualLabeling.equalLabelingLists(new ArrayList<>(containedVertices),
                new ArrayList<>(that.containedVertices)) && EqualLabeling.equalLabelingLists(
                new ArrayList<>(containedVertexGroups), new ArrayList<>(that.containedVertexGroups)) &&
                EqualLabeling.equalLabelingLists(new ArrayList<>(touchingPairs), new ArrayList<>(that.touchingPairs)) &&
                EqualLabeling.equalLabelingLists(new ArrayList<>(portPairings), new ArrayList<>(that.portPairings)) &&
                labelManager.equalLabeling(that.labelManager) && Objects.equals(reference, that.reference);
    }
}
