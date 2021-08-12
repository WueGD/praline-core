package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import org.eclipse.elk.core.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JZ 2020/06/06:
 * copied from original class CrossingMinimization to remove duplicate code,
 * add more variants and handle mixed cases of vertices with some but not all ports being in a port pairing.
 *
 * In the meantime it became a completely new class.
 * Hardly anything similar to its original class.
 *
 * JZ 2020/08/05:
 * copied again from original class to remove HashMaps and speed it up
 */
public class CrossingMinimization2 {

    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.MIXED;
    public static final boolean DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE = true;
    public static final boolean DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX = true;
    public static final HandlingDeadEnds DEFAULT_HANDLING_DEAD_ENDS = HandlingDeadEnds.PREV_RELATIVE_POSITIONS;

    private SugiyamaLayouter sugy;
    private List<List<SortingNode>> layers; //for CrossingMinimizationMethod VERTICES: one per layer, otherwise: one for
    // bottom, one for top layer
    private Set<Vertex> adjacentToDummyTurningPoints;
    private int maxRank;
    private int numberOfCrossings;
    private CrossingMinimizationMethod method;
    private boolean movePortsAdjToTurningDummiesToTheOutside;
    private boolean placeTurningDummiesNextToTheirVertex;
    private SortingOrder originalOrders;

    public CrossingMinimization2(SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean handlePortPairings) {
        return layerSweepWithBarycenterHeuristic(method, orders, true,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, handlePortPairings, DEFAULT_HANDLING_DEAD_ENDS);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean randomStartPermutation,
                                                          boolean handlePortPairings) {
        return layerSweepWithBarycenterHeuristic(method, orders, randomStartPermutation,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, handlePortPairings, DEFAULT_HANDLING_DEAD_ENDS);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean randomStartPermutation,
                                                          boolean movePortsAdjToTurningDummiesToTheOutside,
                                                          boolean placeTurningDummiesNextToTheirVertex,
                                                          boolean handlePortPairings,
                                                          HandlingDeadEnds handlingDeadEnds) {
        //init
        this.originalOrders = orders;
        initialize(method, new SortingOrder(orders), randomStartPermutation, movePortsAdjToTurningDummiesToTheOutside,
                placeTurningDummiesNextToTheirVertex);

        //do sweeping and allow movement of ports
        doSweeping(handlePortPairings, true, handlingDeadEnds, method != CrossingMinimizationMethod.VERTICES, originalOrders);
        SortingOrder ordersAfterFirstSweeping = transformSortingNodes2SortingOrder(layers,
                method == CrossingMinimizationMethod.VERTICES, originalOrders);
        //do several iterations because these 2 steps influence each other
        int iterations = 2;
        for (int i = 0; i < iterations; i++) {
            handleTurningVerticesFinally(ordersAfterFirstSweeping, true);
            orderPortsFinally(ordersAfterFirstSweeping, i % 2 == 0, i == iterations - 1, handlePortPairings, false);
        }
//      handleTurningVerticesFinally(true);

        //do sweeping while fixing the order of ports, this also eliminates multiple crossings of the same pair of edges
        initSortingNodes(ordersAfterFirstSweeping, true);
        doSweeping(false, false, handlingDeadEnds, false, ordersAfterFirstSweeping);
        SortingOrder finalOrders = transformSortingNodes2SortingOrder(layers, true, ordersAfterFirstSweeping);
        iterations = 2;
        for (int i = 0; i < iterations; i++) {
            orderPortsFinally(finalOrders, i % 2 == 0, true, false, true);
        }


        return finalOrders;
    }

    private void initialize(CrossingMinimizationMethod method, SortingOrder orders, boolean randomStartPermutation,
                            boolean movePortsAdjToTurningDummiesToTheOutside,
                            boolean placeTurningDummiesNextToTheirVertex) {
        if (method == null) {
            this.method = DEFAULT_CROSSING_MINIMIZATION_METHOD;
        }
        this.method = method;
        this.movePortsAdjToTurningDummiesToTheOutside = movePortsAdjToTurningDummiesToTheOutside;
        this.placeTurningDummiesNextToTheirVertex = placeTurningDummiesNextToTheirVertex;

        this.adjacentToDummyTurningPoints = new LinkedHashSet<>();
        maxRank = sugy.getMaxRank();
        this.layers = new ArrayList<>(
                orders.getNodeOrder().size() * (method == CrossingMinimizationMethod.VERTICES ? 1 : 2));




        if (randomStartPermutation) {
            //compute random start position for each layer
            for (List<Vertex> layer : orders.getNodeOrder()) {
                Collections.shuffle(layer, Constants.random);
            }

            //shuffle ports
            orders.shufflePorts();
        }
        //place dummy turning points close to their vertices
        if (this.placeTurningDummiesNextToTheirVertex) {
            placeTurningDummiesNextToTheirVertices(orders);
        }

        //init layers with sorting nodes; use the current orders
        initSortingNodes(orders, method == CrossingMinimizationMethod.VERTICES);
    }

    private void initSortingNodes(SortingOrder orders, boolean ignorePorts) {
        this.layers.clear();
        for (int rank = 0; rank < orders.getNodeOrder().size(); rank++) {
            List<Vertex> baseLayer = orders.getNodeOrder().get(rank);

            List<SortingNode> bottomLayer = new ArrayList<>();
            List<SortingNode> topLayer = ignorePorts ? null : new ArrayList<>();

            for (Vertex node : baseLayer) {
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

                // initialize SortingNodes
                if (ignorePorts) {
                    bottomLayer.add(new SortingNode(node));
                }
                else {
                    if (considerPortsOfNode(node)) {
                        List<Port> bottomPorts = orders.getBottomPortOrder().get(node);
                        List<Port> topPorts = orders.getTopPortOrder().get(node);
                        for (Port bottomPort : bottomPorts) {
                            SortingNode sortingNode = new SortingNode(bottomPort);
                            sortingNode.setPortCountOnVertexSide(bottomPorts.size());
                            bottomLayer.add(sortingNode);
                        }
                        for (Port topPort : topPorts) {
                            SortingNode sortingNode = new SortingNode(topPort);
                            sortingNode.setPortCountOnVertexSide(topPorts.size());
                            topLayer.add(sortingNode);
                        }
                        //special case: a side of a vertex does not have ports -> also create a sorting node for the vertex
                        if (bottomPorts.isEmpty()) {
                            bottomLayer.add(new SortingNode(node));
                        }
                        if (topPorts.isEmpty()) {
                            topLayer.add(new SortingNode(node));
                        }
                    }
                    else {
                        SortingNode sortingNode = new SortingNode(node);
                        bottomLayer.add(sortingNode);
                        topLayer.add(sortingNode);
                    }
                }
            }

            //find neighbors between adjacent layers (the prev top layer and the curr bottom layer)
            if (!this.layers.isEmpty()) {
                findEdgesBetweenLayers(this.layers.get(this.layers.size() - 1), bottomLayer);
            }

            //find port pairings between bottom and top layer
            if (!ignorePorts) {
                findPortPairingsBetweenLayers(bottomLayer, topLayer);
            }

            //add the newly determined layers to all layers
            this.layers.add(bottomLayer);
            if (topLayer != null) {
                this.layers.add(topLayer);
            }
        }

        //update current values acc to type
        for (int i = 0; i < layers.size(); i++) {
            updateCurrentPositions(i);
        }
    }

    private void findEdgesBetweenLayers(List<SortingNode> lowerTopLayer, List<SortingNode> upperBottomLayer) {
        for (SortingNode upperNode : upperBottomLayer) {
            Collection<Edge> edgesUpperNode = getIncidentEdges(upperNode);

            for (Edge edge : edgesUpperNode) {
                Port port0 = edge.getPorts().get(0);
                Port port1 = edge.getPorts().get(1);
                Vertex vertex0 = port0.getVertex();
                Vertex vertex1 = port1.getVertex();
                for (SortingNode lowerNode : lowerTopLayer) {
                    if (lowerNode.representsPort()) {
                        if (lowerNode.getPort().equals(port0) || lowerNode.getPort().equals(port1)) {
                            upperNode.getNeighborsBelow().add(lowerNode);
                            lowerNode.getNeighborsAbove().add(upperNode);
                        }
                    }
                    else {
                        if (lowerNode.getVertex().equals(vertex0) || lowerNode.getVertex().equals(vertex1)) {
                            upperNode.getNeighborsBelow().add(lowerNode);
                            lowerNode.getNeighborsAbove().add(upperNode);
                        }
                    }
                }
            }
        }
    }

    private void findPortPairingsBetweenLayers(List<SortingNode> bottomLayer, List<SortingNode> topLayer) {
        for (SortingNode bottomNode : bottomLayer) {
            if (bottomNode.representsPort()) {
                Port bottomPort = bottomNode.getPort();
                Port topPort = sugy.isPaired(bottomPort) ? sugy.getPairedPort(bottomPort) : null;
                if (topPort != null) {
                    for (SortingNode topNode : topLayer) {
                        if (topNode.representsPort() && topNode.getPort().equals(topPort)) {
                            topNode.setPairedPortSortingNode(bottomNode);
                            bottomNode.setPairedPortSortingNode(topNode);
                        }
                    }
                }
            }
        }
    }

