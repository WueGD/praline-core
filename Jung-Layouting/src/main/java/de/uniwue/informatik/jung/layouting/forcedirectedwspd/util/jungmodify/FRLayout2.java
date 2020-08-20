/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ConcurrentModificationException;
/**
 * Implements the Fruchterman-Reingold force-directed algorithm for node layout.
 * This is an experimental attempt at optimizing {@code FRLayout}; if it is successful
 * it will be folded back into {@code FRLayout} (and this class will disappear).
 * 
 * <p>Behavior is determined by the following settable parameters:
 * <ul>
 * <li/>attraction multiplier: how much edges try to keep their vertices together
 * <li/>repulsion multiplier: how much vertices try to push each other apart
 * <li/>maximum iterations: how many iterations this algorithm will use before stopping
 * </ul>
 * Each of the first two defaults to 0.75; the maximum number of iterations defaults to 700.
 * 
 * <p>
 * <br>
 * EDIT Johannes Zink on 2015/05/18:
 * The following changes were made to get access to private/protected class variables/...
 * <ul>
 * <li/> "private int currentIteration" to "protected int currentIteration" (instance variable)
 * <li/> "private double EPSILON" to "protected double EPSILON" (instance variable)
 * <li/> "private double repulsion_constant" to "protected double repulsion_constant" (instance variable)
 * <li/> "private void cool()" to "protected void cool()" (instance method)
 * <li/> "private Map<V, FRVertexData> frVertexData" to "protected Map<V, FRVertexData> frVertexData" (instance variable)
 * </ul>
 * <br>
 * <p>
 * 
 * @see "Fruchterman and Reingold, 'Graph Drawing by Force-directed Placement'"
 * @see http://i11www.ilkd.uni-karlsruhe.de/teaching/SS_04/visualisierung/papers/fruchterman91graph.pdf
 * 
 * @author Tom Nelson
 * @author Scott White, Yan-Biao Boey, Danyel Fisher
 */
public class FRLayout2<V, E> extends AbstractLayout<V, E> implements IterativeContext {

    private double forceConstant;

    private double temperature;

    protected int currentIteration;

    private int maxIterations = 700;

    protected LoadingCache<V, FRLayout.FRVertexData> frVertexData =
            CacheBuilder.newBuilder().build(new CacheLoader<V, FRLayout.FRVertexData>() {
                public FRLayout.FRVertexData load(V vertex) {
                    return new FRLayout.FRVertexData();
                }
            });

    private double attraction_multiplier = 0.75;
    
    private double attraction_constant;
    
    private double repulsion_multiplier = 0.75;
    
    protected double repulsion_constant;
    
    private double max_dimension;
    
    private Rectangle2D innerBounds = new Rectangle2D.Double();
    
    private boolean checked = false;
    
    /**
     * Creates an instance for the specified graph.
     */
    public FRLayout2(Graph<V, E> g) {
        super(g);
    }
    
    /**
     * Creates an instance of size {@code d} for the specified graph.
     */
    public FRLayout2(Graph<V, E> g, Dimension d) {
        super(g, new RandomLocationTransformer<V>(d, Constants.random.nextLong()), d);
        max_dimension = Math.max(d.height, d.width);
        initialize();
    }

    public FRLayout2(Graph<V, E> g, Dimension d, long seed) {
        super(g, new RandomLocationTransformer<V>(d, seed), d);
        max_dimension = Math.max(d.height, d.width);
        initialize();
    }

    @Override
    public void setSize(Dimension size) {
        setSize(size, Constants.random.nextLong());
    }

    public void setSize(Dimension size, long seed) {
		if(initialized == false) 
			setInitializer(new RandomLocationTransformer<V>(size, seed));
		super.setSize(size);
		double t = size.width/50.0;
		innerBounds.setFrameFromDiagonal(t,t,size.width-t,size.height-t);
        max_dimension = Math.max(size.height, size.width);
	}

    /**
     * Sets the attraction multiplier.
     */
	public void setAttractionMultiplier(double attraction) {
        this.attraction_multiplier = attraction;
    }
    
    /**
     * Sets the repulsion multiplier.
     */
    public void setRepulsionMultiplier(double repulsion) {
        this.repulsion_multiplier = repulsion;
    }
    
	public void reset() {
		doInit();
	}
    
    public void initialize() {
    	doInit();
    }

    private void doInit() {
    	Graph<V,E> graph = getGraph();
    	Dimension d = getSize();
    	if(graph != null && d != null) {
    		currentIteration = 0;
    		temperature = d.getWidth() / 10;

    		forceConstant = 
    			Math
    			.sqrt(d.getHeight()
    					* d.getWidth()
    					/ graph.getVertexCount());

    		attraction_constant = attraction_multiplier * forceConstant;
    		repulsion_constant = repulsion_multiplier * forceConstant;
    	}
    }

    protected double EPSILON = 0.000001D;

