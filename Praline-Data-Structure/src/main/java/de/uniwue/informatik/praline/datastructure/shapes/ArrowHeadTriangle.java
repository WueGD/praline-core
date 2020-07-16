package de.uniwue.informatik.praline.datastructure.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Implementation of {@link Shape}.
 * It is like a "normal" isosceles triangle, but its designated purpose is representing arrow heads for
 * {@link de.uniwue.informatik.praline.datastructure.labels.LeaderedLabel}s -- hence the name.
 * The size is specified via {@link ArrowHeadTriangle#getWidth()}, which is the length of the base side in the
 * isosceles triangle, which is the length of the back side of the arrow head triangle, and by
 * {@link ArrowHeadTriangle#getLength()}, which is the altitude of the isosceles triangle (a line segment
 * perpendicular from the base to the apex).
 * The orientation of the main axis (the line segment being the altitude of the isosceles triangle) is specified via
 * The vertex angle of the triangle at the apex (which is the top of the head of the arrow head triangle) is implicitly
 * defined by the former length and width.
 * The position ({@link ArrowHeadTriangle#getXPosition()} + {@link ArrowHeadTriangle#getYPosition()}) is relative to
 * the apex of the isosceles triangle, i. e., the pointing top vertex of the {@link ArrowHeadTriangle}.
 */
@JsonIgnoreProperties({ "cornerPoints" })
public class ArrowHeadTriangle implements Shape {

    /*==========
     * Default values
     *==========*/

    public static final double UNDEFINED_TRIANGLE_LENGTH = java.lang.Double.NaN;
    public static final double UNDEFINED_TRIANGLE_WIDTH = java.lang.Double.NaN;
    public static final double UNDEFINED_ORIENTATION_ANGLE = java.lang.Double.NaN;


    /*==========
     * Instance variables
     *==========*/

    private double xPosition;
    private double yPosition;
    private double length;
    private double width;
    /**
     * angle in radians at the apex between the line through the apex parallel to the +x axis and the shortest the
     * segment between the apex and the base (i.e. the segment with length the altitude of the triangle.
     */
    private double orientationAngle;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public ArrowHeadTriangle() {
        this(UNDEFINED_POSITION, UNDEFINED_POSITION, UNDEFINED_TRIANGLE_LENGTH, UNDEFINED_TRIANGLE_WIDTH,
                UNDEFINED_ORIENTATION_ANGLE, null);
    }

    @JsonCreator
    public ArrowHeadTriangle(
            @JsonProperty("xposition") final double x,
            @JsonProperty("yposition") final double y,
            @JsonProperty("length") final double length,
            @JsonProperty("width") final double width,
            @JsonProperty("orientationAngle") final double orientationAngle,
            @JsonProperty("color") final Color color
    ) {
        this.xPosition = x;
        this.yPosition = y;
        this.length = length;
        this.width = width;
        this.orientationAngle = orientationAngle;
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


    /**
     * angle in radians at the apex between the line through the apex parallel to the +x axis and the shortest the
     * segment between the apex and the base (i.e. the segment with length the altitude of the triangle.
     */
    public double getOrientationAngle() {
        return orientationAngle;
    }


    /**
     * angle in radians at the apex between the line through the apex parallel to the +x axis and the shortest the
     * segment between the apex and the base (i.e. the segment with length the altitude of the triangle.
     */
    public void setOrientationAngle(double orientationAngle) {
        this.orientationAngle = orientationAngle;
    }

    @Override
    public Rectangle2D getBoundingBox() {
        Collection<Point2D.Double> cornerPoints = getCornerPoints();
        double minX = Collections.min(cornerPoints, Comparator.comparing(p -> p.x)).getX();
        double minY = Collections.min(cornerPoints, Comparator.comparing(p -> p.y)).getY();
        double maxX = Collections.max(cornerPoints, Comparator.comparing(p -> p.x)).getX();
        double maxY = Collections.max(cornerPoints, Comparator.comparing(p -> p.y)).getY();

        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }

    public Collection<Point2D.Double> getCornerPoints() {
        List<Point2D.Double> cornerPoints = new ArrayList<>(3);
        //add apex
        cornerPoints.add(new Point2D.Double(xPosition, yPosition));
        //for the other corner points first determine their angle and distance to the apex corner
        double distanceApexOtherCorners = Math.sqrt(Math.pow(length, 2) + Math.pow(0.5 * width, 2));
        double halfInnerAngleAtApex = Math.atan(0.5 * width / length);
        cornerPoints.add(new Point2D.Double(
                Math.sin(orientationAngle + halfInnerAngleAtApex) * distanceApexOtherCorners,
                Math.cos(orientationAngle + halfInnerAngleAtApex) * distanceApexOtherCorners));
        cornerPoints.add(new Point2D.Double(
                Math.sin(orientationAngle - halfInnerAngleAtApex) * distanceApexOtherCorners,
                Math.cos(orientationAngle - halfInnerAngleAtApex) * distanceApexOtherCorners));

        return cornerPoints;
    }


    /*==========
     * Clone, equals, hashCode
     *==========*/

    @Override
    public ArrowHeadTriangle clone() {
        try {
            return (ArrowHeadTriangle) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrowHeadTriangle that = (ArrowHeadTriangle) o;
        return Double.compare(that.xPosition, xPosition) == 0 && Double.compare(that.yPosition, yPosition) == 0 &&
                Double.compare(that.length, length) == 0 && Double.compare(that.width, width) == 0 &&
                Double.compare(that.orientationAngle, orientationAngle) == 0 && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xPosition, yPosition, length, width, orientationAngle, color);
    }
}
