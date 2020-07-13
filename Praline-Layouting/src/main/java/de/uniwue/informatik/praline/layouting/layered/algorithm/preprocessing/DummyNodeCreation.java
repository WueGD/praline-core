package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;

import java.util.*;

public class DummyNodeCreation {

    private SugiyamaLayouter sugy;
    private Map<Vertex, Integer> newRanks;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Vertex, Vertex> nodeToLowerDummyTurningPoint;
    private Map<Vertex, Vertex> nodeToUpperDummyTurningPoint;
    private Map<Port, Port> correspondingPortsAtDummy;
    private Map<Edge, Edge> dummyEdge2RealEdge;
    private boolean[] usedRanks;

    public DummyNodeCreation (SugiyamaLayouter sugy) {
        this.sugy = sugy;
        this.newRanks = new LinkedHashMap<>();
        this.dummyNodesLongEdges = new LinkedHashMap<>();
        this.dummyTurningNodes = new LinkedHashMap<>();
        this.nodeToLowerDummyTurningPoint = new LinkedHashMap<>();
        this.nodeToUpperDummyTurningPoint = new LinkedHashMap<>();
        this.correspondingPortsAtDummy = new LinkedHashMap<>();
        this.dummyEdge2RealEdge = new LinkedHashMap<>();
    }

    // goal: create as less dummyNodesLongEdges as possible to minimize computation time and storage

