package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.EdgeTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.TripleSetGraph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.List;

/**
 * It is the class {@link FRLayoutNoMaps} with two modifications:
 * <br>
 * <ol>
 *  <li>
 *   The frame as it is used in {@link FRLayout} and suggested by the paper from
 *   Fruchterman and Reingold is left out (not used).
 *   Instead the layout is scaled to that frame after drawing it.
 *  </li>
 *  <li>
 *   Every fifth (this may be changed via
 *   {@link FRLayoutNoMapsNoFrame#intervalForLeavingOutCalculationOfRepulsiveForces})
 *   iteration there are no repulsive forces.
 *   This was suggested by the paper from Fruchterman and Reingold, but not done in {@link FRLayout}.
 *   To still calculate repulsive forces in every iteration and use no frame as well
 *   use {@link FRLayoutNoMapsNoFrameAlwaysRep}.
 *  </li>
 * </ol>
 * <br>
 * To still fit a given rectangle (frame) the drawing is scaled to fit that frame
 * afterwards (after computing a drawing).
 * That means the drawing can be unfold without being bounded by some arbitrary
 * frame (boundaries).
 * Hence there is no need no more to balance the absolute value of the forces
 * according to any given frame.
 * In the JUNG-algorithm {@link FRLayout} these values seem to be balanced 
 * not very well for the standard-size frame 600x600.
 * It produces thus quite bad drawings. This problem could be solved that way.
 * <p>
 * Another problem appearing this way is, that a graph consisting
 * of more than one connected component would have its components
 * moving far away from each other because there is no boundary
 * keeping them togother no more.
 * This problem is solved here by dividing the drawing areas.
 * Every component gets an portion proportional to its number
 * of vertices. Then each component is drawn independently from each other
 * and scaled to its portion of the drawing area afterwards.
 * The problem of dividing the area is done in a rather easy way:
 * Every part has the same hight (whole height of the drawing area)
 * and every part a width proportional to the number of vertices.
 * This leads to quite small, long stripes for components with
 * few vertices.
 * In the scaling step a drawing might be rotated 90 degree
 * by switching x- and y-coordinates, but of course this is still
 * far from omptimal and may be changed for a better way of dividing the area.
 * <br>
 * Some constants for drawing properties ({@link FRLayoutNoMapsNoFrame#threshold}, {@link FRLayoutNoMapsNoFrame#mMaxIterations},
 * {@link FRLayoutNoMapsNoFrame#coolDownFactorEachIteration}) are defined here
 */
public class FRLayoutNoMapsNoFrame<V, E> extends FRLayoutNoMaps<V, E> implements AlgorithmMeasureRepulsiveTime {
	

	public static final double threshold = 0.482842712474619; //0.482842712474619; should be the same value as in OGDF-SpringEmbedderFRExact
	public static final int mMaxIterations = 1000; //1000; same value as in OGDF-SpringEmbedderFRExact
	public static final double coolDownFactorEachIteration = 0.9; //0.9; value from OGDF
	
	/**
	 * In the original paper from Fruchterman and Reingold there is suggested to turn off
	 * repulsive forces every fifth iteration.
	 * This is not done in {@link FRLayout} in JUNG.
	 * This was added here.
	 * Default is 5 as suggested there but may be changed.
	 * If repulsive forces shall be calculated every iteration use {@link FRLayoutNoMapsNoFrameAlwaysRep}
	 * instead of {@link FRLayoutNoMapsNoFrame}.
	 * It is the same class with the only difference that every iteration repulsive forces are calculated.
	 * (As in {@link FRLayout})
	 */
	public static final int intervalForLeavingOutCalculationOfRepulsiveForces = 5;
	
	
	/**
	 * Overwrites the list of vertices in the super-class by this array of lists of vertices.
	 * <br>
	 * In the array there is a list of vertices for every connencted component,
	 * giving every component an specific index.
	 * <p>
	 * Note: edges-list is not overwritten by an array of lists because for the
	 * calculation of the attractive forces it is irrelevant in which component which
	 * edge is, as it acts only within its component anyway.
	 */
	protected List<VertexTriple<V>>[] vertices;
	protected boolean scaleToDrawingAreaAtTheEnd = true; //default: true
	/**
	 * Specifies the sizes for each component (drawing areas;
	 * {@link FRLayoutNoMapsNoFrame#getSize()} all added together).
	 * Each component has its value saved with the same index
	 * as in {@link FRLayoutNoMapsNoFrame#vertices}.
	 * <br>
	 * Here each subsize is an stripe of the total size.
	 * (Same height as the total one)
	 */
	protected Dimension[] subsize;
	/**
	 * Specifies for every component where in the total are the area
	 * specified in {@link FRLayoutNoMapsNoFrame#subsize} begins.
	 * It is only an double value because here the area is devided in
	 * stripes so the position of a subarea needs to be defined only
	 * in one dimension.
	 * Each component has its value saved with the same index
	 * as in {@link FRLayoutNoMapsNoFrame#vertices}.
	 */
	protected double[] subsizeOffset;
	
