package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;
import java.awt.geom.Ellipse2D;

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
     * Clone
     *==========*/

    @Override
    public Circle clone() {
        return (Circle) super.clone();
    }
}
