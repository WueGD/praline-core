package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.PortSideAssignment;

import java.util.*;

public class DummyNodeCreation {

    private SugiyamaLayouter sugy;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Edge> dummyNodesSelfLoops;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Port, Boolean> portOnWrongSideHasEdgeGoingOnTheLeftSideAroundNode;
    private Map<Vertex, Vertex> nodeToLowerDummyTurningPoint;
    private Map<Vertex, Vertex> nodeToUpperDummyTurningPoint;
    private Map<Port, Port> correspondingPortsAtDummy;
    private Map<Edge, Edge> dummyEdge2RealEdge;
    private List<PortGroup> allWrongBottom;
    private List<PortGroup> allWrongTop;
    //when we move a port temporary to the other side, save old location of that port
    private Map<Port, PortGroup> oldContainerCorrectSide;
    private PortSideAssignment psaInternal;

    public DummyNodeCreation(SugiyamaLayouter sugy) {
        this.sugy = sugy;
        this.dummyNodesLongEdges = new LinkedHashMap<>();
        this.dummyNodesSelfLoops = new LinkedHashMap<>();
        this.dummyTurningNodes = new LinkedHashMap<>();
        this.portOnWrongSideHasEdgeGoingOnTheLeftSideAroundNode = new LinkedHashMap<>();
        this.nodeToLowerDummyTurningPoint = new LinkedHashMap<>();
        this.nodeToUpperDummyTurningPoint = new LinkedHashMap<>();
        this.correspondingPortsAtDummy = new LinkedHashMap<>();
        this.dummyEdge2RealEdge = new LinkedHashMap<>();
        this.allWrongBottom = new ArrayList<>();
        this.allWrongTop = new ArrayList<>();
        this.oldContainerCorrectSide = new LinkedHashMap<>();
        this.psaInternal = new PortSideAssignment(sugy);
    }

    public void assignWrongSidePortsTemporaryToOtherSide() {
        for (Vertex node : new ArrayList<>(sugy.getGraph().getVertices())) {
            List<Port> bottomPortOrder = sugy.getOrders().getBottomPortOrder().get(node);
            List<Port> topPortOrder = sugy.getOrders().getTopPortOrder().get(node);


            List<Port> wrongBottomPorts = new ArrayList<>();
            for (Port bottomPort : bottomPortOrder) {
                for (Edge edge : bottomPort.getEdges()) {
                    //check if edge points upwards -> if yes move it
                    if (sugy.getStartNode(edge).equals(node)) {
                        oldContainerCorrectSide.put(bottomPort, bottomPort.getPortGroup());
                        wrongBottomPorts.add(bottomPort);
                    }
                }
            }
            List<Port> wrongTopPorts = new ArrayList<>();
            for (Port topPort : topPortOrder) {
                for (Edge edge : topPort.getEdges()) {
                    //check if edge points downwards -> if yes move it
                    if (!sugy.getStartNode(edge).equals(node)) {
                        oldContainerCorrectSide.put(topPort, topPort.getPortGroup());
                        wrongTopPorts.add(topPort);
                    }
                }
            }

            if (!wrongBottomPorts.isEmpty()) {
                reassignPortCompositions(node, Orientation.NORTH, topPortOrder, bottomPortOrder, wrongBottomPorts,
                        this.allWrongBottom);
            }
            if (!wrongTopPorts.isEmpty()) {
                reassignPortCompositions(node, Orientation.SOUTH, bottomPortOrder, topPortOrder, wrongTopPorts,
                        this.allWrongTop);
            }
        }
    }

    public void undoAssigningPortsTemporaryToOtherSide() {
        for (PortGroup wrongBottom : this.allWrongBottom) {
            removePortGroupForWrongSidePorts(wrongBottom, false);
        }
        for (PortGroup wrongTop : this.allWrongTop) {
            removePortGroupForWrongSidePorts(wrongTop, true);
        }
        //clean up
        this.allWrongBottom.clear();
        this.allWrongTop.clear();
        this.oldContainerCorrectSide.clear();
    }

