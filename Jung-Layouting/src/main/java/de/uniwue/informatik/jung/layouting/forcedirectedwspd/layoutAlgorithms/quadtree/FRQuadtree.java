package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree.QuadTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.quadtree.QuadTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;

/**
 * FR+Quadtree
 * <p>
 * Modifies the {@link FRLayout}-algorithm (respectively the {@link FRLayoutNoMapsNoFrame})
 * by approximating the repulsive forces with a {@link QuadTree}.
 * This computation follows the article "A hierarchical O(N log N) force-calculation algorithm"
 * from Josh Barnes and Piet Hut in Nature Vol. 324 4 from 1986.
 */
public class FRQuadtree<V, E> extends FRLayoutNoMapsNoFrame<V, E> {
	
	
	
    private double theta;
    protected QuadTree<V>[] quadtree;
	
	
	
	
    /*
     * Constructors
     */
	
	public FRQuadtree(Graph<V, E> g, double theta, Dimension d) {
		super(g, d);
		this.theta = theta;
	}

	
	/*
	 * Some Setters and Getters
	 */
	
	
    public double getTheta(){
    	return theta;
    }
	
	
	
	
    
    /*
	 * Update-Methods
	 */
	
	protected void recomputeQuadtree(){
		quadtree = new QuadTree[vertices.length];
		
		for(int i=0; i<vertices.length; i++){
			quadtree[i] = new QuadTree<V>(vertices[i]);
		}

	}
	
	
	
	/*
	 * Methods for calculation
	 */
	
    /*
	 * Change:
	 * Do not calculate for every possible tuple of vertices, instead use Quadtreecells and theta value
	 * to combine some vertices
     */
	@Override
	protected void calcRepulsion() {
		recomputeQuadtree();
		
		super.calcRepulsion();
    }
	
	@Override
	protected void calcRepulsion(VertexTriple<V> t, int indexConnectedComponent) {
		FRVertexData fvd = getFRData(t);
	    if(fvd == null)
	        return;
	    fvd.setLocation(0, 0);
	    	    
	    try {
		    calcRepulsion(t, quadtree[indexConnectedComponent], quadtree[indexConnectedComponent].getRoot());
		    
	    } catch(ConcurrentModificationException cme) {
	        calcRepulsion(t);
	    }
	};
	
	/**
	 * Recursive method that is needed by
	 * {@link FRQuadtree#calcRepulsion(VertexTriple, int)}
	 * 
	 * @param t
	 * {@link VertexTriple} of the currently considered vertex
	 * @param quadTreeOfItsComponent
	 * {@link QuadTree} of t
	 * @param qtn
	 * {@link QuadTreeNode} containing t
	 */
	private void calcRepulsion(VertexTriple<V> t, QuadTree<V> quadTreeOfItsComponent, QuadTreeNode<V> qtn){
		try {
            Point2D barycenter = quadTreeOfItsComponent.getBarycenter(qtn);
            
            double l = quadTreeOfItsComponent.getSquare(qtn).getSideLength(); //length of qtn
            double d = Point2D.distance(t.get2().getX(), t.get2().getY(), barycenter.getX(), barycenter.getY()); //distance barycenter(qtn)<->pV
            
            //if this is true then calc force between pV and the barycenter
            if(l/d < theta){
            
	            double xDelta = t.get2().getX() - barycenter.getX();
	            double yDelta = t.get2().getY() - barycenter.getY();
	
	            double deltaLength = Math.max(EPSILON, Math
	                    .sqrt((xDelta * xDelta) + (yDelta * yDelta)));
	
	            double force = (repulsion_constant * repulsion_constant) / deltaLength;
	
	            if (Double.isNaN(force)) { throw new RuntimeException(
	            "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }
	
	            t.get3().offset((xDelta / deltaLength) * force * quadTreeOfItsComponent.getTripleSet(qtn).size(),
	                    (yDelta / deltaLength) * force * quadTreeOfItsComponent.getTripleSet(qtn).size());
	            
            }
            //otherwise calc force between pV and the children of qtn
            else{
            	for(QuadTreeNode<V> child: quadTreeOfItsComponent.getChildren(qtn)){
            		calcRepulsion(t, quadTreeOfItsComponent, child);
            	}
            }
            
	    } catch(ConcurrentModificationException cme) {
	        calcRepulsion(t, quadTreeOfItsComponent, qtn);
	    }
	}
}
