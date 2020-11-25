package de.uniwue.informatik.praline.layouting.layered.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.io.output.svg.SVGDrawer;
import de.uniwue.informatik.praline.layouting.PralineLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.PortSideAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.GraphPreprocessor;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.drawing.DrawingPreparation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgerouting.EdgeRouting;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.LayerAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.NodePlacement;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.DummyCreationResult;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.DummyNodeCreation;

import java.util.*;

public class SugiyamaLayouter implements PralineLayouter {

    public static final DirectionMethod DEFAULT_DIRECTION_METHOD = DirectionMethod.FORCE;
    public static final int DEFAULT_NUMBER_OF_FD_ITERATIONS = 10;
    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.PORTS;
    public static final int DEFAULT_NUMBER_OF_CM_ITERATIONS = 5; //iterations for crossing minimization

    private Graph graph;
    private DrawingInformation drawInfo;
    private Map<Vertex, VertexGroup> plugs;
    private Map<Vertex, VertexGroup> vertexGroups;
    private Map<Vertex, PortGroup> origVertex2replacePortGroup;
    private Map<Vertex, Edge> hyperEdges;
    private Map<Edge, Vertex> hyperEdgeParts;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Edge> dummyNodesSelfLoops;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Set<Vertex> dummyNodesForEdgesOfDeg1or0;
    private Set<Vertex> dummyNodesForNodelessPorts;
    private Map<Port, Port> replacedPorts;
    private Map<Port, List<Port>> multipleEdgePort2replacePorts;
    private Map<Port, Port> keptPortPairings;
    private Map<Edge, Edge> dummyEdge2RealEdge;
    private Map<Vertex, Set<Edge>> loopEdges;
    private Map<Edge, List<Port>> loopEdge2Ports;
    private Map<Vertex, Set<Port>> dummyPortsForLabelPadding;
    private List<Port> dummyPortsForNodesWithoutPort;
    private List<PortGroup> dummyPortGroupsForEdgeBundles;
    private Map<EdgeBundle, Collection<Edge>> originalEdgeBundles;
    private Map<PortPairing, PortPairing> replacedPortPairings;

    //additional structures

    private Map<Edge, Vertex> edgeToStart;
    private Map<Edge, Vertex> edgeToEnd;
    private Map<Vertex, Collection<Edge>> nodeToOutgoingEdges;
    private Map<Vertex, Collection<Edge>> nodeToIncomingEdges;
    private Map<Vertex, Vertex> nodeToLowerDummyTurningPoint;
    private Map<Vertex, Vertex> nodeToUpperDummyTurningPoint;
    private Map<Port, Port> correspondingPortsAtDummy;
    private Map<Vertex, Integer> nodeToRank;
    private Map<Integer, Collection<Vertex>> rankToNodes;
    private SortingOrder orders;
    private boolean hasAssignedLayers;
    private Set<Object> deviceVertices;

    public SugiyamaLayouter(Graph graph) {
        this(graph, new DrawingInformation());
    }

    public SugiyamaLayouter(Graph graph, DrawingInformation drawInfo) {
        this.graph = graph;

        initialize();
        for (Vertex node : graph.getVertices()) {
            if (node.getLabelManager().getLabels().get(0) instanceof TextLabel) {
                drawInfo.setFont(((TextLabel) node.getLabelManager().getLabels().get(0)).getFont());
                break;
            }
        }
        this.drawInfo = drawInfo;
    }

    @Override
    public void computeLayout() {
        computeLayout(DEFAULT_DIRECTION_METHOD, DEFAULT_NUMBER_OF_FD_ITERATIONS, DEFAULT_CROSSING_MINIMIZATION_METHOD,
                DEFAULT_NUMBER_OF_CM_ITERATIONS);
    }

    /**
     *
     * @param method
     * @param numberOfIterationsFD
     *      when employing a force-directed algo, it uses so many iterations with different random start positions
     *      and takes the one that yields the fewest crossings.
     *      If you use anything different from {@link DirectionMethod#FORCE}, then this value will be ignored.
     * @param cmMethod
     * @param numberOfIterationsCM
     *      for the crossing minimization phase you may have several independent random iterations of which the one
     *      that yields the fewest crossings of edges between layers is taken.
     */
    public void computeLayout (DirectionMethod method, int numberOfIterationsFD,
                               CrossingMinimizationMethod cmMethod, int numberOfIterationsCM) {
        construct();
        assignDirections(method, numberOfIterationsFD);
        assignLayers();
        createDummyNodes();
        crossingMinimization(cmMethod, numberOfIterationsCM);
        nodePositioning();
        edgeRouting();
        prepareDrawing();
    }

