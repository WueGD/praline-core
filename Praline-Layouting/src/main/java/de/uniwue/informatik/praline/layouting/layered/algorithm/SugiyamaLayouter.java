package de.uniwue.informatik.praline.layouting.layered.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.svg.SVGDrawer;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.io.output.util.DrawingUtils;
import de.uniwue.informatik.praline.layouting.PralineLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.HandlingDeadEnds;
import de.uniwue.informatik.praline.layouting.layered.algorithm.drawing.DrawingPreparation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgerouting.EdgeRouting;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.*;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.AlignmentParameters;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.NodePlacement;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.ConnectedComponentClusterer;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.DummyCreationResult;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.DummyNodeCreation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing.GraphPreprocessor;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;

import java.awt.geom.Rectangle2D;
import java.util.*;

public class SugiyamaLayouter implements PralineLayouter {

    public static final DirectionMethod DEFAULT_DIRECTION_METHOD = DirectionMethod.FORCE;
    public static final LayerAssignmentMethod DEFAULT_LAYER_ASSIGNMENT_METHOD = LayerAssignmentMethod.NETWORK_SIMPLEX;
    public static final int DEFAULT_NUMBER_OF_FD_ITERATIONS = 1;
    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.PORTS;
    public static final int DEFAULT_NUMBER_OF_CM_ITERATIONS = 5; //iterations for crossing minimization
    public static final boolean DEFAULT_DETERMINE_SIDE_LENGTHS_OF_NODES = false; //for NodePlacement; see there
    public static final AlignmentParameters.Method
            DEFAULT_ALIGNMENT_METHOD = AlignmentParameters.Method.FIRST_COMES; //for NodePlacement; see there
    public static final AlignmentParameters.Preference
            DEFAULT_ALIGNMENT_PREFERENCE = AlignmentParameters.Preference.LONG_EDGE; //for NodePlacement; see there



    private Graph graph;
    private DrawingInformation drawInfo;

    private boolean isSingleComponent;

    /////////
    //if it has multiple components use these substructures
    /////////
    private List<SugiyamaLayouter> componentLayouters;
    private Graph combinedGraph;

    /////////
    //otherwise use these structures
    /////////
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
    private Map<Vertex, Double> preSetWidth;
    private Map<Vertex, Double> preSetHeight;

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

    //internal
    private boolean useFDLayoutForInitialNodeOrder;
    private DirectionAssignment da;

    public SugiyamaLayouter(Graph graph) {
        this(graph, new DrawingInformation());
    }

    public SugiyamaLayouter(Graph graph, DrawingInformation drawInfo) {
        this(graph, drawInfo, false);
    }

    private SugiyamaLayouter(Graph graph, DrawingInformation drawInfo, boolean isSingleComponent) {
        this.graph = graph;
        this.drawInfo = drawInfo;

        //find components
        List<Graph> components = null;
        if (!isSingleComponent) {
            ConnectedComponentClusterer clusterer = new ConnectedComponentClusterer(graph);
            components = clusterer.getConnectedComponentsBySize();

            if (components.size() == 1) {
                isSingleComponent = true;
            }
        }

        //if it is a single component use this to draw the graph
        if (isSingleComponent) {
            initialize();
        }
        //otherwise create a new SugiyamaLayouter for each connected component
        else {

            componentLayouters = new ArrayList<>(components.size());

            for (Graph component : components) {
                componentLayouters.add(new SugiyamaLayouter(component, drawInfo, true));
            }
        }

        this.isSingleComponent = isSingleComponent;
    }

    @Override
    public void computeLayout() {
        computeLayout(DEFAULT_DIRECTION_METHOD, DEFAULT_LAYER_ASSIGNMENT_METHOD, DEFAULT_NUMBER_OF_FD_ITERATIONS,
                DEFAULT_CROSSING_MINIMIZATION_METHOD, DEFAULT_NUMBER_OF_CM_ITERATIONS, DEFAULT_ALIGNMENT_METHOD,
                DEFAULT_ALIGNMENT_PREFERENCE);
    }

