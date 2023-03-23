package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import edu.uci.ics.jung.graph.util.Pair;

import java.util.*;

public class PortSideAssignment {

    private SugiyamaLayouter sugy;

    public PortSideAssignment (SugiyamaLayouter sugy) {
        this.sugy = sugy;
        if (sugy.getOrders() == null) {
            sugy.setOrders(new SortingOrder());
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

            List<Collection<PortComposition>> allPCsGrouped = new ArrayList<>(node.getPortCompositions().size());
            if (sugy.isPlug(node)) {
                // special case: if we have a plug, we need some port( group)s to be on the same side
                Pair<Set<PortComposition>> pairedPCs = fixPortSidesOfPlug(node);
                allPCsGrouped.add(pairedPCs.getFirst());
                allPCsGrouped.add(pairedPCs.getSecond());
                //find the other pcs of this plug that are not paired and also add them
                for (PortComposition pc : node.getPortCompositions()) {
                    if (!pairedPCs.getFirst().contains(pc) && !pairedPCs.getSecond().contains(pc)) {
                        allPCsGrouped.add(Collections.singleton(pc));
                    }
                }
            }
            else {
                for (PortComposition pc : node.getPortCompositions()) {
                    allPCsGrouped.add(Collections.singleton(pc));
                }
            }


            for (Collection<PortComposition> portCompositionGroup : allPCsGrouped) {
                //for each collection of port compositions (usually a single port group), compute a score that equals
                // the number of edges
                // going upwards minus the number of edges going downwards. Depending on the sign of the score, we
                // will assign the port composition.
                // Maybe vertex side is also predefined, then set it to a positive or negative value first
                int score = predefinedPortSide(portCompositionGroup);
                if (score == 0) {
                    score = countEdgesUpwardsMinusEdgesDownwards(portCompositionGroup);
                }
                //assign to side acc. to score
                if (score < 0) {
                    portCompositionsBottom.addAll(portCompositionGroup);
                }
                else if (score > 0) {
                    portCompositionsTop.addAll(portCompositionGroup);
                } else {
                    freePortCompositions.addAll(portCompositionGroup);
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

            List<Port> topPorts = PortUtils.getPortsRecursively(portCompositionsTop);
            sugy.getOrders().getTopPortOrder().put(node, topPorts);
            setContainedPortsToVertexSide(topPorts, Orientation.NORTH);
            List<Port> bottomPorts = PortUtils.getPortsRecursively(portCompositionsBottom);
            sugy.getOrders().getBottomPortOrder().put(node, bottomPorts);
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
     * @param portCompositions
     * @return
     *      a negative value if portComposition has more South side ports than North side ports,
     *      a positive value if portComposition has fewer South side ports than North side ports,
     *      and 0 if it has equally many or no North or South side ports.
     */
    private int predefinedPortSide(Collection<PortComposition> portCompositions) {
        boolean hasNorthSidePorts = false;
        boolean hasSouthSidePorts = false;
        Vertex northSidePortVertex = null;
        Vertex southSidePortVertex = null;

        int score = 0;
        for (Port port : PortUtils.getPortsRecursively(portCompositions)) {
            if (port.getOrientationAtVertex() == Orientation.WEST
                    || port.getOrientationAtVertex() == Orientation.EAST) {
                System.out.println("Warning! Port " + port + " at vertex " + port.getVertex() + " has orientation " +
                        port.getOrientationAtVertex() + ", but this case is not yet implemented. Ignored orientation.");
            }
            else if (port.getOrientationAtVertex() == Orientation.NORTH) {
                hasNorthSidePorts = true;
                northSidePortVertex = port.getVertex();
                ++score;
            }
            else if (port.getOrientationAtVertex() == Orientation.SOUTH) {
                hasSouthSidePorts = true;
                southSidePortVertex = port.getVertex();
                --score;
            }
        }
        if (hasNorthSidePorts && hasSouthSidePorts) {
            String vertexName = northSidePortVertex == southSidePortVertex ? northSidePortVertex.toString() :
                    northSidePortVertex + " and vertex " + southSidePortVertex;
            System.out.println("Warning! A port group at vertex " + vertexName + " has both, ports " +
                    "assigned to " + Orientation.NORTH + " and to " + Orientation.SOUTH + ".");
        }

        return score;
    }

    private int countEdgesUpwardsMinusEdgesDownwards(PortComposition portComposition) {
        return countEdgesUpwardsMinusEdgesDownwards(Collections.singleton(portComposition));
    }

    private int countEdgesUpwardsMinusEdgesDownwards(Collection<PortComposition> portCompositions) {
        int score = 0;
        for (PortComposition pc : portCompositions) {
            Vertex node = pc.getVertex();
            if (pc instanceof Port) {
                for (Edge edge : ((Port) pc).getEdges()) {
                    if (!sugy.staysOnSameLayer(edge)) {
                        score += sugy.getStartNode(edge).equals(node) ? 1 : -1;
                    }
                }
            } else if (pc instanceof PortGroup) {
                for (PortComposition subPortComposition : ((PortGroup) pc).getPortCompositions()) {
                    score += countEdgesUpwardsMinusEdgesDownwards(subPortComposition);
                }
            }
        }
        return score;
    }


    private Pair<Set<PortComposition>> fixPortSidesOfPlug(Vertex plug) {
        List<Pair<PortComposition>> pairedPairs = getPairedPairs(plug.getPortCompositions());

        Set<PortComposition> firstSide = new LinkedHashSet<>();
        Set<PortComposition> secondSide = new LinkedHashSet<>();

        while (!pairedPairs.isEmpty()) {
            //try to find a pair where we already know 1 endpoint
            Pair<PortComposition> nextPair = pairedPairs.get(0);
            for (Pair<PortComposition> pairedPair : pairedPairs) {
                if (firstSide.contains(pairedPair.getFirst()) || firstSide.contains(pairedPair.getSecond()) ||
                        secondSide.contains(pairedPair.getFirst()) || secondSide.contains(pairedPair.getSecond())) {
                    nextPair = pairedPair;
                    break;
                }
            }
            //now resolve this pair and remove it from the remaining paired pairs
            PortComposition first = nextPair.getFirst();
            PortComposition second = nextPair.getSecond();
            pairedPairs.remove(nextPair);

            if ((firstSide.contains(first) && firstSide.contains(second)) ||
                    (secondSide.contains(first) && secondSide.contains(second))) {
                System.out.println("Warning! Port Pairings at " + plug + " are not paired in a bipartite way. Some " +
                        "port pairings could not be added.");
                continue;
            }

            if (firstSide.contains(first)) {
                secondSide.add(second);
            }
            else if (firstSide.contains(second)) {
                secondSide.add(first);
            }
            else if (secondSide.contains(first)) {
                firstSide.add(second);
            }
            else if (secondSide.contains(second)) {
                firstSide.add(first);
            }
            else {
                firstSide.add(first);
                secondSide.add(second);
            }
        }

        return new Pair<>(firstSide, secondSide);
    }

    private List<Pair<PortComposition>> getPairedPairs(List<PortComposition> portCompositions) {
        List<Pair<PortComposition>> allConflictingPairs = new LinkedList<>();
        for (int i = 0; i < portCompositions.size() - 1; i++) {
            for (int j = i + 1; j < portCompositions.size(); j++) {
                PortComposition pc0 = portCompositions.get(i);
                PortComposition pc1 = portCompositions.get(j);
                if (havePortPairing(pc0, pc1)) {
                    allConflictingPairs.add(new Pair<>(pc0, pc1));
                }
            }
        }
        return allConflictingPairs;
    }

     private boolean havePortPairing(PortComposition pc0, PortComposition pc1) {
        List<Port> ports0 = PortUtils.getPortsRecursively(pc0);
        List<Port> ports1 = PortUtils.getPortsRecursively(pc1);
        for (Port port0 : ports0) {
            Port pairedPort = sugy.getPairedPort(port0);
            if (pairedPort != null && ports1.contains(pairedPort)) {
                return true;
            }
        }
        return false;
    }
}
