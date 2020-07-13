package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.layout.ObservableCachingLayout;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration.DemonstrationLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration.DemonstrationOfGraphDrawing;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityCriterion;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityTesterForLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.ScaledLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

import java.awt.*;
import java.text.NumberFormat;

//import java.lang.management.ManagementFactory;

public class DefaultVisualizationModelForDemonstration<V,E> extends DefaultVisualizationModel<V,E> {
	
	private int layoutindex;
	
	AffineTransformModifiedStartingPositions<V> atvs;
	
	/*
	 * The two standard constructors from the super-class
	 */
	public DefaultVisualizationModelForDemonstration(DemonstrationLayout<V, E> layout, int layoutindex,
			AffineTransformModifiedStartingPositions<V> atvs) {
		super(layout);
		this.layoutindex = layoutindex;
		this.atvs = atvs;
	}
	public DefaultVisualizationModelForDemonstration(DemonstrationLayout<V, E> layout, Dimension d, int layoutindex,
			AffineTransformModifiedStartingPositions<V> atvs) {
		super(layout, d);
		this.layoutindex = layoutindex;
		this.atvs = atvs;
	}
	
	/*
	 * Adjustment to fit the Generics.
	 * Some things changed.
	 * Compare with original method to see changes.
	 */
	@Override
	public void setGraphLayout(final Layout<V,E> layout, Dimension viewSize) {
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
//        	layout.initialize(); //moved to VisRunner.run()! (Some lines under this in this code)
        	if(atvs!=null){
        		atvs.update();
        	}
        	/**
        	 * This variable was added to be sure that the next graph is already selected, when the
        	 * previous graph is still drawn.
        	 * Otherwise maybe wrong numbers are displayed.
        	 */
        	final int counterAtTheBeginningOfTheDrawing = DemonstrationOfGraphDrawing.counterHowOftenNextOrPrevOrNewWasClicked;

            if(relaxer == null) {
            	relaxer = new VisRunner((IterativeContext)this.layout){
            		@Override
            		public void run() {
            		    running = true;
            		    sleepTime = (long) (1000.0/(double)DemonstrationOfGraphDrawing.demonstrationConfig.animationStepsPerSecond);
//    					long startTime = System.currentTimeMillis();
//    					long cpuStartTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
            		    try {
            		    	layout.initialize();
            		        while (!process.done() && !stop) {
            		            synchronized (pauseObject) {
            		                while (manualSuspend && !stop) {
            		                    try {
            		                        pauseObject.wait();
            		                    } catch (InterruptedException e) {
            		                    	// ignore
            		                    }
            		                }
            		            }
            		            process.step();
            		            if(atvs!=null){
            		            	atvs.update();
            		            }
            		            
            		            if (stop)
            		                return;
            		            
            		            try {
            		                Thread.sleep(sleepTime);
            		            } catch (InterruptedException ie) {
            		            	// ignore
            		            }
            		        }

            		    } finally {
//            		    	long cpuEndTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
//        					long endTime = System.currentTimeMillis();
        					        					
            		    	//No displaying of time if empty layout is selected
        					if(!(DemonstrationOfGraphDrawing.graphIndex==-1
        							|| DemonstrationOfGraphDrawing.graphIndex==DemonstrationOfGraphDrawing.objectsInDirectory.length)
        							&& counterAtTheBeginningOfTheDrawing == DemonstrationOfGraphDrawing.counterHowOftenNextOrPrevOrNewWasClicked){
//	        					double needeCPUTime = (double)(cpuEndTime-cpuStartTime)/1000000000;
//	        					double neededTime = (double)(endTime-startTime)/1000;
	    						NumberFormat nf = NumberFormat.getInstance();
	    						nf.setMinimumFractionDigits(2);
	    						nf.setMaximumFractionDigits(2);
	    						DemonstrationOfGraphDrawing.allCpuTimeJLabels[layoutindex].setText(
	    								"Used CPU time: "+nf.format((double)((DemonstrationLayout<V,E>)layout).getNeededTimeInNs()/
	    										1000000000)+" s");
	    						double timeInS = (double)((DemonstrationLayout<V,E>)layout).getNeededTimeInNs()/1000000000;
	    						/*
	    						 * If timeInS==0 then you do not see a bar in the diagram-graphic.
	    						 * That makes it seem that the diagram is not drawn correctly (sth missing).
	    						 * To avoid this simply a very small value is selected instead of 0
	    						 */
	    						if(timeInS==0){
	    							timeInS = 0.0001;
	    						}
	    						DemonstrationOfGraphDrawing.dataset[0].setValue(timeInS,
	    								DemonstrationOfGraphDrawing.algorithmsStringForDiagram[layoutindex],
	    								DemonstrationOfGraphDrawing.categoriesStringForDiagram[0]);
	    						QualityTesterForLayout<V,E> qt = 
	    								new QualityTesterForLayout<V,E>(
	    										new ScaledLayout<V,E>(layout));
	    						DemonstrationOfGraphDrawing.allNumberOfEdgeCrossingsJLabelsJLabels[layoutindex].setText(
	    								"Number of crossings: "+qt.getNumberOfCrossings());
	    						DemonstrationOfGraphDrawing.dataset[1].setValue(
	    								qt.getNumberOfCrossings(),
	    								DemonstrationOfGraphDrawing.algorithmsStringForDiagram[layoutindex],
	    								DemonstrationOfGraphDrawing.categoriesStringForDiagram[1]);
	    						nf.setMinimumFractionDigits(1);
	    						nf.setMaximumFractionDigits(1);
	    						DemonstrationOfGraphDrawing.allEdgeLengthStandardDeviationJLabels[layoutindex].setText(
	    								"Standard deviation edge length: "+nf.format(qt.get(QualityCriterion.EDGE_LENGTH,
	    										Statistic.STANDARD_DEVIATION)));
	    						DemonstrationOfGraphDrawing.dataset[2].setValue(
	    								qt.get(QualityCriterion.EDGE_LENGTH, Statistic.STANDARD_DEVIATION),
	    								DemonstrationOfGraphDrawing.algorithmsStringForDiagram[layoutindex],
	    								DemonstrationOfGraphDrawing.categoriesStringForDiagram[2]);
//	    						DemonstrationDesGraphzeichnens.updateDiagramName();
	    						DemonstrationOfGraphDrawing.animationCompleted(); //Tell the Demonstration-class that the procedure was completed (is finished)
        					}
//        					DemonstrationDesGraphzeichnens.repaintVV(layout);
        					
        					//if needed draw next layout
        					if(((DemonstrationLayout<V,E>)layout).getIndexOfTheAlgorithmInDemonstrationOfGraphDrawing()==0){
            					DemonstrationOfGraphDrawing.computeLayoutsInAdvance(false);
        					}
        					
            		        running = false;
            		    }
            		}
            	};
//            	relaxer.prerelax(); //Taken out, so that only relax() is used to paint where the Thread is called and time is measured
            	relaxer.relax();
            }
        }
        fireStateChanged();
	}
}
