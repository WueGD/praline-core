package de.uniwue.informatik.praline.datastructure.utils;

import de.uniwue.informatik.praline.datastructure.graphs.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PortUtils {

    public static List<Port> getPortsRecursively(PortComposition pc) {
        List<Port> appendTo = new ArrayList<>();
        getPortsRecursively(pc, appendTo);
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

    public static PortGroup getLeastCommonAncestor(Port port0, Port port1) {
        List<PortGroup> containmentHierarchy0 = getContainmentHierarchy(port0);
        List<PortGroup> containmentHierarchy1 = getContainmentHierarchy(port1);

        for (PortGroup portGroup0 : containmentHierarchy0) {
            for (PortGroup portGroup1 : containmentHierarchy1) {
                if (portGroup0.equals(portGroup1)) {
                    return portGroup0;
                }
            }
        }

        return null;
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
    public static List<PortGroup> getContainmentHierarchy(Port port) {
        List<PortGroup> containmentHierarchy = new ArrayList<>();
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

    public static Port getPortAtVertex(Edge edge, Vertex vertex) {
        for (Port port : edge.getPorts()) {
            if (vertex.getPorts().contains(port)) {
                return port;
            }
        }
        return null;
    }
}
