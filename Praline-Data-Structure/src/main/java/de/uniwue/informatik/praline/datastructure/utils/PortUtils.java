package de.uniwue.informatik.praline.datastructure.utils;

import de.uniwue.informatik.praline.datastructure.graphs.*;

import java.util.*;

public class PortUtils {

    public static boolean contains(VertexGroup vertexGroup, Port port) {
        if (port == null) {
            return vertexGroup == null;
        }
        return vertexGroup.getAllRecursivelyContainedVertices().contains(port.getVertex());
    }

    public static List<Edge> getEdges(Vertex vertex) {
        List<Edge> edges = new ArrayList<>();
        for (Port port : vertex.getPorts()) {
            edges.addAll(port.getEdges());
        }
        return edges;
    }

    public static Collection<Port> getOtherEndPoints(Edge edge, Port port) {
        Set<Port> endpoints = new LinkedHashSet<>(edge.getPorts());
        endpoints.remove(port);
        return endpoints;
    }

    /**
     *
     * @param edge
     * @param port
     * @return
     *      some other endpoint (not being port) of this edge or null if there is no other endpoint
     */
    public static Port getOtherEndPoint(Edge edge, Port port) {
        Collection<Port> otherEndPoints = getOtherEndPoints(edge, port);
        if (otherEndPoints.contains(port)) {
            otherEndPoints.remove(port);
        }
        if (otherEndPoints.isEmpty()) {
            return null;
        }
        return otherEndPoints.iterator().next();
    }

    public static VertexGroup getTopLevelVertexGroup(Vertex vertex) {
        VertexGroup vertexGroup = vertex.getVertexGroup();
        VertexGroup topLevelVertexGroup = null;
        while (vertexGroup != null) {
            topLevelVertexGroup = vertexGroup;
            vertexGroup = vertexGroup.getVertexGroup();
        }
        return topLevelVertexGroup;
    }

    public static EdgeBundle getTopLevelEdgeBundle(Edge edge) {
        EdgeBundle edgeBundle = edge.getEdgeBundle();
        EdgeBundle topLevelEdgeBundle = null;
        while (edgeBundle != null) {
            topLevelEdgeBundle = edgeBundle;
            edgeBundle = edgeBundle.getEdgeBundle();
        }
        return topLevelEdgeBundle;
    }

    public static List<Port> getAdjacentPorts(Vertex vertex) {
        List<Port> adjPorts = new ArrayList<>();
        for (Edge edge : getEdges(vertex)) {
            for (Port port : edge.getPorts()) {
                if (!port.getVertex().equals(vertex)) {
                    adjPorts.add(port);
                }
            }
        }
        return adjPorts;
    }

    public static Set<Vertex> getAdjacentVertices(Vertex vertex) {
        Set<Vertex> adjVertices = new LinkedHashSet<>();
        for (Port adjacentPort : getAdjacentPorts(vertex)) {
            adjVertices.add(adjacentPort.getVertex());
        }
        return adjVertices;
    }

    public static List<Port> getPortsRecursively(PortComposition pc) {
        return getPortsRecursively(Collections.singleton(pc));
    }

    public static List<Port> getPortsRecursively(Collection<PortComposition> pcs) {
        List<Port> appendTo = new ArrayList<>();
        for (PortComposition pc : pcs) {
            getPortsRecursively(pc, appendTo);
        }
        return appendTo;
    }

    public static void getPortsRecursively(PortComposition pc, Collection<Port> appendTo) {
        if (pc instanceof PortGroup) {
            for (PortComposition lowerPc : ((PortGroup)pc).getPortCompositions()) {
                getPortsRecursively(lowerPc, appendTo);
            }
        } else if (pc instanceof Port) {
            appendTo.add(((Port)pc));
        }
    }

    public static PortComposition getLeastCommonAncestor(PortComposition port0, PortComposition port1) {
        if (port0 == null || port1 == null) {
            return null;
        }

        List<PortComposition> containmentHierarchy0 = getContainmentHierarchy(port0);
        List<PortComposition> containmentHierarchy1 = getContainmentHierarchy(port1);

        for (PortComposition portComp0 : containmentHierarchy0) {
            for (PortComposition portComp1 : containmentHierarchy1) {
                if (portComp0.equals(portComp1)) {
                    return portComp0;
                }
            }
        }

        return null;
    }

