package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;
import java.awt.geom.Ellipse2D;

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

    public Circle(double x, double y, double radius, Color color) {
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
