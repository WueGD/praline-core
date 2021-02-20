package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import edu.uci.ics.jung.graph.util.Pair;

import java.util.*;

public class NodePlacement {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private List<List<PortValues>> structure;
    private Map<Port, PortValues> port2portValues;
    private SortingOrder sortingOrder;
    private List<Double> heightOfLayers;
    private Vertex dummyVertex;
    private double layerHeight;
    private Map<Vertex, Set<Port>> dummyPorts;
    private Map<Port, Vertex> dummyPort2unionNode;
    private List<Edge> dummyEdges;
    private int portnumber;
    // spacing variable according to paper:
    private double delta;
    //new max port spacing within a vertex
    private double maxPortSpacing;

    public NodePlacement (SugiyamaLayouter sugy, SortingOrder sortingOrder, DrawingInformation drawingInformation) {
        this.sugy = sugy;
        this.sortingOrder = sortingOrder;
        this.drawInfo = drawingInformation;
    }

    /**
     *
     * @return
     *      Map linking to all dummy ports that were inserted for padding the width of a vertex due to a (long) label.
     *      These ports are still in the data structure and should be removed later.
     */
    public Map<Vertex, Set<Port>> placeNodes () {
        initialize();
        // create lists of ports for layers
        initializeStructure();
        // create dummyPorts to have enough space for labels
        dummyPortsForWidth(false);
        //late initialization of port values - now that we have the overall structure constructed
        initializePortValueParams();

        for (int i = 0; i < 4; i++) {
            switch (i) {
                case 2:
                    for (List<PortValues> order : structure) {
                        Collections.reverse(order);
                    }
                    initializePortValueParams();
                    break;
                case 1:
                    //same as case 3
                case 3:
                    Collections.reverse(structure);
                    initializePortValueParams();
            }
            // initialize datastructure portValues
            resetPortValues();
            // mark conflicts (crossing edges)
            handleCrossings();
            // make compact
            horizontalCompaction();
            //we often don't want arbitrarily broad vertices
            closeRemainingGapsWithinNodes();
            // add to xValues
            switch (i) {
                case 0:
                    //same as case 1
                case 1:
                    for (List<PortValues> portLayer : structure) {
                        for (PortValues portValues : portLayer) {
                            portValues.addToXValues(portValues.getX());
                        }
                    }
                    break;
                case 2:
                    //same as case 3
                case 3:
                    for (List<PortValues> portLayer : structure) {
                        for (PortValues portValues : portLayer) {
                            portValues.addToXValues(-portValues.getX());
                        }
                    }
            }
        }
        // change to positive x-values and align to smallest width
        makePositiveAndAligned();
        //set final x
        for (List<PortValues> portLayer : structure) {
            for (PortValues portValues : portLayer) {
                //find 2 medians of the 4 entries and take their avarage
                List<Double> xValues = portValues.getXValues();
                xValues.sort(Double::compareTo);
                portValues.setX((xValues.get(1) + xValues.get(2)) / 2.0);
            }
        }
        //the medians may still be negative
        makeFinalDrawingPositive();
        // bring back original order
        for (List<PortValues> order : structure) {
            Collections.reverse(order);
            initializePortValueParams();
        }

        reTransformStructure(false);

        return dummyPorts;
    }

    public void initialize() {
        structure = new ArrayList<>();
        port2portValues = new LinkedHashMap<>();
        delta = Math.max(drawInfo.getEdgeDistanceHorizontal() - drawInfo.getPortWidth(), drawInfo.getPortSpacing());
        maxPortSpacing = Math.max(delta, drawInfo.getPortSpacing() * drawInfo.getVertexWidthMaxStretchFactor());
        heightOfLayers = new ArrayList<>();
        dummyPorts = new LinkedHashMap<>();
        dummyPort2unionNode = new LinkedHashMap<>();
        dummyEdges = new LinkedList<>();
        dummyVertex = new Vertex();
        dummyVertex.getLabelManager().addLabel(new TextLabel("dummyVertex"));
        dummyVertex.getLabelManager().setMainLabel(dummyVertex.getLabelManager().getLabels().get(0));
        portnumber = 0;
    }

