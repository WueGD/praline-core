package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Implementation of {@link Shape}.
 * A {@link Circle} has a position ({@link Circle#getXPosition()} + {@link Circle#getYPosition()}) and a
 * size ({@link Circle#getRadius()}).
 * The position is relative to the upper-left corner of the framing rectangle of the {@link Circle}
 * (by inheritance from {@link Ellipse2D.Double} -- maybe change this to the center point of the {@link Circle} later).
 */
@JsonIgnoreProperties({ "width", "height" })
public class Circle extends Ellipse2D.Double implements Shape {

    /*==========
     * Instance variables
     *==========*/

    private Color color;


    /*==========
     * Constructors
     *==========*/

    public Circle() {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, UNDEFINED_LENGTH, null);
    }

    public Circle(double x, double y) {
        this(x, y, UNDEFINED_LENGTH, null);
    }

    public Circle(double radius) {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, radius, null);
    }

    @JsonCreator
    public Circle(
            @JsonProperty("xposition") final double x,
            @JsonProperty("yposition") final double y,
            @JsonProperty("radius") final double radius,
            @JsonProperty("color") final Color color
    ) {
        super(x, y, 2.0 * radius, 2.0 * radius);
        this.color = color != null ? color : DEFAULT_COLOR;
    }


    /*==========
     * Getters & Setters
     *==========*/

    @Override
    public double getXPosition() {
        return getX();
    }

    @Override
    public double getYPosition() {
        return getY();
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return super.getBounds2D();
    }

    public double getRadius() {
        return width / 2.0;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }


    /*==========
     * Modifiers
     *==========*/

    @Override
    public void translate(double xOffset, double yOffset) {
        this.x += xOffset;
        this.y += yOffset;
    }


    /*==========
     * Clone, equals, hashCode
     *==========*/

    @Override
    public Circle clone() {
        return (Circle) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Circle circle = (Circle) o;
        return Objects.equals(color, circle.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color);
    }
}