    public void createDummyNodesForEdges() {
        createDummyNodesForEdges(true);
        psaInternal.assignPortsToVertexSides(dummyNodesLongEdges.keySet());
        sugy.changeRanksAccordingToSortingOrder();
    }

    /**
     *
     * @return
     *      Mapping of dummy vertices to edges
     */
    public DummyCreationResult createAllDummyNodes() {

        // dummy nodes on new intermediate layers
        createTurningDummiesAndSelfLoopDummies();

        sugy.changeRanksAccordingToSortingOrder();

        // create dummy nodes for each edge passing a layer
        createDummyNodesForEdges(false);


        // assign ports to vertex sides for dummy vertices
        psaInternal.assignPortsToVertexSides(dummyTurningNodes.keySet());
        psaInternal.assignPortsToVertexSides(dummyNodesSelfLoops.keySet());
        psaInternal.assignPortsToVertexSides(dummyNodesLongEdges.keySet()); //todo part of them has already been
        // assigned when calling createDummyNodesForEdges() earlier. To save time we may skip them later(?)

        sugy.changeRanksAccordingToSortingOrder();

        //TODO: insert new ranks and nodes in node order (that was previously done in CrossingMinimization)

        return new DummyCreationResult(dummyNodesLongEdges, dummyNodesSelfLoops, dummyTurningNodes,
                nodeToLowerDummyTurningPoint, nodeToUpperDummyTurningPoint, correspondingPortsAtDummy,
                dummyEdge2RealEdge);
    }

    private void reassignPortCompositions(Vertex node, Orientation consideredSide, List<Port> consideredSidePortOrder,
                                          List<Port> wrongPortOrder, List<Port> wrongSidePorts,
                                          List<PortGroup> allWrongSide) {
        //now make new port groups: one new super group for all port compositions on the considered side, also
        // containing a new port group for all ports that are assigned to the "wrong" side (i.e. allWrongSide)
        PortGroup allConsideredSidePCs = new PortGroup();
        for (PortComposition portComposition : new ArrayList<>(node.getPortCompositions())) {
            List<Port> containedPorts = PortUtils.getPortsRecursively(portComposition);
            if (!containedPorts.isEmpty() && containedPorts.get(0).getOrientationAtVertex() == consideredSide) {
                allConsideredSidePCs.addPortComposition(portComposition);
            }
        }
        //now add the ports from the "wrong" side to that new port group
        PortGroup wrongSide = new PortGroup();
        allWrongSide.add(wrongSide);
        allConsideredSidePCs.addPortComposition(wrongSide);
        for (Port wrongBottomPort : wrongSidePorts) {
            wrongSide.addPortComposition(wrongBottomPort);
            changeNodeSidePortList(wrongBottomPort, wrongPortOrder, consideredSidePortOrder);
        }
        node.addPortComposition(allConsideredSidePCs);
    }

    private void changeNodeSidePortList(Port port, List<Port> removeFrom, List<Port> moveTo) {
        removeFrom.remove(port); //todo this is linear. speed up if necessary
        moveTo.add(port);
    }

