package de.uniwue.informatik.praline.layouting.layered.algorithm.drawing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

import java.awt.geom.Point2D;
import java.util.*;

public class DrawingPreparation {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private CMResult cmResult;
    private double delta;

    public DrawingPreparation (SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    private void initialise (DrawingInformation drawInfo, CMResult cmResult) {
        this.drawInfo = drawInfo;
        this.delta = Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing());
        this.cmResult = cmResult;
    }

    private void construct() {
        for (List<Port> topPorts : cmResult.getTopPortOrder().values()) {
            for (Port topPort : topPorts) {
                topPort.getLabelManager().setMainLabel(new TextLabel(topPort.getLabelManager().getMainLabel().toString() + "t"));
            }
        }
        for (List<Port> bottomPorts : cmResult.getBottomPortOrder().values()) {
            for (Port bottomPort : bottomPorts) {
                bottomPort.getLabelManager().setMainLabel(new TextLabel(bottomPort.getLabelManager().getMainLabel().toString() + "b"));
            }
        }
    }

    public void prepareDrawing(DrawingInformation drawInfo, CMResult cmResult) {
        initialise(drawInfo, cmResult);
        construct();
        Collection<Vertex> vertices = new LinkedHashSet<>(sugy.getGraph().getVertices());
        Map<Vertex, Edge> deletedDummys = new LinkedHashMap<>();
        List<List<Point2D.Double>> allPaths = new LinkedList<>();
        for (Vertex node : vertices) {
            // remove edge Us
            boolean handled = false;
            /**
            if (sugy.isTurningPointDummy(node) && node.getPorts().size() == 2) {
                Port p1 = null;
                Port p2 = null;
                for (Port port : node.getPorts()) {
                    if (p1 == null) p1 = port;
                    else p2 = port;
                }
                // todo: if should be unnecessary and always be true
                if ((p1.getLabelManager().getMainLabel().toString().endsWith("t") && p2.getLabelManager().getMainLabel().toString().endsWith("t")) || (p1.getLabelManager().getMainLabel().toString().endsWith("b") && p2.getLabelManager().getMainLabel().toString().endsWith("b"))) {
                    Edge edge = null;
                    Edge edge2 = null;
                    Edge newEdge = null;
                    if ((!p1.getEdges().get(0).getPaths().isEmpty()) && (!p2.getEdges().get(0).getPaths().isEmpty())) {
                        edge = p1.getEdges().get(0);
                        edge2 = p2.getEdges().get(0);
                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>(((PolygonalPath) edge.getPaths().get(0)).getTerminalAndBendPoints());
                        LinkedList<Point2D.Double> pathPoints2 = new LinkedList<>(((PolygonalPath) edge2.getPaths().get(0)).getTerminalAndBendPoints());
                        // find endPoints at dummy node and create path
                        if (pathPoints.getFirst().getX() == p1.getShape().getXPosition()) {
                            pathPoints.removeFirst();
                            Collections.reverse(pathPoints);
                        } else {
                            pathPoints.removeLast();
                        }
                        if (pathPoints2.getFirst().getX() == p2.getShape().getXPosition()) {
                            pathPoints2.removeFirst();
                        } else {
                            pathPoints2.removeLast();
                            Collections.reverse(pathPoints2);
                        }
                        pathPoints.add(new Point2D.Double(pathPoints.getLast().getX(), pathPoints2.getFirst().getY()));
                        pathPoints.addAll(pathPoints2);

//                        // shift pathPoints to prevent overlapping
//                        for (Point2D.Double pP : pathPoints) {
//                            if ((!pP.equals(pathPoints.getFirst())) && (!pP.equals(pathPoints.getLast()))) {
//                                if (p1.getLabelManager().getMainLabel().toString().endsWith("t")) {
//                                    pP.setLocation(pP.getX(), (pP.getY() + (drawInfo.getEdgeDistanceVertical() / 2)));
//                                } else {
//                                    pP.setLocation(pP.getX(), (pP.getY() - (drawInfo.getEdgeDistanceVertical() / 2)));
//                                }
//                            }
//                        }

                        // create Edge to replace dummy node
                        allPaths.add(new LinkedList<>(pathPoints));
                        PolygonalPath pathForNewEdge = new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        if (edge.getPorts().get(0).equals(p1)) {
                            portsForNewEdge.add(edge.getPorts().get(1));
                        } else {
                            portsForNewEdge.add(edge.getPorts().get(0));
                        }
                        if (edge2.getPorts().get(0).equals(p2)) {
                            portsForNewEdge.add(edge2.getPorts().get(1));
                        } else {
                            portsForNewEdge.add(edge2.getPorts().get(0));
                        }
                        newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(pathForNewEdge);
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge));
                        Vertex endNode0 = portsForNewEdge.get(0).getVertex();
                        Vertex endNode1 = portsForNewEdge.get(1).getVertex();
                        sugy.assignDirection(newEdge,
                                sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode0 : endNode1,
                                sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode1 : endNode0);
                    } else {
                        for (Port port : node.getPorts()) {
                            edge = port.getEdges().get(0);
                            if (!edge.getPaths().isEmpty()) {
                                if (port.equals(p2)) {
                                    p2 = p1;
                                    p1 = port;
                                }
                                edge2 = p2.getEdges().get(0);
                                Port endPort = edge2.getPorts().get(0);
                                if (endPort.equals(p2)) endPort = edge2.getPorts().get(1);
                                Port startPort = edge.getPorts().get(0);
                                if (startPort.equals(p1)) startPort = edge.getPorts().get(1);

                                LinkedList<Point2D.Double> pathPoints = new LinkedList<>(((PolygonalPath) edge.getPaths().get(0)).getTerminalAndBendPoints());
                                if (pathPoints.getFirst().getX() == p1.getShape().getXPosition()) {
                                    Collections.reverse(pathPoints);
                                }
                                //pathPoints = new LinkedList<>(pathPoints.subList(0, (pathPoints.size() - 1)));
                                pathPoints = new LinkedList<>(pathPoints.subList(0, 3));
                                Point2D.Double toChange = pathPoints.getLast();
                                toChange.setLocation(endPort.getShape().getXPosition(), toChange.getY());
                                pathPoints.add(new Point2D.Double(endPort.getShape().getXPosition(), endPort.getShape().getYPosition()));

//                                // shift pathPoints to prevent overlapping
//                                for (Point2D.Double pP : pathPoints) {
//                                    if ((!pP.equals(pathPoints.getFirst())) && (!pP.equals(pathPoints.getLast()))) {
//                                        if (p1.getLabelManager().getMainLabel().toString().endsWith("t")) {
//                                            pP.setLocation(pP.getX(), (pP.getY() + (drawInfo.getEdgeDistanceVertical() / 2)));
//                                        } else {
//                                            pP.setLocation(pP.getX(), (pP.getY() - (drawInfo.getEdgeDistanceVertical() / 2)));
//                                        }
//                                    }
//                                }

                                allPaths.add(new LinkedList<>(pathPoints));
                                PolygonalPath pathForNewEdge = new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints);
                                List<Port> portsForNewEdge = new LinkedList<>();
                                portsForNewEdge.add(startPort);
                                portsForNewEdge.add(endPort);
                                newEdge = new Edge(portsForNewEdge);
                                newEdge.addPath(pathForNewEdge);
                                sugy.getGraph().addEdge(newEdge);
                                sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge));
                                Vertex endNode0 = portsForNewEdge.get(0).getVertex();
                                Vertex endNode1 = portsForNewEdge.get(1).getVertex();
                                sugy.assignDirection(newEdge,
                                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode0 : endNode1,
                                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode1 : endNode0);
                                break;
                            }
                        }
                    }
                    if (edge2 != null) {
                        handled = true;
                        sugy.getGraph().removeEdge(edge);
                        sugy.getGraph().removeEdge(edge2);
                        sugy.getGraph().removeVertex(node);
                        deletedDummys.put(node, newEdge);
                    }
                } else {
                    System.out.println("if is not unnecessary");
                }
            }
            */
            if (sugy.isTurningPointDummy(node) && !handled) {
                if (cmResult.getTopPortOrder().get(node).isEmpty()) {
                    // create two lists of ports; those whose corresponding edge is routed to the left of v
                    List<Port> portsL = new LinkedList<>();
                    // and those whose edge is routed to the right
                    List<Port> portsR = new LinkedList<>();
                    Vertex v = sugy.getVertexOfTurningDummy(node);
                    // sort ports into these lists
                    for (Port port : cmResult.getBottomPortOrder().get(node)) {
                        Edge edge = port.getEdges().get(0);
                        Port p2 = edge.getPorts().get(0);
                        if (p2.equals(port)) p2 = edge.getPorts().get(1);
                        if (p2.getVertex().equals(v)) {
                            p2 = sugy.getCorrespondingPortAtDummy(port);
                            if (p2.getShape().getXPosition() < port.getShape().getXPosition()) {
                                portsR.add(port);
                            } else {
                                portsL.add(port);
                            }
                        }
                    }
                    // route edges
                    int level = 0;
                    Collections.reverse(portsL);
                    for (Port p2 : portsL) {
                        Edge edge1 = p2.getEdges().get(0);
                        Port p1 = edge1.getPorts().get(0);
                        if (p2.equals(p1)) p1 = edge1.getPorts().get(1);
                        Port p3 = sugy.getCorrespondingPortAtDummy(p2);
                        Edge edge2 = p3.getEdges().get(0);
                        Port p4 = edge2.getPorts().get(0);
                        if (p3.equals(p4)) p4 = edge2.getPorts().get(1);
                        // new path is going along p1 - edge1 - p2 - p3 - edge2 - p4
                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(p1.getShape().getXPosition(), p1.getShape().getYPosition()));
                        if (!edge1.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge1.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
                        pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
                        if (!edge2.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge2.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p4.getShape().getXPosition(), p4.getShape().getYPosition()));
                        sugy.getGraph().removeEdge(edge1);
                        sugy.getGraph().removeEdge(edge2);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(p1);
                        portsForNewEdge.add(p4);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints));
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge1));
                        level++;
                    }
                    level = 0;
                    for (Port p2 : portsR) {
                        Edge edge1 = p2.getEdges().get(0);
                        Port p1 = edge1.getPorts().get(0);
                        if (p2.equals(p1)) p1 = edge1.getPorts().get(1);
                        Port p3 = sugy.getCorrespondingPortAtDummy(p2);
                        Edge edge2 = p3.getEdges().get(0);
                        Port p4 = edge2.getPorts().get(0);
                        if (p3.equals(p4)) p4 = edge2.getPorts().get(1);
                        // new path is going along p1 - edge1 - p2 - p3 - edge2 - p4
                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(p1.getShape().getXPosition(), p1.getShape().getYPosition()));
                        if (!edge1.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge1.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
                        pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
                        if (!edge2.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge2.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p4.getShape().getXPosition(), p4.getShape().getYPosition()));
                        sugy.getGraph().removeEdge(edge1);
                        sugy.getGraph().removeEdge(edge2);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(p1);
                        portsForNewEdge.add(p4);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints));
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge1));
                        level++;
                    }
                } else {
                    // create two lists of ports; those whose corresponding edge is routed to the left of v
                    List<Port> portsL = new LinkedList<>();
                    // and those whose edge is routed to the right
                    List<Port> portsR = new LinkedList<>();
                    Vertex v = sugy.getVertexOfTurningDummy(node);
                    // sort ports into these lists
                    for (Port port : cmResult.getTopPortOrder().get(node)) {
                        Edge edge = port.getEdges().get(0);
                        Port p2 = edge.getPorts().get(0);
                        if (p2.equals(port)) p2 = edge.getPorts().get(1);
                        if (p2.getVertex().equals(v)) {
                            p2 = sugy.getCorrespondingPortAtDummy(port);
                            if (p2.getShape().getXPosition() < port.getShape().getXPosition()) {
                                portsR.add(port);
                            } else {
                                portsL.add(port);
                            }
                        }
                    }
                    // route edges
                    int level = 0;
                    Collections.reverse(portsL);
                    for (Port p2 : portsL) {
                        Edge edge1 = p2.getEdges().get(0);
                        Port p1 = edge1.getPorts().get(0);
                        if (p2.equals(p1)) p1 = edge1.getPorts().get(1);
                        Port p3 = sugy.getCorrespondingPortAtDummy(p2);
                        Edge edge2 = p3.getEdges().get(0);
                        Port p4 = edge2.getPorts().get(0);
                        if (p3.equals(p4)) p4 = edge2.getPorts().get(1);
                        // new path is going along p1 - edge1 - p2 - p3 - edge2 - p4
                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(p1.getShape().getXPosition(), p1.getShape().getYPosition()));
                        if (!edge1.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge1.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
                        pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
                        if (!edge2.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge2.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p4.getShape().getXPosition(), p4.getShape().getYPosition()));
                        sugy.getGraph().removeEdge(edge1);
                        sugy.getGraph().removeEdge(edge2);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(p1);
                        portsForNewEdge.add(p4);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints));
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge1));
                        level++;
                    }
                    level = 0;
                    for (Port p2 : portsR) {
                        Edge edge1 = p2.getEdges().get(0);
                        Port p1 = edge1.getPorts().get(0);
                        if (p2.equals(p1)) p1 = edge1.getPorts().get(1);
                        Port p3 = sugy.getCorrespondingPortAtDummy(p2);
                        Edge edge2 = p3.getEdges().get(0);
                        Port p4 = edge2.getPorts().get(0);
                        if (p3.equals(p4)) p4 = edge2.getPorts().get(1);
                        // new path is going along p1 - edge1 - p2 - p3 - edge2 - p4
                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(p1.getShape().getXPosition(), p1.getShape().getYPosition()));
                        if (!edge1.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge1.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
                        pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
                        if (!edge2.getPaths().isEmpty()) {
                            LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge2.getPaths().get(0)).getTerminalAndBendPoints());
                            if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                            pathPointsEdge.removeFirst();
                            pathPointsEdge.removeLast();
                            pathPoints.addAll(pathPointsEdge);
                        }
                        pathPoints.add(new Point2D.Double(p4.getShape().getXPosition(), p4.getShape().getYPosition()));
                        sugy.getGraph().removeEdge(edge1);
                        sugy.getGraph().removeEdge(edge2);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(p1);
                        portsForNewEdge.add(p4);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints));
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getDummyEdge2RealEdge().put(newEdge, sugy.getDummyEdge2RealEdge().get(edge1));
                        level++;
                    }
                }
            } else {
                // tighten node to smallest width possible
                // find leftmost and rightmost Port
                Rectangle nodeShape = (Rectangle) node.getShape();
                double minL = nodeShape.getXPosition();
                double maxL = nodeShape.getXPosition() + nodeShape.getWidth();
                double minR = minL;
                double maxR = maxL;
                for (Port port : node.getPorts()) {
                    maxL = Math.min(maxL, port.getShape().getXPosition());
                    minR = Math.max(minR, port.getShape().getXPosition());
                }
                maxL -= (drawInfo.getPortWidth() + (delta / 2));
                minR += (drawInfo.getPortWidth() + (delta / 2));
                // tighten to smallest width possible
                double size = nodeShape.getWidth();
                double minSize = minR - maxL;
                minSize = Math.max(minSize, (sugy.getTextWidthForNode(node) + (drawInfo.getBorderWidth() * 2)));
                if (minSize < size) {
                    double dif = size - minSize;
                    double newL;
                    double newR;
                    if ((maxL - minL) < (dif / 2)) {
                        newL = maxL;
                        newR = maxR - (dif - (maxL - minL));
                    } else if ((maxR - minR) < (dif / 2)) {
                        newL = minL + (dif - (maxR - minR));
                        newR = minR;
                    } else {
                        newL = minL + (dif / 2);
                        newR = maxR - (dif / 2);
                    }
                    node.setShape(new Rectangle(newL, nodeShape.getYPosition(), (newR - newL), nodeShape.getHeight(), null));
                }
            }
        }
        // fix overlapping edges between deleted U's
        Point2D.Double point1 = new Point2D.Double(0,-2);
        Point2D.Double point2 = new Point2D.Double(0,-2);
        Point2D.Double point3 = new Point2D.Double(0,-2);
        Point2D.Double point4 = new Point2D.Double(0,-2);
        for (List<Point2D.Double> pathPoints : allPaths) {
            if (pathPoints.size() == 6) {
                if (pathPoints.get(3).getY() == point1.getY()) {
                    double x1 = pathPoints.get(3).getX();
                    double x2 = pathPoints.get(4).getX();
                    double xa = point1.getX();
                    double xb = point2.getX();
                    if ((x1 < xa && x1 > xb) || (x1 > xa && x1 < xb) || (x2 < xa && x2 > xb) || (x2 > xa && x2 < xb)) {
                        pathPoints.get(3).y += (drawInfo.getEdgeDistanceVertical() / 2);
                        pathPoints.get(4).y += (drawInfo.getEdgeDistanceVertical() / 2);
                    }
                } else if (pathPoints.get(3).getY() == point3.getY()) {
                    double x1 = pathPoints.get(3).getX();
                    double x2 = pathPoints.get(4).getX();
                    double xa = point3.getX();
                    double xb = point4.getX();
                    if ((x1 < xa && x1 > xb) || (x1 > xa && x1 < xb) || (x2 < xa && x2 > xb) || (x2 > xa && x2 < xb)) {
                        pathPoints.get(3).y += (drawInfo.getEdgeDistanceVertical() / 2);
                        pathPoints.get(4).y += (drawInfo.getEdgeDistanceVertical() / 2);
                    }
                } else {
                    point3 = pathPoints.get(3);
                    point4 = pathPoints.get(4);
                }
            }
            if (pathPoints.get(1).getY() == point1.getY()) {
                double x1 = pathPoints.get(1).getX();
                double x2 = pathPoints.get(2).getX();
                double xa = point1.getX();
                double xb = point2.getX();
                if ((x1 < xa && x1 > xb) || (x1 > xa && x1 < xb) || (x2 < xa && x2 > xb) || (x2 > xa && x2 < xb)) {
                    pathPoints.get(1).y += (drawInfo.getEdgeDistanceVertical() / 2);
                    pathPoints.get(2).y += (drawInfo.getEdgeDistanceVertical() / 2);
                }
            } else if (pathPoints.get(1).getY() == point3.getY()) {
                double x1 = pathPoints.get(1).getX();
                double x2 = pathPoints.get(2).getX();
                double xa = point3.getX();
                double xb = point4.getX();
                if ((x1 < xa && x1 > xb) || (x1 > xa && x1 < xb) || (x2 < xa && x2 > xb) || (x2 > xa && x2 < xb)) {
                    pathPoints.get(1).y += (drawInfo.getEdgeDistanceVertical() / 2);
                    pathPoints.get(2).y += (drawInfo.getEdgeDistanceVertical() / 2);
                }
            } else {
                point1 = pathPoints.get(1);
                point2 = pathPoints.get(2);
            }
        }
        // delete dummyLayers and fix overlapping edges
        /**
        double shiftValue = 0;
        for (int rank = 0; rank < cmResult.getNodeOrder().size(); rank++) {
            boolean dummyLayer = true;
            for (Vertex node : cmResult.getNodeOrder().get(rank)) {
                if (!sugy.isDummy(node)) {
                    dummyLayer = false;
                }
            }
            if (shiftValue != 0) {
                shift(shiftValue, rank, true);
            }
            if (dummyLayer) {
                List<Edge> overlappingEdgeToMove = new LinkedList<>();
                Set<Vertex> tt = new LinkedHashSet<>();
                Set<Vertex> bb = new LinkedHashSet<>();
                Set<Vertex> bt = new LinkedHashSet<>();
                SortedSet<Double> layers = new TreeSet<>();
                Point2D.Double lastEnd = new Point2D.Double(0,0);
                for (Vertex node : cmResult.getNodeOrder().get(rank)) {
                    Port p1 = (Port) node.getPortCompositions().get(0);
                    Port p2 = (Port) node.getPortCompositions().get(1);
                    if (deletedDummys.containsKey(node)) {
                        if (((PolygonalPath)deletedDummys.get(node).getPaths().get(0)).getBendPoints().size() == 2) {
                            PolygonalPath path = (PolygonalPath) deletedDummys.get(node).getPaths().get(0);
                            double x = Math.min(path.getBendPoints().get(0).getX(), path.getBendPoints().get(1).getX());
                            double y = path.getBendPoints().get(0).getY();
                            if (x > lastEnd.getX()) {
                                overlappingEdgeToMove.add(deletedDummys.get(node));
                                layers.add(y);
                            }
                            x = Math.max(path.getBendPoints().get(0).getX(), path.getBendPoints().get(1).getX());
                            lastEnd = new Point2D.Double(x, y);
                        }
                    } else if (p1.getLabelManager().getMainLabel().toString().endsWith("t") && p2.getLabelManager().getMainLabel().toString().endsWith("t")) {
                        tt.add(node);
                    } else if (p1.getLabelManager().getMainLabel().toString().endsWith("b") && p2.getLabelManager().getMainLabel().toString().endsWith("b")) {
                        bb.add(node);
                    } else {
                        bt.add(node);
                    }
                }
                if (!bb.isEmpty()) {
                    // move all edges below
                    double minY = Double.MAX_VALUE;
                    for (Vertex n : cmResult.getNodeOrder().get(rank - 1)) {
                        for (Port p : cmResult.getTopPortOrder().get(n)) {
                            Edge e = p.getEdges().get(0);
                            if (!e.getPaths().isEmpty()) {
                                for (Point2D.Double point : ((PolygonalPath)e.getPaths().get(0)).getBendPoints()) {
                                    if (point.getY() < minY) minY = point.getY();
                                    point.y += drawInfo.getEdgeDistanceVertical();
                                }
                                Point2D.Double start = ((PolygonalPath)e.getPaths().get(0)).getStartPoint();
                                Point2D.Double end = ((PolygonalPath)e.getPaths().get(0)).getEndPoint();
                                if (start.getY() < end.getY()) {
                                    end.y += drawInfo.getEdgeDistanceVertical();
                                } else {
                                    start.y += drawInfo.getEdgeDistanceVertical();
                                }
                            }
                        }
                    }
                    // replace node by Edge
                    for (Vertex node : bb) {
                        Port p1 = (Port) node.getPortCompositions().get(0);
                        Port p2 = (Port) node.getPortCompositions().get(1);
                        Edge edge = p1.getEdges().get(0);
                        Edge edge2 = p2.getEdges().get(0);
                        Port endPort = edge.getPorts().get(0);
                        if (endPort.equals(p1)) endPort = edge.getPorts().get(1);
                        Port startPort = edge2.getPorts().get(0);
                        if (startPort.equals(p2)) startPort = edge2.getPorts().get(1);
                        if (minY == Double.MAX_VALUE) {
                            minY = (endPort.getShape().getYPosition() + ((p1.getShape().getYPosition() - endPort.getShape().getYPosition()) / 2));
                        }

                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(startPort.getShape().getXPosition(), startPort.getShape().getYPosition()));
                        pathPoints.add(new Point2D.Double(startPort.getShape().getXPosition(), minY));
                        pathPoints.add(new Point2D.Double(endPort.getShape().getXPosition(), minY));
                        pathPoints.add(new Point2D.Double(endPort.getShape().getXPosition(), endPort.getShape().getYPosition()));

                        PolygonalPath pathForNewEdge = new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(startPort);
                        portsForNewEdge.add(endPort);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(pathForNewEdge);
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getGraph().removeEdge(edge);
                        sugy.getGraph().removeEdge(edge2);
                        sugy.getGraph().removeVertex(node);
                        deletedDummys.put(node, newEdge);
                    }
                    shiftValue += drawInfo.getEdgeDistanceVertical();
                    shift(drawInfo.getEdgeDistanceVertical(), rank, false);
                }
                if (!layers.isEmpty()) {
                    if (rank > 0) {
                        for (Vertex n : cmResult.getNodeOrder().get(rank - 1)) {
                            for (Port p : cmResult.getTopPortOrder().get(n)) {
                                Edge e = p.getEdges().get(0);
                                if (!e.getPaths().isEmpty()) {
                                    for (Point2D.Double point : ((PolygonalPath) e.getPaths().get(0)).getTerminalAndBendPoints()) {
                                        for (double value : layers) {
                                            if (value < point.getY()) {
                                                point.y += drawInfo.getEdgeDistanceVertical();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rank < (cmResult.getNodeOrder().size() - 1)) {
                        for (Vertex n : cmResult.getNodeOrder().get(rank + 1)) {
                            for (Port p : cmResult.getBottomPortOrder().get(n)) {
                                Edge e = p.getEdges().get(0);
                                if (!e.getPaths().isEmpty()) {
                                    for (Point2D.Double point : ((PolygonalPath) e.getPaths().get(0)).getTerminalAndBendPoints()) {
                                        for (double value : layers) {
                                            if (value < point.getY()) {
                                                point.y += drawInfo.getEdgeDistanceVertical();
                                            }
                                        }
                                        point.y -= (drawInfo.getEdgeDistanceVertical() * layers.size());
                                    }
                                }
                            }
                        }
                    }
                    double lastX = 0;
                    double lastY = 0;
                    for (Edge edge : overlappingEdgeToMove) {
                        Point2D.Double point1 = ((PolygonalPath)edge.getPaths().get(0)).getBendPoints().get(0);
                        Point2D.Double point2 = ((PolygonalPath)edge.getPaths().get(0)).getBendPoints().get(1);
                        if (!(point1.getY() + drawInfo.getEdgeDistanceVertical() == lastY && (point1.getX() < lastX || point2.getX() < lastX))) {
                            point1.y += drawInfo.getEdgeDistanceVertical();
                            point2.y += drawInfo.getEdgeDistanceVertical();
                        }
                        lastY = point1.getY();
                        lastX = Math.max(point1.getX(), point2.getX());
                    }
                    shiftValue += (drawInfo.getEdgeDistanceVertical() * layers.size());
                }
                if (!tt.isEmpty()) {
                    // move all edges below
                    double maxY = Double.MIN_VALUE;
                    for (Vertex n : cmResult.getNodeOrder().get(rank + 1)) {
                        for (Port p : cmResult.getBottomPortOrder().get(n)) {
                            Edge e = p.getEdges().get(0);
                            if (!e.getPaths().isEmpty()) {
                                for (Point2D.Double point : ((PolygonalPath)e.getPaths().get(0)).getBendPoints()) {
                                    if (point.getY() > maxY) maxY = point.getY();
                                    point.y -= drawInfo.getEdgeDistanceVertical();
                                }
                                Point2D.Double start = ((PolygonalPath)e.getPaths().get(0)).getStartPoint();
                                Point2D.Double end = ((PolygonalPath)e.getPaths().get(0)).getEndPoint();
                                if (start.getY() > end.getY()) {
                                    end.y -= drawInfo.getEdgeDistanceVertical();
                                } else {
                                    start.y -= drawInfo.getEdgeDistanceVertical();
                                }
                            }
                        }
                    }
                    // replace node by Edge
                    for (Vertex node : tt) {
                        Port p1 = (Port) node.getPortCompositions().get(0);
                        Port p2 = (Port) node.getPortCompositions().get(1);
                        Edge edge = p1.getEdges().get(0);
                        Edge edge2 = p2.getEdges().get(0);
                        Port endPort = edge.getPorts().get(0);
                        if (endPort.equals(p1)) endPort = edge.getPorts().get(1);
                        Port startPort = edge2.getPorts().get(0);
                        if (startPort.equals(p2)) startPort = edge2.getPorts().get(1);
                        if (maxY == Double.MIN_VALUE) {
                            maxY = (endPort.getShape().getYPosition() + ((p1.getShape().getYPosition() - endPort.getShape().getYPosition()) / 2));
                        }

                        LinkedList<Point2D.Double> pathPoints = new LinkedList<>();
                        pathPoints.add(new Point2D.Double(startPort.getShape().getXPosition(), startPort.getShape().getYPosition()));
                        pathPoints.add(new Point2D.Double(startPort.getShape().getXPosition(), maxY));
                        pathPoints.add(new Point2D.Double(endPort.getShape().getXPosition(), maxY));
                        pathPoints.add(new Point2D.Double(endPort.getShape().getXPosition(), endPort.getShape().getYPosition()));

                        PolygonalPath pathForNewEdge = new PolygonalPath(pathPoints.removeFirst(), pathPoints.removeLast(), pathPoints);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(startPort);
                        portsForNewEdge.add(endPort);
                        Edge newEdge = new Edge(portsForNewEdge);
                        newEdge.addPath(pathForNewEdge);
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getGraph().removeEdge(edge);
                        sugy.getGraph().removeEdge(edge2);
                        sugy.getGraph().removeVertex(node);
                        deletedDummys.put(node, newEdge);
                    }
                    shiftValue += drawInfo.getEdgeDistanceVertical();
                }
                if (!bt.isEmpty()) {
                    for (Vertex node : bt) {
                        Port p1 = (Port) node.getPortCompositions().get(0);
                        Port p2 = (Port) node.getPortCompositions().get(1);
                        Edge edge = p1.getEdges().get(0);
                        Edge edge2 = p2.getEdges().get(0);
                        Port endPort = edge.getPorts().get(0);
                        if (endPort.equals(p1)) endPort = edge.getPorts().get(1);
                        Port startPort = edge2.getPorts().get(0);
                        if (startPort.equals(p2)) startPort = edge2.getPorts().get(1);
                        List<Port> portsForNewEdge = new LinkedList<>();
                        portsForNewEdge.add(startPort);
                        portsForNewEdge.add(endPort);
                        Edge newEdge = new Edge(portsForNewEdge);
                        if (!(edge.getPaths().isEmpty() && edge2.getPaths().isEmpty())) {
                            LinkedList<Point2D.Double> pathPointsForNewEdge = new LinkedList<>();
                            if (!edge.getPaths().isEmpty()) {
                                LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath)edge.getPaths().get(0)).getTerminalAndBendPoints());
                                if (pathPointsEdge.getFirst().getX() == endPort.getShape().getXPosition()) Collections.reverse(pathPointsEdge);
                            }
                            if (!edge2.getPaths().isEmpty()) {
                                LinkedList<Point2D.Double> pathPointsEdge2 = new LinkedList<>(((PolygonalPath)edge2.getPaths().get(0)).getTerminalAndBendPoints());
                                if (pathPointsEdge2.getFirst().getX() == startPort.getShape().getXPosition()) Collections.reverse(pathPointsEdge2);
                            }
                            newEdge.addPath(new PolygonalPath(pathPointsForNewEdge.removeFirst(), pathPointsForNewEdge.removeLast(), pathPointsForNewEdge));
                        }
                        sugy.getGraph().addEdge(newEdge);
                        sugy.getGraph().removeEdge(edge);
                        sugy.getGraph().removeEdge(edge2);
                        sugy.getGraph().removeVertex(node);
                        deletedDummys.put(node, newEdge);
                    }
                    shiftValue -= ((Rectangle)cmResult.getNodeOrder().get(rank).get(0).getShape()).getHeight();
                    shiftValue -= drawInfo.getDistanceBetweenLayers();
                    shiftValue -= (2 * drawInfo.getPortHeight());
                    shiftValue += drawInfo.getEdgeDistanceVertical();
                }
            }
        }*/
        // do path for edges
        for (Edge edge : sugy.getGraph().getEdges()) {
            if (edge.getPaths().isEmpty()) {
                Port p1 = edge.getPorts().get(0);
                Port p2 = edge.getPorts().get(1);
                if (p1.getLabelManager().getMainLabel().toString().endsWith("t")) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                Point2D.Double start = new Point2D.Double(p1.getShape().getXPosition(), (p1.getShape().getYPosition() - drawInfo.getPortHeight()));
                Point2D.Double end = new Point2D.Double(p2.getShape().getXPosition(), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                edge.addPath(new PolygonalPath(start, end, new LinkedList<>()));
            } else {
                Port p1 = edge.getPorts().get(0);
                Port p2 = edge.getPorts().get(1);
                PolygonalPath path = (PolygonalPath) edge.getPaths().get(0);
                Point2D.Double start = path.getStartPoint();
                Point2D.Double end = path.getEndPoint();
                if (end.getX() == p1.getShape().getXPosition()) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                if (p1.getLabelManager().getMainLabel().toString().endsWith("t")) {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() - drawInfo.getPortHeight())));
                }
                if (p2.getLabelManager().getMainLabel().toString().endsWith("t")) {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() - drawInfo.getPortHeight())));
                }
            }
            // do port shapes
            for (Port port : edge.getPorts()) {
                Rectangle portShape = (Rectangle) port.getShape();
                if (port.getLabelManager().getMainLabel().toString().endsWith("t")) {
                    port.setShape(new Rectangle((portShape.getXPosition() - (drawInfo.getPortWidth() / 2)), portShape.getYPosition(), portShape.getWidth(), portShape.getHeight(), null));
                } else {
                    port.setShape(new Rectangle((portShape.getXPosition() - (drawInfo.getPortWidth() / 2)), (portShape.getYPosition() - drawInfo.getPortHeight()), portShape.getWidth(), portShape.getHeight(), null));
                }
            }
        }
        // add Edges with Paths for remaining dummyNodes
        vertices = new LinkedHashSet<>(sugy.getGraph().getVertices());
        for (Vertex node : vertices) {
            if (sugy.isDummy(node)) {
                Port p1 = (Port) node.getPortCompositions().get(0);
                Port p2 = (Port) node.getPortCompositions().get(1);
                Edge originalEdge = sugy.getDummyEdge2RealEdge().get(p1.getEdges().get(0));
                List<Port> portsForNewEdge = new LinkedList<>();
                portsForNewEdge.add(p1);
                portsForNewEdge.add(p2);
                Edge newEdge = new Edge(portsForNewEdge);
                Point2D.Double start;
                Point2D.Double end;
                if (p1.getLabelManager().getMainLabel().toString().endsWith("t")) {
                    if (p2.getLabelManager().getMainLabel().toString().endsWith("t")){
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                    } else {
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition()));
                    }
                } else {
                    if (p2.getLabelManager().getMainLabel().toString().endsWith("t")){
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                    } else {
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition()));
                    }
                }
                newEdge.addPath(new PolygonalPath(start, end, new LinkedList<>()));
                sugy.getGraph().addEdge(newEdge);
                sugy.getDummyEdge2RealEdge().put(newEdge, originalEdge);
                Vertex endNode0 = portsForNewEdge.get(0).getVertex();
                Vertex endNode1 = portsForNewEdge.get(1).getVertex();
                sugy.assignDirection(newEdge,
                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode0 : endNode1,
                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode1 : endNode0);
            }
        }
    }

    // shift all nodes of rank rank with their ports and all edgePaths to nodes below
    private void shift (double shiftValue, int rank, boolean shiftEdges) {
        for (Vertex node : cmResult.getNodeOrder().get(rank)) {
            Rectangle currentShape = (Rectangle) node.getShape();
            currentShape.y = currentShape.getY() + shiftValue;
            for (PortComposition portComposition : node.getPortCompositions()) {
                shift(shiftValue, portComposition);
            }
            if (shiftEdges) {
                // shift edgePaths
                for (Port bottomPort : cmResult.getBottomPortOrder().get(node)) {
                    for (Edge edge : bottomPort.getEdges()) {
                        if (!edge.getPaths().isEmpty()) {
                            for (Point2D.Double pathPoint : ((PolygonalPath) edge.getPaths().get(0)).getTerminalAndBendPoints()) {
                                pathPoint.setLocation(pathPoint.getX(), (pathPoint.getY() + shiftValue));
                            }
                        }
                    }
                }
            }
        }
    }

    private void shift (double shiftValue, PortComposition portComposition) {
        if (portComposition instanceof Port) {
            Rectangle currentShape = (Rectangle) ((Port)portComposition).getShape();
            currentShape.y = currentShape.getY() + shiftValue;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                shift(shiftValue, member);
            }
        }
    }
}
