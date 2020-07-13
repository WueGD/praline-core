package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * For statistical evaluation it is better to scale to a special square of of size
 * (different layouts can be compared better if having the same size).
 * That happens using this pseudo-layout.
 */
public class ScaledLayout<V, E> extends AbstractLayout<V, E>{
	
	/**
	 * Modify if wanted
	 */
	public final static Dimension sizeToScaleTo = new Dimension(600, 600);
	
	Layout<V, E> represantative;
	
	public ScaledLayout(Layout<V,E> represantative) {
		super(represantative.getGraph(), sizeToScaleTo);
		this.represantative = represantative;
		
		//copy points
		LoadingCache<V, Point2D> locationsCopy = CacheBuilder.newBuilder().build(new CacheLoader<V, Point2D>() {
			public Point2D load(V vertex) {
				return new Point2D.Double();
			}
		});
		 for(V v: locations.asMap().keySet()){
			 Point2D oldPoint = locations.getUnchecked(v);
			 locationsCopy.put(v, new Point2D.Double(oldPoint.getX(), oldPoint.getY()));
		 }
		 locations = locationsCopy;
		 
		
		//Scaling
    	//find min and max
    	double minX = Double.POSITIVE_INFINITY;
    	double maxX = Double.NEGATIVE_INFINITY;
    	double minY = Double.POSITIVE_INFINITY;
    	double maxY = Double.NEGATIVE_INFINITY;
    	for(V v: represantative.getGraph().getVertices()){
    		minX = Math.min(minX, represantative.apply(v).getX());
    		maxX = Math.max(maxX, represantative.apply(v).getX());
    		minY = Math.min(minY, represantative.apply(v).getY());
    		maxY = Math.max(maxY, represantative.apply(v).getY());
    	}    	

    	//find offsets
    	double xPreOffset = -minX;
    	double yPreOffset = -minY;
    	double xPostOffset = 0;
    	double yPostOffset = 0;
    	
    	/*
    	 * determine scaling factor if there is more than 1 point
    	 * It is scaled according to the "weaker" scale-factor (x or y) to not deform everything
    	 * (otherwise it may be streched/compressed in x/y-direction).
    	 * As the "weaker" one also determines in which dimension the border of the available space
    	 * is touched, it is moved to the mid of the area in the other dimension
    	 */
    	double scalFactor = 1.0;
    	boolean xScalIsSmaller = true;
    	if(represantative.getGraph().getVertexCount()>1){
        	double xScalFactor = sizeToScaleTo.getWidth() /(maxX-minX);
        	double yScalFactor = sizeToScaleTo.getHeight() /(maxY-minY);
        	scalFactor = Math.min(xScalFactor, yScalFactor);
        	if(yScalFactor<xScalFactor){
        		xScalIsSmaller = false;
        	}
    	}
    	//move to the mid of the "weaker" side
    	if(xScalIsSmaller){
    		yPostOffset += (sizeToScaleTo.getHeight()-(maxY-minY)*scalFactor)/2;
    	}
    	else{
    		xPostOffset += (sizeToScaleTo.getWidth()-(maxX-minX)*scalFactor)/2;
    	}
    	
    	//Adjust every vertex acc. to these
    	for(V v: represantative.getGraph().getVertices()){
    		this.apply(v).setLocation((represantative.apply(v).getX()+xPreOffset)*scalFactor+xPostOffset,
    				(represantative.apply(v).getY()+yPreOffset)*scalFactor+yPostOffset);
    	}
    }
	
	
	
	
	
	@Override
	public void initialize() {
	}
	@Override
	public void reset() {		
	}
}
