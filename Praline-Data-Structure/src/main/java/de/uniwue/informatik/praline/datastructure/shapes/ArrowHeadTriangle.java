package de.uniwue.informatik.praline.datastructure.shapes;

import java.awt.*;

public class ArrowHeadTriangle implements Shape {

    /*==========
     * Default values
     *==========*/

    public static final double UNDEFINED_TRIANGLE_LENGTH = java.lang.Double.NaN;
    public static final double UNDEFINED_TRIANGLE_WIDTH = java.lang.Double.NaN;
    public static final double UNDEFINED_ANGLE = java.lang.Double.NaN;


    /*==========
     * Instance variables
     *==========*/

    private double xPosition;
    private double yPosition;
    private double length;
    private double width;
    private double angle;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public ArrowHeadTriangle() {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, UNDEFINED_TRIANGLE_LENGTH, UNDEFINED_TRIANGLE_WIDTH,
                UNDEFINED_ANGLE, null);
    }

    /**
     *
     * @param x
     * @param y
     * @param color
     *      color can be set to null -- this parameter was necessary to make this constructor distinguishable
     *      from constructor {@link ArrowHeadTriangle#ArrowHeadTriangle(double length, double width)} with length and
     *      width.
     */
    public ArrowHeadTriangle(double x, double y, Color color) {
        this(x, y, UNDEFINED_TRIANGLE_LENGTH, UNDEFINED_TRIANGLE_WIDTH, UNDEFINED_ANGLE, color);
    }

    public ArrowHeadTriangle(double length, double width) {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, length, width, UNDEFINED_ANGLE, null);
    }

    public ArrowHeadTriangle(double length, double width, double angle) {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, length, width, angle, null);
    }

    public ArrowHeadTriangle(double x, double y, double length, double width, double angle, Color color) {
        this.xPosition = x;
        this.yPosition = y;
        this.length = length;
        this.width = width;
        this.angle = angle;
        this.color = color != null ? color : DEFAULT_COLOR;
    }

    /*==========
     * Getters & Setters
     *==========*/

    @Override
    public double getXPosition() {
        return xPosition;
    }

    public void setXPosition(double x) {
        this.xPosition = x;
    }

    @Override
    public double getYPosition() {
        return yPosition;
    }

    public void setYPosition(double y) {
        this.yPosition = y;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
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
        return new ArrowHeadTriangle(this.getXPosition(), this.getYPosition(), this.getLength(), this.getWidth(),
                this.getAngle(), this.getColor());
    }
}