    private Collection<Edge> getIncidentEdges(SortingNode sortingNode) {
        Collection<Edge> edges = null;
        if (sortingNode.representsPort()) {
            edges = sortingNode.getPort().getEdges();
        }
        else {
            edges = PortUtils.getEdges(sortingNode.getVertex());
        }
        return edges;
    }

    private void doSweeping(boolean handlePortPairings, boolean allowPortPermuting, HandlingDeadEnds handlingDeadEnds,
                            boolean sortingNodesCanRepresentPorts, SortingOrder referenceOrders) {
        List<List<SortingNode>> lastStepLayers;

        numberOfCrossings = Integer.MAX_VALUE;
        int currentIteration = 0;
        boolean hasChanged = true;
        while (hasChanged) {

            //save lastStepOrders for check
            lastStepLayers = new ArrayList<>();
            for (List<SortingNode> layer : layers) {
                lastStepLayers.add(new ArrayList<>(layer));
            }

            // as long as some orders change iterate from top to bottom and back to top over all ranks
            // reorder all nodes of a rank corresponding to the result of calculateXCoordinates()
            for (int directedRank = (1 - maxRank); directedRank <= maxRank; directedRank++) {

                int rank = Math.abs(directedRank);
                boolean upwards = directedRank > 0;
                int indexCurrentLayer = (sortingNodesCanRepresentPorts ? 2 : 1) * rank
                        + (upwards || !sortingNodesCanRepresentPorts ? 0 : 1);
                List<SortingNode> currentLayer = layers.get(indexCurrentLayer);
                List<SortingNode> currentLayerWithEdges = new ArrayList<>(currentLayer.size());
                int indexPrevLayer = indexCurrentLayer + (upwards ? -1 : 1);
                List<SortingNode> adjacentPreviousLayer = layers.get(indexPrevLayer);
                int indexNextLayer = indexCurrentLayer + (sortingNodesCanRepresentPorts ? 2 : 1) * (upwards ? 1 : -1);
                List<SortingNode> adjacentNextLayer = handlingDeadEnds == HandlingDeadEnds.BY_OTHER_SIDE ?
                        indexNextLayer<0 || maxRank <= indexNextLayer ? null : layers.get(indexNextLayer) : null;

                List<SortingNode> deadEnds = new ArrayList<>(); //nodes without edges in the considered direction
                List<Integer> currPositionsOfDeadEnds = new ArrayList<>();
                for (int i = 0; i < currentLayer.size(); i++) {
                    SortingNode node = currentLayer.get(i);
                    //barycenter in considered direction
                    double barycenter = getBarycenter(node, upwards);
                    if (Double.isNaN(barycenter)) {
                        deadEnds.add(node);
                        currPositionsOfDeadEnds.add(i);
                    } else {
                        currentLayerWithEdges.add(node);
                    }
                    if (handlingDeadEnds == HandlingDeadEnds.BY_OTHER_SIDE) {
                        //we use barycenters from the "wrong" direction as a second criterion for nodes that don't have
                        // edges in the considered direction.
                        //we don't consider the barycenter in wrong direction if it is an outer layer or if it is the
                        // very first top-down swipe
                        if (adjacentNextLayer == null || (currentIteration == 0 && directedRank <= 0)) {
                            //special case: if dead end in current direction and this is the last layer, we use the current
                            // position as barycenter
                            for (SortingNode deadEnd : new ArrayList<>(deadEnds)) {
                                barycenter = (double) currentLayer.indexOf(node) / (double) (currentLayer.size()) *
                                        (double) (adjacentPreviousLayer.size() - 1);
                                currentLayerWithEdges.add(deadEnd);
                                deadEnds.remove(deadEnd);
                            }
                        } else {
                            //barycenter in wrong direction
                            double barycenterFromOtherSide = getBarycenter(node, !upwards);
                            if (Double.isNaN(barycenterFromOtherSide)) {
                                //special case: if dead end in wrong direction, use current position as barycenter
                                barycenterFromOtherSide =
                                        (double) currentLayer.indexOf(node) / (double) (currentLayer.size()) *
                                                (double) (adjacentNextLayer.size() - 1);
                            }
                            //save barycenter from other side
                            node.setCurrentBarycenterFromOtherSide(barycenterFromOtherSide);
                        }
                    }
                    //save barycenter
                    node.setCurrentBarycenter(barycenter);
                }
                currentLayerWithEdges.sort(Comparator.comparingDouble(SortingNode::getCurrentBarycenter));

                List<SortingNode> combinedOrder;
                if (!deadEnds.isEmpty()) {
                    //best: integrateDeadEndsViaOldRelativePosition, worst: integrateDeadEndsViaBarycentersFromOtherSide
                    if (handlingDeadEnds == HandlingDeadEnds.PSEUDO_BARYCENTERS) {
                        combinedOrder = integrateDeadEndsViaPseudoBarycenters(currentLayer, deadEnds,
                                currPositionsOfDeadEnds, currentLayer.size());
                    }
                    else if (handlingDeadEnds == HandlingDeadEnds.PREV_RELATIVE_POSITIONS) {
                        combinedOrder = integrateDeadEndsViaOldRelativePosition(currentLayerWithEdges, deadEnds,
                                currPositionsOfDeadEnds);
                    }
                    else { //handlingDeadEnds == HandlingDeadEnds.BY_OTHER_SIDE
                        combinedOrder = integrateDeadEndsViaBarycentersFromOtherSide(currentLayer,
                                currentLayerWithEdges, deadEnds);
                    }
                } else {
                    //if there are no dead ends, just take the order computed before
                    combinedOrder = currentLayerWithEdges;
                }

                layers.set(indexCurrentLayer, combinedOrder);

                updateCurrentPositions(indexCurrentLayer);
                if (sortingNodesCanRepresentPorts) {
                    updateVerticesAndPortsOrder(indexCurrentLayer, upwards, allowPortPermuting);
                    updateCurrentPositions(indexCurrentLayer);
                    updateCurrentPositions(indexCurrentLayer + (upwards ? 1 : -1));
                    if (handlePortPairings && allowPortPermuting) {
                        reorderPortParingsAndTurningDummies(indexCurrentLayer, upwards);
                        updateCurrentPositions(indexCurrentLayer);
                        updateCurrentPositions(indexCurrentLayer + (upwards ? 1 : -1));
                    }
                }
            }

            hasChanged = checkIfHasChanged(lastStepLayers, currentIteration, handlePortPairings && allowPortPermuting
                    , allowPortPermuting, !sortingNodesCanRepresentPorts, referenceOrders);
            ++currentIteration;
        }
    }

    private List<SortingNode> integrateDeadEndsViaOldRelativePosition(List<SortingNode> currentLayerWithEdges,
                                                                      List<SortingNode> deadEnds,
                                                                      List<Integer> currPositionsOfDeadEnds) {
        /*
        this is an alternative for integrateDeadEndsViaBarycentersFromOtherSide because the latter seems to work bad in
        practice unfortunately :(

        here, if a node without edges in the considered direction was the k-th element before, we will keep it as the
         k-th element. This also alternative to computing a barycenter for those edges just from its old position,
         i.e., k, normalized by the size of the adjacent previous layer
         */

        List<SortingNode> combinedOrder = new ArrayList<>(currentLayerWithEdges.size() + deadEnds.size());
        Iterator<SortingNode> nodesWithEdgesIterator = currentLayerWithEdges.iterator();
        int indexDeadEnds = 0;
        for (int i = 0; i < currentLayerWithEdges.size() + deadEnds.size(); i++) {
            if (indexDeadEnds < deadEnds.size() && currPositionsOfDeadEnds.get(indexDeadEnds) == i) {
                combinedOrder.add(deadEnds.get(indexDeadEnds));
                ++indexDeadEnds;
            }
            else {
                combinedOrder.add(nodesWithEdgesIterator.next());
            }
        }
        return combinedOrder;
    }

    private List<SortingNode> integrateDeadEndsViaPseudoBarycenters(List<SortingNode> currentLayer, List<SortingNode> deadEnds,
                                                                    List<Integer> currPositionsOfDeadEnds,
                                                                    int sizeAdjacentPreviousLayer) {

        /*
        For comparison reasons another alternative to integrateDeadEndsViaOldRelativePosition().
        We compute a pseudo barycenter as the current position normalized by the sizes of this layer and the adjacent
        previous layer.
        It was the old default before this step of the crossing minimization phase was tackled.
        However it works worse.
         */

        int sizeCurrentLayer = currentLayer.size();
        for (int i = 0; i < deadEnds.size(); i++) {
            SortingNode deadEnd = deadEnds.get(i);
            double pseudoBarycenter = (double) currPositionsOfDeadEnds.get(i) /
                    (double) (sizeCurrentLayer) * (double) (sizeAdjacentPreviousLayer - 1);
            deadEnd.setCurrentBarycenter(pseudoBarycenter);
        }

        currentLayer.sort(Comparator.comparingDouble(SortingNode::getCurrentBarycenter));
        return currentLayer;
    }