    public void initializeStructure() {
        layerHeight = drawInfo.getVertexHeight();
        int layer = -1;
        for (List<Vertex> rankNodes : sortingOrder.getNodeOrder()) {
            ++layer;

            heightOfLayers.add(0.0);
            for (Vertex node : rankNodes) {
                heightOfLayers.set(layer, Math.max(heightOfLayers.get(layer), sugy.isDummy(node) ? 0.0 : 1.0));
            }
            List<PortValues> rankBottomPorts = new ArrayList<>();
            List<PortValues> rankTopPorts = new ArrayList<>();
            // crate a List with all bottomPorts and one with all topPorts
            for (Vertex node : rankNodes) {
                if (!sugy.isDummy(node) || sugy.isHyperEdgeDummy(node)) {
                    addDividingNodePair(rankBottomPorts, rankTopPorts);
                }
                for (Port port : sortingOrder.getBottomPortOrder().get(node)) {
                    rankBottomPorts.add(createNewPortValues(port, sugy.getWidthForPort(port)));

                    // add new Edge for each PortPairing
                    if (sugy.isPaired(port)) {
                        List<Port> ports = new ArrayList<>();
                        ports.add(port);
                        ports.add(sugy.getPairedPort(port));
                        dummyEdges.add(new Edge(ports));
                    }

                    // add new Edge if dummy node of a long edge
                    if (sugy.isDummyNodeOfLongEdge(node) && sortingOrder.getBottomPortOrder().get(node).size() == 1) {
                        List<Port> ports = new ArrayList<>();
                        ports.add(port);
                        ports.add(sortingOrder.getTopPortOrder().get(node).get(0));
                        dummyEdges.add(new Edge(ports));
                    }

                }
                for (Port port : sortingOrder.getTopPortOrder().get(node)) {
                    rankTopPorts.add(createNewPortValues(port, sugy.getWidthForPort(port)));
                }
                if (!sugy.isDummy(node) || sugy.isHyperEdgeDummy(node)) {
                    addDividingNodePair(rankBottomPorts, rankTopPorts);
                }
            }
            structure.add(rankBottomPorts);
            structure.add(rankTopPorts);
        }
    }

    private PortValues createNewPortValues(Port port, double width) {
        PortValues portValues = new PortValues(port);
        port2portValues.put(port, portValues);
        portValues.setWidth(width);
        return portValues;
    }

    private void initializePortValueParams() {
        //assign missing indices and neighborings to PortValues (late initialization)
        for (int i = 0; i < structure.size(); i++) {
            List<PortValues> portLayer = structure.get(i);
            for (int j = 0; j < portLayer.size(); j++) {
                portLayer.get(j).lateInit(j > 0 ? portLayer.get(j - 1) : null, i, j);
            }
        }
    }

    public void reTransformStructure(boolean addDummyPortsForPaddingToOrders) {
        // creates shapes for all nodes
        draw(addDummyPortsForPaddingToOrders);
        // remove the dummy edges of port pairings und dummy vertices of multiple-layers-spanning edges
        for (Edge dummy : dummyEdges) {
            for (Port port : new LinkedList<>(dummy.getPorts())) {
                port.removeEdge(dummy);
            }
        }
    }

    private void addDividingNodePair(List<PortValues> rankBottomPorts, List<PortValues> rankTopPorts) {
        Port p1 = new Port();
        Port p2 = new Port();
        createMainLabel(p1);
        createMainLabel(p2);
        List<Port> ports = new ArrayList<>();
        ports.add(p1);
        ports.add(p2);
        new Edge(ports);
        rankBottomPorts.add(createNewPortValues(p1, 0));
        rankTopPorts.add(createNewPortValues(p2, 0));
        dummyVertex.addPortComposition(p1);
        dummyVertex.addPortComposition(p2);
    }

