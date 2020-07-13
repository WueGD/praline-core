package de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree;

import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Counter;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Quadtree over a set of points.
 * To the construction of this quadtree see
 * "A hierarchical O(N log N) force-calculation algorithm"
 * from J. Barnes and P. Hut from 1986.
 * Can be done some time: Add a construction algorithm for a reduced quadtree to use it
 * in {@link FRQuadtree} (See e.g. "Truly Distribution-Independent Algorithms for the N-body Problem"
 * from Srinivas Aluru, G.M. Prabhu and John Gustafson).
 * Other than this quadtree a reduced quadtree can guarantee a construction time in O(n log n)
 * and bounds the max. depth (and size) of the tree.
 * <p>
 * Analog to the {@link SplitTree} also here the data structure was adapted to fit {@link FRLayoutNoMaps}.
 * Instead of points, {@link VertexTriple}s are handled.
 * For the {@link QuadTree} only the second element of it, this is the location-point is relevant
 * (Thus is still handled as a point set).
 * And, also analog to {@link SplitTree}, the inheritance from {@link DelegateTree} was given up
 * because of the same reason. (To speed up {@link FRLayoutNoMaps} a bit in practice)
 */
public class QuadTree<V>{
	
	private QuadTreeNode<V> root = null;
	
	/**
	 * Constructs a {@link QuadTree} over the passed tripleSet (better said
	 * the set of points gotten by the 2nd elements of each {@link VertexTriple})
	 */
	public QuadTree(Collection<VertexTriple<V>> tripleSet){
		/*
		 * Catch case that the empty set was passed
		 */
		if(tripleSet.size()==0){
			setRoot(new QuadTreeNode<V>(new Square(0,0,0)));
			return;
		}
		
		//Set root
		VertexTripleComparator<V> xComparator = new VertexTripleComparator<V>(true);
		VertexTripleComparator<V> yComparator = new VertexTripleComparator<V>(false);
		double minX = Collections.min(tripleSet, xComparator).get2().getX();
		double maxX = Collections.max(tripleSet, xComparator).get2().getX();
		double minY = Collections.min(tripleSet, yComparator).get2().getY();
		double maxY = Collections.max(tripleSet, yComparator).get2().getY();
		double sideLength = Math.max(maxX-minX, maxY-minY);
		/*
		 * May it happen that through bad rounding a point does not lie within this bounding square any more?
		 * As i can not say definitely I add a margin of 0.5% of the side length to the square to be safer.
		 * So in total the square gets 1% "longer".
		 * So the bounding square is not really the [smallest] bounding square any more.
		 */
		Square boundingSquare = new Square((minX+(maxX-minX)/2)-sideLength/2 - /*safety-buffer that is added*/sideLength*0.005
				, (minY+(maxY-minY)/2)-sideLength/2 - sideLength*0.005, sideLength + 0.01*sideLength);
		this.setRoot(new QuadTreeNode<V>(boundingSquare));
		
		
		for(VertexTriple<V> t: tripleSet){
			insert(t);
		}
	}
	
