package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.uniwue.informatik.praline.datastructure.PropertyObject;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;
import de.uniwue.informatik.praline.datastructure.utils.InconsistentStateException;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * A {@link Port} is part of a {@link Vertex}.
 * It is something like the access points of a {@link Vertex} to the {@link Edge}s.
 * A {@link Vertex} is never directly connected to an {@link Edge} but only via {@link Port}s.
 * Typically there is one {@link Edge} per {@link Port}, but you may have arbitrarily many {@link Edge}s per
 * {@link Port} and you can also have {@link Port}s without {@link Edge}s.
 * <p>
 * You may wish to have a set of {@link Port}s of a {@link Vertex} next to each other.
 * You can do this by using a {@link PortGroup} containing these {@link Port}s; see there for more.
 * <p>
 * You may wish to have a coupling between two {@link Port}s of the same {@link Vertex} or of different vertices in the
 * way that they appear on the same vertical or horizontal line.
 * This can be done by using {@link PortPairing}s, which are stored in {@link VertexGroup}s.
 * <p>
 * A {@link Port} can be labeled.
 * In particular, if you want to assign a name or an ID to a {@link Port} you should use a
 * {@link de.uniwue.informatik.praline.datastructure.labels.TextLabel} attached to this {@link Port} and make it
 * the main label of this {@link Port}.
 * <p>
 * A {@link Port} should have a {@link Shape} in the end -- typically a
 * {@link de.uniwue.informatik.praline.datastructure.shapes.Rectangle}.
 * The idea is that a layouting algorithm will take a {@link Graph} and will set the coordinates and sizes of this
 * {@link Port}, so there is no need to set this in the forehand.
 * If this {@link Port} should not be visible (later a viewer should get the impression that an {@link Edge} / the
 * {@link Edge}s are directly accessing the {@link Vertex}), then use a {@link Shape} of size 0 for this port.
 */
@JsonIgnoreProperties({"vertex", "portGroup", "edges"})
@JsonPropertyOrder({"shape", "labelManager", "properties"})
public class Port implements PortComposition, ShapedObject, LabeledObject, ReferenceObject, PropertyObject {

    /*==========
     * Default values
     *==========*/

    public static final Shape DEFAULT_SHAPE_TO_BE_CLONED = new Rectangle();
    public static final Orientation DEFAULT_ORIENTATION_AT_VERTEX = Orientation.FREE;


    /*==========
     * Instance variables
     *==========*/

    private Vertex vertex;
    private Orientation orientationAtVertex = DEFAULT_ORIENTATION_AT_VERTEX;
    private PortGroup portGroup;
    private final List<Edge> edges;
    private final LabelManager labelManager;
    private Shape shape;
    private String reference;
    private final Map<String, String> properties;


    /*==========
     * Constructors
     *==========*/

    public Port() {
        this(null, null, null, null);
    }

    public Port(Collection<Edge> edges) {
        this(edges, null, null, null);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels) {
        this(edges, labels, null, null);
    }

    public Port(Collection<Edge> edges, Shape shape) {
        this(edges, null, null, shape);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels, Shape shape) {
        this(edges, labels, null, shape);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels, Label mainLabel, Shape shape) {
        this(edges, labels, mainLabel, shape, null);
    }

    @JsonCreator
    private Port(
            @JsonProperty("labelManager") final LabelManager labelManager,
            @JsonProperty("shape") final Shape shape,
            @JsonProperty("properties") final Map<String, String> properties
    ) {
        this(null, labelManager.getLabels(), labelManager.getMainLabel(), shape, properties);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels, Label mainLabel, Shape shape, Map<String, String> properties) {
        this.edges = newArrayListNullSafe(edges);
        for (Edge edge : this.edges) {
            edge.addPortButNotEdge(this);
        }
        this.labelManager = new LabelManager(this, labels, mainLabel);
        if (shape == null) {
            this.shape = DEFAULT_SHAPE_TO_BE_CLONED.clone();
        } else {
            this.shape = shape;
        }
        this.properties = new LinkedHashMap<>();
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }


    /*==========
     * Getters & Setters
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

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
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
    public String getReference() {
        return this.reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public Orientation getOrientationAtVertex() {
        return orientationAtVertex;
    }

    public void setOrientationAtVertex(Orientation orientationAtVertex) {
        this.orientationAtVertex = orientationAtVertex;
    }

    /*==========
     * Modifiers
     *==========*/

    /**
     * this {@link Port} is also added to the list of {@link Port}s of the passed {@link Edge} e
     *
     * @param e
     * @return true if {@link Edge} is added to the {@link Edge}s of this {@link Port} and false if the input parameter
     * is set to an {@link Edge} that is already associated with this {@link Port}.
     */
    public boolean addEdge(Edge e) {
        if (addEdgeButNotPort(e)) {
            if (!e.addPortButNotEdge(this)) {
                //TODO: maybe change this construction later (do real throwing methodwise or just use no exception)
                try {
                    throw new InconsistentStateException("Port " + this + " was already added to Edge " + e + ", but " +
                            "not the other way around");
                } catch (InconsistentStateException exception) {
                    exception.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * this {@link Port} is also removed from the list of {@link Port}s of the passed {@link Edge} e
     *
     * @param e
     * @return
     */
    public boolean removeEdge(Edge e) {
        e.removePortButNotEdge(this);
        return removeEdgeButNotPort(e);
    }

    protected boolean addEdgeButNotPort(Edge e) {
        if (!edges.contains(e)) {
            edges.add(e);
            return true;
        }
        return false;
    }

    protected boolean removeEdgeButNotPort(Edge e) {
        return edges.remove(e);
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
        return equalLabelingInternal(o);
    }

    @Override
    public boolean equalLabeling(PortComposition o) {
        return equalLabelingInternal(o);
    }

    private boolean equalLabelingInternal(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Port port = (Port) o;
        return (labelManager.equalLabeling(port.labelManager) && Objects.equals(reference, port.reference));
    }
}
