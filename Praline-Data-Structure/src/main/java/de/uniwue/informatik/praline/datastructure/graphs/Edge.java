package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.PropertyObject;
import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.labels.EdgeLabelManager;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.styles.PathStyle;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;
import de.uniwue.informatik.praline.datastructure.utils.InconsistentStateException;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;
import static de.uniwue.informatik.praline.datastructure.graphs.Edge.Direction.*;

/**
 * In typical applications, {@link Edge}s of the {@link Graph} are wires or logical connections.
 * They connect a set of {@link Vertex} -- typically two, but we also allow hyperedges connection more than two
 * {@link Vertex}es -- via {@link Port}s.
 * Their course is determined by the algorithm which sets the {@link Path}s of this edge.
 * Their course may be the unification of several {@link Path}s since we allow hyperedges,
 * but on a classical edge one should expect just one path.
 * <p>
 * The thickness may be set by the user and will then be taken as thickness for the {@link Path}s.
 * <p>
 * Several {@link Edge}s may be grouped together via {@link EdgeBundle}s.
 * <p>
 * You can add {@link Label}s to the interior of an {@link Edge}e or to the end of an {@link Edge} at any of its
 * ports. See {@link EdgeLabelManager}.
 */
@JsonIgnoreProperties({"edgeBundle", "thickness", "color"})
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class Edge implements LabeledObject, ReferenceObject, PropertyObject {

    /*==========
     * Instance variables
     *==========*/

    private final List<Port> ports;
    private final List<Direction> directions;
    private final List<Path> paths;
    private PathStyle pathStyle;
    private EdgeBundle edgeBundle;
    private final EdgeLabelManager labelManager;
    private String reference;
    private final Map<String, String> properties;

    /*==========
     * Constructors
     *==========*/

    public Edge(Collection<Port> ports) {
        this(ports, null, null, null, null, null, null);
    }

    public Edge(Collection<Port> ports, Collection<Label<? extends LabelStyle>> innerLabels) {
        this(ports, innerLabels, null, null, null);
    }

    public Edge(Collection<Port> ports, Label<? extends LabelStyle> mainLabel) {
        this(ports, Collections.singleton(mainLabel), null, mainLabel, null);
    }

    public Edge(Collection<Port> ports, Collection<Label<? extends LabelStyle>> innerLabels, Map<Port,
            List<Label<? extends LabelStyle>>> portLabels) {
        this(ports, innerLabels, portLabels, null, null);
    }

    public Edge(Collection<Port> ports, Collection<Label<? extends LabelStyle>> innerLabels, Map<Port,
            List<Label<? extends LabelStyle>>> portLabels, Label<? extends LabelStyle> mainLabel, PathStyle pathStyle) {
        this(ports, null, innerLabels, portLabels, mainLabel,pathStyle, null);
    }

    @JsonCreator
    protected Edge(
            @JsonProperty("ports") final Collection<Port> ports,
            @JsonProperty("directions") final Collection<DirAtPort> directions,
            @JsonProperty("labelManager") final EdgeLabelManager labelManager,
            @JsonProperty("pathStyle") final PathStyle pathStyle,
            @JsonProperty("paths") final Collection<Path> paths,
            @JsonProperty("properties") final Map<String, String> properties
    ) {
        //do not add port labels first because they are in the wrong format
        this(ports, directions, labelManager.getInnerLabels(), null, labelManager.getMainLabel(), pathStyle, properties);
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
     * @param directions
     * @param innerLabels
     * @param portLabels
     * @param mainLabel
     * @param pathStyle
     * @param properties
     */
    public Edge(
            Collection<Port> ports,
            Collection<DirAtPort> directions,
            Collection<Label<? extends LabelStyle>> innerLabels,
            Map<Port, List<Label<? extends LabelStyle>>> portLabels,
            Label<? extends LabelStyle> mainLabel,
            PathStyle pathStyle,
            Map<String, String> properties
    ) {
        this.ports = newArrayListNullSafe(ports);
        this.directions = new ArrayList<>(Collections.nCopies(this.ports.size(), Direction.UNDIRECTED));
        if (directions != null) {
            for (var entry : directions) {
                var index = this.ports.indexOf(entry.port);
                if (index >= 0) {
                    this.directions.set(index, entry.direction);
                }
            }
        }
        for (Port port : this.ports) {
            port.addEdgeButNotPort(this);
        }
        this.labelManager = new EdgeLabelManager(this, innerLabels, portLabels, mainLabel);
        this.pathStyle = pathStyle == null ? PathStyle.DEFAULT_PATH_STYLE : pathStyle;
        this.paths = new LinkedList<>();
        this.properties = new LinkedHashMap<>();
        if(properties != null){
            this.properties.putAll(properties);
        }
    }


    /*==========
     * Getters & Setters
     *==========*/

    public List<Port> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    public List<DirAtPort> getDirectedPorts() {
        return java.util.stream.IntStream.range(0, ports.size())
                .mapToObj(i -> new DirAtPort(directions.get(i), ports.get(i)))
                .toList();
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
    public String getReference() {
        return this.reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
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
     * @return true if {@link Port} is added to the {@link Port}s of this {@link Edge} and false if the input parameter
     * is set to an {@link Port} that is already associated with this {@link Edge}.
     */
    public boolean addPort(Port p) {
        return addDirectedPort(p, UNDIRECTED);
    }

    public boolean addDirectedPort(Port p, Direction dir) {
        if (addPortButNotEdge(p, dir)) {
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

    protected boolean addPortButNotEdge(Port p, Direction dir) {
        var index = ports.indexOf(p);
        if (index == -1) {
            ports.add(p);
            directions.add(dir);
            return true;
        }
        return false;
    }

    protected boolean removePortButNotEdge(Port p) {
        var index = ports.indexOf(p);
        if (index > -1) {
            ports.remove(index);
            directions.remove(index);
        }
        return index != -1;
    }

    /*=============
     * Simple Edge
     *=============*/

    private boolean isSimple() {
        return this.ports.size() == 2 && (
                this.directions.get(0) == UNDIRECTED && this.directions.get(1) == UNDIRECTED
                        || this.directions.get(0) == OUTGOING && this.directions.get(1) == INCOMING
                        || this.directions.get(0) == INCOMING && this.directions.get(1) == OUTGOING);
    }

    public Direction simpleEdgeDirection() {
        assert isSimple();
        return directions.get(0);
    }

    public void directSimpleEdge(Direction direction) {
        assert isSimple();
        switch (direction) {
            case UNDIRECTED -> {
                directions.set(0, UNDIRECTED);
                directions.set(1, UNDIRECTED);
            }
            case INCOMING -> {
                directions.set(0, INCOMING);
                directions.set(1, OUTGOING);
            }
            case OUTGOING -> {
                directions.set(1, INCOMING);
                directions.set(0, OUTGOING);
            }
        }
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

    public record DirAtPort(Direction direction, Port port) {}

    public enum Direction {
        UNDIRECTED, INCOMING, OUTGOING
    }
}
