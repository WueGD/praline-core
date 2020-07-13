package de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;

/**
 * Well-Separated Pair Decomposition for a point set P,
 * with respect to a parameter s as it may
 * be found in literature.
 * <br>
 * ____________
 * <br>
 * To the shift from points to {@link VertexTriple}s see {@link SplitTree}
 */
public class WellSeparatedPairDecomposition<V> {
	/**
	 * Here all well-separated pairs that are found are saved.
	 * Every entry in this list must be a pair
	 * that means every array in this list as a length of 2
	 * (2 {@link SplitTreeNode}s).
	 * Both of the two parts of the pair can represent
	 * arbitrarily many points (respectively {@link VertexTriple}s)
	 * that are represented by one {@link SplitTreeNode}.
	 */
	private LinkedList<SplitTreeNode<V>[]> wellSeparatedPairs;
	
	public LinkedList<SplitTreeNode<V>[]> getWellSeparatedPairs(){
		return wellSeparatedPairs;
	}
	
	/**
	 * {@link SplitTree} from which this {@link WellSeparatedPairDecomposition} is built of
	 */
	private SplitTree<V> t;
	
	/**
	 * s-value on which this {@link WellSeparatedPairDecomposition} is based
	 * (all pairs are well-separated with respect to s)
	 */
	private double s;
	
	/**
	 * Computes for the passed {@link SplitTree} t and the passed minimum-distance-factor s
	 * a {@link WellSeparatedPairDecomposition} (WSPD).
	 * This is done with the algorithm "ComputeWSPD(SplitTree T, realNumer(>0) s)"
	 * as it is described in the book "Geometric Spanner Networks"
	 * from G. Narasimhan and Michiel Smid from the year 2007.
	 * That algorithm needs O(s^2*n) time where n is the size of the point set.
	 * Thus it is in O(n) time if s is assumed a constant value.
	 * 
	 * @param t
	 * @param s
	 */
	public WellSeparatedPairDecomposition(SplitTree<V> t, double s){
		this.t = t;
		this.s = s;
		this.wellSeparatedPairs = new LinkedList<SplitTreeNode<V>[]>();
		computeWSPD();
	}
	
	
	/**
	 * This is the method computeWSPD(T, s) from the book.
	 * Here there are no input parameters T and s expected because
	 * they have to be set as instance variables before calling
	 * this method.
	 * The implementation was changed a little.
	 * In the book the algorithm works with such a for-loop:
	 * <p>
	 * for each internal node u of T do
	 * <p>
	 * [...]
	 * <p>
	 * endfor
	 * <p>
	 * Instead of using that for-loop it is realized here with
	 * a recursive method.
	 * {@link WellSeparatedPairDecomposition#computeWSPDForChildrenOfVertexU(SplitTreeNode)}
	 * is called for the root of t in this method.
	 * There the same method is called for both children if a
	 * inner {@link SplitTreeNode} (inner in sense of "not a leaf") has been passed.
	 * <p>
	 * This method is called by the constructor. 
	 */
	private void computeWSPD(){
		//catch special case: no points in the split tree
		if(t.getRoot().triple==null && t.isLeaf(t.getRoot())){
			return;
		}
		//expected case else
		computeWSPDForChildrenOfVertexU(t.getRoot());
	}
	
	/**
	 * This method realizes the algorithm {@link WellSeparatedPairDecomposition#computeWSPD()}
	 * for every (inner) node recursively. For more information see there.
	 * 
	 * @param u
	 */
	private void computeWSPDForChildrenOfVertexU(SplitTreeNode<V> u){
		//Only inner nodes (in sense of "not a leaf") are relevant!
		//In the split tree this is true: v is leaf <=> v has one referenced point
		//thus this if-check is sufficient to check if it is an inner node
		if(u.triple==null){
			Collection<SplitTreeNode<V>> children = t.getChildren(u);
			//1. call FindPairs
			Iterator<SplitTreeNode<V>> i = children.iterator();
			findPairs(i.next(), i.next());
			//2. Call the same alg for both kids
			i = children.iterator();
			computeWSPDForChildrenOfVertexU(i.next()); //first child
			computeWSPDForChildrenOfVertexU(i.next()); //second child
		}
	}
	
	/**
	 * Algorithm from the book (see {@link WellSeparatedPairDecomposition}).
	 * Found pairs are saved in the list
	 * {@link WellSeparatedPairDecomposition#wellSeparatedPairs}.
	 * 
	 * @param v
	 * @param w
	 */
	private void findPairs(SplitTreeNode<V> v, SplitTreeNode<V> w) {
		if(isWellSeparated(v, w)){
			SplitTreeNode[] newWellSeparatedPair = {v,w};
			wellSeparatedPairs.add(newWellSeparatedPair);
		}
		else{
			if(v.r.getLengthOfTheLongerSide() <= w.r.getLengthOfTheLongerSide()){
				Collection<SplitTreeNode<V>> childOfW = t.getChildren(w);
				Iterator<SplitTreeNode<V>> i = childOfW.iterator();
				findPairs(v, i.next()); //findPairs(v, leftChild(w))
				findPairs(v, i.next()); //findPairs(v, rightChild(w))				
			}
			else{
				Collection<SplitTreeNode<V>> childOfV = t.getChildren(v);
				Iterator<SplitTreeNode<V>> i = childOfV.iterator();
				findPairs(i.next(), w); //findPairs(leftChild(v), w)
				findPairs(i.next(), w); //findPairs(rightChild(v), w)	
			}
		}
	}

	/**
	 * Checks if the two point sets, each represented by one {@link SplitTreeNode},
	 * are well-separated with respect to s.
	 * (If true is returned both point sets are well-separated with respect to s)
	 * <p>
	 * How the method works:
	 * The property, that for every {@link SplitTreeNode} u
	 * the bounding rectangle R(u) is stored, is used here.
	 * It is assumed that this rectangle is contained completely in a
	 * ball (in 2d circle) with corner points lying on the boundary of
	 * the circle.
	 * 
	 * The center of the rectangle can be determined easily.
	 * This is also the center of the circle.
	 * Radius of the circle is distance from the center to
	 * one of the corner-points of R(u), this is half of
	 * the diagonal of R(u).
	 * <br>
	 * This method is taken from the book.
	 * 
	 * @param v
	 * @param w
	 * @return
	 */
	public boolean isWellSeparated(SplitTreeNode<V> v, SplitTreeNode<V> w) {
		return isWellSeparated(v, w, this.s);
	}
	
	/**
	 * See {@link WellSeparatedPairDecomposition#isWellSeparated(SplitTreeNode, SplitTreeNode)}.
	 * Only difference: Here some value for the parameter s can be passed, there the s from instance
	 * variable s is taken.
	 * 
	 * @param v
	 * @param w
	 * @param s
	 * @return
	 */
	public static boolean isWellSeparated(SplitTreeNode<?> v, SplitTreeNode<?> w, double s) {
		Point2D centerV = v.r.getCenter();
		Point2D centerW = w.r.getCenter();
		//Radius of the bigger circle is the relevant radius
		double radius = Math.max(v.r.getLengthOfTheDiagonal()/2, w.r.getLengthOfTheDiagonal()/2);
		//That it is s-well-separated, the distance of the centers must be >= (s+2)*radius.
		//The +2 is because the distance from the centers to the boundaries must be added for each circle
		if(Point2D.distance(centerV.getX(), centerV.getY(), centerW.getX(), centerW.getY())>=(s+2)*radius){
			return true;
		}
		return false;
	}
}