    private void removePortGroupForWrongSidePorts(PortGroup wrongSidePortGroup, boolean moveFromBottomToTop) {
        Vertex node = wrongSidePortGroup.getVertex();
        List<Port> topPortOrder = sugy.getOrders().getTopPortOrder().get(node);
        List<Port> bottomPortOrder = sugy.getOrders().getBottomPortOrder().get(node);
        List<Port> oldWrongSidePortOrder = moveFromBottomToTop ? bottomPortOrder : topPortOrder;
        List<Port> newCorrectSidePortOrder = moveFromBottomToTop ? topPortOrder : bottomPortOrder;
        //determine if wrongSidePortGroup is on the left or the right side of its node and save this info
        boolean isLeft = PortUtils.containsPortRecursively(wrongSidePortGroup, oldWrongSidePortOrder.get(0));
        for (Port port : PortUtils.getPortsRecursively(wrongSidePortGroup)) {
            this.portOnWrongSideHasEdgeGoingOnTheLeftSideAroundNode.put(port, isLeft);
        }
        //remove dummy super group and transfer its children directly to the vertex
        PortGroup superGroup = wrongSidePortGroup.getPortGroup();
        PortUtils.movePortCompositionsToPortGroup(superGroup.getPortCompositions(), null);
        for (PortComposition port : new ArrayList<>(wrongSidePortGroup.getPortCompositions())) {
            PortGroup oldContainer = this.oldContainerCorrectSide.get(port);
            if (oldContainer == null) {
                wrongSidePortGroup.removePortComposition(port);
                node.addPortComposition(port);
            } else {
                oldContainer.addPortComposition(port);
            }
            changeNodeSidePortList((Port) port, oldWrongSidePortOrder, newCorrectSidePortOrder);
        }
        node.removePortComposition(superGroup);
        node.removePortComposition(wrongSidePortGroup);
    }

    private void createTurningDummiesAndSelfLoopDummies() {
        //go through all nodes on all layers in order and create for them dummy turning nodes in order on
        // intermediate layer. We only really create the intermediate layers we need
        List<Vertex> dummyFirstLayer = new ArrayList<>(); //we use this dummy layer so that we can insert a new lower
        // intermediate layer BEFORE the first actual layer. We will remove this dummy layer in the end
        sugy.getOrders().getNodeOrder().add(0, dummyFirstLayer);
        ListIterator<List<Vertex>> layerIterator = sugy.getOrders().getNodeOrder().listIterator();
        List<Vertex> lowerIntermediateLayer = null;
        List<Vertex> upperIntermediateLayer = null;
        while (layerIterator.hasNext()) {
            List<Vertex> layer = layerIterator.next();

            lowerIntermediateLayer = upperIntermediateLayer;
            upperIntermediateLayer = null;

            for (Vertex node : layer) {
                Set<Edge> loopEdgesOfNode = sugy.getLoopEdges().get(node) == null ? new LinkedHashSet<>() :
                        new LinkedHashSet<>(sugy.getLoopEdges().get(node));
                for (Port bottomPort : sugy.getOrders().getBottomPortOrder().get(node)) {

                    //lower turning dummies
                    for (Edge edge : bottomPort.getEdges()) {
                        //check if edge points upwards -> if yes insert turning dummy
                        if (sugy.getStartNode(edge).equals(node)) {

                            //add intermediate layer to sorting order if not yet there
                            lowerIntermediateLayer =
                                    initLowerIntermediateLayerIfNotDoneYet(lowerIntermediateLayer,
                                            upperIntermediateLayer, layerIterator);

                            //create turning dummy vertex on intermediate layer
                            Vertex lowerDummyTurningNode = getDummyTurningNodeForVertex(node, true,
                                    lowerIntermediateLayer);
                            splitEdgeByTurningDummyNode(edge, lowerDummyTurningNode, true);
                        }
                    }

                    //lower self loop dummies
                    for (Iterator<Edge> loopEdgeIterator = loopEdgesOfNode.iterator(); loopEdgeIterator.hasNext(); ) {
                        Edge loopEdge = loopEdgeIterator.next();
                        List<Port> portsOfLoopEdge = sugy.getPortsOfLoopEdge(loopEdge);
                        if (portsOfLoopEdge.contains(bottomPort)) {
                            lowerIntermediateLayer =
                                    initLowerIntermediateLayerIfNotDoneYet(lowerIntermediateLayer,
                                            upperIntermediateLayer, layerIterator);
                            if (portsOfLoopEdge.get(0).getOrientationAtVertex() == Orientation.NORTH ||
                                    portsOfLoopEdge.get(1).getOrientationAtVertex() == Orientation.NORTH) {
                                upperIntermediateLayer =
                                        initUpperIntermediateLayerIfNotDoneYet(upperIntermediateLayer, layerIterator);
                            }
                            createSelfLoopDummyNodes(loopEdge, lowerIntermediateLayer, upperIntermediateLayer);
                            loopEdgeIterator.remove();
                        }
                    }
                }
                for (Port topPort : sugy.getOrders().getTopPortOrder().get(node)) {

                    //upper turning dummies
                    for (Edge edge : topPort.getEdges()) {
                        //check if edge points downwards -> if yes insert turning dummy
                        if (!sugy.getStartNode(edge).equals(node)) {

                            //add intermediate layer to sorting order if not yet there
                            upperIntermediateLayer =
                                    initUpperIntermediateLayerIfNotDoneYet(upperIntermediateLayer, layerIterator);

                            //create turning dummy vertex on intermediate layer
                            Vertex upperDummyTurningNode = getDummyTurningNodeForVertex(node, false,
                                    upperIntermediateLayer);
                            splitEdgeByTurningDummyNode(edge, upperDummyTurningNode, false);
                        }
                    }

                    //upper self loop dummies
                    for (Iterator<Edge> loopEdgeIterator = loopEdgesOfNode.iterator(); loopEdgeIterator.hasNext(); ) {
                        Edge loopEdge = loopEdgeIterator.next();
                        List<Port> portsOfLoopEdge = sugy.getPortsOfLoopEdge(loopEdge);
                        if (portsOfLoopEdge.contains(topPort)) {
                            upperIntermediateLayer =
                                    initUpperIntermediateLayerIfNotDoneYet(upperIntermediateLayer, layerIterator);
                            //if it was a self loop on both sides of the node, we would have already handled it when
                            // considering the bottom side ports
                            createSelfLoopDummyNodes(loopEdge, lowerIntermediateLayer, upperIntermediateLayer);
                            loopEdgeIterator.remove();
                        }
                    }
                }
            }
        }

        sugy.getOrders().getNodeOrder().remove(dummyFirstLayer);
    }