    // change graph so that
    // each Edge has exactly two Ports
    // each Port has max one Edge
    // VertexGroups are replaced by a single node
    // if all Nodes of a Group are touching each other PortGroups are kept
    // save changes to resolve later

    public void construct() {
        GraphPreprocessor graphPreprocessor = new GraphPreprocessor(this);
        graphPreprocessor.construct();
    }

    public void assignDirections (DirectionMethod method) {
        assignDirections(method, 1);
    }

    /**
     *
     * @param method
     * @param numberOfIterationsForForceDirected
     *      when employing a force-directed algo, it uses so many iterations with different random start positions
     *      and takes the one that yields the fewest crossings.
     *      If you use anything different from {@link DirectionMethod#FORCE}, then this value will be ignored.
     */
    public void assignDirections (DirectionMethod method, int numberOfIterationsForForceDirected) {
        DirectionAssignment da = new DirectionAssignment();
        switch (method) {
            case FORCE:
                da.forceDirected(this, numberOfIterationsForForceDirected);
                break;
            case BFS:
                da.breadthFirstSearch(this);
                break;
            case RANDOM:
                da.randomDirected(this);
                break;
        }
    }
    public void copyDirections(SugiyamaLayouter otherSugiyamaLayouterWithSameGraph)  {
        for (Edge edge : otherSugiyamaLayouterWithSameGraph.getGraph().getEdges()) {
            this.assignDirection(edge, otherSugiyamaLayouterWithSameGraph.getStartNode(edge),
                    otherSugiyamaLayouterWithSameGraph.getEndNode(edge));
        }

        //check that all edges got a direction
        for (Edge edge : this.getGraph().getEdges()) {
            if (!edgeToStart.containsKey(edge)) {
                throw new NoSuchElementException("No edge direction found to copy. The input parameter " +
                        "otherSugiyamaLayouterWithSameGraph has either not yet directions assigned or the graph is not "
                        + "identical with the graph of this SugiyamaLayouter object.");
            }
        }
    }

    public void assignLayers () {
        LayerAssignment la = new LayerAssignment(this);
        nodeToRank = la.networkSimplex();
        PortSideAssignment pa = new PortSideAssignment(this);
        pa.assignPortsToVertexSides();
        createRankToNodes();
        hasAssignedLayers = true;
    }

    public void createDummyNodes() {
        DummyNodeCreation dnc = new DummyNodeCreation(this);
        DummyCreationResult dummyNodeData = dnc.createDummyNodes();
        this.dummyNodesLongEdges = dummyNodeData.getDummyNodesLongEdges();
        this.dummyNodesSelfLoops = dummyNodeData.getDummyNodesSelfLoops();
        this.dummyTurningNodes = dummyNodeData.getDummyTurningNodes();
        this.nodeToLowerDummyTurningPoint = dummyNodeData.getNodeToLowerDummyTurningPoint();
        this.nodeToUpperDummyTurningPoint = dummyNodeData.getNodeToUpperDummyTurningPoint();
        this.correspondingPortsAtDummy = dummyNodeData.getCorrespondingPortsAtDummy();
        for (Edge edge : dummyNodeData.getDummyEdge2RealEdge().keySet()) {
            this.dummyEdge2RealEdge.put(edge, dummyNodeData.getDummyEdge2RealEdge().get(edge));
        }
    }

    public void crossingMinimization (CrossingMinimizationMethod cmMethod, int numberOfIterations) {
        crossingMinimization(cmMethod, CrossingMinimization.DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE
                , CrossingMinimization.DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, numberOfIterations);
    }

