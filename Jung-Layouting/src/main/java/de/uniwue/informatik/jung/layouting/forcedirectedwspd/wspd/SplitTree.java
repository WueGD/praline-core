package de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout.FRVertexData;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Counter;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.LinkedList.Node;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Split tree for a set of points
 * as it is described in the book "Geometric Spanner Networks" from G. Narasimhan and Michiel Smid from the year 2007
 * 
 * <p>
 * Annotation:
 * 
 * This class was adapted later.
 * Originally the {@link SplitTree} was given a set of points (as expected acc. to literature),
 * later a set of {@link VertexTriple}s of vertex-information, {@link Point2D}, {@link FRVertexData}.
 * This was done to be compatible with the changes of the layout algorithms (only reason for that: better access times in practice).
 * See {@link FRLayoutNoMaps} and {@link FRWSPD}.
 * The split tree is still built for a set of points by using only the 2nd value of the {@link VertexTriple},
 * this is a point.
 * This point corresponds therefore with its {@link VertexTriple}.
 */
public class SplitTree<V>{
	
	private SplitTreeNode<V> root = null;
	
	private int sizeOfThePointSet;
	
	/** 
	 * Calculates for the given set of {@link VertexTriple}s (corresponding to a set of points,
	 * that one gets by taking the 2nd values of the {@link VertexTriple}s) immedeately the
	 * {@link SplitTree} to be able to execute operations on it afterwards.
	 * Construction is done using the algorithm "FastSplitTree(point set S, rectangle R)"
	 * as described in the book (see {@link SplitTree}).
	 * The theoretical concept is adappted in minor extend for concrete implemtation
	 * but is the same in terms of procedure and needed time class, that is O(n log n)
	 * with n being the size (cardinality) of the point set.
	 * 
	 * @param setOfTriples
	 */
	public SplitTree(Collection<VertexTriple<V>> setOfTriples){
		sizeOfThePointSet = setOfTriples.size();
		/*
		 * catch case that an empty set was gotten
		 */
		if(setOfTriples.size()==0){
			setRoot(new SplitTreeNode<V>(new Rectangle(0,0,0,0)));
			return;
		}
		/*
		 * Create 2 lists for the points (saved with its tripels in SplitTreeListElements) and sort them
		 * acc. to their x-coord. (1st list) and their y-coord. (2nd list).
		 * Both lists contain all points.
		 * 
		 * Note: LinkedList is here the adapted version from the same package
		 */
		LinkedList<SplitTreeListElement<V>>[] ls = new LinkedList[2];
		for(int i=0; i<ls.length; i++){
			ls[i] = new LinkedList<SplitTreeListElement<V>>();
		}
		//Fill lists and add cross reference from listelements in one list to the listelements in the other list
		//where the points are the same
		//also links from a splittreelistelement to its Node-object from the class LinkedList
		for(Iterator<VertexTriple<V>> i=setOfTriples.iterator(); i.hasNext();){
			VertexTriple<V> currentTripel = i.next();
			for(int j=0; j<ls.length; j++){
				SplitTreeListElement<V> lsElement = new SplitTreeListElement<V>(currentTripel, j);
				ls[j].add(lsElement);
				lsElement.correspondingNodeInTheLinkedList = ls[j].last; //set pointer to Node
			}
			for(int j=0; j<ls.length; j++){
				ls[j].getLast().listenElementInOtherDimension = ls[(j+1)%2].getLast(); //insert cross reference
			}
		}
		//Sort ascending
		for(int i=0; i<ls.length; i++){
			Collections.sort(ls[i]);
		}
		/*
		 * The linking (splittreelistelement -> Node) has to be renewed,
		 * because it became incorrect by sorting the lists
		 * This can be seen by using checkLinking(ls) in this class.
		 */
		linkNodesToTheElementsInTheLinkedLists(ls);
		/*
		 * Create Rectangle R, that is needed for the initial call of
		 * computeFastSplitTree(Pointset S, Rectangle R, Lists ls).
		 * Must include all points of the point set.
		 * I define R as an square.
		 */
		double squareSideLength = Math.max(ls[0].getLast().referenceTriple.get2().getX()-ls[0].getFirst().referenceTriple.get2().getX(),
				ls[1].getLast().referenceTriple.get2().getY()-ls[1].getFirst().referenceTriple.get2().getY());
		double[] cornerPointCoordinates = new double[2]; //coordinates for the boundary of square r (cornerpoint for the square with lowest x-/y-coordinate) 
		for(int i=0; i<cornerPointCoordinates.length; i++){
			cornerPointCoordinates[i] = computMidOfTheEdgeOfTheBoundingBoxViaSortedLists(ls, i)-0.5*squareSideLength;
		}
		Rectangle r0 = new Rectangle(cornerPointCoordinates[0], cornerPointCoordinates[1], squareSideLength, squareSideLength);
		/*
		 * Calculate Split tree
		 */
		SplitTreeNode<V> root = new SplitTreeNode<V>(r0);
		setRoot(root);
		computeFastSplitTree(root, ls);
	}
	
	
	
	
	
	
	
	/*
	 * Public Methods
	 */
	
	
	/**
	 * Size (cardinality) of the point set (resp. the corresponding set of {@link VertexTriple}s).
	 * Is a saved value, so calling this is in O(1).
	 * 
	 * @return
	 */
	public int getSizeOfThePointSet(){
		return sizeOfThePointSet;
	}
	
