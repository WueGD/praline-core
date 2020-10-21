package de.uniwue.informatik.praline.layouting.layered.algorithm.util;

import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.PortComposition;
import de.uniwue.informatik.praline.datastructure.graphs.PortGroup;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;

import java.util.*;

public class SortingOrder {

    private List<List<Vertex>> nodeOrder;
    private Map<Vertex, List<Port>> topPortOrder;
    private Map<Vertex, List<Port>> bottomPortOrder;

    public SortingOrder() {
        this(new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public SortingOrder(SortingOrder copyOrder) {
        this(copyOrder.getNodeOrder(), copyOrder.getTopPortOrder(), copyOrder.getBottomPortOrder());
    }

    public SortingOrder(List<List<Vertex>> nodeOrder, Map<Vertex, List<Port>> topPortOrder, Map<Vertex, List<Port>> bottomPortOrder) {
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

    public void shufflePorts() {
        for (List<Vertex> layer : nodeOrder) {
            for (Vertex vertex : layer) {
                // top ports
                Set<PortComposition> topPortCompositions = new LinkedHashSet<>();
                for (Port port : topPortOrder.get(vertex)) {
                    topPortCompositions.add(PortUtils.getTopMostAncestor(port));
                }
                this.topPortOrder.put(vertex, shufflePortCompositions(topPortCompositions));
                // bottom ports
                Set<PortComposition> bottomPortCompositions = new LinkedHashSet<>();
                for (Port port : bottomPortOrder.get(vertex)) {
                    bottomPortCompositions.add(PortUtils.getTopMostAncestor(port));
                }
                this.bottomPortOrder.put(vertex, shufflePortCompositions(bottomPortCompositions));
            }
        }
    }


    private static List<Port> shufflePortCompositions(Collection<PortComposition> portCompositions) {
        List<Port> order = new ArrayList<>();
        shufflePortCompositionsRecursively(portCompositions, order);
        return order;
    }

    private static void shufflePortCompositionsRecursively(Collection<PortComposition> portCompositions,
                                                           List<Port> order) {
        List<PortComposition> toShuffle = new ArrayList<>(portCompositions);
        Collections.shuffle(toShuffle, Constants.random);
        for (PortComposition portComposition : toShuffle) {
            if (portComposition instanceof Port) {
                order.add((Port)portComposition);
            } else if (portComposition instanceof PortGroup) {
                shufflePortCompositionsRecursively(((PortGroup)portComposition).getPortCompositions(), order);
            }
        }
    }
}
