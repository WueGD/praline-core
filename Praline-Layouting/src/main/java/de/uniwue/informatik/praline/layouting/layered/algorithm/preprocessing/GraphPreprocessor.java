package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.ImplicitCharacteristics;

import java.util.*;

public class GraphPreprocessor {
    private final SugiyamaLayouter sugy;

    public GraphPreprocessor(SugiyamaLayouter sugiyamaLayouter) {
        this.sugy = sugiyamaLayouter;
    }

    public void construct() {
        //handle edge bundles
        handleEdgeBundles();
        // handle Port if it has no Vertex
        handlePortsWithoutNode();
        // vertices without ports
        handleNodesWithoutPort();
        // handle Edge if connected to more than two Ports
        handleHyperEdges();
        // handle Port if it has more than one Edge
        handlePortsWithMultipleEdges();
        // handle VertexGroups
        handleVertexGroups();
        // handle Edge if both Ports have same Vertex
        handleLoopEdges();
    }


    private void handleEdgeBundles() {
        for (EdgeBundle edgeBundle : sugy.getGraph().getEdgeBundles()) {
            handleEdgeBundle(edgeBundle);
        }
    }

    private void handleEdgeBundle(EdgeBundle edgeBundle) {
        //save current edge bundle
        sugy.getOriginalEdgeBundles().put(edgeBundle, new ArrayList<>(edgeBundle.getContainedEdges()));
        //first find all ports of the bundle
        Map<Vertex, Set<Port>> vertices2bundlePorts = new LinkedHashMap<>();
        //all ports in the bundle should end up next to the other -> also include recursively contained ones
        for (Edge edge : edgeBundle.getAllRecursivelyContainedEdges()) {
            for (Port port : edge.getPorts()) {
                Vertex vertex = port.getVertex();
                vertices2bundlePorts.putIfAbsent(vertex, new LinkedHashSet<>());
                vertices2bundlePorts.get(vertex).add(port);
            }
        }
        //create a port group for the ports of the bundle at each vertex
        for (Vertex vertex : vertices2bundlePorts.keySet()) {
            Set<Port> ports = vertices2bundlePorts.get(vertex);
            Map<PortGroup, List<PortComposition>> group2bundlePorts = new LinkedHashMap<>();
            PortGroup nullGroup = new PortGroup(); //dummy object for ports without port group
            //for this first find the containing port groups
            for (Port port : ports) {
                PortGroup portGroup = port.getPortGroup() == null ? nullGroup : port.getPortGroup();
                group2bundlePorts.putIfAbsent(portGroup, new ArrayList<>());
                group2bundlePorts.get(portGroup).add(port);
            }
            //and now create these port groups
            for (PortGroup portGroup : group2bundlePorts.keySet()) {
                List<PortComposition> portCompositions = group2bundlePorts.get(portGroup);
                PortGroup portGroupForEdgeBundle = new PortGroup(null, false);
                if (portGroup == nullGroup) {
                    //if not port group add it directly to the vertex
                    vertex.addPortComposition(portGroupForEdgeBundle);
                }
                else {
                    portGroup.addPortComposition(portGroupForEdgeBundle);
                }
                PortUtils.movePortCompositionsToPortGroup(portCompositions, portGroupForEdgeBundle);
                sugy.getDummyPortGroupsForEdgeBundles().add(portGroupForEdgeBundle);
            }
        }
        //do this recursively for contained edge bundles
        for (EdgeBundle containedEdgeBundle : edgeBundle.getContainedEdgeBundles()) {
            handleEdgeBundle(containedEdgeBundle);
        }
    }

    private void handlePortsWithoutNode() {
        for (Edge edge : sugy.getGraph().getEdges()) {
            for (Port port : edge.getPorts()) {
                if (port.getVertex() == null) {
                    Vertex dummyNode = new Vertex();
                    sugy.getGraph().addVertex(dummyNode);
                    createMainLabel("addNodeFor" + port , dummyNode);
                    dummyNode.addPortComposition(port);
                    sugy.addDummyNodeForNodelessPorts(dummyNode);
                }
            }
        }
    }

