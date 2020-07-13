package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.restore.OneNodeEdge;

import java.util.*;

public class NodePlacement {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private List<List<Port>> structure;
    private CMResult cmResult;
    private List<Integer> heightOfLayers;
    private Vertex dummyVertex;
    private double layerHeight;
    private List<Port> dummyPorts;
    private List<Edge> dummyEdges;
    private Set<Edge> oneNodeEdges;

    private int portnumber; //TODO: this was static before; I don't know why this should be static?

    // stuff from paper
    private Map<Port, PortValues> portValues;
    private double delta;

    public NodePlacement (SugiyamaLayouter sugy, CMResult cmResult, DrawingInformation drawingInformation) {
        this.sugy = sugy;
        this.cmResult = cmResult;
        this.drawInfo = drawingInformation;
    }

    public void placeNodes () {
        Map<Port, Double> xValues = new LinkedHashMap<>();
        initialise();
        // create lists of ports for layers
        // and algorithm.restore all oneNodeEdges as well as noEdgePorts
        initialiseStructure();
        // create dummyPorts to have enough space for labels
        dummyPortsForWidth();
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
            // initialise datastructure portValues
            initialisePortValues();
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
        // creates shapes for all nodes
        draw();
        // delete dummyPorts from nodes
        for (Port dummy : dummyPorts) {
            dummy.getVertex().removePortComposition(dummy);
        }
        for (Edge dummy : dummyEdges) {
            for (Port port : new LinkedList<>(dummy.getPorts())) {
                port.removeEdge(dummy);
            }
        }
    }