    public Map<Vertex, Set<Port>> dummyPortsForWidth(boolean useMultipleRegularSizeDummyPorts) {
        for (int layer = 0; layer < structure.size(); layer++) {
            List<PortValues> order = new ArrayList<>(structure.get(layer));
            List<PortValues> newOrder = new ArrayList<>();
            if (order.isEmpty()) {
                continue;
            }
            double currentWidth = 0;
            double minWidth = 0;
            double currentWidthUnionNode = 0;
            Vertex currentNode = dummyVertex;
            Vertex currentUnionNode = null;
            int nodePosition = -1;
            for (int position = 0; position < order.size(); position++) {
                PortValues portValues = order.get(position);
                Port port = portValues.getPort();
                Vertex portVertex = port.getVertex();
                if (currentNode.equals(dummyVertex)) {
                    currentNode = portVertex;
                    //special case: if current node is a union node, consider the single parts
                    if (sugy.isUnionNode(currentNode)) {
                        currentUnionNode = currentNode;
                        currentWidthUnionNode = portValues.getWidth();
                        currentNode = sugy.getReplacedPorts().get(port).getVertex();
                    }
                    currentWidth = portValues.getWidth();
                    minWidth = 0;
                    if (currentNode.equals(dummyVertex)) {
                        newOrder.add(portValues);
                    } else {
                        nodePosition = position;
                        if (!sugy.getDeviceVertices().contains(currentNode)) {
                            //we will handle device vertices in the end via the union node
                            minWidth = sugy.getMinWidthForNode(currentNode);
                        }
                    }
                } else if (portVertex.equals(currentNode) || (sugy.getReplacedPorts().containsKey(port) && sugy.
                        getReplacedPorts().get(port).getVertex().equals(currentNode))) {
                    currentWidth += delta + portValues.getWidth();
                    currentWidthUnionNode += delta + portValues.getWidth();
                } else if (portVertex.equals(currentUnionNode)) {
                    //still the same union node but different sub-node
                    currentWidth += delta / 2.0;
                    List<PortValues> nodeOrder = addDummyPortsAndGetNewOrder(order, currentWidth, minWidth,
                            currentUnionNode, currentNode, nodePosition, position, useMultipleRegularSizeDummyPorts);
                    newOrder.addAll(nodeOrder);

                    currentNode = sugy.getReplacedPorts().get(port).getVertex();
                    nodePosition = position;
                    currentWidth = delta / 2.0 + portValues.getWidth();
                    currentWidthUnionNode += delta + portValues.getWidth();
                    minWidth = sugy.getMinWidthForNode(currentNode);
                } else {
                    List<PortValues> nodeOrder = addDummyPortsAndGetNewOrder(order, currentWidth, minWidth,
                            currentUnionNode, currentNode, nodePosition, position, useMultipleRegularSizeDummyPorts);
                    newOrder.addAll(nodeOrder);
                    //special case: if we have passed over a union node check if we need additional width
                    if (currentUnionNode != null) {
                        Vertex deviceVertex = null;
                        VertexGroup vertexGroup = sugy.getVertexGroups().get(currentUnionNode);
                        if (vertexGroup != null) {
                            for (Vertex containedVertex : vertexGroup.getContainedVertices()) {
                                if (sugy.getDeviceVertices().contains(containedVertex)) {
                                    deviceVertex = containedVertex;
                                }
                            }
                        }
                        if (deviceVertex != null) {
                            //TODO: currently we just padd to the right. Maybe make it symmetric later? (low priority)
                            double minWidthUnionNode = sugy.getMinWidthForNode(deviceVertex);

                            while (currentWidthUnionNode < minWidthUnionNode) {
                                Port p = new Port();
                                createMainLabel(p);
                                addToCorrectPortGroupOrNode(p, currentUnionNode, deviceVertex);
                                dummyPorts.putIfAbsent(deviceVertex, new LinkedHashSet<>());
                                dummyPorts.get(deviceVertex).add(p);
                                dummyPort2unionNode.put(p, currentUnionNode);
                                PortValues dummyPortValues = createNewPortValues(p,
                                        useMultipleRegularSizeDummyPorts ?
                                                drawInfo.getPortWidth():
                                                Math.max(0,minWidthUnionNode - currentWidthUnionNode - delta));
                                newOrder.add(dummyPortValues);
                                currentWidthUnionNode += delta + portValues.getWidth();
                            }
                        }
                        currentUnionNode = null;
                    }
                    currentNode = portVertex;
                    if (currentNode.equals(dummyVertex)) {
                        newOrder.add(portValues);
                    } else {
                        nodePosition = position;
                    }
                }

            }
            //maybe the last vertex is still open
            if (currentNode != null && !currentNode.equals(dummyVertex)) {
                List<PortValues> nodeOrder = addDummyPortsAndGetNewOrder(order, currentWidth, minWidth,
                        currentUnionNode, currentNode, nodePosition, order.size(), useMultipleRegularSizeDummyPorts);
                newOrder.addAll(nodeOrder);
            }

            structure.set(layer, newOrder);
        }

        return dummyPorts;
    }

    /**
     *
     * @param order
     * @param currentWidth
     * @param minWidth
     * @param currentUnionNode
     *      may be null
     * @param currentNode
     *      may be not null
     * @param nodePosition
     * @param position
     * @param useMultipleRegularSizeDummyPorts
     * @return
     */
    private List<PortValues> addDummyPortsAndGetNewOrder(List<PortValues> order, double currentWidth, double minWidth,
                                                         Vertex currentUnionNode, Vertex currentNode, int nodePosition,
                                                         int position, boolean useMultipleRegularSizeDummyPorts) {
        if (currentUnionNode == null) {
            currentUnionNode = currentNode;
        }
        LinkedList<PortValues> nodeOrder = new LinkedList<>(order.subList(nodePosition, position));
        double missingWidth = minWidth - currentWidth;

        //add two dummy ports, one to the left and on to the right
        // both have the same size, i.e., missingWidth / 2.0
        boolean left = true;
        while (minWidth > currentWidth) {
            Port p = new Port();
            createMainLabel(p);
            addToCorrectPortGroupOrNode(p, currentUnionNode, currentNode);
            dummyPorts.putIfAbsent(currentNode, new LinkedHashSet<>());
            dummyPorts.get(currentNode).add(p);
            dummyPort2unionNode.put(p, currentUnionNode);

            PortValues dummyPortValues = createNewPortValues(p, useMultipleRegularSizeDummyPorts ?
                    drawInfo.getPortWidth(): Math.max(0, missingWidth / 2.0 - delta));
            if (left) {
                left = false;
                nodeOrder.addFirst(dummyPortValues);
            } else {
                left = true;
                nodeOrder.addLast(dummyPortValues);
            }
            currentWidth += dummyPortValues.getWidth() + delta;
        }


        return nodeOrder;
    }

    private void addToCorrectPortGroupOrNode(Port p, Vertex unionNode, Vertex node) {
        //if this remains null, then we add p only to the node on the top level
        // and not to a specific port group any more
        // special case: if the topLevelPortGroup has no vertex we also do not add it there, e.g. because it's a device
        PortGroup topLevelPortGroup = null;
        List<PortComposition> portCompositions = node.getPortCompositions();
        //if there is a single port group on the top level (there may be multiple stacked) -> find lowest
        // and add p to that port group
        if (!unionNode.equals(node)) {
            topLevelPortGroup = sugy.getOrigVertex2replacePortGroup().get(node);
            portCompositions = topLevelPortGroup.getPortCompositions();
        }
        while (portCompositions.size() == 1 && portCompositions.get(0) instanceof PortGroup) {
            topLevelPortGroup = (PortGroup) portCompositions.get(0);
            portCompositions = topLevelPortGroup.getPortCompositions();
        }
        if (topLevelPortGroup == null || topLevelPortGroup.getVertex() == null) {
            unionNode.addPortComposition(p);
        }
        else {
            topLevelPortGroup.addPortComposition(p);
        }
    }

