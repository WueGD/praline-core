package de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.geom.Point2D;
import java.util.Comparator;

public class VertexTripleComparator<V> implements Comparator<VertexTriple<V>>{
	
	boolean compareX;
	
	/**
	 * Comparator that compares two {@link VertexTriple}s either by the x- or
	 * by the y-coordinate of the location ({@link Point2D}) of the vertex (2nd element of the {@link VertexTriple}).
	 * 
	 * After what it should compare (x or y) has to be
	 * determined in the constructor.
	 * 
	 * @param compareX
	 * If true, then the x-coordinates are comperad, otherwise the y-coordinates are compared
	 */
	public VertexTripleComparator(boolean compareX) {
		this.compareX = compareX;
	}
	
	
	/**
	 * Returns true if it compares after the x-coordinate
	 * and returns false if it compares after the y-coordinate.
	 * @return
	 */
	public boolean isCompareX() {
		return compareX;
	}
	
	/**
	 * @param compareX
	 * true: compare after x-coordinate, false -> compare after y-coordinate
	 */
	public void setCompareX(boolean compareX) {
		this.compareX = compareX;
	}



	@Override
	public int compare(VertexTriple<V> o1, VertexTriple<V> o2) {
		//x
		if(compareX){
			return Double.compare(o1.get2().getX(), o2.get2().getX());
		}
		//else: y
		return Double.compare(o1.get2().getY(), o2.get2().getY());
	}

}
