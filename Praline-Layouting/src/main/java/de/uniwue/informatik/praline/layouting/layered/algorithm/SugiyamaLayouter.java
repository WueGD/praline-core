package de.uniwue.informatik.praline.layouting.layered.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
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
    private Map<Vertex, Edge> hyperEdges;
    private Map<Edge, Vertex> hyperEdgeParts;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Port, Port> replacedPorts;
    private Map<Port, List<Port>> multipleEdgePort2replacePorts;
    protected Map<Port, Port> keptPortPairings;
    private Map<Vertex, Set<NoEdgePort>> noEdgePorts;
    private Map<Vertex, Set<OneNodeEdge>> oneNodeEdges;
    private Map<Edge, Edge> dummyEdge2RealEdge;

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
        computeLayout(DEFAULT_DIRECTION_METHOD, DEFAULT_CROSSING_MINIMIZATION_METHOD, DEFAULT_NUMBER_OF_CM_ITERATIONS);
    }

    public void computeLayout (DirectionMethod method, CrossingMinimizationMethod cmMethod, int numberOfIterationsCM) {
        //chose methods for directionassignment
        //chose other steps to be done or not
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
        //handle edge bundles
        handleEdgeBundle();
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
        //restoreOriginalElements();
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
        for (Edge edge : new ArrayList<>(this.getGraph().getEdges())) {
            if (hyperEdgeParts.containsKey(edge)) {
                restoreHyperedgePart(edge);
            }
        }

        //replace dummy vertices
        for (Vertex vertex : new ArrayList<>(this.getGraph().getVertices())) {
            if (hyperEdges.containsKey(vertex)) {
                replaceHyperEdgeDummyVertex(vertex);
            }
            if (vertexGroups.containsKey(vertex)) {
                VertexGroup vertexGroup = vertexGroups.get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }
            if (plugs.containsKey(vertex)) {
                VertexGroup vertexGroup = plugs.get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }
            if (dummyTurningNodes.containsKey(vertex)) {
                getGraph().removeVertex(vertex);
            }
            if (dummyNodesLongEdges.containsKey(vertex)) {
                getGraph().removeVertex(vertex);
            }
        }

        //TODO: add ports without edges, loop-edges, ports with loop-edges, ports of vertexGroup that are paired
        // within the vertex group but do not have outgoing edges
        //TODO: check that #ports, #vertices, #edges is in the end the same as at the beginning

        //first we have already replaced in restoreVertexGroup (...) the ports that were created during vertex group
        // handeling; these are are the ports in replacedPorts where the original vertex is not in
        // multipleEdgePort2replacePorts

        //second we replace the ports that were created during the phase where ports with multiple edges were split to
        // multiple ports; now we re-unify all these ports back to one. If there is a port pairing involved, we keep
        // the one on the opposite site to the port pairing; otherwise we keep the/a middle one
        for (Port origPort : multipleEdgePort2replacePorts.keySet()) {
            restorePortsWithMultipleEdges(origPort);
        }
    }

    private void restorePortsWithMultipleEdges(Port origPort) {
        List<Port> replacePorts = multipleEdgePort2replacePorts.get(origPort);
        Shape shapeOfPairedPort = null;
        Vertex vertex = replacePorts.iterator().next().getVertex();
        PortGroup portGroupOfThisReplacement = replacePorts.iterator().next().getPortGroup();
        PortGroup containingPortGroup = portGroupOfThisReplacement.getPortGroup(); //second call because
        // first port group is just the port group created extra for the replace ports
        // re-add orig port
        if (containingPortGroup == null) {
            vertex.addPortComposition(origPort);
        }
        else {
            containingPortGroup.addPortComposition(origPort);
        }
        //remove all replace ports and possible save shape
        for (Port replacePort : replacePorts) {
            if (isPaired(replacePort)) {
                shapeOfPairedPort = replacePort.getShape();
            }
            vertex.removePortComposition(replacePort);
        }
        vertex.removePortComposition(portGroupOfThisReplacement);
        //find shape of origPort
        if (shapeOfPairedPort != null) {
            origPort.setShape(shapeOfPairedPort.clone());
        }
        else {
            replacePorts.sort(Comparator.comparing(p -> p.getShape().getXPosition()));
            //pick the shape of the (left) middle one
            origPort.setShape(replacePorts.get((replacePorts.size() - 1) / 2).getShape().clone());
        }
        //re-hang edges
        //find target point
        Rectangle origPortShape = (Rectangle) origPort.getShape();
        Point2D.Double targetPoint = new Point2D.Double(
                origPortShape.getXPosition() + 0.5 * origPortShape.getWidth(),
                origPortShape.getYPosition() < vertex.getShape().getYPosition() ?
                        origPortShape.getYPosition() : origPortShape.getYPosition() + origPortShape.getHeight()
        );
        //re-draw edges
        for (Port replacePort : replacePorts) {
            for (Edge edge : new ArrayList<>(replacePort.getEdges())) {
                //change ports
                edge.removePort(replacePort);
                edge.addPort(origPort);
                //change drawn path at the port
                //make path of this edge part longer to reach the middle of the old dummy vertex
                Rectangle shapeReplacePort = (Rectangle) replacePort.getShape();
                if (shapeReplacePort.equals(origPortShape)) {
                    continue;
                }
                for (Path path : edge.getPaths()) {
                    Point2D.Double startPoint = ((PolygonalPath) path).getStartPoint();
                    Point2D.Double endPoint = ((PolygonalPath) path).getEndPoint();
                    Point2D.Double pointAtPort =  null;
                    Point2D.Double foreLastPoint = null;
                    Point2D.Double newForeLastPoint = new Point2D.Double();
                    if (shapeReplacePort.liesOnBoundary(startPoint)) {
                        pointAtPort = startPoint;
                        foreLastPoint = ((PolygonalPath) path).getBendPoints().isEmpty() ?
                                ((PolygonalPath) path).getEndPoint() :
                                ((PolygonalPath) path).getBendPoints().get(0);
                    }
                    else if (shapeReplacePort.liesOnBoundary(endPoint)) {
                        pointAtPort = endPoint;
                        foreLastPoint = ((PolygonalPath) path).getBendPoints().isEmpty() ?
                                ((PolygonalPath) path).getStartPoint() :
                                ((PolygonalPath) path).getBendPoints().get(
                                        ((PolygonalPath) path).getBendPoints().size() -1);
                    }
                    if (pointAtPort != null) {
                        newForeLastPoint.x = pointAtPort.x;
                        newForeLastPoint.y = pointAtPort.y +
                                //TODO: transform the .75 into a variable in DrawingInformation
                                drawInfo.getEdgeDistanceVertical() * (foreLastPoint.y > pointAtPort.y ? .75 : -.75);
                        if (pointAtPort == startPoint) {
                            ((PolygonalPath) path).getBendPoints().add(0, newForeLastPoint);
                            startPoint.setLocation(targetPoint);
                        }
                        else {
                            ((PolygonalPath) path).getBendPoints().add(
                                    ((PolygonalPath) path).getBendPoints().size(), newForeLastPoint);
                            endPoint.setLocation(targetPoint);
                        }
                    }
                }
            }
        }
    }

    private void restoreVertexGroup(Vertex dummyUnificationVertex, VertexGroup vertexGroup) {
        //-1: bottom side, 0: undefined, 1: top side
        Map<Integer, List<Vertex>> vertexSide2origVertex = new LinkedHashMap<>();
        Map<Vertex, Double> minX = new LinkedHashMap<>();
        Map<Vertex, Double> maxX = new LinkedHashMap<>();
        for (Vertex originalVertex : vertexGroup.getAllRecursivelyContainedVertices()) {
            minX.put(originalVertex, Double.POSITIVE_INFINITY);
            maxX.put(originalVertex, Double.NEGATIVE_INFINITY);
            int vertexSide = 0; //-1: bottom side, 0: undefined, 1: top side
            for (Port port : orders.getTopPortOrder().get(dummyUnificationVertex)) {
                int changeTo = 1;
                vertexSide =
                        changeVertexSideIfContained(minX, maxX, originalVertex, vertexSide, port, changeTo);
            }
            for (Port port : orders.getBottomPortOrder().get(dummyUnificationVertex)) {
                int changeTo = -1;
                vertexSide =
                        changeVertexSideIfContained(minX, maxX, originalVertex, vertexSide, port, changeTo);
            }
            if (!vertexSide2origVertex.containsKey(vertexSide)) {
                vertexSide2origVertex.put(vertexSide, new ArrayList<>());
            }
            vertexSide2origVertex.get(vertexSide).add(originalVertex);
        }

        int numberOfDifferentSides = vertexSide2origVertex.keySet().size();
        int yShiftMultiplier = 0;
        for (int vertexSide = -1; vertexSide <= 1; vertexSide++) {
            List<Vertex> originalVertices = vertexSide2origVertex.get(vertexSide);
            if (originalVertices != null) {
                double xPos = dummyUnificationVertex.getShape().getXPosition();
                //sort by x-coordinates
                originalVertices.sort(Comparator.comparing(minX::get));
                for (int j = 0; j < originalVertices.size(); j++) {
                    Vertex originalVertex = originalVertices.get(j);
                    //determine shape for original vertex
                    originalVertex.setShape(dummyUnificationVertex.getShape().clone());
                    Rectangle originalVertexShape = (Rectangle) originalVertex.getShape();
                    originalVertexShape.height = originalVertexShape.height / (double) numberOfDifferentSides;
                    originalVertexShape.y = originalVertexShape.y +
                            (double) yShiftMultiplier * originalVertexShape.getHeight();
                    originalVertexShape.x = xPos;
                    double endXPos = j + 1 == originalVertices.size() ?
                            dummyUnificationVertex.getShape().getXPosition() +
                                    ((Rectangle) dummyUnificationVertex.getShape()).width :
                            (maxX.get(originalVertex) + minX.get(originalVertices.get(j + 1))) / 2.0;
                    originalVertexShape.width = endXPos - xPos;
                    xPos = endXPos;
                    graph.addVertex(originalVertex);
                }
                ++yShiftMultiplier;
            }
        }
        //transfer shape of the replace ports to the original ports
        for (Port replacePort : dummyUnificationVertex.getPorts()) {
            Port origPort = replacedPorts.get(replacePort);
            origPort.setShape(replacePort.getShape().clone());
            if (keptPortPairings.containsKey(replacePort)) {
                keptPortPairings.put(origPort, keptPortPairings.get(replacePort));
                keptPortPairings.remove(replacePort);
            }
            //re-hang edges
            for (Edge edge : new ArrayList<>(replacePort.getEdges())) {
                edge.removePort(replacePort);
                edge.addPort(origPort);
            }
        }
        //remove unification dummy node
        graph.removeVertex(dummyUnificationVertex);
        //re-add vertex group
        graph.addVertexGroup(plugs.get(dummyUnificationVertex));
        graph.addVertexGroup(vertexGroups.get(dummyUnificationVertex));
    }

    private int changeVertexSideIfContained(Map<Vertex, Double> minX, Map<Vertex, Double> maxX, Vertex originalVertex,
                                            int vertexSide, Port port, int changeTo) {
        Port portBeforeUnification = replacedPorts.get(port);
        if (originalVertex.getPorts().contains(portBeforeUnification) ||
                originalVertex.getPorts().contains(replacedPorts.get(port))) {
            vertexSide = changeTo;
            double xBeginPort = port.getShape().getXPosition();
            if (xBeginPort < minX.get(originalVertex)) {
                minX.replace(originalVertex, xBeginPort);
            }
            double xEndPort = xBeginPort + ((Rectangle) port.getShape()).width;
            if (xEndPort > maxX.get(originalVertex)) {
                maxX.replace(originalVertex, xEndPort);
            }
        }
        return vertexSide;
    }

    private void replaceHyperEdgeDummyVertex(Vertex hyperEdgeDummyVertex) {
        Rectangle vertexShape = (Rectangle) hyperEdgeDummyVertex.getShape();
        Edge hyperEdge = hyperEdges.get(hyperEdgeDummyVertex);
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double y = Double.NaN;
        for (Path path : hyperEdge.getPaths()) {
            Point2D.Double startPoint = ((PolygonalPath) path).getStartPoint();
            Point2D.Double endPoint = ((PolygonalPath) path).getEndPoint();
            if (vertexShape.contains(startPoint)) {
                minX = Math.min(minX, startPoint.x);
                maxX = Math.max(maxX, startPoint.x);
                y = startPoint.y;
            }
            if (vertexShape.contains(endPoint)) {
                minX = Math.min(minX, endPoint.x);
                maxX = Math.max(maxX, endPoint.x);
                y = endPoint.y;
            }
        }
        //add horizontal segment as replacement for the dummy vertex
        hyperEdge.addPath(new PolygonalPath(new Point2D.Double(minX, y), new Point2D.Double(maxX, y), null));
        getGraph().removeVertex(hyperEdgeDummyVertex);
    }

    private void restoreHyperedgePart(Edge edgePart) {
        Vertex dummyVertexHyperEdge = hyperEdgeParts.get(edgePart);
        Rectangle shapeDummyVertex = (Rectangle) dummyVertexHyperEdge.getShape();
        //make path of this edge part longer to reach the middle of the old dummy vertex
        Port portAtDummyVertex = PortUtils.getPortAtVertex(edgePart, dummyVertexHyperEdge);
        Rectangle shapeDummyPort = (Rectangle) portAtDummyVertex.getShape();
        Point2D.Double startPoint = ((PolygonalPath) edgePart.getPaths().get(0)).getStartPoint();
        Point2D.Double endPoint = ((PolygonalPath) edgePart.getPaths().get(0)).getEndPoint();
        Point2D.Double pointAtPort = shapeDummyPort.liesOnBoundary(startPoint) ?
                startPoint : shapeDummyPort.liesOnBoundary(endPoint) ? endPoint : null;
        double extraLength = shapeDummyPort.getHeight() + shapeDummyVertex.getHeight() / 2.0;
        if (pointAtPort.getY() < dummyVertexHyperEdge.getShape().getYPosition()) {
            //make longer in +y direction -> move point up
            pointAtPort.y = pointAtPort.y + extraLength;
        }
        else {
            //make longer in -y direction -> move point down
            pointAtPort.y = pointAtPort.y - extraLength;
        }
        //include dummy edge part into original hyperedge
        Edge originalEdge = hyperEdges.get(dummyVertexHyperEdge);
        replaceByOriginalEdge(edgePart, originalEdge);
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
        hyperEdges = new LinkedHashMap<>();
        hyperEdgeParts = new LinkedHashMap<>();
        dummyNodesLongEdges = new LinkedHashMap<>();
        replacedPorts = new LinkedHashMap<>();
        multipleEdgePort2replacePorts = new LinkedHashMap<>();
        keptPortPairings = new LinkedHashMap<>();
        noEdgePorts = new LinkedHashMap<>();
        oneNodeEdges = new LinkedHashMap<>();
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
            Map<Port, Set<Port>> allPairings = new LinkedHashMap<>();
            if (group.getContainedVertices().size() == (group.getTouchingPairs().size() + 1)) {
                stickTogether = true;

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
            String groupLabelText = "no_GroupMainLabel";
            if (group.getLabelManager().getMainLabel() != null) {
                groupLabelText = group.getLabelManager().getMainLabel().toString();
            }
            String idV = ("GroupRep_for_" + groupLabelText + "_#" + index1++);
            if (stickTogether) idV = ("PlugRep_for_" + groupLabelText + "_#" + (index1-1));
            createMainLabel(idV, representative);
            index2 = 0;

            getGraph().addVertex(representative);
            Map<Port, Port> originalPort2representative = new LinkedHashMap<>();
            for (Edge edge : outgoingEdges.keySet()) {
                // create new port at unification vertex and remove old one on original vertex,
                // hang the edges from the old to the new port
                Port p1 = edge.getPorts().get(0);
                Port p2 = outgoingEdges.get(edge);
                if (p2.equals(p1)) p1 = edge.getPorts().get(1);
                Port replacePort = new Port();
                createMainLabel(("VG_PortRep_for_" + p1.getLabelManager().getMainLabel().toString() + "_#" + index1 +
                        "-" + index2), replacePort);

                edge.removePort(p1);
                edge.addPort(replacePort);

                //JZ: I commented the following parts out because we do not really need to create new edges, we can
                // re-hang the old one

//                List<Port> rEdgePorts = new LinkedList<>();
//                rEdgePorts.add(replacePort);
//                rEdgePorts.add(p2);
//                Edge replaceEdge = new Edge(rEdgePorts);
//                createMainLabel(("VG_EdgeRep_for_" + edge.getLabelManager().getMainLabel().toString() + "_#" + index1 + "-" + index2++), replaceEdge);
//
//                getGraph().addEdge(replaceEdge);
//                replacedEdges.put(p2, edge);
//                dummyEdge2RealEdge.put(replaceEdge, edge);
//                getGraph().removeEdge(edge);
//                p2.removeEdge(edge);

                if (stickTogether) {
                    replacedPorts.put(replacePort, p1);
                    originalPort2representative.put(p1, replacePort);
                } else {
                    representative.addPortComposition(replacePort);
                }
            }

            // create portGroups if stickTogether
            if (stickTogether) {
                for (Vertex groupNode : group.getContainedVertices()) {
                    representative.addPortComposition(keepPortGroupsRecursive(new PortGroup(),
                            groupNode.getPortCompositions(), originalPort2representative));
                }
                if (hasPortPairings) {
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

    public boolean isTopPort (Port port) {
        return orders.getTopPortOrder().get(port.getVertex()).contains(port);
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
        return hasAssignedLayers;
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
