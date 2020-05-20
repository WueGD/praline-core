package de.uniwue.informatik.praline.datastructure.utils;

import java.util.ArrayList;
import java.util.Collection;

public class GraphUtils {

    /**
     * Creates a list of the passed elements.
     * It filters out entries that are null and duplicate entries.
     * If the element parameter is null then it returns an empty list.
     *
     * @param elements
     * @param <T>
     * @return
     */
    public static <T> ArrayList<T> newArrayListNullSafe(Collection<T> elements) {
        if (elements == null) {
            return new ArrayList<>();
        }

        ArrayList<T> returnList = new ArrayList<>();
        for (T element : elements) {
            if (element == null) {
                continue;
            }
            if (!returnList.contains(element)) {
                returnList.add(element);
            }
        }

        return returnList;
    }
}