	/**
	 * Returns bounding rectangle of {@link SplitTreeNode}.
	 * That is the smallest axis-parallel {@link Rectangle} containing all points of the point set
	 * belonging to the given {@link SplitTreeNode}.
	 * The value saved in {@link SplitTreeNode} is returned (and not checked if consistent/correct!).
	 * If no value is saved yet it is calculated recursively in O(n)
	 * with n being the number of split tree nodes (what is also linear to the number of points,
	 * because it is a binary-tree and a split tree node corresponds to an point iff it is a leaf)
	 * 
	 * @param stn
	 * @return
	 */
	public Rectangle getBoundingRectangle(SplitTreeNode<V> stn){
		if(stn.r==null){
			if(isLeaf(stn)){
				stn.r = new Rectangle(stn.triple.get2().getX(), stn.triple.get2().getY(), 0, 0);
			}
			else{
				double minX = Double.POSITIVE_INFINITY;
				double minY = Double.POSITIVE_INFINITY;
				double maxX = Double.NEGATIVE_INFINITY;
				double maxY = Double.NEGATIVE_INFINITY;
				for(SplitTreeNode<V> child: getChildren(stn)){
					if(minX<getBoundingRectangle(child).getX()){
						minX = getBoundingRectangle(child).getX();
					}
					if(minY<getBoundingRectangle(child).getY()){
						minY = getBoundingRectangle(child).getY();
					}
					if(maxX<getBoundingRectangle(child).getX()+getBoundingRectangle(child).getWidth()){
						maxX = getBoundingRectangle(child).getX()+getBoundingRectangle(child).getWidth();
					}
					if(maxY<getBoundingRectangle(child).getY()+getBoundingRectangle(child).getHeight()){
						maxY = getBoundingRectangle(child).getY()+getBoundingRectangle(child).getHeight();
					}
				}
				stn.r = new Rectangle(minX, minY, maxX-minX, maxY-minY);
			}
		}
		return stn.r;
	}
	
	/**
	 * Calculates for every {@link SplitTreeNode} stn the bounding {@link Rectangle} of its referenced point set.
	 * That is set of points (corresponding with one leaf in the split tree) in the subtree rooted at stn.
	 * If no values are saved it is calculated first time otherwise the old value is overwritten.
	 * Algorithm works recursively and
	 * time is in O(n) with n being the number of points (or number of {@link SplitTreeNode}s). 
	 */
	public void recalculateAllBoundingRectangles(){
		//1. reset
		resetAllBoundingRectanglesRecursively(root);
		//2. calculate new
		getBoundingRectangle(this.getRoot());
	}
	
	private void resetAllBoundingRectanglesRecursively(SplitTreeNode<V> stn){
		stn.r = null;
		for(SplitTreeNode<V> child: getChildren(stn)){
			resetAllBoundingRectanglesRecursively(child);
		}
	}
	
	/**
	 * Returns for {@link SplitTreeNode} stn the barycenter of its points.
	 * The value saved in {@link SplitTreeNode} is returned (and not checked if consistent/correct!).
	 * If no value is saved yet it is calculated recursively in O(n)
	 * with n being the number of split tree nodes (what is also linear to the number of points,
	 * because it is a binary-tree and a split tree node corresponds to an point iff it is a leaf)
	 * 
	 * @param stn
	 * @return
	 */
	public Point2D getBarycenter(SplitTreeNode<V> stn){
		if(stn.barycenter==null){
			if(this.isLeaf(stn)){
				stn.barycenter = new Point2D.Double(stn.triple.get2().getX(), stn.triple.get2().getY());
			}
			else{
				double sumX = 0;
				double sumY = 0;
				for(SplitTreeNode<V> child: this.getChildren(stn)){
					Point2D barycenterOfChild = getBarycenter(child);
					sumX += barycenterOfChild.getX()*getSizeOfTheTripleSetInTheSubtreeBelow(child); //attach weight to the value
					sumY += barycenterOfChild.getY()*getSizeOfTheTripleSetInTheSubtreeBelow(child); //attach weight to the value
				}
				stn.barycenter = new Point2D.Double(sumX/(double)getSizeOfTheTripleSetInTheSubtreeBelow(stn),
						sumY/(double)getSizeOfTheTripleSetInTheSubtreeBelow(stn));
			}
		}
		return stn.barycenter;
	}
	
	/**
	 * Calculates for every {@link SplitTreeNode} stn the barycenter of its referenced point set.
	 * That is set of points (corresponding with one leaf in the split tree) in the subtree rooted at stn.
	 * If no values are saved it is calculated first time otherwise the old value is overwritten.
	 * Algorithm works recursively and
	 * time is in O(n) with n being the number of points (or number of {@link SplitTreeNode}s). 
	 */
	public void recalculateAllBarycenters(){
		//1. reset
		resetAllBarycentersRecursively(root);
		//2. calculate new
		getBarycenter(this.getRoot());
	}
	
	private void resetAllBarycentersRecursively(SplitTreeNode<V> stn){
		stn.barycenter = null;
		for(SplitTreeNode<V> child: getChildren(stn)){
			resetAllBarycentersRecursively(child);
		}
	}
	
