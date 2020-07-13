package de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;

/**
 * QuadTree-Analogon to {@link SplitTreeNode}
 */
public class QuadTreeNode<V> {
	/**
	 * Square that is referenced by this {@link QuadTreeNode}.
	 * (Area to this {@link QuadTreeNode})
	 */
	protected Square square;
	
	/**
	 * barycenter of the referenced point set (resp. {@link VertexTriple} set, where each 2nd element is a point)
	 * in the {@link Square} represented by this {@link QuadTreeNode}.
	 */
	protected Point2D barycenter = new Point2D.Double(0, 0);
	
	/**
	 * This is a list of points (represented via its {@link VertexTriple})
	 * that are inside the {@link Square} of this {@link QuadTree}.
	 * Size can queried with tripleSetBelow.size().
	 * It should checked if this variable is still null then it has to be set correctly!
	 * Should be updated if the {@link QuadTree} is modified to avoid inconsistences.
	 */
	protected Collection<VertexTriple<V>> tripleSetBelow = new LinkedList<VertexTriple<V>>();
	
	private int numberOfChildren = 0;
	/**
	 * child top-left
	 */
	private QuadTreeNode<V> childTL = null;
	/**
	 * child top-right
	 */
	private QuadTreeNode<V> childTR = null;
	/**
	 * child bottom-left
	 */
	private QuadTreeNode<V> childBL = null;
	/**
	 * child bottom-right
	 */
	private QuadTreeNode<V> childBR = null;
	
	public void setChildTL(QuadTreeNode<V> qtn){
		if(this.childTL!=null){
			numberOfChildren--;
		}
		this.childTL = qtn;
		if(this.childTL!=null){
			numberOfChildren++;
		}
	}
	public void setChildTR(QuadTreeNode<V> qtn){
		if(this.childTR!=null){
			numberOfChildren--;
		}
		this.childTR = qtn;
		if(this.childTR!=null){
			numberOfChildren++;
		}
	}
	public void setChildBL(QuadTreeNode<V> qtn){
		if(this.childBL!=null){
			numberOfChildren--;
		}
		this.childBL = qtn;
		if(this.childBL!=null){
			numberOfChildren++;
		}
	}
	public void setChildBR(QuadTreeNode<V> qtn){
		if(this.childBR!=null){
			numberOfChildren--;
		}
		this.childBR = qtn;
		if(this.childBR!=null){
			numberOfChildren++;
		}
	}
	
	
	
	public QuadTreeNode<V> getChildTL() {
		return childTL;
	}
	public QuadTreeNode<V> getChildTR() {
		return childTR;
	}
	public QuadTreeNode<V> getChildBL() {
		return childBL;
	}
	public QuadTreeNode<V> getChildBR() {
		return childBR;
	}
	
	
	
	public int getNumberOfChildren() {
		return numberOfChildren;
	}
	
	
	
	public QuadTreeNode(Square areaToThisQuadTreeNode) {
		this.square = areaToThisQuadTreeNode;
	}
}
