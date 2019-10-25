package de.uniwue.informatik.praline.datastructure.labels;

import java.util.Collection;

public interface LabeledObject {

    Collection<Label> getLabels();
    boolean addLabel(Label l);
    boolean removeLabel(Label l);
    Label getMainLabel();
    boolean setMainLabel(Label l);

    static void changeAssociatedObject(Label label, LabeledObject object) {
        if (label.getAssociatedObject() != null) {
            LabeledObject currentlyAssociatedObject = label.getAssociatedObject();
            label.setAssociatedObject(null);
            currentlyAssociatedObject.removeLabel(label);
        }
        label.setAssociatedObject(object);
    }

    static void justRemoveAssociatedObject(Label label) {
        label.setAssociatedObject(null);
    }
}
