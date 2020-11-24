package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;

import java.util.*;

public class PortSideAssignment {

    private SugiyamaLayouter sugy;
    private SortingOrder orders;

    public PortSideAssignment (SugiyamaLayouter sugy) {
        this.sugy = sugy;
        this.orders = sugy.getOrders();
        if (this.orders == null) {
            this.orders = new SortingOrder();
            sugy.setOrders(this.orders);
        }
    }

    /**
     * assign ports to vertex sides for regular vertices
     */
    public void assignPortsToVertexSides() {
        assignPortsToVertexSides(sugy.getGraph().getVertices());
    }

    /**
     * You may rather want to call {@link PortSideAssignment#assignPortsToVertexSides()} ?
     *
     * @param vertices
     * @return
     */
    public void assignPortsToVertexSides(Collection<Vertex> vertices) {
        for (Vertex node : vertices) {
            List<PortComposition> portCompositionsTop = new ArrayList<>();
            List<PortComposition> portCompositionsBottom = new ArrayList<>();
            Set<PortComposition> freePortCompositions = new LinkedHashSet<>();
            for (PortComposition portComposition : node.getPortCompositions()) {
                //for each port composition (usually a port group), compute a score that equals the number of edges
                // going upwards minus the number of edges going downwards. Depending on the sign of the score, we
                // will assign the port composition
                //Maybe vertex side is also predefined, then set it to a positive or negative value first
                int score = predefinedPortSide(portComposition);
                if (score == 0) {
                    score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                }
                //assign to side acc. to score
                if (score < 0) {
                    portCompositionsBottom.add(portComposition);
                }
                else if (score > 0) {
                    portCompositionsTop.add(portComposition);
                } else {
                    freePortCompositions.add(portComposition);
                }
            }
            // handle PortCompositions with no edges by adding them to the side with fewer ports
            for (PortComposition portComposition : freePortCompositions) {
                int portsTop = PortUtils.countPorts(portCompositionsTop);
                int portsBottom = PortUtils.countPorts(portCompositionsBottom);
                if (portsTop < portsBottom || (portsTop == portsBottom && Constants.random.nextDouble() < 0.5)) {
                    portCompositionsTop.add(portComposition);
                } else {
                    portCompositionsBottom.add(portComposition);
                }
            }
            // special case: if we have a plug, there cannot be both port groups on the same side
            if (sugy.isPlug(node)) {
                repairPortSidesOfPlug(portCompositionsTop, portCompositionsBottom);
            }

            List<Port> topPorts = PortUtils.getPortsRecursively(portCompositionsTop);
            orders.getTopPortOrder().put(node, topPorts);
            setContainedPortsToVertexSide(topPorts, Orientation.NORTH);
            List<Port> bottomPorts = PortUtils.getPortsRecursively(portCompositionsBottom);
            orders.getBottomPortOrder().put(node, bottomPorts);
            setContainedPortsToVertexSide(bottomPorts, Orientation.SOUTH);
        }
    }

    private void setContainedPortsToVertexSide(List<Port> ports, Orientation vertexSide) {
        for (Port port : ports) {
            port.setOrientationAtVertex(vertexSide);
        }
    }

    /**
     *
     * @param portComposition
     * @return
     *      a negative value if portComposition has more South side ports than North side ports,
     *      a positive value if portComposition has fewer South side ports than North side ports,
     *      and 0 if it has equally many or no North or South side ports.
     */
    private int predefinedPortSide(PortComposition portComposition) {
        boolean hasNorthSidePorts = false;
        boolean hasSouthSidePorts = false;

        int score = 0;
        for (Port port : PortUtils.getPortsRecursively(portComposition)) {
            if (port.getOrientationAtVertex() == Orientation.WEST
                    || port.getOrientationAtVertex() == Orientation.EAST) {
                System.out.println("Warning! Port " + port + " at vertex " + port.getVertex() + " has orientation " +
                        port.getOrientationAtVertex() + ", but this case is not yet implemented. Ignored orientation.");
            }
            else if (port.getOrientationAtVertex() == Orientation.NORTH) {
                hasNorthSidePorts = true;
                ++score;
            }
            else if (port.getOrientationAtVertex() == Orientation.SOUTH) {
                hasSouthSidePorts = true;
                --score;
            }
        }
        if (hasNorthSidePorts && hasSouthSidePorts) {
            System.out.println("Warning! A port group at vertex " + portComposition.getVertex() + " has both, ports " +
                    "assigned to " + Orientation.NORTH + " and to " + Orientation.SOUTH + ".");
        }

        return score;
    }

    private void repairPortSidesOfPlug(List<PortComposition> portCompositionsTop,
                                       List<PortComposition> portCompositionsBottom) {
        if (portCompositionsBottom.isEmpty()) {
            int lowestScore = Integer.MAX_VALUE;
            PortComposition pcLowestScore = null;
            for (PortComposition portComposition : portCompositionsTop) {
                int score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                if (score < lowestScore) {
                    lowestScore = score;
                    pcLowestScore = portComposition;
                }
            }
            portCompositionsTop.remove(pcLowestScore);
            portCompositionsBottom.add(pcLowestScore);
        }
        if (portCompositionsTop.isEmpty()) {
            int highestScore = Integer.MIN_VALUE;
            PortComposition pcHighestScore = null;
            for (PortComposition portComposition : portCompositionsBottom) {
                int score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                if (score > highestScore) {
                    highestScore = score;
                    pcHighestScore = portComposition;
                }
            }
            portCompositionsBottom.remove(pcHighestScore);
            portCompositionsTop.add(pcHighestScore);
        }
    }

    private int countEdgesUpwardsMinusEdgesDownwards(PortComposition portComposition) {
        Vertex node = portComposition.getVertex();
        int score = 0;
        if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                score += sugy.getStartNode(edge).equals(node) ? 1 : -1;
            }
        }
        else if (portComposition instanceof PortGroup) {
            for (PortComposition subPortComposition : ((PortGroup) portComposition).getPortCompositions()) {
                score += countEdgesUpwardsMinusEdgesDownwards(subPortComposition);
            }
        }
        return score;
    }
}