	/**
	 * Returns the number of leaves in the subtree rooted at {@link SplitTreeNode} stn.
	 * A {@link SplitTreeNode} corresponds to an point ( respectively {@link VertexTriple}) of the point set iff
	 * it is a leaf.
	 * So this is also the number of points (and {@link VertexTriple}s) under stn.
	 * Once calculated recursively in O(n) time (with n being the size of the point set)
	 * the value is saved in the {@link SplitTreeNode} and returned in O(1) time.
	 * To avoid inconsistencies the {@link SplitTree} and its {@link SplitTreeNode}s
	 * should not be changed or the affected values saved in {@link SplitTreeNode} should be
	 * reset/calculated new.
	 * One call of this method for the root of the {@link SplitTree} is enough to have
	 * a saved value in every {@link SplitTreeNode} in this {@link SplitTree}.
	 * 
	 * @param stn
	 * @return
	 */
	public int getSizeOfTheTripleSetInTheSubtreeBelow(SplitTreeNode<V> stn){
		if(stn.sizeOfTheTripleSetInTheSubtreeBelow==-1){
			if(stn.tripleSetInTheSubtreeBelow!=null){
				stn.sizeOfTheTripleSetInTheSubtreeBelow = stn.tripleSetInTheSubtreeBelow.size();
			}
			else{
				if(this.isLeaf(stn)){
					stn.sizeOfTheTripleSetInTheSubtreeBelow = 1;
				}
				else{
					Collection<SplitTreeNode<V>> children = this.getChildren(stn);
					Iterator<SplitTreeNode<V>> i = children.iterator();
					stn.sizeOfTheTripleSetInTheSubtreeBelow = getSizeOfTheTripleSetInTheSubtreeBelow(i.next())
							+ getSizeOfTheTripleSetInTheSubtreeBelow(i.next()); //recursive call for left and right child
				}
			}
		}
		return stn.sizeOfTheTripleSetInTheSubtreeBelow;
	}
	
	/**
	 * A collection of the point set being in the subtree rooted at {@link SplitTreeNode} stn is returned.
	 * Once found the collection is saved in every {@link SplitTreeNode}.
	 * This works in the same time and manner as {@link SplitTree#getSizeOfTheTripleSetInTheSubtreeBelow(SplitTreeNode)}.
	 * For more information see there. 
	 * 
	 * @param stn
	 * @return
	 */
	public Collection<VertexTriple<V>> getTripleSetInTheSubtreeBelow(SplitTreeNode<V> stn){
		if(stn.tripleSetInTheSubtreeBelow==null){
			stn.tripleSetInTheSubtreeBelow = new LinkedList<VertexTriple<V>>();
			if(this.isLeaf(stn)){
				stn.tripleSetInTheSubtreeBelow.add(stn.triple);
			}
			else{
				Collection<SplitTreeNode<V>> children = this.getChildren(stn);
				Iterator<SplitTreeNode<V>> i = children.iterator();
				stn.tripleSetInTheSubtreeBelow.addAll(getTripleSetInTheSubtreeBelow(i.next())); //recursive call for left
				stn.tripleSetInTheSubtreeBelow.addAll(getTripleSetInTheSubtreeBelow(i.next())); //and for right child
			}
		}
		return stn.tripleSetInTheSubtreeBelow;
	}
	
	/**
	 * Same as {@link SplitTree#getTripleSetInTheSubtreeBelow(SplitTreeNode)}.
	 * Only difference: The corresponding points are added to the
	 * collection gotten as input in this method.
	 * 
	 * @param stn {@link SplitTreeNode} in this {@link SplitTree}
	 * @param collectionOfTriples A collection of {@link VertexTriple}s to which the found points shall be appended.
	 * @return the collection-object gotten as input 
	 */
	public Collection<VertexTriple<V>> getTripleSetInTheSubtreeBelow(SplitTreeNode<V> stn,
			Collection<VertexTriple<V>> collectionOfTriples){
		collectionOfTriples.addAll(getTripleSetInTheSubtreeBelow(stn));
		return collectionOfTriples;
	}
	
	/**
	 * see {@link SplitTree#getRepulsiveForce(SplitTreeNode)}
	 * 
	 * @param stn
	 * @param xOffset
	 * @param yOffset
	 */
	public void addRepulsiveForce(SplitTreeNode<V> stn, double xOffset, double yOffset){
		stn.repulsiveForce.setLocation(stn.repulsiveForce.getX()+xOffset, stn.repulsiveForce.getY()+yOffset);
	}
	
	/**
	 * Returns the value of the repulsive force saved at {@link SplitTreeNode} stn.
	 * The value can be changed by using {@link SplitTree#addRepulsiveForce(SplitTreeNode, double, double)}
	 * and {@link SplitTree#resetRepulsiveForce(SplitTreeNode)} / {@link SplitTree#resetRepulsiveForceOfAllSplitTreeNodes()}.
	 * <p>
	 * Used in a {@link WellSeparatedPairDecomposition}-graph drawing algorithm this is the repulsive
	 * force calculated for one part of a pair of the {@link WellSeparatedPairDecomposition} used in the
	 * algorithm.
	 */
	public Point2D getRepulsiveForce(SplitTreeNode<V> stn){
		return stn.repulsiveForce;
	}
	
