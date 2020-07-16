package de.uniwue.informatik.praline.layouting.layered.algorithm.drawing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.layouting.layered.algorithm.Sugiyama;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

import java.awt.geom.Point2D;
import java.util.*;

public class DrawingPreparation {

    private Sugiyama sugy;
    private DrawingInformation drawInfo;
    private CMResult cmResult;
    private double delta;

    public DrawingPreparation (Sugiyama sugy) {
        this.sugy = sugy;
    }

    private void initialise (DrawingInformation drawInfo, CMResult cmResult) {
        this.drawInfo = drawInfo;
        this.delta = Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing());
        this.cmResult = cmResult;
    }

    public void prepareDrawing(DrawingInformation drawInfo, CMResult cmResult) {
        initialise(drawInfo, cmResult);
        Collection<Vertex> vertices = new LinkedHashSet<>(sugy.getGraph().getVertices());
        for (Vertex node : vertices) {
            if (sugy.isTurningPointDummy(node)) {
                drawEdgesForTurningDummy(node);
            } else {
                tightenNode(node);
            }
        }
        // do path for edges
        doPathForEdges();
        // add Edges with Paths for remaining dummyNodes
        drawEdgesForDummys();
    }

    private void drawEdgesForTurningDummy(Vertex node) {
        // create two lists of ports; those whose corresponding edge is routed to the left of v
        List<Port> portsL = new LinkedList<>();
        // and those whose edge is routed to the right
        List<Port> portsR = new LinkedList<>();
        boolean top;
        // sort ports into these lists
        if (cmResult.getTopPortOrder().get(node).isEmpty()) {
            sortPortsLeftRight(node, portsL, portsR, cmResult.getBottomPortOrder());
            top = false;
        } else {
            sortPortsLeftRight(node, portsL, portsR, cmResult.getTopPortOrder());
            top = true;
        }
        // route edges
        Collections.reverse(portsL);
        handlePorts(top, portsL);
        handlePorts(top, portsR);
    }

    private void handlePorts(boolean top, List<Port> ports) {
        int level = 0;
        for (Port p2 : ports) {
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
                LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath) edge1.getPaths().get(0)).getTerminalAndBendPoints());
                if (pathPointsEdge.getLast().getX() == pathPoints.getLast().getX()) Collections.reverse(pathPointsEdge);
                pathPointsEdge.removeFirst();
                pathPointsEdge.removeLast();
                pathPoints.addAll(pathPointsEdge);
            }
            if (top) pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
            else pathPoints.add(new Point2D.Double(p2.getShape().getXPosition(), p2.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
            if (top) pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() - (level * drawInfo.getEdgeDistanceVertical())));
            else pathPoints.add(new Point2D.Double(p3.getShape().getXPosition(), p3.getShape().getYPosition() + (level * drawInfo.getEdgeDistanceVertical())));
            if (!edge2.getPaths().isEmpty()) {
                LinkedList<Point2D.Double> pathPointsEdge = new LinkedList<>(((PolygonalPath) edge2.getPaths().get(0)).getTerminalAndBendPoints());
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

    private void sortPortsLeftRight(Vertex node, List<Port> portsL, List<Port> portsR, Map<Vertex, List<Port>> portOrder) {
        Vertex v = sugy.getVertexOfTurningDummy(node);
        for (Port port : portOrder.get(node)) {
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
    }

    private void tightenNode(Vertex node) {
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

    private void doPathForEdges() {
        for (Edge edge : sugy.getGraph().getEdges()) {
            Port p1 = edge.getPorts().get(0);
            Port p2 = edge.getPorts().get(1);
            // create path; else update end-point-positions
            if (edge.getPaths().isEmpty()) {
                if (sugy.isTopPort(p1)) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                Point2D.Double start = new Point2D.Double(p1.getShape().getXPosition(), (p1.getShape().getYPosition() - drawInfo.getPortHeight()));
                Point2D.Double end = new Point2D.Double(p2.getShape().getXPosition(), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                edge.addPath(new PolygonalPath(start, end, new LinkedList<>()));
            } else {
                PolygonalPath path = (PolygonalPath) edge.getPaths().get(0);
                Point2D.Double start = path.getStartPoint();
                Point2D.Double end = path.getEndPoint();
                if (end.getX() == p1.getShape().getXPosition()) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                if (sugy.isTopPort(p1)) {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() - drawInfo.getPortHeight())));
                }
                if (sugy.isTopPort(p2)) {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() - drawInfo.getPortHeight())));
                }
            }
            // after the path is set
            createPortShapes(edge);
        }
    }

    private void createPortShapes(Edge edge) {
        for (Port port : edge.getPorts()) {
            Rectangle portShape = (Rectangle) port.getShape();
            if (sugy.isTopPort(port)) {
                port.setShape(new Rectangle((portShape.getXPosition() - (drawInfo.getPortWidth() / 2)), portShape.getYPosition(), portShape.getWidth(), portShape.getHeight(), null));
            } else {
                port.setShape(new Rectangle((portShape.getXPosition() - (drawInfo.getPortWidth() / 2)), (portShape.getYPosition() - drawInfo.getPortHeight()), portShape.getWidth(), portShape.getHeight(), null));
            }
        }
    }

    private void drawEdgesForDummys() {
        Collection<Vertex> vertices;
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
                if (sugy.isTopPort(p1)) {
                    if (sugy.isTopPort(p2)){
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                    } else {
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition()));
                    }
                } else {
                    if (sugy.isTopPort(p2)){
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