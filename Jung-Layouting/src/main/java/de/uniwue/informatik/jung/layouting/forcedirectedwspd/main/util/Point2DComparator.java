package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import java.awt.geom.Point2D;
import java.util.Comparator;

/**
 * Does compare acc. to x or y coordinate.
 * Note: If x- or y-coordinates are the same
 * the other coordinate is not taken as a second
 * comparison criterion.
 */
public class Point2DComparator {
	
	public class X implements Comparator<Point2D>{
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return Double.compare(o1.getX(), o2.getX());
		}
	}
	

	public class Y implements Comparator<Point2D>{
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return Double.compare(o1.getY(), o2.getY());
		}
	}
}
