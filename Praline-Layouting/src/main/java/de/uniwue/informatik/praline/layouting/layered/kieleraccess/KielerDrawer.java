package de.uniwue.informatik.praline.layouting.layered.kieleraccess;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.kieleraccess.util.OrthogonalCrossingsAnalysis;
import org.eclipse.elk.alg.layered.ElkLayered;
import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.transform.ElkGraphTransformer;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.impl.ElkGraphFactoryImpl;
import org.eclipse.emf.common.util.EList;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;

public class KielerDrawer {

    public static final String JSON_PATH =
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-1dda4e2a-ae64-4e76-916a-822c4e838c41.json";
//            "Praline-Layouting/data/example-very-small/praline-a0b0b5a2-2c23-43b0-bb87-4ddeb34d5a02.json";
//            "Praline-Layouting/data/example-pseudo-plans/praline-pseudo-plan-0a94e4bf6d729042.json";
            "Praline-Layouting/data/praline-package-2020-05-18/praline-0488185b-18b4-4780-a6c8-1d9ece91252e.json";
//            "Praline-Layouting/data/example-pseudo-plans/praline-pseudo-plan-0f90e022f10bae3f.json";

    public static final String SVG_TARGET_PATH = "Praline-Layouting/results/testKIELER.svg";

    public static void main(String[] args) throws IOException {
        //test this class here
        Graph graph = Serialization.read(JSON_PATH, Graph.class); //creator.createTestGraph();
        System.out.println("Graph read");

        KielerDrawer kielerDrawer = new KielerDrawer(graph);

        Graph resultGraph = kielerDrawer.draw();
        System.out.println("Number of crossings = " + kielerDrawer.getNumberOfCrossings());

        kielerDrawer.generateSVG(SVG_TARGET_PATH);

        System.out.println("KIELER test done successfully");
    }

    private SugiyamaLayouter sugiyForInternalUse;
    private DrawingInformation drawInfo;
    private LinkedHashMap<Vertex, ElkNode> vertices;
    private LinkedHashMap<Port, ElkPort> ports;
    private LinkedHashMap<Edge, ElkEdge> edges;
    private int numberOfCrossings = -1;

    public KielerDrawer(Graph graph) {
        this(graph, new DrawingInformation());
    }

    public KielerDrawer(Graph graph, DrawingInformation drawInfo) {
        this(graph, DirectionMethod.FORCE, 1, drawInfo);
    }

    public KielerDrawer(Graph graph, DirectionMethod directionMethod, int numberOfIterationsFD) {
        this(graph, directionMethod, numberOfIterationsFD, new DrawingInformation());
    }

    public KielerDrawer(Graph graph, DirectionMethod directionMethod, int numberOfIterationsFD,
                        DrawingInformation drawInfo) {

        this.drawInfo = drawInfo;

        //do first steps of the "main" implementation to get a directed graph from an undirected one
        sugiyForInternalUse = new SugiyamaLayouter(graph);

        sugiyForInternalUse.construct();

        sugiyForInternalUse.assignDirections(directionMethod, numberOfIterationsFD);

        sugiyForInternalUse.assignLayers();
    }


    public KielerDrawer(SugiyamaLayouter sugiyWithPrecomputedDirectedGraph) {
        this(sugiyWithPrecomputedDirectedGraph, new DrawingInformation());
    }

    public KielerDrawer(SugiyamaLayouter sugiyWithPrecomputedDirectedGraph, DrawingInformation drawInfo) {

        this.drawInfo = drawInfo;

        //the first steps have already be done
        sugiyForInternalUse = sugiyWithPrecomputedDirectedGraph;

        if (!sugiyForInternalUse.hasAssignedLayeres()) {
            sugiyWithPrecomputedDirectedGraph.assignLayers();
        }
    }

    public Graph draw() {
        computeLayeredDrawing();

        Graph pralineGraph = getStoredPralineGraph();
        writeResultToPralineGraph(pralineGraph);

        return pralineGraph;
    }

    public void generateSVG(String path) {
        sugiyForInternalUse.drawResult(path);
        System.out.println("wrote file to " + path);
    }

    private Graph getStoredPralineGraph() {
        return sugiyForInternalUse.getGraph();
    }

    public int getNumberOfCrossings() {
        return numberOfCrossings;
    }


    private ElkNode computeLayeredDrawing() {
        //transform praline to ElkLayout
        Graph pralineGraph = sugiyForInternalUse.getGraph();
        ElkNode graph = transformPralineGraph2ElkGraph(pralineGraph);

        //set properties
        LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
        graph.setProperty(CoreOptions.DIRECTION, Direction.UP);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        graph.setProperty(CoreOptions.SPACING_EDGE_EDGE, drawInfo.getEdgeDistanceHorizontal());
        graph.setProperty(CoreOptions.SPACING_NODE_NODE,
                Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing()));
        graph.setProperty(CoreOptions.SPACING_PORT_PORT, drawInfo.getPortSpacing());

        //transform ElkLayout to LLayout
        ElkGraphTransformer elkGraphTransformer = new ElkGraphTransformer();
        LGraph lGraph = elkGraphTransformer.importGraph(graph);

        //compute drawing
        ElkLayered elkLayeredAlgorithm = new ElkLayered();
        BasicProgressMonitor basicProgressMonitor = new BasicProgressMonitor();
        elkLayeredAlgorithm.doLayout(lGraph, basicProgressMonitor);

        //re-transform
        elkGraphTransformer.applyLayout(lGraph);

