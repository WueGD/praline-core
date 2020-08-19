package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Most typical implementation of {@link Shape}.
 * A {@link Rectangle} has a position ({@link Rectangle#getXPosition()} + {@link Rectangle#getYPosition()}) and a
 * size ({@link Rectangle#getWidth()} + {@link Rectangle#getHeight()}).
 * The position is relative to the top left corner of the {@link Rectangle}.
 */
@JsonPropertyOrder({ "xposition", "yposition", "width", "height", "color" })
public class Rectangle extends Rectangle2D.Double implements Shape {


    /*==========
     * Instance variables
     *==========*/

    private Color color;


    /*==========
     * Constructors
     *==========*/

    public Rectangle() {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, UNDEFINED_LENGTH, UNDEFINED_LENGTH, null);
    }

    /**
     *
     * @param x
     * @param y
     * @param color
     *      color can be set to null -- this parameter was necessary to make this constructor distinguishable
     *      from constructor {@link Rectangle#Rectangle(double width, double height)} with width and height.
     */
    public Rectangle(double x, double y, Color color) {
        this(x, y, UNDEFINED_LENGTH, UNDEFINED_LENGTH, color);
    }

    public Rectangle(double width, double height) {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, width, height, null);
    }

    public Rectangle(double x, double y, double width, double height) {
        this(x, y, width, height, null);
    }

    @JsonCreator
    public Rectangle(
            @JsonProperty("xposition") final double x,
            @JsonProperty("yposition") final double y,
            @JsonProperty("width") final double width,
            @JsonProperty("height") final double height,
            @JsonProperty("color") final Color color
    ) {
        super(x, y, width, height);
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

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }

    /*==========
     * Helpful methods
     *==========*/

    public boolean liesOnBoundary(Point2D.Double point) {
        if ((point.x == this.x || point.x == this.x + this.width)
                && this.y <= point.y && point.y <= this.y + this.height) {
            return true;
        }
        if ((point.y == this.y || point.y == this.y + this.height)
                && this.x <= point.x && point.x <= this.x + this.width) {
            return true;
        }
        return false;
    }

    /*==========
     * Clone, equals, hashCode
     *==========*/

    @Override
    public Rectangle clone() {
        return (Rectangle) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rectangle that = (Rectangle) o;
        return java.lang.Double.compare(that.getX(), getX()) == 0 &&
                java.lang.Double.compare(that.getY(), getY()) == 0 &&
                java.lang.Double.compare(that.getWidth(), getWidth()) == 0 &&
                java.lang.Double.compare(that.getHeight(), getHeight()) == 0 &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color);
    }
}
