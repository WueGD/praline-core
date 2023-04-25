package de.uniwue.informatik.praline.io.output.jsforcegraph;

import de.uniwue.informatik.praline.datastructure.ReferenceObject;
import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.graphs.VertexGroup;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.ReferenceIconLabel;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Group;
import de.uniwue.informatik.praline.io.model.jsforcegraph.JsForceGraph;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Link;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Node;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class JsForceGraphExporter
{
    private final Set<String> usedIds = new HashSet<>();
    private int jsIdCounter = 1;

    public JsForceGraph convertGraph(Graph graph, boolean exportCoordinates)
    {
        JsForceGraph jsForceGraph = new JsForceGraph();
        final Map<Vertex, Node> vertexMap = new LinkedHashMap<>();
        final Map<VertexGroup, Group> groupMap = new LinkedHashMap<>();

        for (Vertex vertex : graph.getVertices())
        {
            Node node = new Node();
            node.setId(this.getId(vertex));
            JsForceGraphExporter.handleMainLabel(vertex, node::setName);

            // Handle ReferenceIconLabel if existing
            vertex.getLabelManager().getLabels().stream()
                    .filter(ReferenceIconLabel.class::isInstance)
                    .map(ReferenceIconLabel.class::cast)
                    .findFirst()
                    .ifPresent(l -> node.setIcon(l.getReference()));

            // Handle properties
            node.setProperties(vertex.getProperties());

            // Store coordinates if requested
            if (exportCoordinates)
            {
                node.setFx(vertex.getShape().getXPosition());
                node.setFy(0.0);
                node.setFz(vertex.getShape().getYPosition());
            }

            // Add possible extended attributes (if this class is derived from)
            this.addVertexAttributes(vertex, node);

            jsForceGraph.getNodes().add(node);
            vertexMap.put(vertex, node);
        }

        for (Edge edge : graph.getEdges())
        {
            Link link = new Link();
            link.setId(this.getId(edge));
            JsForceGraphExporter.handleMainLabel(edge, link::setName);

            if (edge.getPorts().size() > 1)
            {
                link.setSource(vertexMap.get(edge.getPorts().get(0).getVertex()).getId());
                link.setTarget(vertexMap.get(edge.getPorts().get(1).getVertex()).getId());
            }
            link.setProperties(edge.getProperties());
            this.addEdgeAttributes(edge, link);
            jsForceGraph.getLinks().add(link);
        }

        List<VertexGroup> vertexGroups = graph.getAllRecursivelyContainedVertexGroups();
        for (VertexGroup vertexGroup : vertexGroups)
        {
            Group group = new Group();
            group.setId(this.getId(vertexGroup));
            JsForceGraphExporter.handleMainLabel(vertexGroup, group::setName);

            jsForceGraph.getGroups().add(group);
            groupMap.put(vertexGroup, group);
        }

        for (VertexGroup vertexGroup : vertexGroups)
        {
            String groupId = groupMap.get(vertexGroup).getId();
            for (VertexGroup containedVertexGroup : vertexGroup.getContainedVertexGroups())
            {
                if (groupMap.containsKey(containedVertexGroup))
                {
                    groupMap.get(containedVertexGroup).setParent(groupId);
                }
            }
            for (Vertex containedVertex : vertexGroup.getContainedVertices())
            {
                if (vertexMap.containsKey(containedVertex))
                {
                    vertexMap.get(containedVertex).setGroup(groupId);
                }
            }
        }

        return jsForceGraph;
    }

    private String getId(ReferenceObject referenceObject)
    {
        // We first try to use the reference as ID, otherwise we construct a unique ID using the counter.
        // We also ensure that we do not accidentally construct one of the already used references using the counter.
        String reference = referenceObject.getReference();
        if (reference != null && !reference.isBlank() && !this.usedIds.contains(reference))
        {
            this.usedIds.add(reference);
            return reference;
        }

        String newId;
        do
        {
            newId = "c" + this.jsIdCounter++;
        } while (this.usedIds.contains(newId));
        this.usedIds.add(newId);
        return newId;
    }

    private static void handleMainLabel(LabeledObject labeledObject, Consumer<String> labelConsumer)
    {
        // Handle main label (usually this is a TextLabel)
        Label<?> mainLabel = labeledObject.getLabelManager().getMainLabel();
        if (mainLabel instanceof TextLabel)
        {
            labelConsumer.accept(mainLabel.toString());
        }
    }

    // Possibility to extend exported data using inheritance
    @SuppressWarnings("unused")
    protected void addVertexAttributes(Vertex vertex, Node node)
    { }

    // Possibility to extend exported data using inheritance
    @SuppressWarnings("unused")
    protected void addEdgeAttributes(Edge edge, Link link)
    { }
}
