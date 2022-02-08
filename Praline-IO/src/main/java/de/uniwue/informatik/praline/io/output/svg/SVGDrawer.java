package de.uniwue.informatik.praline.io.output.svg;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.io.output.util.DrawingUtils;
import de.uniwue.informatik.praline.io.output.util.FontManager;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
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
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
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
        for (Edge edge : graph.getEdges()) {
            if (edge.getPaths().isEmpty()) {
                if (edge.getPorts().size() == 2) {
                    Point2D.Double start = new Point2D.Double(edge.getPorts().get(0).getShape().getXPosition(),
                            edge.getPorts().get(0).getShape().getYPosition());
                    Point2D.Double end = new Point2D.Double(edge.getPorts().get(1).getShape().getXPosition(),
                            edge.getPorts().get(1).getShape().getYPosition());
                    g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
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
                }
            }
        }
    }

    private void drawNodeLabel(SVGGraphics2D g2d, Vertex node) {
        Rectangle2D nodeRectangle = (Rectangle2D) node.getShape();
        Label mainLabel = node.getLabelManager().getMainLabel(); //TODO: draw all labels, not only main label
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
        Label mainLabel = port.getLabelManager().getMainLabel(); //TODO: draw all labels, not only main label
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

}