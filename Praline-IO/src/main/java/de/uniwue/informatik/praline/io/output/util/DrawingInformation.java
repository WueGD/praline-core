package de.uniwue.informatik.praline.io.output.util;

import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;

import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class DrawingInformation {

    public final static double DEFAULT_BORDER_WIDTH = 2;
    public final static double DEFAULT_VERTEX_HEIGHT = 30; //24 //48;
    public final static double DEFAULT_VERTEX_MINIMUM_WIDTH = 12; //20;
    public final static double DEFAULT_VERTEX_WIDTH_MAX_STRETCH_FACTOR = 16.0; //1.0; //Double.POSITIVE_INFINITY;
    private static final Color DEFAULT_VERTEX_COLOR = Color.lightGray; //null for transparent;
    public final static double DEFAULT_PORT_WIDTH = 8;
    public final static double DEFAULT_PORT_HEIGHT = 4;
    public final static double DEFAULT_PORT_SPACING = 4;
    private static final Color DEFAULT_PORT_COLOR = Color.black; //null for transparent;
    public final static double DEFAULT_EDGE_DISTANCE_HORIZONTAL = 10;
    public final static double DEFAULT_EDGE_DISTANCE_VERTICAL = 10;
    public final static double DEFAULT_DISTANCE_BETWEEN_LAYERS = 20;
    public final static double DEFAULT_DISTANCE_BETWEEN_CONNECTED_COMPONENTS = 20;
    public final static double DEFAULT_HORIZONTAL_VERTEX_LABEL_OFFSET = 2;
    public final static double DEFAULT_VERTICAL_VERTEX_LABEL_OFFSET = 0;
    public final static double DEFAULT_HORIZONTAL_PORT_LABEL_OFFSET = -2;
    public final static double DEFAULT_VERTICAL_PORT_LABEL_OFFSET = 0;
    public final static Color DEFAULT_PORT_PAIRING_COLOR = Color.DARK_GRAY;
    public final static boolean DEFAULT_SHOW_PORT_PAIRINGS = true;
    public static final boolean DEFAULT_SHOW_VERTEX_LABELS = true;
    public static final boolean DEFAULT_SHOW_PORT_LABELS = true;
    public static final boolean DEFAULT_SHOW_VERTEX_LABEL_FRAMES = false;
    public static final boolean DEFAULT_SHOW_PORT_LABEL_FRAMES = false;
    public final static Color DEFAULT_PORT_GROUP_COLOR = Color.LIGHT_GRAY;
    public final static double DEFAULT_PORT_GROUP_BORDER = 2;
    public final static boolean DEFAULT_SHOW_PORT_GROUPS = false;


    //TODO: JZ: I think this should not be static, but instance-wide, moreover set font in constructor
    //          currently this is done on various other places
    public static Graphics2D g2d = new BufferedImage(4000,2000,TYPE_INT_ARGB).createGraphics();

    private double borderWidth;
    private double vertexHeight; //TODO: as minimum height/flexible for diff. heights (multiple labels above each other)
    private double vertexMinimumWidth;
    private double vertexWidthMaxStretchFactor; //for horizontal stretch; 1 means (almost) no stretch, positive infinity
    // means arbitrary stretch
    private Color vertexColor; //null for transparent
    private double portWidth;
    private double portHeight;
    private double portSpacing;
    private Color portColor; //null for transparent
    private double edgeDistanceHorizontal;
    private double edgeDistanceVertical;
    private double distanceBetweenLayers;
    private double distanceBetweenConnectedComponents;
    private double horizontalVertexLabelOffset;
    private double verticalVertexLabelOffset;
    private double horizontalPortLabelOffset;
    private double verticalPortLabelOffset;
    private Color portPairingColor;
    private boolean showVertexLabels;
    private boolean showPortLabels;
    private boolean showVertexLabelFrames;
    private boolean showPortLabelFrames;
    private boolean showPortPairings;
    private Color portGroupColor;
    private double portGroupBorder;
    private boolean showPortGroups;

    public DrawingInformation() {
        this(DEFAULT_BORDER_WIDTH, DEFAULT_VERTEX_HEIGHT, DEFAULT_VERTEX_MINIMUM_WIDTH,
                DEFAULT_VERTEX_WIDTH_MAX_STRETCH_FACTOR, DEFAULT_VERTEX_COLOR, DEFAULT_PORT_WIDTH, DEFAULT_PORT_HEIGHT,
                DEFAULT_PORT_SPACING, DEFAULT_PORT_COLOR, DEFAULT_EDGE_DISTANCE_HORIZONTAL,
                DEFAULT_EDGE_DISTANCE_VERTICAL, DEFAULT_DISTANCE_BETWEEN_LAYERS,
                DEFAULT_DISTANCE_BETWEEN_CONNECTED_COMPONENTS, DEFAULT_HORIZONTAL_VERTEX_LABEL_OFFSET,
                DEFAULT_VERTICAL_VERTEX_LABEL_OFFSET, DEFAULT_HORIZONTAL_PORT_LABEL_OFFSET,
                DEFAULT_VERTICAL_PORT_LABEL_OFFSET, DEFAULT_PORT_PAIRING_COLOR, DEFAULT_SHOW_VERTEX_LABELS,
                DEFAULT_SHOW_PORT_LABELS, DEFAULT_SHOW_VERTEX_LABEL_FRAMES, DEFAULT_SHOW_PORT_LABEL_FRAMES,
                DEFAULT_SHOW_PORT_PAIRINGS, DEFAULT_PORT_GROUP_COLOR, DEFAULT_PORT_GROUP_BORDER,
                DEFAULT_SHOW_PORT_GROUPS);
    }

    public DrawingInformation(double borderWidth, double vertexHeight, double vertexMinimumWidth,
                              double vertexWidthMaxStretchFactor, Color vertexColor, double portWidth,
                              double portHeight, double portSpacing, Color portColor, double edgeDistanceHorizontal,
                              double edgeDistanceVertical, double distanceBetweenLayers,
                              double distanceBetweenConnectedComponents, double horizontalVertexLabelOffset,
                              double verticalVertexLabelOffset, double horizontalPortLabelOffset,
                              double verticalPortLabelOffset, Color portPairingColor, boolean showVertexLabels,
                              boolean showPortLabels, boolean showVertexLabelFrames, boolean showPortLabelFrames,
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
        this.distanceBetweenConnectedComponents = distanceBetweenConnectedComponents;
        this.horizontalVertexLabelOffset = horizontalVertexLabelOffset;
        this.verticalVertexLabelOffset = verticalVertexLabelOffset;
        this.horizontalPortLabelOffset = horizontalPortLabelOffset;
        this.verticalPortLabelOffset = verticalPortLabelOffset;
        this.portPairingColor = portPairingColor;
        this.showVertexLabels = showVertexLabels;
        this.showPortLabels = showPortLabels;
        this.showVertexLabelFrames = showVertexLabelFrames;
        this.showPortLabelFrames = showPortLabelFrames;
        this.showPortPairings = showPortPairings;
        this.portGroupColor = portGroupColor;
        this.portGroupBorder = portGroupBorder;
        this.showPortGroups = showPortGroups;
    }

    //old constructor to be removed later (is still here to avoid potential compatibility issues)
    @Deprecated
    public DrawingInformation(double borderWidth, double vertexHeight, double vertexMinimumWidth,
                              double vertexWidthMaxStretchFactor, Color vertexColor, double portWidth,
                              double portHeight, double portSpacing, Color portColor, double edgeDistanceHorizontal,
                              double edgeDistanceVertical, double distanceBetweenLayers,
                              double horizontalVertexLabelOffset, double verticalVertexLabelOffset,
                              double horizontalPortLabelOffset, double verticalPortLabelOffset,
                              Color portPairingColor, boolean showVertexLabels, boolean showPortLabels,
                              boolean showVertexLabelFrames, boolean showPortLabelFrames, boolean showPortPairings,
                              Color portGroupColor, double portGroupBorder, boolean showPortGroups) {
        this(borderWidth, vertexHeight, vertexMinimumWidth, vertexWidthMaxStretchFactor, vertexColor, portWidth,
                portHeight, portSpacing, portColor, edgeDistanceHorizontal, edgeDistanceVertical,
                distanceBetweenLayers, DEFAULT_DISTANCE_BETWEEN_CONNECTED_COMPONENTS, horizontalVertexLabelOffset,
                verticalVertexLabelOffset, horizontalPortLabelOffset, verticalPortLabelOffset, portPairingColor,
                showVertexLabels, showPortLabels, showVertexLabelFrames, showPortLabelFrames, showPortPairings,
                portGroupColor, portGroupBorder, showPortGroups);
    }

    public double computeMinVertexWidth(Vertex vertex) {
        double labelWidth = getMinLabelWidth(vertex.getLabelManager().getLabels());
        double labelWithBufferWidth = labelWidth + 2.0 * this.horizontalVertexLabelOffset;

        double givenWidth =
                // we don't use the pre-set width any more here because maybe we want to tighten the existing node.
                // Then we cannot depend on the currently set width
//                vertex.getShape() != null && vertex.getShape() instanceof Rectangle2D &&
//                ((Rectangle2D) vertex.getShape()).getWidth() >= 0 ?
//                ((Rectangle2D) vertex.getShape()).getWidth() :
                this.vertexMinimumWidth;

        return Math.max(labelWithBufferWidth, givenWidth);
    }

    public double computePortWidth(Port port) {
        double labelWidth = getMinLabelWidth(port.getLabelManager().getLabels());
        double labelWithBufferWidth = labelWidth + 2.0 * this.horizontalPortLabelOffset;

        double givenWidth = port.getShape() != null && port.getShape() instanceof Rectangle &&
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) port.getShape()).getWidth() >= 0 ?
                ((de.uniwue.informatik.praline.datastructure.shapes.Rectangle) port.getShape()).getWidth() :
                this.portWidth;

        return Math.max(labelWithBufferWidth, givenWidth);
    }

    private double getMinLabelWidth(Collection<Label<? extends LabelStyle>> labels) {
        double minWidth = 0;
        for (Label<? extends LabelStyle> label : labels) {
            if (label instanceof TextLabel) {
                g2d.setFont(FontManager.fontOf((TextLabel) label));
                if (((TextLabel) label).getLayoutText() != null) {
                    minWidth = Math.max(minWidth,
                            g2d.getFontMetrics().getStringBounds(((TextLabel) label).getLayoutText(), g2d).getWidth());
                }
            }
        }
        return minWidth;
    }

    public double computeMinVertexHeight(Vertex vertex) {
        double labelHeight = getMinLabelHeight(vertex.getLabelManager().getLabels());

        double givenHeight =
                // we don't use the pre-set height any more here because maybe we want to tighten the existing node.
                // Then we cannot depend on the currently set height
//                vertex.getShape() != null && vertex.getShape() instanceof Rectangle2D &&
//                ((Rectangle2D) vertex.getShape()).getHeight() >= 0 ?
//                ((Rectangle2D) vertex.getShape()).getHeight() :
                this.vertexHeight;

        return Math.max(labelHeight, givenHeight);
    }

    private double getMinLabelHeight(Collection<Label<? extends LabelStyle>> labels) {
        double minHeight = vertexHeight;
        for (Label<? extends LabelStyle> label : labels) {
            if (label instanceof TextLabel) {
                g2d.setFont(FontManager.fontOf((TextLabel) label));
                if (((TextLabel) label).getLayoutText() != null) {
                    minHeight = Math.max(minHeight,
                            g2d.getFontMetrics().getStringBounds(((TextLabel) label).getLayoutText(), g2d).getHeight());
                }
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

    public double getDistanceBetweenConnectedComponents() {
        return distanceBetweenConnectedComponents;
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

    public void setDistanceBetweenConnectedComponents(double distanceBetweenConnectedComponents) {
        this.distanceBetweenConnectedComponents = distanceBetweenConnectedComponents;
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

    public boolean isShowVertexLabelFrames() {
        return showVertexLabelFrames;
    }

    public boolean isShowPortLabelFrames() {
        return showPortLabelFrames;
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