    private void initialise() {
        structure = new ArrayList<>();
        portValues = new LinkedHashMap<>();
        delta = Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing());
        heightOfLayers = new ArrayList<>();
        dummyPorts = new LinkedList<>();
        dummyEdges = new LinkedList<>();
        dummyVertex = new Vertex();
        dummyVertex.getLabelManager().addLabel(new TextLabel("dummyVertex"));
        dummyVertex.getLabelManager().setMainLabel(dummyVertex.getLabelManager().getLabels().get(0));
        oneNodeEdges = new LinkedHashSet<>();
        portnumber = 1000;
    }

    private void initialiseStructure() {
        for (List<Vertex> rankNodes : cmResult.getNodeOrder()) {
            List<Port> rankBottomPorts = new ArrayList<>();
            List<Port> rankTopPorts = new ArrayList<>();
            // Map<Port, Integer> rankBottomPortsMap = new LinkedHashMap<>();
            // Map<Port, Integer> rankTopPortsMap = new LinkedHashMap<>();
            addDividingNodePair(rankBottomPorts, rankTopPorts);
            // crate a List with all bottomPorts and one with all topPorts
            for (Vertex node : rankNodes) {

                // algorithm.restore
                for (OneNodeEdge one : sugy.getOneNodeEdges(node)) {
                    if (one.hasPortGroup()) one.restore();
                    for (Port port : one.getPorts()) {

                    }
                }

                for (Port port : cmResult.getBottomPortOrder().get(node)) {
                    // rankBottomPortsMap.put(port, rankBottomPorts.size());
                    rankBottomPorts.add(port);

                    // add new Edge for each PortPairing
                    if (sugy.isPaired(port)) {
                        List<Port> ports = new ArrayList<>();
                        ports.add(port);
                        ports.add(sugy.getPairedPort(port));
                        dummyEdges.add(new Edge(ports));
                    }

                    // add new Edge if DummyNode
                    if (sugy.isDummy(node) && cmResult.getBottomPortOrder().get(node).size() == 1) {
                        List<Port> ports = new ArrayList<>();
                        ports.add(port);
                        ports.add(cmResult.getTopPortOrder().get(node).get(0));
                        dummyEdges.add(new Edge(ports));
                    }

                }
                for (Port port : cmResult.getTopPortOrder().get(node)) {
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
        for (int i = 0; i < structure.size(); i++) {
            heightOfLayers.add(0);
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
        Edge e = new Edge(ports);
        rankBottomPorts.add(p1);
        rankTopPorts.add(p2);
        dummyVertex.addPortComposition(p1);
        dummyVertex.addPortComposition(p2);
    }

    private void dummyPortsForWidth() {
        layerHeight = drawInfo.getVertexHeight();
        for (int layer = 0; layer < structure.size(); layer++) {
            List<Port> order = new ArrayList<>(structure.get(layer));
            List<Port> newOrder = new ArrayList<>();
            newOrder.add(order.get(0));
            double currentWidth = 0;
            double minWidth = 0;
            Vertex currentNode = dummyVertex;
            int nodePosition = 0;
            for (int position = 1; position < order.size(); position++) {
                if (currentNode.equals(dummyVertex)) {
                    currentNode = order.get(position).getVertex();
                    nodePosition = position;
                    currentWidth = (delta);
                    minWidth = 0;
                    if (currentNode.equals(dummyVertex)) {
                        newOrder.add(order.get(position));
                    } else {
                        minWidth = sugy.getTextWidthForNode(currentNode);
                    }
                    heightOfLayers.set(layer, Math.max(heightOfLayers.get(layer), sugy.getNodeName(currentNode).length));
                    if (sugy.isTurningPointDummy(currentNode)) {
                        double value = (((currentNode.getPorts().size() / 2) * drawInfo.getEdgeDistanceVertical()) / layerHeight);
                        heightOfLayers.set(layer, Math.max(heightOfLayers.get(layer), (int)Math.ceil(value)));
                    }
                } else if (order.get(position).getVertex().equals(currentNode)) {
                    currentWidth += (delta);
                } else {
                    LinkedList<Port> nodeOrder = new LinkedList<>(order.subList(nodePosition, position));
                    boolean first = true;
                    while (currentWidth < minWidth) {
                        Port p = new Port();
                        createMainLabel(p);
                        currentNode.addPortComposition(p);
                        dummyPorts.add(p);
                        if (first) {
                            first = false;
                            nodeOrder.addFirst(p);
                        } else {
                            first = true;
                            nodeOrder.addLast(p);
                        }
                        currentWidth += (delta);
                    }
                    newOrder.addAll(nodeOrder);
                    newOrder.add(order.get(position));
                    currentNode = dummyVertex;
                }
            }
            structure.set(layer, newOrder);
        }
    }

    private void initialisePortValues() {
        for (int i = 0; i < structure.size(); i++) {
            for (int j = 0; j < structure.get(i).size(); j++) {
                Port port = structure.get(i).get(j);
                if (j == 0) portValues.put(port, new PortValues(port,port,j,i));
                else portValues.put(port, new PortValues(port,structure.get(i).get(j-1),j,i));
            }
        }
    }

    private void handleCrossings() {
        for (int layer = 0; layer < (structure.size() - 1); layer++) {
            LinkedList<Port[]> stack = new LinkedList<>();
            for (Port port1 : structure.get(layer)) {
                for (Edge edge : port1.getEdges()) {
                    if (!oneNodeEdges.contains(edge)) {
                        Port port2 = edge.getPorts().get(0);
                        if (port2.equals(port1)) port2 = edge.getPorts().get(1);
                        if (portValues.get(port2).getLayer() == (layer + 1)) {
                            Port[] stackEntry = {port1, port2};
                            while (true) {
                                if (stack.isEmpty()) {
                                    stack.push(stackEntry);
                                    break;
                                }
                                Port[] top = stack.pop();
                                // check whether top element of stack (top) has higher index than new Element (stackEntry) => crossing => conflict
                                if (portValues.get(top[1]).getPosition() < portValues.get(stackEntry[1]).getPosition()) {
                                    stack.push(top);
                                    stack.push(stackEntry);
                                    break;
                                } else {
                                    // solve conflict by preferring one edge
                                    // prefer dummy to dummy edge
                                    if ((sugy.isDummy(top[0].getVertex()) || sugy.isTurningPointDummy(top[0].getVertex())) && (sugy.isDummy(top[1].getVertex()) || sugy.isTurningPointDummy(top[1].getVertex()))) {
                                        stack.push(top);
                                        break;
                                    } else if (!((sugy.isDummy(stackEntry[0].getVertex()) || sugy.isTurningPointDummy(stackEntry[0].getVertex())) && (sugy.isDummy(stackEntry[1].getVertex()) || sugy.isTurningPointDummy(stackEntry[1].getVertex())))) {
                                        // prefer edge with min index distance (just an arbitrary heuristic - maybe can do better)
                                        if (Math.abs(portValues.get(stackEntry[1]).getPosition() - portValues.get(stackEntry[0]).getPosition()) >
                                                Math.abs(portValues.get(top[1]).getPosition() - portValues.get(top[0]).getPosition())) {
                                            stack.push(top);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // initialise root and align according to Alg. 2 from the paper
            verticalAlignment(stack);
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

    private void draw() {
        double currentY = drawInfo.getPortHeight();
        for (int layer = 0; layer < structure.size(); layer++) {
            // x1, y1, x2, y2
            double[] coordinates = new double[4];
            int nodePosition = 0;
            // initialise shape of first node
            coordinates[0] = (portValues.get(structure.get(layer).get(0)).getX() + (delta / 2.0));
            coordinates[1] = currentY;

            for (int pos = 1; pos < structure.get(layer).size(); pos++) {
                Port port = structure.get(layer).get(pos);
                if (port.getVertex().equals(dummyVertex)) {
                    // one node done - create Rectangle
                    coordinates[2] = (portValues.get(port).getX() - (delta / 2.0));
                    coordinates[3] = (coordinates[1] + (layerHeight * heightOfLayers.get(layer)) + (2.0 * drawInfo.getBorderWidth()));
                    Rectangle nodeShape = new Rectangle(coordinates[0], coordinates[1], coordinates[2] - coordinates[0], coordinates[3] - coordinates [1], null);
                    cmResult.getNodeOrder().get(layer / 2).get(nodePosition++).setShape(nodeShape);

                    // initialise shape of next node
                    coordinates[0] = (portValues.get(port).getX() + (delta / 2.0));
                    coordinates[1] = currentY;
                } else {
                    Rectangle portShape = new Rectangle(portValues.get(port).getX(), currentY, drawInfo.getPortWidth(), drawInfo.getPortHeight(),null);
                    port.setShape(portShape);
                }
            }

            currentY += ((layerHeight * heightOfLayers.get(layer)) + (2.0 * drawInfo.getBorderWidth()));
            layer++;

            for (int pos = 1; pos < structure.get(layer).size(); pos++) {
                Port port = structure.get(layer).get(pos);
                if (!port.getVertex().equals(dummyVertex)) {
                    Rectangle portShape = new Rectangle(portValues.get(port).getX(), currentY, drawInfo.getPortWidth(), drawInfo.getPortHeight(),null);
                    port.setShape(portShape);
                }
            }
            currentY += ((2 * drawInfo.getPortHeight()) + drawInfo.getDistanceBetweenLayers());
        }
    }

//    private void drawFirst() {
//        Set<Edge> edges = new LinkedHashSet<>();
//        sugy.getGraph().addVertex(dummyVertex);
//        double currentY = 10;
//        boolean bool = true;
//        for (List<Port> layer : structure) {
//            for (Port port : layer) {
//                Rectangle portShape = new Rectangle((portValues.get(port).getX() - (drawInfo.getPortWidth() / 2.0)), currentY, drawInfo.getPortWidth(), drawInfo.getPortHeight(),null);
//                port.setShape(portShape);
//                for (Edge edge : port.getEdges()) {
//                    if (!sugy.getGraph().getEdges().contains(edge)) {
//                        sugy.getGraph().addEdge(edge);
//                        edges.add(edge);
//                    }
//                }
//            }
//            if (bool) currentY += 12;
//            else currentY += 20;
//        }
//        for (Vertex node : sugy.getGraph().getVertices()) {
//            node.setShape(new Rectangle(0,0,0,0, null));
//        }
//        sugy.drawResult("test11x.svg");
//        for (Edge edge : edges) sugy.getGraph().removeEdge(edge);
//        sugy.getGraph().removeVertex(dummyVertex);
//    }

    private void createMainLabel (LabeledObject lo) {
        Label newLabel = new TextLabel("" + portnumber++);
        lo.getLabelManager().addLabel(newLabel);
        lo.getLabelManager().setMainLabel(newLabel);
    }
}
