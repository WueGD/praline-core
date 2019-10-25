package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.util.InconsistentStateException;

import java.awt.*;
import java.util.*;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class Edge implements LabeledObject {

    /*==========
     * Default values
     *==========*/

    public static final double UNSPECIFIED_THICKNESS = -1;
    public static final Color DEFAULT_COLOR = Color.BLACK;

    /*==========
     * Instance variables
     *==========*/

    private List<Port> ports;
    private List<Path> paths;
    private List<Label> innerLabels;
    private Map<Port, List<Label>> portLabels;
    private Label mainLabel;
    /**
     * -1 for not specified
     */
    private double thickness;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public Edge(Collection<Port> ports) {
        this(ports, null, null, Edge.UNSPECIFIED_THICKNESS, Edge.DEFAULT_COLOR);
    }

    /**
     * leave value as null if it should be empty initially (e.g. no labels)
     *
     * @param ports
     * @param innerLabels
     * @param portLabels
     * @param thickness
     *      -1 for not specified
     * @param color
     */
    public Edge(Collection<Port> ports, Collection<Label> innerLabels, Map<Port, List<Label>> portLabels,
                double thickness, Color color) {
        this.ports = newArrayListNullSave(ports);
        this.innerLabels = new ArrayList<>();
        this.addAllInnerLabels(innerLabels);
        this.portLabels = new HashMap<>(portLabels.size());
        for (Port port : portLabels.keySet()) {
            if (portLabels.get(port) != null){
                addAllPortLabels(port, portLabels.get(port));
            }
        }
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

    public List<Label> getInnerLabels() {
        return innerLabels;
    }

    public Map<Port, List<Label>> getPortLabels() {
        return portLabels;
    }

    @Override
    public List<Label> getLabels() {
        ArrayList<Label> labels = new ArrayList<>();
        labels.addAll(innerLabels);
        for (Port port : portLabels.keySet()) {
            if (portLabels.get(port) != null) {
                labels.addAll(portLabels.get(port));
            }
        }
        return labels;
    }


    @Override
    public Label getMainLabel() {
        return mainLabel;
    }

    @Override
    public boolean setMainLabel(Label mainLabel) {
        if (!getLabels().contains(mainLabel)) {
            if (!addInnerLabel(mainLabel)) {
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
     * (except for labels and ports)
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
    public boolean addAllInnerLabels(Collection<Label> labels) {
        boolean success = true;
        for (Label label : labels) {
            success = success & this.addInnerLabel(label);
        }
        return success;
    }

    /**
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    public boolean addAllPortLabels(Port p, Collection<Label> labels) {
        boolean success = true;
        for (Label label : labels) {
            success = success & this.addPortLabel(p, label);
        }
        return success;
    }

    public boolean addPortLabel(Port p, Label l) {
        if (!this.portLabels.containsKey(p)) {
            portLabels.put(p, new ArrayList<>());
        }
        List<Label> labelsOfThisPort = this.portLabels.get(p);
        if (!labelsOfThisPort.contains(l)) {
            labelsOfThisPort.add(l);
            LabeledObject.changeAssociatedObject(l, this);
            return true;
        }
        return false;
    }

    public boolean removePortLabel(Port p, Label l) {
        if (this.portLabels.containsKey(p)) {
            List<Label> labelsOfThisPort = this.portLabels.get(p);
            if (labelsOfThisPort.contains(l)) {
                labelsOfThisPort.remove(l);
                LabeledObject.justRemoveAssociatedObject(l);

                if (mainLabel.equals(l)) {
                    findNewMainLabel();
                }
                return true;
            }
        }
        return false;
    }

    public boolean addInnerLabel(Label l) {
        if (!innerLabels.contains(l)) {
            innerLabels.add(l);
            LabeledObject.changeAssociatedObject(l, this);
            return true;
        }
        return false;
    }

    public boolean removeInnerLabel(Label l) {
        if (innerLabels.contains(l)) {
            innerLabels.remove(l);
            LabeledObject.justRemoveAssociatedObject(l);

            if (mainLabel.equals(l)) {
                findNewMainLabel();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addLabel(Label l) {
        return addInnerLabel(l);
    }

    @Override
    public boolean removeLabel(Label l) {
        boolean success = removeInnerLabel(l);
        for (Port port : ports) {
            success = success | removePortLabel(port, l);
        }
        return success;
    }

    private void findNewMainLabel() {
        if (getLabels().size() == 1) {
            mainLabel = getLabels().get(0);
        }
        else if (innerLabels.size() == 1) {
            mainLabel = innerLabels.get(0);
        }
        else {
            mainLabel = null;
        }
    }

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
}