    /**
     *
     * @return
     *      Mapping of dummy vertices to edges
     */
    public DummyCreationResult createDummyNodes () {
        // create new ranks to have place for dummyNodesLongEdges by doubling all ranks
        // dummyNodesLongEdges can then be placed in even ranks with rank 0 and rank maxRank = empty
        for (Vertex node : sugy.getGraph().getVertices()) {
            newRanks.put(node, ((sugy.getRank(node)*2)+1));
        }
        sugy.changeRanks(newRanks);

        // initalise usedRanks
        int maxRank = sugy.getMaxRank();
        // usedRanks[r] is true if there is any node with rank r
        usedRanks = new boolean[maxRank + 2];
        boolean value = false;
        for (int i = 0; i < usedRanks.length; i++) {
            usedRanks[i] = value;
            value = !value;
        }

        // dummynodes for portGroups
        for (Vertex node : new ArrayList<>(sugy.getGraph().getVertices())) {
            if (sugy.isPlug(node)) {
                LinkedList<Port> ports1 = new LinkedList();
                LinkedList<Port> ports2 = new LinkedList();
                for (PortComposition pc : node.getPortCompositions()) {
                    if (pc instanceof PortGroup && ports1.isEmpty()) {
                        PortUtils.getPortsRecursively(pc, ports1);
                    } else if (pc instanceof PortGroup && ports2.isEmpty()) {
                        PortUtils.getPortsRecursively(pc, ports2);
                    } else {
                        // something went wrong during SugiyamaLayouter.construct().handleVertexGroup()
                    }
                }

                // check whether they are connected up or down
                LinkedHashSet<Edge> upEdges1 = new LinkedHashSet<>();
                LinkedHashSet<Edge> downEdges1 = new LinkedHashSet<>();
                LinkedHashSet<Edge> upEdges2 = new LinkedHashSet<>();
                LinkedHashSet<Edge> downEdges2 = new LinkedHashSet<>();

                for (Port port : ports1) {
                    for (Edge edge : port.getEdges()) {
                        if (sugy.getStartNode(edge).equals(node)) {
                            // edge is directed upwards
                            upEdges1.add(edge);
                        } else {
                            // edge is directed downwards
                            downEdges1.add(edge);
                        }
                    }
                }
                for (Port port : ports2) {
                    for (Edge edge : port.getEdges()) {
                        if (sugy.getStartNode(edge).equals(node)) {
                            // edge is directed upwards
                            upEdges2.add(edge);
                        } else {
                            // edge is directed downwards
                            downEdges2.add(edge);
                        }
                    }
                }
                // place portGroups so that more ports are on the right side
                // create dummynodes for edges on wrong side
                if ((upEdges1.size() - downEdges1.size()) > (upEdges2.size() - downEdges2.size())) {
                    // portGroup1 will be placed on the upper side of the node due to more connections upwards
                    // add dummynodes into an additional rank above the nodes rank
                    for (Edge downEdge : downEdges1) {
                        Vertex upperDummyTurningNode =
                                getDummyTurningNodeForVertex(node, false, (newRanks.get(node) + 1));
                        splitEdgeByTurningDummyNode(downEdge, upperDummyTurningNode);
                        usedRanks[(newRanks.get(node) + 1)] = true;
                    }
                    // add dummynodes into an additional below the nodes rank
                    for (Edge upEdge : upEdges2) {
                        Vertex lowerDummyTurningNode =
                                getDummyTurningNodeForVertex(node, true, (newRanks.get(node) - 1));
                        splitEdgeByTurningDummyNode(upEdge, lowerDummyTurningNode);
                        usedRanks[(newRanks.get(node) - 1)] = true;
                    }
                } else {
                    // portGroup2 will be placed on the upper side of the node
                    // add dummynodes into an additional below the nodes rank
                    for (Edge upEdge : upEdges1) {
                        Vertex lowerDummyTurningNode =
                                getDummyTurningNodeForVertex(node, true, (newRanks.get(node) - 1));
                        splitEdgeByTurningDummyNode(upEdge, lowerDummyTurningNode);
                        usedRanks[(newRanks.get(node) - 1)] = true;
                    }
                    // add dummynodes into an additional rank above the nodes rank
                    for (Edge downEdge : downEdges2) {
                        Vertex upperDummyTurningNode =
                                getDummyTurningNodeForVertex(node, false, (newRanks.get(node) + 1));
                        splitEdgeByTurningDummyNode(downEdge, upperDummyTurningNode);
                        usedRanks[(newRanks.get(node) + 1)] = true;
                    }
                }
            } else {
                // for each portGroup
                for (PortComposition pc : node.getPortCompositions()) {
                    if (pc instanceof PortGroup) {
                        // find all ports
                        LinkedList<Port> ports = new LinkedList();
                        for (PortComposition lowerPc : ((PortGroup) pc).getPortCompositions()) {
                            PortUtils.getPortsRecursively(lowerPc, ports);
                        }
                        // check whether they are connected up or down
                        LinkedHashSet<Edge> upEdges = new LinkedHashSet<>();
                        LinkedHashSet<Edge> downEdges = new LinkedHashSet<>();
                        for (Port port : ports) {
                            for (Edge edge : port.getEdges()) {
                                if (sugy.getStartNode(edge).equals(node)) {
                                    // edge is directed upwards
                                    upEdges.add(edge);
                                } else {
                                    // edge is directed downwards
                                    downEdges.add(edge);
                                }
                            }
                        }
                        // place portGroup so that more ports are on the right side
                        // create dummynodes for edges on wrong side
                        if (downEdges.size() < upEdges.size()) {
                            // portGroup will be placed on the upper side of the node due to more connections upwards
                            // add dummynodes into an additional rank above the nodes rank
                            for (Edge downEdge : downEdges) {
                                Vertex upperDummyTurningNode =
                                        getDummyTurningNodeForVertex(node, false, (newRanks.get(node) + 1));
                                splitEdgeByTurningDummyNode(downEdge, upperDummyTurningNode);
                                usedRanks[(newRanks.get(node) + 1)] = true;
                            }
                        } else {
                            // portGroup will be placed on the lower side of the node due to more connections downwards
                            // add dummynodes into an additional layer below the nodes rank
                            for (Edge upEdge : upEdges) {
                                Vertex lowerDummyTurningNode =
                                        getDummyTurningNodeForVertex(node, true, (newRanks.get(node) - 1));
                                splitEdgeByTurningDummyNode(upEdge, lowerDummyTurningNode);
                                usedRanks[(newRanks.get(node) - 1)] = true;
                            }
                        }
                    }
                }
            }
        }

        // delete empty ranks
        sugy.changeRanks(newRanks);
        int rankAdd = 0;
        for (int rank = 0; rank < usedRanks.length; rank++) {
            if (!usedRanks[rank]) {
                rankAdd--;
            } else {
                for (Vertex node : sugy.getAllNodesWithRank(rank)) {
                    newRanks.replace(node, (rank + rankAdd));
                }
            }
        }

        // create dummynodes for each edge passing a layer
        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            int dist = (newRanks.get(sugy.getEndNode(edge))) - (newRanks.get(sugy.getStartNode(edge)));
            if (dist > 1) {
                createAllDummyNodesForEdge(edge);
            }
        }

