package de.uniwue.informatik.praline.io.output.svg;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.io.output.util.DrawingUtils;
import de.uniwue.informatik.praline.io.output.util.FontManager;
import de.uniwue.informatik.praline.io.output.util.SVGLineShape;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.List;

public class SVGDrawer {

    //TODO was created for the specific use for the layered drawing algo --> change to make it general usable

    public static final double EMPTY_MARGIN_WIDTH = 20.0;
    private static final boolean USE_CSS = true; // we want to use CSS style attributes


    private final Graph graph;

    private DrawingInformation drawInfo;

    public SVGDrawer(Graph graph) {
        this.graph = graph;
    }

    public void draw(String savePath, DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
        SVGGraphics2D svgGenerator = this.getSvgGenerator();

        // Finally, stream out SVG to a file using UTF-8 encoding.
        try {
            svgGenerator.stream(savePath, USE_CSS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Writer writer, DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
        SVGGraphics2D svgGenerator = this.getSvgGenerator();

        // Finally, stream out SVG to a writer using UTF-8 encoding.
        try {
            svgGenerator.stream(writer, USE_CSS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SVGGraphics2D getSvgGenerator() {
        // Get a DOMImplementation.
        DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // Ask the test to render into the SVG Graphics2D implementation.
        paint(svgGenerator);

        return svgGenerator;
    }

    public void paint(SVGGraphics2D g2d) {
        //set canvas
        Rectangle2D bounds = DrawingUtils.determineDrawingBounds(graph, drawInfo, EMPTY_MARGIN_WIDTH);
        g2d.translate(-bounds.getX(), -bounds.getY());
        int canvasWidth = (int) (bounds.getWidth());
        int canvasHeight = (int) (bounds.getHeight());
        g2d.setSVGCanvasSize(new Dimension(canvasWidth, canvasHeight));

        LinkedHashSet<Port> portPairingsAlreadyDrawn = new LinkedHashSet<>();

        for (Vertex node : graph.getVertices()) {
            //determine node rectangle
            if (node.getShape() == null) {
                node.setShape(new Rectangle(drawInfo.getVertexMinimumWidth(), drawInfo.getVertexHeight()));
            }
            //draw node rectangle (possibly filled)
            Rectangle2D nodeRectangle = (Rectangle2D) node.getShape();
            if (drawInfo.getVertexColor() != null) {
                g2d.setColor(drawInfo.getVertexColor());
                g2d.fill(nodeRectangle);
                g2d.setColor(Color.BLACK);
            }
            g2d.draw(nodeRectangle);
        }
        for (Vertex node : graph.getVertices()) {
            //draw port pairings
            if (drawInfo.isShowPortPairings()) {
                for (Port port : node.getPorts()) {
                    if (PortUtils.isPaired(port) && !portPairingsAlreadyDrawn.contains(port)) {
                        Port otherPort = PortUtils.getPairedPort(port);
                        drawPortPairing(port, otherPort, g2d);
                        portPairingsAlreadyDrawn.add(port);
                        portPairingsAlreadyDrawn.add(otherPort);
                    }
                }
            }
            //draw ports and port groups
            for (PortComposition pc : node.getPortCompositions()) {
                drawPortComposition(pc, g2d, (Rectangle) node.getShape());
            }
            //draw node label or label frame
            if (drawInfo.isShowVertexLabels() || drawInfo.isShowVertexLabelFrames()) {
                drawNodeLabel(g2d, node);
            }
        }
        //draw edges
        if (drawInfo.getLineShape().equals(SVGLineShape.STRAIGHT)) {
            for (Edge edge : graph.getEdges()) {
                if (edge.getPaths().isEmpty()) {
                    if (edge.getPorts().size() == 2) {
                        Point2D.Double start = new Point2D.Double(edge.getPorts().get(0).getShape().getXPosition(),
                                edge.getPorts().get(0).getShape().getYPosition());
                        Point2D.Double end = new Point2D.Double(edge.getPorts().get(1).getShape().getXPosition(),
                                edge.getPorts().get(1).getShape().getYPosition());
                        g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
                        if (drawInfo.isShowEdgeLabels()) {
                            float startX = (float) start.getX(), startY = (float) start.getY(),
                                    endX = (float) end.getX(), endY = (float) end.getY();
                            drawEdgeLabel(g2d, edge,  (startX + endX) / 2, (startY + endY) / 2);
                        }
                    }
                    else {
                        String moreOrLess = edge.getPorts().size() > 2 ? "more" : "less";
                        System.out.println("Edge " + edge + " with " + moreOrLess + " than 2 ports and without paths found."
                                + " This edge was ignored for the svg.");
                    }
                } else {
                    for (Path path : edge.getPaths()) {
                        List<Point2D.Double> edgePoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                        Point2D.Double start = null;
                        for (Point2D.Double end : edgePoints) {
                            if (start != null) {
                                g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
                            }
                            start = end;
                        }
                        if (drawInfo.isShowEdgeLabels()) {
                            Point2D.Double startL = edgePoints.get(edgePoints.size() / 2 - 1);
                            Point2D.Double endL = edgePoints.get(edgePoints.size() / 2);
                            drawEdgeLabel(g2d, edge,  ((float) startL.getX() + (float) endL.getX()) / 2,
                                    ((float) startL.getY() + (float) endL.getY()) / 2);
                        }
                    }
                }
                if (drawInfo.isShowEdgeDirection())
                    drawEdgeDirection(g2d, edge);
            }
        } else {
            for (Edge edge : graph.getEdges()) {
                Map<Vertex, List<Point2D>> banned = calcNodeBannedArea();
                if (drawInfo.getLineShape().equals(SVGLineShape.BEZIER2D))
                    drawBezier2dEdge(g2d, edge, banned);
                else if (drawInfo.getLineShape().equals(SVGLineShape.BEZIER3D))
                    drawBezier3dEdge(g2d, edge, banned);
                if (drawInfo.isShowEdgeDirection())
                    drawEdgeDirection(g2d, edge);
            }
        }
    }

    private void drawNodeLabel(SVGGraphics2D g2d, Vertex node) {
        Rectangle2D nodeRectangle = (Rectangle2D) node.getShape();
        Label<? extends LabelStyle> mainLabel = node.getLabelManager().getMainLabel(); //TODO: draw all labels, not only main label
        if (mainLabel instanceof TextLabel) {
            g2d.setFont(FontManager.fontOf((TextLabel) mainLabel));
            String text = ((TextLabel) mainLabel).getLayoutText();
            if (text == null) {
                System.out.println("Warning! No layout text found for label " + mainLabel + " of " + node);
                return;
            }
            double xCoordinate = nodeRectangle.getX() + drawInfo.getHorizontalVertexLabelOffset();
            double yCoordinate = nodeRectangle.getY() + 0.5 * nodeRectangle.getHeight()
                    - 0.5 * g2d.getFontMetrics().getStringBounds(text, g2d).getHeight()
                    - g2d.getFontMetrics().getStringBounds(text, g2d).getY()
                    + drawInfo.getVerticalVertexLabelOffset();
            if (drawInfo.isShowVertexLabels()) {
                g2d.drawString(text, (float) xCoordinate, (float) yCoordinate);
            }

            if (drawInfo.isShowVertexLabelFrames()) {
                g2d.draw(new Rectangle(xCoordinate, yCoordinate + g2d.getFontMetrics().getStringBounds(text, g2d).getY(),
                        g2d.getFontMetrics().getStringBounds(text, g2d).getWidth(),
                        g2d.getFontMetrics().getStringBounds(text, g2d).getHeight()));
            }
        }
    }

    private void drawPortPairing(Port port0, Port port1, Graphics2D g2d) {
        Port lowerPort = port0.getShape().getYPosition() < port1.getShape().getYPosition() ? port0 : port1;
        Port upperPort = port0 == lowerPort ? port1 : port0;

        if (lowerPort.getShape().equals(Port.DEFAULT_SHAPE_TO_BE_CLONED)
                || upperPort.getShape().equals(Port.DEFAULT_SHAPE_TO_BE_CLONED)) {
            return;
        }

        Color saveColor = g2d.getColor();
        g2d.setColor(drawInfo.getPortPairingColor());

        Point2D.Double start = new Point2D.Double(lowerPort.getShape().getXPosition() + drawInfo.getPortWidth() / 2.0,
                lowerPort.getShape().getYPosition());
        Point2D.Double end = new Point2D.Double(upperPort.getShape().getXPosition() + drawInfo.getPortWidth() / 2.0,
                upperPort.getShape().getYPosition() + drawInfo.getPortHeight());
        g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));

        g2d.setColor(saveColor);

    }

    private Rectangle2D drawPortComposition(PortComposition pc, Graphics2D g2d, Rectangle nodeRectangle) {
        if (pc instanceof PortGroup) {
            double maxX = Double.MIN_VALUE;
            double minX = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            for (PortComposition pcspc : ((PortGroup)pc).getPortCompositions()) {
                Rectangle2D rect = drawPortComposition(pcspc, g2d, nodeRectangle);
                maxX = Math.max(maxX, rect.getMaxX());
                minX = Math.min(minX, rect.getMinX());
                maxY = Math.max(maxY, rect.getMaxY());
                minY = Math.min(minY, rect.getMinY());
            }
            double gb = drawInfo.getPortGroupBorder();
            Rectangle2D groupRect = new Rectangle(minX - gb, minY - gb, (maxX - minX) + (2 * gb),
                    (maxY - minY) + (2 * gb), null); //Color.LIGHT_GRAY); TOOD: setting colors for rectangles or ...
            // is not yet used for svgs here
            if (drawInfo.isShowPortGroups()) {
                Color saveColor = g2d.getColor();
                g2d.setColor(drawInfo.getPortGroupColor());
                g2d.draw(groupRect);
                g2d.setColor(saveColor);
            }
            return groupRect;
        }
        if (pc instanceof Port) {
            Rectangle2D portRectangle = (Rectangle2D) ((Port) pc).getShape();
            if (drawInfo.getPortColor() != null) {
                g2d.setColor(drawInfo.getPortColor());
                g2d.fill(portRectangle);
                g2d.setColor(Color.BLACK);
            }
            g2d.draw(portRectangle);

            //draw port label or label frame
            if (drawInfo.isShowPortLabels() || drawInfo.isShowPortLabelFrames()) {
                drawPortLabel((Port) pc, g2d, nodeRectangle, portRectangle);
            }
            return portRectangle;
        }
        return null;
    }

    private void drawPortLabel(Port port, Graphics2D g2d, Rectangle nodeRectangle, Rectangle2D portRectangle) {
        Label<? extends LabelStyle> mainLabel = port.getLabelManager().getMainLabel(); //TODO: draw all labels, not only main label
        if (mainLabel instanceof TextLabel) {
            g2d.setFont(FontManager.fontOf((TextLabel) mainLabel));
            String text = ((TextLabel) mainLabel).getLayoutText();
            if (text == null) {
                System.out.println("Warning! No layout text found for label " + mainLabel + " of " + port);
                return;
            }

            double xCoordinate = portRectangle.getX() + drawInfo.getHorizontalPortLabelOffset();
            double yCoordinate = portRectangle.getY() - g2d.getFontMetrics().getStringBounds(text, g2d).getY() +
                    (portRectangle.getY() < nodeRectangle.getY() ?
                            portRectangle.getHeight() + drawInfo.getVerticalPortLabelOffset() :
                            - g2d.getFontMetrics().getStringBounds(text, g2d).getHeight()
                                    - drawInfo.getVerticalPortLabelOffset());
            if (drawInfo.isShowPortLabels()) {
                g2d.drawString(text, (float) xCoordinate, (float) yCoordinate);
            }

            if (drawInfo.isShowPortLabelFrames()) {
                g2d.draw(new Rectangle(xCoordinate, yCoordinate + g2d.getFontMetrics().getStringBounds(text, g2d).getY(),
                        g2d.getFontMetrics().getStringBounds(text, g2d).getWidth(),
                        g2d.getFontMetrics().getStringBounds(text, g2d).getHeight()));
            }
        }
    }

    Map<Vertex, List<Point2D>> calcNodeBannedArea() {

        Map<Vertex, List<Point2D>> nodeBannedArea = new HashMap<>();
        for (Vertex node : graph.getVertices()) {

            List<Point2D> banned = new ArrayList<>();
            nodeBannedArea.put(node, banned);
            Rectangle2D box = node.getShape().getBoundingBox();

            banned.add(new Point2D.Double(box.getX(), box.getY()));
            banned.add(new Point2D.Double(box.getX() + box.getWidth(), box.getY()));
            banned.add(new Point2D.Double(box.getX(), box.getY() + box.getHeight()));
            banned.add(new Point2D.Double(box.getX() + box.getWidth(), box.getY() + box.getHeight()));
        }
        return nodeBannedArea;
    }

    public void drawBezier2dEdge(SVGGraphics2D g2d, Edge edge, Map<Vertex, List<Point2D>> nodeBannedArea) {

        List<List<Point2D>> edgeSegments = new ArrayList<>();
        Path2D bezierPath = new Path2D.Double();
        boolean bezierPathInit = false;

        if (edge.getPaths().isEmpty()) {
            if (edge.getPorts().size() == 2) {
                drawStraightEdge(g2d, edge.getPorts().get(0).getShape().getXPosition(),
                        edge.getPorts().get(0).getShape().getYPosition(),
                        edge.getPorts().get(1).getShape().getXPosition(),
                        edge.getPorts().get(1).getShape().getYPosition()
                );
                List<Point2D> seg = new ArrayList<>();
                seg.add(new Point2D.Double(edge.getPorts().get(0).getShape().getXPosition(),
                        edge.getPorts().get(0).getShape().getYPosition()));
                seg.add(new Point2D.Double(edge.getPorts().get(1).getShape().getXPosition(),
                        edge.getPorts().get(1).getShape().getYPosition()));
                edgeSegments.add(seg);
            }
            else {
                String moreOrLess = edge.getPorts().size() > 2 ? "more" : "less";
                System.out.println("Edge " + edge + " with " + moreOrLess + " than 2 ports and without paths found."
                        + " This edge was ignored for the svg.");
            }
        } else {
            for (Path path : edge.getPaths()) {

                List<Point2D.Double> edgePoints = ((PolygonalPath) path).getTerminalAndBendPoints();

                // edge with only two bend points -> straight-line segment
                if (edgePoints.size() == 2) {
                    drawStraightEdge(g2d, edgePoints.get(0).getX(), edgePoints.get(0).getY(),
                            edgePoints.get(1).getX(), edgePoints.get(1).getY());

                    List<Point2D> seg = new ArrayList<>();
                    seg.add(new Point2D.Double(edgePoints.get(0).getX(), edgePoints.get(0).getY()));
                    seg.add(new Point2D.Double(edgePoints.get(1).getX(), edgePoints.get(1).getY()));
                    edgeSegments.add(seg);

                } else if (edgePoints.size() > 2 && edgePoints.size() % 2 == 0) {
                    // edge with more than 2 bend points -> "real" Bezier curve
                    List<List<Point2D>> pointList = new ArrayList<>();

                    // 1. part: start point, 1. bend point as control point
                    // middle point btw. 1. and 2. bend point as end points
                    List<Point2D> firstPoints = new ArrayList<>();
                    pointList.add(firstPoints);
                    firstPoints.add(edgePoints.get(0));
                    firstPoints.add(edgePoints.get(1));
                    firstPoints.add(new Point2D.Double(edgePoints.get(1).getX()
                            + (edgePoints.get(2).getX() - edgePoints.get(1).getX()) / 2,
                            edgePoints.get(1).getY()));

                    // middle part: middle points as start and end point, bend points as control points
                    for (int i = 2; i < edgePoints.size() - 2; i ++) {

                        List<Point2D> points = new ArrayList<>();
                        pointList.add(points);
                        Point2D st, en;
                        Point2D bf = edgePoints.get(i - 1);
                        Point2D cur = edgePoints.get(i);
                        Point2D af = edgePoints.get(i + 1);

                        // bend from vertical to horizontal (change y at st, s at en)
                        if (i % 2 == 1) {
                            st = new Point2D.Double(cur.getX(), bf.getY() + (cur.getY() - bf.getY()) / 2);
                            en = new Point2D.Double(cur.getX() + (af.getX() - cur.getX()) / 2, cur.getY());
                            // bend from horizontal to vertical (change x at st, y at en)
                        } else {
                            st = new Point2D.Double(bf.getX() + (cur.getX() - bf.getX()) / 2, cur.getY());
                            en = new Point2D.Double(cur.getX(), cur.getY() + (af.getY() - cur.getY()) / 2);
                        }
                        points.add(st);
                        points.add(edgePoints.get(i));
                        points.add(en);
                    }

                    // last part: middle point btw. second to last and last corner point as start,
                    // last bend point as control point, end point as terminal
                    List<Point2D> lastPoints = new ArrayList<>();
                    pointList.add(lastPoints);
                    lastPoints.add(new Point2D.Double(edgePoints.get(edgePoints.size() - 3).getX()
                            + (edgePoints.get(edgePoints.size() - 2).getX()
                            - edgePoints.get(edgePoints.size() - 3).getX()) / 2,
                            edgePoints.get(edgePoints.size() - 2).getY()));
                    lastPoints.add(edgePoints.get(edgePoints.size() - 2));
                    lastPoints.add(edgePoints.get(edgePoints.size() - 1));

                    // draw Bezier curves part-wise
                    for (List<Point2D> points : pointList) {

                        // p0: Start, p1: Kontrollpunkt, p2: Ende
                        Point2D p0 = points.get(0);
                        Point2D p1 = points.get(1);
                        Point2D p2 = points.get(2);

                        // find all points of the triangle from P0, P1, P2
                        boolean controlPointsNeedAdjustments = true;
                        int maxAdjustmentIterations = 5, iteration = 0;
                        while (controlPointsNeedAdjustments && iteration < maxAdjustmentIterations) {
                            iteration += 1;

                            List<Vertex> verticesInTriangle = new ArrayList<>();
                            for (Vertex node : nodeBannedArea.keySet()) {
                                if (!node.equals(edge.getPorts().get(0).getVertex())
                                        && !node.equals(edge.getPorts().get(1).getVertex())) {
                                    List<Point2D> corners = nodeBannedArea.get(node);
                                    for (Point2D corner : corners) {
                                        if (!verticesInTriangle.contains(node)
                                                && triangleInnerArea(p0, p1, corner) + triangleInnerArea(p0, p2, corner)
                                                + triangleInnerArea(p1, p2, corner) <= triangleInnerArea(p0, p1, p2)) {
                                            verticesInTriangle.add(node);
                                        }
                                    }
                                }
                            }

                            // if vertices lie inside the triangle
                            if (!verticesInTriangle.isEmpty()) {
                                // find the next vertex that lies at P1 and determine the next bend point
                                Point2D nearest = nodeBannedArea.get(verticesInTriangle.get(0)).get(0);
                                double nearestDistance = distance(p1, nearest);
                                for (Vertex vertex : verticesInTriangle) {
                                    for (Point2D point : nodeBannedArea.get(vertex)) {
                                        if (distance(p1, point) < nearestDistance) {
                                            nearest = point;
                                            nearestDistance = distance(p1, point);
                                        }
                                    }
                                }
                                // Compute the slope of the line through P0 and P2
                                double slope = (p2.getY() - p0.getY()) / (p2.getX() - p0.getX());
                                Point2D newP0 = adjustPoint2D(p0, nearest, p1, slope);
                                Point2D newP2 = adjustPoint2D(p2, nearest, p1, slope);
                                drawStraightEdge(g2d, newP0.getX(), newP0.getY(), p0.getX(), p0.getY());
                                drawStraightEdge(g2d, newP2.getX(), newP2.getY(), p2.getX(), p2.getY());

                                List<Point2D> seg0 = new ArrayList<>();
                                seg0.add(newP0);
                                seg0.add(p0);
                                edgeSegments.add(seg0);

                                List<Point2D> seg2 = new ArrayList<>();
                                seg2.add(newP2);
                                seg2.add(p2);
                                edgeSegments.add(seg2);

                                p0 = newP0;
                                p2 = newP2;
                            } else {
                                controlPointsNeedAdjustments = false;
                            }
                        }

                        if (!bezierPathInit || !p0.equals(bezierPath.getCurrentPoint())) {
                            bezierPath.moveTo(p0.getX(), p0.getY());
                            bezierPathInit = true;
                        }
                        bezierPath.quadTo(p1.getX(), p1.getY(), p2.getX(), p2.getY());

                        List<Point2D> seg2d = new ArrayList<>();
                        seg2d.add(p0);
                        seg2d.add(p1);
                        seg2d.add(p2);
                        edgeSegments.add(seg2d);
                    }
                }
            }
        }
        g2d.draw(bezierPath);

        // draw the edge label at the middle of the drawn curve
        if (drawInfo.isShowEdgeLabels()) {
            Point2D midpoint = null;
            if (edgeSegments.size() == 1) {
                if (edgeSegments.get(0).size() == 2) {
                    midpoint = new Point2D.Double(
                            (edgeSegments.get(0).get(0).getX() + edgeSegments.get(0).get(1).getX()) / 2,
                            (edgeSegments.get(0).get(0).getY() + edgeSegments.get(0).get(1).getY()) / 2);
                } else {
                    midpoint = new Point2D.Double(calcBezier2D(edgeSegments.get(0).get(0).getX(),
                            edgeSegments.get(0).get(1).getX(), edgeSegments.get(0).get(2).getX(), 0.5),
                            calcBezier2D(edgeSegments.get(0).get(0).getY(),
                                    edgeSegments.get(0).get(1).getY(), edgeSegments.get(0).get(2).getY(), 0.5));
                }
            } else if (edgeSegments.size() == 2) {
                List<Point2D> midSeg1 = edgeSegments.get(0);
                List<Point2D> midSeg2 = edgeSegments.get(1);
                List<Point2D> largerSeg;
                if (distance(midSeg1.get(0), midSeg1.get(midSeg1.size() - 1)) >
                        distance(midSeg2.get(0), midSeg2.get(midSeg2.size() - 1))) {
                    largerSeg = midSeg1;
                } else {
                    largerSeg = midSeg2;
                }
                if (largerSeg.size() == 2) {
                    midpoint = new Point2D.Double(
                            (largerSeg.get(0).getX() + largerSeg.get(1).getX()) / 2,
                            (largerSeg.get(0).getY() + largerSeg.get(1).getY()) / 2);
                } else {
                    midpoint = new Point2D.Double(calcBezier2D(largerSeg.get(0).getX(),
                            largerSeg.get(1).getX(), largerSeg.get(2).getX(), 0.5),
                            calcBezier2D(largerSeg.get(0).getY(),
                                    largerSeg.get(1).getY(), largerSeg.get(2).getY(), 0.5));
                }
            } else if (edgeSegments.size() > 2) {
                List<Point2D> midSeg1 = edgeSegments.get(Math.max((int) ((double) edgeSegments.size() / 2) - 1, 0));
                List<Point2D> midSeg2 = edgeSegments.get(Math.max((int) ((double) edgeSegments.size() / 2), 0));
                if (distance(midSeg1.get(0), midSeg1.get(midSeg1.size() - 1)) >
                        distance(midSeg2.get(0), midSeg2.get(midSeg2.size() - 1))) {
                    midpoint = midSeg1.get(midSeg1.size() - 1);
                } else {
                    midpoint = midSeg2.get(midSeg2.size() - 1);
                }
            }
            if (midpoint != null) {
                Double xEdge = midpoint.getX();
                Double yEdge = midpoint.getY();
                drawEdgeLabel(g2d, edge, xEdge.floatValue(), yEdge.floatValue());
            }
        }
    }

    public static double calcBezier2D(double start, double control, double end, double t) {
        return Math.pow((1 - t), 2) * start + 2 * t * (1 - t) * control + Math.pow(t, 2) * end;
    }

    public Point2D adjustPoint2D(Point2D point, Point2D nearest, Point2D controlPoint, double slope) {

        // adjust point such that overlaps are avoided
        Point2D newPoint;
        if (point.getX() != controlPoint.getX()) {
            newPoint = new Point2D.Double(
                    nearest.getX() - (nearest.getY() - point.getY()) / slope , point.getY());

        } else {
            newPoint = new Point2D.Double(
                    point.getX(), nearest.getY() - (nearest.getX() - point.getX()) * slope);
        }

        // check if the next point lies outside the Bezier area
        if ((controlPoint.getX() < point.getX() && point.getX() < newPoint.getX())
                ||(controlPoint.getX() > point.getX() && point.getX() > newPoint.getX())) {
            newPoint.setLocation(point.getX(), newPoint.getY());
        }
        if ((controlPoint.getY() < point.getY() && point.getY() < newPoint.getY())
                ||(controlPoint.getY() > point.getY() && point.getY() > newPoint.getY())) {
            newPoint.setLocation(newPoint.getX(), point.getY());
        }
        return newPoint;
    }

    public double distance(Point2D p1, Point2D p2) {
        return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
    }

    public double triangleInnerArea(Point2D p1, Point2D p2, Point2D p3) {
        return .5 * Math.abs(p1.getX() * (p2.getY() - p3.getY()) + p2.getX() * (p3.getY() - p1.getY())
                + p3.getX() * (p1.getY() - p2.getY()));
    }

    public void drawBezier3dEdge(SVGGraphics2D g2d, Edge edge, Map<Vertex, List<Point2D>> nodeBannedArea) {

        List<List<Point2D>> edgeSegments = new ArrayList<>();
        //graph.addSegment(edgeSegments);
        Path2D bezierPath = new Path2D.Double();
        boolean bezierPathInit = false;

        if (edge.getPaths().isEmpty()) {
            if (edge.getPorts().size() == 2) {
                drawStraightEdge(g2d, edge.getPorts().get(0).getShape().getXPosition(),
                        edge.getPorts().get(0).getShape().getYPosition(),
                        edge.getPorts().get(1).getShape().getXPosition(),
                        edge.getPorts().get(1).getShape().getYPosition()
                );
                List<Point2D> seg = new ArrayList<>();
                seg.add(new Point2D.Double(edge.getPorts().get(0).getShape().getXPosition(),
                        edge.getPorts().get(0).getShape().getYPosition()));
                seg.add(new Point2D.Double(edge.getPorts().get(1).getShape().getXPosition(),
                        edge.getPorts().get(1).getShape().getYPosition()));
                edgeSegments.add(seg);
            }
            else {
                String moreOrLess = edge.getPorts().size() > 2 ? "more" : "less";
                System.out.println("Edge " + edge + " with " + moreOrLess + " than 2 ports and without paths found."
                        + " This edge was ignored for the svg.");
            }
        } else {
            for (Path path : edge.getPaths()) {

                List<Point2D.Double> edgePoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                List<List<Point2D>> pointList = new ArrayList<>();

                // edge has only two corner points, i.e., it is a line segment
                if (edgePoints.size() == 2) {
                    drawStraightEdge(g2d, edgePoints.get(0).getX(), edgePoints.get(0).getY(),
                            edgePoints.get(1).getX(), edgePoints.get(1).getY());

                    List<Point2D> seg = new ArrayList<>();
                    seg.add(new Point2D.Double(edgePoints.get(0).getX(), edgePoints.get(0).getY()));
                    seg.add(new Point2D.Double(edgePoints.get(1).getX(), edgePoints.get(1).getY()));
                    edgeSegments.add(seg);

                } else if (edgePoints.size() == 4) {
                    // edge has 4 bend points and becomes a Bezier curve
                    List<Point2D> points = new ArrayList<>();
                    pointList.add(points);
                    points.add(edgePoints.get(0));
                    points.add(edgePoints.get(1));
                    points.add(edgePoints.get(2));
                    points.add(edgePoints.get(3));

                } else if (edgePoints.size() > 4 && edgePoints.size() % 2 == 0) {

                    // Erste Kante: Start als Startpunkt, erste 2 Eckpunkte als Kontrollpunkte,
                    // Mitte zwischen 3. und 4. Eckpunkt als Endpunkt
                    // first edge: start at start point, first two bend points as control points,
                    // middle between 3. and 4. bend point as end point
                    List<Point2D> firstPoints = new ArrayList<>();
                    pointList.add(firstPoints);
                    firstPoints.add(edgePoints.get(0));
                    firstPoints.add(edgePoints.get(1));
                    firstPoints.add(edgePoints.get(2));
                    firstPoints.add(new Point2D.Double(edgePoints.get(2).getX(),
                            edgePoints.get(2).getY() +
                                    (edgePoints.get(3).getY() - edgePoints.get(2).getY()) / 2));

                    for (int i = 3; i < edgePoints.size() - 4; i += 2) {

                        // middle edges: middle points as start and end points, each 2 bend points as control points
                        List<Point2D> points = new ArrayList<>();
                        pointList.add(points);
                        Point2D bf = edgePoints.get(i - 1);
                        Point2D cur1 = edgePoints.get(i);
                        Point2D cur2 = edgePoints.get(i + 1);
                        Point2D af = edgePoints.get(i + 2);

                        points.add(new Point2D.Double(cur1.getX(), bf.getY() + (cur1.getY() - bf.getY()) / 2));
                        points.add(cur1);
                        points.add(cur2);
                        points.add(new Point2D.Double(cur2.getX(), af.getY() + (cur2.getY() - af.getY()) / 2));
                    }

                    // last edge: middle point btw. 3.- and 2.-last bend point as start
                    // second to last and last bend point as control points, last as terminal point
                    List<Point2D> lastPoints = new ArrayList<>();
                    pointList.add(lastPoints);
                    lastPoints.add(new Point2D.Double(edgePoints.get(edgePoints.size() - 3).getX(),
                            edgePoints.get(edgePoints.size() - 4).getY()
                                    + (edgePoints.get(edgePoints.size() - 3).getY()
                                    - edgePoints.get(edgePoints.size() - 4).getY()) / 2));
                    lastPoints.add(edgePoints.get(edgePoints.size() - 3));
                    lastPoints.add(edgePoints.get(edgePoints.size() - 2));
                    lastPoints.add(edgePoints.get(edgePoints.size() - 1));
                }

                for (List<Point2D> points : pointList) {

                    Point2D p0 = points.get(0);
                    Point2D p1 = points.get(1);
                    Point2D p2 = points.get(2);
                    Point2D p3 = points.get(3);

                    boolean controlPointsNeedAdjustments = true;
                    int maxAdjustmentIterations = 5, iteration = 0;
                    while (controlPointsNeedAdjustments && iteration < maxAdjustmentIterations) {
                        iteration += 1;

                        // find all points of the triangle from P0, P1, P2 and P1, P2, P3
                        Map<Integer, List<Vertex>> verticesInTriangle = new HashMap<>();
                        verticesInTriangle.put(0, new ArrayList<>());
                        verticesInTriangle.put(1, new ArrayList<>());

                        for (Vertex node : nodeBannedArea.keySet()) {
                            List<Point2D> corners = nodeBannedArea.get(node);
                            for (Point2D corner : corners) {
                                if (!verticesInTriangle.get(0).contains(node)
                                        && triangleInnerArea(p0, p1, corner) + triangleInnerArea(p0, p2, corner)
                                        + triangleInnerArea(p1, p2, corner) <= triangleInnerArea(p0, p1, p2)) {
                                    verticesInTriangle.get(0).add(node);
                                }
                                if (!verticesInTriangle.get(1).contains(node)
                                        && triangleInnerArea(p1, p2, corner) + triangleInnerArea(p1, p3, corner)
                                        + triangleInnerArea(p2, p3, corner) <= triangleInnerArea(p1, p2, p3)) {
                                    verticesInTriangle.get(1).add(node);
                                }
                            }
                        }

                        boolean pointsInAnyTriangle = false;

                        // if vertices lie inside the triangle
                        for (Integer triangleIndex : verticesInTriangle.keySet()) {

                            Point2D control, control2;
                            if (triangleIndex == 0) { control = p1; control2 = p2; }
                            else { control = p2; control2 = p1; }

                            if (!verticesInTriangle.get(triangleIndex).isEmpty()) {
                                pointsInAnyTriangle = true;

                                // find the next vertex that lies at the control point and determine the next bend
                                Point2D nearest = nodeBannedArea.get(verticesInTriangle.get(triangleIndex).get(0)).get(0);
                                double nearestDistance = distance(control, nearest);
                                for (Vertex vertex : verticesInTriangle.get(triangleIndex)) {
                                    for (Point2D point : nodeBannedArea.get(vertex)) {
                                        if (distance(control, point) < nearestDistance) {
                                            nearest = point;
                                            nearestDistance = distance(control, point);
                                        }
                                    }
                                }

                                // compute the slope of the line through P0 and P2
                                double slope = (control2.getY() - nearest.getY()) / (control2.getX() - nearest.getX());

                                Point2D newPoint;
                                if (triangleIndex == 0) {
                                    newPoint = adjustPoint3D(p0, nearest, slope);
                                    drawStraightEdge(g2d, newPoint.getX(), newPoint.getY(), p0.getX(), p0.getY());
                                    List<Point2D> seg = new ArrayList<>();
                                    seg.add(newPoint);
                                    seg.add(p0);
                                    edgeSegments.add(seg);
                                    p0 = newPoint;

                                    // adjust control point to avoid bends
                                    double percNP = Math.max(60 - Math.abs(p1.getY() - newPoint.getY()), 0) / 100;
                                    p1.setLocation(p1.getX(), p1.getY() * (1 - percNP) + newPoint.getY() * percNP);

                                } else {
                                    newPoint = adjustPoint3D(p3, nearest, slope);
                                    drawStraightEdge(g2d, newPoint.getX(), newPoint.getY(), p3.getX(), p3.getY());
                                    List<Point2D> seg = new ArrayList<>();
                                    seg.add(newPoint);
                                    seg.add(p3);
                                    edgeSegments.add(seg);
                                    p3 = newPoint;

                                    // adjust control point to avoid bends
                                    double percNP = Math.max(60 - Math.abs(p2.getY() - newPoint.getY()), 0) / 100;
                                    p2.setLocation(p2.getX(), p2.getY() * (1 - percNP) + newPoint.getY() * percNP);
                                }
                            }
                        }
                        if (!pointsInAnyTriangle) {
                            controlPointsNeedAdjustments = false;
                        }
                    }

                    if (!bezierPathInit || !p0.equals(bezierPath.getCurrentPoint())) {
                        bezierPath.moveTo(p0.getX(), p0.getY());
                        bezierPathInit = true;
                    }
                    bezierPath.curveTo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());

                    List<Point2D> seg3d = new ArrayList<>();
                    seg3d.add(p0);
                    seg3d.add(p1);
                    seg3d.add(p2);
                    seg3d.add(p3);
                    edgeSegments.add(seg3d);
                }
            }
        }
        g2d.draw(bezierPath);

        // draw the edge label at the middle of the drawn curve
        if (drawInfo.isShowEdgeLabels()) {
            Point2D midpoint = null;
            if (edgeSegments.size() == 1) {
                if (edgeSegments.get(0).size() == 2) {
                    midpoint = new Point2D.Double(
                            (edgeSegments.get(0).get(0).getX() + edgeSegments.get(0).get(1).getX()) / 2,
                            (edgeSegments.get(0).get(0).getY() + edgeSegments.get(0).get(1).getY()) / 2);
                } else {
                    midpoint = new Point2D.Double(calcBezier3D(edgeSegments.get(0).get(0).getX(),
                            edgeSegments.get(0).get(1).getX(), edgeSegments.get(0).get(2).getX(),
                            edgeSegments.get(0).get(3).getX(), 0.5),
                            calcBezier3D(edgeSegments.get(0).get(0).getY(),
                                    edgeSegments.get(0).get(1).getY(), edgeSegments.get(0).get(2).getY(),
                                    edgeSegments.get(0).get(3).getY(), 0.5));
                }
            } else if (edgeSegments.size() == 2) {
                List<Point2D> midSeg1 = edgeSegments.get(0);
                List<Point2D> midSeg2 = edgeSegments.get(1);
                List<Point2D> largerSeg;
                if (distance(midSeg1.get(0), midSeg1.get(midSeg1.size() - 1)) >
                        distance(midSeg2.get(0), midSeg2.get(midSeg2.size() - 1))) {
                    largerSeg = midSeg1;
                } else {
                    largerSeg = midSeg2;
                }
                if (largerSeg.size() == 2) {
                    midpoint = new Point2D.Double(
                            (largerSeg.get(0).getX() + largerSeg.get(1).getX()) / 2,
                            (largerSeg.get(0).getY() + largerSeg.get(1).getY()) / 2);
                } else {
                    midpoint = new Point2D.Double(calcBezier3D(largerSeg.get(0).getX(),
                            largerSeg.get(1).getX(), largerSeg.get(2).getX(),
                            largerSeg.get(3).getX(), 0.5),
                            calcBezier3D(largerSeg.get(0).getY(),
                                    largerSeg.get(1).getY(), largerSeg.get(2).getY(),
                                    largerSeg.get(3).getY(), 0.5));
                }
            } else if (edgeSegments.size() > 2) {
                List<Point2D> midSeg1 = edgeSegments.get(Math.max((int) ((double) edgeSegments.size() / 2) - 1, 0));
                List<Point2D> midSeg2 = edgeSegments.get(Math.max((int) ((double) edgeSegments.size() / 2), 0));
                if (distance(midSeg1.get(0), midSeg1.get(midSeg1.size() - 1)) >
                        distance(midSeg2.get(0), midSeg2.get(midSeg2.size() - 1))) {
                    midpoint = midSeg1.get(midSeg1.size() - 1);
                } else {
                    midpoint = midSeg2.get(midSeg2.size() - 1);
                }
            }
            if (midpoint != null) {
                Double xEdge = midpoint.getX();
                Double yEdge = midpoint.getY();
                drawEdgeLabel(g2d, edge, xEdge.floatValue(), yEdge.floatValue());
            }
        }
    }

    public static double calcBezier3D(double start, double control1, double control2, double end, double t) {
        return Math.pow(1 - t, 3) * start + 3 * Math.pow(1 - t, 2) * t * control1
                + 3 * (1 - t) * Math.pow(t, 2) * control2 + Math.pow(t, 3) * end;
    }

    public Point2D adjustPoint3D(Point2D point, Point2D nearest, double slope) {

        // adjust point such that overlaps are avoided
        return new Point2D.Double(
                point.getX(), nearest.getY() - (nearest.getX() - point.getX()) * slope);
    }

    public static void drawStraightEdge(SVGGraphics2D g2d, double x1, double y1, double x2, double y2) {
        g2d.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private void drawEdgeLabel(SVGGraphics2D g2d, Edge edge, float xCoordinate, float yCoordinate) {
        Label mainLabel = edge.getLabelManager().getMainLabel();
        if (mainLabel instanceof TextLabel) {
            g2d.setFont(FontManager.fontOf((TextLabel) mainLabel));
            String text = ((TextLabel) mainLabel).getLayoutText();
            if (text == null) {
                System.out.println("Warning! No layout text found for label " + mainLabel + " of " + edge);
                return;
            }
            if (drawInfo.isShowVertexLabels()) {
                Rectangle2D background = new Rectangle2D.Double(
                        xCoordinate - (float) text.length() * 3 - 1,
                        yCoordinate - 5,
                        text.length() * 6 + 2, 10);
                g2d.setColor(Color.white);
                g2d.fill(background);
                g2d.draw(background);
                g2d.setColor(Color.black);
                g2d.drawString(text,
                        xCoordinate - (float) text.length() * 3,
                        yCoordinate + 4);
            }
        }
    }

    public void drawEdgeDirection(SVGGraphics2D g2d, Edge edge) {
        if (!edge.getDirection().equals(EdgeDirection.UNDIRECTED)) {
            Point2D arrowHead = new Point2D.Double(), arrowOrigin = new Point2D.Double();
            if (edge.getDirection().equals(EdgeDirection.OUTGOING)) {
                Path path = edge.getPaths().get(edge.getPaths().size() - 1);
                List<Point2D.Double> tbPoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                arrowHead = tbPoints.get(tbPoints.size() - 1);
                arrowOrigin = tbPoints.get(tbPoints.size() - 2);
            } else if (edge.getDirection().equals(EdgeDirection.INCOMING)) {
                Path path = edge.getPaths().get(0);
                List<Point2D.Double> tbPoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                arrowHead = tbPoints.get(0);
                arrowOrigin = tbPoints.get(1);
            }

            double triangleSize = drawInfo.computePortWidth(edge.getPorts().get(0)) * .7;
            if (arrowHead.getY() > arrowOrigin.getY() && arrowHead.getY() - arrowOrigin.getY() > triangleSize)
                arrowOrigin.setLocation(arrowOrigin.getX(), arrowHead.getY() - triangleSize);
            else if (arrowHead.getY() < arrowOrigin.getY() && arrowHead.getY() - arrowOrigin.getY() < (-1) * triangleSize)
                arrowOrigin.setLocation(arrowOrigin.getX(), arrowHead.getY() + triangleSize);

            Path2D triangle = new Path2D.Double();
            triangle.moveTo(arrowHead.getX(), arrowHead.getY());
            triangle.lineTo(arrowOrigin.getX() - triangleSize / 2, arrowOrigin.getY());
            triangle.lineTo(arrowOrigin.getX() + triangleSize / 2, arrowOrigin.getY());
            triangle.lineTo(arrowHead.getX(), arrowHead.getY());
            g2d.draw(triangle);
            g2d.fill(triangle);
        }
    }

}