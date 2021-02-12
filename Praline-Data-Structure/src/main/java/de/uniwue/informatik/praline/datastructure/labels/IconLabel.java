package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

/**
 * Version of {@link Label} that provides an icon or an image (see {@link Label} for more).
 * Currently rather a placeholder and not yet capable of handling image files.
 */
public abstract class IconLabel extends Label<LabelStyle> {
    public IconLabel() {
        this(null, null);
    }

    public IconLabel(LabelStyle labelStyle, Shape shape) {
        super(labelStyle == null ? LabelStyle.DEFAULT_LABEL_STYLE : labelStyle, shape);
    }
}
