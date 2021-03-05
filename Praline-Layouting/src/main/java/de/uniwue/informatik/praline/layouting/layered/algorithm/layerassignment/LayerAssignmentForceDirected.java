package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionAssignment;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;

import java.util.*;
import java.util.function.Function;

public class LayerAssignmentForceDirected implements LayerAssignment {

    public static final Function<Integer, Integer> NUMER_OF_VERTICES_TO_NUMBER_OF_LAYERS =
            n -> (int) Math.ceil(Math.sqrt((double) n));

    private SugiyamaLayouter sugy;
    private AbstractLayout<Long, Long> fdLayout;
    private Map<Vertex, Long> node2fdLayoutNode;
    private List<Vertex> nodes;
    private SortingOrder orders;
    private Map<Vertex, Integer> ranks;

    public LayerAssignmentForceDirected(SugiyamaLayouter sugy, DirectionAssignment fdBaseDirectionAssignment) {
        this.sugy = sugy;
        this.fdLayout = fdBaseDirectionAssignment.bestFDLayout;
        this.node2fdLayoutNode = fdBaseDirectionAssignment.nodeToLongBestFDLayout;
        if (fdLayout == null || node2fdLayoutNode == null) {
            System.out.println("Warning! No force-directed input drawing found in LayerAssignmentForceDirected. This " +
                    "layer assignment won't work. Check parameter fdBaseDirectionAssignment.");
            return;
        }
        this.nodes = new ArrayList<>(node2fdLayoutNode.keySet());
        this.ranks = new LinkedHashMap<>(nodes.size());
        this.orders = sugy.getOrders();
        if (this.orders == null) {
            this.orders = new SortingOrder();
            sugy.setOrders(this.orders);
        }
    }


    @Override
    public Map<Vertex, Integer> assignLayers() {

        //find width of each node
        Map<Vertex, Double> node2width = new LinkedHashMap<>(nodes.size());
        double totalWidth = determineWidthOfVertices(node2width);

        //sort nodes by y-coordinate in force-directed drawing
        nodes.sort(Comparator.comparingDouble(v -> fdLayout.apply(node2fdLayoutNode.get(v)).getY()));

        //now assign the sorted nodes to layers starting from top to bottom and go to next layer when the current
        // layer has the ideal "fullness"
        double remainingWidth = totalWidth;
        int remainingLayers = NUMER_OF_VERTICES_TO_NUMBER_OF_LAYERS.apply(nodes.size());
        double idealWidthPerLayer = remainingWidth / (double) remainingLayers;

        int currentLayerIndex = 0;
        double currentLayerWidth = 0;
        List<Vertex> currentLayer = new ArrayList<>();
        orders.getNodeOrder().add(currentLayer);
        for (Vertex node : nodes) {
            double nodeWidth = node2width.get(node);

            double currentDiffToIdealLayerWidth = Math.abs(idealWidthPerLayer - currentLayerWidth);
            double nextDiffToIdealLayerWidth = Math.abs(idealWidthPerLayer - currentLayerWidth - nodeWidth);

            //if adding this node to the current layer would bring as further away from the ideal layer width
            // (instead of bringing us closer), we do not add it to the current layer, but to the next layer
            if (nextDiffToIdealLayerWidth > currentDiffToIdealLayerWidth && remainingLayers > 1) {
                //sort each layer additionally by x-coordinate
                currentLayer.sort(Comparator.comparingDouble(v -> fdLayout.apply(node2fdLayoutNode.get(v)).getX()));


                ++currentLayerIndex;
                --remainingLayers;
                currentLayer = new ArrayList<>();
                orders.getNodeOrder().add(currentLayer);
                currentLayerWidth = 0;
                idealWidthPerLayer = remainingWidth / (double) remainingLayers;
            }

            remainingWidth -= nodeWidth;
            currentLayerWidth += nodeWidth;
            currentLayer.add(node);
            ranks.put(node, currentLayerIndex);
        }

        return ranks;
    }

    private double determineWidthOfVertices(Map<Vertex, Double> node2width) {
        double totalWidth = 0;
        for (Vertex node : node2fdLayoutNode.keySet()) {
            double nodeWidth = sugy.getMinWidthForNode(node);
            node2width.put(node, nodeWidth);
            totalWidth += nodeWidth;
        }
        return totalWidth;
    }
}
