package de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Simple implementation of a rectangle in 2 dimensions.
 * The sides of the rectangle are parallel to the x-/y-axis.
 * This class was created primarily for the class {@link SplitTree}
 */
public class Rectangle extends Rectangle2D.Double{
	
	/**
	 * Constructor, in which the size of the rectangle has to be defined by instantiation
	 * 
	 * @param x
	 * Lowest x-value of the rectangle
	 * @param y
	 * Lowest y-value of the rectangle
	 * @param width
	 * @param height
	 */
	public Rectangle(double x, double y, double width, double height) {
		super(x,y,width,height);
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public void setHeight(double height) {
		this.height = height;
	}
	
	public Point2D getCenter(){
		return new Point2D.Double(x+width/2, y+height/2);
	}
	
	public double getLengthOfTheDiagonal(){
		return Math.sqrt(width*width+height*height);
	}
	
	/**
	 * Returns width (dimension=0) or height (dimension=1)
	 * 
	 * So e.g. {@link Rectangle#getWidthOrHeight(1)} is
	 * equivalent to  {@link Rectangle#getHeight()}.
	 * 
	 * Returns -1 for invalid input value of dimension
	 * 
	 * @param dimension
	 */
	public double getWidthOrHeight(int dimension){
		if(dimension==0){
			return width;
		}
		if(dimension==1){
			return height;
		}
		return -1;
	}
	
	/**
	 * Returns width if width>heigt and returns height else
	 * 
	 * @return
	 */
	public double getLengthOfTheLongerSide(){
		if(width>height){
			return width;
		}
		return height;
	}
	
	@Override
	public String toString() {
		return "Rectangle [x="+x+", y="+y+", width="+width+", height="+height+"]";
	}
}