	/**
	 * A value used for the drawing area (rectangle) of each component as margin to the inner area
	 * where vertices may be placed.
	 */
	protected double minDistanceToTheDrawingAreaOfTheNextComponent;
	
	/*
	 * TODO
	 * Note to the next variables:
	 * Here they are for the whole drawing.
	 * But as every component should be drawn independently
	 * it should be possible that one component needs fewer iterations
	 * than another one.
	 * So there should be a converged or done-array for every component
	 * (e.g.: boolean[] converged) analog to the vertices-array.
	 * The whole drawing would then be done() iff every component is drawn.
	 * For a possible solution see class MultiLevelLayout.
	 */
	protected boolean converged;
	
	
	/**
	 * Almost the same as the constructor from {@link FRLayoutNoMaps}.
	 * Only change: Devision acc. to the connected components.
	 * 
	 * @param graph
	 * @param d
	 */
	public FRLayoutNoMapsNoFrame(Graph<V,E> graph, Dimension d) {
		//there locations is touched -> must be changed
		//<Replace that call in AbstractLayout  (= line "super(g, new RandomLocationTransformer<V>(d), d);" in FRLayout)>
		//The new created graph is then saved at this.graph
		super();
		
		
		//Find connected components
		WeakComponentClusterer<V, E> clusterer = new WeakComponentClusterer<V, E>();
		Set<Set<V>> allComponents = clusterer.apply(graph);
		int numberOfComponents = allComponents.size();
		
		//init arrays for components
		vertices = new List[numberOfComponents];
		subsize = new Dimension[numberOfComponents];
		subsizeOffset = new double[numberOfComponents];
		
		//Read graph. This is the only step that works with the JUNG-graph-structure
		int i=0; //countindex
		int offset=0; //offsetindex
		double stripeWidthPerVertex = d.getWidth()/(double)graph.getVertexCount();
		minDistanceToTheDrawingAreaOfTheNextComponent = stripeWidthPerVertex/5.0; //5 is chosen arbitrarily - but should be smaller than stripeWidthPerVertex/2
		vertexData2VertexTriple = new LinkedHashMap<V, VertexTriple<V>>(graph.getVertexCount());
		for(Set<V> component: allComponents){
			subsize[i] = new Dimension((int)(stripeWidthPerVertex * (double)component.size()), (int)d.getHeight());
			subsizeOffset[i] = (double)offset*stripeWidthPerVertex;
			offset += component.size();
			
			vertices[i] = new ArrayList<VertexTriple<V>>(component.size());
			RandomLocationTransformer<V> randomLocationTransformer = new RandomLocationTransformer<V>(subsize[i]);
			for(V v: component){
				VertexTriple<V> newVTriple =
						new VertexTriple<V>(v, randomLocationTransformer.apply(v), new FRVertexData());
				vertices[i].add(newVTriple);
				vertexData2VertexTriple.put(v, newVTriple);
			}			
			i++;
		}
		edges = new ArrayList<EdgeTriple<V,E>>(graph.getEdgeCount());
		for(E e: graph.getEdges()){
			edges.add(new EdgeTriple<V,E>
					(e, vertexData2VertexTriple.get(graph.getEndpoints(e).getFirst()), vertexData2VertexTriple.get(graph.getEndpoints(e).getSecond())));
		}
		
		
		this.size = d;
		this.numberOfVertices = graph.getVertexCount();
		//</Replace that call in AbstractLayoutt>
		//<Replace that call in FRLayout>
        initialize();
        max_dimension = Math.max(d.height, d.width);
		//</Replace that call in FRLayout>
		
        //But still set the variables not wanted to be used to null.
        //If they are used one can see it when a NullPointerException is thrown.
		this.locations = null;
		this.frVertexData = null;
//		this.graph = null;
		
		//do not set graph to null but to an extra for that created graph-object
		List<VertexTriple<V>> allVertices = new ArrayList<VertexTriple<V>>(graph.getVertexCount());
		for(List<VertexTriple<V>> component: vertices){
			allVertices.addAll(component);
		}
		this.graph = new TripleSetGraph<V, E>(allVertices, edges);
	}

	
	/*
	 * New Getters (and Setters)
	 */
	public List<VertexTriple<V>>[] getVerticesToConnectedComponents(){
		return vertices;
	}
	@Override
	public List<VertexTriple<V>> getVertices() {
		List<VertexTriple<V>> allVertices = new ArrayList<VertexTriple<V>>(numberOfVertices);
		for(List<VertexTriple<V>> list: vertices){
			allVertices.addAll(list);
		}
		return allVertices;
	};
	
