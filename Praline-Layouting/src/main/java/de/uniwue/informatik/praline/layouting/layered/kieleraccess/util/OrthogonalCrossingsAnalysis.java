package de.uniwue.informatik.praline.layouting.layered.kieleraccess.util;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.impl.ElkBendPointImpl;
import org.eclipse.emf.common.util.EList;

import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import java.util.*;

public class OrthogonalCrossingsAnalysis {
    public static final String ID = "de.cau.cs.kieler.kiml.grana.orthogonalCrossings";

    public OrthogonalCrossingsAnalysis() {
    }

    public Object doAnalysis(ElkNode parentNode, IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Orthogonal crossings analysis", 1.0F);
        int crossings = this.countCrossings(parentNode);
        progressMonitor.done();
        return new Object[]{crossings};
    }

    public int countCrossings(ElkNode parentNode) {
        List<ElkNode> nodes = Lists.newArrayList(new ElkNode[]{parentNode});
        boolean shouldAnalyzeHierarchy = true; //(Boolean)((KShapeLayout)parentNode.getData(KShapeLayout.class))
        // .getProperty(AnalysisOptions.ANALYZE_HIERARCHY);
        int crossings = 0;

        while(!nodes.isEmpty()) {
            ElkNode node = (ElkNode)nodes.remove(0);
            crossings += (new OrthogonalCrossingsAnalysis.SingleHierarchyLevelOrthogonalCrossCounter(node)).count();
            if (shouldAnalyzeHierarchy) {
                nodes.addAll(node.getChildren());
            }
        }

        return crossings;
    }

    private static class SingleHierarchyLevelOrthogonalCrossCounter {
        private static final double EPSILON = 1.0E-4D;
        private final Set<KVector> junctionPoints;
        private final List<Line2D> segments;
        private final ElkNode parentNode;

        public SingleHierarchyLevelOrthogonalCrossCounter(ElkNode n) {
            this.parentNode = n;
            List<ElkNode> nodes = new ArrayList(this.parentNode.getChildren());
            this.segments = Lists.newArrayList();
            this.junctionPoints = Sets.newHashSet();
            Iterator var4 = nodes.iterator();

            while(var4.hasNext()) {
                ElkNode node = (ElkNode)var4.next();
                this.collectSegmentsOfEdgesConnectedTo(node);
                this.addJunctionPointsOnEdgesConnectedTo(node);
            }

            this.mergeOverlappingSegments();
        }

        private void collectSegmentsOfEdgesConnectedTo(ElkNode node) {
            for (ElkPort port : node.getPorts()) {
                Iterator var3 = port.getIncomingEdges().iterator();

                ElkEdge edge;
                while (var3.hasNext()) {
                    edge = (ElkEdge) var3.next();
//                    if (this.notFromChildOf(edge, node)) {
                        this.addSegmentsOf(edge);
//                    }
                }

                var3 = node.getOutgoingEdges().iterator();

                while (var3.hasNext()) {
                    edge = (ElkEdge) var3.next();
                    if (this.edgeGoesToParentOutPort(edge)) {
                        this.addSegmentsOf(edge);
                    }
                }
            }

        }

        private boolean edgeGoesToParentOutPort(ElkEdge e) {
            return e.getTargets().contains(this.parentNode);
        }

        private boolean notFromChildOf(ElkEdge e, ElkNode n) {
            //TOOD: check this method -- it might be fixed wrongly
            for (ElkConnectableShape source : e.getSources()) {
                if (n.getChildren().contains(source)) {
                    return true;
                }
            }
            return false;
        }

        private void addSegmentsOf(ElkEdge edge) {
            ElkEdgeSection edgeSection = edge.getSections().get(0);
            EList<ElkBendPoint> bendPoints = edgeSection.getBendPoints();
            ElkBendPoint startPoint = new ElkBendPointImpl(){};
            startPoint.set(edgeSection.getStartX(), edgeSection.getStartY());
            ElkBendPoint endPoint = new ElkBendPointImpl(){};
            endPoint.set(edgeSection.getEndX(), edgeSection.getEndY());
            bendPoints.add(0, startPoint);
            bendPoints.add(endPoint);

            Iterator<ElkBendPoint> iterator = bendPoints.iterator();

            ElkBendPoint end;
            for(ElkBendPoint start = iterator.next(); iterator.hasNext(); start = end) {
                end = iterator.next();
                Line2D line = new Double(start.getX(), start.getY(), end.getX(), end.getY());
                this.segments.add(line);
            }

        }

        private void addJunctionPointsOnEdgesConnectedTo(ElkNode node) {
            for (ElkPort port : node.getPorts()) {
                Iterator var3 = port.getIncomingEdges().iterator();

                ElkEdge edge;
                while(var3.hasNext()) {
                    edge = (ElkEdge)var3.next();
                    this.junctionPoints.addAll(edge.getProperty(CoreOptions.JUNCTION_POINTS));
                }

                var3 = node.getOutgoingEdges().iterator();

                while(var3.hasNext()) {
                    edge = (ElkEdge)var3.next();
                    this.junctionPoints.addAll(edge.getProperty(CoreOptions.JUNCTION_POINTS));
                }
            }

        }

