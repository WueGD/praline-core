package de.uniwue.informatik.praline.layouting.layered.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
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
import de.uniwue.informatik.praline.layouting.layered.algorithm.restore.NoEdgePort;
import de.uniwue.informatik.praline.layouting.layered.algorithm.restore.OneNodeEdge;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class SugiyamaLayouter implements PralineLayouter {

    public static final DirectionMethod DEFAULT_DIRECTION_METHOD = DirectionMethod.FORCE;
    public static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.PORTS;
    public static final int DEFAULT_NUMBER_OF_CM_ITERATIONS = 5; //iterations for crossing minimization

    private Graph graph;
    private DrawingInformation drawInfo;
    private Map<Vertex, VertexGroup> plugs;
    private Map<Vertex, VertexGroup> vertexGroups;
    private Map<Port, Edge> replacedEdges;
    private Map<Vertex, Edge> hyperEdges;
    private Map<Edge, Vertex> hyperEdgeParts;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<PortGroup, Port> replacedPorts;
    protected Map<Port, Port> keptPortPairings;
    private Map<Vertex, Set<NoEdgePort>> noEdgePorts;
    private Map<Vertex, Set<OneNodeEdge>> oneNodeEdges;
    private Map<Edge, Edge> dummyEdge2RealEdge;

    //additional structures

    private int numberOfDigitsAdded;
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
    private boolean hasAssignedLayeres;

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
        prepareGraph();
        this.drawInfo = drawInfo;
    }

    @Override
    public void computeLayout() {
        computeLayout(DEFAULT_DIRECTION_METHOD, DEFAULT_CROSSING_MINIMIZATION_METHOD, DEFAULT_NUMBER_OF_CM_ITERATIONS);
    }

    public void computeLayout (DirectionMethod method, CrossingMinimizationMethod cmMethod, int numberOfIterationsCM) {
        //chose methods for directionassignment
        //chose other steps to be done or not
        //maybe preferred edge distance
        construct();
        assignDirections(method);
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
        // handle Port if it has no Vertex
        handlePortWithoutNode();
        // handle Edge if connected to more than two Ports
        handleEdge();
        // handle Port if it has more than one Edge
        handlePort();
        // handle VertexGroup
        handleVertexGroup();
        // handle Port if it has no Edge
        handleNoEdgePort();
        // handle Edge if both Ports have same Vertex
        handleOneNodeEdge();
        // if the Graph is not connected use just biggest connected component
        breakDownToBiggestConnectedComponent();
        //TODO: draw all components
    }
    // todo: change method back to private when done with debugging and testing

    public void assignDirections (DirectionMethod method) {
        DirectionAssignment da = new DirectionAssignment();
        switch (method) {
            case FORCE:
                da.forceDirected(this);
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
        hasAssignedLayeres = true;
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
        np.placeNodes();
    }
    // todo: change method back to private when done with debugging and testing

    public void edgeRouting () {
        EdgeRouting er = new EdgeRouting(this, orders, drawInfo);
        er.routeEdges();
    }
    // todo: change method back to private when done with debugging and testing

    public void prepareDrawing () {
        DrawingPreparation dp = new DrawingPreparation(this);
        dp.prepareDrawing(drawInfo, orders);
        restoreOriginalElements();
    }
    private void restoreOriginalElements () {
        //replace dummy edges
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            for (Edge edge : new ArrayList<>(this.getGraph().getEdges())) {
                if (dummyEdge2RealEdge.containsKey(edge)) {
                    Edge originalEdge = dummyEdge2RealEdge.get(edge);
                    replaceByOriginalEdge(edge, originalEdge);
                    hasChanged = true;
                }
            }
        }

        //until now all edges are deg 2 (so there are no hyperedges)
        //because they are composite of many different edge parts each contributing a path, they have multiple paths
        // now. We unify all the paths to one long path
        for (Edge edge : this.getGraph().getEdges()) {
            unifyPathsOfDeg2Edge(edge);
        }

        //unify single parts of hyperedges to one edge each
        while (hasChanged) {
            hasChanged = false;
            for (Edge edge : new ArrayList<>(this.getGraph().getEdges())) {
                //TODO: re-store hyperedges
//                if (hyperEdgeParts.containsKey(edge)) {
//                    Edge originalEdge = hyperEdges.get(hyperEdgeParts.get(edge));
//                    replaceByOriginalEdge(edge, originalEdge);
//                    hasChanged = true;
//                }
            }
        }

        //replace dummy vertices
        for (Vertex vertex : new ArrayList<>(this.getGraph().getVertices())) {
            if (hyperEdges.containsKey(vertex)) {
                //TODO: replace nodes by horizontal edge segments
//                getGraph().removeVertex(vertex);
            }
            if (vertexGroups.containsKey(vertex)) {
                //TODO: replace by original vertices
            }
            if (plugs.containsKey(vertex)) {
                //TODO: replace by original vertices
            }
            if (dummyTurningNodes.containsKey(vertex)) {
                getGraph().removeVertex(vertex);
            }
            if (dummyNodesLongEdges.containsKey(vertex)) {
                getGraph().removeVertex(vertex);
            }
        }

        //TODO: replace dummy ports by original ports (ports that replaced by multiple ports because they had
        // multiple incident edges)
        //TODO: check that #ports, #vertices, #edges is in the end the same as at the beginning
        //the following is only for intermediate use. change to something better when re-constructing the vertex groups
        for (Port port : keptPortPairings.keySet()) {
            Vertex vertex = port.getVertex();
            VertexGroup vertexGroup = vertex.getVertexGroup();
            if (vertexGroup == null) {
                vertexGroup = new VertexGroup(Collections.singleton(vertex));
            }
            if (!PortUtils.isPaired(port)) {
                vertexGroup.addPortPairing(new PortPairing(port, getPairedPort(port)));
            }
        }
    }

    private void unifyPathsOfDeg2Edge(Edge edge) {
        //first find all segments of the edge paths
        Set<Line2D.Double> allSegments = new LinkedHashSet<>();
        for (Path path : edge.getPaths()) {
            allSegments.addAll(((PolygonalPath) path).getSegments());
        }
        //now re-construct whole paths beginning from the start port
        Port startPort = edge.getPorts().get(0);
        Port endPort = edge.getPorts().get(1);
        Point2D.Double nextPoint = findSegmentPointAt((Rectangle) startPort.getShape(), allSegments);
        Point2D.Double endPoint = findSegmentPointAt((Rectangle) endPort.getShape(), allSegments);
        PolygonalPath newPath = new PolygonalPath();
        newPath.setStartPoint(nextPoint);
        newPath.setEndPoint(endPoint);
        //find all inner bend points
        Point2D.Double prevPoint = null;
        Point2D.Double curPoint = null;
        while (curPoint == null || !nextPoint.equals(endPoint)) {
            prevPoint = curPoint;
            curPoint = nextPoint;
            Line2D.Double curSegment = findSegmentAt(curPoint, allSegments);
            allSegments.remove(curSegment);
            nextPoint = getOtherEndPoint(curSegment, curPoint);

            if (prevPoint != null && !areOnALine(prevPoint, curPoint, nextPoint)) {
                newPath.getBendPoints().add(curPoint);
            }
        }
        //add new path and remove all old ones
        edge.removeAllPaths();
        edge.addPath(newPath);
    }

    private boolean areOnALine(Point2D.Double prevPoint, Point2D.Double curPoint, Point2D.Double nextPoint) {
        return new Line2D.Double(prevPoint, nextPoint).ptSegDist(curPoint) == 0;
    }

    private Point2D.Double getOtherEndPoint(Line2D.Double segment, Point2D.Double point) {
        if (segment.getP1().equals(point)) {
            return (Point2D.Double) segment.getP2();
        }
        return (Point2D.Double) segment.getP1();
    }

    private Line2D.Double findSegmentAt(Point2D.Double point, Set<Line2D.Double> allSegments) {
        for (Line2D.Double segment : allSegments) {
            if (point.equals(segment.getP1())) {
                return segment;
            }
            if (point.equals(segment.getP2())) {
                return segment;
            }
        }
        return null;
    }

    private Point2D.Double findSegmentPointAt(Rectangle portRectangle, Set<Line2D.Double> allSegments) {
        for (Line2D.Double segment : allSegments) {
            if (portRectangle.intersectsLine(new Line2D.Double(segment.getP1(), segment.getP1()))) {
                return (Point2D.Double) segment.getP1();
            }
            if (portRectangle.intersectsLine(new Line2D.Double(segment.getP2(), segment.getP2()))) {
                return (Point2D.Double) segment.getP2();
            }
        }
        return null;
    }

    private void replaceByOriginalEdge(Edge dummyEdge, Edge originalEdge) {
        if (!getGraph().getEdges().contains(originalEdge)) {
            getGraph().addEdge(originalEdge);
            if (!originalEdge.getPaths().isEmpty()) {
                //TODO this was introduced to avoid doubling of edge paths. but ideally that should not be necessary
                // (and this could even lead to new problems) -- so better fix edge-path insertion and edge-removal
                // in DrawingPreparation
                originalEdge.removeAllPaths();
            }
        }
        //transfer the paths form the dummy to the original edge
        originalEdge.addPaths(dummyEdge.getPaths());
        //add ports of dummy edge to original edge
        for (Port port : dummyEdge.getPorts()) {
            Vertex vertex = port.getVertex();
            if (!dummyTurningNodes.containsKey(vertex)
                    && !dummyNodesLongEdges.containsKey(vertex)
                    && !originalEdge.getPorts().contains(port)) {
                originalEdge.addPort(port);
            }
        }
        getGraph().removeEdge(dummyEdge);
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
        replacedEdges = new LinkedHashMap<>();
        hyperEdges = new LinkedHashMap<>();
        hyperEdgeParts = new LinkedHashMap<>();
        dummyNodesLongEdges = new LinkedHashMap<>();
        replacedPorts = new LinkedHashMap<>();
        keptPortPairings = new LinkedHashMap<>();
        noEdgePorts = new LinkedHashMap<>();
        oneNodeEdges = new LinkedHashMap<>();
        dummyEdge2RealEdge = new LinkedHashMap<>();

        numberOfDigitsAdded = 0;
        edgeToStart = new LinkedHashMap<>();
        edgeToEnd = new LinkedHashMap<>();
        nodeToOutgoingEdges = new LinkedHashMap<>();
        nodeToIncomingEdges = new LinkedHashMap<>();
    }
    private void prepareGraph () {
        int v = 0, e = 0, g = 0, b = 0, j = 1;
        p = 0; i = 0;
        for (Vertex node : getGraph().getVertices()) {
            if (node.getLabelManager().getLabels().isEmpty()) {
                createMainLabel(("v" + v++), node);
            } else if (node.getLabelManager().getMainLabel() == null) {
                node.getLabelManager().setMainLabel(node.getLabelManager().getLabels().get(0));
            }
            for (PortComposition portComposition : node.getPortCompositions()) {
                preparePort(portComposition);
            }
        }
        Set<Edge> toRemoveDueToNoVertex = new LinkedHashSet<>();
        for (Edge edge : getGraph().getEdges()) {
            for (Port port : edge.getPorts()) {
                if (port.getVertex() == null) {
                    toRemoveDueToNoVertex.add(edge);
                }
            }
            if (edge.getLabelManager().getLabels().isEmpty()) {
                createMainLabel(("e" + e++), edge);
            } else if (edge.getLabelManager().getMainLabel() == null) {
                edge.getLabelManager().setMainLabel(edge.getLabelManager().getLabels().get(0));
            }
        }
        for (VertexGroup vertexGroup : getGraph().getVertexGroups()) {
            if (vertexGroup.getLabelManager().getLabels().isEmpty()) {
                createMainLabel(("g" + g++), vertexGroup);
            } else if (vertexGroup.getLabelManager().getMainLabel() == null) {
                vertexGroup.getLabelManager().setMainLabel(vertexGroup.getLabelManager().getLabels().get(0));
            }
        }
        for (EdgeBundle edgeBundle : getGraph().getEdgeBundles()) {
            if (edgeBundle.getLabelManager().getLabels().isEmpty()) {
                createMainLabel(("b" + b++), edgeBundle);
            } else if (edgeBundle.getLabelManager().getMainLabel() == null) {
                edgeBundle.getLabelManager().setMainLabel(edgeBundle.getLabelManager().getLabels().get(0));
            }
        }
        for (Edge edge : toRemoveDueToNoVertex) {
            getGraph().removeEdge(edge);
            List<Port> ports = new LinkedList<>(edge.getPorts());
            for (Port port : ports) {
                port.removeEdge(edge);
            }
        }

        i += getGraph().getVertices().size();
        i += getGraph().getEdges().size();
        i += getGraph().getVertexGroups().size();
        i += getGraph().getEdgeBundles().size();
        for (Vertex node : getGraph().getVertices()) {
            for (PortComposition portComposition : node.getPortCompositions()) {
                calculateNumberOfPorts(portComposition);
            }
        }
        while (i != 0) {
            i /= 10;
            j++;
        }
        this.numberOfDigitsAdded = (j + 1);
        for (Vertex node : getGraph().getVertices()) {
            Label mainLabel = node.getLabelManager().getMainLabel();
            node.getLabelManager().removeLabel(mainLabel);
            createMainLabel("v" + String.format("%0" + j + "d" , i++) + mainLabel, node);
            for (PortComposition portComposition : node.getPortCompositions()) {
                numberPortComposition(portComposition, j);
            }
        }
        for (Edge edge : getGraph().getEdges()) {
            Label mainLabel = edge.getLabelManager().getMainLabel();
            edge.getLabelManager().removeLabel(mainLabel);
            createMainLabel("e" + String.format("%0" + j + "d" , i++) + mainLabel, edge);
        }
        for (VertexGroup vertexGroup : getGraph().getVertexGroups()) {
            Label mainLabel = vertexGroup.getLabelManager().getMainLabel();
            vertexGroup.getLabelManager().removeLabel(mainLabel);
            createMainLabel("g" + String.format("%0" + j + "d" , i++) + mainLabel, vertexGroup);
        }
        for (EdgeBundle edgeBundle : getGraph().getEdgeBundles()) {
            Label mainLabel = edgeBundle.getLabelManager().getMainLabel();
            edgeBundle.getLabelManager().removeLabel(mainLabel);
            createMainLabel("e" + String.format("%0" + j + "d" , i++) + mainLabel, edgeBundle);
        }
    }

    private static int i;

    private static int p;
    private void preparePort (PortComposition portComposition) {
        if (portComposition instanceof Port) {
            if (((Port)portComposition).getLabelManager().getLabels().isEmpty()) {
                createMainLabel(("p" + p++), ((Port)portComposition));
            } else {
                ((Port)portComposition).getLabelManager().setMainLabel(((Port)portComposition).getLabelManager().getLabels().get(0));
            }
        } else if (portComposition instanceof PortGroup){
            for (PortComposition pc : ((PortGroup) portComposition).getPortCompositions()) {
                preparePort(pc);
            }
        }
    }
    private void numberPortComposition (PortComposition portComposition, int j) {
        if (portComposition instanceof Port) {
            Label mainLabel = ((Port)portComposition).getLabelManager().getMainLabel();
            ((Port)portComposition).getLabelManager().removeLabel(mainLabel);
            createMainLabel("p" + String.format("%0" + j + "d" , i++) + mainLabel, (Port)portComposition);
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                numberPortComposition(member, j);
            }
        }
    }

    private void calculateNumberOfPorts (PortComposition portComposition) {
        if (portComposition instanceof Port) {
            i++;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup) portComposition).getPortCompositions()) {
                calculateNumberOfPorts(member);
            }
        }
    }

    // construct //

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
    private void handleEdge () {
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
        for (VertexGroup group : new ArrayList<>(getGraph().getVertexGroups())) {
            boolean stickTogether = false;
            boolean hasPortPairings = false;
            Map<Vertex, Map<Port, Port>> referenceVertices = new LinkedHashMap<>();
            Map<Port, Set<Port>> allPairings = new LinkedHashMap<>();
            if (group.getContainedVertices().size() == (group.getTouchingPairs().size() + 1)) {
                stickTogether = true;
                for (Vertex node : group.getContainedVertices()) {
                    referenceVertices.put(node, new LinkedHashMap<>());
                }

                // fill allPairings
                fillAllPairings(allPairings, group);

                // check for hasPortPairings
                // this is the case if in one allPairingsPortSet exist two ports with outgoing edges to notGroupNodes
                hasPortPairings = hasOutgoingPairings(allPairings, group);

            }

            Map<Edge, Port> outgoingEdges = new LinkedHashMap<>();
            List<Vertex> groupVertices = group.getAllRecursivelyContainedVertices();
            fillOutgoingEdges(outgoingEdges, groupVertices);
            Vertex representative = new Vertex();

            // create main Label
            String idV = ("GroupRep_for_" + group.getLabelManager().getMainLabel().toString() + "_#" + index1++);
            if (stickTogether) idV = ("PlugRep_for_" + group.getLabelManager().getMainLabel().toString() + "_#" + (index1-1));
            createMainLabel(idV, representative);
            index2 = 0;

            getGraph().addVertex(representative);
            for (Edge outEdge : outgoingEdges.keySet()) {
                Port p1 = outEdge.getPorts().get(0);
                Port p2 = outgoingEdges.get(outEdge);
                if (p2.equals(p1)) p1 = outEdge.getPorts().get(1);
                Port replacePort = new Port();
                createMainLabel(("VG_PortRep_for_" + p1.getLabelManager().getMainLabel().toString() + "_#" + index1 +
                        "-" + index2), replacePort);

                List<Port> rEdgePorts = new LinkedList<>();
                rEdgePorts.add(replacePort);
                rEdgePorts.add(p2);
                Edge replaceEdge = new Edge(rEdgePorts);
                createMainLabel(("VG_EdgeRep_for_" + outEdge.getLabelManager().getMainLabel().toString() + "_#" + index1 + "-" + index2++), replaceEdge);

                getGraph().addEdge(replaceEdge);
                replacedEdges.put(p2, outEdge);
                dummyEdge2RealEdge.put(replaceEdge, outEdge);
                getGraph().removeEdge(outEdge);
                p2.removeEdge(outEdge);

                if (stickTogether) {
                    referenceVertices.get(p1.getVertex()).put(p1, replacePort);
                } else {
                    representative.addPortComposition(replacePort);
                }
            }

            // create portGroups if stickTogether
            if (stickTogether) {
                for (Vertex groupNode : referenceVertices.keySet()) {
                    representative.addPortComposition(keepPortGroupsRecursive(new PortGroup(), groupNode.getPortCompositions(), referenceVertices.get(groupNode)));
                }
                if (hasPortPairings) {
                    keepPortPairings(referenceVertices, allPairings);
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
            Set<Port> groupVertexPorts = new LinkedHashSet<>();
            for (PortComposition portComposition : groupVertex.getPortCompositions()) {
                getPortsRecursive(groupVertexPorts, portComposition);
            }
            for (Port p1 : groupVertexPorts) {
                for (Edge edge : new LinkedList<>(p1.getEdges())) {
                    Port p2 = edge.getPorts().get(0);
                    if (p1.equals(p2)) p2 = edge.getPorts().get(1);
                    if (!groupVertices.contains(p2.getVertex())) {
                        outgoingEdges.put(edge, p2);
                    } else {
                        getGraph().removeEdge(edge);
                    }
                }
            }
        }
    }

    private Set<Port> getPortsRecursive (Set<Port> vertexPorts, PortComposition portComposition) {
        if (portComposition instanceof Port) {
            vertexPorts.add((Port) portComposition);
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup) portComposition).getPortCompositions()) {
                getPortsRecursive(vertexPorts, member);
            }
        }
        return vertexPorts;
    }

    private PortGroup keepPortGroupsRecursive (PortGroup superiorRepGroup, List<PortComposition> originalMembers, Map<Port, Port> portToRepresentative) {
        for (PortComposition originalMember : originalMembers) {
            if (originalMember instanceof PortGroup) {
                PortGroup newThisLevelGroup = keepPortGroupsRecursive(new PortGroup(), ((PortGroup) originalMember).getPortCompositions(), portToRepresentative);
                if (!newThisLevelGroup.getPortCompositions().isEmpty()) superiorRepGroup.addPortComposition(newThisLevelGroup);
            } else if (portToRepresentative.containsKey(originalMember)) {
                superiorRepGroup.addPortComposition(portToRepresentative.get(originalMember));
            }
        }
        return superiorRepGroup;
    }

    private void keepPortPairings (Map<Vertex, Map<Port, Port>> referenceVertices, Map<Port, Set<Port>> allPairings) {
        Map<Port, Port> portRepMap = new LinkedHashMap<>();
        for (Map<Port, Port> value : referenceVertices.values()) {
            portRepMap.putAll(value);
        }
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
                    }
                    replacedPorts.put(repGroup, port);
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
        for (Map.Entry<PortGroup, Port> entry : replacedPorts.entrySet()) {
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

    private void handleNoEdgePort () {
        for (Vertex node : getGraph().getVertices()) {
            for (PortComposition portComposition : new LinkedList<>(node.getPortCompositions())) {
                handleNoEdgePortRec(portComposition);
            }
        }
    }

    private void handleNoEdgePortRec (PortComposition portComposition) {
        if (portComposition instanceof Port) {
            Port port = ((Port)portComposition);
            if (port.getEdges().isEmpty()) {
                new NoEdgePort(noEdgePorts, port);
            }
        } else {
            for (PortComposition member : new ArrayList<>(((PortGroup)portComposition).getPortCompositions())) {
                handleNoEdgePortRec(member);
            }
        }
    }

    private void handleOneNodeEdge () {
        Set<Edge> toDelete = new LinkedHashSet<>();
        for (Edge edge : getGraph().getEdges()) {
            if (edge.getPorts().get(0).getVertex().equals(edge.getPorts().get(1).getVertex())) {
                toDelete.add(edge);
            }
        }
        for (Edge edge : toDelete) {
            new OneNodeEdge(oneNodeEdges, edge, getGraph());
        }
    }

    private void breakDownToBiggestConnectedComponent() {
        // todo: could be adapted to compute the algorithm for each connectedComponent
        Set<Vertex> biggestConnectedComponent = new LinkedHashSet<>();
        Set<Vertex> vertices = new LinkedHashSet<>(getGraph().getVertices());

        // delete all nodes without any connection
        for (Vertex node : getGraph().getVertices()) {
            if (node.getPortCompositions().isEmpty()) vertices.remove(node);
        }

        while (!vertices.isEmpty()) {
            // find next connected component
            Vertex node = null;
            for (Vertex n : vertices) {
                node = n;
                break;
            }
            Set<Vertex> connectedComponent = new LinkedHashSet<>();
            computeConnectedComponentRecursive(node, connectedComponent);
            for (Vertex n : connectedComponent) {
                vertices.remove(n);
            }
            if (biggestConnectedComponent.size() < connectedComponent.size()) {
                biggestConnectedComponent = connectedComponent;
            }
        }
        // remove all elements from Graph which are not connected to the biggestConnectedComponent
        vertices = new LinkedHashSet<>(getGraph().getVertices());
        Set<Edge> edges = new LinkedHashSet<>(getGraph().getEdges());
        for (Edge edge : edges) {
            for (Port port : edge.getPorts()) {
                if (!biggestConnectedComponent.contains(port.getVertex())) {
                    getGraph().removeEdge(edge);
                    break;
                }
            }
        }
        for (Vertex vertex : vertices) {
            if (!biggestConnectedComponent.contains(vertex)) {
                getGraph().removeVertex(vertex);
            }
        }
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
                    int bottomPortPosition = positions.get(bottomPort);
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
        if (plugs.keySet().contains(possiblePlug)) return true;
        return false;
    }

    public boolean isDummy (Vertex node) {
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

    public Map<Edge, Edge> getDummyEdge2RealEdge() {
        return dummyEdge2RealEdge;
    }

    public Set<NoEdgePort> getNoEdgePorts (Vertex node) {
        if (noEdgePorts.containsKey(node)) {
            return Collections.unmodifiableSet(noEdgePorts.get(node));
        } else {
            return Collections.unmodifiableSet(new LinkedHashSet<>());
        }
    }

    public Set<OneNodeEdge> getOneNodeEdges (Vertex node) {
        if (oneNodeEdges.containsKey(node)) {
            return Collections.unmodifiableSet(oneNodeEdges.get(node));
        } else {
            return Collections.unmodifiableSet(new LinkedHashSet<>());
        }
    }

    public boolean hasAssignedLayeres() {
        return hasAssignedLayeres;
    }

    public String[] getNodeName (Vertex node) {
        String[] nodeName;
        // todo: implement other cases
        if (isDummy(node) || hyperEdges.keySet().contains(node)) {
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
                nodeName[i] = node.getLabelManager().getLabels().get(i).toString().substring(numberOfDigitsAdded);
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
