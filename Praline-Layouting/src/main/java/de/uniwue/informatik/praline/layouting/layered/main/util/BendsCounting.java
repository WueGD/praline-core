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
            //count all inner bend points
            int innerBendPoints = 0;
            //also count start end points of different paths
            int startAndEndPointsOfPaths = 0;
            if (paths != null) {
                for (Path path : paths) {
                    if (path instanceof PolygonalPath) {
                        innerBendPoints += ((PolygonalPath) path).getBendPoints().size();
                    }
                    startAndEndPointsOfPaths += 2;
                }
            }
            //add all inner bends points
            sum += innerBendPoints;
            //for the different paths, we do not count the end points at the ports
            startAndEndPointsOfPaths -= edge.getPorts().size();
            //for the remaining connection points of different paths we count 1,
            //TODO: maybe this is too pessimistic: if two paths end in the same point, they contribute 1 or 0 bends
            // instead of 2
            sum += startAndEndPointsOfPaths;
            //TODO: counting for hyperedges is not yet ideal: currently we count for every vertical segment arriving at
            // the horizontal central segment +1. It's not really clear if it makes sense to count this way. In some
            // cases it feels like this is to pessimistic, in particular if there are two vertical segments arriving
            // at the same point (one coming from above and one from below)
        }
        return sum;
    }
}
