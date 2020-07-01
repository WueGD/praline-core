package de.uniwue.informatik.praline.datastructure.paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * Implementation of {@link Path} for polygonal chains, i. e., a sequence of points determining a path with a
 * straight line segment between each two consecutive points.
 * This sequence of points begins with {@link PolygonalPath#getStartPoint()}, continues with
 * {@link PolygonalPath#getBendPoints()} and ends with {@link PolygonalPath#getEndPoint()}).
 *
 * This class maybe used to model straight line segments (if {@link PolygonalPath#getBendPoints()} is an empty list)
 * or orthogonal paths, i. e. only horizontal and vertical segments, if the choice of points is done accordingly
 * (alternatingly changing only the x- or y- coordinate between each two consecutive points).
 */
@JsonIgnoreProperties({ "terminalAndBendPoints" })
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

    public PolygonalPath(Point2D.Double startPoint, Point2D.Double endPoint, List<Point2D.Double> bendPoints) {
        this(startPoint, endPoint, bendPoints, UNSPECIFIED_THICKNESS);
    }

    public PolygonalPath(List<Point2D.Double> terminalAndBendPoints) {
        this(terminalAndBendPoints, Path.UNSPECIFIED_THICKNESS);
    }

    public PolygonalPath(List<Point2D.Double> terminalAndBendPoints, double thickness) {
        this(terminalAndBendPoints.get(0), terminalAndBendPoints.get(terminalAndBendPoints.size() - 1),
                newListWithoutFirstAndLast(terminalAndBendPoints), thickness);
    }

    @JsonCreator
    public PolygonalPath(
            @JsonProperty("startPoint") final Point2D.Double startPoint,
            @JsonProperty("endPoint") final Point2D.Double endPoint,
            @JsonProperty("bendPoints") final List<Point2D.Double> bendPoints,
            @JsonProperty("thickness") final double thickness
    ) {
        super(thickness);
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.bendPoints = newArrayListNullSafe(bendPoints);
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

    /**
     * Take care: this list does not contain the start and end point.
     * You may query them separately via {@link PolygonalPath#getStartPoint()} and
     * {@link PolygonalPath#getEndPoint()} or you may call {@link PolygonalPath#getTerminalAndBendPoints()} instead.
     *
     * @return
     *      all bend points of this {@link PolygonalPath} in sequence as a list
     */
    public List<Point2D.Double> getBendPoints() {
        return bendPoints;
    }

    /**
     *
     * @return
     *      concatenation of {@link PolygonalPath#getStartPoint()} + {@link PolygonalPath#getBendPoints()} +
     *      {@link PolygonalPath#getEndPoint()}.
     */
    public List<Point2D.Double> getTerminalAndBendPoints() {
        List<Point2D.Double> allPoints = new ArrayList<>();
        allPoints.add(getStartPoint());
        allPoints.addAll(getBendPoints());
        allPoints.add(getEndPoint());
        return Collections.unmodifiableList(allPoints);
    }

    public List<Line2D.Double> getSegments() {
        List<Line2D.Double> allSegments = new ArrayList<>(bendPoints.size() + 1);
        Point2D.Double prevPoint = null;
        for (Point2D.Double curPoint : getTerminalAndBendPoints()) {
            if (prevPoint != null) {
                allSegments.add(new Line2D.Double(prevPoint, curPoint));
            }
            prevPoint = curPoint;
        }
        return allSegments;
    }


    /*==========
     * Internal
     *==========*/

    private static List<Point2D.Double> newListWithoutFirstAndLast(List<Point2D.Double> list) {
        LinkedList<Point2D.Double> reducedList = new LinkedList<>(list);
        reducedList.removeFirst();
        reducedList.removeLast();
        return reducedList;
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
