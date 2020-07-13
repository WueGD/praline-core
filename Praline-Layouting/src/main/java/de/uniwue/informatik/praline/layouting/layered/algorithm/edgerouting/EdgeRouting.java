package de.uniwue.informatik.praline.layouting.layered.algorithm.edgerouting;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class EdgeRouting {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private CMResult cmResult;

    public EdgeRouting (SugiyamaLayouter sugy, CMResult cmResult, DrawingInformation drawingInformation) {
        this.sugy = sugy;
        this.drawInfo = drawingInformation;
        this.cmResult = cmResult;
    }

    public void routeEdges () {
        initialise(cmResult);
        double shiftUpValue = 0;
        // for all ranks
        for (int rank = 0; rank < (cmResult.getNodeOrder().size() - 1); rank++) {
            Map<Edge, Integer> edgeToLayerLR = new LinkedHashMap<>();
            ArrayList<Double> activeCandidates = new ArrayList<>();
            LinkedList<ContourPoint> lastPositions = new LinkedList<>();
            LinkedList<ContourPoint> outlineContourLR = new LinkedList<>();
            Map<Double, Edge> conflictCandidates = new LinkedHashMap<>();
            Map<Edge, Edge> conflicts = new LinkedHashMap<>();
            outlineContourLR.add(new ContourPoint(-1, Double.MIN_VALUE));
            lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
            lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
            // for all nodes
            for (Vertex node : cmResult.getNodeOrder().get(rank)) {
                // for all ports
                for (Port bottomPort : cmResult.getTopPortOrder().get(node)) {
                    // cmResult is build with respect to nodes so the topPortOrder are Ports located on the top of the node
                    // here we work with respect to Edges so a Port on top of a node is a bottomPort for an edge
                    Edge edge = bottomPort.getEdges().get(0);
                    Port topPort = edge.getPorts().get(0);
                    if (bottomPort.equals(topPort)) topPort = edge.getPorts().get(1);
                    // if edge from bottom left to top right
                    // else if edge from bottom right to top left
                    // do nothing if it is a straight edge
                    if (bottomPort.getShape().getXPosition() < topPort.getShape().getXPosition()) {
                        // place edge at new level
                        int newContourPointCase = 1;
                        int level = activeCandidates.size();
                        for (int i = (activeCandidates.size() - 1); i >= 0; i--) {
                            if (activeCandidates.get(i) < bottomPort.getShape().getXPosition()) {
                                level = i;
                                if (newContourPointCase == 0) {
                                    newContourPointCase = -1;
                                }
                            } else if (activeCandidates.get(i) > topPort.getShape().getXPosition()) {
                                if (newContourPointCase == 1) {
                                    newContourPointCase = 0;
                                }
                            } else {
                                break;
                            }
                        }
                        // save position
                        edgeToLayerLR.put(edge, level);
                        // update lastPositons
                        while (lastPositions.getLast().getxPosition() < bottomPort.getShape().getXPosition()) {
                            ContourPoint lp = lastPositions.removeLast();
                            lp.setLevel(lastPositions.getLast().getLevel());
                            outlineContourLR.add(lp);
                        }
                        // update activeCandidates
                        if (level < activeCandidates.size()) {
                            activeCandidates.set(level, topPort.getShape().getXPosition());
                        } else {
                            activeCandidates.add(topPort.getShape().getXPosition());
                        }
                        // update outlineContour
                        if (newContourPointCase > -1) {
                            // create new lastPosition and delete all lastPositions left of it
                            ContourPoint newLp = new ContourPoint(level, topPort.getShape().getXPosition());
                            while (lastPositions.getLast().getxPosition() < newLp.getxPosition()) {
                                lastPositions.removeLast();
                            }
                            lastPositions.add(newLp);
                            // update outlineContour
                            outlineContourLR.add(new ContourPoint(level, bottomPort.getShape().getXPosition()));
                        }
                        // add to conflictCandidates to find possible conflicts
                        conflictCandidates.put(bottomPort.getShape().getXPosition(), edge);
                    } else if (bottomPort.getShape().getXPosition() > topPort.getShape().getXPosition()) {
                        // conflict
                        if (conflictCandidates.keySet().contains(topPort.getShape().getXPosition())) {
                            conflicts.put(edge, conflictCandidates.get(topPort.getShape().getXPosition()));
                        }
                    }
                }
            }
            while (lastPositions.size() > 1) {
                ContourPoint lp = lastPositions.removeLast();
                lp.setLevel(lastPositions.getLast().getLevel());
                outlineContourLR.add(lp);
            }
            int maxLRLevel = activeCandidates.size() - 1;
            // do the same with edges from bottom right to top left
            activeCandidates.clear();
            Map<Edge, Integer> edgeToLayer = new LinkedHashMap<>();
            LinkedList<ContourPoint> outlineContourRL = new LinkedList<>();
            outlineContourRL.add(new ContourPoint(-1, Double.MIN_VALUE));
            lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
            // for all nodes
            for (Vertex node : cmResult.getNodeOrder().get(rank + 1)) {
                // for all ports
                for (Port topPort : cmResult.getBottomPortOrder().get(node)) {
                    Edge edge = topPort.getEdges().get(0);
                    Port bottomPort = edge.getPorts().get(0);
                    if (topPort.equals(bottomPort)) bottomPort = edge.getPorts().get(1);
                    // if edge from bottom right to top left
                    // else do nothing
                    if (topPort.getShape().getXPosition() < bottomPort.getShape().getXPosition()) {
                        // place edge
                        int newContourPointCase = 1;
                        int level = activeCandidates.size();
                        for (int i = (activeCandidates.size() - 1); i >= 0; i--) {
                            if (activeCandidates.get(i) < topPort.getShape().getXPosition()) {
                                level = i;
                                if (newContourPointCase == 0) {
                                    newContourPointCase = -1;
                                }
                            } else if (activeCandidates.get(i) > bottomPort.getShape().getXPosition()) {
                                if (newContourPointCase == 1) {
                                    newContourPointCase = 0;
                                }
                            } else {
                                break;
                            }
                        }
                        // save position
                        edgeToLayer.put(edge, level);
                        // update lastPositons
                        while (lastPositions.getLast().getxPosition() < topPort.getShape().getXPosition()) {
                            ContourPoint lp = lastPositions.removeLast();
                            lp.setLevel(lastPositions.getLast().getLevel());
                            outlineContourRL.add(lp);
                        }
                        // update activeCandidates
                        if (level < activeCandidates.size()) {
                            activeCandidates.set(level, bottomPort.getShape().getXPosition());
                        } else {
                            activeCandidates.add(bottomPort.getShape().getXPosition());
                        }
                        // update outlineContour
                        if (newContourPointCase > -1) {
                            // create new lastPosition and delete all lastPositions left of it
                            ContourPoint newLp = new ContourPoint(level, bottomPort.getShape().getXPosition());
                            while (lastPositions.getLast().getxPosition() < newLp.getxPosition()) {
                                lastPositions.removeLast();
                            }
                            lastPositions.add(newLp);
                            // update outlineContour
                            outlineContourRL.add(new ContourPoint(level, topPort.getShape().getXPosition()));
                        }
                    }
                }
            }
            while (lastPositions.size() > 1) {
                ContourPoint lp = lastPositions.removeLast();
                lp.setLevel(lastPositions.getLast().getLevel());
                outlineContourRL.add(lp);
            }

            // place lr-edges relative to rl-edges
            ContourPoint lr = outlineContourLR.removeFirst();
            outlineContourRL.removeFirst();
            int rlLevel = -1;
            int lrLevel = -1;
            ContourPoint rl = outlineContourRL.removeFirst();
            int maxLevelEquals = 0;
            int maxLevel = 0;
            // find closest point
            while (!(outlineContourLR.isEmpty() && outlineContourRL.isEmpty())) {
                // update
                if (lr.getxPosition() > rl.getxPosition()) {
                    rlLevel = rl.getLevel();
                    rl = outlineContourRL.removeFirst();
                    maxLevel = Math.max(maxLevel, rlLevel);
                } else {
                    lrLevel = lr.getLevel();
                    lr = outlineContourLR.removeFirst();
                }
                maxLevelEquals = Math.max(maxLevelEquals, ((rlLevel + 1) - (maxLRLevel - lrLevel)));
            }
            // add lrEdges to all edges
            for (Map.Entry<Edge, Integer> lrEntry : edgeToLayerLR.entrySet()) {
                int newLevel = (maxLRLevel - lrEntry.getValue() + maxLevelEquals);
                edgeToLayer.put(lrEntry.getKey(), newLevel);
                maxLevel = Math.max(maxLevel, newLevel);
            }

            // todo: not implemented yet
            /**for (Map.Entry<Edge, Edge> entry : conflicts.entrySet()) {
                findSolution (entry.getKey(), entry.getValue(), edgeToLayer);
            }*/

            // shift everything up to get enough space for orthogonal edges
            shiftUpValue += (drawInfo.getEdgeDistanceVertical() * maxLevel);
            if (!conflicts.isEmpty()) shiftUpValue += drawInfo.getEdgeDistanceVertical();
            if (shiftUpValue > 0) shiftUp(shiftUpValue, (rank + 1));

            // create bendpoints
            for (Map.Entry<Edge, Integer> entry : edgeToLayer.entrySet()) {
                Edge edge = entry.getKey();
                int level = entry.getValue();
                LinkedList<Point2D.Double> bendPoints = new LinkedList<>();
                Port start = edge.getPorts().get(0);
                Port end = edge.getPorts().get(1);
                if (sugy.getStartNode(edge).equals(end.getVertex())) {
                    start = edge.getPorts().get(1);
                    end = edge.getPorts().get(0);
                }
                double location = start.getShape().getYPosition() + (drawInfo.getDistanceBetweenLayers() / 2) + drawInfo.getPortHeight() + (drawInfo.getEdgeDistanceVertical() * level);
                // todo: place where conflicts are solved by now
                if (!conflicts.containsKey(edge)) {
                    bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), start.getShape().getYPosition()));
                    bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), location));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), location));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), end.getShape().getYPosition()));
                    edge.addPath(new PolygonalPath(bendPoints.removeFirst(), bendPoints.removeLast(), bendPoints, drawInfo.getPortWidth()));
                } else {
                    double location2 = start.getShape().getYPosition() + (drawInfo.getDistanceBetweenLayers() / 2) + (drawInfo.getEdgeDistanceVertical() * (maxLevel + 1));
                    bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), start.getShape().getYPosition()));
                    bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), location));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition() + (drawInfo.getEdgeDistanceVertical() / 2), location));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition() + (drawInfo.getEdgeDistanceVertical() / 2), location2));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), location2));
                    bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), end.getShape().getYPosition()));
                    edge.addPath(new PolygonalPath(bendPoints.removeFirst(), bendPoints.removeLast(), bendPoints, drawInfo.getPortWidth()));
                }
            }
            //todo: foreach conflict -> solve
        }
    }

    private void initialise (CMResult cmResult) {
        this.cmResult = cmResult;
    }

    private void findSolution (Edge brTotl, Edge blTotr, Map<Edge, Integer> edgeToLayer) {

    }

    // shift up all nodes of rank rank with their ports
    private void shiftUp (double shiftUpValue, int rank) {
        for (Vertex node : cmResult.getNodeOrder().get(rank)) {
            Rectangle currentShape = (Rectangle) node.getShape();
            Rectangle newShape = new Rectangle(currentShape.getX(), (currentShape.getY() + shiftUpValue), currentShape.getWidth(), currentShape.getHeight(), currentShape.getColor());
            node.setShape(newShape);
            for (PortComposition portComposition : node.getPortCompositions()) {
                shiftUp(shiftUpValue, portComposition);
            }
        }
    }

    private void shiftUp (double shiftUpValue, PortComposition portComposition) {
        if (portComposition instanceof Port) {
            Rectangle currentShape = (Rectangle) ((Port)portComposition).getShape();
            Rectangle newShape = new Rectangle(currentShape.getX(), (currentShape.getY() + shiftUpValue), currentShape.getWidth(), currentShape.getHeight(), currentShape.getColor());
            ((Port)portComposition).setShape(newShape);
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                shiftUp(shiftUpValue, member);
            }
        }
    }

    private void solveConflict () {

    }
}
