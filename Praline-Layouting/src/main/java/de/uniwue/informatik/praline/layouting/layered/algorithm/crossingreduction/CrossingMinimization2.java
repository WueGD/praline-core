package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.ConnectedComponentClusterer;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import edu.uci.ics.jung.graph.util.Pair;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;

import java.util.*;

/**
 * JZ 2020/06/06:
 * copied from original class CrossingMinimization to remove duplicate code,
 * add more variants and handle mixed cases of vertices with some but not all ports being in a port pairing.
 *
 * In the meantime it became a completely new class.
 * Hardly anything similar to its original class.
 */
public class CrossingMinimization2 {

    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.MIXED;
    public static final boolean DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE = true;
    public static final boolean DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX = true;

    private SugiyamaLayouter sugy;
    private SortingOrder orders;
    private LinkedHashSet<Vertex> adjacentToDummyTurningPoints;
    private LinkedHashMap<Vertex, SortingNode> vertex2sortingNode;
    private LinkedHashMap<Port, SortingNode> port2SortingNode;
    private Map<SortingNode, Double> currentValues;
    private int maxRank;
    private int numberOfCrossings;
    private CrossingMinimizationMethod method;
    private boolean movePortsAdjToTurningDummiesToTheOutside;
    private boolean placeTurningDummiesNextToTheirVertex;

