package de.uniwue.informatik.praline.layouting.layered.algorithm.restore;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;

import java.util.Set;

public class DummyNode implements Restoreable{

    private DummyType type;
    private Edge edge;
    private Set<Vertex> nodes;

    public DummyNode (Edge edge, DummyType type) {
        this.type = type;
        this.edge = edge;
    }

    private void create () {

    }

    @Override
    public boolean restore () {
        return false;
    }
}
