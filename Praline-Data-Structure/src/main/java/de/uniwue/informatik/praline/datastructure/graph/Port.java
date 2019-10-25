package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;
import de.uniwue.informatik.praline.datastructure.util.InconsistentStateException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class Port implements PortComposition, ShapedObject, LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final Shape DEFAULT_SHAPE_TO_BE_CLONED = new Rectangle();

    /*==========
     * Instance variables
     *==========*/

    private List<Edge> edges;
    private List<Label> labels;
    private Shape shape;
    private Label mainLabel;

    /*==========
     * Constructors
     *==========*/

    public Port() {
        this(null, null, null);
    }

    public Port(Collection<Edge> edges) {
        this(edges, null, null);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels) {
        this(edges, labels, null);
    }

    public Port(Collection<Edge> edges, Shape shape) {
        this(edges, null, shape);
    }

    public Port(Collection<Edge> edges, Collection<Label> labels, Shape shape) {
        this.edges = newArrayListNullSave(edges);
        this.labels = new ArrayList<>();
        this.addAllLabels(labels);
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

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public Collection<Label> getLabels() {
        return Collections.unmodifiableList(labels);
    }


    @Override
    public Label getMainLabel() {
        return mainLabel;
    }

    @Override
    public boolean setMainLabel(Label mainLabel) {
        if (!labels.contains(mainLabel)) {
            if (!addLabel(mainLabel)) {
                return false;
            }
        }
        this.mainLabel = mainLabel;
        return true;
    }

    /*==========
     * Modifiers
     *==========*/

    /**
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    public boolean addAllLabels(Collection<Label> labels) {
        boolean success = true;
        for (Label label : labels) {
            success = success & this.addLabel(label);
        }
        return success;
    }

    @Override
    public boolean addLabel(Label l) {
        if (!labels.contains(l)) {
            labels.add(l);
            LabeledObject.changeAssociatedObject(l, this);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeLabel(Label l) {
        if (labels.contains(l)) {
            labels.remove(l);
            LabeledObject.justRemoveAssociatedObject(l);

            if (mainLabel.equals(l)) {
                findNewMainLabel();
            }
            return true;
        }
        return false;
    }

    private void findNewMainLabel() {
        if (labels.size() == 1) {
            mainLabel = labels.get(0);
        } else {
            mainLabel = null;
        }
    }

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
}
