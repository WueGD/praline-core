package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;

import java.util.*;

public class LayerAssignment {

    private Collection<Edge> tree;
    private Map<Vertex, Collection<Edge>> treeNodes;
    private Map<Vertex, Integer> ranks;
    private SugiyamaLayouter sugy;
    private Set<Vertex> tailComponent;
    private Set<Vertex> headComponent;
    private SortingOrder orders;

    public LayerAssignment (SugiyamaLayouter sugy) {
        this.sugy = sugy;
        this.ranks = new LinkedHashMap<>();
        this.orders = sugy.getOrders();
        if (this.orders == null) {
            this.orders = new SortingOrder();
            sugy.setOrders(this.orders);
        }
    }

    /**
     C.  Network Simplex
     Here, we describe a simple approach to the problem based
     on  a network simplex formulation  [15].  Although its time
     complexity has not been proven polynomial, in practice it takes
     few iterations and runs quickly.
     calculateCutValues can be speed up by pre-calculating cutValues for all nodes if necessary
     */
    public Map<Vertex, Integer> networkSimplex () {
        //we only have one connected component, so execute it directly to the graph
        networkSimplexPerComponent(new LinkedHashSet<>(sugy.getGraph().getVertices()));

        assignRanksToOrders();

        return ranks;
    }

    private void networkSimplexPerComponent(Set<Vertex> component) {
        initializeRankAndTree(component);
        // find edge with negative cut value and replace it
        // do till no more such edges can be found
        while (true) {
            Edge negative = null;
            for (Edge edge : new LinkedList<>(tree)) {
                // remove edge from tree for calculateCutValue
                tree.remove(edge);
                treeNodes.get(sugy.getStartNode(edge)).remove(edge);
                treeNodes.get(sugy.getEndNode(edge)).remove(edge);
                // find edge with negative cutValue
                if (calculateCutValue(edge, component) < 0) {
                    negative = edge;
                    break;
                }
                // put edge with non-negative cutValue back into tree
                tree.add(edge);
                treeNodes.get(sugy.getStartNode(edge)).add(edge);
                treeNodes.get(sugy.getEndNode(edge)).add(edge);
            }
            // break if no edge with negative cutValue was found
            if (negative == null) break;
            // find alternative for the edge with negative cutValue and replace it
            replaceEdge();
        }
        //normalize
        //after this step the min rank is 0
        int minRank = Integer.MAX_VALUE;
        for (int rank : ranks.values()) {
            if (rank < minRank) minRank = rank;
        }
        for (Map.Entry<Vertex, Integer> entry : ranks.entrySet()) {
            entry.setValue(entry.getValue()-minRank);
        }
    }

    private void initializeRankAndTree(Set<Vertex> component) {
        this.tree = new LinkedHashSet<>();
        this.treeNodes = new LinkedHashMap<>();
        for (Vertex node : component) {
            treeNodes.put(node, new LinkedHashSet<>());
        }
        Map<Vertex, Collection<Edge>> edges = new LinkedHashMap<>();
        for (Vertex node : component) {
            edges.put(node, new LinkedHashSet<>(sugy.getIncomingEdges(node)));
        }
        List<Vertex> vertices = new LinkedList<>(component);
        int rank = 0;

        // iterate over all vertices
        while (!vertices.isEmpty()) {
            // find all vertices without incoming edges
            Set<Vertex> withoutIncomingEdge = new LinkedHashSet<>();
            for (Vertex node : vertices) {
                if (edges.get(node).isEmpty()) {
                    withoutIncomingEdge.add(node);
                }
            }
            // rank found vertices
            // delete all of their outgoing edges afterwards
            for (Vertex toRemove : withoutIncomingEdge) {
                for (Edge outEdge : sugy.getOutgoingEdges(toRemove)) {
                    edges.get(sugy.getEndNode(outEdge)).remove(outEdge);
                }
                ranks.put(toRemove,rank);

                //// start //// initialize tree ///////////
                // find one incoming edge from a node with rank one smaller
                // this edge is tight and therefore used as tree edge
                for (Edge inEdge : sugy.getIncomingEdges(toRemove)) {
                    if (ranks.get(sugy.getStartNode(inEdge)) == (rank - 1)) {
                        tree.add(inEdge);
                        treeNodes.get(sugy.getStartNode(inEdge)).add(inEdge);
                        treeNodes.get(sugy.getEndNode(inEdge)).add(inEdge);
                        break;
                    }
                }
                ///// end ///// initialize tree ///////////

                vertices.remove(toRemove);
            }
            // increase rank for next round
            rank++;
        }

        // some nodes of rank 0 may have not been connected to the tree because just incoming edges where considered
        // so the outgoing edge to the neighbouring node with lowest rank is computed and used for this purpose
        for (Vertex node : treeNodes.keySet()) {
            if (treeNodes.get(node).isEmpty()) {
                if (!sugy.getOutgoingEdges(node).isEmpty()) {
                    int smallestRank = Integer.MAX_VALUE;
                    Edge possibleTreeEdge = null;
                    for (Edge outEdge : sugy.getOutgoingEdges(node)) {
                        if (smallestRank > ranks.get(sugy.getEndNode(outEdge))) {
                            smallestRank = ranks.get(sugy.getEndNode(outEdge));
                            possibleTreeEdge = outEdge;
                        }
                    }
                    ranks.replace(node, (smallestRank - 1));
                    if (possibleTreeEdge != null) {
                        tree.add(possibleTreeEdge);
                        treeNodes.get(node).add(possibleTreeEdge);
                        treeNodes.get(sugy.getEndNode(possibleTreeEdge)).add(possibleTreeEdge);
                    }
                }
            }
        }

        // if the tree is not connected
        while (tree.size() != (treeNodes.size() - 1)) {
            Set<Vertex> connectedComponent = new LinkedHashSet<>();
            if (component.isEmpty()) {
                System.out.println("Warning! No vertex found in (a component of) graph " + sugy.getGraph() +". Abort " +
                        "layer assignment.");
                return;
            }
            Vertex firstNode = component.iterator().next();
            connectedComponent.add(firstNode);
            // find a connected component
            findConnectedComponent(connectedComponent, firstNode);
            boolean connected = false;
            // find an edge to a not contained node and add it to the tree
            // note this edge is probably not tight but there can max be one per node with rank 0
            // todo: if nevertheless runtime critical change to try to find tight edges
            for (Vertex node : connectedComponent) {
                for (Edge edge : sugy.getOutgoingEdges(node)) {
                    if (!connectedComponent.contains(sugy.getEndNode(edge))) {
                        tree.add(edge);
                        treeNodes.get(sugy.getEndNode(edge)).add(edge);
                        treeNodes.get(node).add(edge);
                        connected = true;
                        break;
                    }
                }
                if (connected) break;
                for (Edge edge : sugy.getIncomingEdges(node)) {
                    if (!connectedComponent.contains(sugy.getStartNode(edge))) {
                        tree.add(edge);
                        treeNodes.get(sugy.getStartNode(edge)).add(edge);
                        treeNodes.get(node).add(edge);
                        connected = true;
                        break;
                    }
                }
                if (connected) break;
            }
        }
    }

