package de.uniwue.informatik.praline.layouting.layered.kieleraccess;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.PralineLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.LayerAssignmentMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import de.uniwue.informatik.praline.layouting.layered.kieleraccess.util.ElkLayeredWithoutLayerRemoval;
import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.graph.Layer;
import org.eclipse.elk.alg.layered.graph.transform.ElkGraphTransformer;
import org.eclipse.elk.alg.layered.options.InternalProperties;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.impl.ElkGraphFactoryImpl;
import org.eclipse.emf.common.util.EList;

import java.awt.geom.Point2D;
import java.util.*;

public class KielerLayouter implements PralineLayouter {

    private SugiyamaLayouter sugiyForInternalUse;
    private DrawingInformation drawInfo;
    private LinkedHashMap<Vertex, ElkNode> vertices;
    private LinkedHashMap<ElkNode, Vertex> reverseVertices;
    private LinkedHashMap<Port, ElkPort> ports;
    private LinkedHashMap<Edge, ElkEdge> edges;
    private List<List<LNode>> lGraphLayers;

    public KielerLayouter(Graph graph) {
        this(graph, new DrawingInformation());
    }

    public KielerLayouter(Graph graph, DrawingInformation drawInfo) {
        this(graph, DirectionMethod.FORCE, LayerAssignmentMethod.NETWORK_SIMPLEX, 1, drawInfo);
    }

    public KielerLayouter(Graph graph, DirectionMethod directionMethod, LayerAssignmentMethod layerAssignmentMethod,
                          int numberOfIterationsFD) {
        this(graph, directionMethod, layerAssignmentMethod, numberOfIterationsFD, new DrawingInformation());
    }

    public KielerLayouter(Graph graph, DirectionMethod directionMethod,
                          LayerAssignmentMethod layerAssignmentMethod, int numberOfIterationsFD,
                          DrawingInformation drawInfo) {

        this.drawInfo = drawInfo;

        //do first steps of the "main" implementation to get a directed graph from an undirected one
        sugiyForInternalUse = new SugiyamaLayouter(graph);

        sugiyForInternalUse.construct();

        sugiyForInternalUse.assignDirections(directionMethod, numberOfIterationsFD);

        sugiyForInternalUse.assignLayers(layerAssignmentMethod);

        sugiyForInternalUse.nodePadding();
    }


    public KielerLayouter(SugiyamaLayouter sugiyWithPrecomputedDirectedGraph) {
        this(sugiyWithPrecomputedDirectedGraph, new DrawingInformation());
    }

    public KielerLayouter(SugiyamaLayouter sugiyWithPrecomputedDirectedGraph, DrawingInformation drawInfo) {

        this.drawInfo = drawInfo;

        //the first steps have already be done
        sugiyForInternalUse = sugiyWithPrecomputedDirectedGraph;

        if (!sugiyForInternalUse.hasAssignedLayers()) {
            sugiyForInternalUse.assignLayers(LayerAssignmentMethod.NETWORK_SIMPLEX);
            sugiyForInternalUse.nodePadding();
        }
    }

    @Override
    public void computeLayout() {
        computeLayeredDrawing();

        Graph pralineGraph = getStoredPralineGraph();
        writeResultToPralineGraph(pralineGraph);

        analyzeOrderings();

        sugiyForInternalUse.restoreOriginalElements();
    }

    @Override
    public Graph getGraph() {
        return getStoredPralineGraph();
    }

    @Override
    public DrawingInformation getDrawingInformation() {
        return this.drawInfo;
    }

    @Override
    public void setDrawingInformation(DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
    }

    public void generateSVG(String path) {
        sugiyForInternalUse.drawResult(path);
    }

    private Graph getStoredPralineGraph() {
        return sugiyForInternalUse.getGraphWithPreprocessedVertices();
    }

