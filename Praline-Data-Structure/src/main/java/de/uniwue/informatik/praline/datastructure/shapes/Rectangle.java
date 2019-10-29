package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;
import java.awt.geom.Rectangle2D;

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

    public Rectangle(double x, double y, double width, double height, Color color) {
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
    public double getWidth() {
        return super.getWidth();
    }

    @Override
    public double getHeight() {
        return super.getHeight();
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
    public Shape clone() {
        return new Rectangle(this.getXPosition(), this.getYPosition(), this.getWidth(), this.getHeight(),
                this.getColor());
    }
}
