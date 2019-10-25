package de.uniwue.informatik.praline.datastructure.util;

import java.util.ArrayList;
import java.util.Collection;

public class GraphUtils {


    public static <T> ArrayList<T> newArrayListNullSave(Collection<T> elements) {
        if (elements == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(elements);
    }
}
