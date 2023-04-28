package de.uniwue.informatik.praline.io.input.jsforcegraph;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.ReferenceIconLabel;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.io.model.jsforcegraph.JsForceGraph;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Link;
import de.uniwue.informatik.praline.io.model.jsforcegraph.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JsForceGraphImporter
{
    public Graph convertGraph(JsForceGraph jsForceGraph)
    {
        Graph graph = new Graph();
        Map<String, Vertex> idVertexMap = new HashMap<>();

        for (Node node : jsForceGraph.getNodes())
        {
            Vertex vertex = new Vertex();
            if (node.getName() != null) {
                vertex.getLabelManager().setMainLabel(new TextLabel(node.getName()));
            }
            if (node.getIcon() != null) {
                vertex.getLabelManager().addLabel(new ReferenceIconLabel(node.getIcon()));
            }
            vertex.setReference(node.getId());
            idVertexMap.put(node.getId(), vertex);
            graph.addVertex(vertex);
        }

        for (Link link: jsForceGraph.getLinks())
        {
            Port sourcePort = createPort(link.getSource(), idVertexMap);
            Port targetPort = createPort(link.getTarget(), idVertexMap);
            Edge edge = new Edge(Arrays.asList(sourcePort, targetPort));
            String name = link.getName();
            if (name != null) {
                edge.getLabelManager().setMainLabel(new TextLabel(link.getName()));
            }
            edge.setReference(link.getId());
            graph.addEdge(edge);
        }

        return graph;
    }

    private static Port createPort(String vertexId, Map<String, Vertex> idVertexMap)
    {
        Port port = new Port();
        port.getLabelManager().addLabel(new TextLabel("dummyport"));
        idVertexMap.get(vertexId).addPortComposition(port);
        return port;
    }
}
