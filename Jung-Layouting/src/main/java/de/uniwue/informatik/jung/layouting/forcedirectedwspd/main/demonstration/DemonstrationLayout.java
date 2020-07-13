package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration;

import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Layout for saving the positions of the vertices in every iteration of the referenced
 * (coresponding) layout.
 * The referenced layout of type {@link FRLayoutNoMapsNoFrame} is iterated (drawn) automatically in this class.
 * This class was desined for use in class {@link DemonstrationOfGraphDrawing} only.
 */
public class DemonstrationLayout<V,E> implements Layout<V,E>, IterativeContext, Runnable {
	
	private FRLayoutNoMapsNoFrame<V, E> referencedLayout;
	
	private Graph<V, E> graphToReferencedLayout;
	
	private boolean referencedLayoutCompletelyIterated = false;
	
	private long neededTimeInNs = -1; //-1 for undetermined
	
	private LinkedList<LinkedHashMap<V,Point2D>> allScaledPositionsEveryIteration;
	
	/**
	 * For the user iterationg over this layout it shall seem as if it is drawn
	 * (iterating) but in fact it just uses just the locations computed by
	 * its referenced layout in the i-th iteration as its locations in the i-th
	 * iteration. 
	 */
	private int currentIteration = 0;
	
	private int indexOfTheAlgorithmInDemonstrationOfGraphDrawing;

	/**
	 * As size of this layout the size of the referenced layout is taken.
	 * 
	 * @param referencedLayout
	 * @param graphToReferencedLayout must be the graph of the referencedLayout otherwise
	 * the getGraph()-method of this instance is not consistent. If this method is not used one can pass null.
	 * @param indexOfTheAlgorithmInDemonstrationOfGraphDrawing Variable that is needed for {@link DemonstrationOfGraphDrawing}.
	 * If -1 is passed nothing in {@link DemonstrationOfGraphDrawing} will be called.
	 */
	public DemonstrationLayout(FRLayoutNoMapsNoFrame<V, E> referencedLayout, Graph<V,E> graphToReferencedLayout,
			int indexOfTheAlgorithmInDemonstrationOfGraphDrawing) {
		this.referencedLayout = referencedLayout;
		this.graphToReferencedLayout = graphToReferencedLayout;
		this.indexOfTheAlgorithmInDemonstrationOfGraphDrawing = indexOfTheAlgorithmInDemonstrationOfGraphDrawing;
	}
	
	public void run() {
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		
		allScaledPositionsEveryIteration = new LinkedList<LinkedHashMap<V,Point2D>>();
		
		long cpuStartTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		
		referencedLayout.initialize();
		while(true){
			/*
			 * Read all point-locations, scale and save
			 */
			//For that copy at first the current vertexTriples. V and FRVertexData are kept.
			//The location (the point-object) is copied and scaled to the correct size for this
			//demonstration layout and saved here
			List<VertexTriple<V>>[] currentVertices = referencedLayout.getVerticesToConnectedComponents().clone();
			for(int i=0; i<currentVertices.length; i++){
				List<VertexTriple<V>> cloneList = new LinkedList<VertexTriple<V>>();
				for(VertexTriple<V> triple: currentVertices[i]){
					VertexTriple<V> cloneTriple = new VertexTriple<V>(triple.get1(),
							new Point2D.Double(triple.get2().getX(), triple.get2().getY()), triple.get3());
					cloneList.add(cloneTriple);
				}
				currentVertices[i] = cloneList;
			}
			//now scale
			referencedLayout.scaleComponents(currentVertices, false);
			//now save
			LinkedHashMap<V,Point2D> scaledPointsInThisIteration = new LinkedHashMap<V, Point2D>(referencedLayout.getVertexCount());
			for(List<VertexTriple<V>> tripleList: currentVertices){
				for(VertexTriple<V> triple: tripleList){
					scaledPointsInThisIteration.put(triple.get1(), triple.get2());
				}
			}
			allScaledPositionsEveryIteration.add(scaledPointsInThisIteration);
			
			if(referencedLayout.done()){
				//The breaking condition for this while-loop is placed here instead of in the original while-condition
				//because so the locations at the beginning and at the end can be saved as well
				break;
			}
			
			//Draw the referenced layout step by step in this while-loop
			referencedLayout.step();
		}

		long cpuEndTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		
		neededTimeInNs = cpuEndTime - cpuStartTime;
		
		referencedLayoutCompletelyIterated = true;
		
		//Next line for calling in demonstrationclass
		//If this layout is used outside the demonstrationclass adapt
		if(indexOfTheAlgorithmInDemonstrationOfGraphDrawing==0){ //call only when the first (index 0), this then calcs for all 3 new there
			DemonstrationOfGraphDrawing.computeLayoutsInAdvance(false);
		}
	};