    public CrossingMinimization2(SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    public SortingOrder layerSweepWithBarycenterHeuristic (CrossingMinimizationMethod method, SortingOrder orders) {
        return layerSweepWithBarycenterHeuristic(null, method, orders,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic (List<List<Vertex>> currentNodeOrderInput,
                                                           CrossingMinimizationMethod method, SortingOrder orders) {
        return layerSweepWithBarycenterHeuristic(currentNodeOrderInput, method, orders,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic (List<List<Vertex>> currentNodeOrderInput,
                                                           CrossingMinimizationMethod method,
                                                           SortingOrder inputOrders,
                                                           boolean movePortsAdjToTurningDummiesToTheOutside,
                                                           boolean placeTurningDummiesNextToTheirVertex) {
        //init
        this.orders = new SortingOrder(inputOrders);
        initialize(currentNodeOrderInput, method, movePortsAdjToTurningDummiesToTheOutside,
                placeTurningDummiesNextToTheirVertex);

        List<List<Vertex>> lastStepNodeOrder;
        Map<Vertex, List<Port>> lastStepTopPortOrder;
        Map<Vertex, List<Port>> lastStepBottomPortOrder;



        numberOfCrossings = Integer.MAX_VALUE;
        int currentIteration = 0;
        boolean hasChanged = true;
        while (hasChanged) {

            //save lastStepOrders for check
            lastStepNodeOrder = new ArrayList<>();
            lastStepTopPortOrder = new LinkedHashMap<>();
            lastStepBottomPortOrder = new LinkedHashMap<>();
            for (int rank = 0; rank <= maxRank; rank++) {
                lastStepNodeOrder.add(new ArrayList<>(orders.getNodeOrder().get(rank)));
                for (Vertex node : orders.getNodeOrder().get(rank)) {
                    if (considerPortsOfNode(node)) {
                        lastStepTopPortOrder.put(node, new ArrayList<>(orders.getTopPortOrder().get(node)));
                        lastStepBottomPortOrder.put(node, new ArrayList<>(orders.getBottomPortOrder().get(node)));
                    }
                }
            }

            // as long as some orders change iterate from top to bottom and back to top over all ranks
            // reorder all nodes of a rank corresponding to the result of calculateXCoordinates()
            for (int directedRank = (1 - maxRank); directedRank <= maxRank; directedRank++) {

                int rank = Math.abs(directedRank);
                boolean upwards = directedRank > 0;
                List<SortingNode> currentLayer = getSortingNodeLayer(rank, upwards);
                List<SortingNode> adjacentPreviousLayer = getSortingNodeLayer(upwards ? rank - 1 : rank + 1, !upwards);

                Map<SortingNode, Double> barycenters = new LinkedHashMap<>();
                for (SortingNode node : currentLayer) {
                    double barycenter = getBarycenter(node, adjacentPreviousLayer);
                    //special case: if node has no neighbor in adjacent layer, keep its old position. In this case
                    // its barycenter is set to NaN (because of division by zero [vertices])
                    if (Double.isNaN(barycenter)) {
                        barycenter = (double) currentLayer.indexOf(node) / ((double) currentLayer.size() - 1.0)
                                * (double) adjacentPreviousLayer.size();
                    }
                    barycenters.put(node, barycenter);
                }
                Collections.sort(currentLayer, Comparator.comparingDouble(barycenters::get));
                updateVerticesAndPortsOrder(rank, currentLayer, barycenters, upwards);
                reorderPortParingsAndTurningDummies(rank, upwards);
                updateCurrentValues();
            }

            hasChanged = checkIfHasChanged(lastStepNodeOrder, lastStepTopPortOrder, lastStepBottomPortOrder,
                    currentIteration);
            ++currentIteration;
        }

        //do several iterations because these 2 steps influence each other
        int iterations = 2;
        for (int i = 0; i < iterations; i++) {
            handleTurningVerticesFinally(true);
            orderPortsFinally(i % 2 == 0, i == iterations - 1);
//            orderPortsFinally(i % 2 != 0);
        }
//        handleTurningVerticesFinally(true);

        return orders;
    }

    private static List<List<Vertex>> copyCurrentNodeOrder (List<List<Vertex>> currentNodeOrder) {
        List<List<Vertex>> currentNodeOrderCopy = new ArrayList<>();
        for (List<Vertex> list : currentNodeOrder) {
            currentNodeOrderCopy.add(new ArrayList<>(list));
        }
        return currentNodeOrderCopy;
    }

    private void initialize(List<List<Vertex>> currentNodeOrder, CrossingMinimizationMethod method,
                            boolean movePortsAdjToTurningDummiesToTheOutside,
                            boolean placeTurningDummiesNextToTheirVertex) {
        if (method == null) {
            this.method = DEFAULT_CROSSING_MINIMIZATION_METHOD;
        }
        this.method = method;
        this.movePortsAdjToTurningDummiesToTheOutside = movePortsAdjToTurningDummiesToTheOutside;
        this.placeTurningDummiesNextToTheirVertex = placeTurningDummiesNextToTheirVertex;

        this.currentValues = new LinkedHashMap<>();
        this.vertex2sortingNode = new LinkedHashMap<>();
        this.port2SortingNode = new LinkedHashMap<>();
        this.adjacentToDummyTurningPoints = new LinkedHashSet<>();
        maxRank = sugy.getMaxRank();
        if (currentNodeOrder == null) {
            // generate random order for each rank if nothing is given
            // do that for one component after the other
            this.orders.getNodeOrder().clear();
            ConnectedComponentClusterer connectedComponentClusterer = new ConnectedComponentClusterer(sugy.getGraph());
            List<Set<Vertex>> components = new ArrayList<>(connectedComponentClusterer.getConnectedComponents());
            Collections.shuffle(components, Constants.random);

            for (int r = 0; r <= maxRank; r++) {
                Collection<Vertex> allNodesWithRank = sugy.getAllNodesWithRank(r);
                List<Vertex> order = new ArrayList<>(allNodesWithRank.size());
                for (Set<Vertex> component : components) {
                    List<Vertex> compVtcsOnR = getSetIntersection(allNodesWithRank, component);
                    Collections.shuffle(compVtcsOnR, Constants.random);
                    order.addAll(compVtcsOnR);
                }
                this.orders.getNodeOrder().add(order);
            }

            //TODO: maybe forbid swaps between nodes/ports of different components to have all components
            // cleanly separated from each other in the end.
            // Alternatively run the whole crossing minimization once for each component and put the single components
            // layer-wise together in the end.
        }
        else {
            copyCurrentNodeOrder(currentNodeOrder);
        }
        //place dummy turning points close to their vertices
        if (this.placeTurningDummiesNextToTheirVertex) {
            placeTurningDummiesNextToTheirVertices();
        }
        for (int rank = 0; rank < this.orders.getNodeOrder().size(); rank++) {
            List<Vertex> layer = this.orders.getNodeOrder().get(rank);
            for (int i = 0; i < layer.size(); i++) {
                Vertex node = layer.get(i);
                //check if adjacent to dummy turning points
                for (Port port : node.getPorts()) {
                    for (Edge edge : port.getEdges()) {
                        for (Port otherPort : edge.getPorts()) {
                            if (!port.equals(otherPort) && sugy.isDummyTurningNode(otherPort.getVertex())) {
                                this.adjacentToDummyTurningPoints.add(node);
                            }
                        }
                    }
                }

                // initialize SortingNodes and currentValues
                if (considerPortsOfNode(node)) {
                    for (Port port : node.getPorts()) {
                        SortingNode sortingNode = new SortingNode(port);
                        port2SortingNode.put(port, sortingNode);
                    }
                    //special case: a side of a vertex does not have ports -> also create a sorting node for the vertex
                    if (orders.getTopPortOrder().get(node).isEmpty() || orders.getBottomPortOrder().get(node).isEmpty()) {
                        SortingNode sortingNode = new SortingNode(node);
                        vertex2sortingNode.put(node, sortingNode);
                    }
                }
                else {
                    SortingNode sortingNode = new SortingNode(node);
                    vertex2sortingNode.put(node, sortingNode);
                }
            }
        }

        //shuffle ports
        orders.shufflePorts();

        //update current values acc to type
        updateCurrentValues();
    }

    private static <E> List<E> getSetIntersection(Collection<E> collection0, Set<E> collection1) {
        List<E> intersectionList = new ArrayList<>();
        for (E e : collection0) {
            if (collection1.contains(e)) {
                intersectionList.add(e);
            }
        }
        return intersectionList;
    }

    private void placeTurningDummiesNextToTheirVertices() {
        for (int rank = 0; rank < orders.getNodeOrder().size(); rank++) {
            List<Vertex> currentLayer = orders.getNodeOrder().get(rank);
            //extract all turning dummies
            ArrayList<Vertex> turningDummiesOnLayer = new ArrayList<>();
            for (Vertex vertex : new ArrayList<>(currentLayer)) {
                if (sugy.isDummyTurningNode(vertex)) {
                    currentLayer.remove(vertex);
                    turningDummiesOnLayer.add(vertex);
                }
            }
            //re-insert them in random order close to their corresponding vertex
            Collections.shuffle(turningDummiesOnLayer, Constants.random);
            for (Vertex turningDummy : turningDummiesOnLayer) {
                Vertex vertex = sugy.getVertexOfTurningDummy(turningDummy);
                List<Vertex> adjacentLayer = orders.getNodeOrder().get(sugy.getRank(vertex));
                double relativePositionVertex =
                        (double) adjacentLayer.indexOf(vertex) / ((double) adjacentLayer.size() - 1.0);
                int targetIndexDummy = (int) Math.round((double) currentLayer.size() * relativePositionVertex);
                currentLayer.add(targetIndexDummy, turningDummy);
            }
        }
    }

    /**
     * for each edge, there is an object in the resulting list,
     * so if there are multiple edges to the same node, it will appear multiple times in the list.
     * This makes sense to have a weighted instance!
     *
     * @param sortingNode
     * @return
     */
    private List<SortingNode> getAdjacentSortingNodes(SortingNode sortingNode) {
        Object nodeObject = sortingNode.getStoredObject();
        ArrayList<SortingNode> adjacentNodes = new ArrayList<>();
        if (nodeObject instanceof Port) {
            extractAdjacentSortingNodes((Port) nodeObject, adjacentNodes);
        }
        else if (nodeObject instanceof Vertex) {
            for (Port port : ((Vertex) nodeObject).getPorts()) {
                extractAdjacentSortingNodes(port, adjacentNodes);
            }
        }
        return adjacentNodes;
    }

    /**
     * for each edge, there is an object in the resulting list,
     * so if there are multiple edges to the same node, it will appear multiple times in the list.
     * This makes sense to have a weighted instance!
     *
     * @param port
     * @param adjacentNodesAppendCollection
     */
    private void extractAdjacentSortingNodes(Port port, Collection<SortingNode> adjacentNodesAppendCollection) {
        for (Edge edge : port.getEdges()) {
            for (Port otherPort : edge.getPorts()) {
                if (!otherPort.equals(port)) {
                    if (port2SortingNode.containsKey(otherPort)) {
                        adjacentNodesAppendCollection.add(port2SortingNode.get(otherPort));
                    }
                    else {
                        adjacentNodesAppendCollection.add(vertex2sortingNode.get(otherPort.getVertex()));
                    }
                }
            }
        }
    }

    private void updateCurrentValues() {
        if (method.equals(CrossingMinimizationMethod.VERTICES)) {
            for (List<Vertex> layer : this.orders.getNodeOrder()) {
                for (int i = 0; i < layer.size(); i++) {
                    Vertex vertex = layer.get(i);
                    currentValues.put(vertex2sortingNode.get(vertex), (double) i);
                }
            }
        }
        else if (method.equals(CrossingMinimizationMethod.MIXED)) {
            for (List<Vertex> layer : this.orders.getNodeOrder()) {
                updateFractionalPortOrderPositions(layer);
            }
        }
        else if (method.equals(CrossingMinimizationMethod.PORTS)) {
            for (List<Vertex> layer : this.orders.getNodeOrder()) {
                updatePortOrderPositions(layer);
            }
        }
    }

    private void updatePortOrderPositions(List<Vertex> layer) {
        double valueBottom = 0.0;
        double valueTop = 0.0;
        for (Vertex node : layer) {
            for (Port port : orders.getTopPortOrder().get(node)) {
                currentValues.put(port2SortingNode.get(port), valueTop++);
            }
            for (Port port : orders.getBottomPortOrder().get(node)) {
                currentValues.put(port2SortingNode.get(port), valueBottom++);
            }
        }
    }

    private void updateFractionalPortOrderPositions(List<Vertex> layer) {
        for (int nodePosition = 0; nodePosition < layer.size(); nodePosition++) {
            Vertex node = layer.get(nodePosition);
            if (considerPortsOfNode(node)) {
                updateFractionalPortOrderPositions(nodePosition, node);
            }
            if (vertex2sortingNode.containsKey(node)) {
                currentValues.put(vertex2sortingNode.get(node), (double) nodePosition);
            }
        }
    }

    private void updateFractionalPortOrderPositions(int nodePosition, Vertex node) {
        double dividerTop = (double) orders.getTopPortOrder().get(node).size() + 1.0;
        double dividerBottom = (double) orders.getBottomPortOrder().get(node).size() + 1.0;
        for (int portPosition = 0; portPosition < orders.getTopPortOrder().get(node).size(); portPosition++) {
            currentValues.put(port2SortingNode.get(orders.getTopPortOrder().get(node).get(portPosition)),
                    (-0.5 + (double) nodePosition + (((double) portPosition + 1.0) / dividerTop)));
        }
        for (int portPosition = 0; portPosition < orders.getBottomPortOrder().get(node).size(); portPosition++) {
            currentValues.put(port2SortingNode.get(orders.getBottomPortOrder().get(node).get(portPosition)),
                    (-0.5 + (double) nodePosition + (((double) portPosition + 1.0) / dividerBottom)));
        }
    }

    private boolean considerPortsOfNode(Vertex node) {
        return method.equals(CrossingMinimizationMethod.PORTS)
                || (method.equals(CrossingMinimizationMethod.MIXED)
                && (sugy.isPlug(node) || sugy.isDummyTurningNode(node)
                    || this.adjacentToDummyTurningPoints.contains(node)));
    }

    private List<SortingNode> getSortingNodeLayer(int rank, boolean bottomSidePorts) {
        List<SortingNode> layerList = new ArrayList<>();
        for (Vertex vertex : orders.getNodeOrder().get(rank)) {
            if (considerPortsOfNode(vertex)) {
                List<Port> portsOnConsideredPortSide =
                        bottomSidePorts ? orders.getBottomPortOrder().get(vertex) : orders.getTopPortOrder().get(vertex);
                for (Port port : portsOnConsideredPortSide) {
                    layerList.add(port2SortingNode.get(port));
                }
                //special case: this vertex side has no ports -> use sorting node for the vertex
                if (portsOnConsideredPortSide.isEmpty()) {
                    layerList.add(vertex2sortingNode.get(vertex));
                }
            }
            else {
                layerList.add(vertex2sortingNode.get(vertex));
            }
        }
        return layerList;
    }

    private void updateVerticesAndPortsOrder(int layerIndex, List<SortingNode> currentLayer,
                                             Map<SortingNode, Double> barycenters, boolean upwards) {
        //bring vertices together acc. to the previously compute barycenters
        List<Vertex> verticesOfLayer = new ArrayList<>();
        List<Port> portOrdering = new ArrayList<>();
        Map<Vertex, Double> barycenterSum = new LinkedHashMap<>();
        Map<Vertex, Integer> counter = new LinkedHashMap<>();
        for (SortingNode node : currentLayer) {
            Object nodeObject = node.getStoredObject();
            if (nodeObject instanceof Vertex) {
                verticesOfLayer.add((Vertex) nodeObject);
                barycenterSum.put((Vertex) nodeObject, barycenters.get(node));
                counter.put((Vertex) nodeObject, 1);
            }
            else if (nodeObject instanceof Port) {
                portOrdering.add((Port) nodeObject);
                Vertex vertex = ((Port) nodeObject).getVertex();
                if (!barycenterSum.containsKey(vertex)) {
                    verticesOfLayer.add(vertex);
                    barycenterSum.put(vertex, barycenters.get(node));
                    counter.put(vertex, 1);
                }
                else {
                    barycenterSum.replace(vertex, barycenterSum.get(vertex) + barycenters.get(node));
                    counter.replace(vertex, counter.get(vertex) + 1);
                }
            }
        }
        //re-sort vertices
        Collections.sort(verticesOfLayer,
                Comparator.comparingDouble(v -> barycenterSum.get(v) / (double) counter.get(v)));
        orders.getNodeOrder().set(layerIndex, verticesOfLayer);

        //re-sort ports
        for (Vertex vertex : verticesOfLayer) {
            if (considerPortsOfNode(vertex)) {
                List<Port> portsOfVertex = orderPortsConstraintToPortGroups(portOrdering,vertex.getPortCompositions());
                if (upwards) {
                    orders.getBottomPortOrder().replace(vertex, portsOfVertex);
                } else {
                    orders.getTopPortOrder().replace(vertex, portsOfVertex);
                }
            }
        }
    }

    private boolean checkIfHasChanged(List<List<Vertex>> lastStepNodeOrder,
                                      Map<Vertex, List<Port>> lastStepTopPortOrder,
                                      Map<Vertex, List<Port>> lastStepBottomPortOrder, int currentIteration) {
        // todo: number of iterations is calculated arbitrarily - adapt if necessary - lower in case of runtime issues
        int numberOfIterations = (orders.getNodeOrder().size());
        boolean hasChanged = false;
        if (currentIteration < numberOfIterations) {
            // check for changes in the structure
            for (int rank = 0; (rank <= maxRank) && !hasChanged; rank++) {
                for (int i = 0; (i < orders.getNodeOrder().get(rank).size()) && !hasChanged; i++) {
                    Vertex node = orders.getNodeOrder().get(rank).get(i);
                    if (!lastStepNodeOrder.get(rank).get(i).equals(node)) hasChanged = true;
                    if (considerPortsOfNode(node)) {
                        for (int j = 0; (j < orders.getBottomPortOrder().get(node).size()) && !hasChanged; j++) {
                            Port bottomPort = orders.getBottomPortOrder().get(node).get(j);
                            if (!lastStepBottomPortOrder.get(node).get(j).equals(bottomPort)) hasChanged = true;
                        }
                        for (int j = 0; (j < orders.getTopPortOrder().get(node).size()) && !hasChanged; j++) {
                            Port topPort = orders.getTopPortOrder().get(node).get(j);
                            if (!lastStepTopPortOrder.get(node).get(j).equals(topPort)) hasChanged = true;
                        }
                    }
                }
            }
        } else {
            // check for changes in number of crossings due to possibility of an endless loop
            // todo: in case of runtime issues change to more intelligent counting method
            Map<Vertex, List<Port>> currentTPortOrder = new LinkedHashMap<>(orders.getTopPortOrder());
            Map<Vertex, List<Port>> currentBPortOrder = new LinkedHashMap<>(orders.getBottomPortOrder());
            orderPortsFinally(currentTPortOrder, currentBPortOrder, false, true, false);
            int newNumberOfCrossings = sugy.countCrossings(new SortingOrder(orders.getNodeOrder(), currentTPortOrder, currentBPortOrder));
            if (newNumberOfCrossings < numberOfCrossings) {
                numberOfCrossings = newNumberOfCrossings;
                hasChanged = true;
            }
        }
        return hasChanged;
    }

    private void handleTurningVerticesFinally(boolean sortPortsAtCorrespondingRealNode) {
        for (List<Vertex> layer : orders.getNodeOrder()) {
            for (Vertex node : layer) {
                if (sugy.isDummyTurningNode(node)) {
                    sortPortsAtTurningDummy(node, sortPortsAtCorrespondingRealNode);
                }
            }
            //update current values
            if (method.equals(CrossingMinimizationMethod.PORTS)) {
                updatePortOrderPositions(layer);
            } else {
                updateFractionalPortOrderPositions(layer);
            }
        }
    }

    private void sortPortsAtTurningDummy(Vertex turningDummy, boolean sortPortsAtCorrespondingRealNode) {
        List<Port> portsBottom = orders.getBottomPortOrder().get(turningDummy);
        List<Port> portsTop = orders.getTopPortOrder().get(turningDummy);
        List<Port> ports = portsBottom != null && !portsBottom.isEmpty() ? portsBottom : portsTop;
        Map<Port, Double> port2adjacentNodePosition = new LinkedHashMap<>();
        for (Port port : ports) {
            Port otherPort = PortUtils.getAdjacentPort(port);
            double adjacentObjectPosition = port2SortingNode.get(otherPort) != null ?
                    currentValues.get(port2SortingNode.get(otherPort)) :
                    currentValues.get(vertex2sortingNode.get(otherPort.getVertex()));
            port2adjacentNodePosition.put(port, adjacentObjectPosition);
        }

        //first sort ports at dummy turning vertex -- this should be unproblematic
        Collections.sort(ports, Comparator.comparingDouble(port2adjacentNodePosition::get));

        //now extract internal orderings
        List<Port> portsToDummyEdgeNodesLeft = new ArrayList<>();
        List<Port> portsToDummyEdgeNodesRight = new ArrayList<>();
        List<Port> portsToItsVertex = new ArrayList<>();
        Vertex correspondingVertex = sugy.getVertexOfTurningDummy(turningDummy);
        List<Port> portsAtItsVertex = new ArrayList<>();
        List<Port> allPortsAtItsVertex = ports == portsBottom ?
                orders.getTopPortOrder().get(correspondingVertex) : orders.getBottomPortOrder().get(correspondingVertex);
        boolean encounteredMiddlePart = false;
        for (Port port : ports) {
            Port otherPort = PortUtils.getAdjacentPort(port);
            if (otherPort.getVertex().equals(correspondingVertex)) {
                portsToItsVertex.add(port);
                portsAtItsVertex.add(otherPort);
                encounteredMiddlePart = true;
            }
            else {
                if (encounteredMiddlePart) {
                    portsToDummyEdgeNodesRight.add(port);
                }
                else {
                    portsToDummyEdgeNodesLeft.add(port);
                }
            }
        }
        Collections.sort(portsAtItsVertex, Comparator.comparingInt(allPortsAtItsVertex::indexOf));

        //now re-sort the ports at this turning dummy going to its corresponding vertex -- this is always possible
        Collections.reverse(portsToDummyEdgeNodesLeft);
        Collections.reverse(portsToDummyEdgeNodesRight);
        int i = 0;
        for (Port port : portsToDummyEdgeNodesLeft) {
            Port currentPortToBeReplaced = portsToItsVertex.get(i++);
            Port targetPort = sugy.getCorrespondingPortAtDummy(port);
            swapPortsIfPossible(ports, currentPortToBeReplaced, targetPort, false);
            Collections.swap(portsToItsVertex, portsToItsVertex.indexOf(currentPortToBeReplaced),
                    portsToItsVertex.indexOf(targetPort));
        }
        for (Port port : portsToDummyEdgeNodesRight) {
            Port currentPortToBeReplaced = portsToItsVertex.get(i++);
            Port targetPort = sugy.getCorrespondingPortAtDummy(port);
            swapPortsIfPossible(ports, currentPortToBeReplaced, targetPort, false);
            Collections.swap(portsToItsVertex, portsToItsVertex.indexOf(currentPortToBeReplaced),
                    portsToItsVertex.indexOf(targetPort));
        }

        //now re-sort at the "real" vertex of this turning dummy accordingly
        if (sortPortsAtCorrespondingRealNode) {
            for (int j = 0; j < portsToItsVertex.size(); j++) {
                Port portAtTurningDummy = portsToItsVertex.get(j);
                Port portAtVertexToBeReplaced = portsAtItsVertex.get(j);
                Port portAtVertexTarget = PortUtils.getAdjacentPort(portAtTurningDummy);
                boolean didSwap = swapPortsIfPossible(allPortsAtItsVertex, portAtVertexToBeReplaced, portAtVertexTarget, true);
                if (didSwap) {
                    Collections.swap(portsAtItsVertex, portsAtItsVertex.indexOf(portAtVertexToBeReplaced),
                            portsAtItsVertex.indexOf(portAtVertexTarget));
                }
            }
        }
    }

    private boolean swapPortsIfPossible(List<Port> ports, Port port0, Port port1, boolean swapPairedPorts) {
        if ((!sugy.isPaired(port0) && !sugy.isPaired(port1))) {
            if (PortUtils.areInTheSamePortGroup(port0, port1)) {
                Collections.swap(ports, ports.indexOf(port0), ports.indexOf(port1));
                return true;
            }
        }
        else if (swapPairedPorts) {
            List<Port> otherSidePorts = orders.getTopPortOrder().get(port0.getVertex()).equals(ports) ?
                    orders.getBottomPortOrder().get(port0.getVertex()) : orders.getTopPortOrder().get(port0.getVertex());
            Port otherSidePort0 = sugy.isPaired(port0) ? sugy.getPairedPort(port0) : null;
            Port otherSidePort1 = sugy.isPaired(port1) ? sugy.getPairedPort(port1) : null;

            int indexPort0 = ports.indexOf(port0);
            int indexPort1 = ports.indexOf(port1);

            //check if we can swap the other side
            boolean canSwapOtherSide = true;
            int indexUnpairedOtherSidePort = -1;
            if (otherSidePort0 == null) {
                indexUnpairedOtherSidePort = otherSidePorts.indexOf(otherSidePort1) + (indexPort0 - indexPort1);
                if (indexUnpairedOtherSidePort < 0) {
                    canSwapOtherSide = PortUtils.areInTheSamePortGroup(otherSidePorts.get(0), otherSidePort1);
                }
                else if (indexUnpairedOtherSidePort >= otherSidePorts.size()) {
                    canSwapOtherSide = PortUtils.areInTheSamePortGroup(otherSidePorts.get(otherSidePorts.size() - 1),
                            otherSidePort1);
                }
                else {
                    otherSidePort0 = otherSidePorts.get(indexUnpairedOtherSidePort);
                }
            }
            else if (otherSidePort1 == null) {
                indexUnpairedOtherSidePort = otherSidePorts.indexOf(otherSidePort0) + (indexPort1 - indexPort0);
                if (indexUnpairedOtherSidePort < 0) {
                    canSwapOtherSide = PortUtils.areInTheSamePortGroup(otherSidePort0, otherSidePorts.get(0));
                }
                else if (indexUnpairedOtherSidePort >= otherSidePorts.size()) {
                    canSwapOtherSide = PortUtils.areInTheSamePortGroup(otherSidePort0,
                            otherSidePorts.get(otherSidePorts.size() - 1));
                }
                else {
                    otherSidePort1 = otherSidePorts.get(indexUnpairedOtherSidePort);
                }
            }

            if (otherSidePort0 != null && otherSidePort1 != null) {
                canSwapOtherSide = PortUtils.areInTheSamePortGroup(otherSidePort0, otherSidePort1);
            }

            //if we can swap both sides, we do it
            if (canSwapOtherSide && PortUtils.areInTheSamePortGroup(port0, port1)) {
                //swap this side
                Collections.swap(ports, ports.indexOf(port0), ports.indexOf(port1));
                //swap other side
                if (otherSidePort0 == null) {
                    otherSidePorts.remove(otherSidePort1);
                    if (indexUnpairedOtherSidePort < 0) {
                        otherSidePorts.add(0, otherSidePort1);
                    }
                    else {
                        otherSidePorts.add(otherSidePorts.size(), otherSidePort1);
                    }
                }
                else if (otherSidePort1 == null) {
                    otherSidePorts.remove(otherSidePort0);
                    if (indexUnpairedOtherSidePort < 0) {
                        otherSidePorts.add(0, otherSidePort0);
                    }
                    else {
                        otherSidePorts.add(otherSidePorts.size(), otherSidePort0);
                    }
                }
                else {
                    Collections.swap(otherSidePorts, otherSidePorts.indexOf(otherSidePort0),
                            otherSidePorts.indexOf(otherSidePort1));
                }
                return true;
            }
        }
        return false;
    }

    private void orderPortsFinally (boolean upwards, boolean isFinalSorting) {
        orderPortsFinally(this.orders.getTopPortOrder(), this.orders.getBottomPortOrder(), true, upwards, isFinalSorting);
    }

    private void orderPortsFinally (Map<Vertex, List<Port>> currentTPortOrder,
                                    Map<Vertex, List<Port>> currentBPortOrder,
                                    boolean updateCurrentValues, boolean upwards, boolean isFinalSorting) {
        List<Map<Vertex, List<Port>>> portOrdersToBeSorted = Arrays.asList(currentBPortOrder, currentTPortOrder);

        List<List<Vertex>> nodeOrder = new ArrayList<>(orders.getNodeOrder());
        if (!upwards) {
            Collections.reverse(nodeOrder);
        }
        for (List<Vertex> layer : nodeOrder) {
            for (Vertex node : layer) {
                //do not sort ports of turning dummies, this will be done by handleTurningVerticesFinally()
                if (sugy.isDummyTurningNode(node)) {
                    continue;
                }
                for (Map<Vertex, List<Port>> currentPortOrderMap : portOrdersToBeSorted) {
                    List<Port> portsOfThisNodeSide = new ArrayList<>(currentPortOrderMap.get(node));
                    orderPorts(portsOfThisNodeSide, node);
                    portsOfThisNodeSide = orderPortsConstraintToPortGroups(portsOfThisNodeSide, node.getPortCompositions());
                    currentPortOrderMap.replace(node, portsOfThisNodeSide);
                }
                int iterations = 4;
                for (int i = 0; i < iterations; i++) {
                    repairPortPairings(node, currentBPortOrder, currentTPortOrder, ((upwards ? 0 : 1) + i) % 2 == 0,
                            isFinalSorting && i == iterations - 1);
                }
            }
            if (updateCurrentValues) {
                if (method.equals(CrossingMinimizationMethod.PORTS)) {
                    updatePortOrderPositions(layer);
                } else {
                    updateFractionalPortOrderPositions(layer);
                }
            }
        }
    }

    private void movePortsAdjacentToDummyTurningPoints(Vertex node, List<Vertex> layer,
                                                       Map<Vertex, List<Port>> currentBPortOrder,
                                                       Map<Vertex, List<Port>> currentTPortOrder) {

        List<Port> bottomOrder = currentBPortOrder.get(node);
        List<Port> topOrder = currentTPortOrder.get(node);

        movePortsAdjacentToTurningDummy(node, layer, bottomOrder);
        movePortsAdjacentToTurningDummy(node, layer, topOrder);


        currentBPortOrder.replace(node, bottomOrder);
        currentTPortOrder.replace(node, topOrder);
    }

    private void movePortsAdjacentToTurningDummy(Vertex node, List<Vertex> layer, List<Port> portOrder) {
        for (int i = 0; i < portOrder.size(); i++) {
            Port consideredPort = portOrder.get(i);
            if (!consideredPort.getEdges().isEmpty()) {
                Edge edge = consideredPort.getEdges().get(0);
                ArrayList<Port> portsOfEdge = new ArrayList<>(edge.getPorts());
                portsOfEdge.remove(consideredPort);
                Port otherPort = portsOfEdge.get(0);
                Vertex turningPointDummy = otherPort.getVertex();
                if (sugy.isDummyTurningNode(turningPointDummy)) {
                    List<Vertex> otherNodesOnSameLayer = new ArrayList<>();
                    for (Port portOfDummy : turningPointDummy.getPorts()) {
                        for (Edge portEdge : portOfDummy.getEdges()) {
                            if (!portEdge.equals(edge)) {
                                List<Port> portsOfOtherEdge = new ArrayList<>(portEdge.getPorts());
                                portsOfOtherEdge.remove(portOfDummy);
                                otherNodesOnSameLayer.add(portsOfOtherEdge.get(0).getVertex());
                            }
                        }
                    }
                    double barycenterOtherNodesSum = 0;
                    for (Vertex otherNode : otherNodesOnSameLayer) {
                        barycenterOtherNodesSum += layer.indexOf(otherNode);
                    }
                    if (barycenterOtherNodesSum / (double) otherNodesOnSameLayer.size() < layer.indexOf(node)) {
                        int j = i;
                        while (canSwapLeft(j, portOrder)) {
                            swapLeft(j, portOrder);
                            --j;
                        }
                    }
                    else {
                        int j = i;
                        while (canSwapRight(j, portOrder)) {
                            swapRight(j, portOrder);
                            ++j;
                        }
                    }
                }
            }
        }
    }

    private void repairPortPairings(Vertex node, Map<Vertex, List<Port>> currentBPortOrder,
                                    Map<Vertex, List<Port>> currentTPortOrder, boolean preferredSwapSideTop,
                                    boolean isFinalSorting) {
        List<Port> bottomOrder = currentBPortOrder.get(node);
        List<Port> topOrder = currentTPortOrder.get(node);

        List<Port> firstOrder = preferredSwapSideTop ? topOrder : bottomOrder;
        List<Port> secondOrder = preferredSwapSideTop ? bottomOrder : topOrder;

        //first find all port pairings
        List<Pair<Port>> allPortPairings = new ArrayList<>(bottomOrder.size());
        Set<Port> allPairedPorts = new LinkedHashSet<>();
        for (Port port : secondOrder) {
            Port pairedPort = sugy.getPairedPort(port);
            if (pairedPort != null) {
                allPortPairings.add(new Pair<>(port, pairedPort));
                allPairedPorts.add(port);
                allPairedPorts.add(pairedPort);
            }
        }

        //find orderings purely of port pairing ports
        List<Port> bottomPairingOrder = extractOrderingOfPairedPorts(bottomOrder, allPairedPorts);
        List<Port> topPairingOrder = extractOrderingOfPairedPorts(topOrder, allPairedPorts);

        List<Port> firstOrderPairedPorts = preferredSwapSideTop ? topPairingOrder : bottomPairingOrder;
        List<Port> secondOrderPairedPorts = preferredSwapSideTop ? bottomPairingOrder : topPairingOrder;

        for (int i = 0; i < allPortPairings.size(); i++) {
            Pair<Port> portPairing = allPortPairings.get(i);
            Port firstPort = portPairing.getSecond();
            Port secondPort = portPairing.getFirst();
            Port bottomPort = preferredSwapSideTop ? secondPort : firstPort;
            Port topPort = preferredSwapSideTop ? firstPort : secondPort;
            int indexBottom = bottomPairingOrder.indexOf(bottomPort);
            int indexTop = topPairingOrder.indexOf(topPort);
            //move port on preferred side to bring the paired ports closer to each other -- ideally over each other
            //without creating new crossings with other port pairings
            //possibility of endless loop -> add max iteration condition. maybe do something better later
            //going over whole bottom and top order should definitely be enough
            int maxIteration = 2 * (bottomOrder.size() + topOrder.size());
            int currIteration = 0;
            boolean forceSwapping = false;
            while (indexBottom != indexTop && currIteration++ < maxIteration) {
                //first we move ports with swapping ports on the preferred side until it is not possible any more
                // because of port constraints
                //then we try to swap the port groups
                //then the same for the other side
                int indexFirstPort = preferredSwapSideTop ? indexTop : indexBottom;
                int indexSecondPort = preferredSwapSideTop ? indexBottom : indexTop;

                if (indexFirstPort < indexSecondPort) {
                    boolean hasChanged = swapIfPossible(firstOrder, firstPort, firstOrderPairedPorts, indexFirstPort,
                           false, !isFinalSorting, forceSwapping, allPairedPorts);
                    if (hasChanged) {
                        forceSwapping = false;
                        if (preferredSwapSideTop) {
                            indexTop = topPairingOrder.indexOf(topPort);
                        } else {
                            indexBottom = bottomPairingOrder.indexOf(bottomPort);
                        }
                    } else {
                        //for the second side we swap only to the right and we do not swap port groups to not destroy
                        // previously made arrangements on the left
                        hasChanged = swapIfPossible(secondOrder, secondPort, secondOrderPairedPorts, indexSecondPort,
                                true, false, forceSwapping, allPairedPorts);
                        if (hasChanged) {
                            forceSwapping = false;
                            if (preferredSwapSideTop) {
                                indexBottom = bottomPairingOrder.indexOf(bottomPort);
                            } else {
                                indexTop = topPairingOrder.indexOf(topPort);
                            }
                        } else {
                            //we cannot improve the current situation any more -> force swapping and possibly destroy
                            // the order within a port group
                            forceSwapping = true;
                        }
                    }
                } else { //indexFirstPort > indexSecondPort
                    boolean hasChanged = false;
                    if (!isFinalSorting) {
                        hasChanged = swapIfPossible(firstOrder, firstPort, firstOrderPairedPorts, indexFirstPort,
                                true, !isFinalSorting, forceSwapping, allPairedPorts);
                    }
                    if (hasChanged) {
                        forceSwapping = false;
                        if (preferredSwapSideTop) {
                            indexTop = topPairingOrder.indexOf(topPort);
                        } else {
                            indexBottom = bottomPairingOrder.indexOf(bottomPort);
                        }
                    } else {
                        hasChanged = swapIfPossible(secondOrder, secondPort, secondOrderPairedPorts, indexSecondPort,
                                false, false, forceSwapping, allPairedPorts);
                        if (hasChanged) {
                            forceSwapping = false;
                            if (preferredSwapSideTop) {
                                indexBottom = bottomPairingOrder.indexOf(bottomPort);
                            } else {
                                indexTop = topPairingOrder.indexOf(topPort);
                            }
                        } else {
                            //we cannot improve the current situation any more -> force swapping and possibly destroy
                            // the order within a port group
                            forceSwapping = true;
                        }
                    }
                }
            }
        }

        //check success
        if (isFinalSorting) {
            //check port pairings
            boolean portParingsValid = true;
            for (Pair<Port> portPairing : allPortPairings) {
                Port bottomPort = preferredSwapSideTop ? portPairing.getFirst() : portPairing.getSecond();
                Port topPort = preferredSwapSideTop ? portPairing.getSecond() : portPairing.getFirst();
                int indexBottom = bottomPairingOrder.indexOf(bottomPort);
                int indexTop = topPairingOrder.indexOf(topPort);
                if (indexBottom != indexTop) {
                    portParingsValid = false;
                }
            }
            if (!portParingsValid) {
                System.out.println("Warning! No valid arrangement of port pairings found for plug "
                        + sugy.getPlugs().get(node).getContainedVertices() + ".");
            }
            //check port groups
            List<Port> allPortsCombined = new ArrayList<>(bottomOrder);
            allPortsCombined.addAll(topOrder);
            if (!PortUtils.arrangmentOfPortsIsValidAccordingToPortGroups(allPortsCombined, node.getPortCompositions())) {
                System.out.println("Warning! Constraints due to port groups " +
                        "not completely fulfilled (possibly because of conflicts with port pairings) at plug "
                        + sugy.getPlugs().get(node).getContainedVertices() + ".");
            }
        }
    }

    private List<Port> extractOrderingOfPairedPorts(List<Port> orderingOfAllPorts, Set<Port> allPairedPorts) {
        List<Port> orderOfPairedPorts = new ArrayList<>(allPairedPorts.size() / 2);
        return updateOrderingOfPairedPorts(orderOfPairedPorts, orderingOfAllPorts, allPairedPorts);
    }

    private List<Port> updateOrderingOfPairedPorts(List<Port> orderOfPairedPorts, List<Port> orderingOfAllPorts,
                                                   Set<Port> allPairedPorts) {
        orderOfPairedPorts.clear();
        for (Port port : orderingOfAllPorts) {
            if (allPairedPorts.contains(port)) {
                orderOfPairedPorts.add(port);
            }
        }
        return orderOfPairedPorts;
    }

    private boolean swapIfPossible(List<Port> orderedPorts, Port consideredPort, List<Port> orderOfPairedPorts,
                                   int indexWithinPairedPorts, boolean swapLeft, boolean swapPortGroups,
                                   boolean forceSwapping, Set<Port> allPairedPorts) {
        int indexConsideredPort = orderedPorts.indexOf(consideredPort);
        if (swapLeft) {
            int prevIndexPairedPort = indexWithinPairedPorts == 0 ? -1 :
                    orderedPorts.indexOf(orderOfPairedPorts.get(indexWithinPairedPorts - 1));
            //second condition to not make new crossings with previous port parings (only when swapping left)
            if (canSwapLeft(indexConsideredPort, orderedPorts)) {// && prevIndexPairedPort != indexConsideredPort - 1) {
                swapLeft(indexConsideredPort, orderedPorts);
                //check if the swap on the larger list (including non-paired ports) effected also a swap of the list
                // where only paired ports are contained
                if (indexWithinPairedPorts > 0 &&
                        orderOfPairedPorts.get(indexWithinPairedPorts - 1) == orderedPorts.get(indexConsideredPort)) {
                    Collections.swap(orderOfPairedPorts, indexWithinPairedPorts - 1, indexWithinPairedPorts);
                }
                return true;
            }
            else if (swapPortGroups && canSwapPortGroupLeft(indexConsideredPort, orderedPorts, prevIndexPairedPort)) {
                swapPortGroupLeft(indexConsideredPort, orderedPorts);
                updateOrderingOfPairedPorts(orderOfPairedPorts, orderedPorts, allPairedPorts);
                return true;
            }
            else if (forceSwapping && indexConsideredPort > 0) {
                swapLeft(indexConsideredPort, orderedPorts);
                //check if the swap on the larger list (including non-paired ports) effected also a swap of the list
                // where only paired ports are contained
                if (indexWithinPairedPorts > 0 &&
                        orderOfPairedPorts.get(indexWithinPairedPorts - 1) == orderedPorts.get(indexConsideredPort)) {
                    Collections.swap(orderOfPairedPorts, indexWithinPairedPorts - 1, indexWithinPairedPorts);
                }
                return true;
            }
        }
        else {
            //we don't need the check nextIndexPairedPort != index + 1 because if we make new crossings
            // this way, they will be removed in the next steps of the for loop when the next port pairings
            // are considered
            if (canSwapRight(indexConsideredPort, orderedPorts)) {
                swapRight(indexConsideredPort, orderedPorts);
                //check if the swap on the larger list (including non-paired ports) effected also a swap of the list
                // where only paired ports are contained
                if (indexWithinPairedPorts < orderOfPairedPorts.size() - 1 &&
                        orderOfPairedPorts.get(indexWithinPairedPorts + 1) == orderedPorts.get(indexConsideredPort)) {
                    Collections.swap(orderOfPairedPorts, indexWithinPairedPorts + 1, indexWithinPairedPorts);
                }
                return true;
            }
            else if (swapPortGroups && canSwapPortGroupRight(indexConsideredPort, orderedPorts)) {
                swapPortGroupRight(indexConsideredPort, orderedPorts);
                updateOrderingOfPairedPorts(orderOfPairedPorts, orderedPorts, allPairedPorts);
                return true;
            }
            else if (forceSwapping && indexConsideredPort < orderedPorts.size() - 1) {
                swapRight(indexConsideredPort, orderedPorts);
                //check if the swap on the larger list (including non-paired ports) effected also a swap of the list
                // where only paired ports are contained
                if (indexWithinPairedPorts < orderOfPairedPorts.size() - 1 &&
                        orderOfPairedPorts.get(indexWithinPairedPorts + 1) == orderedPorts.get(indexConsideredPort)) {
                    Collections.swap(orderOfPairedPorts, indexWithinPairedPorts + 1, indexWithinPairedPorts);
                }
                return true;
            }
        }
        return false;
    }

    private boolean canSwapLeft(int index, List<Port> portOrder) {
        if (index <= 0) {
            return false;
        }
        PortGroup portGroup = portOrder.get(index).getPortGroup();
        PortGroup portGroupLeft = portOrder.get(index - 1).getPortGroup();
        if (portGroup == null) {
            return portGroupLeft == null;
        }
        return portGroup.equals(portGroupLeft);
    }

    private boolean canSwapRight(int index, List<Port> portOrder) {
        if (index >= portOrder.size() - 1) {
            return false;
        }
        PortGroup portGroup = portOrder.get(index).getPortGroup();
        PortGroup portGroupRight = portOrder.get(index + 1).getPortGroup();
        if (portGroup == null) {
            return portGroupRight == null;
        }
        return portGroup.equals(portGroupRight);
    }

    private void swapLeft(int index, List<Port> portOrder) {
        Collections.swap(portOrder, index, index - 1);
    }

    private void swapRight(int index, List<Port> portOrder) {
        Collections.swap(portOrder, index, index + 1);
    }

    private boolean canSwapPortGroupLeft(int index, List<Port> portOrder, int lastIndexPairedPort) {
        if (index <= 0) {
            return false;
        }
        Port portSelf = portOrder.get(index);
        Port portLeft = portOrder.get(index - 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portLeft);
        PortComposition candidateLeft =
                PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portLeft);
        for (Port port : PortUtils.getPortsRecursively(candidateLeft)) {
            int indexMember = portOrder.indexOf(port);
            if (indexMember == lastIndexPairedPort) {
                return false;
            }
        }
        return true;
    }

    //we don't need the check nextIndexPairedPort because if we make new crossings this way, they will be removed in
    // the next steps of the for loop when the next port pairings are considered
    private boolean canSwapPortGroupRight(int index, List<Port> portOrder) {
        return index < portOrder.size() - 1;
    }

    private void swapPortGroupLeft(int index, List<Port> portOrder) {
        Port portSelf = portOrder.get(index);
        Port portLeft = portOrder.get(index - 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portLeft);
        PortComposition candidateSelf = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portSelf);
        PortComposition candidateLeft = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portLeft);
        swapPortCompositions(portOrder, candidateLeft, candidateSelf);
    }

    private void swapPortGroupRight(int index, List<Port> portOrder) {
        Port portSelf = portOrder.get(index);
        Port portRight = portOrder.get(index + 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portRight);
        PortComposition candidateSelf = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portSelf);
        PortComposition candidateRight = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portRight);
        swapPortCompositions(portOrder, candidateSelf, candidateRight);
    }

