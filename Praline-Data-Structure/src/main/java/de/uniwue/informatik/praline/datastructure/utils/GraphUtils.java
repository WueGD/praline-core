package de.uniwue.informatik.praline.datastructure.utils;

import java.util.ArrayList;
import java.util.Collection;

public class GraphUtils {

    public static <T> ArrayList<T> newArrayListNullSafe(Collection<T> elements) {
        if (elements == null) {
            return new ArrayList<>();
        }
        while (elements.contains(null)) {
            elements.remove(null);
        }
        return new ArrayList<>(elements);
    }
}
