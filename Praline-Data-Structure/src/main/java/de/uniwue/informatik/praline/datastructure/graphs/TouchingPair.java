package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public TouchingPair(
            @JsonProperty("vertex0") final Vertex vertex0,
            @JsonProperty("horizontalPlacementVertex0") final HorizontalPlacement horizontalPlacementVertex0,
            @JsonProperty("verticalPlacementVertex0") final VerticalPlacement verticalPlacementVertex0,
            @JsonProperty("vertex1") final Vertex vertex1,
            @JsonProperty("horizontalPlacementVertex1") final HorizontalPlacement horizontalPlacementVertex1,
            @JsonProperty("verticalPlacementVertex1") final VerticalPlacement verticalPlacementVertex1
    ) {
        this.vertex0 = vertex0;
        this.horizontalPlacementVertex0 = horizontalPlacementVertex0 == null ? HorizontalPlacement.FREE :
                horizontalPlacementVertex0;
        this.verticalPlacementVertex0 = verticalPlacementVertex0 == null ? VerticalPlacement.FREE :
                verticalPlacementVertex0;
        this.vertex1 = vertex1;
        this.horizontalPlacementVertex1 = horizontalPlacementVertex1 == null ? HorizontalPlacement.FREE :
                horizontalPlacementVertex1;
        this.verticalPlacementVertex1 = verticalPlacementVertex1 == null ? VerticalPlacement.FREE :
                verticalPlacementVertex1;
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


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return "[" + getVertex0() + "|" + getVertex1() + "]";
    }
}
