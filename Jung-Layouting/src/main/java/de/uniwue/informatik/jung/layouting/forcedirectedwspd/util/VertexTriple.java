package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout.FRVertexData;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Map;

/**
 * Object that is used as vertex in one drawing of one graph.
 * It is an {@link Triple} that consists of the 3 elements (in this order):
 * <ol>
 *  <li>
 *   Vertex data of variable type (specified with generic V).
 *   This is used in JUNG-{@link Graph}s to specify a vertex.
 *  </li>
 *  <li>
 *   A {@link Point2D}-object that is the location of the vertex within the drawing.
 *  </li>
 *  <li>
 *   A {@link FRVertexData}-object (this inherits from {@link Point2D}).
 *   It is here the computed displacement in one iteration of some graph-drawing-algorithms.
 *   It is used by the algorithm in the class {@link FRLayoutNoMaps} and the algorithms in the classes that have
 *   {@link FRLayoutNoMaps} as super-class.
 *   This value has no application or meaning after or outside the calculation of the graph drawing.
 *  </li>
 * </ol>
 * 
 * <p>
 * In the graph-drawing-classes ({@link Layout}-classes) from the JUNG-library there are no {@link VertexTriple}s used.
 * There the locations of the vertices are stored in (Hash-){@link Map}s.
 * Given one vertex (as an object of some vertex data V) of a {@link Graph}, a {@link Layout} ({@link AbstractLayout})
 * of this graph (there may be many layouts to that graph) returns a location (point-object) by using that {@link Map}.
 * In practice that is a little slower (but only constant time, no asymptotically higher time in practice) than having references
 * to the vertex data and to the location of the vertex
 * in one layout saved together in one object, so this class was created to replace the Maps.
 * Force-directed graph-drawing algorithms implemented in JUNG such as {@link FRLayout} or {@link FRLayout2}
 * calculate a displacement of each vertex in each iteration (displacement induced by many single
 * attractive and repulsive forces). To get this displacement-vector/point for every vertex there is also a {@link Map} used.
 * To speed up the accesses to these values in practice also a little a reference to that calculation-intern value is saved
 * directly in the new class {@link VertexTriple}, too.
 * The algorithms in the classes {@link FRLayoutNoMaps} and classes that have it as super-class (most notably
 * {@link FRWSPD}, {@link FRQuadtree}) use a {@link Collection} of those {@link VertexTriple}s
 * as data structure. This is a little faster when iterated over all elements of the {@link Collection}.
 * If it is used in contexts where queries (give vertex data, get location in the layout) of arbitrary, single vertex-objects
 * (type vertex data V) should be answered, a hashmap from the vertex data to the corresponding {@link VertexTriple}s
 * should be added or the older structure (map: vertex data -> point) should be used instead.
 * <p>
 * The class {@link EdgeTriple} was created for the same reason/use analog to {@link VertexTriple}.
 */
public class VertexTriple<V> extends Triple<V, Point2D, FRVertexData> {
	
	/**
	 * Creates vertex for the drawing with initial location (0,0)
	 * 
	 * @param vertexData
	 */
	public VertexTriple(V vertexData){
		super(vertexData, new Point2D.Double(0, 0), new FRVertexData());
	}
	
	public VertexTriple(V vertexData, Point2D location){
		super(vertexData, location, new FRVertexData());
	}
	
	/**
	 * 
	 * @param vertexData
	 * @param location
	 * @param displacementInOneIterationOfTheComputation
	 * 			If this value is unknow call {@link VertexTriple#VertexTriple(Object, Point2D)} instead
	 * 			as this is only used for intern compuatation in some graph drawing algorithms
	 */
	public VertexTriple(V vertexData, Point2D location, FRVertexData displacementInOneIterationOfTheComputation) {
		super(vertexData, location, displacementInOneIterationOfTheComputation);
	}
	
	/**
	 * Returns the saved Vertex data.
	 * Equivalent to .get1() (as vertex data is the 1st value saved in this triple)
	 * 
	 * @return
	 */
	public V getVertexData(){
		return super.get1();
	}
	
	/**
	 * Returns the location of this vertex in the drawing.
	 * Equivalent to .get2() (as point location is the 2nd value saved in this triple)
	 * 
	 * @return
	 */
	public Point2D getLocation(){
		return super.get2();
	}
	
	/**
	 * Retruns the computed displacement in one iteration (is {@link FRVertexData}, this inherits from {@link Point2D}).
	 * This value is used only by some graph-drawing-algorithms.
	 * This is the algorithm in the class {@link FRLayoutNoMaps} and the algorithms in the classes that have
	 * {@link FRLayoutNoMaps} as super-class.
	 * Equivalent to .get3() (as computation-intern displacementvector is the 3rd value saved in this triple)
	 * 
	 * @return
	 */
	public FRVertexData getDisplacementInOneIterationOfTheComputation(){
		return super.get3();
	}
}
