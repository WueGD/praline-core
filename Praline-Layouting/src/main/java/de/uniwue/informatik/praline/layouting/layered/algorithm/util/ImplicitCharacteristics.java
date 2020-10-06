package de.uniwue.informatik.praline.layouting.layered.algorithm.util;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.util.Collection;
import java.util.LinkedHashSet;

public class ImplicitCharacteristics {

    public static final String SPLICE_LABEL = "e9800998ecf8427e";

    public static boolean isSplice(Vertex vertex, Graph graph) {
        return vertex.getPortCompositions().size() == 1
                && vertex.getPortCompositions().get(0) instanceof Port
                && ((TextLabel) vertex.getLabelManager().getMainLabel()).getInputText().equals(SPLICE_LABEL)
                && getNumberOfVertexGroupsContainingVertex(vertex, graph) == 0;
    }

    public static boolean isSoloVertex(Vertex vertex, Graph graph) {
        return hasOnlyPortGroupsOnTheTopLevel(vertex)
                && getNumberOfVertexGroupsContainingVertex(vertex, graph) == 0;
    }

    public static boolean isConnectorVertex(Vertex vertex, Graph graph) {
        return hasOnlyPortGroupsOnTheTopLevel(vertex)
                && getNumberOfVertexGroupsContainingVertex(vertex, graph) == 1
                && isConnector(vertex.getVertexGroup(), graph);
    }

    public static boolean isDeviceVertex(Vertex vertex, Graph graph) {
        if (vertex.getVertexGroup() == null) {
            return false; //this extra check is only done to prevent null pointer exception with the next local variable
        }
        LinkedHashSet<Vertex> otherVerticesOfDevice = new LinkedHashSet<>(vertex.getVertexGroup().getContainedVertices());
        otherVerticesOfDevice.remove(vertex);

        return hasOnlyPortGroupsOnTheTopLevel(vertex)
                && getNumberOfVertexGroupsContainingVertex(vertex, graph) == 1
                && isDeviceConnector(vertex.getVertexGroup(), graph)
                && !vertexHasPortWithPortPairingAndEdge(vertex.getVertexGroup(), vertex)
                && (findVertexThatConnectsToAllOthers(vertex.getVertexGroup()) == vertex
                    || (vertex.getVertexGroup().getContainedVertices().size() == 2
                        && isDeviceConnectorVertex(otherVerticesOfDevice.iterator().next(), graph)));

    }

    public static boolean isDeviceConnectorVertex(Vertex vertex, Graph graph) {
        return hasOnlyPortGroupsOnTheTopLevel(vertex)
                && getNumberOfVertexGroupsContainingVertex(vertex, graph) == 1
                && isDeviceConnector(vertex.getVertexGroup(), graph)
                && (findVertexThatConnectsToAllOthers(vertex.getVertexGroup()) != vertex
                    || (vertex.getVertexGroup().getContainedVertices().size() == 2
                        && vertexHasPortWithPortPairingAndEdge(vertex.getVertexGroup(), vertex)));
    }

    public static VertexType getVertexType(Vertex vertex, Graph graph) {
        if (isSplice(vertex, graph)) {
            return VertexType.SPLICE;
        }
        if (isSoloVertex(vertex, graph)) {
            return VertexType.SOLO_VERTEX;
        }
        if (isConnectorVertex(vertex, graph)) {
            return VertexType.CONNECTOR_VERTEX;
        }
        if (isDeviceVertex(vertex, graph)) {
            return VertexType.DEVICE_VERTEX;
        }
        if (isDeviceConnectorVertex(vertex, graph)) {
            return VertexType.DEVICE_CONNECTOR_VERTEX;
        }
        return VertexType.UNDEFINED_VERTEX;
    }

    public static boolean isOfType(VertexType vertexType, Vertex vertex, Graph graph) {
        return vertexType == getVertexType(vertex, graph);
    }