    private List<Vertex> initUpperIntermediateLayerIfNotDoneYet(List<Vertex> upperIntermediateLayer,
                                                                ListIterator<List<Vertex>> layerIterator) {
        if (upperIntermediateLayer == null) {
            upperIntermediateLayer = new ArrayList<>();
            layerIterator.add(upperIntermediateLayer);
        }
        return upperIntermediateLayer;
    }

    private static List<Vertex> initLowerIntermediateLayerIfNotDoneYet(List<Vertex> lowerIntermediateLayer,
                                                                       List<Vertex> upperIntermediateLayer,
                                                                       ListIterator<List<Vertex>> layerIterator) {
        if (lowerIntermediateLayer == null) {
            lowerIntermediateLayer = new ArrayList<>();
            //if a new upperIntermediateLayer has been created, we need jump over it
            if (upperIntermediateLayer != null) {
                layerIterator.previous();
            }
            layerIterator.previous(); //this is just the current layer CL (the same as by the last call of .next())
            layerIterator.previous(); //this is the one before CL -- behind this we add the new layer
            layerIterator.next(); //once again the same (for moving the pointer aka cursor)
            layerIterator.add(lowerIntermediateLayer); //the new element is added *before* CL
            layerIterator.next(); //returns CL (called to move the pointer) -- now we are back to where we came from
            // unless we have an upperIntermediateLayer
            //if a new upperIntermediateLayer has been created, we need jump over it
            if (upperIntermediateLayer != null) {
                layerIterator.next();
            }
        }
        return lowerIntermediateLayer;
    }

