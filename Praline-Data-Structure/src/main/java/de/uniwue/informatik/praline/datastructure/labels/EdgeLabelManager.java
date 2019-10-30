package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;

import java.util.*;

/**
 * the list of {@link Label}s from the super class {@link LabelManager} becomes
 * the list of inner {@link Label}s of this object.
 * Beside this internal list, there is a list of {@link Label}s for each {@link Port}.
 */
public class EdgeLabelManager extends LabelManager {

    /*==========
     * Instance variables
     *==========*/

    private Map<Port, List<Label>> portLabels;


    /*==========
     * Constructors
     *==========*/

    public EdgeLabelManager(Edge managedLabeledObject) {
        this(managedLabeledObject,null, null, null);
    }

    public EdgeLabelManager(Edge managedLabeledObject, Collection<Label> innerLabels,
                            Map<Port, List<Label>> portLabels) {
        this(managedLabeledObject, innerLabels, portLabels, null);
        findNewMainLabel();
    }

    /**
     *
     * @param managedLabeledObject
     *      should not be null and should be set correctly initially
     * @param innerLabels
     *      can be null if there are no labels initially
     * @param portLabels
     *      can be null if there are no labels initially
     * @param mainLabel
     *      can be null if there is no label that should become the main label
     */
    public EdgeLabelManager(Edge managedLabeledObject, Collection<Label> innerLabels,
                        Map<Port, List<Label>> portLabels, Label mainLabel) {

        super(managedLabeledObject, innerLabels, mainLabel);
        this.portLabels = new HashMap<>(portLabels == null ?
                managedLabeledObject.getPorts().size() : portLabels.size());
        if (portLabels != null) {
            for (Port port : portLabels.keySet()) {
                if (portLabels.get(port) != null) {
                    addAllPortLabels(port, portLabels.get(port));
                }
            }
        }

        if (mainLabel == null) {
            findNewMainLabel();
        }
    }


    /*==========
     * Getters
     *==========*/

    public List<Label> getInnerLabels() {
        return Collections.unmodifiableList(super.labels);
    }

    public List<Label> getPortLabels(Port p) {
        if (portLabels.containsKey(p)) {
            return Collections.unmodifiableList(portLabels.get(p));
        }
        return null;
    }

    public Set<Port> getRegisteredPorts() {
        return portLabels.keySet();
    }

    @Override
    public List<Label> getLabels() {
        ArrayList<Label> labels = new ArrayList<>();
        labels.addAll(getInnerLabels());
        for (Port port : portLabels.keySet()) {
            if (portLabels.get(port) != null) {
                labels.addAll(portLabels.get(port));
            }
        }
        return labels;
    }


    /*==========
     * Modifiers
     *==========*/

    /**
     * Use this method to add multiple inner labels (different from port labels)
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    public boolean addAllLabels(Collection<Label> labels) {
        return super.addAllLabels(labels);
    }

    /**
     * Use this method to add an inner label (different from port labels)
     *
     * @param l
     * @return
     */
    public boolean addLabel(Label l){
        return super.addLabel(l);
    }

    /**
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    public boolean addAllPortLabels(Port p, Collection<Label> labels) {
        if (!this.portLabels.containsKey(p)) {
            portLabels.put(p, new ArrayList<>(labels.size()));
        }
        return super.addAllLabelsInternally(portLabels.get(p), labels);
    }

    public boolean addPortLabel(Port p, Label l) {
        if (!this.portLabels.containsKey(p)) {
            portLabels.put(p, new ArrayList<>(labels.size()));
        }
        return super.addLabelInternally(this.portLabels.get(p), l, true);
    }

    public boolean removePortLabel(Port p, Label l) {
        if (this.portLabels.containsKey(p)) {
            List<Label> labelsOfThisPort = this.portLabels.get(p);
            return super.removeLabelInternally(labelsOfThisPort, l);
        }
        return false;
    }

    public boolean removeInnerLabel(Label l) {
        return super.removeLabelInternally(this.labels, l);
    }

    @Override
    public boolean removeLabel(Label l) {
        boolean success = removeInnerLabel(l);
        for (Port port : portLabels.keySet()) {
            success = success | removePortLabel(port, l);
        }
        return success;
    }

    @Override
    protected void findNewMainLabel() {
        if (getLabels().size() == 1) {
            mainLabel = getLabels().get(0);
        }
        else if (getInnerLabels().size() == 1) {
            mainLabel = getInnerLabels().get(0);
        }
        else {
            mainLabel = null;
        }
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return super.toString();
    }
}
