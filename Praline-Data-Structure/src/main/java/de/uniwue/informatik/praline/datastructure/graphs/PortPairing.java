package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A {@link PortPairing} represents a coupling between two {@link Port}s of the same {@link Vertex} or of different
 * vertices in the way that they appear on the same vertical or horizontal line.
 * {@link PortPairing}s are stored in {@link VertexGroup}s.
 * The concrete realization depends on the algorithm.
 * The user should take care that such a {@link PortPairing} is realizable and consistent.
 * E. g. if there is a {@link PortPairing} of two {@link Port}s at the same {@link Vertex}, they should end up on
 * different sides of the {@link Vertex} so they should not be in the same {@link PortGroup}.
 * If there is a {@link PortPairing} of two {@link Port}s of two different {@link Vertex}es, they should be in the
 * same {@link VertexGroup} (precisely the {@link VertexGroup} this {@link PortPairing} is stored in) and there
 * should be a {@link TouchingPair} between these two {@link Vertex}es stored in the same {@link VertexGroup}.
 */
@JsonIgnoreProperties({ "ports" })
public class PortPairing {

    /*==========
     * Instance variables
     *==========*/

    private Port port0;
    private Port port1;


    /*==========
     * Constructors
     *==========*/

    @JsonCreator
    public PortPairing(
            @JsonProperty("port0") final Port port0,
            @JsonProperty("port1") final Port port1
    ) {
        this.port0 = port0;
        this.port1 = port1;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public Port getPort0() {
        return port0;
    }

    public void setPort0(Port port0) {
        this.port0 = port0;
    }

    public Port getPort1() {
        return port1;
    }

    public void setPort1(Port port1) {
        this.port1 = port1;
    }

    public List<Port> getPorts() {
        return Collections.unmodifiableList(Arrays.asList(port0, port1));
    }

    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return "[" + getPort0() + "--" + getPort1() + "]";
    }
}
