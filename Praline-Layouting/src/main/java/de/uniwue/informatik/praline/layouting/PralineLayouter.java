package de.uniwue.informatik.praline.layouting;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.io.output.util.DrawingInformation;

public interface PralineLayouter {

    void computeLayout();

    Graph getGraph();

    DrawingInformation getDrawingInformation();

    void setDrawingInformation(DrawingInformation drawInfo);
}
