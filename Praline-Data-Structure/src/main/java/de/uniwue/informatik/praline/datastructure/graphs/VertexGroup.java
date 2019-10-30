package de.uniwue.informatik.praline.datastructure.graphs;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

import java.util.Collection;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

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
    private LabelManager labelManager;
    /**
     * null if this group has no specific shape
     */
    private Shape shape;
    private boolean drawnFrame;


    /*==========
     * Constructors
     *==========*/

    public VertexGroup() {
        this(null, null, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    public VertexGroup(Collection<Vertex> containedVertices) {
        this(containedVertices, null, null, null, null, null, null, DEFAULT_DRAW_FRAME);
    }

    /**
     * Set parameter to null if a {@link VertexGroup} should be initialized without these objects (e.g. without
     * portPairings)
     */
    public VertexGroup(Collection<Vertex> containedVertices, Collection<VertexGroup> containedVertexGroups,
                       Collection<TouchingPair> touchingPairs, Collection<PortPairing> portPairings,
                       Collection<Label> labels, Label mainLabel, Shape shape, boolean drawnFrame) {
        this.containedVertices = newArrayListNullSave(containedVertices);
        this.containedVertexGroups = newArrayListNullSave(containedVertexGroups);
        this.touchingPairs = newArrayListNullSave(touchingPairs);
        this.portPairings = newArrayListNullSave(portPairings);
        this.labelManager = new LabelManager(this, labels, mainLabel);
        this.shape = shape;
        this.drawnFrame = drawnFrame;
    }


    /*==========
     * Getters & Setters
     *
     * Currently: Modify a list by its getter
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
    public LabelManager getLabelManager() {
        return labelManager;
    }


    /*==========
     * TODO:
     * Modifiers
     *
     * Modificiations of the lists currently by List get***()
     * this maybe changed later:
     * make explicit add() and remove() methods and
     * add "Collections.unmodifiableList(...)" to getters
     *
     * Add consistency checks in add/remove methods (e. g. Ports of PortPairings are contained etc.)
     *==========*/


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }
}
