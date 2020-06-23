package de.uniwue.informatik.praline.io.input.graphml;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.io.Constants;
import de.uniwue.informatik.praline.io.model.graphml.DataType;
import de.uniwue.informatik.praline.io.model.graphml.EdgeType;
import de.uniwue.informatik.praline.io.model.graphml.GraphType;
import de.uniwue.informatik.praline.io.model.graphml.GraphmlType;
import de.uniwue.informatik.praline.io.model.graphml.KeyType;
import de.uniwue.informatik.praline.io.model.graphml.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphMLReader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLReader.class);

    public static Graph create(File file)
    {
        GraphmlType graphML = JAXB.unmarshal(file, GraphmlType.class);
        return GraphMLReader.create(graphML);
    }

    public static Graph create(GraphmlType graphML)
    {
        Map<String, Vertex> nodeMap = new HashMap<>();
        List<Edge> edges = new LinkedList<>();

        // Read attribute keys
        Map<AttributeEnum, String> attributeIds = new HashMap<>();
        for (KeyType key : graphML.getKey())
        {
            for (AttributeEnum attribute : AttributeEnum.values())
            {
                if (key.getAttrName().equals(attribute.getAttrName())
                        && key.getFor().value().equals(attribute.getFor()))
                {
                    attributeIds.put(attribute, key.getId());
                    break;
                }
            }
        }

        // Find first graph object in GraphML file (if there is any)
        Optional<GraphType> firstGraph = graphML.getGraphOrData().stream().filter(GraphType.class::isInstance).map(GraphType.class::cast).findFirst();
        if (firstGraph.isEmpty())
        {
            GraphMLReader.LOGGER.error("Did not find a graph in the given GraphML data.");
            return null;
        }
        GraphType graph = firstGraph.get();

        // Read nodes and create Vertex objects for them
        graph.getDataOrNodeOrEdge().stream().filter(NodeType.class::isInstance).map(NodeType.class::cast).forEach(node -> {
            Vertex vertex = new Vertex();
            Rectangle rectangle = new Rectangle();
            rectangle.setRect(0, 0, Constants.DEFAULT_NODE_SIZE, Constants.DEFAULT_NODE_SIZE);
            vertex.setShape(rectangle);
            node.getDataOrPort().stream()
                    .filter(DataType.class::isInstance)
                    .map(DataType.class::cast)
                    .filter(data -> data.getKey().equals(attributeIds.get(AttributeEnum.NODE_LABEL)))
                    .findFirst()
                    .ifPresent(data -> vertex.getLabelManager().setMainLabel(new TextLabel(data.getContent())));
            nodeMap.put(node.getId(), vertex);
        });

        // Read edges and create objects for them
        graph.getDataOrNodeOrEdge().stream().filter(EdgeType.class::isInstance).map(EdgeType.class::cast).forEach(edge -> {
            Vertex sourceVertex = nodeMap.get(edge.getSource());
            Vertex targetVertex = nodeMap.get(edge.getTarget());
            Port sourcePort = new Port();
            Port targetPort = new Port();
            sourceVertex.addPortComposition(sourcePort);
            targetVertex.addPortComposition(targetPort);
            Edge outEdge = new Edge(Arrays.asList(sourcePort, targetPort));
            edge.getData().stream()
                    .filter(data -> data.getKey().equals(attributeIds.get(AttributeEnum.LINK_LABEL)))
                    .findFirst()
                    .ifPresent(data -> outEdge.getLabelManager().setMainLabel(new TextLabel(data.getContent())));
            edges.add(outEdge);
        });

        // Put together Graph object
        Graph returnGraph = new Graph();
        nodeMap.values().forEach(returnGraph::addVertex);
        edges.forEach(returnGraph::addEdge);
        return returnGraph;
    }
}
