package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Randomness;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TripleSetGenerator {
	
	/**
	 * Generates a {@link VertexTriple} set of type <String, Point2D, FRVertexData>.
	 * The second element of each {@link VertexTriple} (these are points) are distributed randomly across the
	 * rectangle defined by the input-parameters.
	 * 
	 * @param minNumberOfTriples
	 * @param maxNumberOfTriples
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 * @param onlyIntegralValues
	 * @return
	 */
	public static Collection<VertexTriple<String>> generateRandomlyDistributedTripleSet(int minNumberOfTriples,
			int maxNumberOfTriples, double minX, double maxX, double minY, double maxY, boolean onlyIntegralValues){
		int numberOfTriplesToBeGenerated = (int) (minNumberOfTriples + Randomness.random.nextDouble()*(maxNumberOfTriples-minNumberOfTriples+1));
		ArrayList<VertexTriple<String>> tripleSet = new ArrayList<VertexTriple<String>>();
		//those int-Values are only needed if onlyIntegralValues==true
		int minXint = (int)Math.ceil(minX);
		int maxXint = (int)maxX;
		int minYint = (int)Math.ceil(minY);
		int maxYint = (int)maxY;
		//here the random points are generated
		for(int i=0; i<numberOfTriplesToBeGenerated; i++){
			Point2D newPoint;
			if(onlyIntegralValues){
				newPoint = new Point2D.Double((double)((int)(minXint+Randomness.random.nextDouble()*(maxXint-minXint+1))),
						(double)((int)(minYint+Randomness.random.nextDouble()*(maxYint-minYint+1))));
			}
			else{
				newPoint = new Point2D.Double(minX+Randomness.random.nextDouble()*(maxX-minX),
						minY+Randomness.random.nextDouble()*(maxY-minY));
			}
			tripleSet.add(new VertexTriple<String>("p"+i, newPoint, new FRLayout.FRVertexData()));
		}
		return tripleSet;
	}
	
	/**
	 * In principle the same as {@link TripleSetGenerator#generateRandomlyDistributedTripleSet(int, int, double, double, double, double, boolean)}.
	 * Only difference is that there is no guarantee that there is a point on the boundary (minX, maxX and so on).
	 * Here it is guaranteed that at least one point is on the upper, lower, right, left border of the area.
	 * If all points have the same x/y coordinate only the minX/minY border have points on it.
	 * 
	 * @param minNumberOfTriples
	 * @param maxNumberOfTriples
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 * @param onlyIntegralValues
	 * @return
	 */
	public static Collection<VertexTriple<String>> generateRandomlyDistributedTripleSetWithOuterPointsOnBoundary(
			int minNumberOfTriples, int maxNumberOfTriples, double minX, double maxX, double minY, double maxY, boolean onlyIntegralValues){
		//Generate point set over interval [0;1[ for x and y
		Collection<VertexTriple<String>> tripleSet =
				generateRandomlyDistributedTripleSet(minNumberOfTriples, maxNumberOfTriples, 0, 1, 0, 1, false);
		double minXPreliminary = 1;
		double maxXPreliminary = 0;
		double minYPreliminary = 1;
		double maxYPreliminary = 0;
		for(VertexTriple<String> t: tripleSet){
			if(t.get2().getX()<minXPreliminary){
				minXPreliminary = t.get2().getX();
			}
			if(t.get2().getX()>maxXPreliminary){
				maxXPreliminary = t.get2().getX();
			}
			if(t.get2().getY()<minYPreliminary){
				minYPreliminary = t.get2().getY();
			}
			if(t.get2().getY()>maxYPreliminary){
				maxYPreliminary = t.get2().getY();
			}
		}
		//those int-Values are only needed if onlyIntegralValues==true
		int minXint = (int)Math.ceil(minX);
		int maxXint = (int)maxX;
		int minYint = (int)Math.ceil(minY);
		int maxYint = (int)maxY;
		//adapt coordinates
		for(VertexTriple<String> t: tripleSet){
			if(onlyIntegralValues){
				t.get2().setLocation(
						(double)((int)((t.get2().getX()-minXPreliminary)/(maxXPreliminary-minXPreliminary)*(maxXint-minXint)+minXint)),
						(double)((int)((t.get2().getY()-minYPreliminary)/(maxYPreliminary-minYPreliminary)*(maxYint-minYint)+minYint)));				
			}
			else{
				t.get2().setLocation(
						(t.get2().getX()-minXPreliminary)/(maxXPreliminary-minXPreliminary)*(maxX-minX)+minX,
						(t.get2().getY()-minYPreliminary)/(maxYPreliminary-minYPreliminary)*(maxY-minY)+minY);
			}
		}
		return tripleSet;
	}
	
	/**
	 * Creates point set in grid structure on lines parallel to x/y-axis.
	 * Grid of size numberOfPointsInXDirection x numberOfPointsInYDirection is used.
	 * 
	 * @param numberOfPointsInXDirection
	 * @param numberOfPointsInYDirection
	 * @param minX
	 * @param minY
	 * @param distanceX
	 * @param distanceY
	 * @param shuffleOrder
	 * @return
	 */
	public static Collection<VertexTriple<String>> generateTripleSetOfGridStructure(int numberOfPointsInXDirection,
			int numberOfPointsInYDirection, double minX, double minY, double distanceX, double distanceY, boolean shuffleOrder){
		ArrayList<VertexTriple<String>> tripleSet = new ArrayList<VertexTriple<String>>();
		//create grid
		for(int i=0; i<numberOfPointsInXDirection; i++){
			for(int j=0; j<numberOfPointsInYDirection; j++){
				tripleSet.add(new VertexTriple<String>("p"+(i*numberOfPointsInYDirection+j),
						new Point2D.Double(minX+i*distanceX, minY+j*distanceY), new FRLayout.FRVertexData()));
			}
		}
		if(shuffleOrder){
			Collections.shuffle(tripleSet, Randomness.random);
		}
		return tripleSet;
	}
}
