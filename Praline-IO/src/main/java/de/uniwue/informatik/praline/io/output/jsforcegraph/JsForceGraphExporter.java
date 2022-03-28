package de.uniwue.informatik.praline.io.output.jsforcegraph;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.graphs.VertexGroup;
import de.uniwue.informatik.praline.datastructure.labels.Label;
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

public class JsForceGraphExporter
{
    public JsForceGraph convertGraph(Graph graph, boolean exportCoordinates)
    {
        JsForceGraph jsForceGraph = new JsForceGraph();
        int jsIdCounter = 1;
        final Set<String> usedIds = new HashSet<>();
        final Map<Vertex, Node> vertexMap = new LinkedHashMap<>();
        final Map<VertexGroup, Group> groupMap = new LinkedHashMap<>();

        for (Vertex vertex : graph.getVertices())
        {
            Node node = new Node();

            // We first try to use the reference as ID, otherwise we construct a unique ID using the counter.
            // We also ensure that we do not accidentally construct one of the already used references using the counter.
            String reference = vertex.getReference();
            if (reference != null && !reference.isBlank() && !usedIds.contains(reference))
            {
                node.setId(reference);
            }
            while (node.getId() == null)
            {
                String newId = "c" + jsIdCounter++;
                if (!usedIds.contains(newId))
                {
                    node.setId(newId);
                }
            }
            usedIds.add(node.getId());

            // Handle main label (usually this is a TextLabel)
            Label<?> mainLabel = vertex.getLabelManager().getMainLabel();
            if (mainLabel instanceof TextLabel)
            {
                node.setName(mainLabel.toString());
            }

            // Handle ReferenceIconLabel if existing
            vertex.getLabelManager().getLabels().stream()
                    .filter(ReferenceIconLabel.class::isInstance)
                    .map(ReferenceIconLabel.class::cast)
                    .findFirst()
                    .ifPresent(l -> node.setIcon(l.getReference()));

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
            if (edge.getPorts().size() > 1)
            {
                link.setSource(vertexMap.get(edge.getPorts().get(0).getVertex()).getId());
                link.setTarget(vertexMap.get(edge.getPorts().get(1).getVertex()).getId());
            }
            Label<?> mainLabel = edge.getLabelManager().getMainLabel();
            if (mainLabel instanceof TextLabel)
            {
                link.setName(mainLabel.toString());
            }
            this.addEdgeAttributes(edge, link);
            jsForceGraph.getLinks().add(link);
        }

        List<VertexGroup> vertexGroups = graph.getAllRecursivelyContainedVertexGroups();
        for (VertexGroup vertexGroup : vertexGroups)
        {
            Group group = new Group();

            while (group.getId() == null)
            {
                String newId = "c" + jsIdCounter++;
                if (!usedIds.contains(newId))
                {
                    group.setId(newId);
                }
            }
            usedIds.add(group.getId());

            // Handle main label (usually this is a TextLabel)
            Label<?> mainLabel = vertexGroup.getLabelManager().getMainLabel();
            if (mainLabel instanceof TextLabel)
            {
                group.setName(mainLabel.toString());
            }

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

    // Possibility to extend exported data using inheritance
    @SuppressWarnings("unused")
    protected void addVertexAttributes(Vertex vertex, Node node)
    { }

    // Possibility to extend exported data using inheritance
    @SuppressWarnings("unused")
    protected void addEdgeAttributes(Edge edge, Link link)
    { }
}
