package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class EdgeBundle implements LabeledObject {

    /*==========
     * Instance variables
     *==========*/

    private List<Edge> containedEdges;
    private List<EdgeBundle> containedEdgeBundles;
    private List<Label> labels;
    private Label mainLabel;


    /*==========
     * Constructors
     *==========*/

    public EdgeBundle() {
        this(null, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles) {
        this(containedEdges, containedEdgeBundles, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles,
                      Collection<Label> labels) {
        this.containedEdges = newArrayListNullSave(containedEdges);
        this.containedEdgeBundles = newArrayListNullSave(containedEdgeBundles);
        this.labels = new ArrayList<>();
        this.addAllLabels(labels);
    }

    /*==========
     * Getters
     *==========*/

    public List<Edge> getContainedEdges() {
        return containedEdges;
    }

    public List<EdgeBundle> getContainedEdgeBundles() {
        return containedEdgeBundles;
    }

    @Override
    public List<Label> getLabels() {
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
