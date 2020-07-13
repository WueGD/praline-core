package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import edu.uci.ics.jung.algorithms.layout.Layout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class AffineTransformModifiedStartingPositions<V> extends AffineTransform {
	
	//Saved values that are the reference when the view is reset
	private double m00Save;
	private double m11Save;
	private double m02Save;
	private double m12Save;
	
	private Layout<V, ?> referenceLayout;
	private Dimension desiredSize;
	
	/**
	 * Pass extreme values to one layout.
	 * Get it with {@link FRLayoutNoMaps#getXMinXMax_yMinYMax(java.util.Collection)}
	 * @param <V>
	 */
	public AffineTransformModifiedStartingPositions(Dimension desiredSize, Layout<V,?> layout) {
		super();
		this.referenceLayout = layout;
		this.desiredSize = desiredSize;
		update();
	}
	
	public Layout<V, ?> getReferenceLayout() {
		return referenceLayout;
	}
	/**
	 * It is not updated automatically!
	 * Call {@link AffineTransformModifiedStartingPositions#setReferenceLayoutAndUpdate(Layout)}
	 * instead if necessary.
	 * 
	 * @param referenceLayout
	 */
	public void setReferenceLayout(Layout<V, ?> referenceLayout) {
		this.referenceLayout = referenceLayout;
	}
	public void setReferenceLayoutAndUpdate(Layout<V, ?> referenceLayout) {
		this.referenceLayout = referenceLayout;
		this.update();
	}

	/**
	 * Updating to the layout-size and passed at the beginning
	 */
	@Override
	public void setToIdentity() {
		super.setToIdentity();
		super.setTransform(m00Save, 0, 0, m11Save, m02Save, m12Save);
	}
	
	/**
	 * Updates the values m00, m11, m02, m12 according to the current minima/maxima of the x and y values
	 * in the layout passed at the beginning and according to the initially passed desired size.
	 */
	public void update(){
		//special case: catch empty layout
		if(referenceLayout.getGraph().getVertexCount()==0){
			return;
		}
		
		
		double puffer = 0.05; //Which ratio should be left empty at the sides as a buffer
		
		ArrayList<VertexTriple<V>> positionTriples = new ArrayList<VertexTriple<V>>(
				referenceLayout.getGraph().getVertexCount());
		
		for(V v: referenceLayout.getGraph().getVertices()){
			positionTriples.add(new VertexTriple<V>(v, referenceLayout.apply(v)));
		}
		
		Tuple<Tuple<Double,Double>,Tuple<Double,Double>> xMinXMax_yMinYMax = FRLayoutNoMaps.getXMinXMax_yMinYMax(positionTriples);
		double xDiff = (xMinXMax_yMinYMax.get1().get2()-xMinXMax_yMinYMax.get1().get1());
		double yDiff = (xMinXMax_yMinYMax.get2().get2()-xMinXMax_yMinYMax.get2().get1());
		double xScal = desiredSize.width/xDiff;
		double yScal = desiredSize.height/yDiff;
		double totalScal = Math.min(xScal, yScal);
		super.setTransform(
				(1-2*puffer)*totalScal, //x-Scaling
				0,0, //shear-values (see super), there nothing is done
				(1-2*puffer)*totalScal, //y-Scaling
				-xMinXMax_yMinYMax.get1().get1() * (1-2*puffer)*totalScal + puffer * desiredSize.width +
						(Math.max(xDiff, yDiff)-xDiff)/2*(1-2*puffer)*totalScal, //x-Offset
				-xMinXMax_yMinYMax.get2().get1() * (1-2*puffer)*totalScal + puffer * desiredSize.height +
						(Math.max(xDiff, yDiff)-yDiff)/2*(1-2*puffer)*totalScal //y-Offset
		);
		m00Save = getScaleX();
		m11Save = getScaleY();
		m02Save = getTranslateX();
		m12Save = getTranslateY();
	}
}
