package de.uniwue.informatik.praline.layouting.layered.main.util;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.utils.ArithmeticOperation;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class CrossingsCounting {

    public static int countNumberOfCrossings(Graph graph) {
        //first find all segments
        ArrayList<Line2D.Double> allSegments = new ArrayList<>();
        Map<Line2D.Double, Collection<Line2D.Double>> adjacentSegments = new LinkedHashMap<>();
        Map<Line2D.Double, List<Port>> segment2Port = new LinkedHashMap<>();
        Map<Line2D.Double, Edge> segment2Edge = new LinkedHashMap<>();
        Map<Line2D.Double, Collection<Point2D.Double>> pathEndingSegment2EndPoints = new LinkedHashMap<>();
        for (Edge edge : graph.getEdges()) {
            List<Path> paths = edge.getPaths();
            if (paths != null) {
                Map<Point2D.Double, Collection<Line2D.Double>> outsideEndPointsOfPaths = new LinkedHashMap<>();
                List<Line2D.Double> allSegmentsOfThisEdge = new ArrayList<>();
                for (Path path : paths) {
                    if (path instanceof PolygonalPath) {
                        Point2D.Double prevPoint = null;
                        Line2D.Double prevSegment = null;
                        for (Point2D.Double curPoint : ((PolygonalPath) path).getTerminalAndBendPoints()) {
                            if (prevPoint != null) {
                                Line2D.Double curSegment = new Line2D.Double(prevPoint, curPoint);
                                allSegments.add(curSegment);
                                allSegmentsOfThisEdge.add(curSegment);
                                segment2Edge.put(curSegment, edge);
                                if (prevSegment == null) {
                                    registerAdjacentSegmentsOfOtherPaths(adjacentSegments, outsideEndPointsOfPaths,
                                            prevPoint, curSegment);
                                    registerPortAtSegment(segment2Port, edge.getPorts(), prevPoint, curSegment);
                                    pathEndingSegment2EndPoints.computeIfAbsent(curSegment, k -> new ArrayList<>())
                                            .add(prevPoint);
                                }
                                else {
                                    adjacentSegments.computeIfAbsent(prevSegment, k -> new ArrayList<>())
                                            .add(curSegment);
                                }
                                prevSegment = curSegment;
                            }
                            prevPoint = curPoint;
                        }
                        registerAdjacentSegmentsOfOtherPaths(adjacentSegments, outsideEndPointsOfPaths, prevPoint,
                                prevSegment);
                        registerPortAtSegment(segment2Port, edge.getPorts(), prevPoint, prevSegment);
                        pathEndingSegment2EndPoints.computeIfAbsent(prevSegment, k -> new ArrayList<>()).add(prevPoint);
                    }
                }
                //special case: kick out all segments of this edge that are completely covered by another edge
                filterOutOverlayingSegments(allSegments, allSegmentsOfThisEdge);
            }
        }

        //now count crossings
        //TODO currently this is done naively in quadratic time. if necessary make it O(n log n) later
        int counter = 0;
        for (int i = 0; i < allSegments.size() - 1; i++) {
            for (int j = i + 1; j < allSegments.size(); j++) {
                Line2D.Double segment0 = allSegments.get(i);
                Line2D.Double segment1 = allSegments.get(j);
                if (!adjacentSegments.containsKey(segment0) || !adjacentSegments.get(segment0).contains(segment1)) {
                    if (!segment2Port.containsKey(segment0) || !segment2Port.containsKey(segment1) ||
                            !haveCommonPort(segment2Port.get(segment0), segment2Port.get(segment1))) {
                        if (segment2Edge.get(segment0) != segment2Edge.get(segment1) ||
                                !containsEndingPoint(segment0, segment1, pathEndingSegment2EndPoints)) {
                            if (segment0.intersectsLine(segment1)) {
                                ++counter;
                            }
                        }
                    }
                }
            }
        }


        return counter;
    }

    private static boolean haveCommonPort(List<Port> ports0, List<Port> ports1) {
        for (Port port0 : ports0) {
            for (Port port1 : ports1) {
                if (port0.equals(port1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * to avoid couting a crossing in hyperedges (where one path ends at a segment of another path)
     *
     * @param segment0
     * @param segment1
     * @param pathEndingSegment2EndPoints
     * @return
     */
    private static boolean containsEndingPoint(Line2D.Double segment0, Line2D.Double segment1,
                                               Map<Line2D.Double, Collection<Point2D.Double>> pathEndingSegment2EndPoints) {


        Collection<Point2D.Double> endPoints0 = pathEndingSegment2EndPoints.get(segment0);
        Collection<Point2D.Double> endPoints1 = pathEndingSegment2EndPoints.get(segment1);
        boolean returnValue = containsEndingPoints(segment0, endPoints1);
        returnValue |= containsEndingPoints(segment1, endPoints0);
        return returnValue;
    }

    private static boolean containsEndingPoints(Line2D.Double segment, Collection<Point2D.Double> endPoints) {
        if (endPoints != null) {
            for (Point2D.Double endPoint : endPoints) {
                if (ArithmeticOperation.precisionEqual(segment.ptSegDist(endPoint), 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void filterOutOverlayingSegments(ArrayList<Line2D.Double> allSegments,
                                                    List<Line2D.Double> allSegmentsOfThisEdge) {
        for (int i = 0; i < allSegmentsOfThisEdge.size(); i++) {
            for (int j = 0; j < allSegmentsOfThisEdge.size(); j++) {
                if (i != j) {
                    Line2D.Double seg0 = allSegmentsOfThisEdge.get(i);
                    Line2D.Double seg1 = allSegmentsOfThisEdge.get(j);

                    if (ArithmeticOperation.precisionEqual(seg0.ptSegDist(seg1.getP1()), 0)
                            && ArithmeticOperation.precisionEqual(seg0.ptSegDist(seg1.getP2()), 0)) {
                        //if they are identical, remove only if i < j (not to remove both
                        //otherwise seg1 is strictly contained -> we remove it
                        if (i < j || !((seg0.getP1().equals(seg1.getP1()) && seg0.getP1().equals(seg1.getP1()))
                            || (seg0.getP1().equals(seg1.getP1()) && seg0.getP1().equals(seg1.getP1())))) {
                            allSegments.remove(seg1);
                        }
                    }
                }
            }
        }
    }

    private static void registerAdjacentSegmentsOfOtherPaths(
            Map<Line2D.Double, Collection<Line2D.Double>> adjacentSegments,
            Map<Point2D.Double, Collection<Line2D.Double>> outsideEndPointsOfPaths, Point2D.Double prevPoint,
            Line2D.Double curSegment) {
        Collection<Line2D.Double> segmentsEndingAtPrevPoint =
                outsideEndPointsOfPaths.computeIfAbsent(prevPoint, k -> new ArrayList<>());
        for (Line2D.Double adjacentSegment : segmentsEndingAtPrevPoint) {
            adjacentSegments.computeIfAbsent(adjacentSegment, k -> new ArrayList<>()).add(curSegment);
        }
        segmentsEndingAtPrevPoint.add(curSegment);
    }

    private static void registerPortAtSegment(Map<Line2D.Double, List<Port>> segment2Port, List<Port> ports,
                                              Point2D.Double endPoint, Line2D.Double segment) {
        for (Port port : ports) {
            if (((Rectangle) port.getShape()).liesOnBoundary(endPoint)) {
                segment2Port.putIfAbsent(segment, new ArrayList<>(1));
                segment2Port.get(segment).add(port);
            }
        }
    }
}
