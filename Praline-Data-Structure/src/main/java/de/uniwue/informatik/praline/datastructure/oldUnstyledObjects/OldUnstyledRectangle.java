package de.uniwue.informatik.praline.datastructure.oldUnstyledObjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.styles.ShapeStyle;

import java.awt.*;

public class OldUnstyledRectangle extends Rectangle {

    @JsonCreator
    public OldUnstyledRectangle(
            @JsonProperty("xposition") final double x,
            @JsonProperty("yposition") final double y,
            @JsonProperty("width") final double width,
            @JsonProperty("height") final double height,
            @JsonProperty("color") final Color color
    ) {
        super(x, y, width, height, color.equals(ShapeStyle.DEFAULT_COLOR) ? ShapeStyle.DEFAULT_SHAPE_STYLE :
                new ShapeStyle(null, color));
    }
}