    /**
     * Moves the iteration forward one notch, calculation attraction and
     * repulsion between vertices and edges and cooling the temperature.
     */
    public synchronized void step() {
        currentIteration++;

        /**
         * Calculate repulsion
         */
        while(true) {
            
            try {
                for(V v1 : getGraph().getVertices()) {
                    calcRepulsion(v1);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }

        /**
         * Calculate attraction
         */
        while(true) {
            try {
                for(E e : getGraph().getEdges()) {
                    calcAttraction(e);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }


        while(true) {
            try {    
                for(V v : getGraph().getVertices()) {
                    if (isLocked(v)) continue;
                    calcPositions(v);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }
        cool();
    }

    protected synchronized void calcPositions(V v) {
        Point2D fvd = this.frVertexData.getUnchecked(v);
        if(fvd == null) return;
        Point2D xyd = apply(v);
        double deltaLength = Math.max(EPSILON, 
        		Math.sqrt(fvd.getX()*fvd.getX()+fvd.getY()*fvd.getY()));

        double newXDisp = fvd.getX() / deltaLength
                * Math.min(deltaLength, temperature);

        assert Double.isNaN(newXDisp) == false : "Unexpected mathematical result in FRLayout:calcPositions [xdisp]";

        double newYDisp = fvd.getY() / deltaLength
                * Math.min(deltaLength, temperature);
        double newX = xyd.getX()+Math.max(-5, Math.min(5,newXDisp));
        double newY = xyd.getY()+Math.max(-5, Math.min(5,newYDisp));
        
        newX = Math.max(innerBounds.getMinX(), Math.min(newX, innerBounds.getMaxX()));
        newY = Math.max(innerBounds.getMinY(), Math.min(newY, innerBounds.getMaxY()));
        
        xyd.setLocation(newX, newY);

    }

    protected void calcAttraction(E e) {
    	Pair<V> endpoints = getGraph().getEndpoints(e);
        V v1 = endpoints.getFirst();
        V v2 = endpoints.getSecond();
        boolean v1_locked = isLocked(v1);
        boolean v2_locked = isLocked(v2);
        
        if(v1_locked && v2_locked) {
        	// both locked, do nothing
        	return;
        }
        Point2D p1 = apply(v1);
        Point2D p2 = apply(v2);
        if(p1 == null || p2 == null) return;
        double xDelta = p1.getX() - p2.getX();
        double yDelta = p1.getY() - p2.getY();

        double deltaLength = Math.max(EPSILON, p1.distance(p2));

        double force = deltaLength  / attraction_constant;

        assert Double.isNaN(force) == false : "Unexpected mathematical result in FRLayout:calcPositions [force]";

        double dx = xDelta * force;
        double dy = yDelta * force;
        Point2D fvd1 = frVertexData.getUnchecked(v1);
        Point2D fvd2 = frVertexData.getUnchecked(v2);
        if(v2_locked) {
        	// double the offset for v1, as v2 will not be moving in
        	// the opposite direction
        	fvd1.setLocation(fvd1.getX()-2*dx, fvd1.getY()-2*dy);
        } else {
        fvd1.setLocation(fvd1.getX()-dx, fvd1.getY()-dy);
        }
        if(v1_locked) {
        	// double the offset for v2, as v1 will not be moving in
        	// the opposite direction
        	fvd2.setLocation(fvd2.getX()+2*dx, fvd2.getY()+2*dy);
        } else {
        fvd2.setLocation(fvd2.getX()+dx, fvd2.getY()+dy);
    }
    }

    protected void calcRepulsion(V v1) {
        Point2D fvd1 = frVertexData.getUnchecked(v1);
        if(fvd1 == null) return;
        fvd1.setLocation(0, 0);
        boolean v1_locked = isLocked(v1);

        try {
            for(V v2 : getGraph().getVertices()) {

                boolean v2_locked = isLocked(v2);
            	if (v1_locked && v2_locked) continue;
                if (v1 != v2) {
                    Point2D p1 = apply(v1);
                    Point2D p2 = apply(v2);
                    if(p1 == null || p2 == null) continue;
                    double xDelta = p1.getX() - p2.getX();
                    double yDelta = p1.getY() - p2.getY();
                    
                    double deltaLength = Math.max(EPSILON, p1.distanceSq(p2));
                    
                    double force = (repulsion_constant * repulsion_constant);// / deltaLength;
                    
                    double forceOverDeltaLength = force / deltaLength;
                    
                    assert Double.isNaN(force) == false : "Unexpected mathematical result in FRLayout:calcPositions [repulsion]";
                    
                    if(v2_locked) {
                    	// double the offset for v1, as v2 will not be moving in
                    	// the opposite direction
                    	fvd1.setLocation(fvd1.getX()+2 * xDelta * forceOverDeltaLength,
                    		fvd1.getY()+ 2 * yDelta * forceOverDeltaLength);
                    } else {
                    	fvd1.setLocation(fvd1.getX()+xDelta * forceOverDeltaLength,
                        		fvd1.getY()+yDelta * forceOverDeltaLength);
                }
            }
            }
        } catch(ConcurrentModificationException cme) {
            calcRepulsion(v1);
        }
    }

    protected void cool() {
        temperature *= (1.0 - currentIteration / (double) maxIterations);
    }

    /**
     * Sets the maximum number of iterations.
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * This one is an incremental visualization.
     */
    public boolean isIncremental() {
        return true;
    }

    /**
     * Returns true once the current iteration has passed the maximum count,
     * <tt>MAX_ITERATIONS</tt>.
     */
    public boolean done() {
        if (currentIteration > maxIterations || temperature < 1.0/max_dimension) { 
            if (!checked)
            {
//                System.out.println("current iteration: " + currentIteration);
//                System.out.println("temperature: " + temperature);
                checked = true;
            }
            return true; 
        } 
        return false;
    }
}