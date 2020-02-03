package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * This is the top-level class of the praline data structure containing all elements of a network.
 * The network represented by an instance of {@link Graph} may in practice e. g. be a circuit diagram or a computer
 * network of a company.
 * It contains a list of all vertices ({@link Vertex}) and a list of all edges ({@link Edge}) of this graph.
 * Note that an {@link Edge} connects usually two vertices, but it is also possible to connect more than two
 * vertices, so you may represent hypergraphs.
 *
 * Moreover, a {@link Graph} stores all {@link VertexGroup}s and {@link EdgeBundle}s of the network.
 * Note that these lists of {@link VertexGroup}s and {@link EdgeBundle}s should only contain the top-level elements,
 * i. e., {@link VertexGroup}s that are *not* contained in another {@link VertexGroup} and {@link EdgeBundle}s that
 * are *not* contained in another {@link EdgeBundle}.
 * Lower-level elements are contained hierarchically in higher-level elements.
 * {@link Port}s are contained in the vertices, {@link PortPairing}s are contained in {@link VertexGroup}s and
 * similar with other elements of the network.
 */
public class Graph {

    /*==========
     * Instance variables
     *==========*/

    private final List<Vertex> vertices;
    private final List<VertexGroup> vertexGroups;
    private final List<Edge> edges;
    private final List<EdgeBundle> edgeBundles;


    /*==========
     * Constructors
     *==========*/

    public Graph() {
        this(null, null, null, null);
    }

    public Graph(Collection<Vertex> vertices, Collection<Edge> edges) {
        this(vertices, null, edges, null);
    }

    /**
     * Set parameter to null if a {@link Graph} should be initialized without these objects (e.g. without edgeBundles)
     *
     * @param vertices
     *      should contain all vertices of this graph once -- regardless of whether they are in a (sub-)vertex group
     *      passed by the parameter {@link Graph#vertexGroups} or not
     * @param vertexGroups
     *      should form a tree structure (or better said forest structure), but this collection should only contain
     *      the top level elements (i.e., roots)
     * @param edges
     *      should contain all edges of this graph once -- regardless of whether they are in a (sub-)edge bundle
     *      passed by the parameter {@link Graph#edgeBundles} or not
     * @param edgeBundles
     *      should form a tree structure (or better said forest structure), but this collection should only contain
     *      the top level elements (i.e., roots)
     */
    @JsonCreator
    public Graph(
            @JsonProperty("vertices") final Collection<Vertex> vertices,
            @JsonProperty("vertexGroups") final Collection<VertexGroup> vertexGroups,
            @JsonProperty("edges") final Collection<Edge> edges,
            @JsonProperty("edgeBundles") final Collection<EdgeBundle> edgeBundles
    ) {
        this.vertices = newArrayListNullSafe(vertices);
        this.vertexGroups = newArrayListNullSafe(vertexGroups);
        this.edges = newArrayListNullSafe(edges);
        this.edgeBundles = newArrayListNullSafe(edgeBundles);
    }


    /*==========
     * Getters
     *==========*/

    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public List<VertexGroup> getVertexGroups() {
        return Collections.unmodifiableList(vertexGroups);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public List<EdgeBundle> getEdgeBundles() {
        return Collections.unmodifiableList(edgeBundles);
    }


    /*==========
     * Modifiers
     *==========*/
    
    public void addVertex(Vertex v) {
        vertices.add(v);
    }
    
    public boolean removeVertex(Vertex v) {
        return vertices.remove(v);
    }

    public void addVertexGroup(VertexGroup vg) {
        vertexGroups.add(vg);
    }

    public boolean removeVertexGroup(VertexGroup vg) {
        return vertexGroups.remove(vg);
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public boolean removeEdge(Edge e) {
        return edges.remove(e);
    }

    public void addEdgeBundle(EdgeBundle eb) {
        edgeBundles.add(eb);
    }

    public boolean removeEdgeBundle(EdgeBundle eb) {
        return edgeBundles.remove(eb);
    }

    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return "Graph{vertices:" + vertices + ", vertexGroups:" + vertexGroups + ", edges:" + edges + ", edgeBundles:"
                + edgeBundles + "}";
    }
}
