package de.uniwue.informatik.praline.layouting.layered.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.svg.SVGDrawer;
import de.uniwue.informatik.praline.layouting.PralineLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimization2;
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
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.ImplicitCharacteristics;

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
    private Map<Vertex, Edge> hyperEdges;
    private Map<Edge, Vertex> hyperEdgeParts;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Port, Port> replacedPorts;
    private Map<Port, List<Port>> multipleEdgePort2replacePorts;
    private Map<Port, Port> keptPortPairings;
    private Map<Edge, Edge> dummyEdge2RealEdge;
    private Map<Vertex, Set<Edge>> loopEdges;
    private Map<Edge, Set<Port>> loopEdge2Ports;
    private Map<Vertex, Set<Port>> dummyPortsForLabelPadding;

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
    private CMResult orders;
    private boolean hasAssignedLayers;
    private Set<Object> deviceVertices;

    //TODO: take into account pre-set values like port.getOrientationAtVertex(), fixed order port groups
    //TODO: edge bundles

    public SugiyamaLayouter(Graph graph) {
        this(graph, new DrawingInformation());
    }

    public SugiyamaLayouter(Graph graph, DrawingInformation drawInfo) {
        this.graph = graph;
        initialise();
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
        //chose methods for directionassignment
        //chose other steps to be done or not
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
    // each Edge has exact two Ports
    // each Port has not max one Edge
    // VertexGroups are replaced by a single node
    // if all Nodes of a Group are touching each other PortGroups are kept
    // save changes to resolve later
    // todo: change method back to private when done with debugging and testing

    public void construct() {
        //handle edge bundles
//        handleEdgeBundle(); //TODO uncomment and fix (+ re-inserting)
        // handle Port if it has no Vertex
        handlePortWithoutNode();
        // handle Edge if connected to more than two Ports
        handleHyperEdges();
        // handle Port if it has more than one Edge
        handlePort();
        // handle VertexGroup
        handleVertexGroup();
        // handle Edge if both Ports have same Vertex
//        handleLoopEdges(); //TODO: check
    }
    // todo: change method back to private when done with debugging and testing

    public void assignDirections (DirectionMethod method) {
        assignDirections(method, 1);
    }

    // todo: change method back to private when done with debugging and testing

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
            this.assignDirection(edge,
                    otherSugiyamaLayouterWithSameGraph.getStartNode(edge), otherSugiyamaLayouterWithSameGraph.getEndNode(edge));
        }

        //check that all edges got a direction
        for (Edge edge : this.getGraph().getEdges()) {
            if (!edgeToStart.containsKey(edge)) {
                throw new NoSuchElementException("No edge direction found to copy. The input parameter " +
                        "otherSugiyamaLayouterWithSameGraph has either not yet directions assigned or the graph is not " +
                        "identical with the graph of this SugiyamaLayouter object.");
            }
        }
    }

    // todo: change method back to private when done with debugging and testing

    public void assignLayers () {
        LayerAssignment la = new LayerAssignment(this);
        nodeToRank = la.networkSimplex();
        createRankToNodes();
        hasAssignedLayers = true;
    }
    // todo: change method back to private when done with debugging and testing

    public void createDummyNodes() {
        DummyNodeCreation dnc = new DummyNodeCreation(this);
        DummyCreationResult dummyNodeData = dnc.createDummyNodes();
        this.dummyNodesLongEdges = dummyNodeData.getDummyNodesLongEdges();
        this.dummyTurningNodes = dummyNodeData.getDummyTurningNodes();
        this.nodeToLowerDummyTurningPoint = dummyNodeData.getNodeToLowerDummyTurningPoint();
        this.nodeToUpperDummyTurningPoint = dummyNodeData.getNodeToUpperDummyTurningPoint();
        this.correspondingPortsAtDummy = dummyNodeData.getCorrespondingPortsAtDummy();
        for (Edge edge : dummyNodeData.getDummyEdge2RealEdge().keySet()) {
            this.dummyEdge2RealEdge.put(edge, dummyNodeData.getDummyEdge2RealEdge().get(edge));
        }
    }

    // todo: change method back to private when done with debugging and testing

    public void crossingMinimization (CrossingMinimizationMethod cmMethod, int numberOfIterations) {
        crossingMinimization(cmMethod, CrossingMinimization2.DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE
                , CrossingMinimization2.DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX, numberOfIterations);
    }
    // todo: change method back to private when done with debugging and testing

    public void crossingMinimization(CrossingMinimizationMethod cmMethod,
                                     boolean movePortsAdjToTurningDummiesToTheOutside,
                                     boolean placeTurningDummiesNextToTheirVertex, int numberOfIterations) {
        CrossingMinimization2 cm = new CrossingMinimization2(this);
        CMResult result = cm.layerSweepWithBarycenterHeuristic(null, cmMethod, movePortsAdjToTurningDummiesToTheOutside,
                placeTurningDummiesNextToTheirVertex);
        orders = result;
        int crossings = countCrossings(result);
        for (int i = 1; i < numberOfIterations; i++) {
            result = cm.layerSweepWithBarycenterHeuristic(null, cmMethod, movePortsAdjToTurningDummiesToTheOutside,
                    placeTurningDummiesNextToTheirVertex);
            int crossingsNew = countCrossings(result);
            if (crossingsNew < crossings) {
                crossings = crossingsNew;
                orders = result;
            }
        }
    }
    // todo: delete when done with debugging and testing

    public List<Integer> crossingMinimization (CrossingMinimizationMethod cmMethod, int numberOfIterations,
                                               List<List<Vertex>> currentNodeOrder) {
        List<Integer> allCrossings = new ArrayList<>();
        for (int i = 0; i < numberOfIterations; i++) {
            CrossingMinimization2 cm = new CrossingMinimization2(this);
            allCrossings.add(countCrossings(cm.layerSweepWithBarycenterHeuristic(Collections.unmodifiableList(currentNodeOrder), cmMethod)));
        }
        return allCrossings;
    }
    // todo: change method back to private when done with debugging and testing

    public void nodePositioning () {
        NodePlacement np = new NodePlacement(this, orders, drawInfo);
        dummyPortsForLabelPadding = np.placeNodes();
    }
    // todo: change method back to private when done with debugging and testing

    public void edgeRouting () {
        EdgeRouting er = new EdgeRouting(this, orders, drawInfo);
        er.routeEdges();
    }
    // todo: change method back to private when done with debugging and testing

    public void prepareDrawing () {
        DrawingPreparation dp = new DrawingPreparation(this);
        dp.prepareDrawing(drawInfo, orders, dummyPortsForLabelPadding);
    }

    /**
     * This is already done when calling {@link SugiyamaLayouter#prepareDrawing()}.
     * So only use this if, the former one is not used!
     */
    public void restoreOriginalElements() {
        DrawingPreparation dp = new DrawingPreparation(this);
        dp.restoreOriginalElements();
    }

    // todo: change method back to private when done with debugging and testing

    public void drawResult (String path) {
        SVGDrawer dr = new SVGDrawer(this.getGraph());
        dr.draw(path, drawInfo);
    }

    ////////////////////////
    // additional methods //
    ////////////////////////
    // constructor //

    private void initialise() {
        plugs = new LinkedHashMap<>();
        vertexGroups = new LinkedHashMap<>();
        hyperEdges = new LinkedHashMap<>();
        hyperEdgeParts = new LinkedHashMap<>();
        dummyNodesLongEdges = new LinkedHashMap<>();
        replacedPorts = new LinkedHashMap<>();
        multipleEdgePort2replacePorts = new LinkedHashMap<>();
        keptPortPairings = new LinkedHashMap<>();
        loopEdges = new LinkedHashMap<>();
        loopEdge2Ports = new LinkedHashMap<>();
        dummyEdge2RealEdge = new LinkedHashMap<>();

        edgeToStart = new LinkedHashMap<>();
        edgeToEnd = new LinkedHashMap<>();
        nodeToOutgoingEdges = new LinkedHashMap<>();
        nodeToIncomingEdges = new LinkedHashMap<>();
    }

    // construct //

    private void handleEdgeBundle () {
        for (EdgeBundle edgeBundle : getGraph().getEdgeBundles()) {
            handleEdgeBundle(edgeBundle);
        }
    }

    private void handleEdgeBundle(EdgeBundle edgeBundle) {
        //first find all ports of the bundle
        Map<Vertex, List<Port>> vertices2bundlePorts = new LinkedHashMap<>();
        //all ports in the bundle should end up next to the other -> also include recursively contained ones
        for (Edge edge : edgeBundle.getAllRecursivelyContainedEdges()) {
            for (Port port : edge.getPorts()) {
                Vertex vertex = port.getVertex();
                vertices2bundlePorts.putIfAbsent(vertex, new ArrayList<>());
                vertices2bundlePorts.get(vertex).add(port);
            }
        }
        //create a port group for the ports of the bundle at each vertex
        for (Vertex vertex : vertices2bundlePorts.keySet()) {
            List<Port> ports = vertices2bundlePorts.get(vertex);
            Map<PortGroup, List<PortComposition>> group2bundlePorts = new LinkedHashMap<>();
            PortGroup nullGroup = new PortGroup(); //dummy object for ports without port group
            //for this first find the containing port groups
            for (Port port : ports) {
                PortGroup portGroup = port.getPortGroup() == null ? nullGroup : port.getPortGroup();
                group2bundlePorts.put(portGroup, new ArrayList<>());
                group2bundlePorts.get(portGroup).add(port);
            }
            //and now create these port groups
            for (PortGroup portGroup : group2bundlePorts.keySet()) {
                PortGroup portGroupForEdgeBundle = new PortGroup(null, false);
                if (portGroup == nullGroup) {
                    //if not port group add it directly to the vertex
                    vertex.addPortComposition(new PortGroup(Arrays.asList(new PortGroup(Arrays.asList(new PortGroup(Arrays.asList(portGroupForEdgeBundle)))))));
                }
                else {
                    portGroup.addPortComposition(new PortGroup(Arrays.asList(new PortGroup(Arrays.asList(new PortGroup(Arrays.asList(portGroupForEdgeBundle)))))));
                }
                PortUtils.movePortCompositionsToPortGroup(group2bundlePorts.get(portGroup), portGroupForEdgeBundle);
            }
        }
        //do this recursively for contained edge bundles
        for (EdgeBundle containedEdgeBundle : edgeBundle.getContainedEdgeBundles()) {
            handleEdgeBundle(containedEdgeBundle);
        }
    }

    private void handlePortWithoutNode () {
        for (Edge edge : getGraph().getEdges()) {
            for (Port port : edge.getPorts()) {
                if (port.getVertex() == null) {
                    Vertex node = new Vertex();
                    getGraph().addVertex(node);
                    createMainLabel("addNodeFor" + port.getLabelManager().getMainLabel().toString() , node);
                    node.addPortComposition(port);
                }
            }
        }
    }
    private void handleHyperEdges() {
        int index1 = 0;
        int index2 = 0;

        for (Edge edge : new ArrayList<>(getGraph().getEdges())) {
            if (edge.getPorts().size() > 2) {
                Vertex representative = new Vertex();
                createMainLabel(("EdgeRep_for_" + edge.getLabelManager().getMainLabel().toString() + "_#" + index1++), representative);
                index2 = 0;
                for (Port port : edge.getPorts()) {
                    Port p = new Port();
                    createMainLabel(("HE_PortRep_for_" + port.getLabelManager().getMainLabel().toString() + "_#" + index1 + "-" + index2), p);
                    representative.addPortComposition(p);
                    List<Port> ps = new LinkedList<>();
                    ps.add(p);
                    ps.add(port);
                    Edge e = new Edge(ps);
                    createMainLabel(("HE_AddEdge_#" + index1 + "-" + index2++), e);
                    getGraph().addEdge(e);
                    hyperEdgeParts.put(e, representative);
                }
                getGraph().addVertex(representative);
                hyperEdges.put(representative, edge);
            }
        }
        for (Edge edge : hyperEdges.values()) {
            for (Port port : new ArrayList<>(edge.getPorts())) {
                port.removeEdge(edge);
            }
            getGraph().removeEdge(edge);
        }
        for (Edge edge : new LinkedList<>(getGraph().getEdges())) {
            if (edge.getPorts().size() < 2) {
                System.out.println("removed edge " + edge.toString() + " because it was not connected to at least two vertices");
                getGraph().removeEdge(edge);
            }

        }
    }

    private void handleVertexGroup () {
        int index1 = 0;
        int index2 = 0;
        deviceVertices = new LinkedHashSet<>();
        Set<VertexGroup> connectors = new LinkedHashSet<>();
        for (VertexGroup vertexGroup : getGraph().getVertexGroups()) {
            if (ImplicitCharacteristics.isConnector(vertexGroup, graph)) {
                connectors.add(vertexGroup);
            }
            for (Vertex containedVertex : vertexGroup.getContainedVertices()) {
                if (ImplicitCharacteristics.isDeviceVertex(containedVertex, graph)) {
                    deviceVertices.add(containedVertex);
                }
            }
        }

        for (VertexGroup group : new ArrayList<>(getGraph().getVertexGroups())) {
            boolean stickTogether = false;
            boolean hasPortPairings = false;
            Map<Port, Set<Port>> allPairings = new LinkedHashMap<>();
            if (group.getContainedVertices().size() == (group.getTouchingPairs().size() + 1)) {
                stickTogether = true;

                // fill allPairings
                fillAllPairings(allPairings, group);

//                // check for hasPortPairings
//                // this is the case if in one allPairingsPortSet exist two ports with outgoing edges to notGroupNodes
//                hasPortPairings = hasOutgoingPairings(allPairings, group);
                //EDIT JZ 2020/09/24: we don't want to exclude ports without edges any more -> simpler check
                hasPortPairings = !allPairings.isEmpty();
            }

            Map<Edge, Port> outgoingEdges = new LinkedHashMap<>();
            List<Vertex> groupVertices = group.getAllRecursivelyContainedVertices();
            fillOutgoingEdges(outgoingEdges, groupVertices);
            Vertex representative = new Vertex();

            // create main Label
            String groupLabelText = ""; //"no_GroupMainLabel";
//            if (group.getLabelManager().getMainLabel() != null) {
//                groupLabelText = group.getLabelManager().getMainLabel().toString();
//            }
//            String idV = ("GroupRep_for_" + groupLabelText + "_#" + index1++);
            String idV = ("R#" + index1++);
//            if (stickTogether) idV = ("PlugRep_for_" + groupLabelText + "_#" + (index1-1));
            createMainLabel(idV, representative);
            index2 = 0;

            getGraph().addVertex(representative);
            Map<Port, Port> originalPort2representative = new LinkedHashMap<>();

            for (Vertex containedVertex : group.getContainedVertices()) {
                for (Port port : containedVertex.getPorts()) {
                    if (!(deviceVertices.contains(containedVertex) && port.getEdges().isEmpty())) {
                        // create new port at unification vertex and remove old one on original vertex,
                        // hang the edges from the old to the new port
                        Port replacePort = new Port();
                        createMainLabel(
                                ("VG_PortRep_for_" + port.getLabelManager().getMainLabel().toString() + "_#" + index1 +
                                        "-" + index2), replacePort);

                        for (Edge edge : new ArrayList<>(port.getEdges())) {
                            edge.removePort(port);
                            edge.addPort(replacePort);
                        }


                        if (stickTogether) {
                            replacedPorts.put(replacePort, port);
                            originalPort2representative.put(port, replacePort);
                        } else {
                            representative.addPortComposition(replacePort);
                        }
                    }
                }
            }

            // create portGroups if stickTogether
            if (stickTogether) {
                for (Vertex groupNode : group.getContainedVertices()) {
                    representative.addPortComposition(keepPortGroupsRecursive(new PortGroup(),
                            groupNode.getPortCompositions(), originalPort2representative));
                }
                if (connectors.contains(group)) {
                    keepPortPairings(originalPort2representative, allPairings);
                    plugs.put(representative, group);
                } else {
                    vertexGroups.put(representative, group);
                }
            } else {
                vertexGroups.put(representative, group);
            }

            //remove group and its vertices
            getGraph().removeVertexGroup(group);
            for (Vertex groupVertex : groupVertices) {
                getGraph().removeVertex(groupVertex);
            }

            for (PortComposition portComposition : new LinkedHashSet<>(representative.getPortCompositions())) {
                if (findPort(portComposition) == null) representative.removePortComposition(portComposition);
            }
        }
    }

    private Port findPort(PortComposition portComposition) {
        Port port = null;
        if (portComposition instanceof Port) {
            port = (Port)portComposition;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                port = findPort(member);
                if (port != null) break;
            }
        }
        return port;
    }

    private void fillAllPairings (Map<Port, Set<Port>> allPairings, VertexGroup group) {
        for (PortPairing portPairing : group.getPortPairings()) {
            Port p0 = portPairing.getPort0();
            Port p1 = portPairing.getPort1();
            if (!allPairings.containsKey(p0)) {
                allPairings.put(p0, new LinkedHashSet<>());
            }
            if (!allPairings.containsKey(p1)) {
                allPairings.put(p1, new LinkedHashSet<>());
            }
            allPairings.get(p0).add(p0);
            allPairings.get(p0).add(p1);
            allPairings.get(p0).addAll(allPairings.get(p1));
            allPairings.get(p1).addAll(allPairings.get(p0));
            for (Port port : allPairings.get(p0)) {
                allPairings.get(port).addAll(allPairings.get(p0));
            }
        }
    }

    private boolean hasOutgoingPairings (Map<Port, Set<Port>> allPairings, VertexGroup group) {
        for (Port port : allPairings.keySet()) {
            int outEdges = 0;
            for (Port pairedPort : allPairings.get(port)) {
                boolean hasOutEdge = false;
                for (Edge edge : pairedPort.getEdges()) {
                    if (hasOutEdge) break;
                    for (Port edgePort : edge.getPorts()) {
                        if (!group.getContainedVertices().contains(edgePort.getVertex())) {
                            outEdges++;
                            hasOutEdge = true;
                            break;
                        }
                    }
                }
                if (outEdges >= 2) return true;
            }
        }
        return false;
    }

    private void fillOutgoingEdges (Map<Edge, Port> outgoingEdges, List<Vertex> groupVertices) {
        for (Vertex groupVertex : groupVertices) {
            Set<Port> groupVertexPorts = new LinkedHashSet<>(groupVertex.getPorts());
            for (Port p1 : groupVertexPorts) {
                for (Edge edge : new LinkedList<>(p1.getEdges())) {
                    Port p2 = edge.getPorts().get(0);
                    if (p1.equals(p2)) p2 = edge.getPorts().get(1);
                    if (!groupVertices.contains(p2.getVertex())) {
                        outgoingEdges.put(edge, p2);
                    } else {
                        //TODO: save edge instead of removing
                        getGraph().removeEdge(edge);
                    }
                }
            }
        }
    }

    private PortGroup keepPortGroupsRecursive (PortGroup superiorRepGroup, List<PortComposition> originalMembers, Map<Port, Port> portToRepresentative) {
        for (PortComposition originalMember : originalMembers) {
            if (originalMember instanceof PortGroup) {
                PortGroup newThisLevelGroup = keepPortGroupsRecursive(new PortGroup(((PortGroup) originalMember).isOrdered()),
                        ((PortGroup) originalMember).getPortCompositions(), portToRepresentative);
                if (!newThisLevelGroup.getPortCompositions().isEmpty()) superiorRepGroup.addPortComposition(newThisLevelGroup);
            } else if (portToRepresentative.containsKey(originalMember)) {
                superiorRepGroup.addPortComposition(portToRepresentative.get(originalMember));
            }
        }
        return superiorRepGroup;
    }

    private void keepPortPairings (Map<Port, Port> portRepMap, Map<Port, Set<Port>> allPairings) {
        LinkedList<Port> keySet = new LinkedList<>(allPairings.keySet());
        int i = keySet.size()-1;
        while (i > -1) {
            Port key = keySet.get(i--);
            Port p0 = portRepMap.get(key);
            for (Port port : allPairings.get(key)) {
                if (!port.equals(key)) {
                    keySet.remove(port);
                    allPairings.remove(port);
                    i--;
                    if (p0 == null) {
                        p0 = portRepMap.get(port);
                    } else if (portRepMap.containsKey(port)) {
                        keptPortPairings.put(p0, portRepMap.get(port));
                        keptPortPairings.put(portRepMap.get(port), p0);
                    }
                }
            }
        }
    }

    private void handlePort () {
        int index1 = 0;

        Map<PortGroup, Port> replaceGroups = new LinkedHashMap<>();
        for (Vertex node : getGraph().getVertices()) {
            LinkedHashMap<Port, Set<Edge>> toRemove = new LinkedHashMap<>();
            LinkedHashMap<Port, Edge> toAdd = new LinkedHashMap<>();
            for (Port port : node.getPorts()) {
                if (port.getEdges().size() > 1) {
                    toRemove.put(port,new LinkedHashSet<>());
                    index1 = 0;
                    // create a PortGroup with one Port for each connected Edge
                    PortGroup repGroup = new PortGroup();
                    for (Edge edge: port.getEdges()) {
                        Port addPort = new Port();
                        repGroup.addPortComposition(addPort);
                        toRemove.get(port).add(edge);
                        toAdd.put(addPort, edge);
                        createMainLabel(("AddPort_for_" + port.getLabelManager().getMainLabel().toString() + "_#" + index1++), addPort);
                        replacedPorts.put(addPort, port);
                    }
                    replaceGroups.put(repGroup, port);
                    multipleEdgePort2replacePorts.put(port, PortUtils.getPortsRecursively(repGroup));
                }
            }
            // remove Port from Edges
            for (Map.Entry<Port, Set<Edge>> entry : toRemove.entrySet()) {
                for (Edge edge : entry.getValue()) {
                    edge.removePort(entry.getKey());
                }
            }
            // add new Ports to Edges
            for (Map.Entry<Port, Edge> entry : toAdd.entrySet()) {
                entry.getValue().addPort(entry.getKey());
            }
        }
        // replace Ports with PortGroup in each node
        for (Map.Entry<PortGroup, Port> entry : replaceGroups.entrySet()) {
            PortGroup portGroup = entry.getKey();
            Port port = entry.getValue();
            Vertex node = port.getVertex();
            if (port.getPortGroup() == null) {
                node.addPortComposition(portGroup);
                node.removePortComposition(port);
            } else {
                port.getPortGroup().addPortComposition(portGroup);
                port.getPortGroup().removePortComposition(port);
                node.removePortComposition(port);
            }
            // remove all portPairings to this Port
            if (node.getVertexGroup() != null) {
                Set<PortPairing> toRemove = new LinkedHashSet<>();
                for (PortPairing portPairing : new ArrayList<>(node.getVertexGroup().getPortPairings())) {
                    if (portPairing.getPorts().contains(port)) {
                        toRemove.add(portPairing);
                        // we must preserve port pairings -> pick an arbitrary port of the new ports to participate in
                        // the port pairing
                        Port arbitraryPortOfTheNewGroup = (Port) portGroup.getPortCompositions().iterator().next();
                        LinkedHashSet<Port> portsOfPortPairing = new LinkedHashSet<>(portPairing.getPorts());
                        portsOfPortPairing.remove(port);
                        Port otherPort = portsOfPortPairing.iterator().next();
                        node.getVertexGroup().addPortPairing(new PortPairing(otherPort, arbitraryPortOfTheNewGroup));
                    }
                }
                for (PortPairing portPairing : toRemove) {
                    node.getVertexGroup().removePortPairing(portPairing);
                }
            }
        }
    }

    private void handleLoopEdges() {
        for (Edge edge : new ArrayList<>(getGraph().getEdges())) {
            //we have split all hyperedges with >= 3 ports, so it suffices to consider the first two ports
            Port port0 = edge.getPorts().get(0);
            Port port1 = edge.getPorts().get(1);
            if (port0.getVertex().equals(port1.getVertex())) {
                this.loopEdges.computeIfAbsent(port0.getVertex(), k -> new LinkedHashSet<>()).add(edge);
                this.loopEdge2Ports.put(edge, new LinkedHashSet<>(Arrays.asList(port0, port1)));

                //remove loop edge and ports
                getGraph().removeEdge(edge);
                port0.getVertex().removePortComposition(port0);
                port1.getVertex().removePortComposition(port1);
            }
        }
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
    public int countCrossings (CMResult cmResult) {
        // create Port lists
        List<List<Port>> topPorts = new ArrayList<>();
        List<List<Port>> bottomPorts = new ArrayList<>();
        Map<Port, Integer> positions = new LinkedHashMap<>();
        for (int layer = 0; layer < cmResult.getNodeOrder().size(); layer++) {
            topPorts.add(new ArrayList<>());
            bottomPorts.add(new ArrayList<>());
            int position = 0;
            for (Vertex node : cmResult.getNodeOrder().get(layer)) {
                for (Port topPort : cmResult.getTopPortOrder().get(node)) {
                    topPorts.get(layer).add(topPort);
                }
                for (Port bottomPort : cmResult.getBottomPortOrder().get(node)) {
                    bottomPorts.get(layer).add(bottomPort);
                    positions.put(bottomPort, position++);
                }
            }
        }
        // count crossings
        int crossings = 0;
        for (int layer = 0; layer < (cmResult.getNodeOrder().size() - 1); layer++) {
            for (int topPortPosition = 0; topPortPosition < topPorts.get(layer).size(); topPortPosition++) {
                Port topPort = topPorts.get(layer).get(topPortPosition);
                for (Edge edge : topPort.getEdges()) {
                    Port bottomPort = edge.getPorts().get(0);
                    if (topPort.equals(bottomPort)) bottomPort = edge.getPorts().get(1);
                    int bottomPortPosition = 0;
                    bottomPortPosition = positions.get(bottomPort);
                    for (int topPosition = (topPortPosition + 1); topPosition < topPorts.get(layer).size(); topPosition++) {
                        Port crossingTopPort = topPorts.get(layer).get(topPosition);
                        for (Edge crossingEdge : crossingTopPort.getEdges()) {
                            Port crossingBottomPort = crossingEdge.getPorts().get(0);
                            if (crossingTopPort.equals(crossingBottomPort)) crossingBottomPort = crossingEdge.getPorts().get(1);
                            if (positions.get(crossingBottomPort) < bottomPortPosition) crossings++;
                        }
                    }
                }
            }
        }
        return crossings;
    }

    private void createMainLabel (String id, LabeledObject lo) {
        Label newLabel = new TextLabel(id);
        lo.getLabelManager().addLabel(newLabel);
        lo.getLabelManager().setMainLabel(newLabel);
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

    public void changeRanks (Map<Vertex, Integer> newRanks) {
        for (Vertex node: newRanks.keySet()) {
            int newRank = newRanks.get(node);
            if (nodeToRank.containsKey(node)) {
                int oldRank = getRank(node);
                rankToNodes.get(oldRank).remove(node);
                if (rankToNodes.get(oldRank).isEmpty()) rankToNodes.remove(oldRank);
                if (!rankToNodes.containsKey(newRank)) rankToNodes.put(newRank, new LinkedHashSet<>());
                rankToNodes.get(newRank).add(node);
                nodeToRank.replace(node, newRanks.get(node));
            } else {
                nodeToRank.put(node, newRank);
                if (!rankToNodes.containsKey(newRank)) rankToNodes.put(newRank, new LinkedHashSet<>());
                rankToNodes.get(newRank).add(node);
            }
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
        return isDummyNodeOfLongEdge(node) || isTurningPointDummy(node) || getHyperEdges().containsKey(node);
    }

    public boolean isDummyNodeOfLongEdge(Vertex node) {
        return dummyNodesLongEdges.containsKey(node);
    }

    public boolean isTurningPointDummy (Vertex node) {
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

    public Map<Edge, Edge> getDummyEdge2RealEdge () {
        return dummyEdge2RealEdge;
    }

    public Set<Edge> getLoopEdges (Vertex node) {
        if (loopEdges.containsKey(node)) {
            return Collections.unmodifiableSet(loopEdges.get(node));
        } else {
            return Collections.unmodifiableSet(new LinkedHashSet<>());
        }
    }

    public Set<Port> getPortsOfLoopEdge (Edge loopEdge) {
        if (loopEdge2Ports.containsKey(loopEdge)) {
            return Collections.unmodifiableSet(loopEdge2Ports.get(loopEdge));
        } else {
            return Collections.unmodifiableSet(new LinkedHashSet<>());
        }
    }

    public boolean hasAssignedLayers () {
        return hasAssignedLayers;
    }

    public String[] getNodeName (Vertex node) {
        String[] nodeName;
        // todo: implement other cases
        if (isDummy(node)) {
            nodeName = new String[1];
            nodeName[0] = "";
        } else if (isPlug(node)) {
            if (plugs.get(node).getContainedVertices().size() == 2) {
                nodeName = new String[node.getLabelManager().getLabels().size()];
                for (int i = 0; i < nodeName.length; i++) {
                    nodeName[i] = node.getLabelManager().getLabels().get(i).toString();
                }
            } else {
                nodeName = new String[node.getLabelManager().getLabels().size()];
                for (int i = 0; i < nodeName.length; i++) {
                    nodeName[i] = node.getLabelManager().getLabels().get(i).toString();
                }
            }
        } else if (vertexGroups.keySet().contains(node)) {
            nodeName = new String[node.getLabelManager().getLabels().size()];
            for (int i = 0; i < nodeName.length; i++) {
                nodeName[i] = node.getLabelManager().getLabels().get(i).toString();
            }
        } else {
            nodeName = new String[node.getLabelManager().getLabels().size()];
            for (int i = 0; i < nodeName.length; i++) {
                nodeName[i] = node.getLabelManager().getLabels().get(i).toString();
            }
        }
        return nodeName;
    }

    //TODO: re-visit later
    public double getTextWidthForNode(Vertex node) {
        double width = 0;
        for (String label : getNodeName(node)) {
            width = Math.max(width, DrawingInformation.g2d.getFontMetrics().getStringBounds(label, DrawingInformation.g2d).getWidth());
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

    public CMResult getOrders() {
        return orders;
    }

    public Map<Vertex, VertexGroup> getPlugs() {
        return plugs;
    }

    public Map<Vertex, VertexGroup> getVertexGroups() {
        return vertexGroups;
    }

    public Map<Vertex, Edge> getHyperEdges() {
        return hyperEdges;
    }

    public Map<Edge, Vertex> getHyperEdgeParts() {
        return hyperEdgeParts;
    }

    public Map<Vertex, Edge> getDummyNodesLongEdges() {
        return dummyNodesLongEdges;
    }

    public Map<Vertex, Vertex> getDummyTurningNodes() {
        return dummyTurningNodes;
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

    /////////////////
    // for testing //
    /////////////////

    // todo: delete when done with debugging and testing
    public int getNumberOfDummys () {
        return dummyNodesLongEdges.size();
    }

    // todo: delete when done with debugging and testing
    public int getNumberOfCrossings () {
        return countCrossings(orders);
    }

    //////////////
    // override //
    //////////////

    @Override
    // todo: delete when done with debugging and testing or change to useful method
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph:\n");
        sb.append("Vertices and Ports:\n");
        for (Vertex v : getGraph().getVertices()) {
            sb.append(v.getLabelManager().getMainLabel().toString()).append(" - Ports:\n");
            for (Port p : v.getPorts()) {
                sb.append("\t").append(p.getLabelManager().getMainLabel().toString()).append("\n");
            }
        }
        sb.append("Edges:\n");
        for (Edge e : getGraph().getEdges()) {
            sb.append(e.getLabelManager().getMainLabel().toString()).append("\n\t");
            sb.append(edgeToStart.get(e).getLabelManager().getMainLabel().toString()).append(" --> ");
            sb.append(edgeToEnd.get(e).getLabelManager().getMainLabel().toString()).append("\n");
        }
        if (nodeToRank != null && !nodeToRank.keySet().isEmpty()) {
            sb.append("Ranks:\n");
            for (Vertex v : nodeToRank.keySet()) {
                sb.append(v.getLabelManager().getMainLabel().toString()).append(" - ").append(nodeToRank.get(v)).append("\n");
            }
        }
        return sb.toString();
    }

}