    /**
     * both port compositions (they can both also be port groups of multiple ports) must be direct neighbors!!
     * they must have no ports in common and there must be no other ports between them
     * @param portOrder
     * @param leftPC
     * @param rightPC
     */
    private void swapPortCompositions(List<Port> portOrder, PortComposition leftPC, PortComposition rightPC) {
        List<Port> portsLeftPC = PortUtils.getPortsRecursively(leftPC);
        LinkedList<Port> portsLeftPCInOrder = new LinkedList<>();
        List<Port> portsRightPC = PortUtils.getPortsRecursively(rightPC);
        for (Port port : new ArrayList<>(portOrder)) {
            if (portsLeftPC.contains(port)) {
                portsLeftPCInOrder.add(port);
                portOrder.remove(port);
            }
        }
        //find end of right pc
        int endIndexRightPC = -1;
        for (int i = 0; i < portOrder.size(); i++) {
            if (portsRightPC.contains(portOrder.get(i))) {
                endIndexRightPC = i;
            }
        }
        //re-insert ports of left pc after the last of the right pc
        while (!portsLeftPCInOrder.isEmpty()) {
            portOrder.add(endIndexRightPC + 1, portsLeftPCInOrder.removeLast());
        }
    }

    private void orderPorts(List<Port> ports, Vertex node) {
        //find the barycenter for each port, we don't care about port groups in this method
        LinkedHashMap<Port, Double> port2barycenter = new LinkedHashMap<>();
        for (Port port : ports) {
            port2barycenter.put(port, getBarycenter(port));
        }
        Collections.sort(ports, (Comparator.comparingDouble(port2barycenter::get)));
    }