    private void resetPortValues() {
        for (List<PortValues> portLayer : structure) {
            for (PortValues portValues : portLayer) {
                portValues.resetValues();
            }
        }
    }

    private void handleCrossings() {
        //determine for all long edges, over how many dummy vertices they go, i.e., their length.
        //later, longer edges will be preferred for making them straight compared to shorter edges
        Map<Edge, Integer> lengthOfLongEdge = new LinkedHashMap<>();
        determineLengthOfLongEdges(lengthOfLongEdge);

        for (int layer = 0; layer < (structure.size() - 1); layer++) {
            LinkedList<PortValues[]> stack = new LinkedList<>(); //stack of edges that are made straight
            //an entry of this stack is the 2 end ports of the corresponding edge, the first for the lower, the second
            // for the upper port
            for (PortValues port0 : structure.get(layer)) {
                for (Edge edge : port0.getPort().getEdges()) {
                    PortValues port1 = port2portValues.get(PortUtils.getOtherEndPoint(edge, port0.getPort()));
                    if (port1.getLayer() == (layer + 1)) {
                        PortValues[] stackEntry = {port0, port1};
                        fillStackOfCrossingEdges(stack, stackEntry, new LinkedList<>(), lengthOfLongEdge);
                    }
                }
            }
            // initialize root and align according to Alg. 2 from paper
            verticalAlignment(stack);
        }
    }

    private void determineLengthOfLongEdges(Map<Edge, Integer> lengthOfLongEdge) {
        for (Vertex vertex : sugy.getDummyNodesLongEdges().keySet()) {
            Edge longEdge = sugy.getOriginalEdgeExceptForHyperE(vertex);
            Integer oldValue = lengthOfLongEdge.get(longEdge);
            if (oldValue == null || oldValue == 0) {
                lengthOfLongEdge.put(longEdge, 1);
            }
            else {
                lengthOfLongEdge.replace(longEdge, oldValue + 1);
            }
        }
    }

    private void fillStackOfCrossingEdges(LinkedList<PortValues[]> stack, PortValues[] stackEntry,
                                          LinkedList<PortValues[]> removedStackEntriesPotentiallyToBeReAdded,
                                          Map<Edge, Integer> lengthOfLongEdge) {
        while (true) {
            if (stack.isEmpty()) {
                stack.push(stackEntry);
                break;
            }
            PortValues[] top = stack.pop();
            // if the upper port values of the top edge of the stack and the current edge are increasing, they don't
            // cross (on this layer) and we can keep them both.
            // However we will discard all edges in removedStackEntriesPotentiallyToBeReAdded that were potentially
            // in between them two because they have lower priority than stackEntry and are in conflict with stackEntry
            if (top[1].getPosition() < stackEntry[1].getPosition()) {
                stack.push(top);
                stack.push(stackEntry);
                break;
            }
            // otherwise => crossing => conflict
            else {
                /*
                keep the edge that...

                #1. has greater length (in terms of being a long edge)
                #2. is non-incident to a dummy turning node or a dummy self loop node
                #3. is more left, i.e., already on the stack

                EDIT JZ 2021/02/02: #2 does not help in one or the other direction -> removed it (commented out)

                 */
                //#1.
                int topLength =
                        lengthOfLongEdge.getOrDefault(sugy.getOriginalEdgeExceptForHyperE(top[1].getPort().getVertex()),0);
                int stackEntryLength =
                        lengthOfLongEdge.getOrDefault(sugy.getOriginalEdgeExceptForHyperE(stackEntry[1].getPort().getVertex()),0);
                if (topLength > stackEntryLength) {
                    keepTop(stack, removedStackEntriesPotentiallyToBeReAdded, top);
                    break;
                }
                else if (topLength == stackEntryLength) {
                    //#2.
    //                    boolean topIncidentToTurningPoint =
    //                            sugy.isDummyTurningNode(top[0].getVertex()) ||
    //                            sugy.isDummyNodeOfSelfLoop(top[0].getVertex()) ||
    //                            sugy.isDummyTurningNode(top[1].getVertex()) ||
    //                            sugy.isDummyNodeOfSelfLoop(top[1].getVertex());
    //                    boolean stackEntryIncidentToTurningPoint =
    //                            sugy.isDummyTurningNode(stackEntry[0].getVertex()) ||
    //                            sugy.isDummyNodeOfSelfLoop(stackEntry[0].getVertex()) ||
    //                            sugy.isDummyTurningNode(stackEntry[1].getVertex()) ||
    //                            sugy.isDummyNodeOfSelfLoop(stackEntry[1].getVertex());
    //                    if (!topIncidentToTurningPoint && stackEntryIncidentToTurningPoint) {
    //                        keepTop(stack, removedStackEntriesPotentiallyToBeReAdded, top);
    //                        break;
    //                    }
    //                    //#3.
    //                    else if (topIncidentToTurningPoint == stackEntryIncidentToTurningPoint) {
                        keepTop(stack, removedStackEntriesPotentiallyToBeReAdded, top);
                        break;
//                    }
                }

                //otherwise we discard top and compare the current stackEntry to the next one on top of the stack
                removedStackEntriesPotentiallyToBeReAdded.push(top);
            }
        }
    }

