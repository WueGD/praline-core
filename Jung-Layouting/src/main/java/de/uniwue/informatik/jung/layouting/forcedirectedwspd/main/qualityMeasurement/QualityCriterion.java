package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement;

public enum QualityCriterion {
	EDGE_LENGTH,
	DISTANCE_VERTEX_VERTEX,
	DISTANCE_VERTEX_NOT_INCIDENT_EDGE,
	/**
	 * Shortest path means here the smallest number of edges that form a path
	 * between the 2 considered vertices (Thus that is 1 for adjacent vertices
	 * and >1 for vertices in the same connected graph-component being not
	 * adjacent).
	 * <p>
	 * Example: Two vertices with distance 42.36 (in the drawing)
	 * and shortest path (regarding the number of edges) going
	 * over a third vertex would have the total value
	 * 42.36/2 = 21.18 here.
	 * <p>
	 * Values to itself (devide by 0) or to vertices of other connected
	 * components (devide by infinity) should not be considered
	 */
	RATIO_GEOMETRIC_VERTEX_DISTANCE_TO_SHORTEST_PATH,
	/**
	 * The optimal angle at a vertex v of a graph is
	 * 2Pi/deg(v).
	 * "Deviation" from that optimal angle is here definded as
	 * the difference of the considered angle
	 * and the optimal angle normalized (devided) by the optimal angle
	 * and this is squared then.
	 * It is squared to weight large deviations from the optimal angle
	 * quite strong, but small deviations hardly affect the value.
	 * Values of angles must be in radian measure.
	 */
	DEVIATION_FROM_THE_OPTIMAL_ANGLE;
}
