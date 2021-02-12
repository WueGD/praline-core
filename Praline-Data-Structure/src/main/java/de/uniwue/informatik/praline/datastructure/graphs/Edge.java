package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.EdgeLabelManager;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.styles.PathStyle;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;
import de.uniwue.informatik.praline.datastructure.utils.InconsistentStateException;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * In typical applications, {@link Edge}s of the {@link Graph} are wires or logical connections.
 * They connect a set of {@link Vertex} -- typically two, but we also allow hyperedges connection more than two
 * {@link Vertex}es -- via {@link Port}s.
 * Their course is determined by the algorithm which sets the {@link Path}s of this edge.
 * Their course may be the unification of several {@link Path}s since we allow hyperedges,
 * but on a classical edge one should expect just one path.
 *
 * The thickness may be set by the user and will then be taken as thickness for the {@link Path}s.
 *
 * Several {@link Edge}s may be grouped together via {@link EdgeBundle}s.
 *
 * You can add {@link Label}s to the interior of an {@link Edge}e or to the end of an {@link Edge} at any of its
 * ports. See {@link EdgeLabelManager}.
 */
@JsonIgnoreProperties({ "edgeBundle", "thickness", "color" })
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class Edge implements LabeledObject, ReferenceObject {

    /*==========
     * Instance variables
     *==========*/

    private final List<Port> ports;
    private List<Path> paths;
    private PathStyle pathStyle;
    private EdgeBundle edgeBundle;
    private final EdgeLabelManager labelManager;
    private String reference;


    /*==========
     * Constructors
     *==========*/

    public Edge(Collection<Port> ports) {
        this(ports, null, null, null, null);
    }

    public Edge(Collection<Port> ports, Collection<Label> innerLabels) {
        this(ports, innerLabels, null, null, null);
    }

    public Edge(Collection<Port> ports, Collection<Label> innerLabels, Map<Port, List<Label>> portLabels) {
        this(ports, innerLabels, portLabels, null, null);
    }


    @JsonCreator
    protected Edge(
            @JsonProperty("ports") final Collection<Port> ports,
            @JsonProperty("labelManager") final EdgeLabelManager labelManager,
            @JsonProperty("pathStyle") final PathStyle pathStyle,
            @JsonProperty("paths") final Collection<Path> paths
    ) {
        //do not add port labels first because they are in the wrong format
        this(ports, labelManager.getInnerLabels(), null, labelManager.getMainLabel(), pathStyle);
        //but do it more manually here
        for (EdgeLabelManager.PairPort2Labels pair : labelManager.getAllPortLabels()) {
            labelManager.addPortLabels(pair.port, pair.labels);
        }
        this.addPaths(paths);
    }

    /**
     * leave value as null if it should be empty initially (e.g. no labels)
     *
     * @param ports
     * @param innerLabels
     * @param portLabels
     * @param mainLabel
     * @param pathStyle
     */
    public Edge(
            Collection<Port> ports, Collection<Label> innerLabels, Map<Port, List<Label>> portLabels, Label mainLabel,
            PathStyle pathStyle
    ) {
        this.ports = newArrayListNullSafe(ports);
        for (Port port : this.ports) {
            port.addEdgeButNotPort(this);
        }
        this.labelManager = new EdgeLabelManager(this, innerLabels, portLabels, mainLabel);
        this.pathStyle = pathStyle == null ? PathStyle.DEFAULT_PATH_STYLE : pathStyle;
        this.paths = new LinkedList<>();
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<Port> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    public List<Path> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public PathStyle getPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(PathStyle pathStyle) {
        this.pathStyle = pathStyle;
    }

    public EdgeBundle getEdgeBundle() {
        return edgeBundle;
    }

    protected void setEdgeBundle(EdgeBundle edgeBundle) {
        this.edgeBundle = edgeBundle;
    }

    @Override
    public EdgeLabelManager getLabelManager() {
        return labelManager;
    }

    @Override
    public String getReference()
    {
        return this.reference;
    }

    @Override
    public void setReference(String reference)
    {
        this.reference = reference;
    }


    /*==========
     * Modifiers
     *==========*/

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public void addPaths(Collection<Path> paths) {
        this.paths.addAll(paths);
    }

    public boolean removePath(Path path) {
        return this.paths.remove(path);
    }

    public void removeAllPaths() {
        this.paths.clear();
    }

    public boolean addPorts(Collection<Port> ports) {
        boolean success = true;
        for (Port p : ports) {
            success &= addPort(p);
        }
        return success;
    }

    /**
     * this {@link Edge} is also added to the list of {@link Edge}s of the passed {@link Port} p
     *
     * @param p
     * @return
     *      true if {@link Port} is added to the {@link Port}s of this {@link Edge} and false if the input parameter
     *      is set to an {@link Port} that is already associated with this {@link Edge}.
     */
    public boolean addPort(Port p) {
        if (addPortButNotEdge(p)) {
            if (!p.addEdgeButNotPort(this)) {
                //TODO: maybe change this construction later (do real throwing methodwise or just use no exception)
                try {
                    throw new InconsistentStateException("Edge " + this + " was already added to Port " + p + ", but " +
                            "not the other way around");
                } catch (InconsistentStateException exception) {
                    exception.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * this {@link Edge} is also removed from the list of {@link Edge}s of the passed {@link Port} p
     *
     * @param p
     * @return
     */
    public boolean removePort(Port p) {
        p.removeEdgeButNotPort(this);
        return removePortButNotEdge(p);
    }

    protected boolean addPortButNotEdge(Port p) {
        if (!ports.contains(p)) {
            ports.add(p);
            return true;
        }
        return false;
    }

    protected boolean removePortButNotEdge(Port p) {
        return ports.remove(p);
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return labelManager.getStringForLabeledObject();
    }

    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(LabeledObject o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return EqualLabeling.equalLabelingLists(new ArrayList<>(ports), new ArrayList<>(edge.ports)) &&
                labelManager.equalLabeling(edge.labelManager) &&
                Objects.equals(reference, edge.reference);
    }

}
