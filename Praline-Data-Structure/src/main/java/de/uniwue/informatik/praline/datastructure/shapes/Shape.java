package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.awt.*;

@JsonIgnoreProperties({ "bounds", "bounds2D", "x", "y", "empty", "minX", "minY", "maxX", "maxY", "centerX", "centerY",
        "frame", "pathIterator" })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Rectangle.class, name = "rectangle"),
        @JsonSubTypes.Type(value = Circle.class, name = "circle"),
        @JsonSubTypes.Type(value = ArrowHeadTriangle.class, name = "arrowHeadTriangle"),
})
public interface Shape extends Cloneable {

    /*==========
     * Default values
     *==========*/

    Color DEFAULT_COLOR = Color.BLACK;
    double UNDEFINED_LENGTH = java.lang.Double.NaN;
    double UNDEFINED_POSITION = java.lang.Double.NaN;


    /*==========
     * Methods to be implemented
     *==========*/

    double getXPosition();
    double getYPosition();
    Color getColor();
    void setColor(Color c);
    Shape clone();
}
