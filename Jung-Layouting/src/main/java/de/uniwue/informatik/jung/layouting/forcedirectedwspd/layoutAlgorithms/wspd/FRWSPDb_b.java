package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * <u>FR</u>+<u>WSPD</u> with repulsive forces between <u>b</u>arycenter-<u>b</u>arycenter
 * <p>
 * 
 * Force-directed graph-drawing algorithm that works with a {@link WellSeparatedPairDecomposition}.
 * For the computation of the repulsive forces, the force acting between the two barycenters of the
 * two parts of each well-separted pair of the {@link WellSeparatedPairDecomposition} is calculated.
 * So repulsive forces between two sets of points are calculated.
 * In contrast to this in {@link FRWSPDp_b} repulsive forces are calculated between one point and a set of points
 * are calculated (for every well-separated pair of the {@link WellSeparatedPairDecomposition} between one point
 * of one pair-part and the other pair-part).
 * Thus the computation of the repulsive forces here in {@link FRWSPDb_b} takes a bit less time
 * and is a bit less accurate (to the original FR-algorithm) than in {@link FRWSPDp_b}.
 * The asymptotical time bound of O(n log n) with n being the number of vertices in the drawn graph that is not hold
 * in {@link FRWSPDb_b} can be hold here.
 * The time needed in {@link FRWSPDp_b} is in Theta(n^2) like in the original FR-algorithm (but it is still faster in practice).
 * The published FR+WSPD version is based on {@link FRWSPDb_b}.
 */
public class FRWSPDb_b<V, E> extends FRWSPD<V, E> {
	
	public FRWSPDb_b(Graph<V, E> g, double sFuerDieWSPD, Dimension d) {
		super(g, sFuerDieWSPD, d);
	}

	public FRWSPDb_b(Graph<V, E> g, double sFuerDieWSPD, Dimension d, long seed) {
		super(g, sFuerDieWSPD, d, seed);
	}
	
	
	@Override
	protected void calcAndRecordRepulsiveForces(SplitTreeNode<V>[] wsPair, int indexConnectedComponent){
		Point2D[] barycenter = new Point2D[2];
    	int[] sizeOfThePointSet = new int[2];
    	for(int i=0; i<wsPair.length; i++){
        	barycenter[i] = splitTree[indexConnectedComponent].getBarycenter(wsPair[i]);
        	sizeOfThePointSet[i] = splitTree[indexConnectedComponent].getSizeOfTheTripleSetInTheSubtreeBelow(wsPair[i]);
    	}
		//deltaX and deltaY as distance between the 2 barycenters in x- and y-dimension
    	double xDelta = barycenter[0].getX() - barycenter[1].getX();
    	double yDelta = barycenter[0].getY() - barycenter[1].getY();

    	double distance = Point2D.distance(barycenter[0].getX(), barycenter[0].getY(), barycenter[1].getX(), barycenter[1].getY());
        double deltaLength = Math.max(EPSILON, distance);
        
        double force = (repulsion_constant * repulsion_constant) / deltaLength;                	
    	
        if (Double.isNaN(force)) { throw new RuntimeException(
                "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }
        
    	for(int i=0; i<wsPair.length; i++){
    		double xOffset = (xDelta / deltaLength) * force * sizeOfThePointSet[1-i];
    		double yOffset = (yDelta / deltaLength) * force * sizeOfThePointSet[1-i];
    		splitTree[indexConnectedComponent].addRepulsiveForce(wsPair[i], xOffset, yOffset);
    		
        	xDelta = -xDelta; //xDelta and yDelta must be multiplied by -1 when i=1 (2nd pair-part)
        	yDelta = -yDelta; //in order to switch signs (both forces acting away from the other, thus opposite direction)
        }
	}

	@Override
	protected void propagateRepulsiveForcesToLeaves() {
		for(int i=0; i<splitTree.length; i++){
			propagateRecordedRepulsiveForceInTheSplitTree(splitTree[i].getRoot(), new Point2D.Double(0, 0), i);
		}
	}
}
