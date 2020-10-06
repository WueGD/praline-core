package de.uniwue.informatik.praline.io.output.util;

import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class DrawingInformation {

    public final static double DEFAULT_BORDER_WIDTH = 2;
    public final static double DEFAULT_VERTEX_HEIGHT = 24; //32; //48;
    public final static double DEFAULT_VERTEX_MINIMUM_WIDTH = 12; //20;
    private static final Color DEFAULT_VERTEX_COLOR = null; //Color.lightGray; //null for transparent;
    public final static double DEFAULT_PORT_WIDTH = 8;
    public final static double DEFAULT_PORT_HEIGHT = 4;
    public final static double DEFAULT_PORT_SPACING = 4;
    private static final Color DEFAULT_PORT_COLOR = null; //Color.black; //null for transparent;
    public final static double DEFAULT_EDGE_DISTANCE_HORIZONTAL = 10;
    public final static double DEFAULT_EDGE_DISTANCE_VERTICAL = 10;
    public final static double DEFAULT_DISTANCE_BETWEEN_LAYERS = 20;
    public final static Font DEFAULT_FONT = new Font("Myanmar Text", Font.PLAIN, 10);
    public final static double DEFAULT_HORIZONTAL_TEXT_OFFSET = 2;
    public final static double DEFAULT_VERTICAL_TEXT_OFFSET = -5;
    public final static Color DEFAULT_PORT_PAIRING_COLOR = Color.DARK_GRAY;
    public final static boolean DEFAULT_SHOW_PORT_PAIRINGS = true;
    public final static Color DEFAULT_PORT_GROUP_COLOR = Color.LIGHT_GRAY;
    public final static double DEFAULT_PORT_GROUP_BORDER = 2;
    public final static boolean DEFAULT_SHOW_PORT_GROUPS = false;


    //TODO: JZ: I think this should not be static, but instance-wide, moreover set font in constructor
    //          currently this is done on various other places
    public static Graphics2D g2d = new BufferedImage(4000,2000,TYPE_INT_ARGB).createGraphics();


    private double borderWidth;
    private double vertexHeight; //TODO: as minimum height/flexible for diff. heights (multiple labels above each other)
    private double vertexMinimumWidth;
    private Color vertexColor; //null for transparent
    private double portWidth;
    private double portHeight;
    private double portSpacing;
    private Color portColor; //null for transparent
    private double edgeDistanceHorizontal;
    private double edgeDistanceVertical;
    private double distanceBetweenLayers;
    private Font font;
    private double horizontalTextOffset;
    private double verticalTextOffset;
    private Color portPairingColor;
    private boolean showPortPairings;
    private Color portGroupColor;
    private double portGroupBorder;
    private boolean showPortGroups;

    public DrawingInformation() {
        this(DEFAULT_BORDER_WIDTH, DEFAULT_VERTEX_HEIGHT, DEFAULT_VERTEX_MINIMUM_WIDTH, DEFAULT_VERTEX_COLOR,
                DEFAULT_PORT_WIDTH, DEFAULT_PORT_HEIGHT, DEFAULT_PORT_SPACING, DEFAULT_PORT_COLOR,
                DEFAULT_EDGE_DISTANCE_HORIZONTAL, DEFAULT_EDGE_DISTANCE_VERTICAL, DEFAULT_DISTANCE_BETWEEN_LAYERS,
                DEFAULT_FONT, DEFAULT_HORIZONTAL_TEXT_OFFSET, DEFAULT_VERTICAL_TEXT_OFFSET, DEFAULT_PORT_PAIRING_COLOR,
                DEFAULT_SHOW_PORT_PAIRINGS, DEFAULT_PORT_GROUP_COLOR, DEFAULT_PORT_GROUP_BORDER,
                DEFAULT_SHOW_PORT_GROUPS);
    }

    public DrawingInformation(double borderWidth, double vertexHeight, double vertexMinimumWidth, Color vertexColor,
                              double portWidth, double portHeight, double portSpacing, Color portColor,
                              double edgeDistanceHorizontal, double edgeDistanceVertical,
                              double distanceBetweenLayers, Font font, double horizontalTextOffset,
                              double verticalTextOffset, Color portPairingColor, boolean showPortPairings,
                              Color portGroupColor, double portGroupBorder, boolean showPortGroups) {
        this.borderWidth = borderWidth;
        this.vertexHeight = vertexHeight;
        this.vertexMinimumWidth = vertexMinimumWidth;
        this.vertexColor = vertexColor;
        this.portWidth = portWidth;
        this.portHeight = portHeight;
        this.portSpacing = portSpacing;
        this.portColor = portColor;
        this.edgeDistanceHorizontal = edgeDistanceHorizontal;
        this.edgeDistanceVertical = edgeDistanceVertical;
        this.distanceBetweenLayers = distanceBetweenLayers;
        this.font = font;
        this.horizontalTextOffset = horizontalTextOffset;
        this.verticalTextOffset = verticalTextOffset;
        this.portPairingColor = portPairingColor;
        this.showPortPairings = showPortPairings;
        this.portGroupColor = portGroupColor;
        this.portGroupBorder = portGroupBorder;
        this.showPortGroups = showPortGroups;
    }

    public static double getStringMinimumHeight() {
        return g2d.getFontMetrics().getStringBounds(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwyxz1234567890", DrawingInformation.g2d).getHeight();
    }

    public double getMinVertexWidth(Vertex vertex) {
        String[] labelStrings = new String[vertex.getLabelManager().getLabels().size()];

        int i = 0;
        for (Label label : vertex.getLabelManager().getLabels()) {
            labelStrings[i++] = label instanceof TextLabel ? ((TextLabel) label).getInputText() : "";
        }

        return getMinVertexWidth(labelStrings);
    }

    public double getMinVertexWidth(String[] labelStrings) {
        double minWidth = vertexMinimumWidth;
        for (String name : labelStrings) {
            minWidth = Math.max(minWidth, g2d.getFontMetrics().getStringBounds(name, g2d).getWidth());
        }
        return minWidth;
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

    public Font getFont() {
        return font;
    }

    public double getHorizontalTextOffset() {
        return horizontalTextOffset;
    }

    public double getVerticalTextOffset() {
        return verticalTextOffset;
    }

    public Color getPortPairingColor() {
        return portPairingColor;
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

    public void setFont(Font font) {
        this.font = font;
    }

    public void setHorizontalTextOffset(double horizontalTextOffset) {
        this.horizontalTextOffset = horizontalTextOffset;
    }

    public void setVerticalTextOffset(double verticalTextOffset) {
        this.verticalTextOffset = verticalTextOffset;
    }

    public void setPortPairingColor(Color portPairingColor) {
        this.portPairingColor = portPairingColor;
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
