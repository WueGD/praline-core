package de.uniwue.informatik.praline.datastructure.paths;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

public class PolygonalPath extends Path {

    /*==========
     * Instance variables
     *==========*/

    private Point2D.Double startPoint;
    private Point2D.Double endPoint;
    private final List<Point2D.Double> bendPoints;


    /*==========
     * Constructors
     *==========*/

    public PolygonalPath() {
        this(null, null, null, Path.UNSPECIFIED_THICKNESS);
    }

    public PolygonalPath(double thickness) {
        this(null, null, null, thickness);
    }

    public PolygonalPath(Point2D.Double startPoint, Point2D.Double endPoint, Collection<Point2D.Double> bendPoints) {
        this(startPoint, endPoint, bendPoints, UNSPECIFIED_THICKNESS);
    }

    public PolygonalPath(Point2D.Double startPoint, Point2D.Double endPoint, Collection<Point2D.Double> bendPoints,
                         double thickness) {
        super(thickness);
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.bendPoints = newArrayListNullSave(bendPoints);
    }


    /*==========
     * Getters & Setters
     *==========*/

    public Point2D.Double getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point2D.Double startPoint) {
        this.startPoint = startPoint;
    }

    public Point2D.Double getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point2D.Double endPoint) {
        this.endPoint = endPoint;
    }

    public List<Point2D.Double> getBendPoints() {
        return bendPoints;
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(startPoint.toString());
        for (Point2D.Double bendPoint : bendPoints) {
            sb.append("-").append(bendPoint.toString());
        }
        return sb.append("-").append(endPoint.toString()).toString();
    }
}
