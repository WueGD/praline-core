package de.uniwue.informatik.praline.datastructure.graphs;

import de.uniwue.informatik.praline.datastructure.labels.EdgeLabelManager;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.utils.InconsistentStateException;

import java.awt.*;
import java.util.*;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

public class Edge implements LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final double UNSPECIFIED_THICKNESS = -1;
    public static final Color DEFAULT_COLOR = Color.BLACK;


    /*==========
     * Instance variables
     *==========*/

    private final List<Port> ports;
    private List<Path> paths;
    /**
     * -1 for not specified
     */
    private double thickness;
    private Color color;
    private EdgeBundle edgeBundle;
    private final EdgeLabelManager labelManager;


    /*==========
     * Constructors
     *==========*/

    public Edge(Collection<Port> ports) {
        this(ports, null, null, null, Edge.UNSPECIFIED_THICKNESS, Edge.DEFAULT_COLOR);
    }

    /**
     * leave value as null if it should be empty initially (e.g. no labels)
     *
     * @param ports
     * @param innerLabels
     * @param portLabels
     * @param mainLabel
     * @param thickness
     *      -1 for not specified
     * @param color
     */
    public Edge(Collection<Port> ports, Collection<Label> innerLabels, Map<Port, List<Label>> portLabels,
                Label mainLabel, double thickness, Color color) {
        this.ports = newArrayListNullSave(ports);
        this.labelManager = new EdgeLabelManager(this, innerLabels, portLabels, mainLabel);
        this.thickness = thickness;
        this.color = color;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<Port> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void setPaths(List<Path> paths) {
        this.paths = paths;
    }

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public EdgeBundle getEdgeBundle() {
        return edgeBundle;
    }

    protected void setEdgeBundle(EdgeBundle edgeBundle) {
        this.edgeBundle = edgeBundle;
    }

    @Override
    public EdgeLabelManager getLabelManager() {
        return labelManager;
    }


    /*==========
     * Modifiers
     *==========*/

    /**
     * this {@link Edge} is also added to the list of {@link Edge}s of the passed {@link Port} p
     *
     * @param p
     * @return
     *      true if {@link Port} is added to the {@link Port}s of this {@link Edge} and false if the input parameter
     *      is set to an {@link Port} that is already associated with this {@link Edge}.
     */
    public boolean addPort(Port p) {
        if (addPortButNotEdge(p)) {
            if (!p.addEdgeButNotPort(this)) {
                //TODO: maybe change this construction later (do real throwing methodwise or just use no exception)
                try {
                    throw new InconsistentStateException("Edge " + this + " was already added to Port " + p + ", but " +
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
     * this {@link Edge} is also removed from the list of {@link Edge}s of the passed {@link Port} p
     *
     * @param p
     * @return
     */
    public boolean removePort(Port p) {
        p.removeEdgeButNotPort(this);
        return removePortButNotEdge(p);
    }

    protected boolean addPortButNotEdge(Port p) {
        if (!ports.contains(p)) {
            ports.add(p);
            return true;
        }
        return false;
    }

    protected boolean removePortButNotEdge(Port p) {
        return ports.remove(p);
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }
}
