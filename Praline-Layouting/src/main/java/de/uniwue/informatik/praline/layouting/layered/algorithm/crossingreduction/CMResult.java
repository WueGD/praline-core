package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;

import java.util.List;
import java.util.Map;

public class CMResult {

    private List<List<Vertex>> nodeOrder;
    private Map<Vertex, List<Port>> topPortOrder;
    private Map<Vertex, List<Port>> bottomPortOrder;

    public CMResult(List<List<Vertex>> nodeOrder, Map<Vertex, List<Port>> topPortOrder, Map<Vertex, List<Port>> bottomPortOrder) {
        this.nodeOrder = nodeOrder;
        this.topPortOrder = topPortOrder;
        this.bottomPortOrder = bottomPortOrder;
    }

    public List<List<Vertex>> getNodeOrder() {
        return nodeOrder;
    }

    public Map<Vertex, List<Port>> getTopPortOrder() {
        return topPortOrder;
    }

    public Map<Vertex, List<Port>> getBottomPortOrder() {
        return bottomPortOrder;
    }
}
