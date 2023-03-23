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

    public PortSideAssignment(SugiyamaLayouter sugy) {
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

            List<Pair<Set<PortComposition>>> pairedPCs = new ArrayList<>();
            List<PortComposition> allOtherPcs = new ArrayList<>(node.getPortCompositions().size());
            if (sugy.isPlug(node)) {
                // special case: if we have a plug, we need some port( group)s to be on the same side
                pairedPCs = fixPortSidesOfPlug(node);
                //find the other pcs of this plug that are not paired and also add them
                for (PortComposition pc : node.getPortCompositions()) {
                    if (!containsPc(pairedPCs, pc)) {
                        allOtherPcs.add(pc);
                    }
                }
            } else {
                allOtherPcs = node.getPortCompositions();
            }

            //handle groups of paired ports
            for (Pair<Set<PortComposition>> pair : pairedPCs) {
                //see comments below how scores are computed and how they are defined
                int scoreFirst = predefinedPortSide(pair.getFirst());
                int scoreSecond = predefinedPortSide(pair.getSecond());
                if ((scoreFirst < 0 && scoreSecond < 0) || (scoreFirst > 0 && scoreSecond > 0)) {
                    System.out.println("Warning! Predefined NORTH and SOUTH sides of ports could not all be fulfilled" +
                            " due to port pairings.");
                }
                //if they are both equal use 2nd criterion
                if (scoreFirst == scoreSecond) {
                    scoreFirst = countEdgesUpwardsMinusEdgesDownwards(pair.getFirst());
                    scoreSecond = countEdgesUpwardsMinusEdgesDownwards(pair.getSecond());
                }
                //if they are still the same -> do random decision
                if (scoreFirst == scoreSecond) {
                    if (Constants.random.nextBoolean()) {
                        scoreFirst = 1;
                        scoreSecond = -1;
                    } else {
                        scoreFirst = -1;
                        scoreSecond = 1;
                    }
                }
                //now assign them according to their score
                if (scoreFirst < scoreSecond) {
                    portCompositionsBottom.addAll(pair.getFirst());
                    portCompositionsTop.addAll(pair.getSecond());
                } else {
                    portCompositionsTop.addAll(pair.getFirst());
                    portCompositionsBottom.addAll(pair.getSecond());
                }
            }

            //handle all other
            for (PortComposition portComposition : allOtherPcs) {
                //for each collection of port compositions (usually a single port group), compute a score that equals
                // the number of edges
                // going upwards minus the number of edges going downwards. Depending on the sign of the score, we
                // will assign the port composition.
                // Maybe vertex side is also predefined, then set it to a positive or negative value first
                int score = predefinedPortSide(Collections.singleton(portComposition));
                if (score == 0) {
                    score = countEdgesUpwardsMinusEdgesDownwards(portComposition);
                }
                //assign to side acc. to score
                if (score < 0) {
                    portCompositionsBottom.add(portComposition);
                } else if (score > 0) {
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

            List<Port> topPorts = PortUtils.getPortsRecursively(portCompositionsTop);
            sugy.getOrders().getTopPortOrder().put(node, topPorts);
            setContainedPortsToVertexSide(topPorts, Orientation.NORTH);
            List<Port> bottomPorts = PortUtils.getPortsRecursively(portCompositionsBottom);
            sugy.getOrders().getBottomPortOrder().put(node, bottomPorts);
            setContainedPortsToVertexSide(bottomPorts, Orientation.SOUTH);
        }
    }

    private boolean containsPc(List<Pair<Set<PortComposition>>> pairedPCs, PortComposition pc) {
        for (Pair<Set<PortComposition>> pair : pairedPCs) {
            if (pair.getFirst().contains(pc) || pair.getSecond().contains(pc)) {
                return true;
            }
        }
        return false;
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


    private List<Pair<Set<PortComposition>>> fixPortSidesOfPlug(Vertex plug) {
        List<Pair<PortComposition>> pairedPairs = getPairedPairs(plug.getPortCompositions());

        List<Pair<Set<PortComposition>>> groups = new ArrayList<>();
        Set<PortComposition> oneSide = new LinkedHashSet<>();
        Set<PortComposition> otherSide = new LinkedHashSet<>();

        while (!pairedPairs.isEmpty()) {
            //try to find a pair where we already know 1 endpoint
            Pair<PortComposition> nextPair = null;
            for (Pair<PortComposition> pairedPair : pairedPairs) {
                if (oneSide.contains(pairedPair.getFirst()) || oneSide.contains(pairedPair.getSecond()) ||
                        otherSide.contains(pairedPair.getFirst()) || otherSide.contains(pairedPair.getSecond())) {
                    nextPair = pairedPair;
                    break;
                }
            }
            if (nextPair == null) {
                //we did not find another pair with a connection to what we have already -> start new group
                nextPair = pairedPairs.get(0);
                if (!oneSide.isEmpty()) {
                    groups.add(new Pair<>(oneSide, otherSide));
                    oneSide = new LinkedHashSet<>();
                    otherSide = new LinkedHashSet<>();
                }
            }
            //now resolve this pair and remove it from the remaining paired pairs
            PortComposition first = nextPair.getFirst();
            PortComposition second = nextPair.getSecond();
            pairedPairs.remove(nextPair);

            if ((oneSide.contains(first) && oneSide.contains(second)) ||
                    (otherSide.contains(first) && otherSide.contains(second))) {
                System.out.println("Warning! Port Pairings at " + plug + " are not paired in a bipartite way. Some " +
                        "port pairings could not be added.");
                continue;
            }

            if (oneSide.contains(first)) {
                otherSide.add(second);
            }
            else if (oneSide.contains(second)) {
                otherSide.add(first);
            }
            else if (otherSide.contains(first)) {
                oneSide.add(second);
            }
            else if (otherSide.contains(second)) {
                oneSide.add(first);
            }
            else {
                oneSide.add(first);
                otherSide.add(second);
            }
        }
        groups.add(new Pair<>(oneSide, otherSide));

        return groups;
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
