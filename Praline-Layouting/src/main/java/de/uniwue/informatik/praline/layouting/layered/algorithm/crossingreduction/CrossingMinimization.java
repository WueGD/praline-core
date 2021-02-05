package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
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
public class CrossingMinimization {

    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.MIXED;
    public static final boolean DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE = true;
    public static final boolean DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX = true;

    private SugiyamaLayouter sugy;
    private SortingOrder orders;
    private Set<Vertex> adjacentToDummyTurningPoints;
    private Map<Vertex, SortingNode> vertex2sortingNode;
    private Map<Port, SortingNode> port2SortingNode;
    private Map<SortingNode, Double> currentValues;
    private int maxRank;
    private int numberOfCrossings;
    private CrossingMinimizationMethod method;
    private boolean movePortsAdjToTurningDummiesToTheOutside;
    private boolean placeTurningDummiesNextToTheirVertex;

    public CrossingMinimization(SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean handlePortPairings) {
        return layerSweepWithBarycenterHeuristic(method, orders, true,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, handlePortPairings);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean randomStartPermutation,
                                                          boolean handlePortPairings) {
        return layerSweepWithBarycenterHeuristic(method, orders, randomStartPermutation,
                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, handlePortPairings);
    }

    public SortingOrder layerSweepWithBarycenterHeuristic(CrossingMinimizationMethod method, SortingOrder orders,
                                                          boolean randomStartPermutation,
                                                          boolean movePortsAdjToTurningDummiesToTheOutside,
                                                          boolean placeTurningDummiesNextToTheirVertex,
                                                          boolean handlePortPairings) {
        //init
        this.orders = new SortingOrder(orders);
        initialize(method, randomStartPermutation, movePortsAdjToTurningDummiesToTheOutside,
                placeTurningDummiesNextToTheirVertex);

        //do sweeping and allow movement of ports
        doSweeping(handlePortPairings, true);
        //do several iterations because these 2 steps influence each other
        int iterations = 2;
        for (int i = 0; i < iterations; i++) {
            handleTurningVerticesFinally(true);
            orderPortsFinally(i % 2 == 0, i == iterations - 1, handlePortPairings, false);
        }
//      handleTurningVerticesFinally(true);

        //do sweeping while fixing the order of ports, this also eliminates multiple crossings of the same pair of edges
        doSweeping(false, false);
        iterations = 2;
        for (int i = 0; i < iterations; i++) {
            orderPortsFinally(i % 2 == 0, true, false, true);
        }


        return this.orders;
    }

    private void initialize(CrossingMinimizationMethod method, boolean randomStartPermutation,
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

        if (randomStartPermutation) {
            //compute random start position for each vertex
            for (List<Vertex> layer : orders.getNodeOrder()) {
                Collections.shuffle(layer, Constants.random);
            }

            //shuffle ports
            orders.shufflePorts();
        }
        //place dummy turning points close to their vertices
        if (this.placeTurningDummiesNextToTheirVertex) {
            placeTurningDummiesNextToTheirVertices();
        }
        for (int rank = 0; rank < this.orders.getNodeOrder().size(); rank++) {
            List<Vertex> layer = this.orders.getNodeOrder().get(rank);
            for (Vertex node : layer) {
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
                    if (orders.getTopPortOrder().get(node).isEmpty() ||
                            orders.getBottomPortOrder().get(node).isEmpty()) {
                        SortingNode sortingNode = new SortingNode(node);
                        vertex2sortingNode.put(node, sortingNode);
                    }
                } else {
                    SortingNode sortingNode = new SortingNode(node);
                    vertex2sortingNode.put(node, sortingNode);
                }
            }
        }

