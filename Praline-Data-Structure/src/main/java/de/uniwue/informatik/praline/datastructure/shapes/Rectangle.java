package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.awt.*;
import java.awt.geom.Rectangle2D;

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
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }

    /*==========
     * Clone
     *==========*/

    @Override
    public Rectangle clone() {
        return (Rectangle) super.clone();
    }
}
