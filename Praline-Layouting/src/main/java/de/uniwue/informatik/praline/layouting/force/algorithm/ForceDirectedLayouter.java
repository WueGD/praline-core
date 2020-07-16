package de.uniwue.informatik.praline.layouting.force.algorithm;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;
import de.uniwue.informatik.praline.layouting.PralineLayouter;

public class ForceDirectedLayouter implements PralineLayouter {

    private Graph graph;
    private DrawingInformation drawInfo;

    public ForceDirectedLayouter(Graph graph, DrawingInformation drawInfo) {
        this.graph = graph;
        this.drawInfo = drawInfo;
    }

    @Override
    public void computeLayout() {
        //TODO
    }

    @Override
    public Graph getGraph() {
        return this.graph;
    }

    @Override
    public DrawingInformation getDrawingInformation() {
        return this.drawInfo;
    }

    @Override
    public void setDrawingInformation(DrawingInformation drawInfo) {
        this.drawInfo = drawInfo;
    }
}
