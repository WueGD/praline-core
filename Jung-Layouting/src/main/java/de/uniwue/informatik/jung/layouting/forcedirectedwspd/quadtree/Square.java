package de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree;

import java.awt.geom.Point2D;

/**
 * Simple Implementation of a square in the plane.
 * The sides of the square are parallel to the x-/y-axis.
 * The {@link QuadTree}-algorithm is based in a great extent on squares,
 * therefore this class was created.
 */
public class Square {
	private double x;
	private double y;
	private double sideLength;
	
	/**
	 * Constructor in which the extent of the square has to be defined.
	 * 
	 * @param x
	 * Lowest x-value
	 * @param y
	 * Lowest y-value
	 * @param sideLength
	 * side length of all 4 sides of the square (the corner with the lowest x- and lowest y-coordinate
	 * is at the point (x/y) (the two input-parameters before this))
	 */
	public Square(double x, double y, double sideLength) {
		this.x = x;
		this.y = y;
		this.sideLength = sideLength;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	
	public double getSideLength() {
		return sideLength;
	}

	public void setSideLength(double sideLength) {
		this.sideLength = sideLength;
	}
	
	public Point2D getCenter(){
		return new Point2D.Double(x+sideLength/2, y+sideLength/2);
	}
	
	public double getLengthOfTheDiagonal(){
		return Math.sqrt(2)*sideLength;
	}
	
	/**
	 * Returns if a point lies on this square (incl. boandary)
	 * 
	 * @param point
	 * @return
	 */
	public boolean contains(Point2D point){
		if((point.getX()>=x && point.getX()<=x+sideLength)
				&& (point.getY()>=y && point.getY()<=y+sideLength)){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Square [x="+x+", y="+y+", sideLength="+sideLength+"]";
	}
}