        private void mergeOverlappingSegments() {
            boolean hasMerged;
            do {
                hasMerged = false;

                for(int i = 0; i < this.segments.size(); ++i) {
                    for(int j = i + 1; j < this.segments.size(); ++j) {
                        Line2D lineOne = (Line2D)this.segments.get(i);
                        Line2D lineTwo = (Line2D)this.segments.get(j);
                        if (this.canBeMerged(lineOne, lineTwo)) {
                            this.merge(lineOne, lineTwo);
                            this.segments.remove(j);
                            hasMerged = true;
                        }
                    }
                }
            } while(hasMerged);

        }

        private void merge(Line2D lineOne, Line2D lineTwo) {
            double x1 = 0.0D;
            double x2 = 0.0D;
            double y1 = 0.0D;
            double y2 = 0.0D;
            java.lang.Double[] xCoords;
            if (this.isHorizontal(lineOne)) {
                xCoords = new java.lang.Double[]{lineOne.getX1(), lineOne.getX2(), lineTwo.getX1(), lineTwo.getX2()};
                Arrays.sort(xCoords);
                x1 = xCoords[0];
                x2 = xCoords[xCoords.length - 1];
                y1 = lineOne.getY1();
                y2 = lineOne.getY1();
            } else {
                xCoords = new java.lang.Double[]{lineOne.getY1(), lineOne.getY2(), lineTwo.getY1(), lineTwo.getY2()};
                Arrays.sort(xCoords);
                y1 = xCoords[0];
                y2 = xCoords[xCoords.length - 1];
                x1 = lineOne.getX1();
                x2 = lineOne.getX1();
            }

            lineOne.setLine(x1, y1, x2, y2);
        }

        public int count() {
            int crossings = 0;

            for(int i = 0; i < this.segments.size(); ++i) {
                Line2D firstLine = (Line2D)this.segments.get(i);

                for(int j = i + 1; j < this.segments.size(); ++j) {
                    Line2D secondLine = (Line2D)this.segments.get(j);
                    if (this.crossingIsVisible(firstLine, secondLine)) {
                        ++crossings;
                    }
                }
            }

            return crossings;
        }

        private boolean crossingIsVisible(Line2D firstLine, Line2D secondLine) {
            return this.areNotParallel(firstLine, secondLine) && this.doNotOnlyTouchAtEnds(firstLine, secondLine) && this.intersect(firstLine, secondLine) && this.intersectionHasNoJunctionPoint(firstLine, secondLine);
        }

        private boolean areNotParallel(Line2D firstLine, Line2D secondLine) {
            return this.isVertical(firstLine) ^ this.isVertical(secondLine);
        }

        private boolean doNotOnlyTouchAtEnds(Line2D firstLine, Line2D secondLine) {
            Line2D verticalLine = this.isVertical(firstLine) ? firstLine : secondLine;
            Line2D horizontalLine = this.isVertical(firstLine) ? secondLine : firstLine;
            return !this.doubleEquals(horizontalLine.getX1(), verticalLine.getX1()) && !this.doubleEquals(horizontalLine.getX2(), verticalLine.getX1()) && !this.doubleEquals(horizontalLine.getY1(), verticalLine.getY1()) && !this.doubleEquals(horizontalLine.getY1(), verticalLine.getY2());
        }

        private boolean intersect(Line2D firstLine, Line2D secondLine) {
            Line2D verticalLine = this.isVertical(firstLine) ? firstLine : secondLine;
            Line2D horizontalLine = this.isVertical(firstLine) ? secondLine : firstLine;
            double xValue = verticalLine.getX1();
            double yValue = horizontalLine.getY1();
            boolean xIntersects = Math.min(horizontalLine.getX1(), horizontalLine.getX2()) < xValue && Math.max(horizontalLine.getX1(), horizontalLine.getX2()) > xValue;
            boolean yInstersects = Math.min(verticalLine.getY1(), verticalLine.getY2()) < yValue && Math.max(verticalLine.getY1(), verticalLine.getY2()) > yValue;
            return xIntersects && yInstersects;
        }

        private boolean intersectionHasNoJunctionPoint(Line2D firstLine, Line2D secondLine) {
            KVector intersection = this.intersectionOf(firstLine, secondLine);
            return !this.junctionPoints.contains(intersection);
        }

        private KVector intersectionOf(Line2D firstLine, Line2D secondLine) {
            Line2D verticalLine = this.isVertical(firstLine) ? firstLine : secondLine;
            Line2D horizontalLine = this.isHorizontal(firstLine) ? firstLine : secondLine;
            return new KVector(verticalLine.getX1(), horizontalLine.getY1());
        }

        private boolean isVertical(Line2D line) {
            return this.doubleEquals(line.getX1(), line.getX2());
        }

        private boolean isHorizontal(Line2D line) {
            return this.doubleEquals(line.getY1(), line.getY2());
        }

        private boolean doubleEquals(double y1, double y2) {
            return Math.abs(y1 - y2) < 1.0E-4D;
        }

        private boolean canBeMerged(Line2D line1, Line2D line2) {
            return line1.intersectsLine(line2) && !this.areNotParallel(line1, line2);
        }
    }
}