	/**
	 * see {@link SplitTree#getRepulsiveForce(SplitTreeNode)}
	 * <br>
	 * This operation is done only for this {@link SplitTreeNode}.
	 * NOT for all {@link SplitTreeNode}s in the subtree
	 * rooted at stn. 
	 * 
	 * @param stn
	 */
	public void resetRepulsiveForce(SplitTreeNode<V> stn){
		stn.repulsiveForce.setLocation(0, 0);
	}
	
	/**
	 * Recursive algorithm working in O(n) with n being
	 * the size of the point/{@link VertexTriple} set
	 * <br>
	 * see {@link SplitTree#getRepulsiveForce(SplitTreeNode)}
	 */
	public void resetRepulsiveForceOfAllSplitTreeNodes(){
		resetRepulsiveForceRecursively(root);
	}
	
	private void resetRepulsiveForceRecursively(SplitTreeNode<V> stk){
		resetRepulsiveForce(stk);
		for(SplitTreeNode<V> child: getChildren(stk)){
			resetRepulsiveForceRecursively(child);
		}
	}
	
	
	/**
	 * The class {@link SplitTree} does not have {@link Tree} or {@link DelegateTree} or {@link Graph} as superclass.
	 * As it is in principle still a tree one can call this method to get the corresponding {@link DelegateTree} to
	 * this {@link SplitTree}.
	 * This is useful for using methods/applications expacting a JUNG-tree object.
	 * The returned {@link DelegateTree} is no deep copy of this {@link SplitTree}.
	 * 
	 * @return
	 */
	public DelegateTree<SplitTreeNode<V>, Integer> createDelegateTreeToThisSplitTree(){
		DelegateTree<SplitTreeNode<V>, Integer> correspondingDelegateTree =
				new DelegateTree<SplitTreeNode<V>, Integer>();
		correspondingDelegateTree.setRoot(this.getRoot());
		
		Counter edgeCounter = new Counter();
		addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, this.getRoot(), edgeCounter);
		
