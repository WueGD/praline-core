package de.uniwue.informatik.praline.io.output.jsforcegraph;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.io.model.jsforcegraph.JsForceGraph;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Link;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Node;

import java.util.HashSet;
import java.util.LinkedHashMap;
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

            Label<?> mainLabel = vertex.getLabelManager().getMainLabel();
            if (mainLabel instanceof TextLabel)
            {
                node.setName(mainLabel.toString());
            }

            if (exportCoordinates)
            {
                node.setFx(vertex.getShape().getXPosition());
                node.setFy(0.0);
                node.setFz(vertex.getShape().getYPosition());
            }

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
