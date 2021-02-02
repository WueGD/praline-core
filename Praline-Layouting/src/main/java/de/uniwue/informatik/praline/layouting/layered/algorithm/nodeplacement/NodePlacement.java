package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

import java.util.*;

public class NodePlacement {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private List<List<Port>> structure;
    private SortingOrder sortingOrder;
    private List<Double> heightOfLayers;
    private Vertex dummyVertex;
    private double layerHeight;
    private Map<Vertex, Set<Port>> dummyPorts;
    private List<Edge> dummyEdges;
    private int portnumber;
    // variables according to paper:
    private Map<Port, PortValues> portValues;
    private double delta;

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
        dummyPortsForWidth();

        Map<Port, Double> xValues = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) {
            switch (i) {
                case 2:
                    for (List<Port> order : structure) {
                        Collections.reverse(order);
                    }
                    break;
                case 1:
                    //same as case 3
                case 3:
                    Collections.reverse(structure);
            }
            // initialize datastructure portValues
            initializePortValues();
            // mark conflicts (crossing edges)
            handleCrossings();
            // make compact
            horizontalCompaction();
            // add to xValues
            switch (i) {
                case 0:
                    for (Port port : portValues.keySet()) {
                        xValues.put(port, portValues.get(port).getX());
                    }
                    break;
                case 1:
                    for (Port port : portValues.keySet()) {
                        xValues.replace(port, (xValues.get(port) + portValues.get(port).getX()));
                    }
                    break;
                case 2:
                    //same as case 3
                case 3:
                    for (Port port : portValues.keySet()) {
                        xValues.replace(port, (xValues.get(port) - portValues.get(port).getX()));
                    }
            }
        }
        for (Map.Entry<Port, PortValues> entry : portValues.entrySet()) {
            entry.getValue().setX(xValues.get(entry.getKey()) / 4);
        }
        // bring back original order
        for (List<Port> order : structure) {
            Collections.reverse(order);
        }
        // change to positive x-values
        makePositive();

        reTransformStructure(false);

        return dummyPorts;
    }

    public void initialize() {
        structure = new ArrayList<>();
        portValues = new LinkedHashMap<>();
        delta = Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing());
        heightOfLayers = new ArrayList<>();
        dummyPorts = new LinkedHashMap<>();
        dummyEdges = new LinkedList<>();
        dummyVertex = new Vertex();
        dummyVertex.getLabelManager().addLabel(new TextLabel("dummyVertex"));
        dummyVertex.getLabelManager().setMainLabel(dummyVertex.getLabelManager().getLabels().get(0));
        portnumber = 0;
    }

    public void initializeStructure() {
        int layer = -1;
        for (List<Vertex> rankNodes : sortingOrder.getNodeOrder()) {
            ++layer;
            heightOfLayers.add(0.0);
            for (Vertex node : rankNodes) {
                heightOfLayers.set(layer, Math.max(heightOfLayers.get(layer),
                        sugy.isDummy(node) ? 0.0 : 1.0)); //sugy.getNodeName(node).size()));
            }
            List<Port> rankBottomPorts = new ArrayList<>();
            List<Port> rankTopPorts = new ArrayList<>();
            // Map<Port, Integer> rankBottomPortsMap = new LinkedHashMap<>();
            // Map<Port, Integer> rankTopPortsMap = new LinkedHashMap<>();
            addDividingNodePair(rankBottomPorts, rankTopPorts);
            // crate a List with all bottomPorts and one with all topPorts
            for (Vertex node : rankNodes) {
                for (Port port : sortingOrder.getBottomPortOrder().get(node)) {
                    // rankBottomPortsMap.put(port, rankBottomPorts.size());
                    rankBottomPorts.add(port);

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
                    // rankTopPortsMap.put(port, rankTopPorts.size());
                    rankTopPorts.add(port);
                }
                addDividingNodePair(rankBottomPorts, rankTopPorts);
            }
            structure.add(rankBottomPorts);
            structure.add(rankTopPorts);
            //structureMap.add(rankBottomPortsMap);
            //structureMap.add(rankTopPortsMap);
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

    private void addDividingNodePair(List<Port> rankBottomPorts, List<Port> rankTopPorts) {
        Port p1 = new Port();
        Port p2 = new Port();
        createMainLabel(p1);
        createMainLabel(p2);
        List<Port> ports = new ArrayList<>();
        ports.add(p1);
        ports.add(p2);
        new Edge(ports);
        rankBottomPorts.add(p1);
        rankTopPorts.add(p2);
        dummyVertex.addPortComposition(p1);
        dummyVertex.addPortComposition(p2);
    }

    public Map<Vertex, Set<Port>> dummyPortsForWidth() {
        layerHeight = drawInfo.getVertexHeight();
        for (int layer = 0; layer < structure.size(); layer++) {
            List<Port> order = new ArrayList<>(structure.get(layer));
            List<Port> newOrder = new ArrayList<>();
            newOrder.add(order.get(0));
            double currentWidth = 0;
            double minWidth = 0;
            double currentWidthUnionNode = 0;
            Vertex currentNode = dummyVertex;
            Vertex currentUnionNode = null;
            int nodePosition = 0;
            for (int position = 1; position < order.size(); position++) {
                if (currentNode.equals(dummyVertex)) {
                    currentNode = order.get(position).getVertex();
                    //special case: if current node is a union node, consider the single parts
                    if (sugy.isUnionNode(currentNode)) {
                        currentUnionNode = currentNode;
                        currentWidthUnionNode = (delta);
                        currentNode = sugy.getReplacedPorts().get(order.get(position)).getVertex();
                    }
                    nodePosition = position;
                    currentWidth = (delta);
                    minWidth = 0;
                    if (currentNode.equals(dummyVertex)) {
                        newOrder.add(order.get(position));
                    } else if (!sugy.getDeviceVertices().contains(currentNode)) {
                        //we will handle device vertices in the end via the union node
                        minWidth = sugy.getMinWidthForNode(currentNode);
                    }
                } else if (order.get(position).getVertex().equals(currentNode)
                        || (sugy.getReplacedPorts().containsKey(order.get(position))
                                && sugy.getReplacedPorts().get(order.get(position)).getVertex().equals(currentNode))) {
                    currentWidth += (delta);
                    currentWidthUnionNode += (delta);
                } else if (order.get(position).getVertex().equals(currentUnionNode)) {
                    //still the same union node but different sub-node
                    List<Port> nodeOrder = addDummyPortsAndGetNewOrder(order, currentWidth, minWidth,
                            currentUnionNode, currentNode, nodePosition, position);
                    newOrder.addAll(nodeOrder);

                    currentNode = sugy.getReplacedPorts().get(order.get(position)).getVertex();
                    nodePosition = position;
                    currentWidth = (delta);
                    currentWidthUnionNode += (delta);
                    minWidth = sugy.getMinWidthForNode(currentNode);
                } else {
                    List<Port> nodeOrder = addDummyPortsAndGetNewOrder(order, currentWidth, minWidth,
                            currentUnionNode, currentNode, nodePosition, position);
                    newOrder.addAll(nodeOrder);
                    currentNode = dummyVertex;
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
                                newOrder.add(p);
                                currentWidthUnionNode += (delta);
                            }
                        }
                        currentUnionNode = null;
                    }
                    newOrder.add(order.get(position));
                }
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
     * @return
     */
    private List<Port> addDummyPortsAndGetNewOrder(List<Port> order, double currentWidth, double minWidth,
                                                   Vertex currentUnionNode, Vertex currentNode, int nodePosition,
                                                   int position) {
        if (currentUnionNode == null) {
            currentUnionNode = currentNode;
        }
        LinkedList<Port> nodeOrder = new LinkedList<>(order.subList(nodePosition, position));
        boolean first = true;
        while (currentWidth < minWidth) {
            Port p = new Port();
            createMainLabel(p);
            addToCorrectPortGroupOrNode(p, currentUnionNode, currentNode);
            dummyPorts.putIfAbsent(currentNode, new LinkedHashSet<>());
            dummyPorts.get(currentNode).add(p);
            if (first) {
                first = false;
                nodeOrder.addFirst(p);
            } else {
                first = true;
                nodeOrder.addLast(p);
            }
            currentWidth += (delta);
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

    private void initializePortValues() {
        for (int i = 0; i < structure.size(); i++) {
            for (int j = 0; j < structure.get(i).size(); j++) {
                Port port = structure.get(i).get(j);
                if (j == 0) portValues.put(port, new PortValues(port,port,j,i));
                else portValues.put(port, new PortValues(port,structure.get(i).get(j-1),j,i));
            }
        }
    }

    private void handleCrossings() {
        //determine for all long edges, over how many dummy vertices they go, i.e., their length.
        //later, longer edges will be preferred for making them straight compared to shorter edges
        Map<Edge, Integer> lengthOfLongEdge = new LinkedHashMap<>();
        determineLengthOfLongEdges(lengthOfLongEdge);

        for (int layer = 0; layer < (structure.size() - 1); layer++) {
            LinkedList<Port[]> stack = new LinkedList<>(); //stack of edges that are made straight
            //an entry of this stack is the 2 end ports of the corresponding edge, the first for the lower, the second
            // for the upper port
            for (Port port0 : structure.get(layer)) {
                for (Edge edge : port0.getEdges()) {
                    Port port1 = PortUtils.getOtherEndPoint(edge, port0);
                    if (portValues.get(port1).getLayer() == (layer + 1)) {
                        Port[] stackEntry = {port0, port1};
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

    private void fillStackOfCrossingEdges(LinkedList<Port[]> stack, Port[] stackEntry,
                                          LinkedList<Port[]> removedStackEntriesPotentiallyToBeReAdded,
                                          Map<Edge, Integer> lengthOfLongEdge) {
        while (true) {
            if (stack.isEmpty()) {
                stack.push(stackEntry);
                break;
            }
            Port[] top = stack.pop();
            // if the upper port values of the top edge of the stack and the current edge are increasing, they don't
            // cross (on this layer) and we can keep them both.
            // However we will discard all edges in removedStackEntriesPotentiallyToBeReAdded that were potentially
            // in between them two because they have lower priority than stackEntry and are in conflict with stackEntry
            if (portValues.get(top[1]).getPosition() < portValues.get(stackEntry[1]).getPosition()) {
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
                int topLength = lengthOfLongEdge.getOrDefault(sugy.getOriginalEdgeExceptForHyperE(top[1].getVertex()),0);
                int stackEntryLength =
                        lengthOfLongEdge.getOrDefault(sugy.getOriginalEdgeExceptForHyperE(stackEntry[1].getVertex()),0);
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

    private void keepTop(LinkedList<Port[]> stack, LinkedList<Port[]> removedStackEntriesPotentiallyToBeReAdded,
                         Port[] top) {
        stack.push(top);
        //re-add the entries we removed in between back to the stack
        while (!removedStackEntriesPotentiallyToBeReAdded.isEmpty()) {
            stack.push(removedStackEntriesPotentiallyToBeReAdded.pop());
        }
    }

    private void verticalAlignment(List<Port[]> edges) {
        for (Port[] entry : edges) {
            if (portValues.get(entry[1]).getAlign() == entry[1]) {
                portValues.get(entry[0]).setAlign(entry[1]);
                portValues.get(entry[1]).setRoot(portValues.get(entry[0]).getRoot());
                portValues.get(entry[1]).setAlign(portValues.get(entry[1]).getRoot());
            }
        }
    }

    private void horizontalCompaction() {
        for (List<Port> list : structure) {
            for (Port v : list) {
                if (portValues.get(v).getRoot().equals(v)) {
                    placeBlock(v);
                }
            }
        }
        // compute x values
        for (List<Port> list : structure) {
            for (Port v : list) {
                portValues.get(v).setX(portValues.get(portValues.get(v).getRoot()).getX());
            }
        }
        // fix shift values
        for (int i = 0; i < structure.size(); i++) {
            setShift(structure.get(i).get(0));
        }
        // do shift
        for (List<Port> list : structure) {
            for (Port v : list) {
                Double shift = portValues.get(portValues.get(portValues.get(v).getRoot()).getSink()).getGlobalShift();
                if (shift < Double.MAX_VALUE) {
                    portValues.get(v).setX(portValues.get(v).getX() + shift);
                }
            }
        }
    }

    // compute global shift
    private void setShift (Port v) {
        // if node is sink and shift is not set
        if (portValues.get(v).getRoot().equals(v) && !(portValues.get(v).getGlobalShift() < Double.MAX_VALUE)) {
            double shift = Double.MAX_VALUE;
            // compute shift value to all other nodes it has one to
            for (Map.Entry<Port, Double> entry : portValues.get(v).getShift().entrySet()) {
                Port u = entry.getKey();
                Double shiftU = portValues.get(u).getGlobalShift();
                Double newShift;
                if (shiftU < Double.MAX_VALUE) {
                    newShift = (entry.getValue() + shiftU);
                } else {
                    newShift = entry.getValue();
                }
                if (shift < Double.MAX_VALUE) {
                    shift = Math.min(shift, newShift);
                } else {
                    shift = newShift;
                }
            }
            portValues.get(v).setGlobalShift(shift);
        }
    }

    // method placeBlock from paper
    private void placeBlock(Port portV) {
        PortValues v = portValues.get(portV);
        if (v.getX() == Double.MIN_VALUE) {
            v.setX(0);
            Port portW = portV;
            PortValues w = v;
            do {
                if (w.getPosition() > 0) {
                    Port portU = portValues.get(w.getPredecessor()).getRoot();
                    PortValues u = portValues.get(portU);
                    placeBlock(portU);
                    if (v.getSink().equals(portV)) {
                        v.setSink(u.getSink());
                    }
                    if (!v.getSink().equals(u.getSink())) {
                        portValues.get(u.getSink()).addShift((v.getX() - u.getX() - delta), v.getSink());
                    } else {
                        v.setX(Math.max(v.getX(),(u.getX() + delta)));
                    }
                }
                portW = w.getAlign();
                w = portValues.get(portW);
            } while (!portW.equals(portV));
        }
    }

    private void makePositive() {
        //double shift = 0.0;
        //for (int layer = 0; layer < structure.size(); layer++) {
        //    Port potentialSink = structure.get(layer).get(0);
        //    if (portValues.get(potentialSink).getSink().equals(potentialSink)) {
        //        shift += portValues.get(potentialSink).getShift();
        //        portValues.get(potentialSink).setShift(shift);
        //    }
        //}

        double minX = Double.MAX_VALUE;
        for (PortValues portValue : portValues.values()) {
            if (portValue.getX() < minX) {
                minX = portValue.getX();
            }
        }
        if (minX < 0) {
            for (PortValues portValue : portValues.values()) {
                portValue.setX(portValue.getX() - minX + 10);
            }
        } else {
            for (PortValues portValue : portValues.values()) {
                portValue.setX(portValue.getX() + 10);
            }
        }
    }

    private void draw(boolean addDummyPortsForPaddingToOrders) {
        double currentY = drawInfo.getPortHeight();
        for (int layer = 0; layer < structure.size(); layer++) {
            // x1, y1, x2, y2
            int nodePosition = 0;
            // initialize shape of first node
            double xPos = addDummyPortsForPaddingToOrders ? 0.0 :
                    (portValues.get(structure.get(layer).get(0)).getX() + (delta / 2.0));
            double yPos = currentY;
            Vertex nodeInTheGraph = null;
            int portIndexAtVertex = 0;
            for (int pos = 0; pos < structure.get(layer).size(); pos++) {
                Port port = structure.get(layer).get(pos);
                if (port.getVertex().equals(dummyVertex)) {
                    portIndexAtVertex = 0;
                    // one node done - create Rectangle
                    if (!addDummyPortsForPaddingToOrders && nodeInTheGraph != null) {
                        double width = (portValues.get(port).getX() - (delta / 2.0)) - xPos;
                        double height = heightOfLayers.get(layer / 2) * layerHeight +
                                Math.min(1.0, heightOfLayers.get(layer / 2)) * 2.0 * drawInfo.getBorderWidth();
                        Rectangle nodeShape = new Rectangle(xPos, yPos, width, height, null);
                        nodeInTheGraph.setShape(nodeShape);

                        // initialize shape of next node
                        xPos = (portValues.get(port).getX() + (delta / 2.0));
                        yPos = currentY;
                    }
                    if (nodePosition < sortingOrder.getNodeOrder().get(layer / 2).size()) {
                        nodeInTheGraph = sortingOrder.getNodeOrder().get(layer / 2).get(nodePosition++);
                    }
                } else {
                    createPortShape(currentY, port, true, nodeInTheGraph, portIndexAtVertex,
                            addDummyPortsForPaddingToOrders);
                    ++portIndexAtVertex;
                }
            }

            currentY += heightOfLayers.get(layer / 2) * layerHeight
                    + Math.min(1.0, heightOfLayers.get(layer / 2)) * 2.0 * drawInfo.getBorderWidth();
            layer++;

            for (int pos = 1; pos < structure.get(layer).size(); pos++) {
                Port port = structure.get(layer).get(pos);
                if (!port.getVertex().equals(dummyVertex)) {
                    Vertex currentNode = createPortShape(currentY, port, false, nodeInTheGraph,
                            portIndexAtVertex, addDummyPortsForPaddingToOrders);
                    if (!currentNode.equals(nodeInTheGraph)) {
                        nodeInTheGraph = currentNode;
                        portIndexAtVertex = 0;
                    }
                    ++portIndexAtVertex;
                }
            }
            currentY += ((2 * drawInfo.getPortHeight()) + drawInfo.getDistanceBetweenLayers());
        }
    }

    private Vertex createPortShape(double currentY, Port port, boolean isBottomSide, Vertex nodeInTheGraph,
                                   int portIndexAtNode, boolean addDummyPortsForPaddingToOrders) {
        if (!port.getVertex().equals(nodeInTheGraph)) {
            nodeInTheGraph = port.getVertex();
            portIndexAtNode = 0;
        }

        if (!addDummyPortsForPaddingToOrders) {
            Rectangle portShape = new Rectangle(portValues.get(port).getX(), currentY, drawInfo.getPortWidth(),
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