    //seems to work badly... so this was taken out again
    private List<SortingNode> integrateDeadEndsViaBarycentersFromOtherSide(List<SortingNode> currentLayer,
                                                                           List<SortingNode> currentLayerWithEdges,
                                                                           List<SortingNode> deadEnds) {

        /*
        In currentLayerWithEdges, there are all nodes of this layer except for the dead ends.
        Next we have to re-insert the dead ends at "good" positions between these entries.
        For this, we use the barycenters from the other side!
        Each node in currentLayerWithEdges has such a value. The intermediate insertion positions get a value
        computed by the weighted average of the barycenterFromOtherSides-entries of the nodes in
        currentLayerWithEdges. The weight is (1/2)^d, where d is the distance to the insertion position
        (minimum is d=0, so weight = 1, then weight 1/2, weight 1/4 and so on).
        If we would compute this value using all entries, we need quadratic time. We speed this up by considering
        only k neighbors to the left and to the right (eg k=10) and then this can be done in linear time.

        For all dead ends we have a barycenterFromTheOtherSide. We take that value and compare it to the
        potential insertion position values. We insert a dead end at the place it has the minimum difference.
        For multiple dead ends at the same position, we sort them in- or decreasingly depending on the size
        of the previous/next insertion position.
         */

        List<Double> currentLayerBarycentersFromOtherSide = new ArrayList<>(currentLayerWithEdges.size());
        for (SortingNode node : currentLayerWithEdges) {
            currentLayerBarycentersFromOtherSide.add(node.getCurrentBarycenterFromOtherSide());
        }
        int k = Math.min(10, currentLayerBarycentersFromOtherSide.size()); //number of neighborhood entries to be checked
        int ell = currentLayerWithEdges.size() + 1; //number of insertion positions
        List<Double> insertionPositionValues = new ArrayList<>(ell);
        for (int i = 0; i < ell; i++) {
            double sum = 0; //s_i (value of the insertion position) to be computed
            for (int j = Math.max(0, i - k); j < Math.min(ell - 1, i + k); j++) {
                sum += currentLayerBarycentersFromOtherSide.get(j) * w(i, j, k) * b(i, j, ell);
            }
            insertionPositionValues.add(sum);
        }
        //now find insertion positions for dead ends
        Map<Integer, List<SortingNode>> insertionPosition2deadEnd = new LinkedHashMap<>();
        for (SortingNode deadEnd : deadEnds) {
            double barycenterFromOtherSide = deadEnd.getCurrentBarycenterFromOtherSide();
            //find min difference
            double minDiff = Double.POSITIVE_INFINITY;
            int idealInsertionPosition = 0;
            for (int i = 0; i < ell; i++) {
                double diff = Math.abs(barycenterFromOtherSide - insertionPositionValues.get(i));
                if (diff < minDiff) {
                    minDiff = diff;
                    idealInsertionPosition = i;
                }
            }
            //remember for ideal insertion position
            insertionPosition2deadEnd.putIfAbsent(idealInsertionPosition, new ArrayList<>());
            insertionPosition2deadEnd.get(idealInsertionPosition).add(deadEnd);
        }
        //now combine currentLayerBarycentersFromOtherSide with dead ends
        List<SortingNode> combinedOrder = new ArrayList<>(currentLayer.size());
        for (int i = 0; i < ell; i++) {
            List<SortingNode> deadEndsAtI = insertionPosition2deadEnd.get(i);
            if (deadEndsAtI != null) {
                if (deadEndsAtI.size() > 1) {
                    //sort it a- or descending depending on the neighboring entries of
                    // currentLayerBarycentersFromOtherSide
                    int startIndex = i == 0 ? 0 : i == ell - 1 ? ell - 3 : i - 1;
                    boolean ascending = (currentLayerBarycentersFromOtherSide.size() <= 1) ||
                            (currentLayerBarycentersFromOtherSide.get(startIndex) <
                                    currentLayerBarycentersFromOtherSide.get(startIndex + 1));
                    if (ascending) {
                        deadEndsAtI.sort(Comparator.comparingDouble(SortingNode::getCurrentBarycenterFromOtherSide));
                    } else {
                        deadEndsAtI.sort(Comparator.comparingDouble(SortingNode::getCurrentBarycenterFromOtherSide).reversed());
                    }
                }
                combinedOrder.addAll(deadEndsAtI);
            }
            if (i < ell - 1) {
                combinedOrder.add(currentLayerWithEdges.get(i));
            }
        }
        return combinedOrder;
    }

    /**
     *
     * @param i
     * @param j
     * @param k
     * @return
     *      weight of this entry
     */
    private double w(int i, int j, int k) {
        return Math.pow(2.0, (k - d(i, j) - 2)) / (Math.pow(2.0, k) - 1);
    }

    /**
     *
     * @param i
     * @param j
     * @return
     *      distance between i and j with different behavior for j < i and i <= j
     */
    private double d(int i, int j) {
        if (j < i) {
            return (double) (i - j - 1);
        }
        return (double) (j - i);
    }

    /**
     *
     * @param i
     * @param j
     * @param ell
     * @return
     *      whether with same distance as j to i but on the other side of i exists (return 1.0 if yes, return 2.0 if no)
     */
    private double b(int i, int j, int ell) {
        if (2 * i - j - 1 >= 0 && 2 * i - j < ell) {
            return 1.0;
        }
        return 2.0;
    }