        sugy.changeRanks(newRanks);
        return new DummyCreationResult(dummyNodesLongEdges, dummyTurningNodes, nodeToLowerDummyTurningPoint,
                nodeToUpperDummyTurningPoint, correspondingPortsAtDummy, dummyEdge2RealEdge);
    }

    private Vertex getDummyTurningNodeForVertex(Vertex vertex, boolean lowerTurningPoint, int rank) {
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
                new TextLabel(place + "_turning_dummy_for_" + vertex.getLabelManager().getMainLabel().toString());
        dummy.getLabelManager().addLabel(idDummy);
        dummy.getLabelManager().setMainLabel(idDummy);

        // add everything to graph and rank dummy
        sugy.getGraph().addVertex(dummy);
        newRanks.put(dummy,rank);
        dummyTurningNodes.put(dummy, vertex);
        if (lowerTurningPoint) {
            nodeToLowerDummyTurningPoint.put(vertex, dummy);
        }
        else {
            nodeToUpperDummyTurningPoint.put(vertex, dummy);
        }

        return dummy;
    }

    private void splitEdgeByTurningDummyNode(Edge edge, Vertex dummy) {
        // create new Ports and Edges to replace edge
        LinkedList<Port> portsFor1 = new LinkedList<>();
        LinkedList<Port> portsFor2 = new LinkedList<>();
        Port p1 = new Port();
        Port p2 = new Port();
        Label idp1 = new TextLabel("DummyPort_to_" + edge.getPorts().get(0).getLabelManager().getMainLabel().toString());
        Label idp2 = new TextLabel("DummyPort_to_" + edge.getPorts().get(1).getLabelManager().getMainLabel().toString());
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
        Label ide1 = new TextLabel("DummyEdge_to_" + edge.getPorts().get(0).getLabelManager().getMainLabel().toString());
        Label ide2 = new TextLabel("DummyEdge_to_" + edge.getPorts().get(1).getLabelManager().getMainLabel().toString());
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
        if (newRanks.get(dummy) > newRanks.get(portsFor1.get(1).getVertex())) {
            sugy.assignDirection(e1, portsFor1.get(1).getVertex(), dummy);
            sugy.assignDirection(e2, portsFor2.get(1).getVertex(), dummy);
        } else {
            sugy.assignDirection(e1, dummy, portsFor1.get(1).getVertex());
            sugy.assignDirection(e2, dummy, portsFor2.get(1).getVertex());
        }
    }

    private void createDummyNodeForEdgeInLayer (Edge edge, int rank) {
        // create dummyNode and ID
        Vertex dummy = new Vertex();
        Label idDummy = new TextLabel("Dummy_for_" + edge.getLabelManager().getMainLabel().toString());
        dummy.getLabelManager().addLabel(idDummy);
        dummy.getLabelManager().setMainLabel(idDummy);

        // create new Ports and Edges to replace edge
        LinkedList<Port> portsFor1 = new LinkedList<>();
        LinkedList<Port> portsFor2 = new LinkedList<>();
        Port p1 = new Port();
        Port p2 = new Port();
        Label idp1 = new TextLabel("DummyPort_to_" + edge.getPorts().get(0).getLabelManager().getMainLabel().toString());
        Label idp2 = new TextLabel("DummyPort_to_" + edge.getPorts().get(1).getLabelManager().getMainLabel().toString());
        p1.getLabelManager().addLabel(idp1);
        p2.getLabelManager().addLabel(idp2);
        p1.getLabelManager().setMainLabel(idp1);
        p2.getLabelManager().setMainLabel(idp2);
        dummy.addPortComposition(p1);
        dummy.addPortComposition(p2);
        portsFor1.add(p1);
        portsFor2.add(p2);
        portsFor1.add(edge.getPorts().get(0));
        portsFor2.add(edge.getPorts().get(1));
        Edge e1 = new Edge(portsFor1);
        Edge e2 = new Edge(portsFor2);
        Label ide1 = new TextLabel("DummyEdge_to_" + edge.getPorts().get(0).getLabelManager().getMainLabel().toString());
        Label ide2 = new TextLabel("DummyEdge_to_" + edge.getPorts().get(1).getLabelManager().getMainLabel().toString());
        e1.getLabelManager().addLabel(ide1);
        e2.getLabelManager().addLabel(ide2);
        e1.getLabelManager().setMainLabel(ide1);
        e2.getLabelManager().setMainLabel(ide2);
        dummyEdge2RealEdge.put(e1, edge);
        dummyEdge2RealEdge.put(e2, edge);

        // add everything to graph and rank dummy
        sugy.getGraph().addVertex(dummy);
        sugy.getGraph().addEdge(e1);
        sugy.getGraph().addEdge(e2);
        newRanks.put(dummy,rank);

        // delete replaced edge
        dummyNodesLongEdges.put(dummy,edge);
        edge.removePort(edge.getPorts().get(1));
        edge.removePort(edge.getPorts().get(0));
        sugy.getGraph().removeEdge(edge);
        sugy.removeDirection(edge);

        // assign directions to dummyedges
        if (newRanks.get(dummy) > newRanks.get(portsFor1.get(1).getVertex())) {
            sugy.assignDirection(e1, portsFor1.get(1).getVertex(), dummy);
            sugy.assignDirection(e2, portsFor2.get(1).getVertex(), dummy);
        } else {
            sugy.assignDirection(e1, dummy, portsFor1.get(1).getVertex());
            sugy.assignDirection(e2, dummy, portsFor2.get(1).getVertex());
        }
    }

    private void createAllDummyNodesForEdge (Edge edge) {
        String edgeName = edge.getLabelManager().getMainLabel().toString();
        Edge refEdge = edge;
        if (edgeName.startsWith("DummyEdge_to_")) {
            refEdge = dummyEdge2RealEdge.get(edge);
            edgeName = refEdge.getLabelManager().getMainLabel().toString();
//            if (edge.getPorts().get(0).getVertex().getLabelManager().getMainLabel().toString().startsWith("Dummy_for_")) {
//                refEdge = dummyNodesLongEdges.get(edge.getPorts().get(0).getVertex());
//                edgeName = refEdge.getLabelManager().getMainLabel().toString();
//            } else {
//                refEdge = dummyNodesLongEdges.get(edge.getPorts().get(1).getVertex());
//                edgeName = refEdge.getLabelManager().getMainLabel().toString();
//            }
        }

        // for each layer create a dummynode and connect it with an additional edge
        Vertex lowerNode = sugy.getStartNode(edge);
        Port lowerPort = edge.getPorts().get(0);
        Port upperPort = edge.getPorts().get(1);
        int layer;
        if (!lowerPort.getVertex().equals(lowerNode)) {
            lowerPort = edge.getPorts().get(1);
            upperPort = edge.getPorts().get(0);
        }
        lowerPort.removeEdge(edge);

        for (layer = (newRanks.get(lowerNode) + 1); layer < newRanks.get(sugy.getEndNode(edge)); layer++) {
            // create
            Vertex dummy = new Vertex();
            Port lowerDummyPort = new Port();
            Port upperDummyPort = new Port();
            LinkedList<Port> dummyList = new LinkedList<>();
            dummyList.add(lowerDummyPort);
            dummyList.add(lowerPort);
            Edge dummyEdge = new Edge(dummyList);
            sugy.assignDirection(dummyEdge, lowerNode, dummy);

            // Label/ID
            Label idd = new TextLabel("Dummy_for_" + edgeName + "_#" + layer);
            Label idlp = new TextLabel("LowerDummyPort_for_" + edgeName + "_#" + layer);
            Label idup = new TextLabel("UpperDummyPort_for_" + edgeName + "_#" + layer);
            Label ide = new TextLabel("DummyEdge_for_" + edgeName + "_L_" + (layer-1) + "_to_L_" + layer);
            dummy.getLabelManager().addLabel(idd);
            dummy.getLabelManager().setMainLabel(idd);
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
            lowerNode = dummy;
            lowerPort = upperDummyPort;
            dummyNodesLongEdges.put(dummy, refEdge);
            newRanks.put(dummy, layer);
        }

        // connect to endnode
        LinkedList<Port> dummyList = new LinkedList<>();
        dummyList.add(upperPort);
        dummyList.add(lowerPort);
        Edge dummyEdge = new Edge(dummyList);
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
}
