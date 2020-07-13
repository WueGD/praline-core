package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.graph.util.Pair;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Passed {@link Layout} is interpreted as a straight-line drawing.
 * Values regarding the angle are in radian measure.
 */
public class QualityTesterForLayout<V, E> {
	
	private Layout<V, E> layout;

	/*
	 * Testvariables
	 * 
	 * -1 means not calculated yet
	 */
	//in the following line statistics.values.length-1 is used because standard deviation is not saved explicitly but implicitly in the variance
	private double [][] qualityValues = new double[QualityCriterion.values().length][Statistic.values().length-1];
	/**
	 * value in radian measure (between two edges incident to the same vertex in a straight-line drawing)
	 */
	private double smallestAngle = -1;
	/**
	 * number of edge crossings in a straight line drawing
	 */
	private int numberOfCrossings = -1;
	
	
	
	
	
	
	public QualityTesterForLayout(Layout<V, E> layout){
		this.layout = layout;
		
		//init as uncalculated
		for(QualityCriterion qc: QualityCriterion.values()){
			for(Statistic st: Statistic.values()){
				//following line because standard deviation is not saved explicitly but implicitly in the variance
				if(st!=Statistic.STANDARD_DEVIATION){
					qualityValues[qc.ordinal()][st.ordinal()] = -1;
				}
			}
		}
	}

	public Layout<V, E> getLayout() {
		return layout;
	}
	
	/*
	 * ===== *
	 * Tests *
	 * ===== *
	 */
	
	/*
	 * callable test-methods (with computation of values not computed yet)
	 */
	public double get(QualityCriterion qualityCriterion, Statistic statistic){

		//following line because standard deviation is not saved explicitly but implicitly in the variance
		if(statistic==Statistic.STANDARD_DEVIATION){
			return Math.sqrt(get(qualityCriterion, Statistic.VARIANCE));
		}
		
		if(qualityValues[qualityCriterion.ordinal()][statistic.ordinal()]==-1){
			if(qualityCriterion==QualityCriterion.EDGE_LENGTH){
				computeAllValuesToEdgeLength();
			}
			else if(qualityCriterion==QualityCriterion.DISTANCE_VERTEX_VERTEX){
				computeAllValuesToVertexDistance();
			}
			else if(qualityCriterion==QualityCriterion.DISTANCE_VERTEX_NOT_INCIDENT_EDGE){
				computeAllValuesToVertexToNotIncidentEdgeDistance();
			}
			else if(qualityCriterion==QualityCriterion.RATIO_GEOMETRIC_VERTEX_DISTANCE_TO_SHORTEST_PATH){
				computeAllValuesToRatioGeometricVertexDistanceToShortestPath();
			}
			else if(qualityCriterion==QualityCriterion.DEVIATION_FROM_THE_OPTIMAL_ANGLE){
				computeAllValuesToAngularResolution();
			}
		}
		return qualityValues[qualityCriterion.ordinal()][statistic.ordinal()];
	}

	public double getSmallestAngle() {
		if(smallestAngle==-1){
			computeAllValuesToAngularResolution();
		}
		return smallestAngle;
	}
	
	public int getNumberOfCrossings() {
		if(numberOfCrossings==-1){
			calculateNumberOfEdgeCrossings();
		}
		return numberOfCrossings;
	}
	
	/*
	 * Methods for intern computation
	 */
	private void computeAllValuesToEdgeLength(){
		LinkedList<Double> allDistances = new LinkedList<Double>();
		//1. calc all distances and store them
		for(E e: layout.getGraph().getEdges()){
			Pair<V> endpoints = layout.getGraph().getEndpoints(e);
			if(endpoints.getFirst()==endpoints.getSecond()) continue;
			Point2D v1 = layout.apply(endpoints.getFirst());
			Point2D v2 = layout.apply(endpoints.getSecond());
			double distance = Point2D.distance(v1.getX(), v1.getY(), v2.getX(), v2.getY());
			allDistances.add(distance);
		}
		//2. calc the statistical values out of them
		for(Statistic st: Statistic.values()){
			//following line because standard deviation is not saved explicitly but implicitly in the variance
			if(st!=Statistic.STANDARD_DEVIATION){
				qualityValues[QualityCriterion.EDGE_LENGTH.ordinal()][st.ordinal()] =
						Statistic.computeStatistic(st, allDistances);
			}
		}
	}
	
