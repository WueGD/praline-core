package de.uniwue.informatik.praline.layouting.layered.algorithm.edgerouting;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

import java.awt.geom.Point2D;
import java.util.*;

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

            double level0 = -1; // reference value for calculating the positions of the horizontal edge parts
            for (Vertex node : cmResult.getNodeOrder().get(rank)) {
                if (!cmResult.getTopPortOrder().get(node).isEmpty()) {
                    level0 = cmResult.getTopPortOrder().get(node).get(0).getShape().getYPosition()
                            + drawInfo.getPortHeight() + (drawInfo.getDistanceBetweenLayers() / 2);
                    break;
                }
            }

            List<ContourPoint> outlineContourBB = new ArrayList<>();
            List<ContourPoint> outlineContourTT = new ArrayList<>();
            Map<Edge, Integer> edgeToLayer = new LinkedHashMap<>();
            Map<Edge, Integer> edgeToLayerTop = new LinkedHashMap<>();

            // handle TurningDummys
            handleDummyLayer(cmResult.getNodeOrder().get(rank + 1), false, edgeToLayer, outlineContourBB);
            handleDummyLayer(cmResult.getNodeOrder().get(rank), true, edgeToLayerTop, outlineContourTT);

            List<ContourPoint> outlineContourTop = new LinkedList<>();
            Map<Edge, Edge> conflicts = new LinkedHashMap<>();

            // handle edges from bottom left to top right
            int maxLRLevel = handleLREdges(rank, edgeToLayerTop, outlineContourTop, outlineContourTT, conflicts);
            mergeContour(outlineContourTop, outlineContourTT);

            // do the same with edges from bottom right to top left
            List<ContourPoint> outlineContourBottom = new LinkedList<>();

            handleRLEdges(rank, edgeToLayer, outlineContourBottom, outlineContourBB);
            mergeContour(outlineContourBottom, outlineContourBB);

            // place lr-edges relative to rl-edges
            int maxLevel = placeEdgesFinally(edgeToLayerTop, outlineContourTop, maxLRLevel, edgeToLayer, outlineContourBottom);

            // shift everything up to get enough space for orthogonal edges
            shiftUpValue += (drawInfo.getEdgeDistanceVertical() * maxLevel);
            if (!conflicts.isEmpty()) shiftUpValue += drawInfo.getEdgeDistanceVertical();
            if (shiftUpValue > 0) shiftUp(shiftUpValue, (rank + 1));

            createBendpoints(conflicts, edgeToLayer, maxLevel, level0);
        }
    }

    private void handleDummyLayer(List<Vertex> vertices, boolean top, Map<Edge, Integer> edgeToLayer, List<ContourPoint> outlineContour) {
        outlineContour.add(new ContourPoint(-1,Double.MIN_VALUE));
        List<ContourPoint> lastPositions = new LinkedList<>();
        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        // search for turningDummys which are routed trough the active edge layer and handle them
        List<Double> activeCandidates = new ArrayList<>();
        for (Vertex node : vertices) {
            if (sugy.isTurningPointDummy(node)) {
                List<Port> ports = (top) ? cmResult.getTopPortOrder().get(node) : cmResult.getBottomPortOrder().get(node);
                if (!ports.isEmpty()) {
                    Vertex v = sugy.getVertexOfTurningDummy(node);
                    List<Port> portOrder = (top) ? cmResult.getBottomPortOrder().get(v) : cmResult.getTopPortOrder().get(v);
                    handleTurningDummy(node, portOrder, edgeToLayer, outlineContour, activeCandidates, lastPositions);
                }
            }
        }
        while (lastPositions.size() > 1) {
            ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
            lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
            outlineContour.add(lp);
        }
    }

    private void handleTurningDummy(Vertex node, List<Port> portOrder, Map<Edge, Integer> edgeToLayer, List<ContourPoint> outlineContour, List<Double> activeCandidates, List<ContourPoint> lastPositions) {
        // create two lists of ports; those whose corresponding edge is routed to the left of v
        List<Edge> edgesL = new LinkedList<>();
        // and those whose edge is routed to the right
        List<Edge> edgesR = new LinkedList<>();
        // sort ports into these lists
        double[] interval = sortPortsLeftRight(node, edgesL, edgesR, portOrder);
        // route edges
        Collections.reverse(edgesR);
        handleTurningEdges(new double[]{interval[0], interval[1]}, edgesL, edgeToLayer, outlineContour, activeCandidates, lastPositions);
        handleTurningEdges(new double[]{interval[2], interval[3]}, edgesR, edgeToLayer, outlineContour, activeCandidates, lastPositions);
    }

    private double[] sortPortsLeftRight(Vertex node, List<Edge> edgesL, List<Edge> edgesR, List<Port> portOrder) {
        // interval contains leftmost and rightmost port of all edges going to the left
        // and then leftmost and rightmost port of all edges going to the right
        double[] interval = {portOrder.get(0).getShape().getXPosition(),
                portOrder.get(0).getShape().getXPosition(),
                portOrder.get(portOrder.size() - 1).getShape().getXPosition(),
                portOrder.get(portOrder.size() - 1).getShape().getXPosition()};
        // edge path is going along p1 - e1 - p2 - p3 - e2 - p4
        for (Port p1 : portOrder) {
            //if it has an edge
            if (!p1.getEdges().isEmpty()) {
                Edge e1 = p1.getEdges().get(0);
                Port p2 = e1.getPorts().get(0);
                if (p2.equals(p1)) p2 = e1.getPorts().get(1);
                if (p2.getVertex().equals(node)) {
                    Port p3 = sugy.getCorrespondingPortAtDummy(p2);
                    Edge e2 = p3.getEdges().get(0);
                    Port p4 = e2.getPorts().get(0);
                    if (p4.equals(p3)) p4 = e2.getPorts().get(1);

                    // create new edge
                    List<Port> ports = new LinkedList<>();
                    ports.add(p1); // add startPort at vertex with dummy
                    ports.add(p4); // add endPort of edge segment
                    Edge newEdge = new Edge(ports);
                    sugy.getGraph().addEdge(newEdge);
                    sugy.assignDirection(newEdge, p1.getVertex(), p4.getVertex());
                    sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(e1));

                    // remove old edges from Ports and Graph
                    p1.removeEdge(e1);
                    p2.removeEdge(e1);
                    p3.removeEdge(e2);
                    p4.removeEdge(e2);
                    sugy.getGraph().removeEdge(e1);
                    sugy.getGraph().removeEdge(e2);

                    if (p4.getShape().getXPosition() < p1.getShape().getXPosition()) {
                        edgesL.add(newEdge);

                        // update interval
                        if (interval[0] > p4.getShape().getXPosition()) interval[0] = p4.getShape().getXPosition();
                        if (interval[1] < p1.getShape().getXPosition()) interval[1] = p1.getShape().getXPosition();

                    } else {
                        edgesR.add(newEdge);

                        // update interval
                        if (interval[2] > p1.getShape().getXPosition()) interval[2] = p1.getShape().getXPosition();
                        if (interval[3] < p4.getShape().getXPosition()) interval[3] = p4.getShape().getXPosition();

                    }
                }
            }
        }
        return interval;
    }

    private void handleTurningEdges(double[] interval, List<Edge> edges, Map<Edge, Integer> edgeToLayer, List<ContourPoint> outlineContour, List<Double> activeCandidates, List<ContourPoint> lastPositions) {
        if (!edges.isEmpty()) {
            // find lowest free level
            int level = activeCandidates.size();
            int freeSpaceHeight = edges.size();
            for (int i = (activeCandidates.size() - 1); i >= 0; i--) {
                if (activeCandidates.get(i) < interval[0]) {
                    freeSpaceHeight++;
                    if (freeSpaceHeight >= edges.size()) level = i;
                } else {
                    freeSpaceHeight = 0;
                }
            }

            // place edges
            for (Edge edge : edges) {
                edgeToLayer.put(edge, level);
                // update activeCandidates
                if (level < activeCandidates.size()) {
                    activeCandidates.set(level, interval[1]);
                } else {
                    activeCandidates.add(interval[1]);
                }
                level++;
            }

            // update outlineContour and lastPositons
            while (lastPositions.get(lastPositions.size() - 1).getxPosition() < interval[0]) {
                ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
                lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
                outlineContour.add(lp);
            }
            if (!outlineContour.isEmpty()) {
                ContourPoint lastCP = outlineContour.remove(outlineContour.size() - 1);
                while (interval[0] < lastCP.getxPosition() && level > lastCP.getLevel()) {
                    lastCP = outlineContour.remove(outlineContour.size() - 1);
                    break;
                }
                outlineContour.add(lastCP);
            }
            boolean newContourPointNeeded = true;
            for (int i = (lastPositions.size() - 1); i >= 0; i--) {
                if (lastPositions.get(i).getxPosition() > interval[1]) {
                    if (lastPositions.get(i).getLevel() < level) {
                        if (newContourPointNeeded) {
                            outlineContour.add(new ContourPoint(level, interval[0]));
                            lastPositions.add(new ContourPoint(level, interval[1]));
                        } else {
                            lastPositions.add((i + 1), new ContourPoint(level, interval[1]));
                        }
                    }
                    break;
                } else if (lastPositions.get(i).getLevel() < level) {
                    lastPositions.remove(i);
                } else {
                    newContourPointNeeded = false;
                }
            }
        }
    }

    private int handleLREdges(int rank, Map<Edge, Integer> edgeToLayerLR, List<ContourPoint> outlineContourLR, List<ContourPoint> outlineContourTD, Map<Edge, Edge> conflicts) {
        int maxLRLevel = 0;
        Map<Double, Edge> conflictCandidates = new LinkedHashMap<>();
        List<Double> activeCandidates = new ArrayList<>();
        List<ContourPoint> lastPositions = new LinkedList<>();

        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        outlineContourLR.add(new ContourPoint(-1, Double.MIN_VALUE));

        int[] position = {0};

        // for all nodes
        for (Vertex node : cmResult.getNodeOrder().get(rank)) {
            // if it is no TurningDummy
            if (!(sugy.isTurningPointDummy(node))) {
                // for all ports
                for (Port bottomPort : cmResult.getTopPortOrder().get(node)) {
                    //if it has an edge
                    if (!bottomPort.getEdges().isEmpty()) {
                        // cmResult is build with respect to nodes so the topPortOrder are Ports located on the top of the node
                        // here we work with respect to Edges so a Port on top of a node is a bottomPort for an edge
                        Edge edge = bottomPort.getEdges().get(0);
                        Port topPort = edge.getPorts().get(0);
                        if (bottomPort.equals(topPort)) topPort = edge.getPorts().get(1);
                        // if it is no turningEdge
                        if (bottomPort.getShape().getYPosition() != topPort.getShape().getYPosition()) {
                            // if edge from bottom left to top right
                            // else if edge from bottom right to top left
                            // do nothing if it is a straight edge
                            if (bottomPort.getShape().getXPosition() < topPort.getShape().getXPosition()) {
                                // place edge at new level
                                int newContourPointCase = 1;
                                int minLevel = findMinLevel(bottomPort.getShape().getXPosition(), topPort.getShape().getXPosition(), outlineContourTD, position);
                                while (activeCandidates.size() < minLevel) activeCandidates.add(Double.MIN_VALUE);
                                int level = activeCandidates.size();
                                // find new level
                                for (int i = (activeCandidates.size() - 1); i >= minLevel; i--) {
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
                                while (lastPositions.get(lastPositions.size() - 1).getxPosition() <
                                        bottomPort.getShape().getXPosition()) {
                                    ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
                                    lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
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
                                    while (lastPositions.get(lastPositions.size() - 1).getxPosition() < newLp.getxPosition()) {
                                        lastPositions.remove(lastPositions.size() - 1);
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
                }
            }
        }
        while (lastPositions.size() > 1) {
            ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
            lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
            outlineContourLR.add(lp);
        }
        return maxLRLevel;
    }

    // returns the lowest free level the edge could be placed at without interfering with outlineContourTD
    private int findMinLevel (double left, double right, List<ContourPoint> outlineContourTD, int[] position) {
        // could maybe be sped up by pre computing and storing
        while (outlineContourTD.get(position[0] + 1).getxPosition() < left) {
            position[0]++;
        }
        int minLevel = outlineContourTD.get(position[0]).getLevel();
        for (int i = (position[0] + 1); outlineContourTD.get(i).getxPosition() < right; i++) {
            minLevel = Math.max(minLevel, outlineContourTD.get(i).getLevel());
        }
        return (minLevel + 1);
    }

    // merges both contourlines into the outlineContourBase
    private void mergeContour (List<ContourPoint> outlineContourBase, List<ContourPoint> outlineContourAdditional) {
        LinkedList<ContourPoint> contourB = new LinkedList<>(outlineContourBase);
        LinkedList<ContourPoint> contourA = new LinkedList<>(outlineContourAdditional);
        outlineContourBase.clear();
        ContourPoint lastBasePoint = contourB.removeFirst();
        ContourPoint lastAddPoint = contourA.removeFirst();
        outlineContourBase.add(lastBasePoint);
        while (!(contourA.isEmpty() || contourB.isEmpty())) {
            if (contourB.getFirst().getxPosition() > contourA.getFirst().getxPosition()) {
                if (lastBasePoint.getLevel() < contourA.getFirst().getLevel()) {
                    outlineContourBase.add(contourA.getFirst());
                }
                lastAddPoint = contourA.removeFirst();
            } else if (contourB.getFirst().getxPosition() == contourA.getFirst().getxPosition()) {
                if (lastBasePoint.getLevel() < contourA.getFirst().getLevel()) {
                    outlineContourBase.add(contourA.getFirst());
                } else {
                    outlineContourBase.add(contourB.getFirst());
                }
                lastAddPoint = contourA.removeFirst();
                lastBasePoint = contourB.removeFirst();
            } else {
                if (lastAddPoint.getLevel() < contourB.getFirst().getLevel()) {
                    outlineContourBase.add(contourB.getFirst());
                }
                lastBasePoint = contourB.removeFirst();
            }
        }
    }

    private void handleRLEdges(int rank, Map<Edge, Integer> edgeToLayer, List<ContourPoint> outlineContourRL, List<ContourPoint> outlineContourTD) {
        LinkedList<Double> activeCandidates = new LinkedList<>();
        List<ContourPoint> lastPositions = new LinkedList<>();

        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        lastPositions.add(new ContourPoint(-1, Double.MAX_VALUE));
        outlineContourRL.add(new ContourPoint(-1, Double.MIN_VALUE));

        int[] position = {0};
        // for all nodes
        for (Vertex node : cmResult.getNodeOrder().get(rank + 1)) {
            // if it is no TurningDummy
            if (!(sugy.isTurningPointDummy(node))) {
                // for all ports
                for (Port topPort : cmResult.getBottomPortOrder().get(node)) {
                    //if it has an edge
                    if (!topPort.getEdges().isEmpty()) {
                        Edge edge = topPort.getEdges().get(0);
                        Port bottomPort = edge.getPorts().get(0);
                        if (topPort.equals(bottomPort)) bottomPort = edge.getPorts().get(1);
                        // if it is no turningEdge
                        if (bottomPort.getShape().getYPosition() != topPort.getShape().getYPosition()) {
                            // if edge from bottom right to top left
                            // else do nothing
                            if (topPort.getShape().getXPosition() < bottomPort.getShape().getXPosition()) {
                                // place edge at new level
                                int newContourPointCase = 1;
                                int minLevel = findMinLevel(topPort.getShape().getXPosition(), bottomPort.getShape().getXPosition(), outlineContourTD, position);
                                while (activeCandidates.size() < minLevel) activeCandidates.add(Double.MIN_VALUE);
                                int level = activeCandidates.size();
                                // find new level
                                for (int i = (activeCandidates.size() - 1); i >= minLevel; i--) {
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
                                while (lastPositions.get(lastPositions.size() - 1).getxPosition() < topPort.getShape().getXPosition()) {
                                    ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
                                    lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
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
                                    while (lastPositions.get(lastPositions.size() - 1).getxPosition() < newLp.getxPosition()) {
                                        lastPositions.remove(lastPositions.size() - 1);
                                    }
                                    lastPositions.add(newLp);
                                    // update outlineContour
                                    outlineContourRL.add(new ContourPoint(level, topPort.getShape().getXPosition()));
                                }
                            }
                        }
                    }
                }
            }
        }
        while (lastPositions.size() > 1) {
            ContourPoint lp = lastPositions.remove(lastPositions.size() - 1);
            lp.setLevel(lastPositions.get(lastPositions.size() - 1).getLevel());
            outlineContourRL.add(lp);
        }
    }

    private int placeEdgesFinally(Map<Edge, Integer> edgeToLayerLR, List<ContourPoint> outlineContourLR, int maxLRLevel, Map<Edge, Integer> edgeToLayer, List<ContourPoint> outlineContourRL) {
        ContourPoint lr = outlineContourLR.remove(0);
        outlineContourRL.remove(0);
        int rlLevel = -1;
        int lrLevel = -1;
        ContourPoint rl = outlineContourRL.remove(0);
        int maxLevelEquals = 0;
        int maxLevel = 0;
        // find closest point
        while (!(outlineContourLR.isEmpty() && outlineContourRL.isEmpty())) {
            // update
            if (lr.getxPosition() > rl.getxPosition()) {
                rlLevel = rl.getLevel();
                rl = outlineContourRL.remove(0);
                maxLevel = Math.max(maxLevel, rlLevel);
            } else {
                lrLevel = lr.getLevel();
                lr = outlineContourLR.remove(0);
            }
            maxLevelEquals = Math.max(maxLevelEquals, ((rlLevel + 1) - (maxLRLevel - lrLevel)));
        }
        // add lrEdges to all edges
        for (Map.Entry<Edge, Integer> lrEntry : edgeToLayerLR.entrySet()) {
            int newLevel = (maxLRLevel - lrEntry.getValue() + maxLevelEquals);
            edgeToLayer.put(lrEntry.getKey(), newLevel);
            maxLevel = Math.max(maxLevel, newLevel);
        }
        return maxLevel;
    }

    private void createBendpoints(Map<Edge, Edge> conflicts, Map<Edge, Integer> edgeToLayer, int maxLevel, double level0) {
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
            double location = level0 + (drawInfo.getEdgeDistanceVertical() * level);
            if (!conflicts.containsKey(edge)) {
                bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), start.getShape().getYPosition()));
                bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), location));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), location));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), end.getShape().getYPosition()));
            } else {
                double location2 = start.getShape().getYPosition() + (drawInfo.getDistanceBetweenLayers() / 2) + (drawInfo.getEdgeDistanceVertical() * (maxLevel + 1));
                bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), start.getShape().getYPosition()));
                bendPoints.add(new Point2D.Double(start.getShape().getXPosition(), location));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition() + (drawInfo.getEdgeDistanceVertical() / 2), location));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition() + (drawInfo.getEdgeDistanceVertical() / 2), location2));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), location2));
                bendPoints.add(new Point2D.Double(end.getShape().getXPosition(), end.getShape().getYPosition()));
            }
            edge.addPath(new PolygonalPath(bendPoints.removeFirst(), bendPoints.removeLast(), bendPoints, drawInfo.getPortWidth()));
        }
    }

    private void initialise (CMResult cmResult) {
        this.cmResult = cmResult;
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

}