    private void placeTurningDummiesNextToTheirVertices(SortingOrder orders) {
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

    private void updateCurrentPositions(int indexLayer) {
        List<SortingNode> layer = layers.get(indexLayer);
        if (method.equals(CrossingMinimizationMethod.VERTICES) || method.equals(CrossingMinimizationMethod.PORTS)) {
            for (int i = 0; i < layer.size(); i++) {
                layer.get(i).setCurrentPosition((double) i);
            }
        }
        else if (method.equals(CrossingMinimizationMethod.MIXED)) {
            updateFractionalPortOrderPositions(layer);
        }
    }

    private void updateFractionalPortOrderPositions(List<SortingNode> layer) {
        Vertex previouslyRepresentedVertex = null;
        int counterPortsOfThisVertex = 0;
        int vertexCount = -1; //counts the number of "real" vertices
        for (SortingNode node : layer) {
            if (node.representsPort()) {
                Vertex representedVertex = node.getPort().getVertex();
                if (!representedVertex.equals(previouslyRepresentedVertex)) {
                    counterPortsOfThisVertex = 0;
                    ++vertexCount;
                }

                node.setCurrentPosition(-0.5 + (double) vertexCount +
                        ((double) (++counterPortsOfThisVertex) / node.getPortCountOnVertexSide()));

                previouslyRepresentedVertex = representedVertex;
            } else {
                ++vertexCount;
                node.setCurrentPosition((double) vertexCount);
            }
        }
    }

    private boolean considerPortsOfNode(Vertex node) {
        return method.equals(CrossingMinimizationMethod.PORTS)
                || (method.equals(CrossingMinimizationMethod.MIXED)
                && (sugy.isPlug(node) || sugy.isDummyTurningNode(node)
                    || this.adjacentToDummyTurningPoints.contains(node)));
    }

    /**
     * Re-sorts {@link SortingNode}s so that ports obey the neighborhoods according to vertices and port groups.
     * No need to call this method if {@link CrossingMinimizationMethod#VERTICES} is selected.
     * The upper/lower side of the same layer (in layers index +1 or -1 depending on whether upwards is true or false)
     * will also be sorted acc to the just found vertex ordering.
     *
     * @param indexCurrentLayer
     * @param upwards
     * @param allowPortMovement
     */
    private void updateVerticesAndPortsOrder(int indexCurrentLayer, boolean upwards, boolean allowPortMovement) {
        List<SortingNode> currentLayer = layers.get(indexCurrentLayer);
        //bring vertices together acc. to the previously computed barycenters
        List<Vertex> verticesOfLayer = new ArrayList<>();
        Map<Vertex, Pair<Double, List<SortingNode>>> vertex2BarycenterAndSortingNodes = new LinkedHashMap<>();
        for (int i = 0; i < currentLayer.size(); i++) {
            SortingNode node = currentLayer.get(i);
            Object nodeObject = node.getStoredObject();
            if (nodeObject instanceof Vertex) {
                verticesOfLayer.add((Vertex) nodeObject);
                vertex2BarycenterAndSortingNodes.put((Vertex) nodeObject, new Pair<>((double) i,
                        Collections.singletonList(node)));
            } else if (nodeObject instanceof Port) {
                Vertex vertex = ((Port) nodeObject).getVertex();
                if (!vertex2BarycenterAndSortingNodes.containsKey(vertex)) {
                    verticesOfLayer.add(vertex);
                    vertex2BarycenterAndSortingNodes.put(vertex, new Pair<>(0.0,
                            new ArrayList<>(vertex.getPorts().size())));
                }
                Pair<Double, List<SortingNode>> pair = vertex2BarycenterAndSortingNodes.get(vertex);
                pair.setFirst(pair.getFirst() + (double) i);
                pair.getSecond().add(node);
            }
        }
        //re-sort vertices
        verticesOfLayer.sort(Comparator.comparingDouble(v -> vertex2BarycenterAndSortingNodes.get(v).getFirst() /
                (double) vertex2BarycenterAndSortingNodes.get(v).getSecond().size()));

        //combine SortingNodes acc to just found vertex order
        currentLayer.clear();
        for (Vertex vertex : verticesOfLayer) {
            List<SortingNode> nodesOfVertex = vertex2BarycenterAndSortingNodes.get(vertex).getSecond();
            //re-sort acc to ports
            if (allowPortMovement && nodesOfVertex.get(0).representsPort()) {
                sortAccordingToPortGroups(nodesOfVertex, vertex, !upwards);
            }
            currentLayer.addAll(nodesOfVertex);
        }

        //sort neighboring layer with same vertices
        //find corresponding nodes
        List<SortingNode> twinLayer = layers.get(indexCurrentLayer + (upwards ? 1 : -1));
        Map<Vertex, List<SortingNode>> vertex2sortingNodesTwinLayer = new LinkedHashMap<>();
        for (int i = 0; i < twinLayer.size(); i++) {
            SortingNode node = twinLayer.get(i);
            Object nodeObject = node.getStoredObject();
            if (nodeObject instanceof Vertex) {
                vertex2sortingNodesTwinLayer.put((Vertex) nodeObject, Collections.singletonList(node));
            } else if (nodeObject instanceof Port) {
                Vertex vertex = ((Port) nodeObject).getVertex();
                if (!vertex2sortingNodesTwinLayer.containsKey(vertex)) {
                    vertex2sortingNodesTwinLayer.put(vertex, new ArrayList<>(vertex.getPorts().size()));
                }
                vertex2sortingNodesTwinLayer.get(vertex).add(node);
            }
        }
        //re-arrange them
        twinLayer.clear();
        for (Vertex vertex : verticesOfLayer) {
            twinLayer.addAll(vertex2sortingNodesTwinLayer.get(vertex));
        }
    }

    /**
     * we assume that all these sorting nodes belong to ports of the same vertex (specified in parameter vertex)
     *
     * @param nodesOfVertex
     * @param vertex
     * @return
     */
    private void sortAccordingToPortGroups(List<SortingNode> nodesOfVertex, Vertex vertex, boolean topSide) {
        Map<PortComposition, Pair<Double, List<SortingNode>>> pc2BarycenterAndSortingNodes = new LinkedHashMap<>();
        for (int i = 0; i < nodesOfVertex.size(); i++) {
            SortingNode node = nodesOfVertex.get(i);
            Port port = node.getPort();
            pc2BarycenterAndSortingNodes.put(port, new Pair<>((double) i, Collections.singletonList(node)));
            PortGroup portGroup = port.getPortGroup();
            //find for all parent port groups the barycenter and save it in the map
            while (portGroup != null) {
                if (!pc2BarycenterAndSortingNodes.containsKey(portGroup)) {
                    pc2BarycenterAndSortingNodes.put(portGroup, new Pair<>(0.0, new ArrayList<>()));
                }
                Pair<Double, List<SortingNode>> pair = pc2BarycenterAndSortingNodes.get(portGroup);
                pair.setFirst(pair.getFirst() + (double) i);
                pair.getSecond().add(node);
                portGroup = portGroup.getPortGroup(); //go one level up
            }
        }

        //now sort port groups recursively
        nodesOfVertex.clear();
        sortRecursively(nodesOfVertex, new ArrayList<>(topSide ? originalOrders.getTopPortOrder().get(vertex) :
                originalOrders.getBottomPortOrder().get(vertex)), pc2BarycenterAndSortingNodes);
    }

    private void sortRecursively(List<SortingNode> appendList, List<PortComposition> portCompositions,
                                 Map<PortComposition, Pair<Double, List<SortingNode>>> pc2BarycenterAndSortingNodes) {
        portCompositions.sort(Comparator.comparingDouble(v -> pc2BarycenterAndSortingNodes.get(v).getFirst() /
                (double) pc2BarycenterAndSortingNodes.get(v).getSecond().size()));
        for (PortComposition portComposition : portCompositions) {
            if (portComposition instanceof Port) {
                appendList.add(pc2BarycenterAndSortingNodes.get(portComposition).getSecond().get(0));
            }
            else if (portComposition instanceof PortGroup) {
                sortRecursively(appendList,new ArrayList<>(((PortGroup) portComposition).getPortCompositions()),
                        pc2BarycenterAndSortingNodes);
            }
        }
    }

    private boolean checkIfHasChanged(List<List<SortingNode>> lastStepLayers, int currentIteration,
                                      boolean handlePortPairings, boolean allowPortPermuting, boolean ignorePorts,
                                      SortingOrder referenceOrders) {
        // todo: number of iterations is calculated arbitrarily - adapt if necessary - lower in case of runtime issues
        int numberOfIterations = layers.size() / (ignorePorts ? 1 : 2);
        boolean hasChanged = false;
        if (currentIteration < numberOfIterations) {
            // check for changes in the structure
            for (int rank = 0; (rank <= maxRank) && !hasChanged; rank++) {
                for (int i = 0; (i < layers.get(rank).size()) && !hasChanged; i++) {
                    SortingNode node = layers.get(rank).get(i);
                    try {
                        if (!lastStepLayers.get(rank).get(i).equals(node)) hasChanged = true;
                    }
                    catch (Exception e) {
                        System.out.println("asdf");
                    }
                }
            }
        } else {
            // check for changes in number of crossings due to possibility of an endless loop
            // todo: in case of runtime issues change to more intelligent counting method
            SortingOrder currentLayersCopy = transformSortingNodes2SortingOrder(layers, ignorePorts, referenceOrders);
            orderPortsFinally(currentLayersCopy, currentLayersCopy.getTopPortOrder(),
                    currentLayersCopy.getBottomPortOrder(),false, true,
                    handlePortPairings, allowPortPermuting);
            int newNumberOfCrossings = sugy.countCrossings(currentLayersCopy);
            if (newNumberOfCrossings < numberOfCrossings) {
                numberOfCrossings = newNumberOfCrossings;
                hasChanged = true;
            }
        }
        return hasChanged;
    }

    private SortingOrder transformSortingNodes2SortingOrder(List<List<SortingNode>> layers, boolean ignorePorts,
                                                            SortingOrder referenceOrders) {
        SortingOrder orders = new SortingOrder(referenceOrders);
        orders.getNodeOrder().clear(); //we use the newly found node ordering instead
        if (ignorePorts) {
            //for ports we don't have ordering here, just use the old one (do nothing, change only vertex layers)
            for (List<SortingNode> layer : layers) {
                List<Vertex> layerList = new ArrayList<>(layer.size());
                orders.getNodeOrder().add(layerList);
                for (SortingNode sortingNode : layer) {
                    layerList.add(sortingNode.getVertex());
                }
            }
        }
        else {
            //the last sweep is upwards and hence only even indices of layers are relevant for vertex positions
            //the odd indices are only relevant for port orderings on the top side
            for (int i = 0; i < layers.size(); i = i + 2) {
                List<SortingNode> layer = layers.get(i);
                List<Vertex> layerList = new ArrayList<>(layer.size());
                orders.getNodeOrder().add(layerList);
                Vertex lastVertex = null;
                for (SortingNode sortingNode : layer) {
                    Vertex vertex = sortingNode.representsPort() ? sortingNode.getPort().getVertex() :
                            sortingNode.getVertex();
                    if (!vertex.equals(lastVertex)) {
                        layerList.add(vertex);
                        lastVertex = vertex;
                    }
                }
            }
            //it remains to determine the port order of a vertex; for that go through the layers again
            //find bottom port ordering in layers with even index
            for (int i = 0; i < layers.size(); i = i + 2) {
                List<SortingNode> layer = layers.get(i);
                Vertex lastVertex = null;
                for (SortingNode sortingNode : layer) {
                    if (sortingNode.representsPort()) {
                        Vertex vertex = sortingNode.getPort().getVertex();
                        if (!vertex.equals(lastVertex)) {
                            orders.getBottomPortOrder().get(vertex).clear();
                            lastVertex = vertex;
                        }
                        orders.getBottomPortOrder().get(vertex).add(sortingNode.getPort());
                    }
                }
            }
            //find top port ordering in layers with odd index
            for (int i = 1; i < layers.size(); i = i + 2) {
                List<SortingNode> layer = layers.get(i);
                Vertex lastVertex = null;
                for (SortingNode sortingNode : layer) {
                    if (sortingNode.representsPort()) {
                        Vertex vertex = sortingNode.getPort().getVertex();
                        if (!vertex.equals(lastVertex)) {
                            orders.getTopPortOrder().get(vertex).clear();
                            lastVertex = vertex;
                        }
                        orders.getTopPortOrder().get(vertex).add(sortingNode.getPort());
                    }
                }
            }
        }
        return orders;
    }

    private void handleTurningVerticesFinally(SortingOrder orders, boolean sortPortsAtCorrespondingRealNode) {
        for (List<Vertex> layer : orders.getNodeOrder()) {
            for (Vertex node : layer) {
                if (sugy.isDummyTurningNode(node)) {
                    sortPortsAtTurningDummy(node, orders, sortPortsAtCorrespondingRealNode);
                }
            }
        }
    }

    private void sortPortsAtTurningDummy(Vertex turningDummy, SortingOrder orders,
                                         boolean sortPortsAtCorrespondingRealNode) {
        List<Port> portsBottom = orders.getBottomPortOrder().get(turningDummy);
        List<Port> portsTop = orders.getTopPortOrder().get(turningDummy);
        List<Port> ports = portsBottom != null && !portsBottom.isEmpty() ? portsBottom : portsTop;
        Map<Port, Double> port2adjacentNodePosition = new LinkedHashMap<>();
        for (Port port : ports) {
            Port otherPort = PortUtils.getAdjacentPort(port);
            Vertex otherVertex = otherPort.getVertex();
            double adjacentObjectPosition =
                    (double) orders.getNodeOrder().get(sugy.getRank(otherVertex)).indexOf(otherVertex)
                    + Math.max((double) orders.getBottomPortOrder().get(otherVertex).indexOf(otherPort) /
                    (double) orders.getBottomPortOrder().get(otherVertex).size(),
                    (double) orders.getTopPortOrder().get(otherVertex).indexOf(otherPort) /
                    (double) orders.getTopPortOrder().get(otherVertex).size());
            port2adjacentNodePosition.put(port, adjacentObjectPosition);
        }

        //first sort ports at dummy turning vertex -- this should be unproblematic
        ports.sort(Comparator.comparingDouble(port2adjacentNodePosition::get));

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
        portsAtItsVertex.sort(Comparator.comparingInt(allPortsAtItsVertex::indexOf));

        //now re-sort the ports at this turning dummy going to its corresponding vertex -- this is always possible
        Collections.reverse(portsToDummyEdgeNodesLeft);
        Collections.reverse(portsToDummyEdgeNodesRight);
        int i = 0;
        for (Port port : portsToDummyEdgeNodesLeft) {
            Port currentPortToBeReplaced = portsToItsVertex.get(i++);
            Port targetPort = sugy.getCorrespondingPortAtDummy(port);
            swapPortsIfPossible(ports, currentPortToBeReplaced, targetPort, false, orders);
            Collections.swap(portsToItsVertex, portsToItsVertex.indexOf(currentPortToBeReplaced),
                    portsToItsVertex.indexOf(targetPort));
        }
        for (Port port : portsToDummyEdgeNodesRight) {
            Port currentPortToBeReplaced = portsToItsVertex.get(i++);
            Port targetPort = sugy.getCorrespondingPortAtDummy(port);
            swapPortsIfPossible(ports, currentPortToBeReplaced, targetPort, false, orders);
            Collections.swap(portsToItsVertex, portsToItsVertex.indexOf(currentPortToBeReplaced),
                    portsToItsVertex.indexOf(targetPort));
        }

        //now re-sort at the "real" vertex of this turning dummy accordingly
        if (sortPortsAtCorrespondingRealNode) {
            for (int j = 0; j < portsToItsVertex.size(); j++) {
                Port portAtTurningDummy = portsToItsVertex.get(j);
                Port portAtVertexToBeReplaced = portsAtItsVertex.get(j);
                Port portAtVertexTarget = PortUtils.getAdjacentPort(portAtTurningDummy);
                boolean didSwap = swapPortsIfPossible(allPortsAtItsVertex, portAtVertexToBeReplaced,
                        portAtVertexTarget, true, orders);
                if (didSwap) {
                    Collections.swap(portsAtItsVertex, portsAtItsVertex.indexOf(portAtVertexToBeReplaced),
                            portsAtItsVertex.indexOf(portAtVertexTarget));
                }
            }
        }
    }

    private boolean swapPortsIfPossible(List<Port> ports, Port port0, Port port1, boolean swapPairedPorts,
                                        SortingOrder orders) {
        if ((!sugy.isPaired(port0) && !sugy.isPaired(port1))) {
            if (areInTheSameFreePortGroup(port0, port1)) {
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
                    canSwapOtherSide = areInTheSameFreePortGroup(otherSidePorts.get(0), otherSidePort1);
                }
                else if (indexUnpairedOtherSidePort >= otherSidePorts.size()) {
                    canSwapOtherSide = areInTheSameFreePortGroup(
                            otherSidePorts.get(otherSidePorts.size() - 1), otherSidePort1);
                }
                else {
                    otherSidePort0 = otherSidePorts.get(indexUnpairedOtherSidePort);
                }
            }
            else if (otherSidePort1 == null) {
                indexUnpairedOtherSidePort = otherSidePorts.indexOf(otherSidePort0) + (indexPort1 - indexPort0);
                if (indexUnpairedOtherSidePort < 0) {
                    canSwapOtherSide = areInTheSameFreePortGroup(otherSidePort0, otherSidePorts.get(0));
                }
                else if (indexUnpairedOtherSidePort >= otherSidePorts.size()) {
                    canSwapOtherSide = areInTheSameFreePortGroup(otherSidePort0,
                            otherSidePorts.get(otherSidePorts.size() - 1));
                }
                else {
                    otherSidePort1 = otherSidePorts.get(indexUnpairedOtherSidePort);
                }
            }

            if (otherSidePort0 != null && otherSidePort1 != null) {
                canSwapOtherSide = areInTheSameFreePortGroup(otherSidePort0, otherSidePort1);
            }

            //if we can swap both sides, we do it
            if (canSwapOtherSide && areInTheSameFreePortGroup(port0, port1)) {
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

    private static boolean areInTheSameFreePortGroup(Port port0, Port port1) {
         return PortUtils.areInTheSamePortGroup(port0, port1) &&
                 (port0.getPortGroup() == null || !port0.getPortGroup().isOrdered());
    }

    private void orderPortsFinally(SortingOrder orders, boolean upwards, boolean isFinalSorting,
                                   boolean handlePortPairings, boolean ignoreNodesWithPairings) {
        orderPortsFinally(orders, orders.getTopPortOrder(), orders.getBottomPortOrder(), upwards, isFinalSorting,
                handlePortPairings, ignoreNodesWithPairings);
    }

    private void orderPortsFinally(SortingOrder orders, Map<Vertex, List<Port>> currentTPortOrder,
                                   Map<Vertex, List<Port>> currentBPortOrder,
                                   boolean upwards, boolean isFinalSorting,
                                   boolean handlePortPairings, boolean ignoreNodesWithPairings) {
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
                if (ignoreNodesWithPairings && sugy.isPlug(node)) {
                    continue;
                }
                for (Map<Vertex, List<Port>> currentPortOrderMap : portOrdersToBeSorted) {
                    List<Port> portsOfThisNodeSide = new ArrayList<>(currentPortOrderMap.get(node));
                    orderPorts(portsOfThisNodeSide, orders);
                    portsOfThisNodeSide = orderPortsConstraintToPortGroups(portsOfThisNodeSide,
                            node.getPortCompositions(), true);
                    currentPortOrderMap.replace(node, portsOfThisNodeSide);
                }
                if (handlePortPairings) {
                    boolean allPortPairingsSeparated = false;
                    int maxTries = 20;
                    int tries = 0;
                    do {
                        int iterations = 4;
                        for (int i = 0; i < iterations; i++) {
                            allPortPairingsSeparated =
                                    repairPortPairings(sugy, node, currentBPortOrder, currentTPortOrder,
                                            ((upwards ? 0 : 1) + i) % 2 == 0, isFinalSorting && i == iterations - 1,
                                            true, false, null);
                        }
                    }
                    while (isFinalSorting && !allPortPairingsSeparated && tries++ < maxTries);

                    if (isFinalSorting) {
                        //check port pairings
                        if (!allPortPairingsSeparated) {
                            System.out.println("Warning! No valid arrangement of port pairings found for plug "
                                    + sugy.getPlugs().get(node).getContainedVertices() + ".");
                        }
                        //check port groups
                        List<Port> allPortsCombined = new ArrayList<>(currentBPortOrder.get(node));
                        allPortsCombined.addAll(currentTPortOrder.get(node));
                        if (!PortUtils.arrangmentOfPortsIsValidAccordingToPortGroups(allPortsCombined, node.getPortCompositions())) {
                            System.out.println("Warning! Constraints due to port groups " +
                                    "not completely fulfilled (possibly because of conflicts with port pairings) at plug "
                                    + sugy.getPlugs().get(node).getContainedVertices() + ".");
                        }
                    }
                }
            }
        }
    }

    private void movePortsAdjacentToTurningDummy(Vertex node, List<Vertex> layer,
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

    private void movePortsAdjacentToTurningDummy(List<SortingNode> portNodes, int indexLayers, boolean upwards) {
        Set<SortingNode> alreadySwapped = new LinkedHashSet<>();
        for (int i = 0; i < portNodes.size(); i++) {
            SortingNode portNode = portNodes.get(i);
            //check for every node whether it is a neighbor of a turning dummy
            SortingNode turningDummyNeighbor = getAdjacentTurningDummySortingNode(portNode, upwards);
            if (turningDummyNeighbor != null) {
                //and if so, find out whether we should shift it to the left or to the right
                //that depends on the barycenter of the turning dummy and on the current position of this node
                double barycenterOfTurningDummy = turningDummyNeighbor.representsPort() ?
                        getBarycenter(turningDummyNeighbor.getPort().getVertex(),
                                layers.get(indexLayers + (upwards ? -1 : 1)), !upwards) :
                        getBarycenter(turningDummyNeighbor, !upwards);
                if (barycenterOfTurningDummy < portNode.getCurrentPosition()) {
                    //move left
                    int j = i;
                    while (j > 0 && !alreadySwapped.contains(portNodes.get(j - 1)) && canSwapLeft(j,
                            portNodes.stream().map(SortingNode::getPort).collect(Collectors.toList()))) {
                        swapLeft(j, portNodes);
                        --j;
                    }
                } else {
                    //move right
                    int j = i;
                    while (j < portNodes.size() - 1 && !alreadySwapped.contains(portNodes.get(j + 1)) && canSwapLeft(j,
                            portNodes.stream().map(SortingNode::getPort).collect(Collectors.toList()))) {
                        swapLeft(j, portNodes);
                        --j;
                    }
                }
                alreadySwapped.add(portNode);
            }
        }
    }

    private SortingNode getAdjacentTurningDummySortingNode(SortingNode node, boolean upwards) {
        for (SortingNode neighbor : upwards ? node.getNeighborsBelow() : node.getNeighborsAbove()) {
            Vertex vertexOfNeighbor = neighbor.representsPort() ? neighbor.getPort().getVertex() : neighbor.getVertex();
            if (sugy.isDummyTurningNode(vertexOfNeighbor)) {
                return neighbor;
            }
        }
        return null;
    }


    /**
     * @param sugy
     * @param node
     * @param currentBPortOrder
     * @param currentTPortOrder
     * @param preferredSwapSideTop
     * @param isFinalSorting
     *          If true port groups are not swapped (as this may result in inconsistent states) and only if true, text
     *          output (warnings) is produced.
     * @param allowForceSwapping
     *          sometimes we need to violate port group constraints to fulfil port pairing constraints. Set this to
     *          true to rather violate port groups constraints for accomplishing port pairing constraints.
     *          The default is true.
     * @param portsNeedAbsoluteSameIndex
     *          if true, the paired ports need the same index among all ports on the top/bottom side, otherwise only
     * @param dummyPortsForAbsoluteIndex
     *          if portsNeedAbsoluteSameIndex, it will try to reach the same index by inserting dummy ports.
     *          This can be set to null (and that's the default).
     * @return
     *          confirmed success (it was checked that all port pairings are separated)
     */
    public static boolean repairPortPairings(SugiyamaLayouter sugy, Vertex node, Map<Vertex, List<Port>> currentBPortOrder,
                                             Map<Vertex, List<Port>> currentTPortOrder, boolean preferredSwapSideTop,
                                             boolean isFinalSorting, boolean allowForceSwapping,
                                             boolean portsNeedAbsoluteSameIndex,
                                             Collection<Port> dummyPortsForAbsoluteIndex) {
        List<Port> bottomOrder = currentBPortOrder.get(node);
        List<Port> topOrder = currentTPortOrder.get(node);

        List<Port> firstOrder = preferredSwapSideTop ? topOrder : bottomOrder;
        List<Port> secondOrder = preferredSwapSideTop ? bottomOrder : topOrder;

        //first find all port pairings
        List<Pair<Port, Port>> allPortPairings = new ArrayList<>(bottomOrder.size());
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
        List<Port> bottomPairingOrder = portsNeedAbsoluteSameIndex ? bottomOrder :
                extractOrderingOfPairedPorts(bottomOrder, allPairedPorts);
        List<Port> topPairingOrder = portsNeedAbsoluteSameIndex ? topOrder :
                extractOrderingOfPairedPorts(topOrder, allPairedPorts);

        List<Port> firstOrderPairedPorts = preferredSwapSideTop ? topPairingOrder : bottomPairingOrder;
        List<Port> secondOrderPairedPorts = preferredSwapSideTop ? bottomPairingOrder : topPairingOrder;

        int dummyPortCounter = 0; //only used for dummyPortsForAbsoluteIndex; see javadoc of this method

        for (int i = 0; i < allPortPairings.size(); i++) {
            Pair<Port, Port> portPairing = allPortPairings.get(i);
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
                           false, !isFinalSorting, forceSwapping, allPairedPorts, portsNeedAbsoluteSameIndex);
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
                                true, false, forceSwapping, allPairedPorts, portsNeedAbsoluteSameIndex);
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
                            forceSwapping = allowForceSwapping;
                            //we may also insert dummy vertices if the absolute order matters
                            if (!forceSwapping && portsNeedAbsoluteSameIndex && dummyPortsForAbsoluteIndex != null) {
                                while (indexFirstPort < indexSecondPort) {
                                    dummyPortCounter = addDummyPort(firstPort, indexFirstPort, false, firstOrder,
                                            dummyPortCounter, dummyPortsForAbsoluteIndex);
                                    ++indexFirstPort;
                                }
                                if (preferredSwapSideTop) {
                                    indexTop = topPairingOrder.indexOf(topPort);
                                } else {
                                    indexBottom = bottomPairingOrder.indexOf(bottomPort);
                                }
                            }
                        }
                    }
                } else { //indexFirstPort > indexSecondPort
                    boolean hasChanged = false;
                    if (!isFinalSorting) {
                        hasChanged = swapIfPossible(firstOrder, firstPort, firstOrderPairedPorts, indexFirstPort,
                                true, !isFinalSorting, forceSwapping, allPairedPorts, portsNeedAbsoluteSameIndex);
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
                                false, false, forceSwapping, allPairedPorts, portsNeedAbsoluteSameIndex);
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
                            forceSwapping = allowForceSwapping;
                            //we may also insert dummy vertices if the absolute order matters
                            if (!forceSwapping && portsNeedAbsoluteSameIndex && dummyPortsForAbsoluteIndex != null) {
                                while (indexSecondPort < indexFirstPort) {
                                    dummyPortCounter = addDummyPort(secondPort, indexSecondPort, false, secondOrder,
                                            dummyPortCounter, dummyPortsForAbsoluteIndex);
                                    ++indexSecondPort;
                                }
                                if (preferredSwapSideTop) {
                                    indexBottom = bottomPairingOrder.indexOf(bottomPort);
                                } else {
                                    indexTop = topPairingOrder.indexOf(topPort);
                                }
                            }
                        }
                    }
                }
            }
        }

        //we may also insert dummy vertices to have the same amount of ports on both sides if the absolute order matters
        if (portsNeedAbsoluteSameIndex && dummyPortsForAbsoluteIndex != null) {
            while (bottomOrder.size() < topOrder.size()) {
                dummyPortCounter = addDummyPort(bottomOrder.get(bottomOrder.size() - 1), bottomOrder.size() - 1,
                        true, bottomOrder, dummyPortCounter, dummyPortsForAbsoluteIndex);
            }
            while (topOrder.size() < bottomOrder.size()) {
                dummyPortCounter = addDummyPort(topOrder.get(topOrder.size() - 1), topOrder.size() - 1,
                        true, topOrder, dummyPortCounter, dummyPortsForAbsoluteIndex);
            }
        }

        //check success
        if (isFinalSorting) {
            //check port pairings
            boolean portParingsValid = true;
            for (Pair<Port, Port> portPairing : allPortPairings) {
                Port bottomPort = preferredSwapSideTop ? portPairing.getFirst() : portPairing.getSecond();
                Port topPort = preferredSwapSideTop ? portPairing.getSecond() : portPairing.getFirst();
                int indexBottom = bottomPairingOrder.indexOf(bottomPort);
                int indexTop = topPairingOrder.indexOf(topPort);
                if (indexBottom != indexTop) {
                    portParingsValid = false;
                }
            }
            return portParingsValid;
        }
        return false;
    }

    private static int addDummyPort(Port pairedPort, int indexPairedPort, boolean addAfter, List<Port> portOrdering,
                                    int dummyPortCounter, Collection<Port> dummyPortsForAbsoluteIndex) {
        Port dummyPort = new Port();
        dummyPort.getLabelManager().addLabel(new TextLabel(
                "dummyPortForPairings_" + dummyPortCounter++));
        dummyPortsForAbsoluteIndex.add(dummyPort);
        if (pairedPort.getPortGroup() != null) {
            pairedPort.getPortGroup().addPortComposition(dummyPort);
        }
        else {
            pairedPort.getVertex().addPortComposition(dummyPort);
        }
        portOrdering.add(indexPairedPort + (addAfter ? 1 : 0), dummyPort);
        return dummyPortCounter;
    }

    private static List<Port> extractOrderingOfPairedPorts(List<Port> orderingOfAllPorts, Set<Port> allPairedPorts) {
        List<Port> orderOfPairedPorts = new ArrayList<>(allPairedPorts.size() / 2);
        return updateOrderingOfPairedPorts(orderOfPairedPorts, orderingOfAllPorts, allPairedPorts);
    }

    private static List<Port> updateOrderingOfPairedPorts(List<Port> orderOfPairedPorts, List<Port> orderingOfAllPorts,
                                                          Set<Port> allPairedPorts) {
        orderOfPairedPorts.clear();
        for (Port port : orderingOfAllPorts) {
            if (allPairedPorts.contains(port)) {
                orderOfPairedPorts.add(port);
            }
        }
        return orderOfPairedPorts;
    }

    private static boolean swapIfPossible(List<Port> orderedPorts, Port consideredPort, List<Port> orderOfPairedPorts,
                                          int indexWithinPairedPorts, boolean swapLeft, boolean swapPortGroups,
                                          boolean forceSwapping, Set<Port> allPairedPorts,
                                          boolean portsNeedAbsoluteSameIndex) {
        int indexConsideredPort = orderedPorts.indexOf(consideredPort);
        if (swapLeft) {
            int prevIndexPairedPort = indexWithinPairedPorts == 0 ? -1 :
                    orderedPorts.indexOf(orderOfPairedPorts.get(indexWithinPairedPorts - 1));
            //second condition to not make new crossings with previous port parings (only when swapping left)
            if (canSwapLeft(indexConsideredPort, orderedPorts) && !allPairedPorts.contains(
                    orderedPorts.get(indexConsideredPort - 1))) {// && prevIndexPairedPort != indexConsideredPort - 1) {
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
            // -- unless we are sorting for precise indices
            if (canSwapRight(indexConsideredPort, orderedPorts) && (!portsNeedAbsoluteSameIndex ||
                    !allPairedPorts.contains(orderedPorts.get(indexConsideredPort + 1)))) {
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

    private static boolean canSwapLeft(int index, List<Port> portOrder) {
        if (index <= 0) {
            return false;
        }
        PortGroup portGroup = portOrder.get(index).getPortGroup();
        PortGroup portGroupLeft = portOrder.get(index - 1).getPortGroup();
        if (portGroup == null) {
            return portGroupLeft == null;
        }
        return portGroup.equals(portGroupLeft) && !portGroup.isOrdered();
    }

    private static boolean canSwapRight(int index, List<Port> portOrder) {
        if (index >= portOrder.size() - 1) {
            return false;
        }
        PortGroup portGroup = portOrder.get(index).getPortGroup();
        PortGroup portGroupRight = portOrder.get(index + 1).getPortGroup();
        if (portGroup == null) {
            return portGroupRight == null;
        }
        return portGroup.equals(portGroupRight) && !portGroup.isOrdered();
    }

    private static void swapLeft(int index, List<?> portOrder) {
        Collections.swap(portOrder, index, index - 1);
    }

    private static void swapRight(int index, List<?> portOrder) {
        Collections.swap(portOrder, index, index + 1);
    }

    private static boolean canSwapPortGroupLeft(int index, List<Port> portOrder, int lastIndexPairedPort) {
        if (index <= 0) {
            return false;
        }
        Port portSelf = portOrder.get(index);
        Port portLeft = portOrder.get(index - 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portLeft);
        if (leastCommonAncestor != null && leastCommonAncestor.isOrdered()) {
            return false; //we cannot swap within port groups of fixed order
        }
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

    //why only 2 params and not 3 as for left swapping groups? -> see comment inside (before return true)
    private static boolean canSwapPortGroupRight(int index, List<Port> portOrder) {
        if (index >= portOrder.size() - 1) {
            return false;
        }

        Port portSelf = portOrder.get(index);
        Port portRight = portOrder.get(index + 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portRight);
        if (leastCommonAncestor != null && leastCommonAncestor.isOrdered()) {
            return false; //we cannot swap within port groups of fixed order
        }
        //we don't need the check nextIndexPairedPort because if we make new crossings this way, they will be removed in
        // the next steps of the for loop when the next port pairings are considered
        return true;
    }

    private static void swapPortGroupLeft(int index, List<Port> portOrder) {
        Port portSelf = portOrder.get(index);
        Port portLeft = portOrder.get(index - 1);
        PortGroup leastCommonAncestor = (PortGroup) PortUtils.getLeastCommonAncestor(portSelf, portLeft);
        PortComposition candidateSelf = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portSelf);
        PortComposition candidateLeft = PortUtils.getTopMostChildContainingThisPort(leastCommonAncestor, portLeft);
        swapPortCompositions(portOrder, candidateLeft, candidateSelf);
    }

    private static void swapPortGroupRight(int index, List<Port> portOrder) {
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
    private static void swapPortCompositions(List<Port> portOrder, PortComposition leftPC, PortComposition rightPC) {
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

    private void orderPorts(List<Port> ports, SortingOrder orders) {
        //find the barycenter for each port, we don't care about port groups in this method
        LinkedHashMap<Port, Double> port2barycenter = new LinkedHashMap<>();
        for (Port port : ports) {
            port2barycenter.put(port, getBarycenter(port, orders));
        }
        ports.sort((Comparator.comparingDouble(port2barycenter::get)));
    }

    private List<Port> orderPortsConstraintToPortGroups(List<Port> idealPortOrder,
                                                        Collection<PortComposition> constrainingPortCompositions,
                                                        boolean reSortPortCompositions) {
        //find all ports of the port compositions and order ports of contained port compositions recursively
        //if we have a port group with fixed order, we set reSortPortCompositions to false
        LinkedHashMap<PortComposition, List<Port>> portComposition2ports = new LinkedHashMap<>();
        for (PortComposition portComposition : constrainingPortCompositions) {
            Set<Port> portsOfThisPC = new LinkedHashSet<>(PortUtils.getPortsRecursively(portComposition));
            List<Port> portsOfThisPcCorrectSideInOrder = new ArrayList<>(portsOfThisPC.size());
            //keep only the ports on the correct side and in correct order
            for (Port port : idealPortOrder) {
                if (portsOfThisPC.contains(port)) {
                    portsOfThisPcCorrectSideInOrder.add(port);
                }
            }
            //find and save barycenter of pc
            if (portComposition instanceof PortGroup) {
                //order ports of this port group recursively
                portsOfThisPcCorrectSideInOrder = orderPortsConstraintToPortGroups(portsOfThisPcCorrectSideInOrder,
                        ((PortGroup) portComposition).getPortCompositions(),
                        !((PortGroup) portComposition).isOrdered());
            }
            //save list that is recursively correctly sorted
            portComposition2ports.put(portComposition, portsOfThisPcCorrectSideInOrder);
        }

        //re-sort port compositions as blocks (if we are allowed to -- it may be forbidden because we are in a port
        // group with fixed order right now)
        List<PortComposition> portCompositions = new ArrayList<>(constrainingPortCompositions);
        if (reSortPortCompositions) {
            //compute the barycenters of the currently considered port compositions
            LinkedHashMap<PortComposition, Double> portComposition2portBarycenter = new LinkedHashMap<>();
            for (PortComposition portComposition : constrainingPortCompositions) {
                List<Port> portsOfThisPcCorrectSideInOrder = portComposition2ports.get(portComposition);
                //find and save barycenter of pc
                if (portComposition instanceof PortGroup) {
                    //compute barycenter of this port group
                    int barycenterSum = 0;
                    int portCount = 0;
                    for (Port port : portsOfThisPcCorrectSideInOrder) {
                        barycenterSum += idealPortOrder.indexOf(port);
                        ++portCount;
                    }
                    portComposition2portBarycenter.put(portComposition, (double) barycenterSum / (double) portCount);
                } else if (portComposition instanceof Port) {
                    portComposition2portBarycenter.put(portComposition,
                            (double) idealPortOrder.indexOf(portComposition));
                }
            }
            //order by barycenter
            portCompositions.sort(Comparator.comparingDouble(portComposition2portBarycenter::get));
        }
        //concatenate all lists of these port compositions sorted by the barycenters
        List<Port> resultingList = new ArrayList<>(idealPortOrder.size());
        for (PortComposition portComposition : portCompositions) {
            resultingList.addAll(portComposition2ports.get(portComposition));
        }
        return resultingList;
    }

    private double getBarycenter(Port port, SortingOrder orders) {
        double sumBarycenter = 0;
        int countRelevantAdjacencies = 0;
        for (Edge edge : port.getEdges()) {
            Port otherPort = PortUtils.getOtherEndPoint(edge, port);
            Vertex otherVertex = otherPort.getVertex();
            sumBarycenter +=
                    (double) orders.getNodeOrder().get(sugy.getRank(otherVertex)).indexOf(otherVertex)
                            + Math.max((double) orders.getBottomPortOrder().get(otherVertex).indexOf(otherPort) /
                                    (double) orders.getBottomPortOrder().get(otherVertex).size(),
                            (double) orders.getTopPortOrder().get(otherVertex).indexOf(otherPort) /
                                    (double) orders.getTopPortOrder().get(otherVertex).size());
            ++countRelevantAdjacencies;
        }
        return sumBarycenter / (double) countRelevantAdjacencies;
    }

    private double getBarycenter(SortingNode node, boolean sweepUpwards) {
        double sumBarycenter = 0;
        int countRelevantAdjacencies = 0;
        for (SortingNode adjacentNode : sweepUpwards ? node.getNeighborsBelow() : node.getNeighborsAbove()) {
            sumBarycenter += adjacentNode.getCurrentPosition();
            ++countRelevantAdjacencies;
        }
        return sumBarycenter / (double) countRelevantAdjacencies;
    }

    private double getBarycenter(Vertex vertex, List<SortingNode> sortingNodes, boolean sweepUpwards) {
        double sumBarycenter = 0;
        int countRelevantAdjacencies = 0;
        for (SortingNode sortingNode : sortingNodes) {
            if ((sortingNode.representsPort() && sortingNode.getPort().getVertex().equals(vertex)) ||
                    (!sortingNode.representsPort() && sortingNode.getVertex().equals(vertex))) {
                sumBarycenter += getBarycenter(sortingNode, sweepUpwards);
                ++countRelevantAdjacencies;
            }
        }
        return sumBarycenter / (double) countRelevantAdjacencies;
    }

    /**
     * assumes that the ports are already sorted and grouped by vertices and port groups
     *
     * @param indexCurrentLayer
     * @param upwards
     */
    private void reorderPortParingsAndTurningDummies(int indexCurrentLayer, boolean upwards) {
        List<SortingNode> layer = layers.get(indexCurrentLayer);
        Set<Vertex> alreadyHandled = new LinkedHashSet<>();
        for (int pos = 0; pos < layer.size(); pos++) {
            SortingNode node = layer.get(pos);
            if (node.representsPort() && !alreadyHandled.contains(node.getPort().getVertex())) {
                Vertex vertex = node.getPort().getVertex();
                alreadyHandled.add(vertex);
                if (sugy.isDummyTurningNode(vertex)) {
                    sortPortsAtTurningDummy(vertex, indexCurrentLayer, pos, upwards);
                } else {
                    sortPortPairingsAndTurningDummyNeighbors(vertex, indexCurrentLayer, pos, upwards);
                }
            }
        }
    }

    private void sortPortPairingsAndTurningDummyNeighbors(Vertex vertex, int indexLayers, int indexPositionInLayer,
                                                          boolean upwards) {
        ArrayList<SortingNode> portNodes = new ArrayList<>();
        List<SortingNode> layer = layers.get(indexLayers);

        //first find all sorting nodes that represent ports belonging to that vertex
        int pos = indexPositionInLayer;
        while (pos < layer.size() && layer.get(pos).representsPort() && layer.get(pos).getPort().getVertex().equals(vertex)) {
            portNodes.add(layer.get(pos));
            ++pos;
        }

        //move dummy turning points to the outsides
        if (movePortsAdjToTurningDummiesToTheOutside) {
            movePortsAdjacentToTurningDummy(portNodes, indexLayers, upwards);
            //re-insert them to the old ordering (save new order)
            for (int i = 0; i < portNodes.size(); i++) {
                layer.set(indexPositionInLayer + i, portNodes.get(i));
            }
        }

        //handle port pairings
        if (sugy.isPlug(vertex)) {
            //first find port nodes of the same vertex on the neighboring layer;
            //this is the layer where the paired ports of this layer lie.
            List<SortingNode> adjLayer = layers.get(indexLayers + (upwards ? 1 : -1));
            ArrayList<SortingNode> adjPortNodes = new ArrayList<>();
            int startIndexInAdjLayer = -1;
            for (int i = 0; i < adjLayer.size(); i++) {
                SortingNode sortingNode = adjLayer.get(i);
                if (sortingNode.representsPort() && sortingNode.getPort().getVertex().equals(vertex)) {
                    if (startIndexInAdjLayer == -1) {
                        startIndexInAdjLayer = i;
                    }
                    adjPortNodes.add(sortingNode);
                } else if (startIndexInAdjLayer >= 0) {
                    break;
                }
            }
            //find paired ports
            List<SortingNode> pairedPorts = new ArrayList<>();
            for (SortingNode portNode : portNodes) {
                SortingNode pairedPort = portNode.getPairedPortSortingNode();
                if (pairedPort != null) {
                    pairedPorts.add(pairedPort);
                }
            }
            //remove them from the port ordering of the other side
            adjPortNodes.removeAll(pairedPorts);
            //re-insert the paired ports from the other side at the same index as their counterparts at
            // this side
            for (int i = 0; i < portNodes.size(); i++) {
                SortingNode pairedPort = portNodes.get(i).getPairedPortSortingNode();
                if (pairedPort != null) {
                    adjPortNodes.add(Math.min(i, adjPortNodes.size()), pairedPort);
                }
            }
            //save new order
            for (int i = 0; i < adjPortNodes.size(); i++) {
                adjLayer.set(startIndexInAdjLayer + i, adjPortNodes.get(i));
            }
        }
    }

    private void sortPortsAtTurningDummy(Vertex turningDummyVertex, int indexLayers, int indexPositionInLayer,
                                         boolean upwards) {
        ArrayList<SortingNode> portNodes = new ArrayList<>();
        List<SortingNode> layer = layers.get(indexLayers);

        //first find all sorting nodes that represent ports belonging to that turning dummy vertex
        int pos = indexPositionInLayer;
        while (pos < layer.size() && layer.get(pos).representsPort() && layer.get(pos).getPort().getVertex().equals(turningDummyVertex)) {
            portNodes.add(layer.get(pos));
            ++pos;
        }
        //now sort them by barycenter
        portNodes.sort(Comparator.comparingDouble(n -> getBarycenter(n, upwards)));

        //re-insert them to the old ordering
        for (int i = 0; i < portNodes.size(); i++) {
            layer.set(indexPositionInLayer + i, portNodes.get(i));
        }
    }

    /**
     * wrapper for {@link Port} or {@link Vertex}, depending on which is used for sorting everything
     */
    private static class SortingNode {
        private Port port;
        private Vertex vertex;
        private boolean representsPort;
        private double portCountOnVertexSide;
        private List<SortingNode> neighborsBelow = new ArrayList<>();
        private List<SortingNode> neighborsAbove = new ArrayList<>();
        private SortingNode pairedPortSortingNode = null;
        private double currentPosition;
        private double currentBarycenter;
        private double currentBarycenterFromOtherSide;

        SortingNode(Port port) {
            this.port =port;
            this.representsPort = true;
        }

        public SortingNode(Vertex vertex) {
            this.vertex = vertex;
            this.representsPort = false;
        }


        /*

        getters and setters

         */


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

        public double getCurrentPosition() {
            return currentPosition;
        }

        public void setCurrentPosition(double currentPosition) {
            this.currentPosition = currentPosition;
        }

        public double getPortCountOnVertexSide() {
            return portCountOnVertexSide;
        }

        public void setPortCountOnVertexSide(double portCountOnVertexSide) {
            this.portCountOnVertexSide = portCountOnVertexSide;
        }

        public double getCurrentBarycenter() {
            return currentBarycenter;
        }

        public void setCurrentBarycenter(double currentBarycenter) {
            this.currentBarycenter = currentBarycenter;
        }

        public List<SortingNode> getNeighborsBelow() {
            return neighborsBelow;
        }

        public List<SortingNode> getNeighborsAbove() {
            return neighborsAbove;
        }

        public SortingNode getPairedPortSortingNode() {
            return pairedPortSortingNode;
        }

        public void setPairedPortSortingNode(SortingNode pairedPortSortingNode) {
            this.pairedPortSortingNode = pairedPortSortingNode;
        }

        public double getCurrentBarycenterFromOtherSide() {
            return currentBarycenterFromOtherSide;
        }

        public void setCurrentBarycenterFromOtherSide(double currentBarycenterFromOtherSide) {
            this.currentBarycenterFromOtherSide = currentBarycenterFromOtherSide;
        }
    }
}