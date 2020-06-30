package de.uniwue.informatik.praline.layouting.layered.algorithm.restore;

import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.PortGroup;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class NoEdgePort implements Restoreable{

    private Port port;
    private PortGroup portGroup;
    private Vertex node;
    private Map<Vertex, Set<NoEdgePort>> store;

    public NoEdgePort (Map<Vertex, Set<NoEdgePort>> store, Port port) {
        this.store = store;
        this.port = port;
        this.portGroup = port.getPortGroup();
        this.node = port.getVertex();
        create();
    }

    private void create () {
        if (portGroup != null) {
            portGroup.removePortComposition(port);
        } else {
            node.removePortComposition(port);
        }
        if (!store.containsKey(node)) {
            store.put(node, new LinkedHashSet<>());
        }
        store.get(node).add(this);
    }

    @Override
    public boolean restore () {
        if (store.containsKey(node) && store.get(node).contains(this)) {
            if (portGroup != null) {
                portGroup.addPortComposition(port);
            }
            node.addPortComposition(port);
            if (store.get(node).size() == 1) {
                store.remove(node);
            } else {
                store.get(node).remove(this);
            }
            return true;
        }
        return false;
    }

    public boolean hasPortGroup() {
        return (portGroup != null);
    }
}
