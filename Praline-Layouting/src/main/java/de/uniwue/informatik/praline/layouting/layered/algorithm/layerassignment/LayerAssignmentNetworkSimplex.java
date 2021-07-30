package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;

import java.util.*;
import java.util.stream.Collectors;

public class LayerAssignmentNetworkSimplex implements LayerAssignment {

    private final DirectionAssignment fdBaseDirectionAssignment;
    private final SugiyamaLayouter sugy;
    private SortingOrder orders;
    private final Map<Vertex, Integer> ranks = new LinkedHashMap<>();
    private Map<Edge, Integer> tree2cutValue;

    public LayerAssignmentNetworkSimplex(SugiyamaLayouter sugy, DirectionAssignment fdBaseDirectionAssignment) {
        this.sugy = sugy;
        this.fdBaseDirectionAssignment = fdBaseDirectionAssignment;
        this.orders = sugy.getOrders();
        if (this.orders == null) {
            this.orders = new SortingOrder();
            sugy.setOrders(this.orders);
        }
    }

    @Override
    public Map<Vertex, Integer> assignLayers() {
        //we only have one connected component, so execute it directly to the graph
        networkSimplexPerComponent();

        return ranks;
    }

    /**
     * see https://cs.brown.edu/people/rtamassi/gdhandbook/chapters/hierarchical.pdf
     *
     * and Gansner, Koutsofios, North, Vo: "A technique for drawing directed graphs"
     * https://ieeexplore.ieee.org/document/221135
     */
    private void networkSimplexPerComponent() {
        //special case: if it is just one vertex, we set its rank to 0 and return
        if (sugy.getGraph().getVertices().size() <= 1) {
            for (Vertex vertex : sugy.getGraph().getVertices()) {
                ranks.put(vertex, 0);
            }
            computeInitialOrder();
            return;
        }

        //regular case
        getTree(); //list of edges of the spanning tree + set cut values
        LinkedList<Edge> treeEdgesByCutValue = new LinkedList<>(tree2cutValue.keySet());
        treeEdgesByCutValue.sort(Comparator.comparingInt(tree2cutValue::get));

        Edge e = leaveEdge(treeEdgesByCutValue);
        while (e != null) {
            Edge f = enterEdge(e);
            exchange(e, f);

            e = leaveEdge(treeEdgesByCutValue);
        }

        normalize();
        computeInitialOrder();
//        balance(); TODO: commented out because it did not bring improvements :( maybe revisit later
        sortLayers();
    }

    private void normalize() {
        int minRank = ranks.values().stream().min(Integer::compareTo).get();
        //subtract minRank from everything -> we start at 0
        for (Vertex v : ranks.keySet()) {
            ranks.replace(v, ranks.get(v) - minRank);
        }
    }

    private void exchange(Edge e, Edge f) {
        //first update the cut values of the neighbors of e. Only its descendants are affected
        int cutValueE = tree2cutValue.get(e);
        for (Edge descendant : sugy.getOutgoingEdges(sugy.getEndNode(e))) {
            if (isTreeEdge(descendant)) {
                Integer cutValueDescendant = tree2cutValue.get(descendant);
                tree2cutValue.replace(descendant, cutValueDescendant - cutValueE);
            }
        }
        //remove e from the tree
        tree2cutValue.remove(e);
        //add f to the tree
        tree2cutValue.put(f, Integer.MIN_VALUE);
        //update all cut values that are the cut value of the new edge f and the cut values of the edges in the top tree
        //which we have to this end made invalid in enterEdge().
        //the cut values can only increase, hence we don't need to re-sort the list treeEdgesByCutValue TODO: do we
        // have to compute them then at all?!
        for (Edge treeEdge : tree2cutValue.keySet()) {
            if (tree2cutValue.get(treeEdge) == Integer.MIN_VALUE) {
                assignCutValue(treeEdge);
            }
        }
    }

