package de.uniwue.informatik.praline.io.output.svg;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

public class SVGDrawer {

    //TODO was created for the specific use for the layered drawing algo --> change to make it general usable

    private static final double EMPTY_MARGIN_WIDTH = 20.0;
    private final Graph graph;

    private DrawingInformation drawInfo;

    public SVGDrawer(Graph graph) {
        this.graph = graph;
    }

    public void draw(String savePath, DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
        // Get a DOMImplementation.
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // Ask the test to render into the SVG Graphics2D implementation.
        paint(svgGenerator);

        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes
        try {
            svgGenerator.stream(savePath, useCSS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void paint(SVGGraphics2D g2d) {
        g2d.setFont(drawInfo.getFont());

        //set canvas
        Rectangle2D bounds = determineDrawingBounds();
        g2d.translate(-bounds.getX(), - bounds.getY());
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
                paintPortComposition(pc, g2d);
            }
            //draw node label
            Rectangle2D nodeRectangle = (Rectangle2D) node.getShape();
            g2d.drawString(node.getLabelManager().getMainLabel().toString(),
                    (float) ((nodeRectangle).getX() + drawInfo.getHorizontalTextOffset()),
                    (float) (((nodeRectangle).getY()
                            + (nodeRectangle).getHeight()
                            + drawInfo.getVerticalTextOffset())));
        }
        //draw edges
        for (Edge edge : graph.getEdges()) {
            if (edge.getPaths().isEmpty()) {
                Point2D.Double start = new Point2D.Double(edge.getPorts().get(0).getShape().getXPosition(), edge.getPorts().get(0).getShape().getYPosition());
                Point2D.Double end = new Point2D.Double(edge.getPorts().get(1).getShape().getXPosition(), edge.getPorts().get(1).getShape().getYPosition());
                g2d.drawLine(((int) Math.round(start.getX())), ((int) Math.round(start.getY())), ((int) Math.round(end.getX())), ((int) Math.round(end.getY())));
            } else {
                for (Path path : edge.getPaths()) {
                    List<Point2D.Double> edgePoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                    Point2D.Double start = null;
                    for (Point2D.Double end : edgePoints) {
                        if (start != null) {
                            g2d.drawLine(((int) Math.round(start.getX())), ((int) Math.round(start.getY())),
                                    ((int) Math.round(end.getX())), ((int) Math.round(end.getY())));
                        }
                        start = end;
                    }
                }
            }
        }
    }

    private Rectangle2D determineDrawingBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Vertex node : graph.getVertices()) {
            if (node.getShape() == null) {
                node.setShape(new Rectangle(drawInfo.getVertexMinimumWidth(), drawInfo.getVertexHeight()));
            }
            Rectangle2D nodeRectangle = (Rectangle2D) node.getShape();
            minX = Math.min(minX, nodeRectangle.getX());
            maxX = Math.max(maxX, nodeRectangle.getX() + nodeRectangle.getWidth());
            minY = Math.min(minY, nodeRectangle.getY());
            maxY = Math.max(maxY, nodeRectangle.getY() + nodeRectangle.getHeight());
        }
        for (Edge edge : graph.getEdges()) {
            if (!edge.getPaths().isEmpty()) {
                for (Path path : edge.getPaths()) {
                    List<Point2D.Double> edgePoints = ((PolygonalPath) path).getTerminalAndBendPoints();
                    for (Point2D.Double end : edgePoints) {
                        minX = Math.min(minX, end.getX());
                        maxX = Math.max(maxX, end.getX());
                        minY = Math.min(minY, end.getY());
                        maxY = Math.max(maxY, end.getY());
                    }
                }
            }
        }


        minX = Math.min(minX - EMPTY_MARGIN_WIDTH, Double.MAX_VALUE);
        maxX = Math.max(maxX + EMPTY_MARGIN_WIDTH, minX);
        minY = Math.min(minY - EMPTY_MARGIN_WIDTH, Double.MAX_VALUE);
        maxY = Math.max(maxY + EMPTY_MARGIN_WIDTH, minY);
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
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
        g2d.drawLine(((int) Math.round(start.getX())), ((int) Math.round(start.getY())), ((int) Math.round(end.getX())), ((int) Math.round(end.getY())));

        g2d.setColor(saveColor);

    }

    private Rectangle2D paintPortComposition(PortComposition pc, Graphics2D g2d) {
        if (pc instanceof PortGroup) {
            double maxX = Double.MIN_VALUE;
            double minX = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            for (PortComposition pcspc : ((PortGroup)pc).getPortCompositions()) {
                Rectangle2D rect = paintPortComposition(pcspc, g2d);
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
            return portRectangle;
            // g2d.drawString(((Port) pc).getLabelManager().getMainLabel().toString(), ((float)(((Rectangle2D)(((Port) pc).getShape())).getX())), ((float)(((Rectangle2D)(((Port) pc).getShape())).getY())));
        }
        return null;
    }
}