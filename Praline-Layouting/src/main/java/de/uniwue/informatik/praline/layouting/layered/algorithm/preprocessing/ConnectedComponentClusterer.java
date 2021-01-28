package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;

import java.util.*;

public class ConnectedComponentClusterer {

    private Graph graph;

    public ConnectedComponentClusterer(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    /**
     * Note: vertices in the same {@link VertexGroup} will be in the same component, even if there is no {@link Edge}
     * , {@link PortPairing} or {@link TouchingPair} between them.
     *
     * @return
     *      A new graph object for each component in a list which is sorted by the number of vertices in ascending order
     */
    public List<Graph> getConnectedComponentsBySize() {
        Set<Set<Vertex>> componentVertices = getConnectedComponents();

        List<Graph> componentGraphs = new ArrayList<>(componentVertices.size());

        for (Set<Vertex> componentVertexSet : componentVertices) {
            componentGraphs.add(getComponentGraph(componentVertexSet));
        }

        componentGraphs.sort(Comparator.comparingInt(g -> g.getVertices().size()));

        return componentGraphs;
    }

    private Graph getComponentGraph(Set<Vertex> componentVertexSet) {
        Set<Edge> componentEdgeSet = new LinkedHashSet<>();
        Set<VertexGroup> componentVertexGroupSet = new LinkedHashSet<>();
        Set<EdgeBundle> componentEdgeBundleSet = new LinkedHashSet<>();

        for (Vertex vertex : componentVertexSet) {
            componentEdgeSet.addAll(PortUtils.getEdges(vertex));
            componentVertexGroupSet.add(PortUtils.getTopLevelVertexGroup(vertex));
        }

        for (Edge edge : componentEdgeSet) {
            componentEdgeBundleSet.add(PortUtils.getTopLevelEdgeBundle(edge));
        }

        return new Graph(componentVertexSet, componentVertexGroupSet, componentEdgeSet, componentEdgeBundleSet);
    }

    private Set<Set<Vertex>> getConnectedComponents() {
        Set<Set<Vertex>> allConnectedComponents = new LinkedHashSet<>();
        Set<Vertex> vertices = new LinkedHashSet<>(getGraph().getVertices());

        while (!vertices.isEmpty()) {
            // find next connected component
            Vertex node = vertices.iterator().next();
            Set<Vertex> connectedComponent = new LinkedHashSet<>();
            computeConnectedComponentRecursively(node, connectedComponent);
            for (Vertex n : connectedComponent) {
                vertices.remove(n);
            }
            allConnectedComponents.add(connectedComponent);
        }
        return allConnectedComponents;
    }

    private void computeConnectedComponentRecursively(Vertex node, Set<Vertex> connectedComponent) {
        if (!connectedComponent.contains(node)) {
            connectedComponent.add(node);
            //find adjacent vertices via edges at ports
            for (PortComposition portComposition : node.getPortCompositions()) {
                computeConnectedComponentRecursively(portComposition, connectedComponent);
            }
            //find adjacent vertices via vertex groups (go all levels up)
            VertexGroup currentVertexGroup = node.getVertexGroup();
            while (currentVertexGroup != null) {
                for (Vertex nodeInTheSameGroup : currentVertexGroup.getContainedVertices()) {
                    computeConnectedComponentRecursively(nodeInTheSameGroup, connectedComponent);
                }

                currentVertexGroup = currentVertexGroup.getVertexGroup();
            }
        }
    }

    private void computeConnectedComponentRecursively(PortComposition portComposition, Set<Vertex> connectedComponent) {
        if (portComposition instanceof PortGroup) {
            for (PortComposition groupMember : ((PortGroup) portComposition).getPortCompositions()) {
                computeConnectedComponentRecursively(groupMember, connectedComponent);
            }
        } else if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                for (Port port : edge.getPorts()) {
                    if (!connectedComponent.contains(port.getVertex())) {
                        computeConnectedComponentRecursively(port.getVertex(), connectedComponent);
                    }
                }
            }
        }
    }
}