    private Edge enterEdge(Edge e) {
        //first find the partition of vertices by a BFS
        Set<Vertex> treeBottom = new LinkedHashSet<>();
        LinkedList<Vertex> queueBottom = new LinkedList<>();
        Set<Vertex> treeTop = new LinkedHashSet<>();
        LinkedList<Vertex> queueTop = new LinkedList<>();
        ArrayList<Edge> candidateInterClusterEdges = new ArrayList<>();

        treeBottom.add(sugy.getStartNode(e));
        queueBottom.add(sugy.getStartNode(e));
        treeTop.add(sugy.getEndNode(e));
        queueTop.add(sugy.getEndNode(e));

        //bfs for bottom tree
        bfsForSubTree(treeBottom, queueBottom, candidateInterClusterEdges, e);
        //bfs for top tree
        Collection<Edge> treeEdgesTopTree = bfsForSubTree(treeTop, queueTop, candidateInterClusterEdges, e);

        //go through all edge candidates and
        // 1) check if they are really inter-cluster edges (initially we may have added too many)
        // 2) of them find the one with minimum slack
        int minSlack = Integer.MAX_VALUE;
        Edge candidateWithMinSlack = null;
        for (Edge candidate : candidateInterClusterEdges) {
            // 1)
            if (!treeBottom.containsAll(candidate.getPorts().stream().map(Port::getVertex).collect(Collectors.toList()))
                    && !treeTop.containsAll(candidate.getPorts().stream().map(Port::getVertex).collect(
                            Collectors.toList()))) {
                // 2)
                int slack = getSlack(candidate);
                if (slack < minSlack) {
                    minSlack = slack;
                    candidateWithMinSlack = candidate;
                }
            }
        }

        //make all cut values of the vertices in the top tree invalid
        for (Edge topTreeEdge : treeEdgesTopTree) {
            tree2cutValue.replace(topTreeEdge, Integer.MIN_VALUE);
        }

        return candidateWithMinSlack;
    }

    private Collection<Edge> bfsForSubTree(Set<Vertex> treeVertices, LinkedList<Vertex> vertexQueue,
                               ArrayList<Edge> candidateInterClusterEdges, Edge removedE) {
        Set<Edge> treeEdges = new LinkedHashSet<>();
        while (!vertexQueue.isEmpty()) {
            Vertex v = vertexQueue.poll();
            for (Edge e : PortUtils.getEdges(v)) {
                if (!e.equals(removedE)) {
                    Vertex otherEndPoint = PortUtils.getOtherEndPoint(e, v).getVertex();
                    if (isTreeEdge(e) && !treeEdges.contains(e)) {
                        treeEdges.add(e);
                        treeVertices.add(otherEndPoint);
                        vertexQueue.add(otherEndPoint);
                    } else if (!treeVertices.contains(otherEndPoint)) {
                        candidateInterClusterEdges.add(e);
                    }
                }
            }
        }
        return treeEdges;
    }

    /**
     * If an edge is chosen, it is also removed from treeEdgesByCutValue
     *
     * @param treeEdgesByCutValue
     * @return
     */
    private Edge leaveEdge(LinkedList<Edge> treeEdgesByCutValue) {
        Edge e = treeEdgesByCutValue.getFirst(); //edge with lowest cut value
        if (tree2cutValue.get(e) < 0) {
            treeEdgesByCutValue.removeFirst();
            return e;
        }
        return null;
    }

    private void getTree() {
        Vertex startVertex = initRank();

        int n = sugy.getGraph().getVertices().size();
        tree2cutValue = new LinkedHashMap<>();
        Set<Vertex> treeVertices = new LinkedHashSet<>(n);

        while (tightTree(treeVertices, startVertex) < n) {
            Edge e = findNonTreeEdgeWithMinimalSlack(treeVertices);
            int delta = getSlack(e) * (treeVertices.contains(sugy.getStartNode(e)) ? 1 : -1);
            for (Vertex treeVertex : treeVertices) {
                ranks.replace(treeVertex, ranks.get(treeVertex) + delta);
            }
        }

        initCutValues();
    }

