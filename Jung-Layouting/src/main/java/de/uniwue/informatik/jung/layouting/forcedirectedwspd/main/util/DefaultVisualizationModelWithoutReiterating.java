package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.layout.ObservableCachingLayout;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;

import java.awt.*;

/**
 * This class shall be nothing else than {@link DefaultVisualizationModel}.
 * But there it is not helpful that a already completly drawn (completely iterated) layout that only needs to be
 * visualized is reset by calling initialize().
 * That is the reason that only the method setGraphLayout(layout, viewSize) is overwritten and only layout.initialize()
 * is removed. This is the only change in this class.
 */
public class DefaultVisualizationModelWithoutReiterating<V, E> extends DefaultVisualizationModel<V, E> {

	/*
	 * The two standard constructors from the super-class
	 */
	public DefaultVisualizationModelWithoutReiterating(Layout<V, E> layout) {
		super(layout);
	}
	public DefaultVisualizationModelWithoutReiterating(Layout<V, E> layout, Dimension d) {
		super(layout, d);
	}
	
	/*
	 * 1 to 1 copy from the original, only "layout.initialize();" is commented out
	 * 
	 * (non-Javadoc)
	 * @see edu.uci.ics.jung.visualization.DefaultVisualizationModel#setGraphLayout(edu.uci.ics.jung.algorithms.layout.Layout, java.awt.Dimension)
	 */
	@Override
	public void setGraphLayout(Layout<V,E> layout, Dimension viewSize) {
		// remove listener from old layout
	    if(this.layout != null && this.layout instanceof ChangeEventSupport) {
	        ((ChangeEventSupport)this.layout).removeChangeListener(changeListener);
        }
	    // set to new layout
	    if(layout instanceof ChangeEventSupport) {
	    	this.layout = layout;
	    } else {
	    	this.layout = new ObservableCachingLayout<V,E>(layout);
	    }
		
		((ChangeEventSupport)this.layout).addChangeListener(changeListener);

        if(viewSize == null) {
            viewSize = new Dimension(600,600);
        }
		Dimension layoutSize = layout.getSize();
		// if the layout has NOT been initialized yet, initialize its size
		// now to the size of the VisualizationViewer window
		if(layoutSize == null) {
		    layout.setSize(viewSize);
        }
        if(relaxer != null) {
        	relaxer.stop();
        	relaxer = null;
        }
        if(layout instanceof IterativeContext) {
//        	layout.initialize();
            if(relaxer == null) {
            	relaxer = new VisRunner((IterativeContext)this.layout);
            	relaxer.prerelax();
            	relaxer.relax();
            }
        }
        fireStateChanged();
	}
}