    public void crossingMinimization(CrossingMinimizationMethod cmMethod,
                                     boolean movePortsAdjToTurningDummiesToTheOutside,
                                     boolean placeTurningDummiesNextToTheirVertex, int numberOfIterations) {
        CrossingMinimization cm = new CrossingMinimization(this);
        SortingOrder result = cm.layerSweepWithBarycenterHeuristic(null, cmMethod, orders,
                movePortsAdjToTurningDummiesToTheOutside,
                placeTurningDummiesNextToTheirVertex);
        orders = result;
        int crossings = countCrossings(result);
        for (int i = 1; i < numberOfIterations; i++) {
            result = cm.layerSweepWithBarycenterHeuristic(null, cmMethod, orders,
                    movePortsAdjToTurningDummiesToTheOutside, placeTurningDummiesNextToTheirVertex);
            int crossingsNew = countCrossings(result);
            if (crossingsNew < crossings) {
                crossings = crossingsNew;
                orders = result;
            }
        }
    }

    public void nodePositioning () {
        NodePlacement np = new NodePlacement(this, orders, drawInfo);
        dummyPortsForLabelPadding = np.placeNodes();
    }

    /**
     * only needed if {@link SugiyamaLayouter#nodePositioning()} is not used.
     */
    public void nodePadding () {
        NodePlacement np = new NodePlacement(this, orders, drawInfo);
        np.initialize();
        np.initializeStructure();
        dummyPortsForLabelPadding = np.dummyPortsForWidth();
        np.reTransformStructure(true);
    }

    public void edgeRouting () {
        EdgeRouting er = new EdgeRouting(this, orders, drawInfo);
        er.routeEdges();
    }

    public void prepareDrawing () {
        DrawingPreparation dp = new DrawingPreparation(this);
        dp.prepareDrawing(drawInfo, orders, dummyPortsForLabelPadding, dummyPortsForNodesWithoutPort);
    }

    /**
     * This is already done when calling {@link SugiyamaLayouter#prepareDrawing()}.
     * So only use this if, the former one is not used!
     *
     * This was extra created for
     * other layouters like {@link de.uniwue.informatik.praline.layouting.layered.kieleraccess.KielerLayouter}
     * that use a {@link SugiyamaLayouter} only partially
     */
    public void restoreOriginalElements() {
        DrawingPreparation dp = new DrawingPreparation(this);
        dp.initialize(drawInfo, orders, dummyPortsForLabelPadding, dummyPortsForNodesWithoutPort);
        dp.restoreOriginalElements(true);
        dp.tightenNodes();
    }

    public void drawResult (String path) {
        SVGDrawer dr = new SVGDrawer(this.getGraph());
        dr.draw(path, drawInfo);
    }

    ////////////////////////
    // additional methods //
    ////////////////////////
    // constructor //

    private void initialize() {
        plugs = new LinkedHashMap<>();
        vertexGroups = new LinkedHashMap<>();
        origVertex2replacePortGroup = new LinkedHashMap<>();
        hyperEdges = new LinkedHashMap<>();
        hyperEdgeParts = new LinkedHashMap<>();
        dummyNodesLongEdges = new LinkedHashMap<>();
        dummyNodesForEdgesOfDeg1or0 = new LinkedHashSet<>();
        dummyNodesForNodelessPorts = new LinkedHashSet<>();
        replacedPorts = new LinkedHashMap<>();
        multipleEdgePort2replacePorts = new LinkedHashMap<>();
        keptPortPairings = new LinkedHashMap<>();
        loopEdges = new LinkedHashMap<>();
        loopEdge2Ports = new LinkedHashMap<>();
        dummyEdge2RealEdge = new LinkedHashMap<>();
        dummyPortGroupsForEdgeBundles = new ArrayList<>(graph.getEdgeBundles().size());
        originalEdgeBundles = new LinkedHashMap<>();
        replacedPortPairings = new LinkedHashMap<>();
        dummyPortsForNodesWithoutPort = new ArrayList<>();
        deviceVertices = new LinkedHashSet<>();

        edgeToStart = new LinkedHashMap<>();
        edgeToEnd = new LinkedHashMap<>();
        nodeToOutgoingEdges = new LinkedHashMap<>();
        nodeToIncomingEdges = new LinkedHashMap<>();
    }
    // other steps //

