package de.uniwue.informatik.praline.io.output.util;

import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class DrawingInformation {

    public final static double DEFAULT_BORDER_WIDTH = 2;
    public final static double DEFAULT_VERTEX_HEIGHT = 30; //24 //48;
    public final static double DEFAULT_VERTEX_MINIMUM_WIDTH = 12; //20;
    public final static double DEFAULT_VERTEX_WIDTH_MAX_STRETCH_FACTOR = 6.0; //1.0; //Double.POSITIVE_INFINITY
    private static final Color DEFAULT_VERTEX_COLOR = Color.lightGray; //null for transparent;
    public final static double DEFAULT_PORT_WIDTH = 8;
    public final static double DEFAULT_PORT_HEIGHT = 4;
    public final static double DEFAULT_PORT_SPACING = 4;
    private static final Color DEFAULT_PORT_COLOR = Color.black; //null for transparent;
    public final static double DEFAULT_EDGE_DISTANCE_HORIZONTAL = 10;
    public final static double DEFAULT_EDGE_DISTANCE_VERTICAL = 10;
    public final static double DEFAULT_DISTANCE_BETWEEN_LAYERS = 20;
    public final static double DEFAULT_HORIZONTAL_VERTEX_LABEL_OFFSET = 2;
    public final static double DEFAULT_VERTICAL_VERTEX_LABEL_OFFSET = -12;
    public final static double DEFAULT_HORIZONTAL_PORT_LABEL_OFFSET = -2;
    public final static double DEFAULT_VERTICAL_PORT_LABEL_OFFSET = 2.0;
    public final static Color DEFAULT_PORT_PAIRING_COLOR = Color.DARK_GRAY;
    public final static boolean DEFAULT_SHOW_PORT_PAIRINGS = true;
    public static final boolean DEFAULT_SHOW_VERTEX_LABELS = true;
    public static final boolean DEFAULT_SHOW_PORT_LABELS = true;
    public final static Color DEFAULT_PORT_GROUP_COLOR = Color.LIGHT_GRAY;
    public final static double DEFAULT_PORT_GROUP_BORDER = 2;
    public final static boolean DEFAULT_SHOW_PORT_GROUPS = false;


    //TODO: JZ: I think this should not be static, but instance-wide, moreover set font in constructor
    //          currently this is done on various other places
    public static Graphics2D g2d = new BufferedImage(4000,2000,TYPE_INT_ARGB).createGraphics();

    private double borderWidth;
    private double vertexHeight; //TODO: as minimum height/flexible for diff. heights (multiple labels above each other)
    private double vertexMinimumWidth;
    private double vertexWidthMaxStretchFactor; //for vertical stretch; 1 means (almost) no stretch, positive infinity
    // means arbitrary stretch
    private Color vertexColor; //null for transparent
    private double portWidth;
    private double portHeight;
    private double portSpacing;
    private Color portColor; //null for transparent
    private double edgeDistanceHorizontal;
    private double edgeDistanceVertical;
    private double distanceBetweenLayers;
    private double horizontalVertexLabelOffset;
    private double verticalVertexLabelOffset;
    private double horizontalPortLabelOffset;
    private double verticalPortLabelOffset;
    private Color portPairingColor;
    private boolean showVertexLabels;
    private boolean showPortLabels;
    private boolean showPortPairings;
    private Color portGroupColor;
    private double portGroupBorder;
    private boolean showPortGroups;

    public DrawingInformation() {
        this(DEFAULT_BORDER_WIDTH, DEFAULT_VERTEX_HEIGHT, DEFAULT_VERTEX_MINIMUM_WIDTH,
                DEFAULT_VERTEX_WIDTH_MAX_STRETCH_FACTOR, DEFAULT_VERTEX_COLOR, DEFAULT_PORT_WIDTH, DEFAULT_PORT_HEIGHT,
                DEFAULT_PORT_SPACING, DEFAULT_PORT_COLOR, DEFAULT_EDGE_DISTANCE_HORIZONTAL,
                DEFAULT_EDGE_DISTANCE_VERTICAL, DEFAULT_DISTANCE_BETWEEN_LAYERS,
                DEFAULT_HORIZONTAL_VERTEX_LABEL_OFFSET, DEFAULT_VERTICAL_VERTEX_LABEL_OFFSET,
                DEFAULT_HORIZONTAL_PORT_LABEL_OFFSET, DEFAULT_VERTICAL_PORT_LABEL_OFFSET, DEFAULT_PORT_PAIRING_COLOR,
                DEFAULT_SHOW_VERTEX_LABELS, DEFAULT_SHOW_PORT_LABELS, DEFAULT_SHOW_PORT_PAIRINGS,
                DEFAULT_PORT_GROUP_COLOR, DEFAULT_PORT_GROUP_BORDER, DEFAULT_SHOW_PORT_GROUPS);
    }

    public DrawingInformation(double borderWidth, double vertexHeight, double vertexMinimumWidth,
                              double vertexWidthMaxStretchFactor, Color vertexColor, double portWidth,
                              double portHeight, double portSpacing, Color portColor, double edgeDistanceHorizontal,
                              double edgeDistanceVertical, double distanceBetweenLayers,
                              double horizontalVertexLabelOffset, double verticalVertexLabelOffset,
                              double horizontalPortLabelOffset, double verticalPortLabelOffset,
                              Color portPairingColor, boolean showVertexLabels, boolean showPortLabels,
                              boolean showPortPairings, Color portGroupColor, double portGroupBorder,
                              boolean showPortGroups) {
        this.borderWidth = borderWidth;
        this.vertexHeight = vertexHeight;
        this.vertexMinimumWidth = vertexMinimumWidth;
        this.vertexWidthMaxStretchFactor = vertexWidthMaxStretchFactor;
        this.vertexColor = vertexColor;
        this.portWidth = portWidth;
        this.portHeight = portHeight;
        this.portSpacing = portSpacing;
        this.portColor = portColor;
        this.edgeDistanceHorizontal = edgeDistanceHorizontal;
        this.edgeDistanceVertical = edgeDistanceVertical;
        this.distanceBetweenLayers = distanceBetweenLayers;
        this.horizontalVertexLabelOffset = horizontalVertexLabelOffset;
        this.verticalVertexLabelOffset = verticalVertexLabelOffset;
        this.horizontalPortLabelOffset = horizontalPortLabelOffset;
        this.verticalPortLabelOffset = verticalPortLabelOffset;
        this.portPairingColor = portPairingColor;
        this.showVertexLabels = showVertexLabels;
        this.showPortLabels = showPortLabels;
        this.showPortPairings = showPortPairings;
        this.portGroupColor = portGroupColor;
        this.portGroupBorder = portGroupBorder;
        this.showPortGroups = showPortGroups;
    }

    public double getMinVertexWidth(Vertex vertex) {
        double labelWidth = getMinLabelWidth(vertex.getLabelManager().getLabels());

        double givenWidth = vertex.getShape() != null && vertex.getShape() instanceof Rectangle &&
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) vertex.getShape()).getWidth() >= 0 ?
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) vertex.getShape()).getWidth() :
                getVertexMinimumWidth();

        return Math.max(labelWidth, givenWidth);
    }

    private double getMinLabelWidth(Collection<Label> labels) {
        double minWidth = 0;
        for (Label label : labels) {
            if (label instanceof TextLabel) {
                g2d.setFont(FontManager.fontOf((TextLabel) label));
                minWidth = Math.max(minWidth, g2d.getFontMetrics().getStringBounds(
                        ((TextLabel) label).getLayoutText(), g2d).getWidth());
            }
        }
        return minWidth;
    }

    public double getVertexHeight(Vertex vertex) {
        double labelHeight = getMinLabelHeight(vertex.getLabelManager().getLabels());

        double givenWidth = vertex.getShape() != null && vertex.getShape() instanceof Rectangle &&
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) vertex.getShape()).getHeight() >= 0 ?
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) vertex.getShape()).getHeight() :
                getVertexHeight();

        return Math.max(labelHeight, givenWidth);
    }

    private double getMinLabelHeight(Collection<Label> labels) {
        double minHeight = vertexHeight;
        for (Label label : labels) {
            if (label instanceof TextLabel) {
                g2d.setFont(FontManager.fontOf((TextLabel) label));
                minHeight = Math.max(minHeight, g2d.getFontMetrics().getStringBounds(
                        ((TextLabel) label).getLayoutText(), g2d).getHeight());
            }
        }
        return minHeight;
    }

    public double getBorderWidth() {
        return borderWidth;
    }

    public double getVertexHeight() {
        return vertexHeight;
    }

    public double getVertexMinimumWidth() {
        return vertexMinimumWidth;
    }

    public double getVertexWidthMaxStretchFactor() {
        return vertexWidthMaxStretchFactor;
    }

    public Color getVertexColor() {
        return vertexColor;
    }

    public double getEdgeDistanceHorizontal() {
        return edgeDistanceHorizontal;
    }

    public double getEdgeDistanceVertical() {
        return edgeDistanceVertical;
    }

    public double getPortWidth() {
        return portWidth;
    }

    public double getPortHeight() {
        return portHeight;
    }

    public double getPortSpacing() {
        return portSpacing;
    }

    public Color getPortColor() {
        return portColor;
    }

    public double getDistanceBetweenLayers() {
        return distanceBetweenLayers;
    }

    public double getHorizontalVertexLabelOffset() {
        return horizontalVertexLabelOffset;
    }

    public double getVerticalVertexLabelOffset() {
        return verticalVertexLabelOffset;
    }

    public double getHorizontalPortLabelOffset() {
        return horizontalPortLabelOffset;
    }

    public double getVerticalPortLabelOffset() {
        return verticalPortLabelOffset;
    }

    public Color getPortPairingColor() {
        return portPairingColor;
    }

    public boolean isShowVertexLabels() {
        return showVertexLabels;
    }

    public boolean isShowPortLabels() {
        return showPortLabels;
    }

    public boolean isShowPortPairings() {
        return showPortPairings;
    }

    public Color getPortGroupColor() {
        return portGroupColor;
    }

    public double getPortGroupBorder() {
        return portGroupBorder;
    }

    public boolean isShowPortGroups() {
        return showPortGroups;
    }

    public void setBorderWidth(double borderWidth) {
        this.borderWidth = borderWidth;
    }

    public void setVertexHeight(double vertexHeight) {
        this.vertexHeight = vertexHeight;
    }

    public void setVertexMinimumWidth(double vertexMinimumWidth) {
        this.vertexMinimumWidth = vertexMinimumWidth;
    }

    public void setVertexColor(Color vertexColor) {
        this.vertexColor = vertexColor;
    }

    public void setPortWidth(double portWidth) {
        this.portWidth = portWidth;
    }

    public void setPortHeight(double portHeight) {
        this.portHeight = portHeight;
    }

    public void setPortSpacing(double portSpacing) {
        this.portSpacing = portSpacing;
    }

    public void setPortColor(Color portColor) {
        this.portColor = portColor;
    }

    public void setEdgeDistanceHorizontal(double edgeDistanceHorizontal) {
        this.edgeDistanceHorizontal = edgeDistanceHorizontal;
    }

    public void setEdgeDistanceVertical(double edgeDistanceVertical) {
        this.edgeDistanceVertical = edgeDistanceVertical;
    }

    public void setDistanceBetweenLayers(double distanceBetweenLayers) {
        this.distanceBetweenLayers = distanceBetweenLayers;
    }

    public void setHorizontalVertexLabelOffset(double horizontalVertexLabelOffset) {
        this.horizontalVertexLabelOffset = horizontalVertexLabelOffset;
    }

    public void setVerticalVertexLabelOffset(double verticalVertexLabelOffset) {
        this.verticalVertexLabelOffset = verticalVertexLabelOffset;
    }

    public void setHorizontalPortLabelOffset(double horizontalPortLabelOffset) {
        this.horizontalPortLabelOffset = horizontalPortLabelOffset;
    }

    public void setVerticalPortLabelOffset(double verticalPortLabelOffset) {
        this.verticalPortLabelOffset = verticalPortLabelOffset;
    }

    public void setPortPairingColor(Color portPairingColor) {
        this.portPairingColor = portPairingColor;
    }

    public void setShowVertexLabels(boolean showVertexLabels) {
        this.showVertexLabels = showVertexLabels;
    }

    public void setShowPortLabels(boolean showPortLabels) {
        this.showPortLabels = showPortLabels;
    }

    public void setShowPortPairings(boolean showPortPairings) {
        this.showPortPairings = showPortPairings;
    }

    public void setPortGroupColor(Color portGroupColor) {
        this.portGroupColor = portGroupColor;
    }

    public void setPortGroupBorder(double portGroupBorder) {
        this.portGroupBorder = portGroupBorder;
    }

    public void setShowPortGroups(boolean showPortGroups) {
        this.showPortGroups = showPortGroups;
    }
}
