package de.uniwue.informatik.praline.io.output.jsforcegraph;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.io.model.jsforcegraph.JsForceGraph;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Link;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Node;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsForceGraphExporter
{
    private final Map<Vertex, Node> vertexMap = new LinkedHashMap<>();

    public JsForceGraphExporter()
    {
    }

    public JsForceGraph convertGraph(Graph graph)
    {
        JsForceGraph jsForceGraph = new JsForceGraph();
        int jsIdCounter = 1;

        for (Vertex vertex : graph.getVertices())
        {
            Node node = new Node();
            if (vertex.getReference() != null && !vertex.getReference().isBlank())
            {
                node.setId(vertex.getReference());
            }
            else
            {
                node.setId(Integer.toString(jsIdCounter));
                jsIdCounter++;
            }
            Label<?> mainLabel = vertex.getLabelManager().getMainLabel();
            if (mainLabel instanceof TextLabel)
            {
                node.setName(mainLabel.toString());
            }
            this.addVertexAttributes(vertex, node);
            jsForceGraph.getNodes().add(node);
            this.vertexMap.put(vertex, node);
        }

        for (Edge edge : graph.getEdges())
        {
            Link link = new Link();
            if (edge.getPorts().size() > 1)
            {
                link.setSource(this.getVertexNodeId(edge.getPorts().get(0)));
                link.setTarget(this.getVertexNodeId(edge.getPorts().get(1)));
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

    private String getVertexNodeId(Port port)
    {
        Vertex vertex = port.getVertex();
        return this.vertexMap.get(vertex).getId();
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