    private void initCutValues() {
        for (Edge treeEdge : tree2cutValue.keySet()) {
            Integer cutValue = tree2cutValue.get(treeEdge);
            if (cutValue == Integer.MIN_VALUE) {
                assignCutValue(treeEdge);
            }
        }
    }

    private void assignCutValue(Edge treeEdge) {
        Vertex lowerEndpoint = sugy.getStartNode(treeEdge);
        Collection<Edge> predecessors = sugy.getIncomingEdges(lowerEndpoint);
        //make sure the predecessors in the tree have their cut value assigned
        List<Edge> treePredecessors = new ArrayList<>();
        for (Edge predecessor : predecessors) {
            if (isTreeEdge(predecessor)) {
                treePredecessors.add(predecessor);
                if (tree2cutValue.get(predecessor) == Integer.MIN_VALUE) {
                    assignCutValue(predecessor);
                }
            }
        }
        //now that all predecessors got a cut value (recursively) we proceed by computing it
        int cutValue =
                1 - treePredecessors.size() + treePredecessors.stream().mapToInt(tree2cutValue::get).sum()
                - (int) sugy.getIncomingEdges(lowerEndpoint).stream().filter(e -> !treePredecessors.contains(e)).count()
                + (int) sugy.getOutgoingEdges(lowerEndpoint).stream().filter(e -> !treePredecessors.contains(e)).count();
        tree2cutValue.replace(treeEdge, cutValue);
    }

    /**
     * just a longest-path layering
     */
    private Vertex initRank() {
        Map<Vertex, Integer> incomingEdges = new LinkedHashMap<>(sugy.getGraph().getVertices().size());

        //vertices without incoming edges (after removing edges
        // incident to vertices with an assigned rank)
        LinkedList<Vertex> queue = new LinkedList<>();

        for (Vertex vertex : sugy.getGraph().getVertices()) {
            int inDeg = sugy.getIncomingEdges(vertex).size();
            if (inDeg == 0) {
                queue.add(vertex);
            } else {
                incomingEdges.put(vertex, inDeg);
            }
        }

        Vertex startVertex = queue.get(0);
        while (!queue.isEmpty()) {
            Vertex vertex = queue.poll();
            int rank = 0;
            //new rank is rank + 1 of its uppermost predecessor
            for (Edge incomingEdge : sugy.getIncomingEdges(vertex)) {
                Vertex predecessor = PortUtils.getOtherEndPoint(incomingEdge, vertex).getVertex();
                rank = Math.max(rank, ranks.get(predecessor) + 1);

            }
            ranks.put(vertex, rank);

            //remove outgoing edges
            for (Edge outgoingEdge : sugy.getOutgoingEdges(vertex)) {
                Vertex successor = PortUtils.getOtherEndPoint(outgoingEdge, vertex).getVertex();
                Integer remainingInDegSuccessor = incomingEdges.get(successor);
                if (remainingInDegSuccessor == 1) {
                    //now the inDeg will be 0 -> add to queue
                    queue.add(successor);
                } else {
                    //otherwise the inDeg > 0 but is decreased by 1 now
                    incomingEdges.replace(successor, remainingInDegSuccessor - 1);
                }
            }
        }

        return startVertex;
    }

    private int tightTree(Set<Vertex> treeVertices, Vertex startVertex) {

        if (treeVertices.isEmpty()) {
            treeVertices.add(startVertex);
        }

        LinkedList<Vertex> queue = new LinkedList<>(treeVertices);
        while (!queue.isEmpty()) {
            Vertex treeVertex = queue.poll();
            for (Edge edge : PortUtils.getEdges(treeVertex)) {
                Vertex otherEndPoint = PortUtils.getOtherEndPoint(edge, treeVertex).getVertex();
                if (!treeVertices.contains(otherEndPoint) && isTight(edge)) {
                    tree2cutValue.put(edge, Integer.MIN_VALUE);
                    treeVertices.add(otherEndPoint);
                    queue.add(otherEndPoint);
                }
            }
        }

        return treeVertices.size();
    }