        //count crossings
        OrthogonalCrossingsAnalysis orthogonalCrossingsAnalysis = new OrthogonalCrossingsAnalysis();
        this.numberOfCrossings = orthogonalCrossingsAnalysis.countCrossings(graph);

        return graph;
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
        //create vertices and ports
        vertices = new LinkedHashMap<>(pralineGraph.getVertices().size());
        ports = new LinkedHashMap<>();
        LinkedHashMap<ElkNode, LinkedHashSet<ElkPort>> pairedPorts = new LinkedHashMap<>();
        LinkedHashMap<ElkPort, ElkPort> portPairings = new LinkedHashMap<>();
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
                vertex.setHeight(drawInfo.getVertexHeight());
            }
            if (pralineVertexShape instanceof Rectangle
                    && !Double.isNaN(((Rectangle) pralineVertexShape).getWidth())) {
                vertex.setWidth(((Rectangle) pralineVertexShape).getWidth());
            }
            else {
                vertex.setWidth(drawInfo.getMinVertexWidth(pralineVertex));
            }
            vertices.put(pralineVertex, vertex);

            //go carefully through each port composition and fix the complete order if there are constraints
            Collection<ElkPort> portsOfThisVertex = getPortsInOrder(pralineVertex.getPortCompositions(), ports,
                    elkGraphFactory);
            vertex.getPorts().addAll(portsOfThisVertex);
            if (List.class.isAssignableFrom(portsOfThisVertex.getClass())) {
                vertex.setProperty(LayeredOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            }
            else {
                //if it contains a group, fix at least sides
                for (PortComposition portComposition : pralineVertex.getPortCompositions()) {
                    if (portComposition instanceof PortGroup) {
                        vertex.setProperty(LayeredOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE);
                    }
                }
            }

            //find port pairings
            pairedPorts.put(vertex, new LinkedHashSet<>());
            for (Port pralinePort : pralineVertex.getPorts()) {
                if (sugiyForInternalUse.isPaired(pralinePort)) {
                    pairedPorts.get(vertex).add(ports.get(pralinePort));
                    portPairings.put(ports.get(pralinePort), ports.get(sugiyForInternalUse.getPairedPort(pralinePort)));
                }
            }
        }
        //fix port pairings
        //push them to the very left of the north and the south side of a vertex
        for (ElkNode vertex : vertices.values()) {
            LinkedHashSet<ElkPort> pairedPortsOfVertex = pairedPorts.get(vertex);
            if (!pairedPortsOfVertex.isEmpty()) {
                vertex.setProperty(LayeredOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
                LinkedList<ElkPort> northPorts = new LinkedList<>();
                LinkedList<ElkPort> southPorts = new LinkedList<>();
                PortSide currentPortSide = PortSide.NORTH;
                List<ElkPort> currentPortList = northPorts;
                for (ElkPort port : vertex.getPorts()) {
                    if (pairedPortsOfVertex.contains(port)) {
                        if (port.getProperty(CoreOptions.PORT_SIDE) == PortSide.SOUTH) {
                            currentPortSide = PortSide.SOUTH;
                            currentPortList = southPorts;
                        }
                        else {
                            //value is not set and this should be on the north side & the paired port on the south side
                            port.setProperty(CoreOptions.PORT_SIDE, PortSide.NORTH);
                            ElkPort pairedPort = portPairings.get(port);
                            pairedPort.setProperty(CoreOptions.PORT_SIDE, PortSide.SOUTH);
                            northPorts.addFirst(port);
                            southPorts.addFirst(pairedPort);
                        }
                    }
                    else {
                        port.setProperty(CoreOptions.PORT_SIDE, currentPortSide);
                        currentPortList.add(port);
                    }
                }
                vertex.getPorts().clear();
                vertex.getPorts().addAll(northPorts);
                Collections.reverse(southPorts);
                vertex.getPorts().addAll(southPorts);
            }

        }

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

    /**
     *
     * @param portCompositions
     * @param ports
     * @return
     *      {@link List} if it is ordered or {@link Set} if it is unordered
     */
    private Collection<ElkPort> getPortsInOrder(List<PortComposition> portCompositions, LinkedHashMap<Port, ElkPort> ports,
                                                ElkGraphFactory elkGraphFactory) {
        boolean fixOrder = false;

        ArrayList<ElkPort> portList = new ArrayList<>();
        for (PortComposition portComposition : portCompositions) {
            if (portComposition instanceof PortGroup) {
                //TODO: first case check additionally that there is >1 port within the port group
                if (portCompositions.size() > 1 || ((PortGroup) portComposition).isOrdered()) {
                    fixOrder = true;
                }
                Collection<ElkPort> subPorts =
                        getPortsInOrder(((PortGroup) portComposition).getPortCompositions(), ports, elkGraphFactory);
                if (List.class.isAssignableFrom(subPorts.getClass())) {
                    fixOrder = true;
                }
                for (ElkPort subPort : subPorts) {
                    portList.add(subPort);
                }
            }
            else if (portComposition instanceof Port) {
                ElkPort port = elkGraphFactory.createElkPort();
                port.setWidth(drawInfo.getPortWidth());
                port.setHeight(drawInfo.getPortHeight());
                port.setProperty(CoreOptions.PORT_SIDE, PortSide.NORTH);
                ports.put((Port) portComposition, port);
                portList.add(port);
            }
        }

        if (!fixOrder) {
            return new LinkedHashSet<>(portList);
        }
        return portList;
    }
}
