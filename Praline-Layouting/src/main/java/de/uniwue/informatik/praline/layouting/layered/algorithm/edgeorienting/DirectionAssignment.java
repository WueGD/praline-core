package de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DirectionAssignment {

    public void randomDirected (SugiyamaLayouter sugy) {
        Map<Vertex, Integer> values = new LinkedHashMap<>();
        List<Vertex> vertices = new LinkedList<>(sugy.getGraph().getVertices());
        Collections.shuffle(vertices, Constants.random);
        for (int i = 0; i < vertices.size(); i++) {
            values.put(vertices.get(i), i);
        }
        for (Edge edge : sugy.getGraph().getEdges()) {
            Vertex node0 = edge.getPorts().get(0).getVertex();
            Vertex node1 = edge.getPorts().get(1).getVertex();
            if (values.get(node0) > values.get(node1)) {
                // direct edge from 1 to 0
                sugy.assignDirection(edge, node1, node0);
            } else {
                // direct edge from 0 to 1
                sugy.assignDirection(edge, node0, node1);
            }
        }
    }

    public void forceDirected (SugiyamaLayouter sugy) {
        // create new Jung graph
        UndirectedSparseGraph<String,String> junggraph = new UndirectedSparseGraph<>();
        int edgeCounter = 0;
        // add vertices and edges from original graph to the Jung graph
        for (Vertex node : sugy.getGraph().getVertices()) {
            junggraph.addVertex(node.getLabelManager().getMainLabel().toString());
        }
        for (Edge edge : sugy.getGraph().getEdges()) {
            junggraph.addEdge(
                    "e" + edgeCounter++ + "-" + edge.getLabelManager().getMainLabel().toString(),
                    edge.getPorts().get(0).getVertex().getLabelManager().getMainLabel().toString(),
                    edge.getPorts().get(1).getVertex().getLabelManager().getMainLabel().toString()
            );
        }
        // create new force directed layout
        FRLayout<String, String> layout = new FRLayout<>(junggraph);
        // calculate height and width so that each node has 6237 pixel space and the drawing space is proportional in size to DIN A4
        int height= ((int) Math.round(Math.sqrt((junggraph.getVertexCount()*6237.0)/0.707)));
        int width = ((int) Math.round((junggraph.getVertexCount()*6237.0)/height));
        Dimension dimension = new Dimension(width, height);
        layout.setSize(dimension);
        layout.setMaxIterations(2000);
        layout.setAttractionMultiplier(0.75); //higher value equals weaker force
        layout.setRepulsionMultiplier(0.75); //lower value equals weaker force
        //TODO: jung.FRLayout does not use seeds in all of its code --> change to something reproducible
        layout.setInitializer(new RandomLocationTransformer<>(dimension, Constants.SEED_JUNG));
        layout.initialize();
        // calculate layout
        while(!layout.done()) {
            layout.step();
        }
        // assign directions to edges
        for (Edge edge : sugy.getGraph().getEdges()) {
            // Fall mit gleichen Koordinaten wird nicht berücksichtigt
            Vertex node0 = edge.getPorts().get(0).getVertex();
            Vertex node1 = edge.getPorts().get(1).getVertex();
            if (layout.getY(node0.getLabelManager().getMainLabel().toString()) > layout.getY(node1.getLabelManager().getMainLabel().toString())) {
                // direct edge from 1 to 0
                sugy.assignDirection(edge, node1, node0);
            } else {
                // direct edge from 0 to 1
                sugy.assignDirection(edge, node0, node1);
            }
        }
    }

    public void breadthFirstSearch(SugiyamaLayouter sugy) {
        if (sugy.getGraph().getVertices().isEmpty()) {
            return;
        }
        Set<Vertex> doneVertices = new LinkedHashSet<>();
        LinkedList<Vertex> queue = new LinkedList<>();
        // use start node by random
        int random = (int)Math.floor(Constants.random.nextDouble()*sugy.getGraph().getVertices().size());
        queue.add(sugy.getGraph().getVertices().get(random));
        while (!queue.isEmpty()) {
            Vertex currentNode = queue.removeFirst();
            doneVertices.add(currentNode);
            Set<Edge> edges = new LinkedHashSet<>();
            for (PortComposition portComposition : currentNode.getPortCompositions()) {
                addEdgesRecursive(portComposition, edges);
            }
            for (Edge edge : edges) {
                if (sugy.getStartNode(edge) == null) {
                    Vertex start = currentNode;
                    Vertex end;
                    if (edge.getPorts().get(0).getVertex().equals(currentNode)) {
                        end = edge.getPorts().get(1).getVertex();
                    } else {
                        end = edge.getPorts().get(0).getVertex();
                    }
                    sugy.assignDirection(edge, start, end);
                    if (!(queue.contains(end) || doneVertices.contains(end))) {
                        queue.add(end);
                    }
                }
            }
        }
    }

    private void addEdgesRecursive (PortComposition portComposition, Set<Edge> edges) {
        if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                edges.add(edge);
            }
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup) portComposition).getPortCompositions()) {
                addEdgesRecursive(member, edges);
            }
        }
    }

}
