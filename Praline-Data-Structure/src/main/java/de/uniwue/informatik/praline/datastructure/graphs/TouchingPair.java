package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A {@link TouchingPair} specifies two vertices ({@link Vertex}) that must be drawn such that their boundaries touch
 * each other.
 * They are stored in {@link VertexGroup}s.
 *
 * The user who give the input has to take care that the information provided by a {@link TouchingPair} is consistent.
 * So e. g. the {@link Vertex}es connected via a {@link TouchingPair} must be in the same {@link VertexGroup}, which is
 * also the same {@link VertexGroup} where this {@link TouchingPair} is stored.
 * The {@link HorizontalPlacement} and {@link VerticalPlacement} of the one {@link Vertex} in this
 * {@link TouchingPair} must be consistent with the {@link HorizontalPlacement} and {@link VerticalPlacement} of the
 * other {@link Vertex}.
 * So e. g. if they should be drawn above and below each other (while touching) it should be
 * {@link TouchingPair#getVerticalPlacementVertex0()} == {@link VerticalPlacement#TOP},
 * {@link TouchingPair#getHorizontalPlacementVertex0()} == {@link HorizontalPlacement#FREE},
 * {@link TouchingPair#getVerticalPlacementVertex1()} == {@link VerticalPlacement#BOTTOM} and
 * {@link TouchingPair#getHorizontalPlacementVertex1()} == {@link HorizontalPlacement#FREE}.
 */
@JsonIgnoreProperties({ "vertices" })
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

    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(Arrays.asList(vertex0, vertex1));
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
     * equals & hashCode
     *
     * (should depend here only on contained objects and their order does not matter)
     *==========*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TouchingPair that = (TouchingPair) o;
        // sort vertices by hash code to compare the correct ones with each other
        Vertex thisVertexA = vertex0.hashCode() <= vertex1.hashCode() ? vertex0 : vertex1;
        Vertex thisVertexB = thisVertexA.equals(vertex0) ? vertex1 : vertex0;
        Vertex thatVertexA = that.vertex0.hashCode() <= that.vertex1.hashCode() ? that.vertex0 : that.vertex1;
        Vertex thatVertexB = thatVertexA.equals(that.vertex0) ? that.vertex1 : that.vertex0;
        return Objects.equals(thisVertexA, thatVertexA) && Objects.equals(thisVertexB, thatVertexB);
    }

    @Override
    public int hashCode() {
        // sort vertices by hash code to compare the correct ones with each other
        Vertex vertexA = vertex0.hashCode() <= vertex1.hashCode() ? vertex0 : vertex1;
        Vertex vertexB = vertexA.equals(vertex0) ? vertex1 : vertex0;
        return Objects.hash(vertexA, vertexB);
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return "[" + getVertex0() + "|" + getVertex1() + "]";
    }
}
