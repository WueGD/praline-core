package de.uniwue.informatik.praline.datastructure.labels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LabelManager {

    /*==========
     * Instance variables
     *==========*/

    private final LabeledObject managedLabeledObject;
    protected final List<Label> labels;
    protected Label mainLabel;


    /*==========
     * Constructors
     *==========*/

    public LabelManager(LabeledObject managedLabeledObject) {
        this(managedLabeledObject,null, null);
    }

    public LabelManager(LabeledObject managedLabeledObject, Collection<Label> labels) {
        this(managedLabeledObject, labels, null);
        findNewMainLabel();
    }

    /**
     *
     * @param managedLabeledObject
     *      should not be null and should be set correctly initially
     * @param labels
     *      can be null if there are no labels initially
     * @param mainLabel
     *      can be null if there is no label that should become the main label
     */
    public LabelManager(LabeledObject managedLabeledObject, Collection<Label> labels, Label mainLabel) {
        this.managedLabeledObject = managedLabeledObject;
        this.labels = new ArrayList<>();
        if (labels != null) {
            this.addAllLabels(labels);
        }
        if (mainLabel != null) {
            this.mainLabel = mainLabel;
        }
    }


    /*==========
     * Getters & Setters
     *==========*/

    public LabeledObject getManagedLabeledObject() {
        return managedLabeledObject;
    }

    public List<Label> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public Label getMainLabel() {
        return mainLabel;
    }

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
        boolean returnValue = addAllLabelsInternally(this.labels, labels);
        if (getLabels().size() == 1 && mainLabel == null) {
            findNewMainLabel();
        }
        return returnValue;
    }

    protected boolean addAllLabelsInternally(List<Label> toThisList, Collection<Label> toBeAdded) {
        boolean success = true;
        for (Label label : toBeAdded) {
            success = success & addLabelInternally(toThisList, label, false);
        }
        return success;
    }

    public boolean addLabel(Label l) {
        return addLabelInternally(labels, l, true);
    }

    protected  boolean addLabelInternally(List<Label> toThisList, Label l, boolean checkMainLabel) {
        if (!toThisList.contains(l)) {
            toThisList.add(l);
            //change value associated object at the label
            //and remove it from the list of labels if this label was previously attached to another LabeldObject
            if (l.getAssociatedLabelManager() != null) {
                LabelManager currentlyAssociatedManager = l.getAssociatedLabelManager();
                l.setAssociatedLabelManager(null);
                currentlyAssociatedManager.removeLabel(l);
            }
            l.setAssociatedLabelManager(this);
            //check if it becomes main label
            if (checkMainLabel && this.getLabels().size() == 1) {
                findNewMainLabel();
            }
            return true;
        }
        return false;
    }

    public boolean removeLabel(Label l) {
        return removeLabelInternally(labels, l);
    }

    protected  boolean removeLabelInternally(List<Label> fromThisList, Label l) {
        if (fromThisList.contains(l)) {
            fromThisList.remove(l);
            l.setAssociatedLabelManager(null);

            if (mainLabel.equals(l)) {
                findNewMainLabel();
            }
            return true;
        }
        return false;
    }

    protected void findNewMainLabel() {
        if (labels.size() == 1) {
            mainLabel = labels.get(0);
        } else {
            mainLabel = null;
        }
    }


    /*==========
     * Other
     *==========*/

    /**
     *
     * @return
     *      A String that depends on the main label and the class name of the labeled object.
     *      It says if there is no main label.
     *      So the returned names are usually not unique for the same object.
     */
    public String getStringForLabeledObject() {
        String mainLabelText = "(no main label)" + managedLabeledObject.hashCode();
        if (mainLabel != null) {
            mainLabelText = mainLabel.toString();
        }
        return managedLabeledObject.getClass().getSimpleName() + "[" + mainLabelText + "]";
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "_of_" + managedLabeledObject.toString();
    }
}
