package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

/**
 * FR+Grid
 * <p>
 * Grid-variant from the paper from Fruchterman and Reingold (1991).
 * To the way the calculation works the javadocs and comments inside this class.
 * As I have treated every component individually in {@link FRLayoutNoMaps},
 * here there is a own k and a own grid for every component.
 */
public class FRGrid<V,E> extends FRLayoutNoMapsNoFrame<V,E> {
	
	/**
	 * The k from the paper from Fruchterman and Reingold.
	 * Here for every component individually (same scheme as in its super-class {@link FRLayoutNoMapsNoFrame}).
	 * 
	 * Is calculated as described there: sqrt(totalWidth*totalHeight / |V|).
	 * 
	 * In this class, where no frame is used (as it as {@link FRLayoutNoMapsNoFrame} as super-class),
	 * the totalWidth and totalHeight is defined by the highest/lowest x-/y-coordinate being there in one iteration.
	 */
	private double[] k;
	
	/**
	 * Grid from the FR-paper.
	 * All {@link VertexTriple}s are stored acc. to their position in the last iteration.
	 * 
	 * For every connected component its own grid after the scheme
	 * grid[indexConnectedComponent][numberOfGridSquareInXDirection][numberOfGridSquareInYDirection]
	 */
	private List<VertexTriple<V>>[][][] grid;
	
	
	/*
     * Constructors
	 */
	public FRGrid(Graph<V, E> graph, Dimension d) {
		super(graph, d);
		
		//for every component an own one
		k = new double[vertices.length];
		grid = new List[vertices.length][0][0];
		computeGrid();
	}
	
	
	/*
	 * Overwritten Methods
	 */
	
	@Override
	protected void calcPositions() {
		super.calcPositions();
		
		computeGrid();
	}
	
	protected void computeGrid(){
		//find min and max to determine the area and size of the grid
		for(int i=0; i<vertices.length; i++){
			double xMin = Double.POSITIVE_INFINITY;
			double xMax = Double.NEGATIVE_INFINITY;
			double yMin = Double.POSITIVE_INFINITY;
			double yMax = Double.NEGATIVE_INFINITY;
			for(VertexTriple<V> t: vertices[i]){
				if(t.get2().getX()<xMin){
					xMin = t.get2().getX();
				}
				if(t.get2().getX()>xMax){
					xMax = t.get2().getX();
				}
				if(t.get2().getY()<yMin){
					yMin = t.get2().getY();
				}
				if(t.get2().getY()>yMax){
					yMax = t.get2().getY();
				}
			}
			double width = xMax - xMin;
			double height = yMax - yMin;
			
			/*
			 * Catch case that width or height == 0.
			 * Then take just a small value for width and height (here arbitrarily defined via
			 * minDistanceToTheDrawingAreaOfTheNexComponent from FRLayoutNoMapsNoFrame)
			 */
			if(width==0){
				width = Math.max(1, minDistanceToTheDrawingAreaOfTheNextComponent/2);
			}
			if(height==0){
				height = Math.max(1, minDistanceToTheDrawingAreaOfTheNextComponent/2);
			}
			
			k[i] = Math.sqrt(width*height/(double)numberOfVertices);
			
			/*
			 * Use Math.floor +1, so that even if the value has been already a integral value, it is rounded to
			 * the next integer value anyways.
			 * In practice this is especially needed when the value 0 is "rounded up" to 1 as there would be an
			 * array of length 0 being initialized 
			 */
			grid[i] = new List[(int)Math.floor(width/(2*k[i])) + 1][(int)Math.floor(height/(2*k[i])) + 1];
			
			//Go through all vertices and sort them in, in the grid-square they belong in
			for(VertexTriple<V> t: vertices[i]){
				int xIndex = (int) Math.floor((t.get2().getX()-xMin)/(2*k[i]));
				int yIndex = (int) Math.floor((t.get2().getY()-yMin)/(2*k[i]));
				
				if(grid[i][xIndex][yIndex]==null){
					grid[i][xIndex][yIndex] = new LinkedList<VertexTriple<V>>();
				}
				
				grid[i][xIndex][yIndex].add(t);
			}
		}
	}
	
	@Override
	protected void calcRepulsion() {
		if(currentIteration%intervalForLeavingOutCalculationOfRepulsiveForces!=0){ //In some iteartions no repulsive forces are computed
			//Individually done for every component, see calcRepulsion(t)
	        for(int i=0; i<vertices.length; i++){
//	        	if(converged[i]==true) continue;
		        while(true) {
		            try {
		            	for(int j=0; j<grid[i].length; j++){
		            		for(int k=0; k<grid[i][j].length; k++){
		            			if(grid[i][j][k]==null) continue;
		            			
		            			for(VertexTriple<V> t: grid[i][j][k]){
				                    calcRepulsion(t, i, j, k);
		            			}
		            		}
		            	}
		                break;
		            } catch(ConcurrentModificationException cme) {}
		        }
	        }
        }
	}
	
	protected void calcRepulsion(VertexTriple<V> v1, int indexConnectedComponent, int xGridIndex, int yGridIndex) {
        FRVertexData fvd1 = getFRData(v1);
        if(fvd1 == null)
            return;
        fvd1.setLocation(0, 0);

        try {
        	/*
        	 * The 8 grid-squares that are around the considered grid-square (where v1 is in) and the considered one itsself
        	 * (thus 9 grid-squares in total) are went through and the repulsive forces that act against v1
        	 * if the points lie inside the ball (radius 2k), in which repulsive forces are calced, are calculated and
        	 * added to v1.
        	 */
        	for(int i=xGridIndex-1; i<xGridIndex+1; i++){
        		for(int j=yGridIndex-1; j<yGridIndex+1; j++){
        			//Catch cases where not all 9 grid-squares are relevant
        			//case: Considered square is not in the inner area of the grid -> no grid-squares further out
        			if(i<0 || j<0 || i>=grid[indexConnectedComponent].length || j>=grid[indexConnectedComponent][i].length) continue;
        			//case: No vertices in this grid-square
        			if(grid[indexConnectedComponent][i][j] == null) continue;
        			
        			//Regular case: Go through vertices and check if distance is small enough to calc rep. forces
        			for(VertexTriple<V> v2: grid[indexConnectedComponent][i][j]){
        				if(Point2D.distance(v1.get2().getX(), v1.get2().getY(), v2.get2().getX(), v2.get2().getY())<=2*k[indexConnectedComponent]){
        					if (v1 != v2) {
        	                    Point2D p1 = apply(v1);
        	                    Point2D p2 = apply(v2);
        	                    if(p1 == null || p2 == null) continue;
        	                    double xDelta = p1.getX() - p2.getX();
        	                    double yDelta = p1.getY() - p2.getY();

        	                    double deltaLength = Math.max(EPSILON, Math
        	                            .sqrt((xDelta * xDelta) + (yDelta * yDelta)));

        	                    double force = (repulsion_constant * repulsion_constant) / deltaLength;

        	                    if (Double.isNaN(force)) { throw new RuntimeException(
        	                    "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }

        	                    fvd1.offset((xDelta / deltaLength) * force,
        	                            (yDelta / deltaLength) * force);
        	                }
        				}
        			}
        		}
        	}
        } catch(ConcurrentModificationException cme) {
            calcRepulsion(v1);
        }
    }
}