    public static PortComposition getLeastCommonAncestor(Collection<Port> ports) {
        if (ports == null || ports.isEmpty()) {
            return null;
        }
        PortComposition leastCommonAncestor = ports.iterator().next();
        for (Port port : ports) {
            leastCommonAncestor = getLeastCommonAncestor(port, leastCommonAncestor);
        }
        return leastCommonAncestor;
    }

    /**
     * @param pc
     * @return
     *      A {@link PortComposition} directly contained in a vertex and no other port group.
     *      If the input fulfills this property already, the input is returned.
     */
    public static PortComposition getTopMostAncestor(PortComposition pc) {
        PortComposition topMostAncestor = pc;
        while (topMostAncestor.getPortGroup() != null) {
            topMostAncestor = topMostAncestor.getPortGroup();
        }
        return topMostAncestor;
    }

    /**
     *
     * @param portCompositions
     * @param portGroup
     *      if null then the port compositions are set as direct children of its vertex
     */
    public static void movePortCompositionsToPortGroup(Collection<? extends PortComposition> portCompositions,
                                                       PortGroup portGroup) {
        for (PortComposition portComposition : new ArrayList<>(portCompositions)) {
            movePortCompositionToPortGroup(portComposition, portGroup);
        }
    }

    /**
     *
     * @param portComposition
     * @param portGroup
     *      if null then the port composition is set as direct child of its vertex
     */
    public static void movePortCompositionToPortGroup(PortComposition portComposition, PortGroup portGroup) {
        Vertex vertex = portComposition.getVertex();
        if (portComposition.getPortGroup() != null) {
            portComposition.getPortGroup().removePortComposition(portComposition);
        }
        else if (portComposition.getVertex() != null) {
            vertex.removePortComposition(portComposition);
        }

        if (portGroup == null && vertex != null) {
            vertex.addPortComposition(portComposition);
        }
        else if (portGroup != null) {
            portGroup.addPortComposition(portComposition);
        }
    }

    public static boolean areInTheSamePortGroup(Port port0, Port port1) {
        if (!port0.getVertex().equals(port1.getVertex())) {
            return false;
        }

        if (port0.getPortGroup() == null && port1.getPortGroup() == null) {
            return true;
        }

        if (port0.getPortGroup() == null || port1.getPortGroup() == null) {
            return false;
        }

        return port0.getPortGroup().equals(port1.getPortGroup());
    }

    /**
     * lowest level first, uppermost level last
     * empty if in no port group
     *
     * @param port
     * @return
     */
    public static List<PortComposition> getContainmentHierarchy(PortComposition port) {
        List<PortComposition> containmentHierarchy = new ArrayList<>();
        PortComposition curr = port;
        while (curr.getPortGroup() != null) {
            PortGroup upperLevelPortGroup = curr.getPortGroup();
            containmentHierarchy.add(upperLevelPortGroup);
            curr = upperLevelPortGroup;
        }
        return containmentHierarchy;
    }

    /**
     *
     * @param portGroup
     *      if null then the top-level port group (or the port itself) containing port is returned
     * @param port
     * @return
     */
    public static PortComposition getTopMostChildContainingThisPort(PortGroup portGroup, Port port) {
        Collection<PortComposition> portCompositionCandidates = portGroup == null ?
                port.getVertex().getPortCompositions() : portGroup.getPortCompositions();
        for (PortComposition portCompositionCandidate : portCompositionCandidates) {
            if (containsPortRecursively(portCompositionCandidate, port)) {
                return portCompositionCandidate;
            }
        }
        return null;
    }