    /**
     *
     * @param vertexGroup
     * @param graph
     * @return
     *      null if there is no DEVICE_VERTEX in this {@link VertexGroup}
     */
    public static Vertex getDeviceVertex(VertexGroup vertexGroup, Graph graph) {
        for (Vertex vertex : vertexGroup.getAllRecursivelyContainedVertices()) {
            if (isDeviceVertex(vertex, graph)) {
                return vertex;
            }
        }
        return null;
    }



    public static boolean isOfType(VertexGroupType vertexGroupType, VertexGroup vertexGroup, Graph graph) {
        return vertexGroupType == getVertexGroupType(vertexGroup, graph);
    }

    public static VertexGroupType getVertexGroupType(VertexGroup vertexGroup, Graph graph) {
        if (isConnector(vertexGroup, graph)) {
            return VertexGroupType.CONNECTOR;
        }
        if (isDeviceConnector(vertexGroup, graph)) {
            return VertexGroupType.DEVICE_CONNECTOR;
        }
        isDeviceConnector(vertexGroup, graph);
        return VertexGroupType.UNDEFINED;
    }

    public static boolean isConnector(VertexGroup vertexGroup, Graph graph) {
        return isUniqueConnectionVertexGroup(vertexGroup, graph)
                && vertexGroup.getContainedVertices().size() == 2
                && vertexGroup.getTouchingPairs().size() == 1
                && allVerticesHavePortWithPortPairingAndEdge(vertexGroup);
    }

    public static boolean isDeviceConnector(VertexGroup vertexGroup, Graph graph) {
        Vertex centralVertex = findVertexThatConnectsToAllOthers(vertexGroup);

        //special case: if it is just two, both could be the central vertex i.e. device
        if (vertexGroup.getContainedVertices().size() == 2 && centralVertex!= null) {
            return isDeviceConnector(vertexGroup.getContainedVertices().get(0), vertexGroup, graph)
                    || isDeviceConnector(vertexGroup.getContainedVertices().get(1), vertexGroup, graph);
        }

        return isDeviceConnector(centralVertex, vertexGroup, graph);
    }

