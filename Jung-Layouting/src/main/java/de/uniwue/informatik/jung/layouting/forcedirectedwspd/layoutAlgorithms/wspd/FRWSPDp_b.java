package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * <u>FR</u>+<u>WSPD</u> with repulsive forces between <u>p</u>oint-<u>b</u>arycenter
 * <p>
 * 
 * Force-directed graph-drawing algorithm that works with a {@link WellSeparatedPairDecomposition}.
 * For the computation of the repulsive forces, the force acting between one concrete point (vertex-point, location of one vertex)
 * and the barycenter of one of the two parts of a well-separated pair of a {@link WellSeparatedPairDecomposition} is taken.
 * So repulsive forces between one point and a set of points are calculated (for every well-separated pair of the
 * {@link WellSeparatedPairDecomposition} between one point of one pair-part and the other pair-part).
 * In contrast to this in {@link FRWSPDb_b} repulsive forces are calculated between two set of points
 * (between the two parts of every well-sparated pair in the {@link WellSeparatedPairDecomposition}).
 * Thus the computation of the repulsive forces here in {@link FRWSPDp_b} takes a bit more time
 * and is a bit more accurate (a better approximation to the original FR-algorithm) than in {@link FRWSPDb_b}.
 * The asymptotical time bound of O(n log n) with n being the number of vertices in the drawn graph that is hold
 * in {@link FRWSPDb_b} can not be hold here.
 * Instead the needed time is in Theta(n^2) like in the original FR-algorithm (but it is still faster in practice).
 * The published FR+WSPD version is based on {@link FRWSPDb_b}.
 */
public class FRWSPDp_b<V, E> extends FRWSPD<V, E> {
	
	public FRWSPDp_b(Graph<V, E> g, double sFuerDieWSPD, Dimension d) {
		super(g, sFuerDieWSPD, d);
	}

	public FRWSPDp_b(Graph<V, E> g, double sFuerDieWSPD, Dimension d, long seed) {
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
    	
        
    	for(int i=0; i<wsPair.length; i++){
    		
        	for(VertexTriple<V> triple: splitTree[indexConnectedComponent].getTripleSetInTheSubtreeBelow(wsPair[i])){
        		//deltaX and deltaY as distance between point and barycenter of the other pair-part in x- and y-dimension
            	double xDelta = triple.get2().getX() - barycenter[1-i].getX();
            	double yDelta = triple.get2().getY() - barycenter[1-i].getY();

            	double distance = Point2D.distance(triple.get2().getX(), triple.get2().getY(), barycenter[1-i].getX(), barycenter[1-i].getY());
                double deltaLength = Math.max(EPSILON, distance);
                
                double force = (repulsion_constant * repulsion_constant) / deltaLength;                	
            	
                if (Double.isNaN(force)) { throw new RuntimeException(
                        "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }
        		
        		
        		double xOffset = (xDelta / deltaLength) * force * sizeOfThePointSet[1-i];
        		double yOffset = (yDelta / deltaLength) * force * sizeOfThePointSet[1-i];
        		
        		recordRepulsion(triple, xOffset, yOffset);
        	}
        }
	}

	@Override
	protected void propagateRepulsiveForcesToLeaves() {
		//do nothing; forces to every point were already recorded/stored after their computation
	}
}