	@Override
	public void step() {
		if(allScaledPositionsEveryIteration==null){ //if not init -> do nothing
			return;
		}
		currentIteration = Math.min(currentIteration+1, Math.max(0, allScaledPositionsEveryIteration.size()-1));
	}

	@Override
	public boolean done() {
		if(referencedLayoutCompletelyIterated){
			return currentIteration >= allScaledPositionsEveryIteration.size()-1;
		}
		return false;
	}

	/**
	 * -1, if not completed computations yet.
	 * (Referenced layout has not completed the drawing or has not yet started to compute the drawing)
	 */
	public long getNeededTimeInNs() {
		return neededTimeInNs;
	}
	
	public boolean isReferencedLayoutCompletelyIterated() {
		return referencedLayoutCompletelyIterated;
	}
	
	public int getIndexOfTheAlgorithmInDemonstrationOfGraphDrawing() {
		return indexOfTheAlgorithmInDemonstrationOfGraphDrawing;
	}

	
	@Override
	public Point2D apply(V input) {
		if(allScaledPositionsEveryIteration==null || allScaledPositionsEveryIteration.size() <= currentIteration){
			return new Point2D.Double(0, 0); //if not init yet -> return dummy-value
		}
		return allScaledPositionsEveryIteration.get(currentIteration).get(input);
	}

	@Override
	public void initialize() {
		//do nothing
	}

	@Override
	public void setInitializer(Function<V, Point2D> initializer) {
		//do nothing
	}

	@Override
	public void setGraph(Graph<V, E> graph) {
		System.err.println("setGraph() is not intended to be used any more in the class "+this.getClass().getName()+"."
    			+ "\nFor Adjustment adapt source code.");
	}

	@Override
	public Graph<V, E> getGraph() {
		return graphToReferencedLayout;
	}

	@Override
	public void reset() {
		currentIteration = 0;
	}
	
	/**
	 * Note:
	 * The size change passed here is passed to the referenced layout (size changed there).
	 * <p>
	 * Beside that:<br>
	 * The points in this layout were, if {@link DemonstrationLayout#isReferencedLayoutCompletelyIterated()}==true,
	 * already calculated/scaled.
	 * A scaling (by calling this) aftwards is not possible any more (resp. has no incluence to the already calculated points).
	 * If this is called while drawing, the scaling changes at one point.
	 * 
	 * @param d
	 */
	@Override
	public void setSize(Dimension d) {
		this.referencedLayout.setSize(d);
	}

	/**
	 * @return
	 * Size of the referenced layout.
	 */
	@Override
	public Dimension getSize() {
		return referencedLayout.getSize();
	}

	@Override
	public void lock(V v, boolean state) {
		//is not used any more
	}

	@Override
	public boolean isLocked(V v) {
		//is not used any more
		return false;
	}

	@Override
	public void setLocation(V v, Point2D location) {
		System.err.println("setLocation() is not intended to be used any more in the class "+this.getClass().getName()+"."
    			+ "\nFor Adjustment adapt source code.");
	}
}
