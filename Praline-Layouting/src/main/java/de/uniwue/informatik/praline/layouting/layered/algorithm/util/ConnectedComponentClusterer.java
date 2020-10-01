package de.uniwue.informatik.praline.layouting.layered.algorithm.util;

import de.uniwue.informatik.praline.datastructure.graphs.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class ConnectedComponentClusterer {

    private Graph graph;

    /**
     * It cannot handle port pairings + vertex groups!
     *
     * replace them by union nodes in the forehand!
     *
     * @param graph
     */
    public ConnectedComponentClusterer(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    /**
     * It cannot handle port pairings + vertex groups!
     *
     * replace them by union nodes in the forehand!
     *
     */
    public Set<Set<Vertex>> getConnectedComponents() {
        Set<Set<Vertex>> allConnectedComponents = new LinkedHashSet<>();
        Set<Vertex> vertices = new LinkedHashSet<>(getGraph().getVertices());

        while (!vertices.isEmpty()) {
            // find next connected component
            Vertex node = vertices.iterator().next();
            Set<Vertex> connectedComponent = new LinkedHashSet<>();
            computeConnectedComponentRecursive(node, connectedComponent);
            for (Vertex n : connectedComponent) {
                vertices.remove(n);
            }
            allConnectedComponents.add(connectedComponent);
        }
        return allConnectedComponents;
    }

    private void computeConnectedComponentRecursive (Vertex node, Set<Vertex> connectedComponent) {
        if (!connectedComponent.contains(node)) {
            connectedComponent.add(node);
            for (PortComposition portComposition : node.getPortCompositions()) {
                computeConnectedComponentRecursive(portComposition, connectedComponent);
            }
        }
    }

    private void computeConnectedComponentRecursive (PortComposition portComposition, Set<Vertex> connectedComponent) {
        if (portComposition instanceof PortGroup) {
            for (PortComposition groupMember : ((PortGroup) portComposition).getPortCompositions()) {
                computeConnectedComponentRecursive(groupMember, connectedComponent);
            }
        } else if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                for (Port port : edge.getPorts()) {
                    if (!connectedComponent.contains(port.getVertex())) {
                        computeConnectedComponentRecursive(port.getVertex(), connectedComponent);
                    }
                }
            }
        }
    }
}
