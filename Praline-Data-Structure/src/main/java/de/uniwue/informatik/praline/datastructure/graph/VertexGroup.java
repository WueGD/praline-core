package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class VertexGroup implements ShapedObject, LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final boolean DEFAULT_DRAW_FRAME = false;

    /*==========
     * Instance variables
     *==========*/

    private List<Vertex> containedVertices;
    private List<VertexGroup> containedVertexGroups;
    private List<TouchingPair> touchingPairs;
    private List<PortPairing> portPairings;
    private List<Label> labels;
    private Label mainLabel;
    /**
     * null if this group has no specific shape
     */
    private Shape shape;
    private boolean drawnFrame;


    /*==========
     * Constructors
     *==========*/

    public VertexGroup() {
        this(null, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    public VertexGroup(Collection<Vertex> containedVertices) {
        this(containedVertices, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    /**
     * Set parameter to null if a {@link VertexGroup} should be initialized without these objects (e.g. without
     * portPairings)
     */
    public VertexGroup(Collection<Vertex> containedVertices, Collection<VertexGroup> containedVertexGroups,
                       Collection<TouchingPair> touchingPairs, Collection<PortPairing> portPairings,
                       Collection<Label> labels, Shape shape, boolean drawnFrame) {
        this.containedVertices = newArrayListNullSave(containedVertices);
        this.containedVertexGroups = newArrayListNullSave(containedVertexGroups);
        this.touchingPairs = newArrayListNullSave(touchingPairs);
        this.portPairings = newArrayListNullSave(portPairings);
        this.labels = new ArrayList<>();
        this.addAllLabels(labels);
        this.shape = shape;
        this.drawnFrame = drawnFrame;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<Vertex> getContainedVertices() {
        return containedVertices;
    }

    public List<VertexGroup> getContainedVertexGroups() {
        return containedVertexGroups;
    }

    public List<TouchingPair> getTouchingPairs() {
        return touchingPairs;
    }

    public List<PortPairing> getPortPairings() {
        return portPairings;
    }

    @Override
    public List<Label> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    @Override
    public Shape getShape() {
        return shape;
    }

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
     *
     * Modificiations of the lists currently by List get***()
     * (except for labels)
     * this maybe changed later:
     * make explicit add() and remove() methods and
     * add "Collections.unmodifiableList(...)" to getters
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
}
