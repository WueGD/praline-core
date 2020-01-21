package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;
import de.uniwue.informatik.praline.datastructure.utils.InconsistentStateException;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

@JsonIgnoreProperties({ "vertex", "portGroup", "edges" })
@JsonPropertyOrder({ "shape", "labelManager" })
public class Port implements PortComposition, ShapedObject, LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final Shape DEFAULT_SHAPE_TO_BE_CLONED = new Rectangle();


    /*==========
     * Instance variables
     *==========*/

    private Vertex vertex;
    private PortGroup portGroup;
    private final List<Edge> edges;
    private final LabelManager labelManager;
    private Shape shape;


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

    @JsonCreator
    private Port(
            @JsonProperty("labelManager") final LabelManager labelManager,
            @JsonProperty("shape") final Shape shape
    ) {
        this(null, labelManager.getLabels(), labelManager.getMainLabel(), shape);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels, Label mainLabel, Shape shape) {
        this.edges = newArrayListNullSafe(edges);
        for (Edge edge : this.edges) {
            edge.addPortButNotEdge(this);
        }
        this.labelManager = new LabelManager(this, labels, mainLabel);
        if (shape == null) {
            this.shape = DEFAULT_SHAPE_TO_BE_CLONED.clone();
        }
        else {
            this.shape = shape;
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


    /*==========
     * Modifiers
     *==========*/

    /**
     * this {@link Port} is also added to the list of {@link Port}s of the passed {@link Edge} e
     *
     * @param e
     * @return
     *      true if {@link Edge} is added to the {@link Edge}s of this {@link Port} and false if the input parameter
     *      is set to an {@link Edge} that is already associated with this {@link Port}.
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
}
