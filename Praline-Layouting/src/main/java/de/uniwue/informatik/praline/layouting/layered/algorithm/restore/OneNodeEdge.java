package de.uniwue.informatik.praline.layouting.layered.algorithm.restore;

import de.uniwue.informatik.praline.datastructure.graphs.*;

import java.util.*;

public class OneNodeEdge implements Restoreable {

    private Edge edge;
    private List<Port> ports;
    private Vertex node;
    private Map<Port, PortGroup> groups;
    private List<Port> dummyPorts;
    private Map<Vertex, Set<OneNodeEdge>> store;

    public OneNodeEdge (Map<Vertex, Set<OneNodeEdge>> store, Edge edge, Graph graph) {
        this.store = store;
        this.edge = edge;
        this.node = edge.getPorts().get(0).getVertex();
        this.ports = new ArrayList<>(edge.getPorts());
        this.groups = new LinkedHashMap<>();
        this.dummyPorts = new ArrayList<>();
        create(graph);
    }

    private void create (Graph graph) {
        for (Port port : ports) {
            port.removeEdge(edge);
            if (port.getEdges().isEmpty()) {
                if (port.getPortGroup() != null) {
                    groups.put(port, port.getPortGroup());
                    port.getPortGroup().removePortComposition(port);
                }
                node.removePortComposition(port);
            }
        }
        graph.removeEdge(edge);
        if (!store.containsKey(node)) {
            store.put(node, new LinkedHashSet<>());
        }
        store.get(node).add(this);
    }

    @Override
    public boolean restore () {
        if (store.containsKey(node) && store.get(node).contains(this)) {
            if (ports.size() == dummyPorts.size()) {
                for (int i = 0; i < ports.size(); i++) {
                    node.addPortComposition(ports.get(i));
                    if (groups.containsKey(ports.get(i))) {
                        groups.get(ports.get(i)).addPortComposition(ports.get(i));
                    }
                    ports.get(i).addEdge(edge);
                    if (dummyPorts.get(i).getPortGroup() != null) {
                        dummyPorts.get(i).getPortGroup().removePortComposition(dummyPorts.get(i));
                    }
                    node.removePortComposition(dummyPorts.get(i));
                    ports.get(i).setShape(dummyPorts.get(i).getShape());
                }
                if (store.get(node).size() == 1) {
                    store.remove(node);
                } else {
                    store.get(node).remove(this);
                }
                return true;
            }
        }
        return false;
    }

    public boolean addDummyPorts (Collection<Port> dummyPorts) {
        if (this.dummyPorts.isEmpty() && dummyPorts.size() == ports.size()) {
            this.dummyPorts.addAll(dummyPorts);
            return true;
        }
        return false;
    }

    public List<Port> getPorts () {
        return Collections.unmodifiableList(ports);
    }

    public Edge getEdge () {
        return edge;
    }

    public Vertex getNode () {
        return node;
    }

    public boolean hasPortGroup () {
        return (!groups.isEmpty());
    }
}