    private void createRankToNodes () {
        rankToNodes = new LinkedHashMap<>();
        for (Vertex node : nodeToRank.keySet()) {
            int key = nodeToRank.get(node);
            if (!rankToNodes.containsKey(key)) {
                rankToNodes.put(key, new LinkedHashSet<Vertex>());
            }
            rankToNodes.get(key).add(node);
        }
    }
    public int countCrossings (SortingOrder sortingOrder) {
        // create Port lists
        List<List<Port>> topPorts = new ArrayList<>();
        List<List<Port>> bottomPorts = new ArrayList<>();
        Map<Port, Integer> positions = new LinkedHashMap<>();
        for (int layer = 0; layer < sortingOrder.getNodeOrder().size(); layer++) {
            topPorts.add(new ArrayList<>());
            bottomPorts.add(new ArrayList<>());
            int position = 0;
            for (Vertex node : sortingOrder.getNodeOrder().get(layer)) {
                for (Port topPort : sortingOrder.getTopPortOrder().get(node)) {
                    topPorts.get(layer).add(topPort);
                }
                for (Port bottomPort : sortingOrder.getBottomPortOrder().get(node)) {
                    bottomPorts.get(layer).add(bottomPort);
                    positions.put(bottomPort, position++);
                }
            }
        }
        // count crossings
        int crossings = 0;
        for (int layer = 0; layer < (sortingOrder.getNodeOrder().size() - 1); layer++) {
            for (int topPortPosition = 0; topPortPosition < topPorts.get(layer).size(); topPortPosition++) {
                Port topPort = topPorts.get(layer).get(topPortPosition);
                for (Edge edge : topPort.getEdges()) {
                    Port bottomPort = edge.getPorts().get(0);
                    if (topPort.equals(bottomPort)) bottomPort = edge.getPorts().get(1);
                    int bottomPortPosition = 0;
                    bottomPortPosition = positions.get(bottomPort);
                    for (int topPosition = (topPortPosition + 1); topPosition < topPorts.get(layer).size();
                         topPosition++) {
                        Port crossingTopPort = topPorts.get(layer).get(topPosition);
                        for (Edge crossingEdge : crossingTopPort.getEdges()) {
                            Port crossingBottomPort = crossingEdge.getPorts().get(0);
                            if (crossingTopPort.equals(crossingBottomPort))
                                crossingBottomPort = crossingEdge.getPorts().get(1);
                            if (positions.get(crossingBottomPort) < bottomPortPosition) crossings++;
                        }
                    }
                }
            }
        }
        return crossings;
    }


    //////////////////////////////////////////
    // public methods (getter, setter etc.) //
    //////////////////////////////////////////
    public Port getPairedPort (Port port) {
        return keptPortPairings.get(port);
    }

    public boolean isPaired (Port port) {
        return keptPortPairings.containsKey(port);
    }

    public Vertex getStartNode (Edge edge) {
        return edgeToStart.get(edge);
    }

    public Vertex getEndNode (Edge edge) {
        return edgeToEnd.get(edge);
    }

    public Collection<Edge> getOutgoingEdges (Vertex node) {
        if (nodeToOutgoingEdges.get(node) == null) return new LinkedList<>();
        return Collections.unmodifiableCollection(nodeToOutgoingEdges.get(node));
    }

    public Collection<Edge> getIncomingEdges (Vertex node) {
        if (nodeToIncomingEdges.get(node) == null) return new LinkedList<>();
        return Collections.unmodifiableCollection(nodeToIncomingEdges.get(node));
    }

    public boolean assignDirection (Edge edge, Vertex start, Vertex end) {
        if (edgeToStart.containsKey(edge)) return false;
        edgeToStart.put(edge, start);
        edgeToEnd.put(edge, end);
        if (!nodeToOutgoingEdges.containsKey(start)) {
            nodeToOutgoingEdges.put(start, new LinkedList<>());
        }
        if (!nodeToIncomingEdges.containsKey(end)) {
            nodeToIncomingEdges.put(end, new LinkedList<>());
        }
        nodeToOutgoingEdges.get(start).add(edge);
        nodeToIncomingEdges.get(end).add(edge);
        return true;
    }

    public boolean removeDirection (Edge edge) {
        if (!edgeToStart.containsKey(edge)) return false;
        Vertex start = edgeToStart.remove(edge);
        Vertex end = edgeToEnd.remove(edge);
        nodeToOutgoingEdges.get(start).remove(edge);
        nodeToIncomingEdges.get(end).remove(edge);
        if (nodeToOutgoingEdges.get(start).isEmpty()) {
            nodeToOutgoingEdges.remove(edge);
        }
        if (nodeToIncomingEdges.get(end).isEmpty()) {
            nodeToIncomingEdges.remove(edge);
        }
        return true;
    }

