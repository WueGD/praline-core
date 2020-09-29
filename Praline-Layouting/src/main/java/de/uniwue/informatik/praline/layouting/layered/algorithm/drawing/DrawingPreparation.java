package de.uniwue.informatik.praline.layouting.layered.algorithm.drawing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
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

    public void prepareDrawing(DrawingInformation drawInfo, CMResult cmResult) {
        initialise(drawInfo, cmResult);
        for (Vertex node : sugy.getGraph().getVertices()) {
            tightenNode(node);
        }
        // do path for edges
        doPathForEdges();
        // adjust port shapes
        adjustPortShapes();
        // add Edges with Paths for remaining dummyNodes
        drawEdgesForDummys();
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
        maxL -= ((drawInfo.getPortWidth() + delta) / 2);
        minR += ((drawInfo.getPortWidth() + delta) / 2);
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
        }
    }

    private void adjustPortShapes() {
        for (Vertex vertex : sugy.getGraph().getVertices()) {
            for (Port port : vertex.getPorts()) {
                Rectangle portShape = (Rectangle) port.getShape();
                portShape.x = portShape.getXPosition() - (drawInfo.getPortWidth() / 2);
                if (!sugy.isTopPort(port)) {
                    portShape.y = portShape.getYPosition() - drawInfo.getPortHeight();
                }
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