    private void createSelfLoopDummyNodes(Edge loopEdge, List<Vertex> lowerIntermediateLayer,
                                          List<Vertex> upperIntermediateLayer) {
        //we have split the hyperedges -> there are precisely 2 ports per edge
        List<Port> ports = sugy.getPortsOfLoopEdge(loopEdge);
        Vertex vertex = ports.get(0).getVertex();
        boolean port0TopSide = sugy.getOrders().getTopPortOrder().get(vertex).contains(ports.get(0));
        boolean port1TopSide = sugy.getOrders().getTopPortOrder().get(vertex).contains(ports.get(1));

        Port dummyPort0 = new Port();
        Port dummyPort1 = new Port();
        Vertex dummy = new Vertex(Arrays.asList(dummyPort0, dummyPort1), Collections.singleton(new TextLabel(
                "selfLoopDummyFor_" + loopEdge)));

        // add everything to graph and rank dummy
        sugy.getGraph().addVertex(dummy);
        if (port0TopSide) {
            upperIntermediateLayer.add(dummy);
        } else {
            lowerIntermediateLayer.add(dummy);
        }
        dummyNodesSelfLoops.put(dummy, loopEdge);
        correspondingPortsAtDummy.put(dummyPort0, dummyPort1);
        correspondingPortsAtDummy.put(dummyPort1, dummyPort0);

        //add new connections
        int counter = 0;
        Edge dummyEdge0 = new Edge(Arrays.asList(ports.get(0), dummyPort0), Collections.singleton(new TextLabel(
                "selfLoopEdge_" + loopEdge + "_#" + counter++)));
        Edge dummyEdge1 = new Edge(Arrays.asList(ports.get(1), dummyPort1), Collections.singleton(new TextLabel(
                "selfLoopEdge_" + loopEdge + "_#" + counter++)));
        sugy.getGraph().addEdge(dummyEdge0);
        sugy.getGraph().addEdge(dummyEdge1);
        sugy.assignDirection(dummyEdge0, port0TopSide ? vertex : dummy, port0TopSide ? dummy : vertex);
        sugy.assignDirection(dummyEdge1, port0TopSide ? vertex : dummy, port0TopSide ? dummy : vertex);
        sugy.getDummyEdge2RealEdge().put(dummyEdge0, loopEdge);
        sugy.getDummyEdge2RealEdge().put(dummyEdge1, loopEdge);

        //if the ports are on different sides, we need more than one dummy node to route the self loop
        if (port0TopSide != port1TopSide) {
            Port dummyPort2 = new Port();
            Port dummyPort3 = new Port();
            Vertex additionalDummy = new Vertex(Arrays.asList(dummyPort2, dummyPort3),
                    Collections.singleton(new TextLabel(
                            "additionalSelfLoopDummyFor_" + loopEdge)));

            // add everything to graph and rank dummy
            sugy.getGraph().addVertex(additionalDummy);
            if (port1TopSide) {
                upperIntermediateLayer.add(additionalDummy);
            } else {
                lowerIntermediateLayer.add(additionalDummy);
            }
            dummyNodesSelfLoops.put(additionalDummy, loopEdge);
            correspondingPortsAtDummy.put(dummyPort2, dummyPort3);
            correspondingPortsAtDummy.put(dummyPort3, dummyPort2);

            //change and add connections
            dummyEdge1.removePort(ports.get(1));
            dummyEdge1.addPort(dummyPort2);
            sugy.removeDirection(dummyEdge1);
            sugy.assignDirection(dummyEdge1,
                    port0TopSide ? additionalDummy : dummy, port0TopSide ? dummy : additionalDummy);
            Edge dummyEdge2 = new Edge(Arrays.asList(ports.get(1), dummyPort3), Collections.singleton(new TextLabel(
                    "selfLoopEdge_" + loopEdge + "_#" + counter++)));
            sugy.getGraph().addEdge(dummyEdge2);
            sugy.assignDirection(dummyEdge2,
                    port1TopSide ? vertex : additionalDummy, port0TopSide ? additionalDummy : vertex);
            sugy.getDummyEdge2RealEdge().put(dummyEdge2, loopEdge);
        }
    }

