package de.uniwue.informatik.praline.datastructure.graph;

import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;

public class TouchingPair {

    /*==========
     * Instance variables
     *==========*/

    private Vertex vertex0;
    private HorizontalPlacement horizontalPlacementVertex0;
    private VerticalPlacement verticalPlacementVertex0;
    private Vertex vertex1;
    private HorizontalPlacement horizontalPlacementVertex1;
    private VerticalPlacement verticalPlacementVertex1;


    /*==========
     * Constructors
     *==========*/

    public TouchingPair(Vertex vertex0, Vertex vertex1) {
        this(vertex0, HorizontalPlacement.FREE, VerticalPlacement.FREE, vertex1, HorizontalPlacement.FREE,
                VerticalPlacement.FREE);
    }

    public TouchingPair(Vertex vertex0, HorizontalPlacement horizontalPlacementVertex0,
                        VerticalPlacement verticalPlacementVertex0, Vertex vertex1) {
        this(vertex0, horizontalPlacementVertex0, verticalPlacementVertex0, vertex1, HorizontalPlacement.FREE,
                VerticalPlacement.FREE);
    }

    public TouchingPair(Vertex vertex0, HorizontalPlacement horizontalPlacementVertex0,
                        VerticalPlacement verticalPlacementVertex0, Vertex vertex1,
                        HorizontalPlacement horizontalPlacementVertex1, VerticalPlacement verticalPlacementVertex1) {
        this.vertex0 = vertex0;
        this.horizontalPlacementVertex0 = horizontalPlacementVertex0;
        this.verticalPlacementVertex0 = verticalPlacementVertex0;
        this.vertex1 = vertex1;
        this.horizontalPlacementVertex1 = horizontalPlacementVertex1;
        this.verticalPlacementVertex1 = verticalPlacementVertex1;
    }

    /*==========
     * Getters & Setters
     *==========*/

    public Vertex getVertex0() {
        return vertex0;
    }

    public void setVertex0(Vertex vertex0) {
        this.vertex0 = vertex0;
    }

    public HorizontalPlacement getHorizontalPlacementVertex0() {
        return horizontalPlacementVertex0;
    }

    public void setHorizontalPlacementVertex0(HorizontalPlacement horizontalPlacementVertex0) {
        this.horizontalPlacementVertex0 = horizontalPlacementVertex0;
    }

    public VerticalPlacement getVerticalPlacementVertex0() {
        return verticalPlacementVertex0;
    }

    public void setVerticalPlacementVertex0(VerticalPlacement verticalPlacementVertex0) {
        this.verticalPlacementVertex0 = verticalPlacementVertex0;
    }

    public Vertex getVertex1() {
        return vertex1;
    }

    public void setVertex1(Vertex vertex1) {
        this.vertex1 = vertex1;
    }

    public HorizontalPlacement getHorizontalPlacementVertex1() {
        return horizontalPlacementVertex1;
    }

    public void setHorizontalPlacementVertex1(HorizontalPlacement horizontalPlacementVertex1) {
        this.horizontalPlacementVertex1 = horizontalPlacementVertex1;
    }

    public VerticalPlacement getVerticalPlacementVertex1() {
        return verticalPlacementVertex1;
    }

    public void setVerticalPlacementVertex1(VerticalPlacement verticalPlacementVertex1) {
        this.verticalPlacementVertex1 = verticalPlacementVertex1;
    }
}
