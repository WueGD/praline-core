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
        this(copyNodeOrder(copyOrder.getNodeOrder()), copyMap(copyOrder.getTopPortOrder()),
                copyMap(copyOrder.getBottomPortOrder()));
    }

    public SortingOrder(List<List<Vertex>> nodeOrder, Map<Vertex, List<Port>> topPortOrder,
                        Map<Vertex, List<Port>> bottomPortOrder) {
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

    /**
     * Changes the order of {@link PortComposition}s in {@link Vertex#getPortCompositions()} and
     * {@link PortGroup#getPortCompositions()} such that they are the same as in the current
     * {@link SortingOrder#getTopPortOrder()} and {@link SortingOrder#getBottomPortOrder()}.
     * It starts with the top ports from left to right, followed by the bottom ports from left to right
     */
    public void transferPortOrderingToPortCompositionLists() {
        for (List<Vertex> layer : nodeOrder) {
            for (Vertex node : layer) {
                transferPortOrderingToPortCompositionLists(node, null);
            }
        }
    }

    private void transferPortOrderingToPortCompositionLists(Vertex node, PortGroup portGroup) {
        Set<PortComposition> unassigned = new LinkedHashSet<>(
                portGroup == null ? node.getPortCompositions() : portGroup.getPortCompositions());
        List<PortComposition> newOrder = new ArrayList<>(
                portGroup == null ? node.getPortCompositions().size() : portGroup.getPortCompositions().size());

        for (Port port : topPortOrder.get(node)) {
            transferPortOrderingToPortCompositionLists(node, portGroup, unassigned, newOrder, port);
        }
        for (Port port : bottomPortOrder.get(node)) {
            transferPortOrderingToPortCompositionLists(node, portGroup, unassigned, newOrder, port);
        }

        if (portGroup == null) {
            //remove them all
            for (PortComposition portComposition : newOrder) {
                node.removePortComposition(portComposition);
            }
            if (!node.getPorts().isEmpty()) {
                System.out.println("Warning! Tried to sort ports at " + node + " acc. to SortingOrder, but not all " +
                        "port have been assigned.");
            }
            //and then re-add them in our desired order
            for (PortComposition portComposition : newOrder) {
                node.addPortComposition(portComposition);
            }
        }
        else {
            //remove them all
            for (PortComposition portComposition : newOrder) {
                portGroup.removePortComposition(portComposition);
            }
            if (!PortUtils.getPortsRecursively(portGroup).isEmpty()) {
                System.out.println("Warning! Tried to sort ports at " + node + " acc. to SortingOrder, but not all " +
                        "port have been assigned. Failed in port group " + portGroup);
            }
            //and then re-add them in our desired order
            for (PortComposition portComposition : newOrder) {
                portGroup.addPortComposition(portComposition);
            }
        }
    }

    private void transferPortOrderingToPortCompositionLists(Vertex node, PortGroup portGroup,
                                                            Set<PortComposition> unassigned,
                                                            List<PortComposition> newOrder, Port port) {
        PortComposition topMostAncestor = PortUtils.getTopMostChildContainingThisPort(portGroup, port);
        if (unassigned.contains(topMostAncestor)) {
            newOrder.add(topMostAncestor);
            unassigned.remove(topMostAncestor);
            if (topMostAncestor instanceof PortGroup) {
                transferPortOrderingToPortCompositionLists(node, (PortGroup) topMostAncestor);
            }
        } else if (topMostAncestor != null && !newOrder.get(newOrder.size() - 1).equals(topMostAncestor)) {
//            System.out.println("Warning! Ordering of ports in SortingOrder of " + node + "violates the structure" +
//                    " of port groups.");
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

    private static List<List<Vertex>> copyNodeOrder(List<List<Vertex>> nodeOrder) {
        List<List<Vertex>> copyNodeOrder = new ArrayList<>();
        for (List<Vertex> layer : nodeOrder) {
            copyNodeOrder.add(new ArrayList<>(layer));
        }
        return copyNodeOrder;
    }

    private static Map<Vertex, List<Port>> copyMap(Map<Vertex, List<Port>> topPortOrder) {
        Map<Vertex, List<Port>> copyTopPortOrder = new LinkedHashMap<>();
        for (Vertex node : topPortOrder.keySet()) {
            copyTopPortOrder.put(node, new ArrayList<>(topPortOrder.get(node)));
        }
        return copyTopPortOrder;
    }
}