    private void keepTop(LinkedList<PortValues[]> stack,
                         LinkedList<PortValues[]> removedStackEntriesPotentiallyToBeReAdded, PortValues[] top) {
        stack.push(top);
        //re-add the entries we removed in between back to the stack
        while (!removedStackEntriesPotentiallyToBeReAdded.isEmpty()) {
            stack.push(removedStackEntriesPotentiallyToBeReAdded.pop());
        }
    }

    private void verticalAlignment(List<PortValues[]> edges) {
        for (PortValues[] entry : edges) {
            if (entry[1].getAlign() == entry[1]) {
                entry[0].setAlign(entry[1]);
                entry[1].setRoot(entry[0].getRoot());
                entry[1].setAlign(entry[1].getRoot());
            }
        }
    }

    //Alg. 3b (alternative) from Brandes, Walter, Zink - Erratum: Fast and Simple Horizontal Coordinate Assignment
    // https://arxiv.org/abs/2008.01252
    private void horizontalCompaction() {
        // coordinates relative to sink
        //we have to go through the structure with increasing indices in both layers and port indices on layers
        for (List<PortValues> layer : structure) {
            for (PortValues v : layer) {
                if (v.getRoot().equals(v)) {
                    placeBlock(v);
                }
            }
        }
        //class offsets
        List<List<Pair<PortValues>>> neighborings = new ArrayList<>(structure.size());
        for (int i = 0; i < structure.size(); i++) {
            neighborings.add(new ArrayList<>());
        }

        //find all neighborings
        for (List<PortValues> layer : structure) {
            for (int j = layer.size() - 1; j > 0; j--) {
                PortValues vJ = layer.get(j);
                PortValues vJMinus1 = layer.get(j - 1);
                if (!areInTheSameClass(vJMinus1, vJ)) {
                    int layerOfSink = vJMinus1.getSink().getLayer();
                    neighborings.get(layerOfSink).add(new Pair<>(vJMinus1, vJ));
                }
            }
        }

        //apply shift for all neighborings
        for (int i = 0; i < structure.size(); i++) {
            List<PortValues> layer = structure.get(i);
            if (layer.isEmpty()) {
                continue;
            }
            PortValues v1 = layer.get(0);
            PortValues sinkV1 = v1.getSink();
            if (sinkV1.getShift() == Double.POSITIVE_INFINITY) {
                sinkV1.setShift(0);
            }
            for (Pair<PortValues> neighboring : neighborings.get(i)) {
                //load variables involved
                PortValues u = neighboring.getFirst();
                PortValues v = neighboring.getSecond();
                PortValues sinkU = u.getSink();
                PortValues sinkV = v.getSink();

                //apply shift
                sinkU.setShift(Math.min(sinkU.getShift(),
                        sinkV.getShift() + v.getX() - (u.getX() + (u.getWidth() + v.getWidth()) / 2.0 + delta)));
            }
        }

        //absolute coordinates
        for (List<PortValues> layer : structure) {
            for (PortValues v : layer) {
                PortValues sinkV = v.getSink();
                v.setX(v.getX() + sinkV.getShift());
            }
        }
    }