    private ElkNode computeLayeredDrawing() {
        //transform praline to ElkLayout
        Graph pralineGraph = sugiyForInternalUse.getGraphWithPreprocessedVertices();
        ElkNode graph = transformPralineGraph2ElkGraph(pralineGraph);

        //set properties
        LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
        graph.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        graph.setProperty(CoreOptions.SPACING_EDGE_EDGE, drawInfo.getEdgeDistanceHorizontal());
        graph.setProperty(CoreOptions.SPACING_NODE_NODE,
                Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing()));
        graph.setProperty(CoreOptions.SPACING_PORT_PORT, drawInfo.getPortSpacing());

        //transform ElkLayout to LLayout
        ElkGraphTransformer elkGraphTransformer = new ElkGraphTransformer();
        LGraph lGraph = elkGraphTransformer.importGraph(graph);

        //compute drawing
        ElkLayeredWithoutLayerRemoval elkLayeredAlgorithm = new ElkLayeredWithoutLayerRemoval();
        BasicProgressMonitor basicProgressMonitor = new BasicProgressMonitor();
        elkLayeredAlgorithm.doLayout(lGraph, basicProgressMonitor);

        //save layers before removing nodes from the layers
        saveLayers(elkLayeredAlgorithm);
        elkLayeredAlgorithm.finishLayouting(lGraph);

        //re-transform
        elkGraphTransformer.applyLayout(lGraph);