        //update current values acc to type
        updateCurrentValues();
    }

    private void doSweeping(boolean handlePortPairings, boolean allowPortPermuting) {
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
                lastStepNodeOrder.add(new ArrayList<>(this.orders.getNodeOrder().get(rank)));
                for (Vertex node : this.orders.getNodeOrder().get(rank)) {
                    if (considerPortsOfNode(node)) {
                        lastStepTopPortOrder.put(node, new ArrayList<>(this.orders.getTopPortOrder().get(node)));
                        lastStepBottomPortOrder.put(node, new ArrayList<>(this.orders.getBottomPortOrder().get(node)));
                    }
                }
            }

            // as long as some orders change iterate from top to bottom and back to top over all ranks
            // reorder all nodes of a rank corresponding to the result of calculateXCoordinates()
            for (int directedRank = (1 - maxRank); directedRank <= maxRank; directedRank++) {

                int rank = Math.abs(directedRank);
                boolean upwards = directedRank > 0;
                List<SortingNode> currentLayer = getSortingNodeLayer(rank, upwards);
                List<SortingNode> currentLayerWithEdges = new ArrayList<>(currentLayer.size());
                List<SortingNode> adjacentPreviousLayer = getSortingNodeLayer(rank + (upwards ? -1 : 1), !upwards);
//                int rankNextLayer = rank + (upwards ? 1 : -1);
//                List<SortingNode> adjacentNextLayer = rankNextLayer < 0 || maxRank <= rankNextLayer ? null :
//                        getSortingNodeLayer(rankNextLayer, upwards);

                Map<SortingNode, Double> barycenters = new LinkedHashMap<>();
                List<SortingNode> deadEnds = new ArrayList<>(); //nodes without edges in the considered direction
                List<Integer> currPositionsOfDeadEnds = new ArrayList<>();
//                Map<SortingNode, Double> barycentersFromOtherSide = new LinkedHashMap<>(); //we use barycenters from
                // the wrong direction as a second criterion for nodes that don't have edges in the considered direction
                for (int i = 0; i < currentLayer.size(); i++) {
                    SortingNode node = currentLayer.get(i);
                    //barycenter in considered direction
                    double barycenter = getBarycenter(node, adjacentPreviousLayer);
                    if (Double.isNaN(barycenter)) {
                        deadEnds.add(node);
                        currPositionsOfDeadEnds.add(i);
                    } else {
                        currentLayerWithEdges.add(node);
                    }
//                    //we don't consider the barycenter in wrong direction if it is an outer layer or if it is the
//                    // very first top-down swipe
//                    if (adjacentNextLayer == null || (currentIteration == 0 && directedRank <= 0)) {
//                        //special case: if dead end in current direction and this is the last layer, we use the current
//                        // position as barycenter
//                        for (SortingNode deadEnd : new ArrayList<>(deadEnds)) {
//                            barycenter = (double) currentLayer.indexOf(node) / (double) (currentLayer.size())
//                                * (double) (adjacentPreviousLayer.size() - 1);
//                            currentLayerWithEdges.add(deadEnds.get(0));
//                            deadEnds.remove(0);
//                        }
//                    }
//                    else {
//                        //barycenter in wrong direction
//                        double barycenterFromOtherSide = getBarycenter(node, adjacentNextLayer);
//                        if (Double.isNaN(barycenterFromOtherSide)) {
//                            //special case: if dead end in wrong direction, use current position as barycenter
//                            barycenterFromOtherSide = (double) currentLayer.indexOf(node) / (double) (currentLayer.size())
//                                    * (double) (adjacentNextLayer.size() - 1);
//                        }
//                        barycentersFromOtherSide.put(node, barycenterFromOtherSide);
//                    }
                    barycenters.put(node, barycenter);
                }
                currentLayerWithEdges.sort(Comparator.comparingDouble(barycenters::get));

                List<SortingNode> combinedOrder;
                if (!deadEnds.isEmpty()) {
                    //TODO: make them 3 variants for the experiments!
                    //best: integrateDeadEndsViaOldRelativePosition, worst: integrateDeadEndsViaBarycentersFromOtherSide
                    combinedOrder =
//                            integrateDeadEndsViaPseudoBarycenters(currentLayer, barycenters, deadEnds,
//                                    currPositionsOfDeadEnds, currentLayer.size());
                            integrateDeadEndsViaOldRelativePosition(
                                   currentLayerWithEdges, deadEnds, currPositionsOfDeadEnds);
//                            integrateDeadEndsViaBarycentersFromOtherSide(currentLayer, currentLayerWithEdges,
//                                  deadEnds, barycentersFromOtherSide);
                } else {
                    //if there are no dead ends, just take the order computed before
                    combinedOrder = currentLayerWithEdges;
                }


                updateVerticesAndPortsOrder(rank, combinedOrder, upwards, allowPortPermuting);
                if (handlePortPairings && allowPortPermuting) {
                    reorderPortParingsAndTurningDummies(rank, upwards);
                }
                updateCurrentValues();
            }

            hasChanged = checkIfHasChanged(lastStepNodeOrder, lastStepTopPortOrder, lastStepBottomPortOrder,
                    currentIteration, handlePortPairings && allowPortPermuting, allowPortPermuting);
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

    private List<SortingNode> integrateDeadEndsViaPseudoBarycenters(List<SortingNode> currentLayer,
                                                                    Map<SortingNode, Double> barycenters,
                                                                    List<SortingNode> deadEnds,
                                                                    List<Integer> currPositionsOfDeadEnds,
                                                                    int sizeAdjacentPreviousLayer) {

        /*
        For comparison reasons another alternative to integrateDeadEndsViaOldRelativePosition().
        We compute a pseudo barycenter as the current position normalized by the sizes of this layer and the adjacent
        previous layer.
        It was the old default before this step of the crossing minimization phase was tackeled.
        However it works worse.
         */

        int sizeCurrentLayer = currentLayer.size();
        for (int i = 0; i < deadEnds.size(); i++) {
            SortingNode deadEnd = deadEnds.get(i);
            double pseudoBarycenter = (double) currPositionsOfDeadEnds.get(i) /
                    (double) (sizeCurrentLayer) * (double) (sizeAdjacentPreviousLayer - 1);
            barycenters.put(deadEnd, pseudoBarycenter);
        }

        currentLayer.sort(Comparator.comparingDouble(barycenters::get));
        return currentLayer;
    }


    //seems to work badly... so this was taken out again
    private List<SortingNode> integrateDeadEndsViaBarycentersFromOtherSide(List<SortingNode> currentLayer,
                                                                           List<SortingNode> currentLayerWithEdges,
                                                                           List<SortingNode> deadEnds,
                                                                           Map<SortingNode, Double> barycentersFromOtherSide) {

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
            currentLayerBarycentersFromOtherSide.add(barycentersFromOtherSide.get(node));
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
            double barycenterFromOtherSide = barycentersFromOtherSide.get(deadEnd);
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
                        deadEndsAtI.sort(Comparator.comparingDouble(barycentersFromOtherSide::get));
                    } else {
                        deadEndsAtI.sort(Comparator.comparingDouble(barycentersFromOtherSide::get).reversed());
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

    private void updateVerticesAndPortsOrder(int layerIndex, List<SortingNode> newOrdering, boolean upwards,
                                             boolean allowPortMovement) {
        //bring vertices together acc. to the previously computed barycenters
        List<Vertex> verticesOfLayer = new ArrayList<>();
        List<Port> portOrdering = new ArrayList<>();
        Map<Vertex, Double> barycenterSum = new LinkedHashMap<>();
        Map<Vertex, Integer> counter = new LinkedHashMap<>();
        for (int i = 0; i < newOrdering.size(); i++) {
            SortingNode node = newOrdering.get(i);
            Object nodeObject = node.getStoredObject();
            if (nodeObject instanceof Vertex) {
                verticesOfLayer.add((Vertex) nodeObject);
                barycenterSum.put((Vertex) nodeObject, (double) i);
                counter.put((Vertex) nodeObject, 1);
            } else if (nodeObject instanceof Port) {
                portOrdering.add((Port) nodeObject);
                Vertex vertex = ((Port) nodeObject).getVertex();
                if (!barycenterSum.containsKey(vertex)) {
                    verticesOfLayer.add(vertex);
                    barycenterSum.put(vertex, (double) i);
                    counter.put(vertex, 1);
                } else {
                    barycenterSum.replace(vertex, barycenterSum.get(vertex) + (double) i);
                    counter.replace(vertex, counter.get(vertex) + 1);
                }
            }
        }
        //re-sort vertices
        verticesOfLayer.sort(Comparator.comparingDouble(v -> barycenterSum.get(v) / (double) counter.get(v)));
        orders.getNodeOrder().set(layerIndex, verticesOfLayer);

        //re-sort ports
        if (allowPortMovement) {
            for (Vertex vertex : verticesOfLayer) {
                if (considerPortsOfNode(vertex)) {
                    List<Port> portsOfVertex =
                            orderPortsConstraintToPortGroups(portOrdering, vertex.getPortCompositions(), allowPortMovement);
                    if (upwards) {
                        orders.getBottomPortOrder().replace(vertex, portsOfVertex);
                    } else {
                        orders.getTopPortOrder().replace(vertex, portsOfVertex);
                    }
                }
            }
        }
    }

    private boolean checkIfHasChanged(List<List<Vertex>> lastStepNodeOrder,
                                      Map<Vertex, List<Port>> lastStepTopPortOrder,
                                      Map<Vertex, List<Port>> lastStepBottomPortOrder, int currentIteration,
                                      boolean handlePortPairings, boolean allowPortPermuting) {
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
            SortingOrder currentOrderCopy = new SortingOrder(orders);
            orderPortsFinally(currentOrderCopy.getTopPortOrder(), currentOrderCopy.getBottomPortOrder(),false, true,
                    false, handlePortPairings, allowPortPermuting);
            int newNumberOfCrossings = sugy.countCrossings(currentOrderCopy);
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

    private void orderPortsFinally(boolean upwards, boolean isFinalSorting, boolean handlePortPairings,
                                   boolean ignoreNodesWithPairings) {
        orderPortsFinally(this.orders.getTopPortOrder(), this.orders.getBottomPortOrder(), true, upwards, isFinalSorting,
                handlePortPairings, ignoreNodesWithPairings);
    }

    private void orderPortsFinally(Map<Vertex, List<Port>> currentTPortOrder, Map<Vertex, List<Port>> currentBPortOrder,
                                   boolean updateCurrentValues, boolean upwards, boolean isFinalSorting,
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
                    orderPorts(portsOfThisNodeSide, node);
                    portsOfThisNodeSide = orderPortsConstraintToPortGroups(portsOfThisNodeSide,
                            node.getPortCompositions(), true);
                    currentPortOrderMap.replace(node, portsOfThisNodeSide);
                }
                if (handlePortPairings) {
                    int iterations = 4;
                    for (int i = 0; i < iterations; i++) {
                        repairPortPairings(sugy, node, currentBPortOrder, currentTPortOrder,
                                ((upwards ? 0 : 1) + i) % 2 == 0, isFinalSorting && i == iterations - 1, true, false,
                                null);
                    }
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
     */
    public static void repairPortPairings(SugiyamaLayouter sugy, Vertex node, Map<Vertex, List<Port>> currentBPortOrder,
                                          Map<Vertex, List<Port>> currentTPortOrder, boolean preferredSwapSideTop,
                                          boolean isFinalSorting, boolean allowForceSwapping,
                                          boolean portsNeedAbsoluteSameIndex,
                                          Collection<Port> dummyPortsForAbsoluteIndex) {
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
        List<Port> bottomPairingOrder = portsNeedAbsoluteSameIndex ? bottomOrder :
                extractOrderingOfPairedPorts(bottomOrder, allPairedPorts);
        List<Port> topPairingOrder = portsNeedAbsoluteSameIndex ? topOrder :
                extractOrderingOfPairedPorts(topOrder, allPairedPorts);

        List<Port> firstOrderPairedPorts = preferredSwapSideTop ? topPairingOrder : bottomPairingOrder;
        List<Port> secondOrderPairedPorts = preferredSwapSideTop ? bottomPairingOrder : topPairingOrder;

        int dummyPortCounter = 0; //only used for dummyPortsForAbsoluteIndex; see javadoc of this method

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
        if (isFinalSorting && !portsNeedAbsoluteSameIndex) {
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

    private static void swapLeft(int index, List<Port> portOrder) {
        Collections.swap(portOrder, index, index - 1);
    }

    private static void swapRight(int index, List<Port> portOrder) {
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

    private void orderPorts(List<Port> ports, Vertex node) {
        //find the barycenter for each port, we don't care about port groups in this method
        LinkedHashMap<Port, Double> port2barycenter = new LinkedHashMap<>();
        for (Port port : ports) {
            port2barycenter.put(port, getBarycenter(port));
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
                        movePortsAdjacentToTurningDummy(node, orders.getNodeOrder().get(rank), orders.getBottomPortOrder(),
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