	private void computeAllValuesToVertexDistance(){
		LinkedList<Double> allDistances = new LinkedList<Double>();
		//1. calc all distances and store them (indices i and j to not calc the same distance again or to itsself)
		int i = 0;
		for(V v1: layout.getGraph().getVertices()){
			Point2D p1 = layout.apply(v1);
			int j = 0;
			for(V v2: layout.getGraph().getVertices()){
				if(j>i){
					Point2D p2 = layout.apply(v2);
					double distance = Point2D.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
					allDistances.add(distance);
				}
				j++;
			}
			i++;
		}
		//2. calc the statistical values out of them
		for(Statistic st: Statistic.values()){
			//following line because standard deviation is not saved explicitly but implicitly in the variance
			if(st!=Statistic.STANDARD_DEVIATION){
				qualityValues[QualityCriterion.DISTANCE_VERTEX_VERTEX.ordinal()][st.ordinal()] =
						Statistic.computeStatistic(st, allDistances);
			}
		}
	}
	
	private void computeAllValuesToVertexToNotIncidentEdgeDistance(){
		LinkedList<Double> allDistances = new LinkedList<Double>();
		//1. calc all distances and store them
		for(V v: layout.getGraph().getVertices()){
			Point2D pV = layout.apply(v);
			double xV = pV.getX();
			double yV = pV.getY();
			for(E e: layout.getGraph().getEdges()){
				//skip if edge is incident
				if(layout.getGraph().getEndpoints(e).contains(v)) continue;
				//otherwise it is not incident and thus relevant
				Point2D pE1 = layout.apply(layout.getGraph().getEndpoints(e).getFirst());
				double x1 = pE1.getX();
				double y1 = pE1.getY();
				Point2D pE2 = layout.apply(layout.getGraph().getEndpoints(e).getSecond());
				double x2 = pE2.getX();
				double y2 = pE2.getY();
				
				/*
				 * To calculation:
				 * 
				 * At first the foot of the perpendicular from the point v to the line, on which e lies, is calculated.
				 * Thereby k is the factor, how often one must take the normal vector (y1-y2, x2-x1) starting at
				 * the point pV to reach the line on which the edge lies.
				 */
				double k = ((x2-x1)*(y1-yV)-(y2-y1)*(x1-xV)) / (Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2));
				Point2D footOfThePerpendicular = new Point2D.Double(xV+k*(y1-y2), yV+k*(x2-x1));
				/*
				 * Now check if the foot of the perpendicular lies on the edge (on the line segment of that straight-line edge).
				 * This is done by taking the vector pE1 to pE2 t times starting at pE1.
				 * As the point on lies on the this line I must reach it this way.
				 * It is enough to consider only x- or y-coordinates (if different)
				 */
				double t;
				if(x2!=x1){
					t = (footOfThePerpendicular.getX()-x1)/(x2-x1);
				}
				else{ //x-coords are the same -> y
					t = (footOfThePerpendicular.getY()-y1)/(y2-y1);
				}
				//( (0<=t<=1) <=> foot of the perpendicular lies on the edge )
				//=> the foot of the perpendicular lies on the edge is the candidate for the list
				if(0<=t && t<=1){
					allDistances.add(Point2D.distance(xV, yV, footOfThePerpendicular.getX(), footOfThePerpendicular.getY()));
				}
				//otherwise the shortest distance from pV to edge e is either from pV to pE1 or from pV to pE2
				else{
					allDistances.add(Math.min(Point2D.distance(xV, yV, x1, y1), Point2D.distance(xV, yV, x2, y2)));
				}
			}
		}
		//2. calc the statistical values out of them
		for(Statistic st: Statistic.values()){
			//following line because standard deviation is not saved explicitly but implicitly in the variance
			if(st!=Statistic.STANDARD_DEVIATION){
				qualityValues[QualityCriterion.DISTANCE_VERTEX_NOT_INCIDENT_EDGE.ordinal()][st.ordinal()] =
						Statistic.computeStatistic(st, allDistances);
			}
		}
	}
	
	private void computeAllValuesToRatioGeometricVertexDistanceToShortestPath(){
		LinkedList<Double> allRatios = new LinkedList<Double>();
		DijkstraDistance<V, E> shortestPathCalculater = new DijkstraDistance<V, E>(layout.getGraph());
		//1. calc all ratios between pairs of vertices them (indices i and j to not consider the same pair again or itsself as pair)
		int i = 0;
		for(V v1: layout.getGraph().getVertices()){
			Point2D p1 = layout.apply(v1);
			int j = 0;
			for(V v2: layout.getGraph().getVertices()){
				if(j>i){
					Point2D p2 = layout.apply(v2);
					double geomDistance = Point2D.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
					Number shortestPath = shortestPathCalculater.getDistance(v1, v2);
					/*
					 * It can happen that there are graphs treated that consist out of more than one connected
					 * component.
					 * Then there are vertices that can not reach each other, then null
					 * is return by shortestPathCalculater.getDistance(v1, v2).
					 * Such pairs are ignored and not taken in account.
					 */
					if(shortestPath==null) continue;
					
					allRatios.add(geomDistance/shortestPath.doubleValue());
				}
				j++;
			}
			i++;
		}
		//2. calc the statistical values out of them
		for(Statistic st: Statistic.values()){
			//following line because standard deviation is not saved explicitly but implicitly in the variance
			if(st!=Statistic.STANDARD_DEVIATION){
				qualityValues[QualityCriterion.RATIO_GEOMETRIC_VERTEX_DISTANCE_TO_SHORTEST_PATH.ordinal()][st.ordinal()] =
						Statistic.computeStatistic(st, allRatios);
			}
		}
	}
	
	private void computeAllValuesToAngularResolution(){
		LinkedList<Double> allDistancesFromTheOptimalAngleSquared = new LinkedList<Double>();
		double smallestAngle = 2*Math.PI;
		//find all angles and differences and store them (error squares, see javadoc of QualityCriterion.DEVIATION_FROM_THE_OPTIMAL_ANGLE)
		for(V v: layout.getGraph().getVertices()){
			//catch vertex with no neighbor
			if(layout.getGraph().degree(v)==0){
				continue;
			}
			LinkedList<Double> neighborVerticesAngles = new LinkedList<Double>();
			for(V vNeighbor: layout.getGraph().getNeighbors(v)){
				if(vNeighbor!=v){ //Must be done because getNeighbors(v) may include v itsself
					double angleToV = Math.atan2(layout.apply(vNeighbor).getY()-layout.apply(v).getY(),
							layout.apply(vNeighbor).getX()-layout.apply(v).getX());
					neighborVerticesAngles.add(angleToV);
				}
			}
			Collections.sort(neighborVerticesAngles);
			
			if(neighborVerticesAngles.size()==0) continue;
			if(neighborVerticesAngles.size()==1) continue;
			/*
			 * Annotation to "if(neighborVerticesAngles.size()==1) continue;":
			 * 
			 * This is a trivial case: only one neighbor and thus
			 * only one angle with always 2PI.
			 * Now is the question: Take that trivial angle out of calculation
			 * or let it in?
			 * I chose to pick them out.
			 * This can be changed.
			 */
			
			double optimalAngle = 2.0*Math.PI/neighborVerticesAngles.size();
						
			//for the first angle the previous angle is the last one, but to overcome the Pi and -Pi difference subtract 2Pi from it
			double previousPositionAngle = neighborVerticesAngles.getLast()-2*Math.PI;
			for(double currentPositionAngle: neighborVerticesAngles){
				double angleBeetweenVertices = currentPositionAngle - previousPositionAngle; //is positive because current > prev (sorted list)
				if(angleBeetweenVertices<smallestAngle){
					smallestAngle = angleBeetweenVertices;
				}
				//squared normalized difference
				double differenceToOptimalAngleSquared = Math.pow((angleBeetweenVertices-optimalAngle)/optimalAngle, 2);
				allDistancesFromTheOptimalAngleSquared.add(differenceToOptimalAngleSquared);
				previousPositionAngle = currentPositionAngle; //assign to current
			}
		}
		this.smallestAngle = smallestAngle;

		//2. calc the statistical values out of them
		for(Statistic st: Statistic.values()){
			//following line because standard deviation is not saved explicitly but implicitly in the variance
			if(st!=Statistic.STANDARD_DEVIATION){
				qualityValues[QualityCriterion.DEVIATION_FROM_THE_OPTIMAL_ANGLE.ordinal()][st.ordinal()] =
						Statistic.computeStatistic(st, allDistancesFromTheOptimalAngleSquared);
			}
		}
	}
	
	
	public void calculateNumberOfEdgeCrossings(){
		numberOfCrossings = 0; //to count correct set from -1 to 0
		//1. find all crossings between pairs of edges (indices i and j to not consider the same pair again or itsself as pair)
		int i = 0;
		for(E e1: layout.getGraph().getEdges()){
			V v1_1 = layout.getGraph().getEndpoints(e1).getFirst();
			V v1_2 = layout.getGraph().getEndpoints(e1).getSecond();
			Point2D p1_1 = layout.apply(v1_1);
			Point2D p1_2 = layout.apply(v1_2);
			
			int j=0;
			for(E e2: layout.getGraph().getEdges()){
				
				if(j>i){
					V v2_1 = layout.getGraph().getEndpoints(e2).getFirst();
					V v2_2 = layout.getGraph().getEndpoints(e2).getSecond();
					
					Point2D p2_1 = layout.apply(v2_1);
					Point2D p2_2 = layout.apply(v2_2);
					
					/*
					 * if the two edges have a common endpoint and do not lie on each other
					 * then skip (no crossing being considered) -> catch that case.
					 * But if those two neigboured edges are lying on each other (one is contained completely by the other)
					 * then all 4 points lie on the same line.
					 * But then also check that these points go in the same direction from the common endpoint,
					 * otherwise there is no crossing counted
					 */
					if(		   v1_1==v2_1 && ( ccw(p1_1, p1_2, p2_2)!=0
								|| ( Math.signum(p1_2.getX()-p1_1.getX())-Math.signum(p2_2.getX()-p1_1.getX())==0
								  && Math.signum(p1_2.getY()-p1_1.getY())-Math.signum(p2_2.getY()-p1_1.getY())==0 ) )
								  
							|| v1_1==v2_2 && ( ccw(p1_1, p1_2, p2_1)!=0
								|| ( Math.signum(p1_2.getX()-p1_1.getX())-Math.signum(p2_1.getX()-p1_1.getX())==0
								  && Math.signum(p1_2.getY()-p1_1.getY())-Math.signum(p2_1.getY()-p1_1.getY())==0 ) )
								  
							|| v1_2==v2_1 && ( ccw(p1_1, p1_2, p2_2)!=0
								|| ( Math.signum(p1_1.getX()-p1_2.getX())-Math.signum(p2_2.getX()-p1_2.getX())==0
								  && Math.signum(p1_1.getY()-p1_2.getY())-Math.signum(p2_2.getY()-p1_2.getY())==0 ) )
								  
							|| v1_2==v2_2 && ( ccw(p1_1, p1_2, p2_1)!=0
								|| ( Math.signum(p1_1.getX()-p1_2.getX())-Math.signum(p2_1.getX()-p1_2.getX())==0
								  && Math.signum(p1_1.getY()-p1_2.getY())-Math.signum(p2_1.getY()-p1_2.getY())==0 ) )
							){
						continue;
					}
					
					/*
					 * That way it is checked if 2 line segments intersect.
					 * This method is taken from
					 * http://www.imn.htwk-leipzig.de/~medocpro/buecher/sedge1/k24t3.html
					 */
					if(ccw(p1_1,p1_2,p2_1)*ccw(p1_1,p1_2,p2_2)<=0 && (ccw(p2_1,p2_2,p1_1)*ccw(p2_1,p2_2,p1_2)<=0)){
						numberOfCrossings++;
					}
				}
				j++;
			}
			i++;
		}
	}
	
	/**
	 * Function taken from:
	 * http://www.imn.htwk-leipzig.de/~medocpro/buecher/sedge1/k24t3.html
	 * 
	 * @param p0
	 * @param p1
	 * @param p2
	 * @return
	 * 		It is returned if one is going clock-wise when travelling from p0 to p1 to p2 and back to p0
	 * 		(return -1) or counter-clock-wise (return 1) or p0 and p1 and p2 lie on one line (return 0).
	 */
	private int ccw(Point2D p0, Point2D p1, Point2D p2) {
		double dx1,dx2,dy1,dy2;
		dx1=p1.getX()-p0.getX();
		dy1=p1.getY()-p0.getY();
		dx2=p2.getX()-p0.getX();
		dy2=p2.getY()-p0.getY();
		if(dx1*dy2>dy1*dx2) return 1;
		if(dx1*dy2<dy1*dx2) return -1;
//		if(dx1*dy2==dy1*dx2) { //commented out because this is the only remaining possibility
			if((dx1*dx2<0) || (dy1*dy2<0)) return-1;
			if((dx1*dx1+dy1*dy1)>=(dx2*dx2+dy2*dy2)) return 0;
			else return 1;
//		}
	}
}
