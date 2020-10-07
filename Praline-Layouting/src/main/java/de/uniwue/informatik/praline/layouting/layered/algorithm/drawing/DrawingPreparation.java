package de.uniwue.informatik.praline.layouting.layered.algorithm.drawing;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.utils.PortUtils;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CMResult;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.ImplicitCharacteristics;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class DrawingPreparation {

    private SugiyamaLayouter sugy;
    private DrawingInformation drawInfo;
    private CMResult cmResult;
    private Map<Vertex, Set<Port>> dummyPortsForLabelPadding;
    private double delta;

    public DrawingPreparation (SugiyamaLayouter sugy) {
        this.sugy = sugy;
    }

    private void initialise(DrawingInformation drawInfo, CMResult cmResult,
                            Map<Vertex, Set<Port>> dummyPortsForLabelPadding) {
        this.drawInfo = drawInfo;
        this.delta = Math.max(drawInfo.getEdgeDistanceHorizontal(), drawInfo.getPortWidth() + drawInfo.getPortSpacing());
        this.cmResult = cmResult;
        this.dummyPortsForLabelPadding = dummyPortsForLabelPadding;
    }

    public void prepareDrawing(DrawingInformation drawInfo, CMResult cmResult,
                               Map<Vertex, Set<Port>> dummyPortsForLabelPadding) {
        initialise(drawInfo, cmResult, dummyPortsForLabelPadding);
        // do path for edges
        doPathForEdges();
        // adjust port shapes
        adjustPortShapes();
        // add Edges with Paths for remaining dummyNodes
        drawEdgesForDummys();
        // restore original elements
        restoreOriginalElements();
        //tighten nodes after unifying ports with multiple edges
        // we also handle vertex groups as one unit
        tightenNodes();
    }

    private void tightenNodes() {
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
        double yMin = Double.MAX_VALUE;
        double yMax = Double.MIN_VALUE;
        for (Vertex node : vertexGroup.getContainedVertices()) {
            Rectangle nodeShape = (Rectangle) node.getShape();
            yMin = Math.min(yMin, nodeShape.y);
            yMax = Math.max(yMax, nodeShape.y);
        }

        //now determine port positions
        double minLBottom = Double.MAX_VALUE;
        double maxLBottom = Double.MAX_VALUE;
        Vertex vLBottom = null;
        double minLTop = Double.MAX_VALUE;
        double maxLTop = Double.MAX_VALUE;
        Vertex vLTop = null;
        double minRBottom = Double.MIN_VALUE;
        double maxRBottom = Double.MIN_VALUE;
        Vertex vRBottom = null;
        double minRTop = Double.MIN_VALUE;
        double maxRTop = Double.MIN_VALUE;
        Vertex vRTop = null;
        for (Vertex node : vertexGroup.getContainedVertices()) {
            VertexPortBounds vertexPortBounds = new VertexPortBounds(node).determine();
            Rectangle nodeShape = (Rectangle) node.getShape();

            if (nodeShape.y == yMin) {
                yMin = nodeShape.y;
                if (vertexPortBounds.getMinL() < minLBottom) {
                    vLBottom = node;
                    minLBottom = vertexPortBounds.getMinL();
                    maxLBottom = vertexPortBounds.getMaxL();
                }
                if (vertexPortBounds.getMaxR() > maxRBottom) {
                    vRBottom = node;
                    minRBottom = vertexPortBounds.getMinR();
                    maxRBottom = vertexPortBounds.getMaxR();
                }
            }
            if (nodeShape.y == yMax) {
                yMax = nodeShape.y;
                if (vertexPortBounds.getMinL() < minLTop) {
                    vLTop = node;
                    minLTop = vertexPortBounds.getMinL();
                    maxLTop = vertexPortBounds.getMaxL();
                }
                if (vertexPortBounds.getMaxR() > maxRTop) {
                    vRTop = node;
                    minRTop = vertexPortBounds.getMinR();
                    maxRTop = vertexPortBounds.getMaxR();
                }
            }
        }
        //if there are no ports on the bottom or top side, take values from the other side
        if (maxLTop == maxRTop) maxLTop = maxLBottom;
        if (minRTop == minLTop) minRTop = minRBottom;
        if (maxLBottom == maxRBottom) maxLBottom = maxLTop;
        if (minRBottom == minLBottom) minRBottom = minRTop;


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
            double deviceMaxL = Math.min(devicePortBounds.getMaxL(), Math.min(maxLBottom, maxLTop));
            double deviceMinR = Math.min(devicePortBounds.getMinR(), Math.min(minRBottom, minRTop));
            idealShapeDevice = getReducedShape(deviceVertex, devicePortBounds.getMinL(), deviceMaxL,
                    deviceMinR, devicePortBounds.getMaxR(), true, true);
        }

        //determine left border
        double newL = Math.min(idealShapeLBottom.x, idealShapeLTop.x);
        if (idealShapeDevice != null) {
            newL = Math.min(newL, idealShapeDevice.x);
        }
        //determine right border
        double newR = Math.max(idealShapeRBottom.x + idealShapeRBottom.width, idealShapeRTop.x + idealShapeRTop.width);
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
        Rectangle realShapeL = (Rectangle) vL.getShape();
        realShapeL.width -= Math.max(0, newL - realShapeL.x);
        realShapeL.x = Math.max(realShapeL.x, newL);
        Rectangle realShapeR = (Rectangle) vR.getShape();
        realShapeR.width -= Math.max(0, (realShapeR.x + realShapeR.width) - newR);
        realShapeR.x = Math.min(realShapeR.x, newR);
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
        Rectangle nodeShape = (Rectangle) node.getShape();
        //check that mins and maxs are in scope of the node
        minL = Math.max(minL, nodeShape.x);
        maxL = Math.max(maxL, nodeShape.x);
        minR = Math.min(minR, nodeShape.x + nodeShape.width);
        maxR = Math.min(maxR, nodeShape.x + nodeShape.width);

        double size = nodeShape.getWidth();
        double minSize = minR - maxL;
        minSize = Math.max(minSize, (sugy.getTextWidthForNode(node) + (drawInfo.getBorderWidth() * 2)));
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

    private void doPathForEdges() {
        for (Edge edge : sugy.getGraph().getEdges()) {
            Port p1 = edge.getPorts().get(0);
            Port p2 = edge.getPorts().get(1);
            // create path; else update end-point-positions
            if (edge.getPaths().isEmpty()) {
                if (sugy.isTopPort(p1)) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                Point2D.Double start = new Point2D.Double(p1.getShape().getXPosition(), (p1.getShape().getYPosition() - drawInfo.getPortHeight()));
                Point2D.Double end = new Point2D.Double(p2.getShape().getXPosition(), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                edge.addPath(new PolygonalPath(start, end, new LinkedList<>()));
            } else {
                PolygonalPath path = (PolygonalPath) edge.getPaths().get(0);
                Point2D.Double start = path.getStartPoint();
                Point2D.Double end = path.getEndPoint();
                if (end.getX() == p1.getShape().getXPosition()) {
                    p1 = edge.getPorts().get(1);
                    p2 = edge.getPorts().get(0);
                }
                if (sugy.isTopPort(p1)) {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setStartPoint(new Point2D.Double(start.getX(), (start.getY() - drawInfo.getPortHeight())));
                }
                if (sugy.isTopPort(p2)) {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() + drawInfo.getPortHeight())));
                } else {
                    path.setEndPoint(new Point2D.Double(end.getX(), (end.getY() - drawInfo.getPortHeight())));
                }
            }
        }
    }

    private void adjustPortShapes() {
        for (Vertex vertex : sugy.getGraph().getVertices()) {
            for (Port port : vertex.getPorts()) {
                Rectangle portShape = (Rectangle) port.getShape();
                portShape.x = portShape.getXPosition() - (drawInfo.getPortWidth() / 2);
                if (!sugy.isTopPort(port)) {
                    portShape.y = portShape.getYPosition() - drawInfo.getPortHeight();
                }
            }
        }
    }

    private void drawEdgesForDummys() {
        Collection<Vertex> vertices;
        vertices = new LinkedHashSet<>(sugy.getGraph().getVertices());
        for (Vertex node : vertices) {
            if (sugy.isDummy(node)) {
                Port p1 = (Port) node.getPortCompositions().get(0);
                Port p2 = (Port) node.getPortCompositions().get(1);
                Edge originalEdge = sugy.getDummyEdge2RealEdge().get(p1.getEdges().get(0));
                List<Port> portsForNewEdge = new LinkedList<>();
                portsForNewEdge.add(p1);
                portsForNewEdge.add(p2);
                Edge newEdge = new Edge(portsForNewEdge);
                Point2D.Double start;
                Point2D.Double end;
                if (sugy.isTopPort(p1)) {
                    if (sugy.isTopPort(p2)){
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                    } else {
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition() + drawInfo.getPortHeight()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition()));
                    }
                } else {
                    if (sugy.isTopPort(p2)){
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition() + drawInfo.getPortHeight()));
                    } else {
                        start = new Point2D.Double(p1.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p1.getShape().getYPosition()));
                        end = new Point2D.Double(p2.getShape().getXPosition() + (drawInfo.getPortWidth() / 2), (p2.getShape().getYPosition()));
                    }
                }
                newEdge.addPath(new PolygonalPath(start, end, new LinkedList<>()));
                sugy.getGraph().addEdge(newEdge);
                sugy.getDummyEdge2RealEdge().put(newEdge, originalEdge);
                Vertex endNode0 = portsForNewEdge.get(0).getVertex();
                Vertex endNode1 = portsForNewEdge.get(1).getVertex();
                sugy.assignDirection(newEdge,
                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode0 : endNode1,
                        sugy.getRank(endNode0) < sugy.getRank(endNode1) ? endNode1 : endNode0);
            }
        }
    }

    // shift all nodes of rank rank with their ports and all edgePaths to nodes below
    private void shift (double shiftValue, int rank, boolean shiftEdges) {
        for (Vertex node : cmResult.getNodeOrder().get(rank)) {
            Rectangle currentShape = (Rectangle) node.getShape();
            currentShape.y = currentShape.getY() + shiftValue;
            for (PortComposition portComposition : node.getPortCompositions()) {
                shift(shiftValue, portComposition);
            }
            if (shiftEdges) {
                // shift edgePaths
                for (Port bottomPort : cmResult.getBottomPortOrder().get(node)) {
                    for (Edge edge : bottomPort.getEdges()) {
                        if (!edge.getPaths().isEmpty()) {
                            for (Point2D.Double pathPoint : ((PolygonalPath) edge.getPaths().get(0)).getTerminalAndBendPoints()) {
                                pathPoint.setLocation(pathPoint.getX(), (pathPoint.getY() + shiftValue));
                            }
                        }
                    }
                }
            }
        }
    }

    private void shift (double shiftValue, PortComposition portComposition) {
        if (portComposition instanceof Port) {
            Rectangle currentShape = (Rectangle) ((Port)portComposition).getShape();
            currentShape.y = currentShape.getY() + shiftValue;
        } else if (portComposition instanceof PortGroup) {
            for (PortComposition member : ((PortGroup)portComposition).getPortCompositions()) {
                shift(shiftValue, member);
            }
        }
    }


    public void restoreOriginalElements () {
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
        for (Vertex vertex : new ArrayList<>(sugy.getGraph().getVertices())) {

            if (sugy.getHyperEdges().containsKey(vertex)) {
                replaceHyperEdgeDummyVertex(vertex);
            }

            if (sugy.getVertexGroups().containsKey(vertex)) {
                VertexGroup vertexGroup = sugy.getVertexGroups().get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }

            if (sugy.getPlugs().containsKey(vertex)) {
                VertexGroup vertexGroup = sugy.getPlugs().get(vertex);
                restoreVertexGroup(vertex, vertexGroup);
            }

            if (dummyPortsForLabelPadding.containsKey(vertex)) {
                for (Port dummyPort : dummyPortsForLabelPadding.get(vertex)) {
                    vertex.removePortComposition(dummyPort);
                }
            }

            if (sugy.getDummyTurningNodes() != null && sugy.getDummyTurningNodes().containsKey(vertex)) {
                sugy.getGraph().removeVertex(vertex);
            }

            if (sugy.getDummyNodesLongEdges().containsKey(vertex)) {
                sugy.getGraph().removeVertex(vertex);
            }
        }

        //TODO: add ports without edges, loop-edges, ports with loop-edges, ports of vertexGroup that are paired
        // within the vertex group but do not have outgoing edges
        //TODO: check that #ports, #vertices, #edges is in the end the same as at the beginning

        //first we have already replaced in restoreVertexGroup (...) the ports that were created during vertex group
        // handeling; these are the ports in replacedPorts where the original vertex is not in
        // multipleEdgePort2replacePorts

        //second we replace the ports that were created during the phase where ports with multiple edges were split to
        // multiple ports; now we re-unify all these ports back to one. If there is a port pairing involved, we keep
        // the one on the opposite site to the port pairing; otherwise we keep the/a middle one
        for (Port origPort : sugy.getMultipleEdgePort2replacePorts().keySet()) {
            restorePortsWithMultipleEdges(origPort);
        }
    }

    private void restorePortsWithMultipleEdges(Port origPort) {
        List<Port> replacePorts = sugy.getMultipleEdgePort2replacePorts().get(origPort);
        Shape shapeOfPairedPort = null;
        Vertex vertex = replacePorts.iterator().next().getVertex();
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
                        newForeLastPoint.y = pointAtPort.y +
                                //TODO: transform the .75 into a variable in DrawingInformation
                                drawInfo.getEdgeDistanceVertical() * (foreLastPoint.y > pointAtPort.y ? .75 : -.75);
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

    private void restoreVertexGroup(Vertex dummyUnificationVertex, VertexGroup vertexGroup) {
        //find for each original vertex to which side of the unification vertex it has ports to the outside
        //-1: bottom side, 0: device vertex (whole length + can be both or in the middle) or undefined, 1: top side
        Map<Integer, List<Vertex>> vertexSide2origVertex = new LinkedHashMap<>();
        Map<Vertex, Double> minX = new LinkedHashMap<>();
        Map<Vertex, Double> maxX = new LinkedHashMap<>();
        Vertex dummyDeviceRepresenter = new Vertex();
        Shape unionVertexShape = dummyUnificationVertex.getShape();
        for (Vertex originalVertex : vertexGroup.getAllRecursivelyContainedVertices()) {
            int vertexSide = 0; //-1: bottom side, 0: device vertex or undefined, 1: top side
            boolean isDevice = sugy.getDeviceVertices().contains(originalVertex);
            if (isDevice) {
                vertexSide2origVertex.putIfAbsent(vertexSide, new ArrayList<>());
                vertexSide2origVertex.get(vertexSide).add(originalVertex);
                minX.put(originalVertex, unionVertexShape.getXPosition());
                maxX.put(originalVertex, unionVertexShape.getXPosition() + ((Rectangle) unionVertexShape).width);
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
                            unionVertexShape.getXPosition() +
                                    ((Rectangle) unionVertexShape).width :
                            (maxX.get(originalVertex) + minX.get(originalVertices.get(j + 1))) / 2.0;
                    originalVertexShape.width = endXPos - xPos;
                    xPos = endXPos;
                    if (originalVertex != dummyDeviceRepresenter) {
                        sugy.getGraph().addVertex(originalVertex);
                    }
                }
                ++yShiftMultiplier;
            }
        }
        //transfer shape of the replace ports to the original ports
        for (Port replacePort : dummyUnificationVertex.getPorts()) {
            Port origPort = sugy.getReplacedPorts().get(replacePort);
            if (origPort != null ) { //may be null because it is a dummy port for label padding
                origPort.setShape(replacePort.getShape().clone());
                if (sugy.getKeptPortPairings().containsKey(replacePort)) {
                    sugy.getKeptPortPairings().put(origPort, sugy.getKeptPortPairings().get(replacePort));
                    sugy.getKeptPortPairings().remove(replacePort);
                }
                //re-hang edges
                for (Edge edge : new ArrayList<>(replacePort.getEdges())) {
                    edge.removePort(replacePort);
                    edge.addPort(origPort);
                }
            }
        }
        //remove unification dummy node
        sugy.getGraph().removeVertex(dummyUnificationVertex);
        //re-add vertex group
        sugy.getGraph().addVertexGroup(sugy.getPlugs().get(dummyUnificationVertex));
        sugy.getGraph().addVertexGroup(sugy.getVertexGroups().get(dummyUnificationVertex));
    }

    private int changeVertexSideIfContained(Map<Vertex, Double> minX, Map<Vertex, Double> maxX, Vertex originalVertex,
                                            Vertex vertexForMinMaxX, int vertexSide, Port port, int changeTo) {
        //two cases: (A) a port corresponding to an original port before unification, (B) a dummy port for padding
        Port portBeforeUnification = sugy.getReplacedPorts().get(port); //for case (A)
        Set<Port> dummyPortsForPadding = dummyPortsForLabelPadding.get(originalVertex); //for case (B)
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
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double y = Double.NaN;
        Path firstPath = null;
        Path lastPath = null;
        for (Path path : hyperEdge.getPaths()) {
            Point2D.Double startPoint = ((PolygonalPath) path).getStartPoint();
            Point2D.Double endPoint = ((PolygonalPath) path).getEndPoint();
            if (vertexShape.contains(startPoint)) {
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
            if (vertexShape.contains(endPoint)) {
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

        if (bendsFirstPath.get(0).distance(minX, y) == 0) {
            Collections.reverse(bendsFirstPath);
        }
        if (bendsLastPath.get(0).distance(maxX, y) > 0) {
            Collections.reverse(bendsLastPath);
        }

        List<Point2D.Double> bendsCombined = bendsFirstPath;
        bendsCombined.addAll(bendsLastPath);
        hyperEdge.addPath(new PolygonalPath(bendsCombined));
        sugy.getGraph().removeVertex(hyperEdgeDummyVertex);
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
            if (portRectangle.intersectsLine(new Line2D.Double(segment.getP1(), segment.getP1()))) {
                return (Point2D.Double) segment.getP1();
            }
            if (portRectangle.intersectsLine(new Line2D.Double(segment.getP2(), segment.getP2()))) {
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
                // in DrawingPreparation
                originalEdge.removeAllPaths();
            }
        }
        //transfer the paths form the dummy to the original edge
        originalEdge.addPaths(dummyEdge.getPaths());
        //add ports of dummy edge to original edge
        for (Port port : dummyEdge.getPorts()) {
            Vertex vertex = port.getVertex();
            if (sugy.getDummyTurningNodes() != null
                    && !sugy.getDummyTurningNodes().containsKey(vertex)
                    && !sugy.getDummyNodesLongEdges().containsKey(vertex)
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
            boolean hasVisiblePorts = false;
            for (Port port : node.getPorts()) {
                if (!Double.isNaN(port.getShape().getXPosition())) {
                    maxL = Math.min(maxL, port.getShape().getXPosition());
                    minR = Math.max(minR, port.getShape().getXPosition());
                    hasVisiblePorts = true;
                }
            }
            if (hasVisiblePorts) {
                maxL -= ((drawInfo.getPortWidth() + delta) / 2);
                minR += ((drawInfo.getPortWidth() + delta) / 2);
            }
            return this;
        }
    }
}