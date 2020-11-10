package de.uniwue.informatik.praline.datastructure.utils;

import de.uniwue.informatik.praline.datastructure.graphs.PortComposition;
import de.uniwue.informatik.praline.datastructure.graphs.PortPairing;
import de.uniwue.informatik.praline.datastructure.graphs.TouchingPair;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class EqualLabeling {

    public static boolean equalLabelingLists(List<Object> objects, List<Object> otherObjects) {
        List<Object> otherObjectsCopy = new LinkedList<>(otherObjects);
        for (Object lo : objects) {
            boolean hasEqualLabel = false;
            Iterator<Object> iterator = otherObjectsCopy.iterator();
            while (iterator.hasNext()) {
                Object otherLo = iterator.next();
                if (equalLabelingTwoObjects(lo, otherLo)) {
                    hasEqualLabel = true;
                    iterator.remove();
                    break;
                }
            }
            if (!hasEqualLabel) {
                return false;
            }
        }
        if (!otherObjectsCopy.isEmpty()) {
            return false;
        }
        return true;
    }

    private static boolean equalLabelingTwoObjects(Object lo, Object otherLo) {
        if (lo instanceof LabeledObject && otherLo instanceof LabeledObject) {
            return ((LabeledObject) lo).equalLabeling((LabeledObject) otherLo);
        }
        if (lo instanceof Label && otherLo instanceof Label) {
            return ((Label) lo).equalLabeling((Label) otherLo);
        }
        if (lo instanceof PortComposition && otherLo instanceof PortComposition) {
            return ((PortComposition) lo).equalLabeling((PortComposition) otherLo);
        }
        if (lo instanceof TouchingPair && otherLo instanceof TouchingPair) {
            return ((TouchingPair) lo).equalLabeling(otherLo);
        }
        if (lo instanceof PortPairing && otherLo instanceof PortPairing) {
            return ((PortPairing) lo).equalLabeling(otherLo);
        }
        return false;
    }
}