        return graph;
    }

    private void saveLayers(ElkLayeredWithoutLayerRemoval lGraphAlgo) {
        lGraphLayers = new ArrayList<>();


        for (LGraph component : lGraphAlgo.getComponents()) {
            for (int i = 0; i < component.getLayers().size(); i++) {
                Layer layer = component.getLayers().get(i);
                if (lGraphLayers.size() <= i) {
                    lGraphLayers.add(new ArrayList<>());
                }
                lGraphLayers.get(i).addAll(layer.getNodes());
            }
        }
    }

    private void writeResultToPralineGraph(Graph pralineGraph) {
        for (Vertex pralineVertex : pralineGraph.getVertices()) {
            ElkNode vertex = vertices.get(pralineVertex);
            double xPositionVertex = vertex.getX();
            double yPositionVertex = vertex.getY();
            pralineVertex.setShape(new Rectangle(xPositionVertex, yPositionVertex, vertex.getWidth(),
                    vertex.getHeight(), null));
            for (Port pralinePort : pralineVertex.getPorts()) {
                ElkPort port = ports.get(pralinePort);
                pralinePort.setShape(new Rectangle(xPositionVertex + port.getX(),
                        yPositionVertex + port.getY(),
                        port.getWidth(), port.getHeight(), null));
            }
        }
        for (Edge pralineEdge : pralineGraph.getEdges()) {
            ElkEdge edge = edges.get(pralineEdge);
            if (edge.getSections().isEmpty()) {
                continue;
            }
            ElkEdgeSection path = edge.getSections().get(0);
            pralineEdge.addPath(new PolygonalPath(
                    new Point2D.Double(path.getStartX(), path.getStartY()),
                    new Point2D.Double(path.getEndX(), path.getEndY()),
                    transformElkBendPoints2ListPoint2dDouble(path.getBendPoints())));
        }
    }

    private void analyzeOrderings() {
        //find node order
        List<List<Vertex>> pralineNodeOrder = new ArrayList<>();

        for (List<LNode> lGraphLayer : lGraphLayers) {
            List<Vertex> pralineLayer = new ArrayList<>();
            pralineNodeOrder.add(pralineLayer);
            //find all praline nodes of this layer and collect them in a list
            for (LNode lNode : lGraphLayer) {
                ElkNode elkNode = (ElkNode) lNode.getProperty(InternalProperties.ORIGIN);
                pralineLayer.add(reverseVertices.get(elkNode));
            }
            //sort nodes on a layer by x-coordinate
            pralineLayer.sort(Comparator.comparingDouble(v -> v.getShape().getXPosition()));
        }

        //find port order
        Map<Vertex, List<Port>> topPortOrder = new LinkedHashMap<>();
        Map<Vertex, List<Port>> bottomPortOrder = new LinkedHashMap<>();

        for (List<Vertex> pralineLayer : pralineNodeOrder) {
            for (Vertex pralineNode : pralineLayer) {
                assignToTopAndBottomSideAndSort(pralineNode, topPortOrder, bottomPortOrder);
            }
        }

        //save ordering
        sugiyForInternalUse.setOrders(new SortingOrder(pralineNodeOrder, topPortOrder, bottomPortOrder));
        sugiyForInternalUse.changeRanksAccordingToSortingOrder();
    }

    private void assignToTopAndBottomSideAndSort(Vertex pralineNode, Map<Vertex, List<Port>> topPortOrder,
                                                 Map<Vertex, List<Port>> bottomPortOrder) {
        List<Port> bottomList = new ArrayList<>();
        List<Port> topList = new ArrayList<>();
        double yPositionNode = pralineNode.getShape().getYPosition();
        //assign ports to either top or bottom side (acc. to y-coordinates)
        for (Port port : pralineNode.getPorts()) {
            if (port.getShape().getYPosition() <= yPositionNode) {
                bottomList.add(port);
            }
            else {
                topList.add(port);
            }
        }
        //sort the ports on each of both sides acc. to x-coordinates
        bottomList.sort(Comparator.comparingDouble(p -> p.getShape().getXPosition()));
        topList.sort(Comparator.comparingDouble(p -> p.getShape().getXPosition()));
        //save results
        bottomPortOrder.put(pralineNode, bottomList);
        topPortOrder.put(pralineNode, topList);
    }

    private static List<Point2D.Double> transformElkBendPoints2ListPoint2dDouble(EList<ElkBendPoint> points) {
        List<Point2D.Double> list = new ArrayList<>(points.size());
        for (ElkBendPoint point : points) {
            list.add(new Point2D.Double(point.getX(), point.getY()));
        }
        return list;
    }

    private ElkNode transformPralineGraph2ElkGraph(Graph pralineGraph) {
        ElkGraphFactory elkGraphFactory = new ElkGraphFactoryImpl();
        ElkNode wholeGraph = elkGraphFactory.createElkNode();
        //fix port pairings in praline graph
        int iterationsOfPortPairRepairing = 2;
        for (Vertex pralinePlug : sugiyForInternalUse.getPlugs().keySet()) {
            //in the first iterationsOfPortPairRepairing we use the "normal" port pairing repair algorithm to just
            // have the port pairings separated from each other
            //in the last run, we want to have the corresponding ports on both sides have *exactly* the same index
            // among the ordering of *all* ports of their vertex side. We need this to make kieler place these ports
            // (of a port pairing) on the same vertical line.
            for (int i = 0; i < iterationsOfPortPairRepairing + 1; i++) {
                sugiyForInternalUse.getDummyPortsForLabelPadding().putIfAbsent(pralinePlug, new LinkedHashSet<>());
                Set<Port> dummyPorts = sugiyForInternalUse.getDummyPortsForLabelPadding().get(pralinePlug);
                CrossingMinimization.repairPortPairings(sugiyForInternalUse, pralinePlug,
                        sugiyForInternalUse.getOrders().getBottomPortOrder(),
                        sugiyForInternalUse.getOrders().getTopPortOrder(), false,
                        i >= iterationsOfPortPairRepairing - 1, i < iterationsOfPortPairRepairing,
                        i == iterationsOfPortPairRepairing, i == iterationsOfPortPairRepairing ? dummyPorts : null);
            }
        }
        //transfer the just found port ordering to the lists of vertices and port groups
        sugiyForInternalUse.getOrders().transferPortOrderingToPortCompositionLists();
        //create vertices and ports
        vertices = new LinkedHashMap<>(pralineGraph.getVertices().size());
        reverseVertices = new LinkedHashMap<>(pralineGraph.getVertices().size());
        ports = new LinkedHashMap<>();
        edges = new LinkedHashMap<>(pralineGraph.getEdges().size());
        for (Vertex pralineVertex : pralineGraph.getVertices()) {
            ElkNode vertex = elkGraphFactory.createElkNode();
            vertex.setParent(wholeGraph);
            Shape pralineVertexShape = pralineVertex.getShape();
            if (pralineVertexShape instanceof Rectangle
                    && !Double.isNaN(((Rectangle) pralineVertexShape).getHeight())) {
                vertex.setHeight(((Rectangle) pralineVertexShape).getHeight());
            }
            else {
                vertex.setHeight(drawInfo.getVertexHeight() * (double) getNumberOfStackedVertices(pralineVertex));
            }
            if (pralineVertexShape instanceof Rectangle
                    && !Double.isNaN(((Rectangle) pralineVertexShape).getWidth())) {
                vertex.setWidth(((Rectangle) pralineVertexShape).getWidth());
            }
            else {
                int numberOfPorts = Math.max(sugiyForInternalUse.getOrders().getTopPortOrder().get(pralineVertex).size(),
                        sugiyForInternalUse.getOrders().getBottomPortOrder().get(pralineVertex).size());
                vertex.setWidth(Math.max(sugiyForInternalUse.getMinWidthForNode(pralineVertex),
                        numberOfPorts * (drawInfo.getPortWidth() + drawInfo.getPortSpacing())
                                + drawInfo.getPortSpacing()));
            }
            vertices.put(pralineVertex, vertex);
            reverseVertices.put(vertex, pralineVertex);

            //create elk ports for each praline port (in order) and check which port constrained level (fixed side or
            // fixed order) we must use
            Collection<ElkPort> portsOfThisVertex = getPortsInOrder(pralineVertex, ports, elkGraphFactory);
            vertex.getPorts().addAll(portsOfThisVertex);
            if (sugiyForInternalUse.isPlug(pralineVertex) || fixOrder(pralineVertex.getPortCompositions())) {
                vertex.setProperty(LayeredOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            }
            else {
                vertex.setProperty(LayeredOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE);
            }
        }

        //re-add self loops
        for (Vertex nodeWithLoop : sugiyForInternalUse.getLoopEdges().keySet()) {
            Set<Edge> loopEdges = sugiyForInternalUse.getLoopEdges().get(nodeWithLoop);
            for (Edge loopEdge : loopEdges) {
                pralineGraph.addEdge(loopEdge);
                if (loopEdge.getPorts().isEmpty()) {
                    loopEdge.addPorts(sugiyForInternalUse.getPortsOfLoopEdge(loopEdge));
                }
            }
        }
        sugiyForInternalUse.getLoopEdges().clear();

        //create edges
        for (Edge pralineEdge : pralineGraph.getEdges()) {
            Port pralinePort0 = pralineEdge.getPorts().get(0);
            int rank0 = sugiyForInternalUse.getRank(pralinePort0.getVertex());
            Port pralinePort1 = pralineEdge.getPorts().get(1);
            int rank1 = sugiyForInternalUse.getRank(pralinePort1.getVertex());
            ElkPort sourcePort = rank0 < rank1 ? ports.get(pralinePort0) : ports.get(pralinePort1);
            ElkPort targetPort = sourcePort.equals(ports.get(pralinePort1)) ? ports.get(pralinePort0) :
                    ports.get(pralinePort1);

            ElkEdge edge = elkGraphFactory.createElkEdge();
            edge.getSources().add(sourcePort);
            edge.getTargets().add(targetPort);
            edges.put(pralineEdge, edge);
        }

        return wholeGraph;
    }

    private int getNumberOfStackedVertices(Vertex pralineVertex) {
        int numberOfStackedVertices = 1;
        if (sugiyForInternalUse.isPlug(pralineVertex)) {
            numberOfStackedVertices = 2;
        }
        else if (sugiyForInternalUse.getVertexGroups().containsKey(pralineVertex)) {
            numberOfStackedVertices = 2;
            //if it is a device connector vertex with device connectors on both sides and a device in the
            // middle, we have 3 rows of vertices stacked on each other
            if (!sugiyForInternalUse.getOrders().getBottomPortOrder().get(pralineVertex).isEmpty() &&
                    !sugiyForInternalUse.getOrders().getTopPortOrder().get(pralineVertex).isEmpty()) {
                numberOfStackedVertices = 3;
            }
        }
        return numberOfStackedVertices;
    }

    private boolean fixOrder(List<PortComposition> portCompositions) {
        int elementsNorthSide = 0;
        int elementsSouthSide = 0;
        boolean hasPortGroupsWithPorts = false;
        for (PortComposition portComposition : portCompositions) {
            if (portComposition instanceof PortGroup) {
                //if a child as order set to fixed and has in turn more than 1 children, then we must fix the order
                if (((PortGroup) portComposition).isOrdered() &&
                        ((PortGroup) portComposition).getPortCompositions().size() > 1) {
                    return true;
                }
                //check contained port compositions recursively
                if (fixOrder(((PortGroup) portComposition).getPortCompositions())) {
                    return true;
                }
                //otherwise we have look if it has ports on the north or the south side and count them
                List<Port> containedPorts = PortUtils.getPortsRecursively(portComposition);
                if (!containedPorts.isEmpty()) {
                    hasPortGroupsWithPorts = true;
                    if (containedPorts.get(0).getOrientationAtVertex() == Orientation.NORTH) {
                        ++elementsNorthSide;
                    }
                    else {
                        ++elementsSouthSide;
                    }
                }
            }
            else if (portComposition instanceof Port) {
                if (((Port) portComposition).getOrientationAtVertex() == Orientation.NORTH) {
                    ++elementsNorthSide;
                }
                else if (((Port) portComposition).getOrientationAtVertex() == Orientation.SOUTH) {
                    ++elementsSouthSide;
                }
                else {
                    System.out.println("Warning! Port " + portComposition + " of vertex " + portComposition.getVertex()
                            + " is neither assigned to the top nor to the bottom side of its vertex. Noticed when"
                            + " transforming to KIELER layout.");
                }
            }
        }
        //if we have more than 1 element on one side and these are not only ports, we must fix the total order
        if (hasPortGroupsWithPorts && (elementsNorthSide > 1 || elementsSouthSide > 1)) {
            return true;
        }

        //otherwise it is enough to set the port constraint to fixed side (but free on each side)
        return false;
    }


    /**
     * first returns the bottom ports, then the top ports.
     * Note that North and South is switched between praline notation and kieler notation!
     * (though in the praline algorithm we rather use top and bottom instead of North and South)
     *
     * @param pralineVertex
     * @param ports
     * @param elkGraphFactory
     * @return
     */
    private Collection<ElkPort> getPortsInOrder(Vertex pralineVertex, LinkedHashMap<Port, ElkPort> ports,
                                                ElkGraphFactory elkGraphFactory) {
        ArrayList<ElkPort> portList = new ArrayList<>();

        for (Port pralinePort : sugiyForInternalUse.getOrders().getBottomPortOrder().get(pralineVertex)) {
            //would be Orientation.SOUTH in praline data structure
            createPort(ports, elkGraphFactory, portList, pralinePort, PortSide.NORTH);
        }
        //top ports in reverse order
        LinkedList<Port> topPorts =
                new LinkedList<>(sugiyForInternalUse.getOrders().getTopPortOrder().get(pralineVertex));
        Iterator<Port> portIterator = topPorts.descendingIterator();
        while (portIterator.hasNext()) {
            Port pralinePort = portIterator.next();
            //would be Orientation.NORTH in praline data structure
            createPort(ports, elkGraphFactory, portList, pralinePort, PortSide.SOUTH);
        }

        return portList;
    }

    private void createPort(LinkedHashMap<Port, ElkPort> ports, ElkGraphFactory elkGraphFactory,
                            ArrayList<ElkPort> portList, Port pralinePort, PortSide portSide) {
        ElkPort port = elkGraphFactory.createElkPort();
        port.setWidth(drawInfo.getPortWidth());
        port.setHeight(drawInfo.getPortHeight());
        port.setProperty(CoreOptions.PORT_SIDE, portSide);
        ports.put(pralinePort, port);
        portList.add(port);
    }
}