	/**
	 * Inserts t to the {@link QuadTree}
	 * 
	 * @param t
	 */
	public void insert(VertexTriple<V> t){
		//<Handling, if t.getLocation() is outside the outer square>
		//That can not happen by a constructor call because outer square is defined (incl. margin) that all points are within
		while(!getRoot().square.contains(t.get2())){
			//TODO handle that special case (can not occur by a constructor call)
			try {
				throw new Exception("Special case occured: Point lies outside the outer Square of the Quadtree. "
						+ "Not yet implemented.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//</Handling, if t.getLocation() is outside the outer square>
		
		insert(getRoot(), t);
	}
	
	private void insert(QuadTreeNode<V> qtn, VertexTriple<V> t){
		//adapt values in the nodes
		addTripleToTheListAndUpdateBarycenter(qtn, t);
		
		//Check if other QuadTreeNodes are affected recursively also by this insertion
		if(qtn.tripleSetBelow.size()>1){
			//check if children are already there, if not create them
			//then the other point already being there must be propagated down as well
			if(qtn.getNumberOfChildren()==0){
				qtn.setChildBL(new QuadTreeNode<V>(new Square(qtn.square.getX(),
						qtn.square.getY(), qtn.square.getSideLength()/2)));
				qtn.setChildTL(new QuadTreeNode<V>(new Square(qtn.square.getX(),
						qtn.square.getY()+qtn.square.getSideLength()/2, qtn.square.getSideLength()/2)));
				qtn.setChildBR(new QuadTreeNode<V>(new Square(qtn.square.getX()+qtn.square.getSideLength()/2,
						qtn.square.getY(), qtn.square.getSideLength()/2)));
				qtn.setChildTR(new QuadTreeNode<V>(new Square(qtn.square.getX()+qtn.square.getSideLength()/2,
						qtn.square.getY()+qtn.square.getSideLength()/2, qtn.square.getSideLength()/2)));
				
				//insert old point(triple) to correct child
				for(VertexTriple<V> old: qtn.tripleSetBelow){
					if(old!=t){ //old must not be the new one (which is to be inserted after this)
						insertPointToCorrectChild(old, qtn);
					}
				}
			}
			//insert new point(triple) to the correct child
			insertPointToCorrectChild(t, qtn);
		}
	}
	
	private void insertPointToCorrectChild(VertexTriple<V> point, QuadTreeNode<V> parentNode){
		Point2D center = parentNode.square.getCenter();
		if(point.get2().getX()<=center.getX()){
			if(point.get2().getY()<=center.getY()){
				insert(parentNode.getChildBL(), point);
			}
			else{
				insert(parentNode.getChildTL(), point);
			}
		}
		else{
			if(point.get2().getY()<=center.getY()){
				insert(parentNode.getChildBR(), point);
			}
			else{
				insert(parentNode.getChildTR(), point);
			}
		}
	}
	
	private void addTripleToTheListAndUpdateBarycenter(QuadTreeNode<V> qtn, VertexTriple<V> t){
		//update barycenter
		double sumX = qtn.barycenter.getX() * qtn.tripleSetBelow.size() + t.get2().getX();
		double sumY = qtn.barycenter.getY() * qtn.tripleSetBelow.size() + t.get2().getY();
		int numberOfPoints = qtn.tripleSetBelow.size() + 1;
		qtn.barycenter.setLocation(sumX/(double)numberOfPoints, sumY/(double)numberOfPoints);
		
		//add to list
		qtn.tripleSetBelow.add(t);
	}
	
	
	public Point2D getBarycenter(QuadTreeNode<V> qtn){
		return qtn.barycenter;
	}
	
	public Square getSquare(QuadTreeNode<V> qtn){
		return qtn.square;
	}
	
	public Collection<VertexTriple<V>> getTripleSet(QuadTreeNode<V> qtn){
		return qtn.tripleSetBelow;
	}
	
	
	/**
	 * The class {@link QuadTree} does not have {@link Tree} or {@link DelegateTree} or {@link Graph} as superclass.
	 * As it is in principle still a tree one can call this method to get the corresponding {@link DelegateTree} to
	 * this {@link QuadTree}.
	 * This is useful for using methods/applications expacting a JUNG-tree object.
	 * The returned {@link DelegateTree} is no deep copy of this {@link QuadTree}.
	 * 
	 * @return
	 */
	public DelegateTree<QuadTreeNode<V>, Integer> createDelegateTreeToThisQuadTree(){
		DelegateTree<QuadTreeNode<V>, Integer> correspondingDelegateTree =
				new DelegateTree<QuadTreeNode<V>, Integer>();
		correspondingDelegateTree.setRoot(this.getRoot());
		
		Counter edgeCounter = new Counter();
		addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, this.getRoot(), edgeCounter);
		
		return correspondingDelegateTree;
	}
	
	private void addChildrenToCorrespondingDelegateTree(DelegateTree<QuadTreeNode<V>, Integer> correspondingDelegateTree,
			QuadTreeNode<V> currentNode, Counter edgeCounter){
				
		if(currentNode.getChildTL() != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.getChildTL());
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.getChildTL(), edgeCounter);
		}		
		if(currentNode.getChildTR() != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.getChildTR());
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.getChildTR(), edgeCounter);
		}		
		if(currentNode.getChildBL() != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.getChildBL());
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.getChildBL(), edgeCounter);
		}		
		if(currentNode.getChildBR() != null){
			correspondingDelegateTree.addChild(edgeCounter.get(), currentNode, currentNode.getChildBR());
			edgeCounter.incrementBy1();
			addChildrenToCorrespondingDelegateTree(correspondingDelegateTree, currentNode.getChildBR(), edgeCounter);
		}
	}
	
	
	
	
	/*
	 * Methods regarding the tree structure
	 * 
	 * The inheritance of Delegate-Tree was given up in favor of mor efficient saving structures.
	 * So these methods were added.
	 */
	public void setRoot(QuadTreeNode<V> qtn){
		this.root = qtn;
	}
	
	public QuadTreeNode<V> getRoot(){
		return this.root;
	}
	
	public boolean isLeaf(QuadTreeNode<V> qtn){
		return qtn.getNumberOfChildren()==0;
	}
	
	public Collection<QuadTreeNode<V>> getChildren(QuadTreeNode<V> qtn){
		Collection<QuadTreeNode<V>> children = new ArrayList<QuadTreeNode<V>>(2);
		if(qtn.getChildTL()!=null){
			children.add(qtn.getChildTL());
		}
		if(qtn.getChildTR()!=null){
			children.add(qtn.getChildTR());
		}
		if(qtn.getChildBL()!=null){
			children.add(qtn.getChildBL());
		}
		if(qtn.getChildBR()!=null){
			children.add(qtn.getChildBR());
		}
		return children;
	}
}