		return correspondingDelegateTree;
	}
	
	private void addChildrenToCorrespondingDelegateTree(DelegateTree<SplitTreeNode<V>, Integer> correspondingDelegateTree,
			SplitTreeNode<V> currentNode, Counter edgeCounter){
				
		if(currentNode.leftChild != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.leftChild);
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.leftChild, edgeCounter);
		}
		if(currentNode.rightChild != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.rightChild);
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.rightChild, edgeCounter);
		}
	}
	
	
	
	
	
	
	
	
	/*
	 * Private Methods
	 */
	
	
	/**
	 * This is a recursive method that implements two algorithms
	 * described in the book (see {@link SplitTree}).
	 * But these two algorithms are closly related, these are:
	 * <p> 
	 * 1st: The algorithm FastSplitTree(S, R) with S being the point set
	 * and R being the rectangle that also encloses the bounding box of S.
	 * <br>
	 * Instead of these two a {@link SplitTreeNode} u is passed as an
	 * input object and a field of lists ls (for every dimension one - so here 2 lists).
	 * u is used as root for the subtree containing the point set instead of
	 * returning a {@link SplitTreeNode}.
	 * The rectangle R can be addressed via u.r0 and the point set S
	 * is saved in the lists ls.
	 * So this change in input and output compared to the textbook
	 * does not make problems to the general principal of the method.
	 * The lists ls must be sorted and consist of correct {@link SplitTreeListElement}s.
	 * u.r0 must be a consistent rectangle to the point set saved in ls.
	 * <br>
	 * The algorithm FastSplitTree(S, R), as described in the book, is rather
	 * simple and consists in a large part out of
	 * preprocessing operations, that are thus not part of this recursive
	 * method and that are executed in the constructor of this class
	 * before calling this method (create lists and sort them).
	 * Only the few instructions of the method that must be done
	 * recursively are included here.
	 * <p>
	 * 2nd: The algorithm is PartialSplitTree(S, R, (LSi)1<=i<=d) with S being
	 * the point set, R being the rectangle that also encloses the bounding box of S,
	 * and (LSi)1<=i<=d being the d lists that save coordinates of the n:=|point set|
	 * points each list for the i-th dimension (so d is the number of dimensions each
	 * point has, here d=2).
	 * Analogous to FastSplitTree(S, R), S can be gotten from ls and r can be gotten
	 * by calling u.r0.
	 * So input and output is adopted in the same way as for FastSplitTree(S, R).
	 * This algorithm works with 6 steps that are implemented as described in the book.
	 * <p>
	 * The recursive call works as follows.
	 * For every passed set of points a partial tree is built whose leaves
	 * represent a subset of the passed point set with size n/2 at most.
	 * For every of those leaves the method is called again. 
	 * 
	 * @param u root for the subtree that shall be created over the point set
	 * delivierd via ls. Rectangle u.r0 must be an {@link Rectangle} consistent with the point set of ls.
	 * @param ls an array of 2 lists (one for x- one for y-dimension), each
	 * containg a {@link SplitTreeListElement} for every point (respectively {@link VertexTriple})
	 * of the desired point set. They must be sorted ascending in its x- respectively y-coordinate.
	 */
	private void computeFastSplitTree(SplitTreeNode<V> u, LinkedList<SplitTreeListElement<V>>[] ls){
		/*
		 * #######
		 * Instructions of FastSplitTree(S, R)
		 * #######
		 */
				
		/* 
		 * Catch special case |point set|==1.
		 * In this trivial case a simple trivial tree is created.
		 */
		if(ls[0].size()==1){
			double x = ls[0].iterator().next().referenceTriple.get2().getX();
			double y = ls[0].iterator().next().referenceTriple.get2().getY();
			u.r = new Rectangle(x,y,0,0);
			u.triple = ls[0].iterator().next().referenceTriple;
			return;
		}
		
		/*
		 * #######
		 * Instructions of PartialSplitTree(S, R, (LSi)1<=i<=d)
		 * #######
		 */
		
		//In step 6 all leaves of the partial tree created before are needed
		//these are saved in this list.
		LinkedList<SplitTreeNode<V>> leaves = new LinkedList<SplitTreeNode<V>>();
		/*
		 * Step 1: 
		 * Copy ls[i] as cls[i] and link the same elements.
		 * Define n=|point set|.
		 * Create node u, that is the root for the partial split tree that will be built.
		 * Set u.r0 = r.
		 * Unnaming as described in the book are not made here. This does not change the functionality of the algorithm.
		 */
		//copy lists
		LinkedList<SplitTreeListElement<V>>[] cls = new LinkedList[ls.length];
		for(int i=0; i<ls.length; i++){
			cls[i] = new LinkedList<SplitTreeListElement<V>>();
			for(Iterator<SplitTreeListElement<V>> j=ls[i].iterator(); j.hasNext();){
				SplitTreeListElement<V> currentLsEl = j.next();
				cls[i].add(currentLsEl.clone());
				cls[i].getLast().listElementInCopyOrOriginalList = currentLsEl; //The two links: from copy to original
				currentLsEl.listElementInCopyOrOriginalList = cls[i].getLast(); //and from original to copy
			}
		}
		//adapt links in the copies
		for(int i=0; i<cls.length; i++){
			for(Iterator<SplitTreeListElement<V>> j=cls[i].iterator(); j.hasNext();){
				SplitTreeListElement<V> currentLsEl = j.next();
				currentLsEl.listenElementInOtherDimension = currentLsEl.listenElementInOtherDimension.listElementInCopyOrOriginalList;
			}
		}
		//further instruction of step 1
		int size = cls[0].size(); //pointset.size() is also termed "n" in the book
		/*
		 * Step 2:
		 * A case-check is done regarding the size.
		 * In one case the program proceeds on step 3.
		 * In the other case a few more operations in step 2 are executed and
		 * then the program jumps to step 6.
		 */
		while(!(size<=cls[0].size()/2)){
			/*
			 * Step 3:
			 * Set u.r = boundingBox(point set).
			 * Determine the index of the longer side of u.r
			 * (it is index=0 if the edge of the rectangle parallel to the x-axis is longer than
			 * the edge of the regtangle parallel to the y-axis, and index=1 else).
			 * Find a separating line h that is orthogonal to the longer side (gotten by the just
			 * determined index) and in the mid of this longer side of u.r.
			 * (For h only a double-value has to be determined, because the dimension can be obtained by the index-value)
			 * After that a procedure described in the book is executed.
			 * At last a check whether the program shall proceed to step 4 or step 5 is done.
			 */
			u.r = getBoundingBoxForSortedLists(ls);
			int index = determineIndexOfTheLongerSideViaSortedLists(ls);
			double h = computMidOfTheEdgeOfTheBoundingBoxViaSortedLists(ls, index);
			//<procedure from the book>
			Iterator<SplitTreeListElement<V>> forwardsIterator = ls[index].iterator();
			Iterator<SplitTreeListElement<V>> backwardsIterator = ls[index].descendingIterator();
			VertexTriple<V> p = forwardsIterator.next().referenceTriple;
			VertexTriple<V> pPrime = forwardsIterator.next().referenceTriple;
			VertexTriple<V> q = backwardsIterator.next().referenceTriple;
			VertexTriple<V> qPrime = backwardsIterator.next().referenceTriple;
			int sizePrime = 1;
			//Annotation: The check, if the forwardsIterator has a next element is done to catch a java.util.NoSuchElementException.
			//This case can occur, if all elements of the list have the same coordinate in all dimensions (same point).
			//Because forwardsIterator.hastNext() must be the same as backwardsIterator.hasNex() only one is checked.
			double pPrimeXOrYInDimensionIndex;
			double qPrimeXOrYInDimensionIndex;
			if(index==0){
				pPrimeXOrYInDimensionIndex = pPrime.get2().getX();
				qPrimeXOrYInDimensionIndex = qPrime.get2().getX();
			}
			else{
				pPrimeXOrYInDimensionIndex = pPrime.get2().getY();
				qPrimeXOrYInDimensionIndex = qPrime.get2().getY();				
			}
			while((pPrimeXOrYInDimensionIndex<=h && qPrimeXOrYInDimensionIndex>=h) && forwardsIterator.hasNext()){
				p = pPrime;
				pPrime = forwardsIterator.next().referenceTriple;
				q = qPrime;
				qPrime = backwardsIterator.next().referenceTriple;
				sizePrime+=1;
				//determine values again
				if(index==0){
					pPrimeXOrYInDimensionIndex = pPrime.get2().getX();
					qPrimeXOrYInDimensionIndex = qPrime.get2().getX();
				}
				else{
					pPrimeXOrYInDimensionIndex = pPrime.get2().getY();
					qPrimeXOrYInDimensionIndex = qPrime.get2().getY();				
				}
			}
			//</procedure from the book>
			if(pPrimeXOrYInDimensionIndex>h){
				/*
				 * Step 4:
				 * see book
				 */
				SplitTreeNode<V> v = new SplitTreeNode<V>(devideRectangleAndReturnLeftSide(u.r, index, h)); //create left child
				addChild(u, v);
				SplitTreeNode<V> w = new SplitTreeNode<V>(devideRectangleAndReturnRightSide(u.r, index, h)); //create right child
				addChild(u, w);
				//adjust lists
				for(Iterator<SplitTreeListElement<V>> i=ls[index].iterator(); i.hasNext();){
					SplitTreeListElement<V> currentLiEl = i.next();
					currentLiEl.listElementInCopyOrOriginalList.pointerToSplitTreeNode = v; //Set pointers to the corresponding points in the copylists to this SplitTreeNode
					currentLiEl.listElementInCopyOrOriginalList.listenElementInOtherDimension.pointerToSplitTreeNode = v; //and this in both dimensions
					//Value of the just handled point in the other list (other dimension) is removed.
					//That works in O(1) by addressing the associated instance of the class node (defined inside of LinkedList).
					//As this class is private in the original LinkedList-class and thus not addressable, LinkedList was copied in this
					//package and the "private"-property of the class Node removed. For that see also class SplitTreeListelement
					//and the description to the variable that links to the Node-object
					ls[(index+1)%2].unlink(currentLiEl.listenElementInOtherDimension.correspondingNodeInTheLinkedList);
					//Value of the just handled point in this list is removed. This time via the iterator.
					i.remove();
					if(currentLiEl.referenceTriple==p){
						break;
					}
				}
				//u and size are assigned with new values, after this go back to step 2 (while-loop-check)
				u = w;
				size = size-sizePrime;
				//hence v is a leaf of this partial tree
				leaves.add(v);
			}
			else{
				/*
				 * Step 5:
				 * see book, this step works analogous to step 4
				 */
				SplitTreeNode<V> v = new SplitTreeNode<V>(devideRectangleAndReturnLeftSide(u.r, index, h)); //create left child
				addChild(u, v);
				SplitTreeNode<V> w = new SplitTreeNode<V>(devideRectangleAndReturnRightSide(u.r, index, h)); //create right child
				addChild(u, w);
				//adjust lists
				for(Iterator<SplitTreeListElement<V>> i=ls[index].descendingIterator(); i.hasNext();){
					SplitTreeListElement<V> currentLiEl = i.next();
					currentLiEl.listElementInCopyOrOriginalList.pointerToSplitTreeNode = w; //Set pointer to correspoinding points in copylists
					currentLiEl.listElementInCopyOrOriginalList.listenElementInOtherDimension.pointerToSplitTreeNode = w; //and that for both dimensions
					//Value of the just handled point in the other list (other dimension) is removed.
					//That works in O(1) by addressing the associated instance of the class node (defined inside of LinkedList).
					//As this class is private in the original LinkedList-class and thus not addressable, LinkedList was copied in this
					//package and the "private"-property of the class Node removed. For that see also class SplitTreeListelement
					//and the description to the variable that links to the Node-object
					ls[(index+1)%2].unlink(currentLiEl.listenElementInOtherDimension.correspondingNodeInTheLinkedList);
					//Value of the just handled point in this list is removed. This time via the iterator.
					i.remove();
					if(currentLiEl.referenceTriple==q){
						break;
					}
				}
				//u and size are assigned with new values, after this go back to step 2 (while-loop-check)
				u = v;
				size = size-sizePrime;
				//hence w is a leaf of this partial tree
				leaves.add(w);
			}
		}
		//This following for-loop belongs to step 2
		for(int i=0; i<ls.length; i++){ //go through both lists and link SplitTreeNode u
			for(Iterator<SplitTreeListElement<V>> j=ls[i].iterator(); j.hasNext();){
				SplitTreeListElement<V> s = j.next();
				s.listElementInCopyOrOriginalList.pointerToSplitTreeNode = u;
			}
		}
		/*
		 * Step 6:
		 * 
		 * Create lists for all leaves of the created partial split tree.
		 * Then go through the copylists cls and fill these and adjust the pointers.
		 * At last for every leaf the enclosing rectangle r is computed.
		 */
		leaves.add(u);
		for(SplitTreeNode<V> stn: leaves){
			stn.lists = new LinkedList[2];
			for(int i=0; i<stn.lists.length; i++){
				stn.lists[i] = new LinkedList<SplitTreeListElement<V>>();
			}
		}
		//go through cls and fill new lists
		for(int i=0; i<cls.length; i++){
			for(Iterator<SplitTreeListElement<V>> j=cls[i].iterator(); j.hasNext();){
				SplitTreeListElement<V> currentClsEl = j.next();
				SplitTreeListElement<V> newLsEl = currentClsEl.clone();
				currentClsEl.listElementInCopyOrOriginalList = newLsEl;
				currentClsEl.pointerToSplitTreeNode.lists[i].add(newLsEl);
			}
		}
		//adjust cross-references inside the new lists
		for(SplitTreeNode<V> stn: leaves){
			for(int i=0; i<stn.lists.length; i++){
				for(Iterator<SplitTreeListElement<V>> j=stn.lists[i].iterator(); j.hasNext();){
					SplitTreeListElement<V> currentLsEl = j.next();
					currentLsEl.listenElementInOtherDimension = currentLsEl.listenElementInOtherDimension.listElementInCopyOrOriginalList;
				}
			}
			linkNodesToTheElementsInTheLinkedLists(stn.lists); //link entries correctly
		}
		//determine r for the leaves
		for(SplitTreeNode<V> stn: leaves){
			stn.r=getBoundingBoxForSortedLists(stn.lists);
		}
		//Call algorithm recursively for all leaves
		for(SplitTreeNode<V> stn: leaves){
			computeFastSplitTree(stn, stn.lists);
			stn.lists = null; //remove reference that came out of use to save storage space
		}
	}
	
	/**
	 * Returns the smallest rectangle of which the edges are parallel to the x- or y-axis,
	 * that encloses the point set belonging to the given list.
	 * Here the lists MUST be sorted,
	 * because it is simply computed by taking the first and last value.
	 * 
	 * @param ls
	 * @return
	 */
	private Rectangle getBoundingBoxForSortedLists(LinkedList<SplitTreeListElement<V>>[] ls){
		return new Rectangle(ls[0].getFirst().referenceTriple.get2().getX(), ls[1].getFirst().referenceTriple.get2().getY(),
				ls[0].getLast().referenceTriple.get2().getX()-ls[0].getFirst().referenceTriple.get2().getX(),
				ls[1].getLast().referenceTriple.get2().getY()-ls[1].getFirst().referenceTriple.get2().getY());
	}
	
	/**
	 * Returns 0, if the edge of the bounding box of the point set saved in ls parallel to the
	 * x-axis is longer than the edge parallel to the y-axis, and returns 1 else.
	 * Here the lists MUST be sorted,
	 * because it is simply computed by taking the first and last value and subtracting them.
	 * 
	 * @param ls
	 * @return index
	 */
	protected int determineIndexOfTheLongerSideViaSortedLists(LinkedList<SplitTreeListElement<V>>[] ls){
		if(ls[0].getLast().referenceTriple.get2().getX()-ls[0].getFirst().referenceTriple.get2().getX() >
					ls[1].getLast().referenceTriple.get2().getY()-ls[1].getFirst().referenceTriple.get2().getY()){
			return 0;
		}
		return 1;
	}
	
	/**
	 * Computes the x-value (if dimension=0) or the y-value (if dimension=1 or sth else) of the mid
	 * of the edge of the bounding box of the point set saved in ls parallel to the x-axis (if dimension=0) / y-axis (else).
	 * Here the lists MUST be sorted.
	 * 
	 * @param ls
	 * @param dimension
	 * @return
	 */
	private double computMidOfTheEdgeOfTheBoundingBoxViaSortedLists(LinkedList<SplitTreeListElement<V>>[] ls, int dimension){
		double startpoint;
		double endpoint;
		if(dimension==0){
			startpoint = ls[dimension].getFirst().referenceTriple.get2().getX();
			endpoint = ls[dimension].getLast().referenceTriple.get2().getX();
		}
		else{
			startpoint = ls[dimension].getFirst().referenceTriple.get2().getY();
			endpoint = ls[dimension].getLast().referenceTriple.get2().getY();
		}
		return startpoint+(endpoint-startpoint)*0.5;
	}
	
	/**
	 * Creates a new {@link Rectangle}.
	 * This {@link Rectangle} gets its location and size by taking the given rectangle and creating a
	 * new {@link Rectangle} that is only the values smaller or equal to coordinateOfTheSeparatingLine
	 * in x-dimension (if dimension=0) or y-dimension (if dimension=1).
	 * This new {@link Rectangle} is called here "left" side of the rectangle.
	 * <br>
	 * An example:
	 * <br>
	 * Given: rectangle=(x=1,y=2,x-side-length=3,y-side-length=4), dimension=1, coordinateOfTheSeparatingLine=3.142
	 * <br>
	 * Returns: rectangle=(x=1,y=2,x-side-length=3,y-side-length=1.142)
	 * <p>
	 * see also: {@link SplitTree#devideRectangleAndReturnRightSide(Rectangle, int, double)}
	 * 
	 * 
	 * @param rectangle
	 * @param dimension =0 for the x-axis, =1 for the y-axis
	 * @param coordinateOfTheSeparatingLine x-value (if dimension=0) or y-value (if dimension=1) for the separating line,
	 * 			the line should cut the rectangle
	 * @return left side of the rectangle cut by the line (line is determined via dimension and coordinateOfTheSeparatingLine)
	 */
	private Rectangle devideRectangleAndReturnLeftSide(Rectangle rectangle, int dimension, double coordinateOfTheSeparatingLine){
		return new Rectangle(rectangle.getX(), rectangle.getY(),
				rectangle.getWidth()-(1-dimension)*(rectangle.getX()+rectangle.getWidth()-coordinateOfTheSeparatingLine),
				rectangle.getHeight()-(0+dimension)*(rectangle.getY()+rectangle.getHeight()-coordinateOfTheSeparatingLine));
	}
	
	/**
	 * Creates a new {@link Rectangle}.
	 * This {@link Rectangle} gets its location and size by taking the given rectangle and creating a
	 * new {@link Rectangle} that is only the values greater or equal to coordinateOfTheSeparatingLine
	 * in x-dimension (if dimension=0) or y-dimension (if dimension=1).
	 * This new {@link Rectangle} is called here "right" side of the rectangle.
	 * <br>
	 * An example:
	 * <br>
	 * Given: rectangle=(x=1,y=2,x-side-length=3,y-side-length=4), dimension=1, coordinateOfTheSeparatingLine=3.142
	 * <br>
	 * Returns: rectangle=(x=1,y=3.142,x-side-length=3,y-side-length=2.858)
	 * <p>
	 * see also: {@link SplitTree#devideRectangleAndReturnLeftSide(Rectangle, int, double)}
	 * 
	 * 
	 * @param rectangle
	 * @param dimension =0 for the x-axis, =1 for the y-axis
	 * @param coordinateOfTheSeparatingLine x-value (if dimension=0) or y-value (if dimension=1) for the separating line,
	 * 			the line should cut the rectangle
	 * @return right side of the rectangle cut by the line (line is determined via dimension and coordinateOfTheSeparatingLine)
	 */
	private Rectangle devideRectangleAndReturnRightSide(Rectangle rechteck, int dimension, double coordinateOfTheSeparatingLine){
		return new Rectangle(
				rechteck.getX()+(1-dimension)*(-rechteck.getX()+coordinateOfTheSeparatingLine),
				rechteck.getY()-(0+dimension)*(-rechteck.getY()+coordinateOfTheSeparatingLine),
				rechteck.getWidth()-(1-dimension)*(coordinateOfTheSeparatingLine-rechteck.getX()),
				rechteck.getHeight()-(0+dimension)*(coordinateOfTheSeparatingLine-rechteck.getY()));
	}
	
	/**
	 * To why this linking is done see {@link SplitTreeListElement#correspondingNodeInTheLinkedList}.
	 * 
	 * @param ls
	 */
	private void linkNodesToTheElementsInTheLinkedLists(LinkedList<SplitTreeListElement<V>>[] ls){
		for(int i=0; i<ls.length; i++){
			Node<SplitTreeListElement<V>> currentNode = ls[i].first;
			for(Iterator<SplitTreeListElement<V>> j=ls[i].iterator(); j.hasNext();){
				j.next().correspondingNodeInTheLinkedList = currentNode;
				currentNode = currentNode.next;
			}
		}
	}
	
	/**
	 * Checks if for all {@link SplitTreeListElement}s of the Lists ls
	 * the reference {@link SplitTreeListElement#correspondingNodeInTheLinkedList}
	 * is correct. Result as console output.
	 * 
	 * @param ls
	 */
	protected void checkLinking(LinkedList<SplitTreeListElement<V>>[] ls){
		System.out.println("=======Check Linking:=======");
		boolean valid = true;
		for(int i=0; i<ls.length; i++){
			for(SplitTreeListElement<V> s: ls[i]){
				System.out.print(s+" must be the same as "+s.correspondingNodeInTheLinkedList.item);
				if(s!=s.correspondingNodeInTheLinkedList.item){
					valid = false;
					System.out.println(" - but it is not!");
				}
				else{
					System.out.println(" - and it is");
				}
			}
		}
		if(valid){
			System.out.println("No error detected.");
		}
		else{
			System.out.println("Error detected!!!!!!!");
		}
		System.out.println("=====End of Linking check=====");
		System.out.println();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/*
	 * Methods regarding the tree structure
	 *
	 * The inheritance of Delegate-Tree was given up in favor of mor efficient saving structures.
	 * So these methods were added.
	 */
	public void setRoot(SplitTreeNode<V> stn){
		this.root = stn;
	}
	
	public SplitTreeNode<V> getRoot(){
		return this.root;
	}
	
	public boolean isLeaf(SplitTreeNode<V> stn){
		return stn.leftChild==null && stn.rightChild==null;
	}
	
	public Collection<SplitTreeNode<V>> getChildren(SplitTreeNode<V> stn){
		Collection<SplitTreeNode<V>> children = new ArrayList<SplitTreeNode<V>>(2);
		if(stn.leftChild!=null){
			children.add(stn.leftChild);
		}
		if(stn.leftChild!=null){
			children.add(stn.rightChild);
		}
		return children;
	}
	
	public void addChild(SplitTreeNode<V> parentNode, SplitTreeNode<V> childNode){
		if(parentNode.leftChild==null){
			parentNode.leftChild = childNode;
			childNode.parentNode = parentNode;
		}
		else if(parentNode.rightChild==null){
			parentNode.rightChild = childNode;
			childNode.parentNode = parentNode;
		}
		else{
			try {
				throw new Exception(parentNode+" has already 2 children! No further node can be added as child of it.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
