package de.uniwue.informatik.praline.datastructure.placements;

/**
 * May be used to describe the relative placement of a {@link de.uniwue.informatik.praline.datastructure.labels.Label}
 * to its {@link de.uniwue.informatik.praline.datastructure.labels.LabeledObject}
 * or to describe the relative position of one {@link de.uniwue.informatik.praline.datastructure.graphs.Vertex} to
 * the other in a {@link de.uniwue.informatik.praline.datastructure.graphs.TouchingPair}.
 *
 * Other placement enums are {@link VerticalPlacement} and {@link Placement} (the latter for inside/outside).
 */
public enum HorizontalPlacement {
    FREE,
    LEFT,
    CENTER,
    RIGHT;
}