    private void placeBlock(PortValues v) {
        if (v.getX() == Double.NEGATIVE_INFINITY) {
            v.setX(0);
            PortValues w = v;
            do {
                if (w.getPosition() > 0) {
                    PortValues u = w.getPredecessor(); //we consider here the real neighbor and not its root, hereunder
                    // we may explicitly consider u's root then. This is different from the paper to incorporate the
                    // width of every vertex
                    placeBlock(u.getRoot());
                    if (v.getSink().equals(v)) {
                        v.setSink(u.getRoot().getSink());
                    }
                    if (areInTheSameClass(v, u.getRoot())) {
                        v.setX(Math.max(v.getX(),(u.getX() + (u.getWidth() + w.getWidth()) / 2.0 + delta)));
                    }
                }
                w = w.getAlign();
            } while (!w.equals(v));

            // Check for all nodes of this block whether their distance to the prev node in the same class is too large:
            // If the max distance within a vertex becomes greater than allowed (within a vertex counts also if the
            // left port is part of regular vertex and the right one belongs to the boundary of the vertex, i.e., it
            // belongs to dummyVertex), break an alignment.
            // This can only be the case when w and its predecessor are in the same block and have the same sink
            // and they are no dummy vertices.
            do {
                PortValues predW = w.getPredecessor();
                Vertex nodeOfW = dummyPort2unionNode.getOrDefault(w.getPort(), w.getPort().getVertex());
                Vertex nodeOfPredW = predW == null ? null :
                        dummyPort2unionNode.getOrDefault(predW.getPort(), predW.getPort().getVertex());

                if (predW != null && w.getAlign() != w && areInTheSameNonDummyNode(nodeOfW, nodeOfPredW) &&
                        areInTheSameClass(v, predW.getRoot()) &&
                        v.getX() - predW.getX() - (v.getWidth() + predW.getWidth()) / 2.0 > maxPortSpacing) {
                    //remove alignments
                    //usually we cut to the top of u, but when it is the first of its block, i.e., v, or if it has a
                    // port paring to to the top, then we cut to the bottom
                    PortValues alignW = w.getAlign();
                    boolean isPairedToTop = sugy.isPaired(w.getPort()) &&
                            sugy.getPairedPort(w.getPort()).equals(w.getAlignRe().getPort());
                    boolean isPairedToBottom = sugy.isPaired(w.getPort()) &&
                            sugy.getPairedPort(w.getPort()).equals(w.getAlign().getPort());
                    boolean cutBelow = isPairedToTop || w.equals(v);

                    //cut alignment below or above w
                    //if w == v and is paired to top or
                    //if we want to cut below but there is nothing below -> leave as is
                    if ( ! ((w.equals(v) && isPairedToBottom) || (cutBelow && w.getAlign().equals(v)))) {
                        if (cutBelow) {
                            removeAlignment(w, false);
                        } else {
                            //cut alignment above w
                            removeAlignment(w, true);
                        }
                        //re-start process for both parts -> the old root v (which is now the root of a smaller
                        // block) and the new root (which becomes now the root of a block)
                        v.setX(Double.NEGATIVE_INFINITY);
                        placeBlock(v); //for v again
                        if (cutBelow) {
                            placeBlock(alignW); //for the new root below v
                        } else {
                            placeBlock(w); //everything above w is fine
                        }

                        //do not continue
                        return;
                    }
                }
                w = w.getAlign();
            } while (!w.equals(v));

            //align the whole block
            while (!w.getAlign().equals(v)) {
                w = w.getAlign();
                w.setX(v.getX());
                w.setSink(v.getSink());
            }
        }
    }

    private boolean areInTheSameClass(PortValues v, PortValues u) {
        return v.getSink().equals(u.getSink());
    }

    private boolean areInTheSameNonDummyNode(Vertex nodeV, Vertex nodeU) {
        //check if the corresponding vertex in sugy is a dummy node. This should not be confused with dummyVertex in
        // this class, which is introduced for ports of dividing pairs that define the boundaries of vertices
        if (sugy.isDummy(nodeV) || sugy.isDummy(nodeU)) {
            return false;
        }
        //equals dummyVertex in this class means equals boundary. The gap between 2 boundaries, as in the following
        // check should not be limited, in particular it is in different vertices
        if (nodeV.equals(dummyVertex) && nodeU.equals(dummyVertex)) {
            return false;
        }
        //they are either in the same node or in a node and one (!) boundary. Hence this boundary is of this node and
        // we can return true
        return nodeV.equals(nodeU) || nodeV.equals(dummyVertex) || nodeU.equals(dummyVertex);
    }

    private void removeAlignment(PortValues w, boolean removeAlignmentReToTop) {
        PortValues oldRoot = w.getRoot();
        PortValues newRoot;
        if (removeAlignmentReToTop) {
            newRoot = w;
            w.getAlignRe().setAlign(oldRoot);
        } else {
            newRoot = w.getAlign();
            w.setAlign(oldRoot);
        }
        PortValues newSink = newRoot.getPredecessor() == null ? newRoot : newRoot.getPredecessor().getRoot().getSink();
        PortValues u = newRoot;
        PortValues lowest;
        do {
            u.setRoot(newRoot);
            u.setSink(newSink);
            lowest = u;
            u = u.getAlign();
        } while (!u.equals(oldRoot));
        lowest.setAlign(newRoot);
    }

    private void closeRemainingGapsWithinNodes() {
        //post processing: if vertices of the same vertex in different classes have distance greater than specified for
        // the vertex stretch, then we "transfer" these ports to the class on the right
        //for this, we again go through the structure and "pull" the ports of the same vertex to the right

        for (List<PortValues> layer : structure) {
            for (int j = 1; j < layer.size(); j++) {
                //load variables involved
                PortValues u = layer.get(j - 1);
                PortValues v = layer.get(j);
                Vertex nodeOfU = dummyPort2unionNode.getOrDefault(u.getPort(), u.getPort().getVertex());
                Vertex nodeOfV = dummyPort2unionNode.getOrDefault(v.getPort(), v.getPort().getVertex());
                //also align it to the right border, i.e., v belongs to the
                if (areInTheSameNonDummyNode(nodeOfV, nodeOfU)
                        && v.getX() - u.getX() - (v.getWidth() + u.getWidth()) / 2.0 > maxPortSpacing) {
                    moveToTheRight(u, nodeOfU);
                }
            }
        }
    }

