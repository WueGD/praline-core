package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.uniwue.informatik.praline.datastructure.styles.ShapeStyle;
import de.uniwue.informatik.praline.datastructure.utils.ArithmeticOperation;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Most typical implementation of {@link Shape}.
 * A {@link Rectangle} has a position ({@link Rectangle#getXPosition()} + {@link Rectangle#getYPosition()}) and a
 * size ({@link Rectangle#getWidth()} + {@link Rectangle#getHeight()}).
 * The position is relative to the top left corner of the {@link Rectangle}.
 */
@JsonPropertyOrder({ "xposition", "yposition", "width", "height", "shapeStyle" })
public class Rectangle extends Rectangle2D.Double implements Shape {

    /*==========
     * Instance variables
     *==========*/

    private ShapeStyle shapeStyle;


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
     * @param shapeStyle
     */
    public Rectangle(double x, double y, ShapeStyle shapeStyle) {
        this(x, y, UNDEFINED_LENGTH, UNDEFINED_LENGTH, shapeStyle);
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
            @JsonProperty("shapeStyle") final ShapeStyle shapeStyle
    ) {
        super(x, y, width, height);
        this.shapeStyle = shapeStyle == null ? ShapeStyle.DEFAULT_SHAPE_STYLE : shapeStyle;
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
    public ShapeStyle getShapeStyle() {
        return shapeStyle;
    }

    @Override
    public void setShapeStyle(ShapeStyle shapeStyle) {
        this.shapeStyle = shapeStyle;
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
     * Helpful methods
     *==========*/

    public boolean liesOnBoundary(Point2D.Double point) {
        if (point == null) return false;
        if ((ArithmeticOperation.precisionEqual(point.x, this.x) ||
                ArithmeticOperation.precisionEqual(point.x, this.x + this.width))
                && ArithmeticOperation.precisionInRange(point.y, this.y, this.y + this.height)) {
            return true;
        }
        if ((ArithmeticOperation.precisionEqual(point.y, this.y) ||
                ArithmeticOperation.precisionEqual(point.y, this.y + this.height))
                && ArithmeticOperation.precisionInRange(point.x, this.x, this.x + this.width)) {
            return true;
        }
        return false;
    }

    public boolean containsInsideOrOnBoundary(Point2D.Double point) {
        return liesOnBoundary(point) || contains(point);
    }

    /*==========
     * clone & equals & hashCode
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
                Objects.equals(shapeStyle, that.shapeStyle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shapeStyle);
    }
}
