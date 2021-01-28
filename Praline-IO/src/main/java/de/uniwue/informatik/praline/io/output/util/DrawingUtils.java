package de.uniwue.informatik.praline.io.output.util;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.*;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class DrawingUtils {
    public static Rectangle2D determineDrawingBounds(Graph graph, DrawingInformation drawInfo, double margin) {
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


        minX = Math.min(minX - margin, Double.MAX_VALUE);
        maxX = Math.max(maxX + margin, minX);
        minY = Math.min(minY - margin, Double.MAX_VALUE);
        maxY = Math.max(maxY + margin, minY);
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    public static void translate(Graph graph, double xOffset, double yOffset) {
        for (Vertex vertex : graph.getVertices()) {
            safeTranslate(vertex, xOffset, yOffset);
            for (Label label : vertex.getLabelManager().getLabels()) {
                safeTranslate(label, xOffset, yOffset);
            }
            for (Port port : vertex.getPorts()) {
                safeTranslate(port, xOffset, yOffset);
                for (Label label : port.getLabelManager().getLabels()) {
                    safeTranslate(label, xOffset, yOffset);
                }
            }
        }
        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
            safeTranslate(vertexGroup, xOffset, yOffset);
            for (Label label : vertexGroup.getLabelManager().getLabels()) {
                safeTranslate(label, xOffset, yOffset);
            }
        }
        for (Edge edge : graph.getEdges()) {
            if (edge.getPaths() != null) {
                for (Path path : edge.getPaths()) {
                    path.translate(xOffset, yOffset);
                }
            }
            for (Label label : edge.getLabelManager().getLabels()) {
                safeTranslate(label, xOffset, yOffset);
            }
        }
        for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
            for (Label label : edgeBundle.getLabelManager().getLabels()) {
                safeTranslate(label, xOffset, yOffset);
            }
        }
    }

    private static void safeTranslate(ShapedObject shapedObject, double xOffset, double yOffset) {
        if (shapedObject.getShape() != null) {
            shapedObject.getShape().translate(xOffset, yOffset);
        }
    }
}
