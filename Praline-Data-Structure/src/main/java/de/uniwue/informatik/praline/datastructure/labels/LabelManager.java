package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

/**
 * Manages the {@link Label}s of and for a {@link LabeledObject}.
 * There is one {@link LabelManager} per {@link LabeledObject}.
 *
 * The {@link LabelManager#mainLabel} may be set, but does not need to be set.
 * If there is just one {@link Label} added and there is no other {@link Label} yet, this {@link Label} is
 * automatically set to be the {@link LabelManager#mainLabel} of this {@link LabeledObject}.
 * Typically the {@link LabelManager#getMainLabel()} provides something like the name or the ID of the
 * {@link LabeledObject} -- the {@link LabeledObject#toString()} method derives the String from
 * {@link LabelManager#getMainLabel()}.
 */
@JsonIgnoreProperties({ "managedLabeledObject", "stringForLabeledObject" })
public class LabelManager {

    /*==========
     * Instance variables
     *==========*/

    private final LabeledObject managedLabeledObject;
    protected final List<Label<? extends LabelStyle>> labels;
    protected Label<? extends LabelStyle> mainLabel;


    /*==========
     * Constructors
     *==========*/

    public LabelManager(LabeledObject managedLabeledObject) {
        this(managedLabeledObject,null, null);
    }

    public LabelManager(LabeledObject managedLabeledObject, Collection<Label<? extends LabelStyle>> labels) {
        this(managedLabeledObject, labels, null);
        findNewMainLabel();
    }

    @JsonCreator
    private LabelManager(
            @JsonProperty("labels") final List<Label<? extends LabelStyle>> labels,
            @JsonProperty("mainLabel") final Label<? extends LabelStyle> mainLabel
    ) {
        this(null, labels, mainLabel);
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
    public LabelManager(LabeledObject managedLabeledObject, Collection<Label<? extends LabelStyle>> labels,
                        Label<? extends LabelStyle> mainLabel) {
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

    public List<Label<? extends LabelStyle>> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public Label<? extends LabelStyle> getMainLabel() {
        return mainLabel;
    }

    public boolean setMainLabel(Label<? extends LabelStyle> mainLabel) {
        if (mainLabel != null && !labels.contains(mainLabel)) {
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
    public boolean addAllLabels(Collection<Label<? extends LabelStyle>> labels) {
        if (labels == null) {
            return false;
        }

        boolean returnValue = addAllLabelsInternally(this.labels, labels);
        if (getLabels().size() == 1 && mainLabel == null) {
            findNewMainLabel();
        }
        return returnValue;
    }

    protected boolean addAllLabelsInternally(List<Label<? extends LabelStyle>> toThisList,
                                             Collection<Label<? extends LabelStyle>> toBeAdded) {
        boolean success = true;
        for (Label<? extends LabelStyle> label : new ArrayList<>(toBeAdded)) {
            success = success & addLabelInternally(toThisList, label, false);
        }
        return success;
    }

    public boolean addLabel(Label<? extends LabelStyle> l) {
        return addLabelInternally(labels, l, true);
    }

    protected boolean addLabelInternally(List<Label<? extends LabelStyle>> toThisList, Label<? extends LabelStyle> l,
                                         boolean checkMainLabel) {
        if (!toThisList.contains(l)) {
            toThisList.add(l);
            //change value associated object at the label
            //and remove it from the list of labels if this label was previously attached to another LabeledObject
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

    public boolean removeLabel(Label<? extends LabelStyle> l) {
        return removeLabelInternally(labels, l);
    }

    protected  boolean removeLabelInternally(List<Label<? extends LabelStyle>> fromThisList,
                                             Label<? extends LabelStyle> l) {
        if (fromThisList.contains(l)) {
            fromThisList.remove(l);
            l.setAssociatedLabelManager(null);

            if (mainLabel != null && mainLabel.equals(l)) {
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
        String mainLabelText;
        if (mainLabel == null) {
            mainLabelText = "(no main label)" + managedLabeledObject.hashCode();
        } else {
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


    /*==========
     * equalLabeling
     *==========*/

    public boolean equalLabeling(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelManager that = (LabelManager) o;
        return EqualLabeling.equalLabelingLists(new ArrayList<>(labels), new ArrayList<>(that.labels))
                && ((mainLabel == null && that.mainLabel == null) || mainLabel.equalLabeling(that.mainLabel));
    }
}
