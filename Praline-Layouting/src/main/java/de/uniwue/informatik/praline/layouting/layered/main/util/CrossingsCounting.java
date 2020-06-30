package de.uniwue.informatik.praline.layouting.layered.main.util;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class CrossingsCounting {

    public static int countNumberOfCrossings(Graph graph) {
        //first find all segments
        ArrayList<Line2D.Double> allSegments = new ArrayList<>();
        Map<Line2D.Double, Collection<Line2D.Double>> adjacentSegments = new LinkedHashMap<>();
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
                                if (prevSegment == null) {
                                    registerAdjacentSegmentsOfOtherPaths(adjacentSegments, outsideEndPointsOfPaths,
                                            prevPoint, curSegment);
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
                    }
                }
                //special case: kick out all segments of this edge that are completely covered by another edge
                filterOutOverlayingSegments(allSegments, allSegmentsOfThisEdge);
            }
        }
        //TODO: add adjacent segments for adjacent edges (currently only between paths of the same edge)

        //TODO: repair overlaying paths (already in Sugiyama when composing an edge from multiple paths)

        //now count crossings
        //TODO currently this is done naively in quadratic time. if necessary make it O(n log n) later
        int counter = 0;
        for (int i = 0; i < allSegments.size() - 1; i++) {
            for (int j = i + 1; j < allSegments.size(); j++) {
                Line2D.Double segment0 = allSegments.get(i);
                Line2D.Double segment1 = allSegments.get(j);
                if (!adjacentSegments.containsKey(segment0) || !adjacentSegments.get(segment0).contains(segment1)) {
                    counter += segment0.intersectsLine(segment1) ? 1 : 0;
                }
            }
        }


        return counter;
    }

    private static void filterOutOverlayingSegments(ArrayList<Line2D.Double> allSegments,
                                                    List<Line2D.Double> allSegmentsOfThisEdge) {
        for (int i = 0; i < allSegmentsOfThisEdge.size(); i++) {
            for (int j = 0; j < allSegmentsOfThisEdge.size(); j++) {
                if (i != j) {
                    Line2D.Double seg0 = allSegmentsOfThisEdge.get(i);
                    Line2D.Double seg1 = allSegmentsOfThisEdge.get(j);

                    if (seg0.ptSegDist(seg1.getP1()) == 0 && seg0.ptSegDist(seg1.getP2()) == 0) {
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
            adjacentSegments.computeIfAbsent(adjacentSegment,
                    k -> new ArrayList<>()).add(curSegment);
        }
        segmentsEndingAtPrevPoint.add(curSegment);
    }
}
