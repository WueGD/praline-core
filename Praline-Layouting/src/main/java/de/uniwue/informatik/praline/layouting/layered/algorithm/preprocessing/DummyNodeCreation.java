package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;

import java.util.*;

public class DummyNodeCreation {

    private SugiyamaLayouter sugy;
    private Map<Vertex, Integer> newRanks;
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Edge> dummyNodesSelfLoops;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Vertex, Vertex> nodeToLowerDummyTurningPoint;
    private Map<Vertex, Vertex> nodeToUpperDummyTurningPoint;
    private Map<Port, Port> correspondingPortsAtDummy;
    private Map<Edge, Edge> dummyEdge2RealEdge;
    private boolean[] usedRanks;
    private SortingOrder orders;

    public DummyNodeCreation (SugiyamaLayouter sugy) {
        this.sugy = sugy;
        this.newRanks = new LinkedHashMap<>();
        this.dummyNodesLongEdges = new LinkedHashMap<>();
        this.dummyNodesSelfLoops = new LinkedHashMap<>();
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
    public DummyCreationResult createDummyNodes (SortingOrder orders) {
        this.orders = orders;

        // assign ports to vertex sides for regular vertices
        assignPortsToVertexSides(sugy.getGraph().getVertices());

        // create new ranks to have place for dummyTurningNodes and dummyNodesSelfLoops by doubling all ranks
        // they can then be placed in even ranks with rank 0 and rank maxRank = empty
        for (Vertex node : sugy.getGraph().getVertices()) {
            newRanks.put(node, ((sugy.getRank(node)*2)+1));
        }
        sugy.changeRanks(newRanks);

        // initialize usedRanks
        int maxRank = sugy.getMaxRank();
        // usedRanks[r] is true if there is any node with rank r
        usedRanks = new boolean[maxRank + 2];
        boolean value = false;
        for (int i = 0; i < usedRanks.length; i++) {
            usedRanks[i] = value;
            value = !value;
        }

        // dummy nodes for portGroups
        createTurningDummyNodes();

        // dummy nodes as turning points for self loops
        createSelfLoopDummyNodes();

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

        // create dummy nodes for each edge passing a layer
        createDummyNodesForEdges();


        // assign ports to vertex sides for dummy vertices
        assignPortsToVertexSides(dummyTurningNodes.keySet());
        assignPortsToVertexSides(dummyNodesSelfLoops.keySet());
        assignPortsToVertexSides(dummyNodesLongEdges.keySet());

        sugy.changeRanks(newRanks);

        return new DummyCreationResult(dummyNodesLongEdges, dummyNodesSelfLoops, dummyTurningNodes,
                nodeToLowerDummyTurningPoint, nodeToUpperDummyTurningPoint, correspondingPortsAtDummy,
                dummyEdge2RealEdge);
    }

    private void assignPortsToVertexSides(Collection<Vertex> vertices) {
        for (Vertex node : vertices) {
            List<PortComposition> portCompositionsTop = new ArrayList<>();
            List<PortComposition> portCompositionsBottom = new ArrayList<>();
            Set<PortComposition> freePortCompositions = new LinkedHashSet<>();
            for (PortComposition portComposition : node.getPortCompositions()) {
                //for each port composition (usually a port group), compute a score that equals the number of edges
                // going upwards minus the number of edges going downwards. Depending on the sign of the score, we
                // will assign the port composition
                int score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                if (score < 0) {
                    portCompositionsBottom.add(portComposition);
                }
                else if (score > 0) {
                    portCompositionsTop.add(portComposition);
                } else {
                    freePortCompositions.add(portComposition);
                }
            }
            // handle PortCompositions with no edges by adding them to the side with fewer ports
            for (PortComposition portComposition : freePortCompositions) {
                int portsTop = PortUtils.countPorts(portCompositionsTop);
                int portsBottom = PortUtils.countPorts(portCompositionsBottom);
                if (portsTop < portsBottom || (portsTop == portsBottom && Constants.random.nextDouble() < 0.5)) {
                    portCompositionsTop.add(portComposition);
                } else {
                    portCompositionsBottom.add(portComposition);
                }
            }
            // special case: if we have a plug, there cannot be both port groups on the same side
            if (sugy.isPlug(node)) {
                repairPortSidesOfPlug(portCompositionsTop, portCompositionsBottom);
            }

            orders.getTopPortOrder().put(node, PortUtils.getPortsRecursively(portCompositionsTop));
            orders.getBottomPortOrder().put(node, PortUtils.getPortsRecursively(portCompositionsBottom));
        }
    }

    private void repairPortSidesOfPlug(List<PortComposition> portCompositionsTop,
                                       List<PortComposition> portCompositionsBottom) {
        if (portCompositionsBottom.isEmpty()) {
            int lowestScore = Integer.MAX_VALUE;
            PortComposition pcLowestScore = null;
            for (PortComposition portComposition : portCompositionsTop) {
                int score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                if (score < lowestScore) {
                    lowestScore = score;
                    pcLowestScore = portComposition;
                }
            }
            portCompositionsTop.remove(pcLowestScore);
            portCompositionsBottom.add(pcLowestScore);
        }
        if (portCompositionsTop.isEmpty()) {
            int highestScore = Integer.MIN_VALUE;
            PortComposition pcHighestScore = null;
            for (PortComposition portComposition : portCompositionsBottom) {
                int score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                if (score > highestScore) {
                    highestScore = score;
                    pcHighestScore = portComposition;
                }
            }
            portCompositionsBottom.remove(pcHighestScore);
            portCompositionsTop.add(pcHighestScore);
        }
    }

    private int countEdgesUpwardsMinusEdgesDownwards(PortComposition portComposition) {
        Vertex node = portComposition.getVertex();
        int score = 0;
        if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                score += sugy.getStartNode(edge).equals(node) ? 1 : -1;
            }
        }
        else if (portComposition instanceof PortGroup) {
            for (PortComposition subPortComposition : ((PortGroup) portComposition).getPortCompositions()) {
                score += countEdgesUpwardsMinusEdgesDownwards(subPortComposition);
            }
        }
        return score;
    }

    private void createSelfLoopDummyNodes() {
        for (Edge loopEdge : sugy.getLoopEdges()) {
            //we have split the hyperedges -> there are precisely 2 ports per edge
            List<Port> ports = sugy.getPortsOfLoopEdge(loopEdge);
            Vertex vertex = ports.get(0).getVertex();
            int vertexRank = sugy.getRank(vertex);
            boolean port0TopSide = orders.getTopPortOrder().get(vertex).contains(ports.get(0));
            boolean port1TopSide = orders.getTopPortOrder().get(vertex).contains(ports.get(1));

            Port dummyPort0 = new Port();
            Port dummyPort1 = new Port();
            Vertex dummy = new Vertex(Arrays.asList(dummyPort0, dummyPort1), Collections.singleton(new TextLabel(
                            "selfLoopDummyFor_" + loopEdge.getLabelManager().getMainLabel().toString())));

            // add everything to graph and rank dummy
            sugy.getGraph().addVertex(dummy);
            newRanks.put(dummy, port0TopSide ? vertexRank + 1 : vertexRank - 1);
            dummyNodesSelfLoops.put(dummy, loopEdge);
            correspondingPortsAtDummy.put(dummyPort0, dummyPort1);
            correspondingPortsAtDummy.put(dummyPort1, dummyPort0);

            //add new connections
            int counter = 0;
            Edge dummyEdge0 = new Edge(Arrays.asList(ports.get(0), dummyPort0), Collections.singleton(new TextLabel(
                    "selfLoopEdge_" + loopEdge.getLabelManager().getMainLabel().toString() + "_#" + counter++)));
            Edge dummyEdge1 = new Edge(Arrays.asList(ports.get(1), dummyPort1), Collections.singleton(new TextLabel(
                    "selfLoopEdge_" + loopEdge.getLabelManager().getMainLabel().toString() + "_#" + counter++)));
            sugy.getGraph().addEdge(dummyEdge0);
            sugy.getGraph().addEdge(dummyEdge1);
            sugy.assignDirection(dummyEdge0);
            sugy.assignDirection(dummyEdge1);
            sugy.getDummyEdge2RealEdge().put(dummyEdge0, loopEdge);
            sugy.getDummyEdge2RealEdge().put(dummyEdge1, loopEdge);

            //if the ports are on different sides, we need more than one dummy node to route the self loop
            if (port0TopSide != port1TopSide) {
                Port dummyPort2 = new Port();
                Port dummyPort3 = new Port();
                Vertex additionalDummy = new Vertex(Arrays.asList(dummyPort2, dummyPort3),
                        Collections.singleton(new TextLabel(
                                "additionalSelfLoopDummyFor_" + loopEdge.getLabelManager().getMainLabel().toString())));

                // add everything to graph and rank dummy
                sugy.getGraph().addVertex(additionalDummy);
                newRanks.put(additionalDummy, port1TopSide ? vertexRank + 1 : vertexRank - 1);
                dummyNodesSelfLoops.put(additionalDummy, loopEdge);

                //change and add connections
                dummyEdge1.removePort(ports.get(1));
                dummyEdge1.addPort(dummyPort2);
                Edge dummyEdge2 = new Edge(Arrays.asList(ports.get(1), dummyPort3), Collections.singleton(new TextLabel(
                        "selfLoopEdge_" + loopEdge.getLabelManager().getMainLabel().toString() + "_#" + counter++)));
                sugy.getGraph().addEdge(dummyEdge2);
                sugy.assignDirection(dummyEdge2);
                sugy.getDummyEdge2RealEdge().put(dummyEdge2, loopEdge);
            }
        }
    }

    private void createDummyNodesForEdges() {
        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            int dist = 0;
            dist = (newRanks.get(sugy.getEndNode(edge))) - (newRanks.get(sugy.getStartNode(edge)));
            if (dist > 1) {
                createAllDummyNodesForEdge(edge);
            }
        }
    }

    private void createTurningDummyNodes() {
        for (Vertex node : new ArrayList<>(sugy.getGraph().getVertices())) {
            for (Port bottomPort : orders.getBottomPortOrder().get(node)) {
                for (Edge edge : bottomPort.getEdges()) {
                    //check if edge points upwards -> if yes insert turning dummy
                    if (sugy.getStartNode(edge).equals(node)) {
                        Vertex lowerDummyTurningNode =
                                getDummyTurningNodeForVertex(node, true, (newRanks.get(node) - 1));
                        splitEdgeByTurningDummyNode(edge, lowerDummyTurningNode);
                        usedRanks[(newRanks.get(node) - 1)] = true;
                    }
                }
            }
            for (Port topPort : orders.getTopPortOrder().get(node)) {
                for (Edge edge : topPort.getEdges()) {
                    //check if edge points downwards -> if yes insert turning dummy
                    if (!sugy.getStartNode(edge).equals(node)) {
                        Vertex upperDummyTurningNode =
                                getDummyTurningNodeForVertex(node, false, (newRanks.get(node) + 1));
                        splitEdgeByTurningDummyNode(edge, upperDummyTurningNode);
                        usedRanks[(newRanks.get(node) + 1)] = true;
                    }
                }
            }
        }
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
