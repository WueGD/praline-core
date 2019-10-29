package de.uniwue.informatik.praline.datastructure.graphs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSave;

public class Graph {

    /*==========
     * Instance variables
     *==========*/

    private List<Vertex> vertices;
    private List<VertexGroup> vertexGroups;
    private List<Edge> edges;
    private List<EdgeBundle> edgeBundles;


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
     * @param vertexGroups
     * @param edges
     * @param edgeBundles
     */
    public Graph(Collection<Vertex> vertices, Collection<VertexGroup> vertexGroups, Collection<Edge> edges,
                 Collection<EdgeBundle> edgeBundles) {
        this.vertices = newArrayListNullSave(vertices);
        this.vertexGroups = newArrayListNullSave(vertexGroups);
        this.edges = newArrayListNullSave(edges);
        this.edgeBundles = newArrayListNullSave(edgeBundles);
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
}