    public int getRank (Vertex node) {
        if (nodeToRank.containsKey(node)) return nodeToRank.get(node);
        return -1;
    }

    public void setRank (Vertex node, Integer rank) {
        if (nodeToRank.containsKey(node)) {
            int oldRank = getRank(node);
            rankToNodes.get(oldRank).remove(node);
            if (rankToNodes.get(oldRank).isEmpty()) {
                rankToNodes.remove(oldRank);
            }
            rankToNodes.putIfAbsent(rank, new LinkedHashSet<>());
            rankToNodes.get(rank).add(node);
            nodeToRank.replace(node, rank);
        } else {
            nodeToRank.put(node, rank);
            rankToNodes.putIfAbsent(rank, new LinkedHashSet<>());
            rankToNodes.get(rank).add(node);
        }
    }

    public void changeRanksAccordingToSortingOrder() {
        List<List<Vertex>> nodeOrder = orders.getNodeOrder();
        for (int i = 0; i < nodeOrder.size(); i++) {
            List<Vertex> layer = nodeOrder.get(i);
            for (Vertex node : layer) {
                setRank(node, i);
            }
        }
    }

    public void changeRanks (Map<Vertex, Integer> newRanks) {
        for (Vertex node: newRanks.keySet()) {
            setRank(node, newRanks.get(node));
        }
    }

    public Collection<Vertex> getAllNodesWithRank (int rank) {
        if (rankToNodes.containsKey(rank)) {
            return Collections.unmodifiableCollection(rankToNodes.get(rank));
        } else {
            return new LinkedHashSet<>();
        }
    }

    public int getMaxRank () {
        int max = 0;
        for (int rank : rankToNodes.keySet()) {
            if (rank > max) max = rank;
        }
        return max;
    }

    public boolean isPlug (Vertex possiblePlug) {
        return plugs.keySet().contains(possiblePlug);
    }

    public boolean isUnionNode (Vertex node) {
        return vertexGroups.keySet().contains(node) || plugs.keySet().contains(node);
    }

    public boolean isDummy(Vertex node) {
        return isDummyNodeOfLongEdge(node) || isDummyNodeOfSelfLoop(node) || isDummyTurningNode(node) ||
                isDummyNodeForEdgesOfDeg1or0(node) || isDummyNodeForNodelessPorts(node) ||
                getHyperEdges().containsKey(node);
    }

    public boolean isDummyNodeOfLongEdge(Vertex node) {
        if (dummyNodesLongEdges == null) {
            return false;
        }
        return dummyNodesLongEdges.containsKey(node);
    }

    public boolean isDummyNodeOfSelfLoop(Vertex node) {
        if (dummyNodesSelfLoops == null) {
            return false;
        }
        return dummyNodesSelfLoops.containsKey(node);
    }

    public boolean isDummyTurningNode(Vertex node) {
        if (dummyTurningNodes == null) {
            return false;
        }
        return dummyTurningNodes.containsKey(node);
    }

    public Vertex getVertexOfTurningDummy (Vertex turningDummy) {
        return dummyTurningNodes.get(turningDummy);
    }

    public Port getCorrespondingPortAtDummy (Port port) {
        return correspondingPortsAtDummy.get(port);
    }

    public boolean isTopPort (Port port) {
        return orders.getTopPortOrder().get(port.getVertex()).contains(port);
    }

    public boolean isDummyNodeForEdgesOfDeg1or0(Vertex node) {
        return dummyNodesForEdgesOfDeg1or0.contains(node);
    }

    public void addDummyNodeForEdgesOfDeg1or0(Vertex dummyNode) {
        dummyNodesForEdgesOfDeg1or0.add(dummyNode);
    }

    public boolean isDummyNodeForNodelessPorts(Vertex node) {
        return dummyNodesForNodelessPorts.contains(node);
    }

    public void addDummyNodeForNodelessPorts(Vertex dummyNode) {
        dummyNodesForNodelessPorts.add(dummyNode);
    }

    public Map<Edge, Edge> getDummyEdge2RealEdge () {
        return dummyEdge2RealEdge;
    }

    public Map<Vertex, Set<Edge>> getLoopEdges() {
        return loopEdges;
    }

    public Set<Edge> getLoopEdgesAsSet() {
        Set<Edge> returnSet = new LinkedHashSet<>();
        for (Vertex vertex : loopEdges.keySet()) {
            returnSet.addAll(loopEdges.get(vertex));
        }
        return returnSet;
    }

