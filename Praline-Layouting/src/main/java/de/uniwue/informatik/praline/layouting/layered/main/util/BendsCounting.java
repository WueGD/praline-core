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
        }
        return sum;
    }
}