    private List<Port> orderPortsConstraintToPortGroups(List<Port> arbitraryPortOrder,
                                                        Collection<PortComposition> constrainingPortCompositions) {
        //find all ports of the port compositions and compute the barycenter
        LinkedHashMap<PortComposition, Double> portComposition2portBarycenter = new LinkedHashMap<>();
        LinkedHashMap<PortComposition, List<Port>> portComposition2ports = new LinkedHashMap<>();
        for (PortComposition portComposition : constrainingPortCompositions) {
            List<Port> portsOfThisPC = PortUtils.getPortsRecursively(portComposition);
            List<Port> portsOfThisPCcorrectSideInOrder = new ArrayList<>(portsOfThisPC.size());
            //keep only the ports on the correct side and in correct order
            for (Port port : arbitraryPortOrder) {
                if (portsOfThisPC.contains(port)) {
                    portsOfThisPCcorrectSideInOrder.add(port);
                }
            }
            //find and save barycenter of pc
            if (portComposition instanceof PortGroup) {
                //compute barycenter of this port group
                int barycenterSum = 0;
                int portCount = 0;
                for (Port port : portsOfThisPCcorrectSideInOrder) {
                    barycenterSum += arbitraryPortOrder.indexOf(port);
                    ++portCount;
                }
                portComposition2portBarycenter.put(portComposition, (double) barycenterSum / (double) portCount);
                //order ports of this port group recursively
                portsOfThisPCcorrectSideInOrder = orderPortsConstraintToPortGroups(portsOfThisPCcorrectSideInOrder,
                        ((PortGroup) portComposition).getPortCompositions());
            }
            else if (portComposition instanceof Port) {
                portComposition2portBarycenter.put(portComposition,
                        (double) arbitraryPortOrder.indexOf(portComposition));
            }
            //save list that is recursively correctly sorted
            portComposition2ports.put(portComposition, portsOfThisPCcorrectSideInOrder);
        }
        //concatenate all lists of these port compositions sorted by the barycenters
        List<PortComposition> portCompositions = new ArrayList<>(constrainingPortCompositions);
        Collections.sort(portCompositions, (Comparator.comparingDouble(portComposition2portBarycenter::get)));
        List<Port> resultingList = new ArrayList<>(arbitraryPortOrder.size());
        for (PortComposition portComposition : portCompositions) {
            for (Port port : portComposition2ports.get(portComposition)) {
                resultingList.add(port);
            }
        }
        return resultingList;
    }

