package de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.LinkedList.Node;

import java.util.LinkedList;

/**
 * For the algorithm FastSplitTree(S, R) and PartialSplitTree(S, R, (LS)[d]),
 * with d=2 in the plane, there are lists needed,
 * in which the elements need not only to be linked directly to the element
 * before and after, but also to elements in other lists.
 * There one list elements corresponds to one point (in this implementation
 * to one {@link VertexTriple}, in which only the second element, that is a point,
 * is relevant) and crossreferences are needed for the listelements of the
 * same point in other dimension, as well as to the listelements of the
 * same point in a copy/original list.
 * <br>
 * A good or better name would also be SplitTreeAlgorithmListElement as this
 * class is only needed for the construction-algorithm of the {@link SplitTree}.
 * <br>
 * ____________
 * <br>
 * To the shift from points to {@link VertexTriple}s see {@link SplitTree}
 */
public class SplitTreeListElement<V> implements Comparable<SplitTreeListElement<V>>{
	/**
	 * Point (respectively {@link VertexTriple}, that has a point as second value)
	 * that is represented by this {@link SplitTreeListElement}
	 */
	public VertexTriple<V> referenceTriple;
	/**
	 * Here the dimension for this {@link SplitTreeListElement} must
	 * be determined as in the {@link SplitTree}-construction-algorithm
	 * there are lists for every dimension.
	 * In the plane valid values for this variable are 0 (x-dimension)
	 * and 1 (y-dimension).
	 * <br>
	 * This variable is needed for the {@link SplitTreeListElement#compareTo(SplitTreeListElement)}
	 * method.
	 */
	public int dimension;
	/**
	 * Link to the {@link SplitTreeListElement} of the same
	 * {@link SplitTreeListElement#referenceTriple} (reference-point/triple)
	 * in the list of the other dimension (in x-dimension to y-dimension and vice versa).
	 * <br>
	 * (Note: Is this an Element of the copy-list CLS_x or CLS_y then
	 * this should also links to the copy-list of the other dimension)
	 */
	public SplitTreeListElement<V> listenElementInOtherDimension;
	/**
	 * Link to the {@link SplitTreeListElement} of the same
	 * {@link SplitTreeListElement#referenceTriple} (reference-point/triple)
	 * in the copy-/original-list.
	 * If this is an element of the original-list
	 * then it should link to the element of the same point({@link VertexTriple}) 
	 * and same dimension in the copy-list.
	 * If this is an element of the copy-list
	 * then it should link to the element of the same point({@link VertexTriple}) 
	 * and same dimension in the original-list.
	 */
	public SplitTreeListElement<V> listElementInCopyOrOriginalList;
	
	/**
	 * This is needed and used by the algorithm
	 * {@link SplitTree#computeFastSplitTree(SplitTreeNode, LinkedList<SplitTreeListelement>[])}
	 */
	public SplitTreeNode<V> pointerToSplitTreeNode;
	
	/**
	 * The internal storage of {@link LinkedList} is made with {@link Node}s.
	 * There it is neither intended that an object knows (has a reference to) the {@link Node} with which it is
	 * stored nor that it can manipulate that {@link Node}.
	 * The {@link Node} class is private within the original {@link LinkedList}.
	 * But one could want to manipulate it given an object to achieve a remove-operation
	 * of a list element from the list in constant time (in O(1)).
	 * This deletion of an list element must be doable in constant time for the
	 * {@link SplitTree}-construction-algorithm to fulfil the O(n log n) upper-bound
	 * with n being the size of the point set.
	 * So {@link wspd.LinkedList} was copied and changed only that
	 * the class {@link Node} is not private any more.
	 * And a {@link wspd.LinkedList} of these {@link SplitTreeListElement}s
	 * must be created and this variable here then can be used to link to the
	 * fitting {@link Node}-object.
	 * <br>
	 * This variable has to be set manually by the user.
	 * It is null after instantiation (or insertion in a list).
	 */
	public Node<SplitTreeListElement<V>> correspondingNodeInTheLinkedList=null;
	
	/**
	 * Constructor for {@link SplitTreeListElement}.
	 * For the 2 input-parameters see the description {@link SplitTreeListElement#referenceTriple}
	 * and {@link SplitTreeListElement#dimension}.
	 * 
	 * @param referenceTriple
	 * @param dimension
	 */
	public SplitTreeListElement(VertexTriple<V> referenceTriple, int dimension){
		this.referenceTriple = referenceTriple;
		this.dimension = dimension;
	}
	
	public int compareTo(SplitTreeListElement<V> o) {
		if(dimension==0){
			return Double.compare(this.referenceTriple.get2().getX(), o.referenceTriple.get2().getX());
		}
		return Double.compare(this.referenceTriple.get2().getY(), o.referenceTriple.get2().getY());
	}
	
	/**
	 * Warning:
	 * <br>
	 * The variable {@link SplitTreeListElement#correspondingNodeInTheLinkedList} links to the
	 * current {@link wspd.LinkedList} (and there to one {@link Node}).
	 * This variable has to be set manually.
	 * As the cloned {@link SplitTreeListElement} can definitely not be stored by the same
	 * {@link Node}-object the old value of that variable is not copied and instead
	 * it is set to null in the cloned object.
	 * It has to be set manually afterwards.
	 */
	public SplitTreeListElement<V> clone(){
		SplitTreeListElement<V> copy = new SplitTreeListElement<V>(this.referenceTriple, this.dimension);
		copy.listenElementInOtherDimension = this.listenElementInOtherDimension;
		copy.listElementInCopyOrOriginalList = this.listElementInCopyOrOriginalList;
		copy.pointerToSplitTreeNode = this.pointerToSplitTreeNode;
		return copy;
	}
	
	@Override
	public String toString(){
		return "SplitTreeListelement[p="+this.referenceTriple.get2()+"]";
	}
}
