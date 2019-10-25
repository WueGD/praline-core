package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.shapes.ShapedObject;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class Vertex implements ShapedObject, LabeledObject {

    /*==========
     * Instance variables
     *==========*/

    /**
     * contains only top level {@link PortGroup}s and {@link Port}s (and in them possibly contained elements)
     */
    private List<PortComposition> portCompositions;
    private HashSet<Port> ports;
    private List<Label> labels;
    private Label mainLabel;
    private Shape shape;


    /*==========
     * Constructors
     *==========*/

    public Vertex() {
        this(null, null, null);
    }

    public Vertex(Collection<PortComposition> portCompositions) {
        this(portCompositions, null, null);
    }

    public Vertex(Collection<PortComposition> portCompositions, Shape shape) {
        this(portCompositions, null, shape);
    }

    public Vertex(Collection<PortComposition> portCompositions, Collection<Label> labels) {
        this(portCompositions, labels, null);
    }

    /**
     * leave value as null if it should be empty initially (e.g. no labels)
     *
     * @param portCompositions
     *      It suffices to only have the top-level {@link PortGroup}s and {@link Port}s in this {@link Collection}
     * @param labels
     * @param shape
     */
    public Vertex(Collection<PortComposition> portCompositions, Collection<Label> labels, Shape shape) {
        this.ports = new HashSet<>();
        this.portCompositions = new ArrayList<>();
        if (portCompositions != null) {
            //find top level PortCompositions and lower level ones
            //moreover find all "real" ports
            HashSet<PortComposition> allLowerLevelPortCompositions = new HashSet<>();
            for (PortComposition portComposition : portCompositions) {
                getContainedPortCompositionsAndAddAllPorts(allLowerLevelPortCompositions, portComposition);
            }
            //add only the top level ones to our list
            for (PortComposition portComposition : portCompositions) {
                if (!allLowerLevelPortCompositions.contains(portComposition)) {
                    portCompositions.add(portComposition);
                }
            }
        }
        this.labels = new ArrayList<>();
        this.addAllLabels(labels);
        this.shape = shape;
    }

    private void getContainedPortCompositionsAndAddAllPorts(HashSet<PortComposition> allLowerLevelPortCompositions,
                                                            PortComposition portComposition) {
        //add Port if possible
        if (portComposition instanceof Port) {
            if (!ports.contains(portComposition)) {
                ports.add((Port) portComposition);
            }
        }
        //add lower level PortCompositions and go into recursion
        if (portComposition instanceof PortGroup) {
            for (PortComposition lowerLevelComposition : ((PortGroup) portComposition).getPortCompositions()) {
                if (!allLowerLevelPortCompositions.contains(lowerLevelComposition)) {
                    allLowerLevelPortCompositions.add(lowerLevelComposition);
                }
                getContainedPortCompositionsAndAddAllPorts(allLowerLevelPortCompositions, lowerLevelComposition);
            }
        }
    }

    /*==========
     * Getters & Setters
     *==========*/

    /* TODO: Attention! By modifying this list, the state may become inconsistent (containing not only top level
    elements any more). Change this by making this gotten list unmodifiable and add save modification methods.
     */
    public List<PortComposition> getPortCompositions() {
        return portCompositions;
    }

    public Set<Port> getPorts() {
        return Collections.unmodifiableSet(ports);
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