    private double getBarycenter(Port port) {
        ArrayList<SortingNode> adjacentNodes = new ArrayList<>();
        for (Edge edge : port.getEdges()) {
            Port otherPort = edge.getPorts().get(0);
            if (otherPort.equals(port)){
                otherPort = edge.getPorts().get(1);
            }
            if (port2SortingNode.containsKey(otherPort)) {
                adjacentNodes.add(port2SortingNode.get(otherPort));
            }
            else {
                adjacentNodes.add(vertex2sortingNode.get(otherPort.getVertex()));
            }
        }
        return getBarycenter(adjacentNodes);
    }

    private double getBarycenter(SortingNode node, Collection<SortingNode> adjacentNodesOnAdjacentLayer) {
        //first filter out the ones node is not adjacent to
        Collection<SortingNode> adjacentToThisNode = getAdjacentSortingNodes(node);
        ArrayList<SortingNode> adjAndOnDesiredLayer = new ArrayList<>();
        for (SortingNode neighbor : adjacentToThisNode) {
            if (adjacentNodesOnAdjacentLayer.contains(neighbor)) {
                adjAndOnDesiredLayer.add(neighbor);
            }
        }
        return getBarycenter(adjAndOnDesiredLayer);
    }

    private double getBarycenter(List<SortingNode> adjacentNodesOnAdjacentLayer) {
        double sumBarycenter = 0;
        int countRelevantAdjacencies = 0;
        for (SortingNode adjacentNode : adjacentNodesOnAdjacentLayer) {
            if (currentValues.containsKey(adjacentNode)) {
                sumBarycenter += currentValues.get(adjacentNode);
                ++countRelevantAdjacencies;
            }
        }
        return sumBarycenter / (double) countRelevantAdjacencies;
    }

