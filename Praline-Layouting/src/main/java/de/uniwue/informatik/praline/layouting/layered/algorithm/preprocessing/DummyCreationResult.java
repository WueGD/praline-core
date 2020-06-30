package de.uniwue.informatik.praline.layouting.layered.algorithm.preprocessing;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;

import java.util.Map;

public class DummyCreationResult {
    private Map<Vertex, Edge> dummyNodesLongEdges;
    private Map<Vertex, Vertex> dummyTurningNodes;
    private Map<Vertex, Vertex> nodeToLowerDummyTurningPoint;
    private Map<Vertex, Vertex> nodeToUpperDummyTurningPoint;
    private Map<Port, Port> correspondingPortsAtDummy;
    private Map<Edge, Edge> dummyEdge2RealEdge;

    public DummyCreationResult(Map<Vertex, Edge> dummyNodesLongEdges, Map<Vertex, Vertex> dummyTurningNodes,
                               Map<Vertex, Vertex> nodeToLowerDummyTurningPoint,
                               Map<Vertex, Vertex> nodeToUpperDummyTurningPoint,
                               Map<Port, Port> correspondingPortsAtDummy, Map<Edge, Edge> dummyEdge2RealEdge) {
        this.dummyNodesLongEdges = dummyNodesLongEdges;
        this.dummyTurningNodes = dummyTurningNodes;
        this.nodeToLowerDummyTurningPoint = nodeToLowerDummyTurningPoint;
        this.nodeToUpperDummyTurningPoint = nodeToUpperDummyTurningPoint;
        this.correspondingPortsAtDummy = correspondingPortsAtDummy;
        this.dummyEdge2RealEdge = dummyEdge2RealEdge;
    }

    public Map<Vertex, Edge> getDummyNodesLongEdges() {
        return dummyNodesLongEdges;
    }

    public Map<Vertex, Vertex> getDummyTurningNodes() {
        return dummyTurningNodes;
    }

    public Map<Vertex, Vertex> getNodeToLowerDummyTurningPoint() {
        return nodeToLowerDummyTurningPoint;
    }

    public Map<Vertex, Vertex> getNodeToUpperDummyTurningPoint() {
        return nodeToUpperDummyTurningPoint;
    }

    public Map<Port, Port> getCorrespondingPortsAtDummy() {
        return correspondingPortsAtDummy;
    }

    public Map<Edge, Edge> getDummyEdge2RealEdge() {
        return dummyEdge2RealEdge;
    }
}
