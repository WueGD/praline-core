package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;

/**
 * Implementation of {@link Shape}.
 * It is like a "normal" isosceles triangle, but its designated purpose is representing arrow heads for
 * {@link de.uniwue.informatik.praline.datastructure.labels.LeaderedLabel}s -- hence the name.
 * The size is specified via {@link ArrowHeadTriangle#getWidth()}, which is the length of the base side in the
 * isosceles triangle, which is the length of the back side of the arrow head triangle, and by
 * {@link ArrowHeadTriangle#getLength()}, which is the altitude of the isosceles triangle (a line segment
 * perpendicular from the base to the apex).
 * The vertex angle at the apex (which is the top of the head of the arrow head triangle) is implicitly defined by the
 * former two values.
 * The position ({@link ArrowHeadTriangle#getXPosition()} + {@link ArrowHeadTriangle#getYPosition()}) is relative to
 * the apex of the isosceles triangle, i. e., the pointing top vertex of the {@link ArrowHeadTriangle}.
 */
public class ArrowHeadTriangle implements Shape {

    /*==========
     * Default values
     *==========*/

    public static final double UNDEFINED_TRIANGLE_LENGTH = java.lang.Double.NaN;
    public static final double UNDEFINED_TRIANGLE_WIDTH = java.lang.Double.NaN;


    /*==========
     * Instance variables
     *==========*/

    private double xPosition;
    private double yPosition;
    private double length;
    private double width;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public ArrowHeadTriangle() {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, UNDEFINED_TRIANGLE_LENGTH, UNDEFINED_TRIANGLE_WIDTH, null);
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
        this(x, y, UNDEFINED_TRIANGLE_LENGTH, UNDEFINED_TRIANGLE_WIDTH, color);
    }

    public ArrowHeadTriangle(double length, double width) {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, length, width, null);
    }

    @JsonCreator
    public ArrowHeadTriangle(
            @JsonProperty("xposition") final double x,
            @JsonProperty("yposition") final double y,
            @JsonProperty("length") final double length,
            @JsonProperty("width") final double width,
            @JsonProperty("color") final Color color
    ) {
        this.xPosition = x;
        this.yPosition = y;
        this.length = length;
        this.width = width;
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
    public ArrowHeadTriangle clone() {
        try {
            return (ArrowHeadTriangle) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