    public static boolean containsPortRecursively(PortComposition portComposition, Port port) {
        if (portComposition instanceof Port) {
            return portComposition.equals(port);
        }
        else if (portComposition instanceof PortGroup) {
            for (PortComposition containedPortComposition : ((PortGroup) portComposition).getPortCompositions()) {
                if (containsPortRecursively(containedPortComposition, port)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Collection<Port> getAllAdjacentPorts(Port port) {
        ArrayList<Port> adjacentPorts = new ArrayList<>();
        for (Edge edge : port.getEdges()) {
            for (Port otherPort : edge.getPorts()) {
                if (!otherPort.equals(port)) {
                    adjacentPorts.add(otherPort);
                }
            }
        }
        return adjacentPorts;
    }

    /**
     * TODO: this functionality is used throughout the code quite often! However, there we do not call this method.
     * TODO: So replace all these code duplicates on many places by a call to this method.
     *
     *
     * @param port
     * @return
     *      some adjacent port (null if there is no) -- for all adjacent ports call
     *      {@link PortUtils#getAllAdjacentPorts(Port)}.
     */
    public static Port getAdjacentPort(Port port) {
        Collection<Port> allAdjacentPorts = getAllAdjacentPorts(port);
        if (allAdjacentPorts.isEmpty()) {
            return null;
        }

        return allAdjacentPorts.iterator().next();
    }

    public static boolean isPaired(Port port) {
        return getPairedPort(port) != null;
    }

    public static Port getPairedPort(Port port) {
        VertexGroup vertexGroup = port.getVertex().getVertexGroup();
        if (vertexGroup != null) {
            for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                for (Port portPairingPort : portPairing.getPorts()) {
                    if (portPairingPort.equals(port)) {
                        return getOtherPortOfPortPairing(portPairing, port);
                    }
                }
            }
        }
        return null;
    }

    public static Port getOtherPortOfPortPairing(PortPairing pp, Port p) {
        if (pp.getPort0().equals(p)) {
            return pp.getPort1();
        }
        return pp.getPort0();
    }

    public static PortPairing getPortPairing(Port port0, Port port1, VertexGroup vertexGroup) {
        for (PortPairing portPairing : vertexGroup.getPortPairings()) {
            if (portPairing.getPorts().contains(port0) && portPairing.getPorts().contains(port1)) {
                return portPairing;
            }
        }
        return null;
    }

    public static PortPairing getPortPairing(Port port, VertexGroup vertexGroup) {
        return getPortPairing(port, port, vertexGroup);
    }

    public static Port getPortAtVertex(Edge edge, Vertex vertex) {
        for (Port port : edge.getPorts()) {
            if (vertex.getPorts().contains(port)) {
                return port;
            }
        }
        return null;
    }

    public static int countPorts(Collection<PortComposition> portCompositionsTop) {
        int sum = 0;
        for (PortComposition portComposition : portCompositionsTop) {
            sum += PortUtils.getPortsRecursively(portComposition).size();
        }
        return sum;
    }

    /**
     *
     * @param portComposition
     * @return
     *      if there is no port with an edge, it will return some port (without edges) and if also not existent, then
     *      null.
     */
    public static Port findPortWithEdgesIfExistent(PortComposition portComposition) {
        Port port = null;
        if (portComposition instanceof Port) {
            port = (Port) portComposition;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                port = findPortWithEdgesIfExistent(member);
                if (port != null && !port.getEdges().isEmpty()) break;
            }
        }
        return port;
    }

    /**
     *
     * @param ports
     * @param portGroups
     *      may be nested (also all contained port groups are checked
     * @return
     */
    public static boolean arrangmentOfPortsIsValidAccordingToPortGroups(List<Port> ports,
                                                                        Collection<PortComposition> portGroups) {
        for (PortComposition portComposition : portGroups) {
            if (portComposition instanceof PortGroup) {
                List<Integer> indicesOfPorts = new ArrayList<>();
                for (Port port : getPortsRecursively(portComposition)) {
                    indicesOfPorts.add(ports.indexOf(port));
                }
                if (!((PortGroup) portComposition).isOrdered()) {
                    //if the order is not fixed, they indices must only appear somewhere in the block so we can sort
                    // them, otherwise they must be in order also internally and we must not sort them
                    Collections.sort(indicesOfPorts);
                }
                //check if the indices form an contiguous block
                int expectedIndex = indicesOfPorts.get(0);
                for (Integer index : indicesOfPorts) {
                    if (index != expectedIndex) {
                        return false;
                    }
                    ++expectedIndex;
                }
                //recursively check for all children
                if (!arrangmentOfPortsIsValidAccordingToPortGroups(
                        ports, ((PortGroup) portComposition).getPortCompositions())) {
                    return false;
                }
            }
        }
        return true;
    }

}
