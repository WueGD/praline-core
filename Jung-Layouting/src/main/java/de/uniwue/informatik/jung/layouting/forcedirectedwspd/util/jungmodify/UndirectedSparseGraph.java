package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify;

/**
 * class copied from Jung, but replaced {@link java.util.HashMap} by {@link java.util.LinkedHashMap}
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
import com.google.common.base.Supplier;
import edu.uci.ics.jung.graph.AbstractTypedGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

public class UndirectedSparseGraph<V, E> extends AbstractTypedGraph<V, E> implements UndirectedGraph<V, E> {
    protected Map<V, Map<V, E>> vertices = new LinkedHashMap();
    protected Map<E, Pair<V>> edges = new LinkedHashMap();

    public static <V, E> Supplier<UndirectedGraph<V, E>> getFactory() {
        return new Supplier<UndirectedGraph<V, E>>() {
            public UndirectedGraph<V, E> get() {
                return new edu.uci.ics.jung.graph.UndirectedSparseGraph();
            }
        };
    }

    public UndirectedSparseGraph() {
        super(EdgeType.UNDIRECTED);
    }

    public boolean addEdge(E edge, Pair<? extends V> endpoints, EdgeType edgeType) {
        this.validateEdgeType(edgeType);
        Pair<V> new_endpoints = this.getValidatedEndpoints(edge, endpoints);
        if (new_endpoints == null) {
            return false;
        } else {
            V v1 = new_endpoints.getFirst();
            V v2 = new_endpoints.getSecond();
            if (this.findEdge(v1, v2) != null) {
                return false;
            } else {
                this.edges.put(edge, new_endpoints);
                if (!this.vertices.containsKey(v1)) {
                    this.addVertex(v1);
                }

                if (!this.vertices.containsKey(v2)) {
                    this.addVertex(v2);
                }

                ((Map)this.vertices.get(v1)).put(v2, edge);
                ((Map)this.vertices.get(v2)).put(v1, edge);
                return true;
            }
        }
    }

    public Collection<E> getInEdges(V vertex) {
        return this.getIncidentEdges(vertex);
    }

    public Collection<E> getOutEdges(V vertex) {
        return this.getIncidentEdges(vertex);
    }

    public Collection<V> getPredecessors(V vertex) {
        return this.getNeighbors(vertex);
    }

    public Collection<V> getSuccessors(V vertex) {
        return this.getNeighbors(vertex);
    }

    public E findEdge(V v1, V v2) {
        return this.containsVertex(v1) && this.containsVertex(v2) ? (E) ((Map)this.vertices.get(v1)).get(v2) : null;
    }

    public Collection<E> findEdgeSet(V v1, V v2) {
        if (this.containsVertex(v1) && this.containsVertex(v2)) {
            ArrayList<E> edge_collection = new ArrayList(1);
            E e = this.findEdge(v1, v2);
            if (e == null) {
                return edge_collection;
            } else {
                edge_collection.add(e);
                return edge_collection;
            }
        } else {
            return null;
        }
    }

    public Pair<V> getEndpoints(E edge) {
        return (Pair)this.edges.get(edge);
    }

    public V getSource(E directed_edge) {
        return null;
    }

    public V getDest(E directed_edge) {
        return null;
    }

    public boolean isSource(V vertex, E edge) {
        return false;
    }

    public boolean isDest(V vertex, E edge) {
        return false;
    }

    public Collection<E> getEdges() {
        return Collections.unmodifiableCollection(this.edges.keySet());
    }

    public Collection<V> getVertices() {
        return Collections.unmodifiableCollection(this.vertices.keySet());
    }

    public boolean containsVertex(V vertex) {
        return this.vertices.containsKey(vertex);
    }

    public boolean containsEdge(E edge) {
        return this.edges.containsKey(edge);
    }

    public int getEdgeCount() {
        return this.edges.size();
    }

    public int getVertexCount() {
        return this.vertices.size();
    }

    public Collection<V> getNeighbors(V vertex) {
        return !this.containsVertex(vertex) ? null : Collections.unmodifiableCollection(((Map)this.vertices.get(vertex)).keySet());
    }

    public Collection<E> getIncidentEdges(V vertex) {
        return !this.containsVertex(vertex) ? null : Collections.unmodifiableCollection(((Map)this.vertices.get(vertex)).values());
    }

    public boolean addVertex(V vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("vertex may not be null");
        } else if (!this.containsVertex(vertex)) {
            this.vertices.put(vertex, new LinkedHashMap());
            return true;
        } else {
            return false;
        }
    }

    public boolean removeVertex(V vertex) {
        if (!this.containsVertex(vertex)) {
            return false;
        } else {
            Iterator var2 = (new ArrayList(((Map)this.vertices.get(vertex)).values())).iterator();

            while(var2.hasNext()) {
                E edge = (E) var2.next();
                this.removeEdge(edge);
            }

            this.vertices.remove(vertex);
            return true;
        }
    }

    public boolean removeEdge(E edge) {
        if (!this.containsEdge(edge)) {
            return false;
        } else {
            Pair<V> endpoints = this.getEndpoints(edge);
            V v1 = endpoints.getFirst();
            V v2 = endpoints.getSecond();
            ((Map)this.vertices.get(v1)).remove(v2);
            ((Map)this.vertices.get(v2)).remove(v1);
            this.edges.remove(edge);
            return true;
        }
    }
}
