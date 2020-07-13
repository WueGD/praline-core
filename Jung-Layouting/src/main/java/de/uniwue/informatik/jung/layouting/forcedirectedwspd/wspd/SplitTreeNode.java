package de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Collection;

/**
 * Node of a {@link SplitTree}.
 * A {@link SplitTreeNode} stands for:
 * <ul>
 *  <li>A point set (respectively {@link VertexTriple} set, where every {@link VertexTriple} has a point as second element),
 *  that consists of the union of all point/triple sets of its children in the {@link SplitTree}.
 *  A leaf in the {@link SplitTree} corresponds thereby always to exactly one point/triple (point set size = 1).
 *  The size of the point set can be queried with {@link SplitTree#getSizeOfTheTripleSetInTheSubtreeBelow(SplitTreeNode)}.
 *  The point/triple set can be queried with {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode)} or
 *  {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode, Collection)}.
 *	</li>
 *  <li>A bounding box of this point set, can be queried with {@link SplitTree#getBoundingRectangle(SplitTreeNode)}
 *  </li>
 *  <li>A barycenter of the referenced point set. It can be queried with {@link SplitTree#getBarycenter(SplitTreeNode)}.
 *  </li>
 * </ul>
 * 
 * Warning!
 * If the {@link SplitTree} or some {@link SplitTreeNode}s or the referenced point sets (resp. referenced
 * {@link VertexTriple} sets) are changed it can lead to inconsistencies.
 * Notably if the points of the point/triple sets change it leads to wrong bounding boxes and
 * wrong barycenters.
 * To prevent this, these should be updated via {@link SplitTree#recalculateAllBoundingRectangles()}
 * and {@link SplitTree#recalculateAllBarycenters()} after a change of the point locations.
 * <br>
 * ____________
 * <br>
 * To the shift from points to {@link VertexTriple}s see {@link SplitTree} 
 */
public class SplitTreeNode<V> {
	/*
	 * Position within the split tree is defined with these 3 variables!
	 */
	protected SplitTreeNode<V> parentNode = null;
	protected SplitTreeNode<V> leftChild = null;
	protected SplitTreeNode<V> rightChild = null; 
	
	
	/**
	 * Outer rectangle R_0(u) with u being this {@link SplitTreeNode}, see book referenced in {@link SplitTree}.
	 */
	protected Rectangle r0;
	
	/**
	 * Bounding rectangle/box R(u) wit u being this {@link SplitTreeNode}, see book referenced in {@link SplitTree}.
	 * <br>
	 * Annotation: For the sake of consistence this must be the smallest rectangle that
	 * includes all points of the referenced point set and has edges parallel to
	 * the x- or y-axis.
	 */
	protected Rectangle r;
	
	/**
	 * Barycenter of the referenced point set
	 */
	protected Point2D barycenter = null;
	
	/**
	 * <ul>
	 *  <li>
	 *  If this {@link SplitTreeNode} is a leaf within its {@link SplitTree},
	 *  then this {@link SplitTreeNode} must be linked to a point of 
	 *  the point set, respectively to a {@link VertexTriple} of
	 *  the {@link VertexTriple} set where every second element is the
	 *  relevant point, via this variable.
	 *  </li>
	 *  <li>
	 *  If this {@link SplitTreeNode} is not a leaf within its {@link SplitTree},
	 *  then this variable must be null.
	 *  Then this {@link SplitTreeNode} corresponds to the {@link VertexTriple} set
	 *  saved in the leaves in the subtree rooted at this {@link SplitTreeNode}.
	 *  This set can be queried with {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode)}
	 *  or {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode, Collection)}.
	 *  </li>
	 * </ul>  
	 */
	protected VertexTriple<V> triple;
		
	/**
	 * Variable to save the number of points (resp. {@link VertexTriple}s) under this
	 * {@link SplitTreeNode} once computed.
	 * This {@link SplitTreeNode} is included.
	 * Thus 0 is an inconsistent value,
	 * 1 means this {@link SplitTreeNode} is a leaf
	 * and -1 means the value is not computed yet.
	 * If the {@link SplitTree} is modified this value should be updated
	 * by setting this value back to -1 and calling
	 * {@link SplitTree#getSizeOfTheTripleSetInTheSubtreeBelow(SplitTreeNode)}
	 * afterwards.
	 * <br>
	 * This value exists beside {@link SplitTreeNode#tripleSetInTheSubtreeBelow}
	 * because one may only need the size and not the whole set,
	 * then there is no need to save these sets in the memory.
	 */
	protected int sizeOfTheTripleSetInTheSubtreeBelow = -1;
	
	/**
	 * Analog to {@link SplitTreeNode#sizeOfTheTripleSetInTheSubtreeBelow}.
	 * But here is not the number of {@link VertexTriple}s saved, but a
	 * collection of the {@link VertexTriple}s below.
	 * It is null if not computed yet.
	 * For consistence this must be true if both values are not
	 * undefined any more:
	 * {@link SplitTreeNode#tripleSetInTheSubtreeBelow}.getSize() ==
	 * {@link SplitTreeNode#sizeOfTheTripleSetInTheSubtreeBelow}.
	 * See also {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode)}
	 * and {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode, Collection)}.
	 */
	protected Collection<VertexTriple<V>> tripleSetInTheSubtreeBelow;
	
	
	/**
	 * Repulsive force that acts on this point set (respectively {@link VertexTriple} set).
	 * This force is computed as repulsive force between two {@link SplitTreeNode}s that
	 * form a well-separated pair in a {@link WellSeparatedPairDecomposition}.
	 * Is used in graph-drawing algorithms that use a {@link WellSeparatedPairDecomposition}.
	 * (See {@link FRWSPD}).
	 */
	public Point2D repulsiveForce = new Point2D.Double(0, 0);
	
	
	/**
	 * This variable is only needed to have a reference from a {@link SplitTreeNode} to its lists
	 * in the {@link SplitTree}-construction-algorithm.
	 * After construction of the {@link SplitTree} this variable is not needed any more.
	 */
	protected LinkedList<SplitTreeListElement<V>>[] lists;
	
	public SplitTreeNode(Rectangle r0) {
		this.r0 = r0;
	}
	
	public SplitTreeNode(Rectangle r0, Rectangle r) {
		this.r0 = r0;
		this.r = r;
	}
	
	public SplitTreeNode(Rectangle r0, Rectangle r, VertexTriple<V> triple) {
		this.r0 = r0;
		this.r = r;
		this.triple = triple;
	}
	
	public VertexTriple<V> getTriple(){
		return triple;
	}
	
	@Override
	public String toString() {
		if(triple!=null && triple.get1()!=null && triple.get2()!=null){
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(2);
			return triple.get1().toString()+"@("+nf.format(triple.get2().getX())+"/"+nf.format(triple.get2().getY())+")";
		}
		if(this.leftChild!=null || this.rightChild!=null){
			return "Inner_Node";
		}
		return super.toString();
	}
}