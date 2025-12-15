package de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.FRWSPDb_bMultiLevel;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityTesterForLayout;
import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

import java.awt.*;
import java.util.List;
import java.util.*;

public class DirectionAssignment {

    public AbstractLayout<Long, Long> bestFDLayout = null;
    public Map<Vertex, Long> nodeToLongBestFDLayout = null;

    public void randomDirected(SugiyamaLayouter sugy) {
        Map<Vertex, Integer> values = new LinkedHashMap<>();
        List<Vertex> vertices = new LinkedList<>(sugy.getGraph().getVertices());
        Collections.shuffle(vertices, Constants.random);
        for (int i = 0; i < vertices.size(); i++) {
            values.put(vertices.get(i), i);
        }
        for (Edge edge : sugy.getGraph().getEdges()) {
            Vertex node0 = edge.getStartPort().getVertex();
            Vertex node1 = edge.getEndPort().getVertex();
            if (values.get(node0) > values.get(node1)) {
                // direct edge from 1 to 0
                sugy.assignDirection(edge, node1, node0);
            } else {
                // direct edge from 0 to 1
                sugy.assignDirection(edge, node0, node1);
            }
        }
    }

    public void forceDirected(SugiyamaLayouter sugy) {
        forceDirected(sugy, 1);
    }

    /**
     *
     * @param sugy
     * @param numberOfIterations
     *      runs so many times with different random start positions and takes the layout producing the fewest crossings
     */
    public void forceDirected(SugiyamaLayouter sugy, int numberOfIterations) {
        //find the drawing with the fewest crossings
        int fewestCrossings = Integer.MAX_VALUE;

        for (int i = 0; i < numberOfIterations; i++) {
            // create new Jung graph
            UndirectedSparseGraph<Long, Long> junggraph = new UndirectedSparseGraph<>();
            Map<Vertex, Long> nodeToLong = new LinkedHashMap<>();
            long counter = 0;
            // add vertices and edges from original graph to the Jung graph
            for (Vertex node : sugy.getGraph().getVertices()) {
                junggraph.addVertex(counter);
                nodeToLong.put(node, counter);
                counter++;
            }
            for (Edge edge : sugy.getGraph().getEdges()) {
                junggraph.addEdge(
                        counter,
                        nodeToLong.get(edge.getStartPort().getVertex()),
                        nodeToLong.get(edge.getEndPort().getVertex())
                );
                counter++;
            }
            // calculate height and width so that each node has 6237 pixel space and the drawing space is proportional in size to DIN A4
            int height = ((int) Math.round(Math.sqrt((junggraph.getVertexCount() * 6237.0) / 0.707)));
            int width = ((int) Math.round((junggraph.getVertexCount() * 6237.0) / height));
            Dimension dimension = new Dimension(width, height);
            // create new force directed layout
            FRWSPDb_bMultiLevel<Long, Long> layout = new FRWSPDb_bMultiLevel<>(junggraph, 1.0, dimension,
                    Constants.random.nextLong());
            layout.setRecomputationOfSplitTreeAndWSPDFunction(new RecomputationOfSplitTreeAndWSPDFunction());
//            layout.setMaxIterations(2000);
//            layout.setAttractionMultiplier(0.75); //higher value equals weaker force
//            layout.setRepulsionMultiplier(0.75); //lower value equals weaker force
//            layout.setInitializer(new RandomLocationTransformer<>(dimension, Constants.random.nextLong()));
            layout.initialize();
            // calculate layout
            while (!layout.done()) {
                layout.step();
            }

            //check if we have a new best layout (fewest crossings) and if so save it
            QualityTesterForLayout<Long, Long> crossingCounter = new QualityTesterForLayout<>(layout);
            crossingCounter.calculateNumberOfEdgeCrossings();
            int crossings = crossingCounter.getNumberOfCrossings();
            if (crossings < fewestCrossings) {
                fewestCrossings = crossings;
                bestFDLayout = layout;
                nodeToLongBestFDLayout = nodeToLong;
            }
        }

        // assign directions to edges acc. to the best layout (the layout with the fewest crossings)
        for (Edge edge : sugy.getGraph().getEdges()) {
            Vertex node0 = edge.getStartPort().getVertex();
            Vertex node1 = edge.getEndPort().getVertex();
            if (bestFDLayout.getY(nodeToLongBestFDLayout.get(node0)) > bestFDLayout.getY(nodeToLongBestFDLayout.get(node1))) {
                // direct edge from 1 to 0
                sugy.assignDirection(edge, node1, node0);
            } else if (bestFDLayout.getY(nodeToLongBestFDLayout.get(node0)) == bestFDLayout.getY(nodeToLongBestFDLayout.get(node1))
                    // in case of same y-coordinate use x-coordinate
                    && bestFDLayout.getX(nodeToLongBestFDLayout.get(node0)) > bestFDLayout.getX(nodeToLongBestFDLayout.get(node1))) {
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
        int random = (int) Math.floor(Constants.random.nextDouble() * sugy.getGraph().getVertices().size());
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
                    Vertex end;
                    if (edge.getStartPort().getVertex().equals(currentNode)) {
                        end = edge.getEndPort().getVertex();
                    } else {
                        end = edge.getStartPort().getVertex();
                    }
                    sugy.assignDirection(edge, currentNode, end);
                    if (!(queue.contains(end) || doneVertices.contains(end))) {
                        queue.add(end);
                    }
                }
            }
        }
    }

    private void addEdgesRecursive(PortComposition portComposition, Set<Edge> edges) {
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
