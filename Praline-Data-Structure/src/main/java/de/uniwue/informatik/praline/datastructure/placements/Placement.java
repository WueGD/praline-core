package de.uniwue.informatik.praline.datastructure.placements;

/**
 * Specifying containment (inside/outside).
 * May be used to describe the relative placement of a {@link de.uniwue.informatik.praline.datastructure.labels.Label}
 * to its {@link de.uniwue.informatik.praline.datastructure.labels.LabeledObject}.
 *
 * Other placement enums are {@link VerticalPlacement} and {@link HorizontalPlacement}.
 */
public enum Placement {
    FREE,
    INSIDE,
    OUTSIDE;
}
