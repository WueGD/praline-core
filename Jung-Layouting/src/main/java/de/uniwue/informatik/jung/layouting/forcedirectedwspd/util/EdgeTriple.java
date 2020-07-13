package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util;

import edu.uci.ics.jung.graph.Graph;

/**
 * Object that is used as edge in one drawing of one graph.
 * It is an {@link Triple} that consists of the 3 elements (in this order):
 * <ol>
 *  <li>
 *   Edge data of variable type (specified with generic E).
 *   This is used in JUNG-{@link Graph}s to specify a edge.
 *  </li>
 *  <li>
 *   The first vertex (better said: one of the two vertices) connected by this edge.
 *   It is represented by a {@link VertexTriple}.
 *  </li>
 *  <li>
 *   The second vertex (better said: the other of the two vertices) connected by this edge.
 *   It is represented by a {@link VertexTriple}.
 *  </li>
 * </ol>
 * <br>
 * Note:
 * As the vertices are represented by {@link VertexTriple}s these {@link EdgeTriple}s are only a valid edges for
 * one drawing of one graph, not for that graph in general (for every drawing).
 * This is because a {@link VertexTriple} contains always also information
 * about the location of that vertex in that drawing.
 * 
 * <p>
 * For the reason why the {@link EdgeTriple}-class was created see {@link VertexTriple}.
 */
public class EdgeTriple<V, E> extends Triple<E, VertexTriple<V>, VertexTriple<V>> {
	
	public EdgeTriple(E edgeData, VertexTriple<V> vertexA, VertexTriple<V> vertexB) {
		super(edgeData, vertexA, vertexB);
	}
	
	/**
	 * Returns the saved Edge data.
	 * Equivalent to .get1() (as edge data is the 1st value saved in this triple)
	 * 
	 * @return
	 */
	public E getEdgeData(){
		return super.get1();
	}
	
	/**
	 * Returns the first of the two vertices (represented by an {@link VertexTriple}-object)
	 * connected by this edge.
	 * Equivalent to .get2() (as the first vertex of this edge is the 2nd value saved in this triple)
	 * 
	 * @return
	 */
	public VertexTriple<V> getVertexA(){
		return super.get2();
	}
	
	/**
	 * Returns the second of the two vertices (represented by an {@link VertexTriple}-object)
	 * connected by this edge.
	 * Equivalent to .get3() (as the second vertex of this edge is the 3rd value saved in this triple)
	 * 
	 * @return
	 */
	public VertexTriple<V> getVertexB(){
		return super.get3();
	}
	
}