    public Set<Edge> getLoopEdgesAsSet(Vertex node) {
        if (loopEdges.containsKey(node)) {
            return Collections.unmodifiableSet(loopEdges.get(node));
        } else {
            return Collections.unmodifiableSet(new LinkedHashSet<>());
        }
    }

    public Map<Edge, List<Port>> getLoopEdge2Ports() {
        return loopEdge2Ports;
    }

    public List<Port> getPortsOfLoopEdge (Edge loopEdge) {
        if (loopEdge2Ports.containsKey(loopEdge)) {
            return Collections.unmodifiableList(loopEdge2Ports.get(loopEdge));
        } else {
            return Collections.unmodifiableList(new ArrayList<>());
        }
    }

    public boolean hasAssignedLayers () {
        return hasAssignedLayers;
    }

    public List<String> getNodeName (Vertex node) {
        List<String> nodeNames = new ArrayList<>();
        // todo: implement other cases
        if (isDummy(node)) {
            nodeNames.add("");
        } else if (isPlug(node)) {
            List<Vertex> originalVertices = plugs.get(node).getContainedVertices();
            for (Vertex originalVertex : originalVertices) {
                for (Label label : originalVertex.getLabelManager().getLabels()) {
                    nodeNames.add(label.toString());
                }
            }
        } else if (vertexGroups.keySet().contains(node)) {
            //TODO
        } else {
            for (Label label : node.getLabelManager().getLabels()) {
                nodeNames.add(label.toString());
            }
        }
        return nodeNames;
    }
    //TODO: re-visit later

    public double getTextWidthForNode(Vertex node) {
        double width = 0;
        for (String label : getNodeName(node)) {
            width = Math.max(width, DrawingInformation.g2d.getFontMetrics().getStringBounds(label,
                    DrawingInformation.g2d).getWidth());
        }
        return width;
    }

    @Override
    public Graph getGraph() {
        return this.graph;
    }

    @Override
    public DrawingInformation getDrawingInformation() {
        return this.drawInfo;
    }

    @Override
    public void setDrawingInformation(DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
    }

    public SortingOrder getOrders() {
        return orders;
    }

    public void setOrders(SortingOrder orders) {
        this.orders = orders;
    }

    public Map<Vertex, VertexGroup> getPlugs() {
        return plugs;
    }

    public Map<Vertex, VertexGroup> getVertexGroups() {
        return vertexGroups;
    }

    public Map<Vertex, PortGroup> getOrigVertex2replacePortGroup() {
        return origVertex2replacePortGroup;
    }

    public void setOrigVertex2replacePortGroup(Map<Vertex, PortGroup> origVertex2replacePortGroup) {
        this.origVertex2replacePortGroup = origVertex2replacePortGroup;
    }

    public Map<Vertex, Edge> getHyperEdges() {
        return hyperEdges;
    }

    public Map<Edge, Vertex> getHyperEdgeParts() {
        return hyperEdgeParts;
    }

    public Map<Port, Port> getReplacedPorts() {
        return replacedPorts;
    }

    public Map<Port, List<Port>> getMultipleEdgePort2replacePorts() {
        return multipleEdgePort2replacePorts;
    }

    public Map<Port, Port> getKeptPortPairings() {
        return keptPortPairings;
    }

    public Set<Object> getDeviceVertices() {
        return deviceVertices;
    }

    public List<PortGroup> getDummyPortGroupsForEdgeBundles() {
        return dummyPortGroupsForEdgeBundles;
    }

    public Map<Vertex, Set<Port>> getDummyPortsForLabelPadding() {
        return dummyPortsForLabelPadding;
    }

    public Map<PortPairing, PortPairing> getReplacedPortPairings() {
        return replacedPortPairings;
    }

    public void addDummyPortsForNodesWithoutPort(Port port) {
        dummyPortsForNodesWithoutPort.add(port);
    }

    public Map<EdgeBundle, Collection<Edge>> getOriginalEdgeBundles() {
        return originalEdgeBundles;
    }

    /////////////////
    // for testing //
    /////////////////

    public int getNumberOfDummys () {
        return dummyNodesLongEdges.size();
    }

    public int getNumberOfCrossings () {
        return countCrossings(orders);
    }
}