    /**
     *  @param directionMethod
     * @param layerAssignmentMethod
     * @param numberOfIterationsFD
 *      when employing a force-directed algo, it uses so many iterations with different random start positions
 *      and takes the one that yields the fewest crossings.
 *      If you use anything different from {@link DirectionMethod#FORCE}, then this value will be ignored.
     * @param cmMethod
     * @param numberOfIterationsCM
*      for the crossing minimization phase you may have several independent random iterations of which the one
     */
    public void computeLayout(DirectionMethod directionMethod, LayerAssignmentMethod layerAssignmentMethod,
                              int numberOfIterationsFD, CrossingMinimizationMethod cmMethod, int numberOfIterationsCM,
                              AlignmentParameters.Method alignmentMethod,
                              AlignmentParameters.Preference alignmentPreference) {
        construct();
        assignDirections(directionMethod, numberOfIterationsFD);
        assignLayers(layerAssignmentMethod, directionMethod);
        createDummyNodesAndDoCrossingMinimization(cmMethod, numberOfIterationsCM);
        nodePositioning(alignmentMethod, alignmentPreference);
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
        if (isSingleComponent) {
            //register pre-set node sizes
            for (Vertex vertex : graph.getVertices()) {
                Shape shape = vertex.getShape();
                if (shape != null) {
                    if (shape instanceof Rectangle2D) {
                        double width = ((Rectangle2D) shape).getWidth();
                        if (Double.isFinite(width)) {
                            preSetWidth.put(vertex, width);
                        }
                        double height = ((Rectangle2D) shape).getHeight();
                        if (Double.isFinite(height)) {
                            preSetHeight.put(vertex, height);
                        }
                    }
                }
            }
            //do preprocessing
            GraphPreprocessor graphPreprocessor = new GraphPreprocessor(this);
            graphPreprocessor.construct();
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.construct();
            }
        }
    }

    public void assignDirections (DirectionMethod method) {
        if (isSingleComponent) {
            assignDirections(method, 1);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.assignDirections(method);
            }
        }
    }

    /**
     *
     * @param method
     * @param numberOfIterationsForForceDirected
     *      when employing a force-directed algo, it uses so many iterations with different random start positions
     *      and takes the one that yields the fewest crossings.
     *      If you use anything different from {@link DirectionMethod#FORCE}, then this value will be ignored.
     */
    public void assignDirections(DirectionMethod method, int numberOfIterationsForForceDirected) {
        if (isSingleComponent) {
            da = new DirectionAssignment();
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
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.assignDirections(method, numberOfIterationsForForceDirected);
            }
        }
    }
//    public void copyDirections(SugiyamaLayouter otherSugiyamaLayouterWithSameGraph)  {
//        for (Edge edge : otherSugiyamaLayouterWithSameGraph.getGraph().getEdges()) {
//            this.assignDirection(edge, otherSugiyamaLayouterWithSameGraph.getStartNode(edge),
//                    otherSugiyamaLayouterWithSameGraph.getEndNode(edge));
//        }
//
//        //check that all edges got a direction
//        for (Edge edge : this.getGraph().getEdges()) {
//            if (!edgeToStart.containsKey(edge)) {
//                throw new NoSuchElementException("No edge direction found to copy. The input parameter " +
//                        "otherSugiyamaLayouterWithSameGraph has either not yet directions assigned or the graph is not "
//                        + "identical with the graph of this SugiyamaLayouter object.");
//            }
//        }
//    }

    public void assignLayers(LayerAssignmentMethod layerAssignmentMethod, DirectionMethod directionMethod) {
        if (isSingleComponent) {
            LayerAssignment la = null;
            useFDLayoutForInitialNodeOrder = layerAssignmentMethod != LayerAssignmentMethod.OLD_NETWORK_SIMPLEX
                    && directionMethod == DirectionMethod.FORCE;
            if (layerAssignmentMethod == LayerAssignmentMethod.NETWORK_SIMPLEX) {
                la = new LayerAssignmentNetworkSimplex(this, da);
            }
            else if (layerAssignmentMethod == LayerAssignmentMethod.OLD_NETWORK_SIMPLEX) {
                la = new OldLayerAssignmentNetworkSimplex(this);
            }
            else if (layerAssignmentMethod == LayerAssignmentMethod.FD_POSITION) {
                la = new LayerAssignmentForceDirected(this, da);
            }
            nodeToRank = la.assignLayers();
            PortSideAssignment pa = new PortSideAssignment(this);
            pa.assignPortsToVertexSides();
            createRankToNodes();
            hasAssignedLayers = true;
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.assignLayers(layerAssignmentMethod, directionMethod);
            }
        }
    }