    private void findConnectedComponent (Set<Vertex> connectedComponent, Vertex node) {
        for (Edge edge : treeNodes.get(node)) {
            Vertex nextNode = sugy.getStartNode(edge);
            if (nextNode.equals(node)) nextNode = sugy.getEndNode(edge);
            if (!connectedComponent.contains(nextNode)) {
                connectedComponent.add(nextNode);
                findConnectedComponent(connectedComponent, nextNode);
            }
        }
    }

    // calculates the cut value of an given edge
    // edge must be already removed from the tree so that the tree is split into two components

    private int calculateCutValue (Edge toCheck, Set<Vertex> component) {

        // initialize the two components of the tree new by using a simple bfs
        tailComponent = new LinkedHashSet<>(component);
        headComponent = new LinkedHashSet<>();

        // start bfs //
        LinkedList<Vertex> queue = new LinkedList<>();
        queue.add(sugy.getEndNode(toCheck));
        while (!queue.isEmpty()) {
            Vertex current = queue.removeFirst();
            headComponent.add(current);
            tailComponent.remove(current);
            for (Edge edge : treeNodes.get(current)) {
                Vertex neighbour = sugy.getStartNode(edge);
                if (neighbour.equals(current)) {
                    neighbour = sugy.getEndNode(edge);
                }
                if (!headComponent.contains(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        /// end bfs ///

        // calculate cut value by summing up all outgoing edges of the tailComponent minus all incoming edges
        // edges from head to head or tail to tail are ignored
        int cutValue = 0;
        for (Edge edge : sugy.getGraph().getEdges()) {
            if (tailComponent.contains(sugy.getStartNode(edge))) {
                if (headComponent.contains(sugy.getEndNode(edge))) {
                    cutValue++;
                }
            } else if (tailComponent.contains(sugy.getEndNode(edge))) {
                cutValue--;
            }
        }
        return cutValue;
    }
    // finds an alternative edge for a precalculated one with negative cutValue
    // replaces the negative edge with the new one and adjusts the ranks
    // just works if tailComponent and headComponent are already set e.g. by calculateCutValue()

    private void replaceEdge () {
        Edge newTreeEdge = null;
        int minSlack = Integer.MAX_VALUE;
        for (Edge candidateEdge : sugy.getGraph().getEdges()) {
            // check whether edge goes in the right direction and therefore is an alternative
            if (headComponent.contains(sugy.getStartNode(candidateEdge)) && tailComponent.contains(sugy.getEndNode(candidateEdge))) {
                // calculate the slack of the Edge
                int slack = (ranks.get(sugy.getEndNode(candidateEdge)) - ranks.get(sugy.getStartNode(candidateEdge)));
                // find edge with minimum slack
                if (slack < minSlack) {
                    minSlack = slack;
                    newTreeEdge = candidateEdge;
                }
            }
        }
        // replace edge
        // old edge is still removed so just adding of the new edge has to be done
        tree.add(newTreeEdge);
        treeNodes.get(sugy.getStartNode(newTreeEdge)).add(newTreeEdge);
        treeNodes.get(sugy.getEndNode(newTreeEdge)).add(newTreeEdge);
        // adjust ranks
        for (Vertex headNode : headComponent) {
            int newRank = (ranks.get(headNode) + minSlack - 1);
            ranks.replace(headNode,newRank);
        }
    }

    private void assignRanksToOrders() {
        int maxRank = 0;
        for (Integer rank : ranks.values()) {
            maxRank = Math.max(maxRank, rank);
        }

        //init lists of node order
        for (int i = 0; i <= maxRank; i++) {
            orders.getNodeOrder().add(new ArrayList<>());
        }


        for (Vertex vertex : ranks.keySet()) {
            int layer = ranks.get(vertex);
            orders.getNodeOrder().get(layer).add(vertex);
        }
    }
}