    private void handleNodesWithoutPort() {
        for (Vertex vertex : sugy.getGraph().getVertices()) {
            if (vertex.getPorts().isEmpty()) {
                Port dummyPort = new Port(null, Collections.singleton(new TextLabel("dummyPortForVertexWithoutPort")));
                vertex.addPortComposition(dummyPort);
                sugy.addDummyPortsForNodesWithoutPort(dummyPort);
            }
        }
    }

    private void handleHyperEdges() {
        int index1 = 0;
        int index2 = 0;

        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            if (edge.getPorts().size() > 2) {
                //for hyperedges of degree >= 3 we add a central representative vertex which is adjacent with a
                // "normal" degree 2 edge to all original end points of this hyperedge
                Vertex representative = new Vertex();
                createMainLabel(("EdgeRep_for_" + edge + "_#" + index1++), representative);
                index2 = 0;
                for (Port port : edge.getPorts()) {
                    Port p = new Port();
                    createMainLabel(("HE_PortRep_for_" + port + "_#" + index1 + "-" + index2), p);
                    representative.addPortComposition(p);
                    List<Port> ps = new LinkedList<>();
                    ps.add(p);
                    ps.add(port);
                    Edge e = new Edge(ps);
                    createMainLabel(("HE_AddEdge_#" + index1 + "-" + index2++), e);
                    sugy.getGraph().addEdge(e);
                    sugy.getHyperEdgeParts().put(e, representative);
                }
                sugy.getGraph().addVertex(representative);
                sugy.getHyperEdges().put(representative, edge);
            }
            else{
                //edges of degree 0 or 1 get dummy nodes as endpoints to have degree 2.
                // In the end these dummy nodes will be removed and only the edge paths remain
                if (edge.getPorts().size() == 0) {
                    System.out.println("Warning! Edge " + edge + " has no endpoints.");
                }
                else if (edge.getPorts().size() == 1) {
                    System.out.println("Warning! Edge " + edge + " has only one endpoint.");
                }
                while (edge.getPorts().size() < 2) {
                    Vertex dummyNode = new Vertex();
                    sugy.getGraph().addVertex(dummyNode);
                    createMainLabel("addNodeForEdge_" + edge, dummyNode);
                    sugy.addDummyNodeForEdgesOfDeg1or0(dummyNode);
                    Port dummyPort = new Port();
                    dummyNode.addPortComposition(dummyPort);
                    createMainLabel("", dummyPort);
                    //add this new dummy port as a second port to this edge
                    edge.addPort(dummyPort);
                }
            }
        }
        for (Edge edge : sugy.getHyperEdges().values()) {
            for (Port port : new ArrayList<>(edge.getPorts())) {
                port.removeEdge(edge);
            }
            sugy.getGraph().removeEdge(edge);
        }
    }

    private void handleVertexGroups() {
        int index1 = 0;
        int index2 = 0;
        Set<VertexGroup> connectors = new LinkedHashSet<>();
        for (VertexGroup vertexGroup : sugy.getGraph().getVertexGroups()) {
            if (ImplicitCharacteristics.isConnector(vertexGroup, sugy.getGraph())) {
                connectors.add(vertexGroup);
            }
            for (Vertex containedVertex : vertexGroup.getContainedVertices()) {
                if (ImplicitCharacteristics.isDeviceVertex(containedVertex, sugy.getGraph())) {
                    sugy.getDeviceVertices().add(containedVertex);
                }
            }
        }

        for (VertexGroup group : new ArrayList<>(sugy.getGraph().getVertexGroups())) {
            boolean stickTogether = false;
            boolean hasPortPairings = false;
            Map<Port, Set<Port>> allPairings = new LinkedHashMap<>();
            if (group.getContainedVertices().size() == (group.getTouchingPairs().size() + 1)) {
                stickTogether = true;

                // fill allPairings
                fillAllPairings(allPairings, group);

//                // check for hasPortPairings
//                // this is the case if in one allPairingsPortSet exist two ports with outgoing edges to notGroupNodes
//                hasPortPairings = hasOutgoingPairings(allPairings, group);
                //EDIT JZ 2020/09/24: we don't want to exclude ports without edges any more -> simpler check
                hasPortPairings = !allPairings.isEmpty();
            }

//            Map<Edge, Port> outgoingEdges = new LinkedHashMap<>();
            List<Vertex> groupVertices = group.getAllRecursivelyContainedVertices();
//            fillOutgoingEdges(outgoingEdges, groupVertices);
            Vertex representative = new Vertex();

            // create main Label
//            String idV = ("GroupRep_for_" + group + "_#" + index1++);
            String idV = ("R#" + index1++);
//            if (stickTogether) idV = ("PlugRep_for_" + groupLabelText + "_#" + (index1-1));
            createMainLabel(idV, representative);
            index2 = 0;

            sugy.getGraph().addVertex(representative);
            Map<Port, Port> originalPort2representative = new LinkedHashMap<>();

            for (Vertex containedVertex : groupVertices) {
                for (Port port : containedVertex.getPorts()) {
                    if (!sugy.getDeviceVertices().contains(containedVertex) || !port.getEdges().isEmpty()) {
                        // create new port at unification vertex and remove old one on original vertex,
                        // hang the edges from the old to the new port
                        Port replacePort = new Port();
                        createMainLabel(
                                ("VG_PortRep_for_" + port + "_#" + index1 +
                                        "-" + index2), replacePort);

                        for (Edge edge : new ArrayList<>(port.getEdges())) {
                            edge.removePort(port);
                            edge.addPort(replacePort);
                        }


                        if (stickTogether) {
                            sugy.getReplacedPorts().put(replacePort, port);
                            originalPort2representative.put(port, replacePort);
                        } else {
                            representative.addPortComposition(replacePort);
                        }
                    }
                }
            }

            // create portGroups if stickTogether
            if (stickTogether) {
                for (Vertex groupNode : group.getContainedVertices()) {
                    PortGroup replacePortGroup = new PortGroup();
                    representative.addPortComposition(keepPortGroupsRecursive(replacePortGroup,
                            groupNode.getPortCompositions(), originalPort2representative));
                    sugy.getOrigVertex2replacePortGroup().put(groupNode, replacePortGroup);
                }
                if (connectors.contains(group)) {
                    keepPortPairings(originalPort2representative, allPairings);
                    sugy.getPlugs().put(representative, group);
                } else {
                    sugy.getVertexGroups().put(representative, group);
                }
            } else {
                sugy.getVertexGroups().put(representative, group);
            }

            //remove group and its vertices
            sugy.getGraph().removeVertexGroup(group);
            for (Vertex groupVertex : groupVertices) {
                sugy.getGraph().removeVertex(groupVertex);
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
                        sugy.getKeptPortPairings().put(p0, portRepMap.get(port));
                        sugy.getKeptPortPairings().put(portRepMap.get(port), p0);
                    }
                }
            }
        }
    }

    private void handlePortsWithMultipleEdges() {
        int index1 = 0;

        Map<PortGroup, Port> replaceGroups = new LinkedHashMap<>();
        for (Vertex node : sugy.getGraph().getVertices()) {
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
                        createMainLabel(("AddPort_for_" + port + "_#" + index1++), addPort);
                        sugy.getReplacedPorts().put(addPort, port);
                    }
                    replaceGroups.put(repGroup, port);
                    sugy.getMultipleEdgePort2replacePorts().put(port, PortUtils.getPortsRecursively(repGroup));
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
                        PortPairing replacementPortPairing = new PortPairing(otherPort, arbitraryPortOfTheNewGroup);
                        node.getVertexGroup().addPortPairing(replacementPortPairing);
                        sugy.getReplacedPortPairings().put(replacementPortPairing, portPairing);
                    }
                }
                for (PortPairing portPairing : toRemove) {
                    node.getVertexGroup().removePortPairing(portPairing);
                }
            }
        }
    }

    private void handleLoopEdges() {
        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            //we have split all hyperedges with >= 3 ports, so it suffices to consider the first two ports
            Port port0 = edge.getPorts().get(0);
            Port port1 = edge.getPorts().get(1);
            if (port0.getVertex().equals(port1.getVertex())) {
                sugy.getLoopEdges().computeIfAbsent(port0.getVertex(), k -> new LinkedHashSet<>()).add(edge);
                sugy.getLoopEdge2Ports().put(edge, Arrays.asList(port0, port1));

                //remove loop edge and ports
                sugy.getGraph().removeEdge(edge);
            }
        }
    }

    private void createMainLabel (String id, LabeledObject lo) {
        Label newLabel = new TextLabel(id);
        lo.getLabelManager().addLabel(newLabel);
        lo.getLabelManager().setMainLabel(newLabel);
    }
}