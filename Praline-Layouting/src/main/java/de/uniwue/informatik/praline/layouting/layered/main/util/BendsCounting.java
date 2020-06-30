package de.uniwue.informatik.praline.layouting.layered.main.util;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.paths.Path;
import de.uniwue.informatik.praline.datastructure.paths.PolygonalPath;

import java.util.List;

public class BendsCounting {

    public static int countNumberOfBends(Graph graph) {
        int sum = 0;
        for (Edge edge : graph.getEdges()) {
            List<Path> paths = edge.getPaths();
            if (paths != null) {
                for (Path path : paths) {
                    if (path instanceof PolygonalPath) {
                        sum += ((PolygonalPath) path).getBendPoints().size();
                    }
                }
            }
        }
        return sum;
    }
}