	/**
	 * Default is true
	 * 
	 * @return
	 */
	public boolean isScaleToDrawingAreaAtTheEnd() {
		return scaleToDrawingAreaAtTheEnd;
	}
	
	/**
	 * Default is true
	 * 
	 * @return
	 */
	public void setScaleToDrawingAreaAtTheEnd(
			boolean scaleToDrawingAreaAtTheEnd) {
		this.scaleToDrawingAreaAtTheEnd = scaleToDrawingAreaAtTheEnd;
	}

	
	
	
	

	@Override
    public synchronized void step() {
        currentIteration++;

        /**
         * Calculate repulsion
         * 
         * Taken out as own method that it can be overwritten easier.
         */
        calcRepulsion();
        

        /**
         * Calculate attraction
         */
        while(true) {
            try {
                for(EdgeTriple<V,E> e : edges) {

                    calcAttraction(e);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }

        /**
         * Calculate positions
         */
        calcPositions();
        
        
        cool();
        
        /*
         * Newly inserted step to scale back to the assigned space/area
         */
        if(done() && scaleToDrawingAreaAtTheEnd){
            scaleComponents(vertices, true);
        }
    }
	

	@Override
	public synchronized long stepMeasureRepulsiveTime() {
        currentIteration++;

        /**
         * Calculate repulsion
         * 
         * Taken out as own method that it can be overwritten easier.
         */
		long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
        calcRepulsion();
		long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();

        /**
         * Calculate attraction
         */
        while(true) {
            try {
                for(EdgeTriple<V,E> e : edges) {

                    calcAttraction(e);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }
        

        /**
         * Calculate positions
         */
        calcPositions();
        
        
        cool();
        
        /*
         * Newly inserted step to scale back to the assigned space/area
         */
        if(done() && scaleToDrawingAreaAtTheEnd){
            scaleComponents(vertices, true);
        }
        
        return endTime - startTime;
	}
	
	/**
	 * Outtaken from the step()-method.
	 * So that it can be overwritten easier.
	 */
	protected void calcRepulsion(){
		if(currentIteration%intervalForLeavingOutCalculationOfRepulsiveForces!=0){ //In some iteartions no repulsive forces are computed
			//Individually done for every component, see calcRepulsion(t)
	        for(int i=0; i<vertices.length; i++){
//	        	if(converged[i]==true) continue;
		        while(true) {
		            try {
		                for(VertexTriple<V> t : vertices[i]) {
		                    calcRepulsion(t, i);
		                }
		                break;
		            } catch(ConcurrentModificationException cme) {}
		        }
	        }
        }
	}
	
	
	/**
	 * Outtaken from the step()-method.
	 * So that it can be overwritten easier.
	 */
	protected void calcPositions(){
		converged = true;
        for(int i=0; i<vertices.length; i++){
//        	if(converged[i]==true) continue;
        	while(true) {
	            try {
	                for(VertexTriple<V> t : vertices[i]) {
//	                    if (isLocked(t)) continue;
	                    calcPositions(t);
	                }
	                break;
	            } catch(ConcurrentModificationException cme) {}
	        }
        }
	}
	
	/**
     * Step to scale every component to its reserved area.
     * <p>
     * This step can be done in O(|V|).
     * <p>
     * The subsize-areas are assumed to be squares or rectangles where the shorter
     * side is in the x-dimension (every area has larger height (delta y) than width (delta x)).
     * Is a drawing more wide than high than x- and y-coordinates are switched to make it
     * more high than wide.
     * This can be deactivated by allow90DegreeRotation=false
     * 
	 * @param componentsToBeScaled
	 * @param allow90DegreeRotation 
	 */
	public void scaleComponents(List<VertexTriple<V>>[] componentsToBeScaled, boolean allow90DegreeRotation){
		for(int i=0; i<componentsToBeScaled.length; i++){            	
			//Find min and max
			Tuple<Tuple<Double, Double>, Tuple<Double, Double>> maxAndMin = super.getXMinXMax_yMinYMax(componentsToBeScaled[i]);
        	double minX = maxAndMin.get1().get1();
        	double maxX = maxAndMin.get1().get2();
        	double minY = maxAndMin.get2().get1();
        	double maxY = maxAndMin.get2().get2();
        	
        	//rotate if it is assumed to be better than (and it is allowed)
        	if(allow90DegreeRotation && subsize[i].height>subsize[i].width && maxX-minX > maxY-minY){
	        	for(VertexTriple<V> t: componentsToBeScaled[i]){
	        		t.get2().setLocation(t.get2().getY(), t.get2().getX());
	        	}
	        	//min and max have to be swapped by x and y, too
	        	double swapStorage = minX;
	        	minX = minY;
	        	minY = swapStorage;
	        	swapStorage = maxX;
	        	maxX = maxY;
	        	maxY = swapStorage;
        	}
        	

        	//find offset
        	double xPreOffset = -minX;
        	double yPreOffset = -minY;
        	double xPostOffset = minDistanceToTheDrawingAreaOfTheNextComponent + subsizeOffset[i];
        	double yPostOffset = minDistanceToTheDrawingAreaOfTheNextComponent;
        	
        	/*
        	 * determine scaling factor if there is more than 1 point
        	 * It is scaled according to the "weaker" scale-factor (x or y) to not deform everything
        	 * (otherwise it may be streched/compressed in x/y-direction).
        	 * As the "weaker" one also determines in which dimension the border of the available space
        	 * is touched, it is moved to the mid of the area in the other dimension
        	 */
        	double scalFactor = 1.0;
        	boolean xScalIsSmaller = true;
        	if(componentsToBeScaled[i].size()>1){
	        	double xScalFactor = (subsize[i].getWidth()-2*minDistanceToTheDrawingAreaOfTheNextComponent)/(maxX-minX);
	        	double yScalFactor = (subsize[i].getHeight()-2*minDistanceToTheDrawingAreaOfTheNextComponent)/(maxY-minY);
	        	scalFactor = Math.min(xScalFactor, yScalFactor);
	        	if(yScalFactor<xScalFactor){
	        		xScalIsSmaller = false;
	        	}
        	}
        	//move to the mid of the "weaker" side
        	if(xScalIsSmaller){
        		yPostOffset += ((subsize[i].getHeight()-2*minDistanceToTheDrawingAreaOfTheNextComponent)-(maxY-minY)*scalFactor)/2;
        	}
        	else{
        		xPostOffset += ((subsize[i].getWidth()-2*minDistanceToTheDrawingAreaOfTheNextComponent)-(maxX-minX)*scalFactor)/2;
        	}
        	
        	//Adjust every vertex acc. to these
        	for(VertexTriple<V> t: componentsToBeScaled[i]){
        		t.get2().setLocation((t.get2().getX()+xPreOffset)*scalFactor+xPostOffset,
        				(t.get2().getY()+yPreOffset)*scalFactor+yPostOffset);
        	}
        }
	}
	
	protected void calcRepulsion(VertexTriple<V> v1, int indexConnectedComponent) {
        FRVertexData fvd1 = getFRData(v1);
        if(fvd1 == null)
            return;
        fvd1.setLocation(0, 0);

        try {
            for(VertexTriple<V> v2 : vertices[indexConnectedComponent]) {

//	                if (isLocked(v2)) continue;
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
        } catch(ConcurrentModificationException cme) {
            calcRepulsion(v1);
        }
    }
	
	@Override
    protected synchronized void calcPositions(VertexTriple<V> t) {
        FRVertexData fvd = getFRData(t);
        if(fvd == null) return;
        Point2D xyd = apply(t);
        double deltaLength = Math.max(EPSILON, fvd.norm());

        double newXDisp = fvd.getX() / deltaLength
                * Math.min(deltaLength, temperature);

        if (Double.isNaN(newXDisp)) {
        	throw new IllegalArgumentException(
                "Unexpected mathematical result in FRLayout:calcPositions [xdisp]"); }

        double newYDisp = fvd.getY() / deltaLength
                * Math.min(deltaLength, temperature);     
        
        xyd.setLocation(xyd.getX()+newXDisp, xyd.getY()+newYDisp);
        if (newXDisp*newXDisp + newYDisp*newYDisp > threshold*threshold) {
        	converged = false;
        }
    }
	
	
	/*
	 * Necessary overwritings where "vertices" appears:
	 */
	
	@Override
	public Graph<V,E> getUndirectedGraph_V_E(){
		UndirectedSparseGraph<V, E> newGraph = new UndirectedSparseGraph<V,E>();
		for(int i=0; i<vertices.length; i++){
			for(VertexTriple<V> t: vertices[i]){
				newGraph.addVertex(t.get1());
			}
		}
		for(EdgeTriple<V,E> e: edges){
			newGraph.addEdge(e.get1(), e.get2().get1(), e.get3().get1());
		}
		return newGraph;
	}
	
	@Override
	public FRLayout<V,E> getCorresponding_V_E_Layout(){
		FRLayout<V,E> newLayout = new FRLayout<V, E>(this.getUndirectedGraph_V_E(), this.getSize());
		for(int i=0; i<vertices.length; i++){
			for(VertexTriple<V> t: vertices[i]){
				newLayout.apply(t.get1()).setLocation(t.get2());
			}
		}
		newLayout.setMaxIterations(-1); //so that the new layout is not changed (recalculated)
		return newLayout;
	}
	
	@Override
	public void initialize() {
    	doInit();
    }
    private void doInit() {
//    	Graph<V,E> graph = getGraph(); //this line is removed and every other use of "graph", too
    	Dimension d = getSize();
    	if(/*graph != null &&*/ d != null) {
    		currentIteration = 0;
    		temperature = d.getWidth() / 10;

    		forceConstant =
    			Math
    			.sqrt(d.getHeight()
    					* d.getWidth()
    					/ /*graph.getVertexCount()*/ numberOfVertices); //Replacement

    		attraction_constant = attraction_multiplier * forceConstant;
    		repulsion_constant = repulsion_multiplier * forceConstant;
    	}
    }
    
    @Override
    public void setSize(Dimension size) {
//    	if(initialized == false) {
//			setInitializer(new RandomLocationTransformer<V>(size));
//		}
    	if(size != null /*&& graph != null*/) { //Removement
			
			Dimension oldSize = this.size;
			this.size = size;
			initialize();
			
			if(oldSize != null) {
				adjustLocations(oldSize, size);
			}
		}
        max_dimension = Math.max(size.height, size.width);
    }
    private void adjustLocations(Dimension oldSize, Dimension size) {

		int xOffset = (size.width - oldSize.width) / 2;
		int yOffset = (size.height - oldSize.height) / 2;

		// now, move each vertex to be at the new screen center
		while(true) {
		    try {
		    	for(int i=0; i<vertices.length; i++){
			    	for(VertexTriple<V> t: vertices[i]){
			            offsetVertex(t, xOffset, yOffset);
			        }
		    	}
		        break;
		    } catch(ConcurrentModificationException cme) {
		    }
		}
	}
    
    @Override
    public boolean done() {
        if (currentIteration > mMaxIterations || converged)
        {
            return true;
        }
        return false;
    }
    
    @Override
    protected void cool() {
    	// Copied from OGDF
        temperature *= 0.9;
    }
}
