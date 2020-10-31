package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

/**
 * Version of {@link Label} that provides an icon or an image (see {@link Label} for more).
 * Currently rather a placeholder and not yet capable of handling image files.
 */
public abstract class IconLabel extends Label {
    public IconLabel() {
        super();
    }

    public IconLabel(Placement placement, HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement,
                     boolean showLabel, Shape shape) {
        super(placement, horizontalPlacement, verticalPlacement, showLabel, shape);
    }
}