    /**
     * a regular port is any port that is not a port of a splice or a port of a device in a port pairing (but it may
     * be a "unpaired" port  of a device)
     *
     * @param port
     * @param graph
     * @return
     */
    public static boolean isRegularPort(Port port, Graph graph) {
        Vertex vertex = port.getVertex();
        if (isSplice(vertex, graph)) {
            return false;
        }
        if (isDeviceVertex(vertex, graph) && isInAPortPairing(vertex.getVertexGroup(), port)) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param centralVertex
     *      candidate to be the device; if it is null, then this method will return false
     * @param vertexGroup
     * @param graph
     * @return
     */
    private static boolean isDeviceConnector(Vertex centralVertex, VertexGroup vertexGroup, Graph graph) {
        return isUniqueConnectionVertexGroup(vertexGroup, graph)
                && vertexGroup.getContainedVertices().size() >= 2
                && centralVertex != null
                && vertexGroup.getTouchingPairs().size() == vertexGroup.getContainedVertices().size() - 1
                && verticesOnlyConnectToTheCentralVertex(vertexGroup, centralVertex)
                && !vertexHasPortWithPortPairingAndEdge(vertexGroup, centralVertex);
    }

    private static boolean hasOnlyPortGroupsOnTheTopLevel(Vertex vertex) {
        for (PortComposition portComposition : vertex.getPortCompositions()) {
            if (!(portComposition instanceof PortGroup)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param vertexGroup
     * @return
     *      a {@link Vertex} that connects to all other vertices of the input {@link VertexGroup} or null if such a
     *      {@link Vertex} does not exist
     */
    private static Vertex findVertexThatConnectsToAllOthers(VertexGroup vertexGroup) {
        //find a vertex that connects to all others
        Vertex vertexThatConnectsToAllOthers = null;
        for (Vertex vertex : vertexGroup.getContainedVertices()) {
            boolean connectsToAllOthers = true;
            for (Vertex otherVertex : vertexGroup.getContainedVertices()) {
                if (vertex == otherVertex) {
                    continue;
                }
                if (!touchingPairExists(vertexGroup, vertex, otherVertex)) {
                    connectsToAllOthers = false;
                    break;
                }
            }
            if (connectsToAllOthers) {
                vertexThatConnectsToAllOthers = vertex;
            }
        }

        return vertexThatConnectsToAllOthers;
    }

    private static boolean verticesOnlyConnectToTheCentralVertex(VertexGroup vertexGroup, Vertex centralVertex) {
        for (Vertex vertex : vertexGroup.getContainedVertices()) {
            if (vertex == centralVertex) {
                continue;
            }
            if (getNumberOfTouchingPairsVertexIsInvolved(vertexGroup, vertex) != 1 || !touchingPairExists(vertexGroup,
                    centralVertex, vertex)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allVerticesHavePortWithPortPairingAndEdge(VertexGroup vertexGroup) {
        for (Vertex vertex : vertexGroup.getContainedVertices()) {
            if (!vertexHasPortWithPortPairingAndEdge(vertexGroup, vertex)) {
                return false;
            }
        }
        return true;
    }

    private static boolean vertexHasPortWithPortPairingAndEdge(VertexGroup vertexGroup, Vertex vertex) {
        for (Port port : vertex.getPorts()) {
            if (hasEdge(port) && isInAPortPairing(vertexGroup, port)) {
                return true;
            }
        }
        return false;
    }

    private static boolean allVerticesHaveEdge(Collection<Vertex> vertices) {
        for (Vertex vertex : vertices) {
            if (!hasEdge(vertex)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasEdge(Vertex vertex) {
        for (Port port : vertex.getPorts()) {
            if (hasEdge(port)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEdge(Port port) {
        return !port.getEdges().isEmpty();
    }

    private static boolean isInAPortPairing(VertexGroup vertexGroup, Port port) {
        for (PortPairing portPairing : vertexGroup.getPortPairings()) {
            if (portPairing.getPorts().contains(port)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUniqueConnectionVertexGroup(VertexGroup vertexGroup, Graph graph) {
        //must not contain more vertex groups
        if (!vertexGroup.getContainedVertexGroups().isEmpty()) {
            return false;
        }
        //its vertices must only be in this vertex group
        for (Vertex containedVertex : vertexGroup.getContainedVertices()) {
            if (getNumberOfVertexGroupsContainingVertex(containedVertex, graph) > 1) {
                return false;
            }
        }
        //its vertices are in a touching pair of this vertex group
        for (Vertex containedVertex : vertexGroup.getContainedVertices()) {
            if (getNumberOfTouchingPairsVertexIsInvolved(vertexGroup, containedVertex) == 0) {
                return false;
            }
        }
        return true;
    }

    private static int getNumberOfVertexGroupsContainingVertex(Vertex vertex, Graph graph) {
        int count = 0;
        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
            if (vertexGroup.getContainedVertices().contains(vertex)) {
                ++count;
            }
        }
        return count;
    }

    private static int getNumberOfTouchingPairsVertexIsInvolved(VertexGroup vertexGroup, Vertex vertex) {
        int count = 0;
        for (TouchingPair touchingPair : vertexGroup.getTouchingPairs()) {
            if (touchingPair.getVertices().contains(vertex)) {
                ++count;
            }
        }
        return count;
    }

    private static boolean touchingPairExists(VertexGroup vertexGroup, Vertex oneVertex, Vertex otherVertex) {
        for (TouchingPair touchingPair : vertexGroup.getTouchingPairs()) {
            if (touchingPair.getVertices().contains(oneVertex) && touchingPair.getVertices().contains(otherVertex)) {
                return true;
            }
        }
        return false;
    }
}