    private void moveToTheRight(PortValues u, Vertex nodeOfU) {
        List<PortValues> portsToBeMoved = new ArrayList<>(2);
        portsToBeMoved.add(u);

        //we must also move a port potentially paired with u
        Port portU = u.getPort();
        if (sugy.isPaired(portU)) {
            Port pairedPort = sugy.getPairedPort(portU);
            if (u.getAlignRe().getPort().equals(pairedPort)) {
                portsToBeMoved.add(u.getAlignRe());
            } else if (u.getAlign().getPort().equals(pairedPort)) {
                portsToBeMoved.add(u.getAlign());
            } else {
                System.out.println("Warning! Found a paired port that is not aligned to its partner. This should " +
                        "never happen.");
            }
        }

        //determine movement to the right
        double moveValue = Double.POSITIVE_INFINITY;
        for (PortValues v : portsToBeMoved) {
            double freeSpaceToTheRight = v.getSuccessor() == null ? Double.POSITIVE_INFINITY :
                    v.getSuccessor().getX() - v.getX();
            //we need to leave at least delta distance between neighborings -> also subtract width and delta once
            moveValue = Math.min(moveValue, freeSpaceToTheRight - v.getWidth() - delta);
        }

        //if we can't move -> abort
        if (moveValue <= 0) {
            return;
        }

        //do actual shift
        for (PortValues v : portsToBeMoved) {
            v.setX(v.getX() + moveValue);
        }

        //continue this process to the right as long as it's the same vertex
        for (PortValues v : portsToBeMoved) {
            PortValues predU = u.getPredecessor();
            Vertex nodeOfPredU = predU == null ? null :
                    dummyPort2unionNode.getOrDefault(predU.getPort(), predU.getPort().getVertex());
            if (predU != null && nodeOfU.equals(nodeOfPredU)
                    && u.getX() - predU.getX() - (u.getWidth() + predU.getWidth()) / 2.0 > maxPortSpacing) {
                moveToTheRight(predU, nodeOfPredU);
            }
        }
    }

    private void makePositiveAndAligned() {
        //find min and max x for each round
        List<Double> minX = new ArrayList<>(4);
        List<Double> maxX = new ArrayList<>(4);
        for (int iteration = 0; iteration < 4; iteration++) {
            double currMinX = Double.POSITIVE_INFINITY;
            double currMaxX = Double.NEGATIVE_INFINITY;
            for (List<PortValues> portLayer : structure) {
                for (PortValues portValues : portLayer) {
                    currMinX = Math.min(currMinX, portValues.getXValues().get(iteration));
                    currMaxX = Math.max(currMaxX, portValues.getXValues().get(iteration));
                }
            }
            minX.add(currMinX);
            maxX.add(currMaxX);
        }

        //determine width for each round
        List<Double> width = new ArrayList<>(4);
        //find run with smallest width
        double smallestWidth = Double.POSITIVE_INFINITY;
        int indexBestRun = -1; //best means smallest here
        for (int iteration = 0; iteration < 4; iteration++) {
            double widthThisRun = maxX.get(iteration) - minX.get(iteration);
            width.add(widthThisRun);
            if (widthThisRun < smallestWidth) {
                smallestWidth = widthThisRun;
                indexBestRun = iteration;
            }
        }

        //make best run positive
        double shiftBestRun = - minX.get(indexBestRun);
        minX.set(indexBestRun, minX.get(indexBestRun) + shiftBestRun);
        maxX.set(indexBestRun, maxX.get(indexBestRun) + shiftBestRun);
        shiftXValuesOfPortValues(indexBestRun, shiftBestRun);

        //align the other runs to the best run
        for (int iteration = 0; iteration < 4; iteration++) {
            if (iteration != indexBestRun) {
                double shift = iteration < 2  ? minX.get(indexBestRun) - minX.get(iteration) :
                        maxX.get(indexBestRun) - maxX.get(iteration);
                shiftXValuesOfPortValues(iteration, shift);
            }
        }
    }

    private void makeFinalDrawingPositive() {
        //find min
        double minX = Double.POSITIVE_INFINITY;
        for (List<PortValues> portLayer : structure) {
            for (PortValues portValues : portLayer) {
                minX = Math.min(minX, portValues.getX());
            }
        }
        //determine shift and apply it
        double shift = -minX;
        shiftXValuesOfPortValues(-1, shift);
    }

    /**
     * iteration = -1 to change the value of .getX()
     */
    private void shiftXValuesOfPortValues(int iteration, double shift) {
        for (List<PortValues> portLayer : structure) {
            for (PortValues portValues : portLayer) {
                if (iteration == -1) {
                    portValues.setX(portValues.getX() + shift);
                } else {
                    portValues.getXValues().set(iteration, portValues.getXValues().get(iteration) + shift);
                }
            }
        }
    }