    private void reorderPortParingsAndTurningDummies(int rank, boolean upwards) {
        if (method.equals(CrossingMinimizationMethod.VERTICES)) {
            return; //do nothing here
        }

        for (Vertex node : orders.getNodeOrder().get(rank)) {
            if (considerPortsOfNode(node)) {
                if (sugy.isDummyTurningNode(node)) {
                    sortPortsAtTurningDummy(node, false);
                }
                else {
                    //move dummy turning points to the outsides
                    if (movePortsAdjToTurningDummiesToTheOutside) {
                        movePortsAdjacentToDummyTurningPoints(node, orders.getNodeOrder().get(rank), orders.getBottomPortOrder(),
                                orders.getTopPortOrder());
                    }

                    List<Port> lastReOrderedPorts =
                            upwards ? orders.getBottomPortOrder().get(node) : orders.getTopPortOrder().get(node);
                    //handle port pairings
                    if (sugy.isPlug(node)) {
                        List<Port> otherSidePorts = upwards ? new LinkedList<>(orders.getTopPortOrder().get(node)) :
                                new LinkedList<>(orders.getBottomPortOrder().get(node));
                        List<Port> pairedPortsOtherSide = new ArrayList<>(otherSidePorts.size());
                        //find paired ports
                        for (Port port : lastReOrderedPorts) {
                            Port pairedPort = sugy.getPairedPort(port);
                            if (pairedPort != null) {
                                pairedPortsOtherSide.add(pairedPort);
                            }
                        }
                        //remove them from the port ordering of the other side
                        otherSidePorts.removeAll(pairedPortsOtherSide);
                        //re-insert the paired ports from the other side at the same index as their counterparts at
                        // this side
                        for (int i = 0; i < lastReOrderedPorts.size(); i++) {
                            Port pairedPort = sugy.getPairedPort(lastReOrderedPorts.get(i));
                            if (pairedPort != null) {
                                otherSidePorts.add(Math.min(i, otherSidePorts.size()), pairedPort);
                            }
                        }
                        //save new order
                        if (upwards) {
                            orders.getTopPortOrder().replace(node, otherSidePorts);
                        } else {
                            orders.getBottomPortOrder().replace(node, otherSidePorts);
                        }
                    }
                }
            }
        }
    }

    /**
     * wrapper for {@link Port} or {@link Vertex}, depending on which is used for sorting everything
     */
    private static class SortingNode {
        private Port port;
        private Vertex vertex;
        private boolean representsPort;

        SortingNode(Port port) {
            this.port =port;
            this.representsPort = true;
        }

        public SortingNode(Vertex vertex) {
            this.vertex = vertex;
            this.representsPort = false;
        }

        public Port getPort() {
            return port;
        }

        public Vertex getVertex() {
            return vertex;
        }

        public boolean representsPort() {
            return representsPort;
        }

        /**
         * is either {@link Port} or {@link Vertex}
         *
         * @return
         */
        public Object getStoredObject() {
            return port != null ? port : vertex;
        }
    }
}