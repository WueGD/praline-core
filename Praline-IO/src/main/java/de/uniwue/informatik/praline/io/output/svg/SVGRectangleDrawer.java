package de.uniwue.informatik.praline.io.output.svg;

import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.io.output.util.DrawingUtils;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SVGRectangleDrawer {

    public static final double EMPTY_MARGIN_WIDTH = 20.0;
    private static final boolean USE_CSS = true; // we want to use CSS style attributes

    private Collection<Rectangle> blackRectangles;
    private Collection<? extends Shape> grayShapes;
    private double grayRectangleXOffset;
    private double grayRectangleYOffset;
    private Collection<Pair<Rectangle>> edges;

    public SVGRectangleDrawer() { }

    public void draw(String savePath, Collection<Rectangle> blackRectangles, Collection<? extends Shape> grayShapes,
                     double grayRectangleXOffset, double grayRectangleYOffset, Collection<Pair<Rectangle>> edges) {
        this.blackRectangles = blackRectangles;
        this.grayShapes = grayShapes;
        this.grayRectangleXOffset = grayRectangleXOffset;
        this.grayRectangleYOffset = grayRectangleYOffset;
        this.edges = edges;
        SVGGraphics2D svgGenerator = this.getSvgGenerator();

        // Finally, stream out SVG to a file using UTF-8 encoding.
        try {
            svgGenerator.stream(savePath, USE_CSS);
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
        //set canvas TODO: at the moment we ignor the gray shapes for the canvas
        Rectangle2D bounds = DrawingUtils.determineDrawingBounds(blackRectangles, EMPTY_MARGIN_WIDTH);
        g2d.translate(-bounds.getX(), -bounds.getY());
        int canvasWidth = (int) (bounds.getWidth());
        int canvasHeight = (int) (bounds.getHeight());
        g2d.setSVGCanvasSize(new Dimension(canvasWidth, canvasHeight));

        //draw gray rectangles
        g2d.setColor(Color.LIGHT_GRAY);
        for (Shape shape : grayShapes) {
            Shape toBeDrawn = shape;
            if (shape instanceof Rectangle) {
                toBeDrawn = new Rectangle(((Rectangle) shape).x + grayRectangleXOffset,
                        ((Rectangle) shape).y + grayRectangleYOffset, ((Rectangle) shape).width,
                        ((Rectangle) shape).height);
            }
            g2d.fill(toBeDrawn);
            g2d.draw(toBeDrawn);
        }

        //draw black rectangles
        g2d.setColor(Color.BLACK);
        for (Rectangle rectangle : blackRectangles) {
            g2d.fill(rectangle);
            g2d.draw(rectangle);
        }

        //draw edges
        for (Pair<Rectangle> edge : edges) {
            Rectangle r0 = edge.getFirst();
            Rectangle r1 = edge.getSecond();

            g2d.draw(new Line2D.Double(r0.getCenterX(),
                    r0.getY() + r0.getHeight() < r1.getY() ? r0.getY() + r0.getHeight() : r0.getY(),
                    r1.getCenterX(),
                    r1.getY() + r1.getHeight() < r0.getY() ? r1.getY() + r1.getHeight() : r1.getY()));
        }
    }
}