    private boolean isTight(Edge edge) {
        return getSlack(edge) == 0;
    }

    private int getSlack(Edge edge) {
        List<Port> ports = edge.getPorts();
        Integer rank0 = ranks.get(ports.get(0).getVertex());
        Integer rank1 = ranks.get(ports.get(1).getVertex());
        return Math.max(rank0, rank1) - Math.min(rank0, rank1) - 1;
    }

    private Edge findNonTreeEdgeWithMinimalSlack(Set<Vertex> treeVertices) {
        Edge bestEdge = null;
        int slackBestEdge = Integer.MAX_VALUE;

        for (Edge edge : sugy.getGraph().getEdges()) {
            //one but not both end points are in the tree
            if (!treeVertices.containsAll(PortUtils.getIncidentVertices(edge)) &&
                    (treeVertices.contains(sugy.getStartNode(edge)) || treeVertices.contains(sugy.getEndNode(edge)))) {
                int slack = getSlack(edge);

                //special case, since we don't have tight edges any more, 1 is the best we can achieve
                if (slack == 1) {
                    return edge;
                }

                if (slack < slackBestEdge) {
                    slackBestEdge = slack;
                    bestEdge = edge;
                }
            }
        }

        return bestEdge;
    }

    private boolean isTreeEdge(Edge e) {
        return tree2cutValue.containsKey(e);
    }

    private void computeInitialOrder() {
        List<List<Vertex>> nodeOrder = this.orders.getNodeOrder();
        int maxRank = ranks.values().stream().max(Integer::compareTo).get();
        //init maxRank + 1 many empty layers
        for (int i = 0; i <= maxRank; i++) {
            nodeOrder.add(new ArrayList<>());
        }
        //fill layers
        for (Vertex v : ranks.keySet()) {
            nodeOrder.get(ranks.get(v)).add(v);
        }
    }

    private void balance() {
        //as proposed by Gansner et al.: move vertices with equal in- and out- degree to less crowded reachable layers
        List<List<Vertex>> nodeOrder = this.orders.getNodeOrder();
        for (Vertex v : sugy.getGraph().getVertices()) {
            Collection<Edge> incomingEdges = sugy.getIncomingEdges(v);
            Collection<Edge> outgoingEdges = sugy.getOutgoingEdges(v);
            if (incomingEdges.size() == outgoingEdges.size()) {
                int minInSlack = incomingEdges.stream().mapToInt(this::getSlack).min().getAsInt();
                int minOutSlack = outgoingEdges.stream().mapToInt(this::getSlack).min().getAsInt();
                //find layer with smallest number of vertices that is available for v
                Integer currentRank = ranks.get(v);
                int sparsestLayer = currentRank;
                int sparsestLayerSize = nodeOrder.get(currentRank).size();
                for (int i = currentRank - minInSlack; i <= currentRank + minOutSlack; i++) {
                    if (nodeOrder.get(i).size() < sparsestLayerSize) {
                        sparsestLayer = i;
                        sparsestLayerSize = nodeOrder.get(i).size();
                    }
                }
                //move to sparsest layer
                if (sparsestLayer != currentRank) {
                    nodeOrder.get(currentRank).remove(v);
                    nodeOrder.get(sparsestLayer).add(v);
                    ranks.replace(v, sparsestLayer);
                }
            }
        }
    }

    private void sortLayers() {
        if (fdBaseDirectionAssignment != null && fdBaseDirectionAssignment.bestFDLayout != null
                && fdBaseDirectionAssignment.nodeToLongBestFDLayout != null) {
            //sort each layer additionally by x-coordinate in the fd-layout
            for (List<Vertex> layer : this.orders.getNodeOrder()) {
                layer.sort(Comparator.comparingDouble(v -> fdBaseDirectionAssignment.bestFDLayout.apply(
                        fdBaseDirectionAssignment.nodeToLongBestFDLayout.get(v)).getX()));
            }
        }
    }
}