    private void draw(boolean addDummyPortsForPaddingToOrders) {
        double currentY = drawInfo.getPortHeight();
        double yPos = currentY;
        for (int layerIndex = 0; layerIndex < structure.size(); layerIndex++) {
            List<PortValues> layer = structure.get(layerIndex);
            if (!layer.isEmpty()) {
                Vertex nodeInTheGraph = null;
                int portIndexAtVertex = 0;
                // x1, y1, x2, y2
                // initialize shape of first node
                double xPos = addDummyPortsForPaddingToOrders ? 0.0 : layer.get(0).getX();
                PortValues portValues = null;
                for (int pos = 0; pos < layer.size(); pos++) {
                    portValues = layer.get(pos);
                    Port port = portValues.getPort();
                    Vertex portVertex = port.getVertex();
                    if (portVertex.equals(dummyVertex) || nodeInTheGraph == null ||
                            !nodeInTheGraph.equals(portVertex)) {
                        portIndexAtVertex = 0;
                        if (nodeInTheGraph != null) {
                            if (!addDummyPortsForPaddingToOrders &&
                                    (nodeInTheGraph.getShape() == null || isNanShape(nodeInTheGraph.getShape()))) {
                                // one node done - create Rectangle
                                createNodeShape(layerIndex, nodeInTheGraph, xPos, yPos, portValues);
                            }
                            nodeInTheGraph = portVertex.equals(dummyVertex) ? null : portVertex;
                            xPos = portValues.getX();
                        } else {
                            nodeInTheGraph = portVertex;
                            if (nodeInTheGraph.equals(dummyVertex)) {
                                xPos = portValues.getX();
                                if (pos + 1 < layer.size()) {
                                    nodeInTheGraph = layer.get(pos + 1).getPort().getVertex();
                                    //if it is still dummyVertex, there are no regular ports -> use the alignment and take
                                    // the entry from the other side
                                    if (nodeInTheGraph.equals(dummyVertex)) {
                                        PortValues realPortOnOtherSideOfVertex = portValues.getAlign().getSuccessor();
                                        if (realPortOnOtherSideOfVertex != null) {
                                            nodeInTheGraph = realPortOnOtherSideOfVertex.getPort().getVertex();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!portVertex.equals(dummyVertex)) {
                        createPortShape(currentY, portValues, layerIndex % 2 == 0, nodeInTheGraph, portIndexAtVertex,
                                addDummyPortsForPaddingToOrders);
                        ++portIndexAtVertex;
                    }
                }
                //for the last we may still need to create a node shape
                if (nodeInTheGraph != null && !addDummyPortsForPaddingToOrders &&
                            (nodeInTheGraph.getShape() == null || isNanShape(nodeInTheGraph.getShape()))) {
                    createNodeShape(layerIndex, nodeInTheGraph, xPos, yPos, portValues);
                }
            }

            if (layerIndex % 2 == 0) {
                currentY += heightOfLayers.get(layerIndex / 2) * layerHeight +
                        Math.min(1.0, heightOfLayers.get(layerIndex / 2)) * 2.0 * drawInfo.getBorderWidth();
            } else {
                currentY += ((2 * drawInfo.getPortHeight()) + drawInfo.getDistanceBetweenLayers());
                yPos = currentY;
            }
        }
    }

    private boolean isNanShape(Shape shape) {
        if (shape instanceof Rectangle) {
            return Double.isNaN(shape.getXPosition()) || Double.isNaN(shape.getYPosition())
                    || Double.isNaN(((Rectangle) shape).getWidth())
                    || Double.isNaN(((Rectangle) shape).getHeight());
        }
        return false;
    }

    private void createNodeShape(int layerIndex, Vertex nodeInTheGraph, double xPos, double yPos,
                                 PortValues portValues) {
        double width = portValues.getX() - (portValues.getWidth() + delta) / 2.0 - xPos;
        double height = heightOfLayers.get(layerIndex / 2) * layerHeight +
                Math.min(1.0, heightOfLayers.get(layerIndex / 2)) * 2.0 * drawInfo.getBorderWidth();
        Rectangle nodeShape = new Rectangle(xPos, yPos, width, height, null);
        nodeInTheGraph.setShape(nodeShape);
    }

    private Vertex createPortShape(double currentY, PortValues portValues, boolean isBottomSide, Vertex nodeInTheGraph,
                                   int portIndexAtNode, boolean addDummyPortsForPaddingToOrders) {
        Port port = portValues.getPort();
        if (!port.getVertex().equals(nodeInTheGraph)) {
            nodeInTheGraph = port.getVertex();
            portIndexAtNode = 0;
        }

        if (!addDummyPortsForPaddingToOrders) {
            Rectangle portShape = new Rectangle(portValues.getX(), currentY, portValues.getWidth(),
                    drawInfo.getPortHeight(), null);
            port.setShape(portShape);
        }

        List<Port> relevantPortOrdering = isBottomSide ? sugy.getOrders().getBottomPortOrder().get(nodeInTheGraph) :
                sugy.getOrders().getTopPortOrder().get(nodeInTheGraph);
        //todo: currently we check the complete list via contains. if necessary speed up by checking at the correct
        // place within the list .get(index).equals(port)
        // I would have expected this index to be portIndexAtNode, but somehow that does not see to work
        if (addDummyPortsForPaddingToOrders && !relevantPortOrdering.contains(port)) {
            relevantPortOrdering.add(portIndexAtNode, port);
            port.setOrientationAtVertex(isBottomSide ? Orientation.SOUTH : Orientation.NORTH);
        }

        return nodeInTheGraph;
    }

    private void createMainLabel (LabeledObject lo) {
        Label newLabel = new TextLabel("dummyPort" + portnumber++);
        lo.getLabelManager().addLabel(newLabel);
        lo.getLabelManager().setMainLabel(newLabel);
    }
}