    private Vertex getDummyTurningNodeForVertex(Vertex vertex, boolean lowerTurningPoint,
                                                List<Vertex> intermediateLayerToAddItIfCreated) {
        if (lowerTurningPoint && nodeToLowerDummyTurningPoint.get(vertex) != null) {
            return nodeToLowerDummyTurningPoint.get(vertex);
        }
        else if (!lowerTurningPoint && nodeToUpperDummyTurningPoint.get(vertex) != null) {
            return nodeToUpperDummyTurningPoint.get(vertex);
        }

        // create dummyNode and ID
        Vertex dummy = new Vertex();
        String place = lowerTurningPoint ? "lower" : "upper";
        Label idDummy =
                new TextLabel(place + "_turning_dummy_for_" + vertex);
        dummy.getLabelManager().addLabel(idDummy);
        dummy.getLabelManager().setMainLabel(idDummy);

        // add everything to graph
        sugy.getGraph().addVertex(dummy);
        intermediateLayerToAddItIfCreated.add(dummy);
        dummyTurningNodes.put(dummy, vertex);
        if (lowerTurningPoint) {
            nodeToLowerDummyTurningPoint.put(vertex, dummy);
        }
        else {
            nodeToUpperDummyTurningPoint.put(vertex, dummy);
        }

        return dummy;
    }

    private void splitEdgeByTurningDummyNode(Edge edge, Vertex dummy, boolean lowerTurningPoint) {
        // create new Ports and Edges to replace edge
        ArrayList<Port> portsFor1 = new ArrayList<>();
        ArrayList<Port> portsFor2 = new ArrayList<>();
        Port p1 = new Port();
        Port p2 = new Port();
        Label idp1 = new TextLabel("DummyPort_to_" + edge.getPorts().get(0));
        Label idp2 = new TextLabel("DummyPort_to_" + edge.getPorts().get(1));
        p1.getLabelManager().addLabel(idp1);
        p2.getLabelManager().addLabel(idp2);
        p1.getLabelManager().setMainLabel(idp1);
        p2.getLabelManager().setMainLabel(idp2);
        dummy.addPortComposition(p1);
        dummy.addPortComposition(p2);
        correspondingPortsAtDummy.put(p1, p2);
        correspondingPortsAtDummy.put(p2, p1);
        portsFor1.add(p1);
        portsFor2.add(p2);
        portsFor1.add(edge.getPorts().get(0));
        portsFor2.add(edge.getPorts().get(1));
        Edge e1 = new Edge(portsFor1);
        Edge e2 = new Edge(portsFor2);
        Label ide1 = new TextLabel("TurnedEdge" + edge + "#1");
        Label ide2 = new TextLabel("TurnedEdge" + edge + "#2");
        e1.getLabelManager().addLabel(ide1);
        e2.getLabelManager().addLabel(ide2);
        e1.getLabelManager().setMainLabel(ide1);
        e2.getLabelManager().setMainLabel(ide2);
        dummyEdge2RealEdge.put(e1, edge);
        dummyEdge2RealEdge.put(e2, edge);

        // add everything to graph and rank dummy
        sugy.getGraph().addEdge(e1);
        sugy.getGraph().addEdge(e2);

        // delete replaced edge
        edge.removePort(edge.getPorts().get(1));
        edge.removePort(edge.getPorts().get(0));
        sugy.getGraph().removeEdge(edge);
        sugy.removeDirection(edge);

        // assign directions to dummyedges
        if (lowerTurningPoint) {
            sugy.assignDirection(e1, dummy, portsFor1.get(1).getVertex());
            sugy.assignDirection(e2, dummy, portsFor2.get(1).getVertex());
        } else {
            sugy.assignDirection(e1, portsFor1.get(1).getVertex(), dummy);
            sugy.assignDirection(e2, portsFor2.get(1).getVertex(), dummy);
        }
    }

