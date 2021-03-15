package de.uniwue.informatik.praline.layouting.layered.algorithm.drawing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.ArithmeticOperation;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.SortingOrder;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.ImplicitCharacteristics;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class DrawingPreparation {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private SortingOrder sortingOrder;
    private Map<Vertex, Set<Port>> dummyPortsForLabelPadding;
    private List<Port> dummyPortsForNodesWithoutPort;
    private double delta;
    private Map<Integer, Double> layer2shiftForUnionNodes;
    private boolean diableShifting = false;

    public DrawingPreparation (SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    public void initialize(DrawingInformation drawInfo, SortingOrder sortingOrder,
                           Map<Vertex, Set<Port>> dummyPortsForLabelPadding, List<Port> dummyPortsForNodesWithoutPort) {
        this.drawInfo = drawInfo;
        this.delta = Math.max(drawInfo.getEdgeDistanceHorizontal() - drawInfo.getPortWidth(),
                drawInfo.getPortSpacing());
        this.sortingOrder = sortingOrder;
        this.dummyPortsForLabelPadding = dummyPortsForLabelPadding;
        this.dummyPortsForNodesWithoutPort = dummyPortsForNodesWithoutPort;
        this.layer2shiftForUnionNodes = new LinkedHashMap<>();
    }

    public void prepareDrawing(DrawingInformation drawInfo, SortingOrder sortingOrder,
                               Map<Vertex, Set<Port>> dummyPortsForLabelPadding,
                               List<Port> dummyPortsForNodesWithoutPort) {
        initialize(drawInfo, sortingOrder, dummyPortsForLabelPadding, dummyPortsForNodesWithoutPort);
        // restore original elements
        restoreOriginalElements(false);
        //tighten nodes after unifying ports with multiple edges
        // we also handle vertex groups as one unit
        tightenNodes();
    }

    public void tightenNodes() {
        Set<VertexGroup> processedVertexGroups = new LinkedHashSet<>();
        for (Vertex node : sugy.getGraph().getVertices()) {
            VertexGroup vertexGroup = node.getVertexGroup();
            if (vertexGroup != null) {
                if (!processedVertexGroups.contains(vertexGroup)) {
                    tightenUnionNode(vertexGroup);
                    processedVertexGroups.add(vertexGroup);
                }
            }
            else {
                tightenNode(node);
            }
        }
    }

    private void tightenNode(Vertex node) {
        //TODO: currently some port labels are cut-off when they are broader than the port shape; make sure that this
        // does not happen in the future

        // tighten node to smallest width possible
        // find leftmost and rightmost Port
        VertexPortBounds vertexPortBounds = new VertexPortBounds(node).determine();
        double minL = vertexPortBounds.getMinL();
        double maxL = vertexPortBounds.getMaxL();
        double minR = vertexPortBounds.getMinR();
        double maxR = vertexPortBounds.getMaxR();
        // tighten to smallest width possible
        node.setShape(getReducedShape(node, minL, maxL, minR, maxR, true, true));
    }

    private void tightenUnionNode(VertexGroup vertexGroup) {
        //first determine start points for bottom and top vertices
        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double xMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        for (Vertex node : vertexGroup.getContainedVertices()) {
            Rectangle nodeShape = (Rectangle) node.getShape();
            yMin = Math.min(yMin, nodeShape.y);
            yMax = Math.max(yMax, nodeShape.y);
            xMin = Math.min(xMin, nodeShape.x);
            xMax = Math.max(xMax, nodeShape.x + nodeShape.getWidth());
        }

        //now determine port positions
        double minLBottom = Double.POSITIVE_INFINITY;
        double maxLBottom = Double.POSITIVE_INFINITY;
        Vertex vLBottom = null;
        double minLTop = Double.POSITIVE_INFINITY;
        double maxLTop = Double.POSITIVE_INFINITY;
        Vertex vLTop = null;
        double minRBottom = Double.NEGATIVE_INFINITY;
        double maxRBottom = Double.NEGATIVE_INFINITY;
        Vertex vRBottom = null;
        double minRTop = Double.NEGATIVE_INFINITY;
        double maxRTop = Double.NEGATIVE_INFINITY;
        Vertex vRTop = null;
        for (Vertex node : vertexGroup.getContainedVertices()) {
            VertexPortBounds vertexPortBounds = new VertexPortBounds(node).determine();
            Rectangle nodeShape = (Rectangle) node.getShape();

            if (nodeShape.y == yMin) {
                if (vertexPortBounds.getMinL() == xMin) {
                    vLBottom = node;
                    minLBottom = vertexPortBounds.getMinL();
                    maxLBottom = vertexPortBounds.getMaxL();
                }
                if (vertexPortBounds.getMaxR() == xMax) {
                    vRBottom = node;
                    minRBottom = vertexPortBounds.getMinR();
                    maxRBottom = vertexPortBounds.getMaxR();
                }
            }
            if (nodeShape.y == yMax) {
                if (vertexPortBounds.getMinL() == xMin) {
                    vLTop = node;
                    minLTop = vertexPortBounds.getMinL();
                    maxLTop = vertexPortBounds.getMaxL();
                }
                if (vertexPortBounds.getMaxR() == xMax) {
                    vRTop = node;
                    minRTop = vertexPortBounds.getMinR();
                    maxRTop = vertexPortBounds.getMaxR();
                }
            }
        }
        //if there are no ports on the bottom or top side, take values from the other side
        if (maxLTop == maxRTop || maxLTop == Double.POSITIVE_INFINITY) maxLTop = maxLBottom;
        if (maxRTop == maxLTop || maxRTop == Double.NEGATIVE_INFINITY) maxRTop = maxRBottom;
        if (minLTop == minRTop || minLTop == Double.POSITIVE_INFINITY) minLTop = minLBottom;
        if (minRTop == minLTop || minRTop == Double.NEGATIVE_INFINITY) minRTop = minRBottom;
        if (maxLBottom == maxRBottom || maxLBottom == Double.POSITIVE_INFINITY) maxLBottom = maxLTop;
        if (maxRBottom == maxLBottom || maxRBottom == Double.NEGATIVE_INFINITY) maxRBottom = maxRTop;
        if (minLBottom == minRBottom || minLBottom == Double.POSITIVE_INFINITY) minLBottom = minLTop;
        if (minRBottom == minLBottom || minRBottom == Double.NEGATIVE_INFINITY) minRBottom = minRTop;


        // tighten to smallest width possible
        //check bottom side
        Rectangle idealShapeLBottom =
                getReducedShape(vLBottom, minLBottom, maxLBottom, minRBottom, maxRBottom, true, vLBottom == vRBottom);
        Rectangle idealShapeRBottom =
                getReducedShape(vRBottom, minLBottom, maxLBottom, minRBottom, maxRBottom, vLBottom == vRBottom, true);
        //check top side
        Rectangle idealShapeLTop = getReducedShape(vLTop, minLTop, maxLTop, minRTop, maxRTop, true, vLTop == vRTop);
        Rectangle idealShapeRTop = getReducedShape(vRTop, minLTop, maxLTop, minRTop, maxRTop, vLTop == vRTop, true);
        //check device vertex (potentially in the middle)
        Vertex deviceVertex = ImplicitCharacteristics.getDeviceVertex(vertexGroup, sugy.getGraph());
        Rectangle idealShapeDevice = null;
        if (deviceVertex != null) {
            VertexPortBounds devicePortBounds = new VertexPortBounds(deviceVertex).determine();
            idealShapeDevice = getReducedShape(deviceVertex, devicePortBounds.getMinL(), devicePortBounds.getMaxL(),
                    devicePortBounds.getMinR(), devicePortBounds.getMaxR(), true, true);
        }

        //determine left border
        double xIdealLB = idealShapeLBottom == null ? Double.POSITIVE_INFINITY : idealShapeLBottom.x;
        double xIdealLT = idealShapeLTop == null ? Double.POSITIVE_INFINITY : idealShapeLTop.x;
        double xIdealRB = idealShapeRBottom == null ? Double.NEGATIVE_INFINITY :
                idealShapeRBottom.x + idealShapeRBottom.width;
        double xIdealRT = idealShapeRTop == null ? Double.NEGATIVE_INFINITY :
                idealShapeRTop.x + idealShapeRTop.width;

        double newL = Math.min(xIdealLB, xIdealLT);
        if (idealShapeDevice != null) {
            newL = Math.min(newL, idealShapeDevice.x);
        }
        //determine right border
        double newR = Math.max(xIdealRB, xIdealRT);
        if (idealShapeDevice != null) {
            newR = Math.max(newR, idealShapeDevice.x + idealShapeDevice.width);
        }
        //apply borders
        applyBorders(vLBottom, vRBottom, newL, newR);
        applyBorders(vLTop, vRTop, newL, newR);
        if (deviceVertex != null) {
            applyBorders(deviceVertex, deviceVertex, newL, newR);
        }
    }

    private void applyBorders(Vertex vL, Vertex vR, double newL, double newR) {
        if (vL != null) {
            Rectangle realShapeL = (Rectangle) vL.getShape();
            realShapeL.width -= Math.max(0, newL - realShapeL.x); //do not increase width (hence max)
            realShapeL.x = newL;
        }
        if (vR != null) {
            Rectangle realShapeR = (Rectangle) vR.getShape();
            realShapeR.width -= Math.max(0, (realShapeR.x + realShapeR.width) - newR); //do not increase width (hence max)
        }
    }

    /**
     *
     * @param node
     * @param minL
     * @param maxL
     * @param minR
     * @param maxR
     * @param tightenLeft
     * @param tightenRight
     * @return
     *      if it is not reduced, it returns the old shape instead
     */
    private Rectangle getReducedShape(Vertex node, double minL, double maxL, double minR, double maxR,
                                      boolean tightenLeft, boolean tightenRight) {
        if (node == null) {
            return null;
        }
        Rectangle nodeShape = (Rectangle) node.getShape();
        //check that mins and maxs are in scope of the node
        minL = Math.max(minL, nodeShape.x);
        maxL = Math.max(maxL, nodeShape.x);
        minR = Math.min(minR, nodeShape.x + nodeShape.width);
        maxR = Math.min(maxR, nodeShape.x + nodeShape.width);

        double size = nodeShape.getWidth();
        double minSize = minR - maxL;
        minSize = Math.max(minSize, (sugy.getMinWidthForNode(node) + (drawInfo.getBorderWidth() * 2)));
        Rectangle tightenedRectangle = nodeShape;
        if (minSize < size) {
            double dif = size - minSize;
            double newL = minL;
            double newR = maxR;
            if (tightenLeft && !tightenRight) {
                newL = Math.min(maxL, minL + dif);
            }
            else if (!tightenLeft && tightenRight) {
                newR = Math.max(minR, maxR - dif);
            }
            else if (tightenLeft && tightenRight) {
                if ((maxL - minL) < (dif / 2)) {
                    newL = maxL;
                    newR = maxR - (dif - (maxL - minL));
                } else if ((maxR - minR) < (dif / 2)) {
                    newL = minL + (dif - (maxR - minR));
                    newR = minR;
                } else {
                    newL = minL + (dif / 2);
                    newR = maxR - (dif / 2);
                }
            }
            tightenedRectangle =
                    new Rectangle(newL, nodeShape.getYPosition(), (newR - newL), nodeShape.getHeight(), null);
        }
        return tightenedRectangle;
    }

    private void shiftAllUpToRank(int rank, double shiftValue) {
        shiftAllUpToRank(rank, shiftValue, shiftValue);
    }

    private void shiftAllUpToRank(int rank, double shiftValue, double shiftValueEdges) {
        Set<Edge> edgesAlreadyShifted = new LinkedHashSet<>();
        for (int i = sugy.getMaxRank(); i >= rank; i--) {
            shift(i, shiftValue, shiftValueEdges, edgesAlreadyShifted);
        }
    }

    // shift all nodes of rank rank with their ports and all edgePaths to nodes below
    private void shift(int rank, double shiftValue, Set<Edge> edgesAlreadyShifted) {
        shift(rank, shiftValue, shiftValue, edgesAlreadyShifted);
    }

    // shift all nodes of rank rank with their ports and all edgePaths to nodes below
    private void shift(int rank, double shiftValue, double shiftValueEdges, Set<Edge> edgesAlreadyShifted) {
        if (diableShifting) {
            return;
        }
        for (Vertex node : sortingOrder.getNodeOrder().get(rank)) {
            Rectangle currentShape = (Rectangle) node.getShape();
            currentShape.y = currentShape.getY() + shiftValue;
            //shift top or bottom side first, such that no new overlaps occur
            List<List<Port>> portsToBeShifted = shiftValue > 0 ?
                    Arrays.asList(sortingOrder.getTopPortOrder().get(node), sortingOrder.getBottomPortOrder().get(node)) :
                    Arrays.asList(sortingOrder.getBottomPortOrder().get(node), sortingOrder.getTopPortOrder().get(node));
            for (List<Port> ports : portsToBeShifted) {
                for (Port port : ports) {
                    shiftPort(port, shiftValue);
                }
            }
            // shift edgePaths on the top side ports
            for (Port topPort : sortingOrder.getTopPortOrder().get(node)) {
                for (Edge edge : topPort.getEdges()) {
                    if (!edgesAlreadyShifted.contains(edge)) {
                        shiftInnerPartOfEdge(edge, shiftValueEdges);
                        edgesAlreadyShifted.add(edge);
                    }
                }
            }
        }
        // shift also edgePaths on the bottom sides one layer above if it has not been shifted
        if (rank < sugy.getMaxRank()) {
            for (Vertex node : sortingOrder.getNodeOrder().get(rank + 1)) {
                for (Port bottomPort : sortingOrder.getBottomPortOrder().get(node)) {
                    for (Edge edge : bottomPort.getEdges()) {
                        if (!edgesAlreadyShifted.contains(edge)) {
                            shiftInnerPartOfEdge(edge, shiftValueEdges);
                            edgesAlreadyShifted.add(edge);
                        }
                    }
                }
            }
        }
    }

    private void shiftInnerPartOfEdge(Edge edge, double shiftValue) {
        if (diableShifting) {
            return;
        }
        for (Path path : edge.getPaths()) {
            for (Point2D.Double innerPoint : ((PolygonalPath) path).getBendPoints()) {
                innerPoint.setLocation(innerPoint.getX(), (innerPoint.getY() + shiftValue));
            }
        }
    }

    private void shiftPort(PortComposition portComposition, double shiftValue) {
        if (diableShifting) {
            return;
        }
        if (portComposition instanceof Port) {
            for (Edge edge : ((Port) portComposition).getEdges()) {
                lengthenEdge(edge, (Rectangle) ((Port) portComposition).getShape(), shiftValue);
            }
            Rectangle currentShape = (Rectangle) ((Port)portComposition).getShape();
            currentShape.y = currentShape.getY() + shiftValue;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                shiftPort(member, shiftValue);
            }
        }
    }

    private void lengthenEdge(Edge edge, Rectangle portShape, double shiftValue) {
        for (Path path : edge.getPaths()) {
            for (Point2D.Double terminalPoint : ((PolygonalPath) path).getTerminalPoints()) {
                if (portShape.liesOnBoundary(terminalPoint)) {
                    terminalPoint.y += shiftValue;
                }
            }
        }
    }


    public void restoreOriginalElements(boolean disableShifting) {
        this.diableShifting = disableShifting;

        //<shifting involved>

        // The first two blocks involve shifting. For them we need a clear structure with edges going only between
        // neighboring layers. Hence they have to occur first before we replace dummy edges, unify their paths and
        // remove several types of dummy vertices contained in edges

        //The shifts are not done if this is disabled because e. g. we are resolving a Kieler layout

        //restore vertex groups
        for (Vertex vertex : new ArrayList<>(sugy.getGraph().getVertices())) {
            if (sugy.getVertexGroups().containsKey(vertex)) {
                VertexGroup vertexGroup = sugy.getVertexGroups().get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }

            if (sugy.getPlugs().containsKey(vertex)) {
                VertexGroup vertexGroup = sugy.getPlugs().get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }
        }


        //do extra shifts at nodes with ports with multiple edges, to redraw them -> they should go as skewed edges
        // to that port
        makeSpaceForSkewEdgesAtPortsWithMultipleEdges();

        //</shifting involved>


        //replace dummy edges
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
                if (sugy.getDummyEdge2RealEdge().containsKey(edge)) {
                    Edge originalEdge = sugy.getDummyEdge2RealEdge().get(edge);
                    replaceByOriginalEdge(edge, originalEdge);
                    hasChanged = true;
                }
            }
        }

        //until now all edges are deg 2 (so there are no hyperedges)
        //because they are composite of many different edge parts each contributing a path, they have multiple paths
        // now. We unify all the paths to one long path
        for (Edge edge : sugy.getGraph().getEdges()) {
            unifyPathsOfDeg2Edge(edge);
        }

        //unify single parts of hyperedges to one edge each
        for (Edge edge : new ArrayList<>(sugy.getGraph().getEdges())) {
            if (sugy.getHyperEdgeParts().containsKey(edge)) {
                restoreHyperedgePart(edge);
            }
        }

        //replace and remove dummy vertices and ports
        for (Port dummyPort : dummyPortsForNodesWithoutPort) {
            dummyPort.getVertex().removePortComposition(dummyPort);
        }
        for (Vertex vertex : new ArrayList<>(sugy.getGraph().getVertices())) {

            if (sugy.getHyperEdges().containsKey(vertex)) {
                replaceHyperEdgeDummyVertex(vertex);
            }

            if (dummyPortsForLabelPadding.containsKey(vertex)) {
                for (Port dummyPort : dummyPortsForLabelPadding.get(vertex)) {
                    vertex.removePortComposition(dummyPort);
                }
            }

            //remove all dummy nodes
            if (sugy.isDummy(vertex)) {
                sugy.getGraph().removeVertex(vertex);
            }
        }

        restoreEdgeBundles();

        //first we have already replaced in restoreVertexGroup (...) the ports that were created during vertex group
        // handling; these are the ports in replacedPorts where the original vertex is not in
        // multipleEdgePort2replacePorts

        //second we replace the ports that were created during the phase where ports with multiple edges were split to
        // multiple ports; now we re-unify all these ports back to one. If there is a port pairing involved, we keep
        // the one on the opposite site to the port pairing; otherwise we keep the/a middle one
        List<Port> portsWithMultipleEdgesToBeRestoredLater = new ArrayList<>();
        for (Port origPort : sugy.getMultipleEdgePort2replacePorts().keySet()) {
            restorePortsWithMultipleEdges(origPort, portsWithMultipleEdgesToBeRestoredLater);
        }
        //Due to ports with multiple edges on both sides of a port pairing, the order we restore these ports matters.
        // Hence, we may handle some ports of in another run to be executed in the following for-loop
        for (Port origPort : portsWithMultipleEdgesToBeRestoredLater) {
            restorePortsWithMultipleEdges(origPort, portsWithMultipleEdgesToBeRestoredLater);
        }

        //restore ports of devices that have nothing but a port pairing to a port of another vertex
        for (VertexGroup vertexGroup : sugy.getGraph().getVertexGroups()) {
            restorePortPairingsOfDeviceVertices(vertexGroup);
        }
    }

    private void restoreEdgeBundles() {
        //remove port groups introduced for edge bundles
        for (PortGroup dummyPortGroupForEdgeBundle : sugy.getDummyPortGroupsForEdgeBundles()) {
            //transfer current children to parent of this dummy port group (may be null then it's directly the vertex)
            PortGroup parent = dummyPortGroupForEdgeBundle.getPortGroup();
            PortUtils.movePortCompositionsToPortGroup(dummyPortGroupForEdgeBundle.getPortCompositions(), parent);
            //remove dummy port group
            Vertex vertex = dummyPortGroupForEdgeBundle.getVertex();
            if (vertex != null) {
                vertex.removePortComposition(dummyPortGroupForEdgeBundle);
            }
        }
        //re-add edges originally contained in edge bundles
        Map<EdgeBundle, Collection<Edge>> originalEdgeBundles = sugy.getOriginalEdgeBundles();
        for (EdgeBundle edgeBundle : originalEdgeBundles.keySet()) {
            edgeBundle.addEdges(originalEdgeBundles.get(edgeBundle));
        }
    }

    private void makeSpaceForSkewEdgesAtPortsWithMultipleEdges() {
        Set<Integer> layerBottomSidesAlreadyShifted = new LinkedHashSet<>();
        Set<Integer> layerTopSidesAlreadyShifted = new LinkedHashSet<>();

        for (Port portWithMultipleEdges : sugy.getMultipleEdgePort2replacePorts().keySet()) {
            List<Port> replacePorts = sugy.getMultipleEdgePort2replacePorts().get(portWithMultipleEdges);

            //shift everything (if it has not been done yet) to have space to add another bend to each edge
            boolean topPort = sugy.isTopPort(replacePorts.get(0));
            int rankOfLayer = sugy.getRank(replacePorts.get(0).getVertex());
            Set<Integer> layerAlreadyShifted = topPort ? layerTopSidesAlreadyShifted : layerBottomSidesAlreadyShifted;
            if (!layerAlreadyShifted.contains(rankOfLayer)) {
                double shiftValue = drawInfo.getEdgeDistanceVertical();
                //shift all layers above
                shiftAllUpToRank(rankOfLayer + 1, shiftValue, shiftValue);
                layerAlreadyShifted.add(rankOfLayer);
                //for the current layer shift the edges, but shift the nodes only if we have to make space for the
                // bottom side
                shift(rankOfLayer, topPort ? 0 : shiftValue, shiftValue, new LinkedHashSet<>());
            }
        }
    }

    private void restorePortsWithMultipleEdges(Port origPort, List<Port> portsWithMultipleEdgesToBeRestoredLater) {
        List<Port> replacePorts = sugy.getMultipleEdgePort2replacePorts().get(origPort);
        Vertex vertex = replacePorts.iterator().next().getVertex();
        VertexGroup vertexGroup = vertex.getVertexGroup();

        //special case: if the origPortPairing was itself also a replacePortPairing, we have to apply this
        // step in the right order to get the real original port pairing -- int the same as it was inserted in the map
        // sugy.getReplacedPortPairings() . This happens if on both sides of a port pairing, the port has multiple edges
        // We handle such a port in the end (after the opposite side port with multiple edges has been handled)
        if (checkIfWeHaveToRestoreItLater(origPort, replacePorts, vertexGroup,
                portsWithMultipleEdgesToBeRestoredLater)) {
            return;
        }

        Shape shapeOfPairedPort = null;
        PortGroup portGroupOfThisReplacement = replacePorts.iterator().next().getPortGroup();
        PortGroup containingPortGroup = portGroupOfThisReplacement.getPortGroup(); //second call because
        // first port group is just the port group created extra for the replace ports
        // re-add orig port
        if (containingPortGroup == null) {
            vertex.addPortComposition(origPort);
        }
        else {
            containingPortGroup.addPortComposition(origPort);
        }
        //remove all replace ports and possible save shape
        for (Port replacePort : replacePorts) {
            if (sugy.isPaired(replacePort)) {
                shapeOfPairedPort = replacePort.getShape();
            }
            //this if condition is not the same as the one above because there may be a port pairing with device port
            // that's been removed from the graph before, so sugy.isPaired(replacePort) would return false in this case
            if (vertexGroup != null && PortUtils.getPortPairing(replacePort, vertexGroup) != null) {
                //replace port pairing back to original
                PortPairing replacePortPairing = PortUtils.getPortPairing(replacePort, vertexGroup);
                PortPairing origPortPairing = sugy.getReplacedPortPairings().get(replacePortPairing);
                vertexGroup.removePortPairing(replacePortPairing);
                vertexGroup.addPortPairing(origPortPairing);
            }
            vertex.removePortComposition(replacePort);
        }
        vertex.removePortComposition(portGroupOfThisReplacement);
        //find shape of origPort
        if (shapeOfPairedPort != null) {
            origPort.setShape(shapeOfPairedPort.clone());
        }
        else {
            replacePorts.sort(Comparator.comparing(p -> p.getShape().getXPosition()));
            //pick the shape of the (left) middle one
            origPort.setShape(replacePorts.get((replacePorts.size() - 1) / 2).getShape().clone());
        }
        //re-hang edges
        //find target point
        Rectangle origPortShape = (Rectangle) origPort.getShape();
        Point2D.Double targetPoint = new Point2D.Double(
                origPortShape.getXPosition() + 0.5 * origPortShape.getWidth(),
                origPortShape.getYPosition() < vertex.getShape().getYPosition() ?
                        origPortShape.getYPosition() : origPortShape.getYPosition() + origPortShape.getHeight()
        );
        //re-draw edges
        for (Port replacePort : replacePorts) {
            for (Edge edge : new ArrayList<>(replacePort.getEdges())) {
                //change ports
                edge.removePort(replacePort);
                edge.addPort(origPort);
                //change drawn path at the port
                //make path of this edge part longer to reach the middle of the old dummy vertex
                Rectangle shapeReplacePort = (Rectangle) replacePort.getShape();
                if (shapeReplacePort.equals(origPortShape)) {
                    continue;
                }
                for (Path path : edge.getPaths()) {
                    Point2D.Double startPoint = ((PolygonalPath) path).getStartPoint();
                    Point2D.Double endPoint = ((PolygonalPath) path).getEndPoint();
                    Point2D.Double pointAtPort =  null;
                    Point2D.Double foreLastPoint = null;
                    Point2D.Double newForeLastPoint = new Point2D.Double();
                    if (shapeReplacePort.liesOnBoundary(startPoint)) {
                        pointAtPort = startPoint;
                        foreLastPoint = ((PolygonalPath) path).getBendPoints().isEmpty() ?
                                ((PolygonalPath) path).getEndPoint() :
                                ((PolygonalPath) path).getBendPoints().get(0);
                    }
                    else if (shapeReplacePort.liesOnBoundary(endPoint)) {
                        pointAtPort = endPoint;
                        foreLastPoint = ((PolygonalPath) path).getBendPoints().isEmpty() ?
                                ((PolygonalPath) path).getStartPoint() :
                                ((PolygonalPath) path).getBendPoints().get(
                                        ((PolygonalPath) path).getBendPoints().size() -1);
                    }
                    if (pointAtPort != null) {
                        newForeLastPoint.x = pointAtPort.x;
                        newForeLastPoint.y = pointAtPort.y + (diableShifting ? 0.75 : 1.0) *
                                drawInfo.getEdgeDistanceVertical() * (foreLastPoint.y > pointAtPort.y ? 1.0 : -1.0);
                        if (pointAtPort == startPoint) {
                            ((PolygonalPath) path).getBendPoints().add(0, newForeLastPoint);
                            startPoint.setLocation(targetPoint);
                        }
                        else {
                            ((PolygonalPath) path).getBendPoints().add(
                                    ((PolygonalPath) path).getBendPoints().size(), newForeLastPoint);
                            endPoint.setLocation(targetPoint);
                        }
                    }
                }
            }
        }
    }

    private boolean checkIfWeHaveToRestoreItLater(Port origPort, List<Port> replacePorts, VertexGroup vertexGroup,
                                                  List<Port> portsWithMultipleEdgesToBeRestoredLater) {
        for (Port replacePort : replacePorts) {
            if (vertexGroup != null && PortUtils.getPortPairing(replacePort, vertexGroup) != null) {
                PortPairing replacePortPairing = PortUtils.getPortPairing(replacePort, vertexGroup);
                PortPairing origPortPairing = sugy.getReplacedPortPairings().get(replacePortPairing);

                if (sugy.getReplacedPortPairings().containsKey(origPortPairing)
                        && origPortPairing.getPorts().contains(replacePort)) {
                    portsWithMultipleEdgesToBeRestoredLater.add(origPort);
                    return true;
                }
            }
        }
        return false;
    }

    private void restoreVertexGroup(Vertex dummyUnificationVertex, VertexGroup vertexGroup) {
        //find for each original vertex to which side of the unification vertex it has ports to the outside
        //-1: bottom side, 0: device vertex (whole length + can be both or in the middle) or undefined, 1: top side
        Map<Integer, List<Vertex>> vertexSide2origVertex = new LinkedHashMap<>();
        Map<Vertex, Double> minX = new LinkedHashMap<>();
        Map<Vertex, Double> maxX = new LinkedHashMap<>();
        Vertex dummyDeviceRepresenter = new Vertex();
        Vertex deviceVertex = null;
        Rectangle unionVertexShape = (Rectangle) dummyUnificationVertex.getShape();
        for (Vertex originalVertex : vertexGroup.getAllRecursivelyContainedVertices()) {
            int vertexSide = 0; //-1: bottom side, 0: device vertex or undefined, 1: top side
            boolean isDevice = sugy.getDeviceVertices().contains(originalVertex);
            if (isDevice) {
                deviceVertex = originalVertex;
                vertexSide2origVertex.putIfAbsent(0, new ArrayList<>());
                vertexSide2origVertex.get(0).add(deviceVertex);
                minX.put(deviceVertex, unionVertexShape.getXPosition());
                maxX.put(deviceVertex, unionVertexShape.getXPosition() + unionVertexShape.width);
            }
            Vertex consideredVertex = isDevice ? dummyDeviceRepresenter : originalVertex;
            for (Port port : sugy.getOrders().getTopPortOrder().get(dummyUnificationVertex)) {
                int changeTo = 1;
                vertexSide = changeVertexSideIfContained(minX, maxX, originalVertex, consideredVertex, vertexSide,
                        port, changeTo);
            }
            for (Port port : sugy.getOrders().getBottomPortOrder().get(dummyUnificationVertex)) {
                int changeTo = -1;
                vertexSide = changeVertexSideIfContained(minX, maxX, originalVertex, consideredVertex, vertexSide,
                        port, changeTo);
            }
            if (minX.containsKey(consideredVertex)) {
                vertexSide2origVertex.putIfAbsent(vertexSide, new ArrayList<>());
                vertexSide2origVertex.get(vertexSide).add(consideredVertex);
            }
        }

        //draw for each side of the unification vertex its contained original vertices next to each other in the order
        // found just in the step before
        int numberOfDifferentSides = vertexSide2origVertex.keySet().size();
        //adjust height of vertex by the number of sides it uses
        //for this also shift the drawing to get extra space
        double shiftNodeBy = diableShifting ? 0.0 : (double) (numberOfDifferentSides - 1) * drawInfo.getVertexHeight();
        double shiftLayerBy = 0;
        int layer = sugy.getRank(dummyUnificationVertex);
        if (layer2shiftForUnionNodes.containsKey(layer)) {
            if (layer2shiftForUnionNodes.get(layer) < shiftNodeBy) {
                shiftLayerBy = shiftNodeBy - layer2shiftForUnionNodes.get(layer);
                layer2shiftForUnionNodes.put(layer, shiftNodeBy);
            }
        }
        else if (shiftNodeBy > 0) {
            shiftLayerBy = shiftNodeBy;
            layer2shiftForUnionNodes.put(layer, shiftNodeBy);
        }
        if (shiftLayerBy > 0) {
            shiftAllUpToRank(layer, shiftLayerBy / 2.0, shiftLayerBy);
            if (layer < sugy.getMaxRank()) {
                shiftAllUpToRank(layer + 1, shiftLayerBy / 2.0, 0);
            }
        }
        unionVertexShape.height += shiftNodeBy;
        unionVertexShape.y -= shiftNodeBy / 2.0;
        for (Port port : sugy.getOrders().getBottomPortOrder().get(dummyUnificationVertex)) {
            shiftPort(port, -shiftNodeBy / 2.0);
        }
        for (Port port : sugy.getOrders().getTopPortOrder().get(dummyUnificationVertex)) {
            shiftPort(port, shiftNodeBy / 2.0);
        }

        //draw each original vertex
        int yShiftMultiplier = 0;
        for (int vertexSide = -1; vertexSide <= 1; vertexSide++) {
            List<Vertex> originalVertices = vertexSide2origVertex.get(vertexSide);
            if (originalVertices != null) {
                double xPos = unionVertexShape.getXPosition();
                //sort by x-coordinates
                originalVertices.sort(Comparator.comparing(minX::get));
                for (int j = 0; j < originalVertices.size(); j++) {
                    Vertex originalVertex = originalVertices.get(j);
                    //determine shape for original vertex
                    originalVertex.setShape(unionVertexShape.clone());
                    Rectangle originalVertexShape = (Rectangle) originalVertex.getShape();
                    originalVertexShape.height = originalVertexShape.height / (double) numberOfDifferentSides;
                    originalVertexShape.y = originalVertexShape.y +
                            (double) yShiftMultiplier * originalVertexShape.getHeight();
                    originalVertexShape.x = xPos;
                    double endXPos = j + 1 == originalVertices.size() ?
                            unionVertexShape.getXPosition() + unionVertexShape.width :
                            (maxX.get(originalVertex) + minX.get(originalVertices.get(j + 1))) / 2.0;
                    originalVertexShape.width = endXPos - xPos;
                    xPos = endXPos;
                    int indexUnionVertex = sortingOrder.getNodeOrder().get(layer).indexOf(dummyUnificationVertex);
                    if (originalVertex != dummyDeviceRepresenter) {
                        sugy.getGraph().addVertex(originalVertex);
                        sugy.setRank(originalVertex, sugy.getRank(dummyUnificationVertex));
                        sortingOrder.getNodeOrder().get(layer).add(indexUnionVertex, originalVertex);
                        sortingOrder.getBottomPortOrder().put(originalVertex, new ArrayList<>());
                        sortingOrder.getTopPortOrder().put(originalVertex, new ArrayList<>());
                    }
                }
                ++yShiftMultiplier;
            }
        }
        //transfer shape of the replace ports to the original ports
        Map<Vertex, List<Port>> bottomPortOrder = sortingOrder.getBottomPortOrder();
        for (Port replacePort : bottomPortOrder.get(dummyUnificationVertex)) {
            transferPortProperties(deviceVertex, bottomPortOrder, replacePort);
        }
        Map<Vertex, List<Port>> topPortOrder = sortingOrder.getTopPortOrder();
        for (Port replacePort : topPortOrder.get(dummyUnificationVertex)) {
            transferPortProperties(deviceVertex, topPortOrder, replacePort);
        }
        //remove unification dummy node
        sugy.getGraph().removeVertex(dummyUnificationVertex);
        //re-add vertex group
        sugy.getGraph().addVertexGroup(sugy.getPlugs().get(dummyUnificationVertex));
        sugy.getGraph().addVertexGroup(sugy.getVertexGroups().get(dummyUnificationVertex));
    }

    private void transferPortProperties(Vertex deviceVertex, Map<Vertex, List<Port>> portOrder, Port replacePort) {
        Port origPort = sugy.getReplacedPorts().get(replacePort);
        if (origPort != null ) { //may be null because it is a dummy port for label padding
            Vertex originalVertex = origPort.getVertex();
            portOrder.get(originalVertex).add(origPort);
            origPort.setShape(replacePort.getShape().clone());
            //adjust at port pairings
            if (sugy.getKeptPortPairings().containsKey(replacePort)) {
                sugy.getKeptPortPairings().put(origPort, sugy.getKeptPortPairings().get(replacePort));
                sugy.getKeptPortPairings().remove(replacePort);
            }
            //re-hang edges
            for (Edge edge : new ArrayList<>(replacePort.getEdges())) {
                edge.removePort(replacePort);
                edge.addPort(origPort);
            }
            //if we have a dummyDeviceRepresenter in use, we have to move the ports of the device
            if (deviceVertex != null && originalVertex == deviceVertex) {
                Rectangle deviceShape = (Rectangle) deviceVertex.getShape();
                Rectangle portShape = (Rectangle) origPort.getShape();
                if (deviceShape.y > portShape.y + portShape.height) {
                    shiftPort(origPort, deviceShape.y - portShape.height - portShape.y);
                }
                else if (deviceShape.y + deviceShape.height < portShape.y) {
                    shiftPort(origPort, deviceShape.y + deviceShape.height - portShape.y);
                }
            }
        }
    }

    private void restorePortPairingsOfDeviceVertices(VertexGroup vertexGroup) {

        Vertex deviceVertex = null;
        for (Vertex vertex : vertexGroup.getContainedVertices()) {
            if (sugy.getDeviceVertices().contains(vertex)) {
                deviceVertex = vertex;
            }
        }
        if (deviceVertex == null) {
            return;
        }
        Rectangle deviceShape = (Rectangle) deviceVertex.getShape();

        //check each port of the other vertices if they have a port pairing to the device vertex
        for (Vertex vertex : vertexGroup.getContainedVertices()) {
            if (vertex == deviceVertex) {
                continue;
            }
            Rectangle vertexShape = (Rectangle) vertex.getShape();
            for (Port port : vertex.getPorts()) {
                //port at the device is connected via a port pairing to a port of another node
                PortPairing portPairing = PortUtils.getPortPairing(port, deviceVertex.getVertexGroup());
                if (portPairing != null) {
                    Port devicePort = PortUtils.getOtherPortOfPortPairing(portPairing, port);
                    Rectangle devicePortShape = (Rectangle) port.getShape().clone();
                    if (devicePortShape.y < deviceShape.y) {
                        devicePortShape.y += vertexShape.height;
                    }
                    else {
                        devicePortShape.y -= vertexShape.height;
                    }
                    devicePort.setShape(devicePortShape);
                }
            }
        }
    }

    private int changeVertexSideIfContained(Map<Vertex, Double> minX, Map<Vertex, Double> maxX, Vertex originalVertex,
                                            Vertex vertexForMinMaxX, int vertexSide, Port port, int changeTo) {
        //two cases: (A) a port corresponding to an original port before unification, (B) a dummy port for padding
        //for case (A)
        Port portBeforeUnification = sugy.getReplacedPorts().get(port);
        //for case (B)
        Set<Port> dummyPortsForPadding =
                dummyPortsForLabelPadding != null && dummyPortsForLabelPadding.containsKey(originalVertex) ?
                dummyPortsForLabelPadding.get(originalVertex) : new LinkedHashSet<>();
        if (originalVertex.getPorts().contains(portBeforeUnification) || originalVertex.getPorts().contains(port)
                || (dummyPortsForPadding != null && dummyPortsForPadding.contains(port))) {
            vertexSide = changeTo;
            double xBeginPort = port.getShape().getXPosition();
            minX.putIfAbsent(vertexForMinMaxX, Double.POSITIVE_INFINITY);
            maxX.putIfAbsent(vertexForMinMaxX, Double.NEGATIVE_INFINITY);
            if (xBeginPort < minX.get(vertexForMinMaxX)) {
                minX.replace(vertexForMinMaxX, xBeginPort);
            }
            double xEndPort = xBeginPort + ((Rectangle) port.getShape()).width;
            if (xEndPort > maxX.get(vertexForMinMaxX)) {
                maxX.replace(vertexForMinMaxX, xEndPort);
            }
        }
        return vertexSide;
    }

    private void replaceHyperEdgeDummyVertex(Vertex hyperEdgeDummyVertex) {
        Rectangle vertexShape = (Rectangle) hyperEdgeDummyVertex.getShape();
        Edge hyperEdge = sugy.getHyperEdges().get(hyperEdgeDummyVertex);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double y = Double.NaN;
        Path firstPath = null;
        Path lastPath = null;
        for (Path path : hyperEdge.getPaths()) {
            Point2D.Double startPoint = ((PolygonalPath) path).getStartPoint();
            Point2D.Double endPoint = ((PolygonalPath) path).getEndPoint();
            if (vertexShape.containsInsideOrOnBoundary(startPoint)) {
                if (startPoint.x < minX) {
                    minX = startPoint.x;
                    firstPath = path;
                }
                if (startPoint.x > maxX) {
                    maxX = startPoint.x;
                    lastPath = path;
                }
                y = startPoint.y;
            }
            if (vertexShape.containsInsideOrOnBoundary(endPoint)) {
                if (endPoint.x < minX) {
                    minX = endPoint.x;
                    firstPath = path;
                }
                if (endPoint.x > maxX) {
                    maxX = endPoint.x;
                    lastPath = path;
                }
                y = endPoint.y;
            }
        }
        //add horizontal segment as replacement for the dummy vertex
        //we insert it as a connection between the first and the last path (which we unify)
        hyperEdge.removePath(firstPath);
        hyperEdge.removePath(lastPath);

        List<Point2D.Double> bendsFirstPath = new ArrayList<>(((PolygonalPath) firstPath).getTerminalAndBendPoints());
        List<Point2D.Double> bendsLastPath = new ArrayList<>(((PolygonalPath) lastPath).getTerminalAndBendPoints());

        if (ArithmeticOperation.precisionEqual(bendsFirstPath.get(0), new Point2D.Double(minX, y))) {
            Collections.reverse(bendsFirstPath);
        }
        if (!ArithmeticOperation.precisionEqual(bendsLastPath.get(0), new Point2D.Double(maxX, y))) {
            Collections.reverse(bendsLastPath);
        }

        List<Point2D.Double> bendsCombined = bendsFirstPath;
        bendsCombined.addAll(bendsLastPath);
        hyperEdge.addPath(new PolygonalPath(bendsCombined));
    }

    private void restoreHyperedgePart(Edge edgePart) {
        Vertex dummyVertexHyperEdge = sugy.getHyperEdgeParts().get(edgePart);
        Rectangle shapeDummyVertex = (Rectangle) dummyVertexHyperEdge.getShape();
        //make path of this edge part longer to reach the middle of the old dummy vertex
        Port portAtDummyVertex = PortUtils.getPortAtVertex(edgePart, dummyVertexHyperEdge);
        Rectangle shapeDummyPort = (Rectangle) portAtDummyVertex.getShape();
        Point2D.Double startPoint = ((PolygonalPath) edgePart.getPaths().get(0)).getStartPoint();
        Point2D.Double endPoint = ((PolygonalPath) edgePart.getPaths().get(0)).getEndPoint();
        Point2D.Double pointAtPort = shapeDummyPort.liesOnBoundary(startPoint) ?
                startPoint : shapeDummyPort.liesOnBoundary(endPoint) ? endPoint : null;
        double extraLength = shapeDummyPort.getHeight() + shapeDummyVertex.getHeight() / 2.0;
        if (pointAtPort.getY() < dummyVertexHyperEdge.getShape().getYPosition()) {
            //make longer in +y direction -> move point up
            pointAtPort.y = pointAtPort.y + extraLength;
        }
        else {
            //make longer in -y direction -> move point down
            pointAtPort.y = pointAtPort.y - extraLength;
        }
        //include dummy edge part into original hyperedge
        Edge originalEdge = sugy.getHyperEdges().get(dummyVertexHyperEdge);
        replaceByOriginalEdge(edgePart, originalEdge);
    }

    private void unifyPathsOfDeg2Edge(Edge edge) {
        //first find all segments of the edge paths
        Set<Line2D.Double> allSegments = new LinkedHashSet<>();
        for (Path path : edge.getPaths()) {
            allSegments.addAll(((PolygonalPath) path).getSegments());
        }
        //remove all points that are saved as a segment
        removePointsInSegments(allSegments);
        //now re-construct whole paths beginning from the start port
        Port startPort = edge.getPorts().get(0);
        Port endPort = edge.getPorts().get(1);
        Point2D.Double nextPoint = findSegmentPointAt((Rectangle) startPort.getShape(), allSegments);
        Point2D.Double endPoint = findSegmentPointAt((Rectangle) endPort.getShape(), allSegments);
        PolygonalPath newPath = new PolygonalPath();
        newPath.setStartPoint(nextPoint);
        newPath.setEndPoint(endPoint);
        //find all inner bend points
        Point2D.Double prevPoint = null;
        Point2D.Double curPoint = null;
        while (curPoint == null || !nextPoint.equals(endPoint)) {
            prevPoint = curPoint;
            curPoint = nextPoint;
            Line2D.Double curSegment = findSegmentAt(curPoint, allSegments);
            allSegments.remove(curSegment);
            nextPoint = getOtherEndPoint(curSegment, curPoint);

            if (prevPoint != null && !areOnALine(prevPoint, curPoint, nextPoint)) {
                newPath.getBendPoints().add(curPoint);
            }
        }
        //add new path and remove all old ones
        edge.removeAllPaths();
        edge.addPath(newPath);
    }

    private void removePointsInSegments(Set<Line2D.Double> segments) {
        for (Line2D.Double segment : new ArrayList<>(segments)) {
            if (segment.getP1().equals(segment.getP2())) {
                segments.remove(segment);
            }
        }
    }

    private boolean areOnALine(Point2D.Double prevPoint, Point2D.Double curPoint, Point2D.Double nextPoint) {
        return new Line2D.Double(prevPoint, nextPoint).ptSegDist(curPoint) == 0;
    }

    private Point2D.Double getOtherEndPoint(Line2D.Double segment, Point2D.Double point) {
        if (segment.getP1().equals(point)) {
            return (Point2D.Double) segment.getP2();
        }
        return (Point2D.Double) segment.getP1();
    }

    private Line2D.Double findSegmentAt(Point2D.Double point, Set<Line2D.Double> allSegments) {
        for (Line2D.Double segment : allSegments) {
            if (point.equals(segment.getP1())) {
                return segment;
            }
            if (point.equals(segment.getP2())) {
                return segment;
            }
        }
        return null;
    }

    private Point2D.Double findSegmentPointAt(Rectangle portRectangle, Set<Line2D.Double> allSegments) {
        for (Line2D.Double segment : allSegments) {
            if (portRectangle.containsInsideOrOnBoundary((Point2D.Double) segment.getP1())) {
                return (Point2D.Double) segment.getP1();
            }
            if (portRectangle.containsInsideOrOnBoundary((Point2D.Double) segment.getP2())) {
                return (Point2D.Double) segment.getP2();
            }
        }
        return null;
    }

    private void replaceByOriginalEdge(Edge dummyEdge, Edge originalEdge) {
        if (!sugy.getGraph().getEdges().contains(originalEdge)) {
            sugy.getGraph().addEdge(originalEdge);
            if (!originalEdge.getPaths().isEmpty()) {
                //TODO this was introduced to avoid doubling of edge paths. but ideally that should not be necessary
                // (and this could even lead to new problems) -- so better fix edge-path insertion and edge-removal
                // in EdgeRouting and DrawingPreparation
                originalEdge.removeAllPaths();
            }
        }
        //transfer the paths form the dummy to the original edge
        originalEdge.addPaths(dummyEdge.getPaths());
        //add ports of dummy edge to original edge
        for (Port port : dummyEdge.getPorts()) {
            Vertex vertex = port.getVertex();
            if (!sugy.isDummyTurningNode(vertex) && !sugy.isDummyNodeOfLongEdge(vertex)
                    && !originalEdge.getPorts().contains(port)) {
                originalEdge.addPort(port);
            }
        }
        sugy.getGraph().removeEdge(dummyEdge);
    }

    private class VertexPortBounds {
        private Vertex node;
        private Rectangle nodeShape;
        private double minL;
        private double maxL;
        private double minR;
        private double maxR;

        public VertexPortBounds(Vertex node) {
            this.node = node;
        }

        public Rectangle getNodeShape() {
            return nodeShape;
        }

        public double getMinL() {
            return minL;
        }

        public double getMaxL() {
            return maxL;
        }

        public double getMinR() {
            return minR;
        }

        public double getMaxR() {
            return maxR;
        }

        public VertexPortBounds determine() {
            nodeShape = (Rectangle) node.getShape();
            minL = nodeShape.getXPosition();
            maxL = nodeShape.getXPosition() + nodeShape.getWidth();
            minR = minL;
            maxR = maxL;
            for (Port port : node.getPorts()) {
                Rectangle portShape = (Rectangle) port.getShape();
                if (!Double.isNaN(portShape.getXPosition())) {
                    //either the port shape or the port label determines the required width
                    maxL = Math.min(maxL, portShape.getXPosition() +
                            Math.min(0, drawInfo.getHorizontalPortLabelOffset()) - delta);
                    minR = Math.max(minR, portShape.getXPosition() + Math.max(portShape.getWidth(),
                            drawInfo.computePortWidth(port) + Math.max(0, -drawInfo.getHorizontalPortLabelOffset()))
                                    + delta);
                }
            }
            return this;
        }
    }
}