//    public void createDummyNodes() {
//        DummyNodeCreation dnc = new DummyNodeCreation(this);
//        DummyCreationResult dummyNodeData = dnc.createAllDummyNodes();
//        this.dummyNodesLongEdges = dummyNodeData.getDummyNodesLongEdges();
//        this.dummyNodesSelfLoops = dummyNodeData.getDummyNodesSelfLoops();
//        this.dummyTurningNodes = dummyNodeData.getDummyTurningNodes();
//        this.nodeToLowerDummyTurningPoint = dummyNodeData.getNodeToLowerDummyTurningPoint();
//        this.nodeToUpperDummyTurningPoint = dummyNodeData.getNodeToUpperDummyTurningPoint();
//        this.correspondingPortsAtDummy = dummyNodeData.getCorrespondingPortsAtDummy();
//        for (Edge edge : dummyNodeData.getDummyEdge2RealEdge().keySet()) {
//            this.dummyEdge2RealEdge.put(edge, dummyNodeData.getDummyEdge2RealEdge().get(edge));
//        }
//    }

    public void createDummyNodesAndDoCrossingMinimization(CrossingMinimizationMethod cmMethod, int numberOfIterations) {
        if (isSingleComponent) {
            createDummyNodesAndDoCrossingMinimization(cmMethod,
                    CrossingMinimization.DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                    CrossingMinimization.DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX,
                    CrossingMinimization.DEFAULT_HANDLING_DEAD_ENDS, numberOfIterations);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.createDummyNodesAndDoCrossingMinimization(cmMethod, numberOfIterations);
            }
        }
    }

    public void createDummyNodesAndDoCrossingMinimization(CrossingMinimizationMethod cmMethod,
                                                          boolean movePortsAdjToTurningDummiesToTheOutside,
                                                          boolean placeTurningDummiesNextToTheirVertex,
                                                          HandlingDeadEnds handlingDeadEnds,
                                                          int numberOfIterations) {
        if (isSingleComponent) {
            //first crossing minimization phase with all ports on the side of its edge direction
            DummyNodeCreation dnc = new DummyNodeCreation(this);
            dnc.assignWrongSidePortsTemporaryToOtherSide();
            dnc.createDummyNodesForEdges();
            CrossingMinimization cm1 = new CrossingMinimization(this);
            SortingOrder result = cm1.layerSweepWithBarycenterHeuristic(cmMethod, orders,
                    !useFDLayoutForInitialNodeOrder, movePortsAdjToTurningDummiesToTheOutside,
                    placeTurningDummiesNextToTheirVertex, false, handlingDeadEnds);
            orders = result;
            int crossings = countCrossings(result);
            for (int i = 1; i < numberOfIterations; i++) {
                result = cm1.layerSweepWithBarycenterHeuristic(cmMethod, orders, true,
                        movePortsAdjToTurningDummiesToTheOutside, placeTurningDummiesNextToTheirVertex, false,
                        handlingDeadEnds);
                int crossingsNew = countCrossings(result);
                if (crossingsNew < crossings) {
                    crossings = crossingsNew;
                    orders = result;
                }
            }
            //second crossing minimization phase with all ports on their "real" side
            dnc.undoAssigningPortsTemporaryToOtherSide();
            DummyCreationResult dummyNodeData = dnc.createAllDummyNodes();
            this.dummyNodesLongEdges = dummyNodeData.getDummyNodesLongEdges();
            this.dummyNodesSelfLoops = dummyNodeData.getDummyNodesSelfLoops();
            this.dummyTurningNodes = dummyNodeData.getDummyTurningNodes();
            this.nodeToLowerDummyTurningPoint = dummyNodeData.getNodeToLowerDummyTurningPoint();
            this.nodeToUpperDummyTurningPoint = dummyNodeData.getNodeToUpperDummyTurningPoint();
            this.correspondingPortsAtDummy = dummyNodeData.getCorrespondingPortsAtDummy();
            for (Edge edge : dummyNodeData.getDummyEdge2RealEdge().keySet()) {
                this.dummyEdge2RealEdge.put(edge, dummyNodeData.getDummyEdge2RealEdge().get(edge));
            }
            CrossingMinimization cm2 = new CrossingMinimization(this);
            orders = cm2.layerSweepWithBarycenterHeuristic(cmMethod, orders, false,
                    movePortsAdjToTurningDummiesToTheOutside, placeTurningDummiesNextToTheirVertex, true,
                    handlingDeadEnds);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.createDummyNodesAndDoCrossingMinimization(cmMethod,
                        movePortsAdjToTurningDummiesToTheOutside, placeTurningDummiesNextToTheirVertex,
                        handlingDeadEnds, numberOfIterations);
            }
        }
    }

    public void nodePositioning(AlignmentParameters.Method alignmentMethod,
                                AlignmentParameters.Preference alignmentPreference) {
        if (isSingleComponent) {
            NodePlacement np = new NodePlacement(this, orders, drawInfo);
            dummyPortsForLabelPadding = np.placeNodes(DEFAULT_DETERMINE_SIDE_LENGTHS_OF_NODES, alignmentMethod,
                    alignmentPreference);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.nodePositioning(alignmentMethod, alignmentPreference);
            }
        }
    }

    /**
     * only needed if
     * {@link SugiyamaLayouter#nodePositioning(AlignmentParameters.Method, AlignmentParameters.Preference)} is not used.
     */
    public void nodePadding() {
        if (isSingleComponent) {
            NodePlacement np = new NodePlacement(this, orders, drawInfo);
            np.initialize();
            np.initializeStructure();
            dummyPortsForLabelPadding = np.dummyPortsForWidth(true);
            np.reTransformStructure(false);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.nodePadding();
            }
        }
    }

    public void edgeRouting() {
        if (isSingleComponent) {
            EdgeRouting er = new EdgeRouting(this, orders, drawInfo);
            er.routeEdges();
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.edgeRouting();
            }
        }
    }

    public void prepareDrawing() {
        if (isSingleComponent) {
            DrawingPreparation dp = new DrawingPreparation(this);
            dp.prepareDrawing(drawInfo, orders, dummyPortsForLabelPadding, dummyPortsForNodesWithoutPort);
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.prepareDrawing();
            }

            //combine drawings of separate layouters
            unifyDrawings();
        }
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
        if (isSingleComponent) {
            DrawingPreparation dp = new DrawingPreparation(this);
            dp.initialize(drawInfo, orders, dummyPortsForLabelPadding, dummyPortsForNodesWithoutPort);
            dp.restoreOriginalElements(true);
            dp.tightenNodes();
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                componentLayouter.restoreOriginalElements();
            }
        }
    }

    public void drawResult(String path) {
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
        preSetWidth = new LinkedHashMap<>();
        preSetHeight = new LinkedHashMap<>();
    }

    // other steps //

    private void unifyDrawings() {
        double xOffset = 0;
        for (SugiyamaLayouter componentLayouter : componentLayouters) {
            //finde offset for all graphs and move objects
            Rectangle2D drawingBounds = DrawingUtils.determineDrawingBounds(componentLayouter.getGraph(), drawInfo, 0);
            xOffset -= drawingBounds.getX();
            if (xOffset != 0) {
                DrawingUtils.translate(componentLayouter.getGraph(), xOffset, 0);
            }
            xOffset += drawingBounds.getWidth() + drawingBounds.getX() + drawInfo.getDistanceBetweenConnectedComponents();
        }
    }

    private void createRankToNodes() {
        rankToNodes = new LinkedHashMap<>();
        for (Vertex node : nodeToRank.keySet()) {
            int key = nodeToRank.get(node);
            if (!rankToNodes.containsKey(key)) {
                rankToNodes.put(key, new LinkedHashSet<Vertex>());
            }
            rankToNodes.get(key).add(node);
        }
    }
    public int countCrossings(SortingOrder sortingOrder) {
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
    public Port getPairedPort(Port port) {
        if (isSingleComponent) {
            return keptPortPairings.get(port);
        }
        //go through all sublayouters otherwise; this is needed for kieler layouter
        for (SugiyamaLayouter componentLayouter : componentLayouters) {
            Port candidatePort = componentLayouter.getPairedPort(port);
            if (candidatePort != null) {
                return candidatePort;
            }
        }
        return null;
    }

    public boolean isPaired(Port port) {
        return keptPortPairings.containsKey(port);
    }

    public boolean staysOnSameLayer(Edge edge) {
        int rank = getRank(getStartNode(edge));
        int i = getRank(getEndNode(edge));
        Vertex startNode = getStartNode(edge);
        Vertex endNode = getEndNode(edge);
        return getRank(getStartNode(edge)) == getRank(getEndNode(edge));
    }

    public Vertex getStartNode(Edge edge) {
        return edgeToStart.get(edge);
    }

    public Vertex getEndNode(Edge edge) {
        return edgeToEnd.get(edge);
    }

    public Collection<Edge> getOutgoingEdges(Vertex node) {
        if (nodeToOutgoingEdges.get(node) == null) return new LinkedList<>();
        return Collections.unmodifiableCollection(nodeToOutgoingEdges.get(node));
    }

    public Collection<Edge> getIncomingEdges(Vertex node) {
        if (nodeToIncomingEdges.get(node) == null) return new LinkedList<>();
        return Collections.unmodifiableCollection(nodeToIncomingEdges.get(node));
    }

    public boolean assignDirection(Edge edge, Vertex start, Vertex end) {
        if (edgeToStart.containsKey(edge)) {
            removeDirection(edge);
        }
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

    public boolean removeDirection(Edge edge) {
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

    public int getRank(Vertex node) {
        if (isSingleComponent) {
            if (nodeToRank.containsKey(node)) return nodeToRank.get(node);
            return -1;
        }
        //go through all sublayouters otherwise; this is needed for kieler layouter
        for (SugiyamaLayouter componentLayouter : componentLayouters) {
            int candidateRank = componentLayouter.getRank(node);
            if (candidateRank >= 0) {
                return candidateRank;
            }
        }
        return -1;
    }

    public void setRank(Vertex node, Integer rank) {
        if (nodeToRank.containsKey(node)) {
            int oldRank = getRank(node);
            Collection<Vertex> oldRankSet = rankToNodes.get(oldRank);
            if (oldRankSet != null) {
                oldRankSet.remove(node);
                if (oldRankSet.isEmpty()) {
                    rankToNodes.remove(oldRank);
                }
            }
            nodeToRank.replace(node, rank);
        } else {
            nodeToRank.put(node, rank);
        }
        rankToNodes.putIfAbsent(rank, new LinkedHashSet<>());
        rankToNodes.get(rank).add(node);
    }

    public void changeRanksAccordingToSortingOrder() {
        if (isSingleComponent) {
            rankToNodes.clear();
            List<List<Vertex>> nodeOrder = orders.getNodeOrder();
            for (int i = 0; i < nodeOrder.size(); i++) {
                List<Vertex> layer = nodeOrder.get(i);
                for (Vertex node : layer) {
                    setRank(node, i);
                }
            }
        }
        else {
            //split sorting order to several sub sorting orders and apply them to the sub layouts
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                SortingOrder componentOrders = getSubOrder(orders, componentLayouter.getGraph());
                componentLayouter.setOrders(componentOrders);
                componentLayouter.changeRanksAccordingToSortingOrder();
            }
        }
    }

    private SortingOrder getSubOrder(SortingOrder orders, Graph subGraph) {
        SortingOrder subOrder = new SortingOrder(orders);
        Set<Vertex> subVertices = new LinkedHashSet<>(subGraph.getVertices());
        //remove vertices not in this graph from node order
        for (List<Vertex> layer : subOrder.getNodeOrder()) {
            for (int i = layer.size() - 1; i >= 0; i--) {
                Vertex vertex = layer.get(i);
                if (!subVertices.contains(vertex)) {
                    layer.remove(vertex);
                }
            }
        }
        //remove empty layers at the end of the list
        int rank = subOrder.getNodeOrder().size() - 1;
        while (rank >= 0 && subOrder.getNodeOrder().get(rank).isEmpty()) {
            subOrder.getNodeOrder().remove(rank);
            --rank;
        }
        //remove vertices not in this graph from port order
        for (Vertex vertex : new ArrayList<>(subOrder.getBottomPortOrder().keySet())) {
            if (!subVertices.contains(vertex)) {
                subOrder.getBottomPortOrder().remove(vertex);
            }
        }
        for (Vertex vertex : new ArrayList<>(subOrder.getTopPortOrder().keySet())) {
            if (!subVertices.contains(vertex)) {
                subOrder.getTopPortOrder().remove(vertex);
            }
        }

        return subOrder;
    }

    public Collection<Vertex> getAllNodesWithRank(int rank) {
        if (rankToNodes.containsKey(rank)) {
            return Collections.unmodifiableCollection(rankToNodes.get(rank));
        } else {
            return new LinkedHashSet<>();
        }
    }

    public int getMaxRank() {
        int max = 0;
        for (int rank : rankToNodes.keySet()) {
            if (rank > max) max = rank;
        }
        return max;
    }

    public boolean isPlug(Vertex possiblePlug) {
        return plugs.keySet().contains(possiblePlug);
    }

    public boolean isUnionNode(Vertex node) {
        return vertexGroups.keySet().contains(node) || plugs.keySet().contains(node);
    }

    public boolean isDummy(Vertex node) {
        return isDummyNodeOfLongEdge(node) || isDummyNodeOfSelfLoop(node) || isDummyTurningNode(node) ||
                isDummyNodeForEdgesOfDeg1or0(node) || isDummyNodeForNodelessPorts(node) ||
                isHyperEdgeDummy(node);
    }

    public boolean isHyperEdgeDummy(Vertex node) {
        return getHyperEdges().containsKey(node);
    }

    public Map<Vertex, Edge> getDummyNodesLongEdges() {
        return dummyNodesLongEdges;
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

    public Vertex getVertexOfTurningDummy(Vertex turningDummy) {
        return dummyTurningNodes.get(turningDummy);
    }

    public Port getCorrespondingPortAtDummy(Port port) {
        return correspondingPortsAtDummy.get(port);
    }

    public boolean isTopPort(Port port) {
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

    public Map<Edge, Edge> getDummyEdge2RealEdge() {
        return dummyEdge2RealEdge;
    }

    public Edge getOriginalEdgeExceptForHyperE(Vertex dummyNodeLongEdge) {
        Edge longEdge = dummyNodesLongEdges.get(dummyNodeLongEdge);
        if (longEdge == null) {
            return null;
        }
        return getOriginalEdgeExceptForHyperE(longEdge);
    }

    public Edge getOriginalEdgeExceptForHyperE(Edge edge) {
        Edge currentEdge = edge;
        while (dummyEdge2RealEdge.containsKey(currentEdge)) {
            currentEdge = dummyEdge2RealEdge.get(currentEdge);
        }
        return currentEdge;
    }

    public Edge getOriginalEdge(Edge edge) {
        Edge originalEdgeExceptForHyperE = getOriginalEdgeExceptForHyperE(edge);
        Edge originalEdge = hyperEdges.get(hyperEdgeParts.get(originalEdgeExceptForHyperE));
        if (originalEdge == null) {
            return originalEdgeExceptForHyperE;
        }
        return originalEdge;
    }

    public Map<Vertex, Set<Edge>> getLoopEdges() {
        if (isSingleComponent) {
            return loopEdges;
        }
        //there is more than one component -> construct a new map from the maps of the components
        //this is needed for the kieler layouter
        if (loopEdges == null) {
            loopEdges = new LinkedHashMap<>();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                for (Map.Entry<Vertex, Set<Edge>> entry : componentLayouter.getLoopEdges().entrySet()) {
                    loopEdges.put(entry.getKey(), entry.getValue());
                }
            }
        }
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

    public List<Port> getPortsOfLoopEdge(Edge loopEdge) {
        if (isSingleComponent) {
            if (loopEdge2Ports.containsKey(loopEdge)) {
                return Collections.unmodifiableList(loopEdge2Ports.get(loopEdge));
            } else {
                return Collections.unmodifiableList(new ArrayList<>());
            }
        }
        //otherwise check component layouters; needed for kieler layouter
        for (SugiyamaLayouter componentLayouter : componentLayouters) {
            List<Port> candidatePortsOfLoopEdge = componentLayouter.getPortsOfLoopEdge(loopEdge);
            if (!candidatePortsOfLoopEdge.isEmpty()) {
                return candidatePortsOfLoopEdge;
            }
        }
        return Collections.unmodifiableList(new ArrayList<>());
    }

    public boolean hasAssignedLayers() {
        return hasAssignedLayers;
    }

    public double getMinWidthForNode(Vertex node) {
        if (isSingleComponent) {
            if (isDummy(node)) {
                return 0;
            }
            if (isPlug(node) || vertexGroups.containsKey(node)) {
                double maxWidth = 0;
                VertexGroup vertexGroup = isPlug(node) ? plugs.get(node) : vertexGroups.get(node);
                for (Vertex originalVertex : vertexGroup.getAllRecursivelyContainedVertices()) {
                    maxWidth = Math.max(maxWidth,
                            Math.max(preSetWidth.getOrDefault(originalVertex, 0d),
                                    drawInfo.computeMinVertexWidth(originalVertex)));
                }
                return maxWidth;
            }
            return Math.max(preSetWidth.getOrDefault(node, 0d), drawInfo.computeMinVertexWidth(node));
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                if (componentLayouter.getGraph().getVertices().contains(node)) {
                    return componentLayouter.getMinWidthForNode(node);
                }
            }
            return -1;
        }
    }

    public double getMinHeightForNode(Vertex node) {
        if (isSingleComponent) {
            if (isDummy(node)) {
                return 0;
            }
            if (isPlug(node) || vertexGroups.containsKey(node)) {
                double maxHeight = 0;
                VertexGroup vertexGroup = isPlug(node) ? plugs.get(node) : vertexGroups.get(node);
                for (Vertex originalVertex : vertexGroup.getAllRecursivelyContainedVertices()) {
                    maxHeight = Math.max(maxHeight,
                            Math.max(preSetHeight.getOrDefault(originalVertex, 0d),
                                    drawInfo.computeMinVertexHeight(originalVertex)));
                }
                return maxHeight;
            }
            return Math.max(preSetHeight.getOrDefault(node, 0d), drawInfo.computeMinVertexHeight(node));
        }
        else {
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                if (componentLayouter.getGraph().getVertices().contains(node)) {
                    return componentLayouter.getMinWidthForNode(node);
                }
            }
            return -1;
        }
    }


        public double getWidthForPort(Port port) {
        if (isSingleComponent) {
            //first find original port
            Port originalPort = port;
            while (replacedPorts.containsKey(originalPort)) {
                originalPort = replacedPorts.get(originalPort);
            }
            double width = drawInfo.computePortWidth(originalPort);//compute the width of the original port
            //potentially this port is connected within a port group to a port that does not have access to the
            // outside any more, but it may be broad port to make this port also wider
            if (vertexGroups.containsKey(port.getVertex())) {
                VertexGroup vertexGroup = vertexGroups.get(port.getVertex());
                PortPairing portPairing = PortUtils.getPortPairing(originalPort, vertexGroup);
                if (portPairing != null) {
                    Port otherPortOfPortPairing = PortUtils.getOtherPortOfPortPairing(portPairing, originalPort);
                    width = Math.max(width, drawInfo.computePortWidth(otherPortOfPortPairing));
                }
            }
            // then check replacements of single ports by multiple ports because they divide the width
            if (multipleEdgePort2replacePorts.containsKey(originalPort)) {
                int numberOfReplacePorts = multipleEdgePort2replacePorts.get(originalPort).size();
                width -= (numberOfReplacePorts - 1) * drawInfo.getPortSpacing();
                width = Math.max(width, 0); //forbid negative values
                width /= (double) numberOfReplacePorts;
                //we also have to make sure that we have enough edge spacing
                width = Math.max(width, drawInfo.getEdgeDistanceHorizontal() - drawInfo.getPortSpacing());
            }
            return width;
        }
        else {
            Vertex vertex = port.getVertex();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                if (componentLayouter.getGraph().getVertices().contains(vertex)) {
                    return componentLayouter.getWidthForPort(port);
                }
            }
            return -1;
        }
    }

    //this method is used by kieler layouter
    public Graph getGraphWithPreprocessedVertices() {
        if (isSingleComponent) {
            return getGraph();
        }
        //otherwise the preprocessing has been taken place in the sublayouters -> combine these graphs to one new graph
        if (combinedGraph == null) {
            combinedGraph = new Graph();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                Graph componentGraph = componentLayouter.getGraph();
                combinedGraph.addVertices(componentGraph.getVertices());
                combinedGraph.addVertexGroups(componentGraph.getVertexGroups());
                combinedGraph.addEdges(componentGraph.getEdges());
                combinedGraph.addEdgeBundles(componentGraph.getEdgeBundles());
            }
        }
        return combinedGraph;
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
        if (isSingleComponent) {
            return orders;
        }
        //there is more than one component -> construct a new order from the orders of the components
        //this is needed for the kieler layouter
        if (orders == null) {
            orders = new SortingOrder();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                SortingOrder componentOrders = componentLayouter.orders;
                //combine node orders
                for (int i = 0; i < componentOrders.getNodeOrder().size(); i++) {
                    if (orders.getNodeOrder().size() <= i) {
                        orders.getNodeOrder().add(new ArrayList<>());
                    }
                    orders.getNodeOrder().get(i).addAll(componentOrders.getNodeOrder().get(i));
                }
                //combine port orders
                for (Map.Entry<Vertex, List<Port>> entry : componentOrders.getBottomPortOrder().entrySet()) {
                    orders.getBottomPortOrder().put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<Vertex, List<Port>> entry : componentOrders.getTopPortOrder().entrySet()) {
                    orders.getTopPortOrder().put(entry.getKey(), entry.getValue());
                }
            }
        }
        return orders;
    }

    public void setOrders(SortingOrder orders) {
        this.orders = orders;
    }

    public Map<Vertex, VertexGroup> getPlugs() {
        if (isSingleComponent) {
            return plugs;
        }
        //there is more than one component -> construct a new map from the maps of the components
        //this is needed for the kieler layouter
        if (plugs == null) {
            plugs = new LinkedHashMap<>();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                for (Map.Entry<Vertex, VertexGroup> plugEntry : componentLayouter.getPlugs().entrySet()) {
                    plugs.put(plugEntry.getKey(), plugEntry.getValue());
                }
            }
        }
        return plugs;
    }

    public Map<Vertex, VertexGroup> getVertexGroups() {
        if (isSingleComponent) {
            return vertexGroups;
        }
        //there is more than one component -> construct a new map from the maps of the components
        //this is needed for the kieler layouter
        if (vertexGroups == null) {
            vertexGroups = new LinkedHashMap<>();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                for (Map.Entry<Vertex, VertexGroup> vgEntry : componentLayouter.getVertexGroups().entrySet()) {
                    vertexGroups.put(vgEntry.getKey(), vgEntry.getValue());
                }
            }
        }
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
        if (isSingleComponent) {
            return dummyPortsForLabelPadding;
        }
        //there is more than one component -> construct a new map from the maps of the components
        //this is needed for the kieler layouter
        if (dummyPortsForLabelPadding == null) {
            dummyPortsForLabelPadding = new LinkedHashMap<>();
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                for (Map.Entry<Vertex, Set<Port>> entry : componentLayouter.getDummyPortsForLabelPadding().entrySet()) {
                    dummyPortsForLabelPadding.put(entry.getKey(), entry.getValue());
                }
            }
        }
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

    public int getNumberOfDummys() {
        if (isSingleComponent) {
            return dummyNodesForNodelessPorts.size() + dummyNodesSelfLoops.size() + dummyTurningNodes.size()
                    + dummyNodesLongEdges.size() + dummyNodesForEdgesOfDeg1or0.size() + hyperEdges.size();
        }
        else {
            int sum = 0;
            for (SugiyamaLayouter componentLayouter : componentLayouters) {
                sum += componentLayouter.getNumberOfDummys();
            }
            return sum;
        }
    }
}