    private void createDummyNodesForEdges(boolean initRanks) {
        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            int dist = (sugy.getRank(sugy.getEndNode(edge))) - (sugy.getRank(sugy.getStartNode(edge)));
            if (dist > 1) {
                createAllDummyNodesForEdge(edge);
            }
        }
    }

    private void createAllDummyNodesForEdge(Edge edge) {
        String edgeName = edge.toString();
        Edge refEdge = edge;
        if (edgeName.startsWith("DummyEdge_to_")) {
            refEdge = dummyEdge2RealEdge.get(edge);
            edgeName = refEdge.toString();
//            if (edge.getPorts().get(0).getVertex().toString().startsWith("Dummy_for_")) {
//                refEdge = dummyNodesLongEdges.get(edge.getPorts().get(0).getVertex());
//                edgeName = refEdge.toString();
//            } else {
//                refEdge = dummyNodesLongEdges.get(edge.getPorts().get(1).getVertex());
//                edgeName = refEdge.toString();
//            }
        }

        // for each layer create a dummynode and connect it with an additional edge
        Vertex lowerNode = sugy.getStartNode(edge);
        Vertex upperNode = sugy.getEndNode(edge);
        Port lowerPort = edge.getPorts().get(0);
        Port upperPort = edge.getPorts().get(1);
        if (!lowerPort.getVertex().equals(lowerNode)) {
            lowerPort = edge.getPorts().get(1);
            upperPort = edge.getPorts().get(0);
        }
        lowerPort.removeEdge(edge);

        int layer;
        for (layer = (sugy.getRank(lowerNode) + 1); layer < sugy.getRank(upperNode); layer++) {
            lowerPort = createDummyNodeForLongEdge(edge, edgeName, refEdge, lowerNode, lowerPort,
                    layer == sugy.getRank(upperNode) - 1 ? upperNode : null, layer);
            lowerNode = lowerPort.getVertex();
        }

        // connect to endnode
        Edge dummyEdge = new Edge(Arrays.asList(upperPort, lowerPort));
        sugy.assignDirection(dummyEdge, lowerNode, upperPort.getVertex());

        // Label/ID
        Label ide = new TextLabel("DummyEdge_for_" + edgeName + "_L_" + (layer-1) + "_to_L_" + layer);
        dummyEdge.getLabelManager().addLabel(ide);
        dummyEdge.getLabelManager().setMainLabel(ide);

        sugy.getGraph().addEdge(dummyEdge);
        dummyEdge2RealEdge.put(dummyEdge, edge);
        sugy.getGraph().removeEdge(edge);
        upperPort.removeEdge(edge);
        sugy.removeDirection(edge);
    }

    /**
     *
     * @param edge
     *      the edge that should be split and is currently in the graph
     * @param edgeName
     * @param refEdge
     *      the same as edge if edge is an orginal one, or the corresponding original edge if edge is a dummy
     * @param lowerNode
     * @param lowerPort
     * @param upperNode
     *      this may be null and only needs to be set if this is the last dummy vertex (uppermost) of this edge to be
     *      created (and if this upperNode is a dummy turning node)
     * @param layer
     *
     * @return
     *      the upper port of the new dummy vertex. You can get this dummy vertex by applying .getVertex() to the
     *      returned port. This returned upper port serves as lowerPort for the next edge part of the long edge
     */
    private Port createDummyNodeForLongEdge(Edge edge, String edgeName, Edge refEdge, Vertex lowerNode,
                                            Port lowerPort, Vertex upperNode, int layer) {
        // create
        Vertex dummy = new Vertex();
        Port lowerDummyPort = new Port();
        Port upperDummyPort = new Port();
        Edge dummyEdge = new Edge(Arrays.asList(lowerDummyPort, lowerPort));
        sugy.assignDirection(dummyEdge, lowerNode, dummy);

        // Label/ID
        Label idn = new TextLabel("DummyV_" + edgeName + "_#" + layer);
        Label idlp = new TextLabel("lDummyPort_" + edgeName + "_#" + layer);
        Label idup = new TextLabel("uDummyPort_" + edgeName + "_#" + layer);
        Label ide = new TextLabel("DummyE_" + edgeName + "_L_" + (layer-1) + "_to_L_" + layer);
        dummy.getLabelManager().addLabel(idn);
        dummy.getLabelManager().setMainLabel(idn);
        lowerDummyPort.getLabelManager().addLabel(idlp);
        lowerDummyPort.getLabelManager().setMainLabel(idlp);
        upperDummyPort.getLabelManager().addLabel(idup);
        upperDummyPort.getLabelManager().setMainLabel(idup);
        dummyEdge.getLabelManager().addLabel(ide);
        dummyEdge.getLabelManager().setMainLabel(ide);

        dummy.addPortComposition(lowerDummyPort);
        dummy.addPortComposition(upperDummyPort);
        sugy.getGraph().addVertex(dummy);
        sugy.getGraph().addEdge(dummyEdge);
        dummyEdge2RealEdge.put(dummyEdge, edge);
        dummyNodesLongEdges.put(dummy, refEdge);

        //we have to insert it on the correct position of the node ordering.
        //we only care if we are adjacent to a turning dummy
        int insertionIndexFromBelow = -1;
        int insertionIndexFromAbove = -1;
        if (isLowerTurningDummy(lowerNode)) {
            Vertex baseNode = dummyTurningNodes.get(lowerNode);
            insertionIndexFromBelow = sugy.getOrders().getNodeOrder().get(layer).indexOf(baseNode)
                    + (isOnTheLeftOfBaseNode(lowerNode) ? 0 : 1);
        }
        if (upperNode != null && isUpperTurningDummy(upperNode)) {
            Vertex baseNode = dummyTurningNodes.get(upperNode);
            insertionIndexFromAbove = sugy.getOrders().getNodeOrder().get(layer).indexOf(baseNode)
                    + (isOnTheLeftOfBaseNode(upperNode) ? 0 : 1);
        }
        if (insertionIndexFromBelow < 0 && insertionIndexFromAbove < 0) {
            sugy.getOrders().getNodeOrder().get(layer).add(dummy);
        }
        else {
            sugy.getOrders().getNodeOrder().get(layer).add(Math.max(insertionIndexFromBelow, insertionIndexFromAbove)
                    , dummy);
        }

        return upperDummyPort;
    }

    private boolean isUpperTurningDummy(Vertex node) {
        Vertex baseNode = this.dummyTurningNodes.get(node);
        return this.nodeToUpperDummyTurningPoint.containsKey(baseNode)
                && this.nodeToUpperDummyTurningPoint.get(baseNode).equals(node);
    }

    private boolean isLowerTurningDummy(Vertex node) {
        Vertex baseNode = this.dummyTurningNodes.get(node);
        return this.nodeToLowerDummyTurningPoint.containsKey(baseNode)
                && this.nodeToLowerDummyTurningPoint.get(baseNode).equals(node);
    }

    private boolean isOnTheLeftOfBaseNode(Vertex turningDummy) {
        Vertex baseNode = this.dummyTurningNodes.get(turningDummy);
        for (Port adjacentPort : PortUtils.getAdjacentPorts(turningDummy)) {
            if (baseNode.getPorts().contains(adjacentPort)) {
                return portOnWrongSideHasEdgeGoingOnTheLeftSideAroundNode.get(adjacentPort);
            }
        }
        System.out.println("This line should never be printed (check in DummyNodeCreation.isOnTheLeftOfBaseNode())");
        return false;
    }
}
