package de.uniwue.informatik.praline.datastructure.graphs;

import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabelManager;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.util.GraphUtils.newArrayListNullSave;

public class EdgeBundle implements LabeledObject {

    /*==========
     * Instance variables
     *==========*/

    private List<Edge> containedEdges;
    private List<EdgeBundle> containedEdgeBundles;
    private LabelManager labelManager;

    /*==========
     * Constructors
     *==========*/

    public EdgeBundle() {
        this(null, null, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles) {
        this(containedEdges, containedEdgeBundles, null, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles,
                      Collection<Label> labels) {
        this(containedEdges, containedEdgeBundles, labels, null);
    }

    public EdgeBundle(Collection<Edge> containedEdges, Collection<EdgeBundle> containedEdgeBundles,
                      Collection<Label> labels, Label mainlabel) {
        this.containedEdges = newArrayListNullSave(containedEdges);
        this.containedEdgeBundles = newArrayListNullSave(containedEdgeBundles);
        this.labelManager = new LabelManager(this, labels, mainlabel);
    }

    /*==========
     * Getters
     *==========*/

    public List<Edge> getContainedEdges() {
        return containedEdges;
    }

    public List<EdgeBundle> getContainedEdgeBundles() {
        return containedEdgeBundles;
    }

    @Override
    public LabelManager getLabelManager() {
        return labelManager;
    }

    /*==========
     * Modifiers
     *
     * Modificiations of the lists currently by List get***()
     * this maybe changed later:
     * make explicit add() and remove() methods and
     * add "Collections.unmodifiableList(...)" to getters
     *==========*/
}
