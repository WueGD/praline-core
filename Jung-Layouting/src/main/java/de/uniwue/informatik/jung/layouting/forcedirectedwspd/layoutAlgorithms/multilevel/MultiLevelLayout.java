package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Randomness;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.UndirectedSparseGraph;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmMeasureRepulsiveTime;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Point2DComparator;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * This Java implementation follows in many points regarding the multi level treatment
 * and {@link GalaxyMultiLevel} handling the OGDF implementation of FastMultipoleMultilevelEmbedder
 * from Martin Gronemann (class in files FastMultipoleMultilevelEmbedder.h + FastMultipoleMultilevelEmbedder.cpp).
 */
public class MultiLevelLayout<V,E> extends AbstractLayout<V, E> implements IterativeContext, AlgorithmMeasureRepulsiveTime {
	
	public static final int multiLevelNumNodesBound = 10; //10; value from OGDF/FastMultipoleEmbedder
	public static final double multiLevelScaleFactor = 1.4; //1.4; value from OGDF/FastMultipoleEmbedder
	public static final double multiLevelRandomPlacingSpan = 2.0; //2.0; value from OGDF/FastMultipoleEmbedder
	
	/**
	 * see {@link FRLayoutNoMapsNoFrame#scaleComponents(List[], boolean)}
	 */
	private boolean allow90DegreeRotationOfEachComponentLayout = true;
	
	private AlgorithmReference layoutingAlgorithmType;
	protected double sOrTheta;
	protected long seed;
	
	/**
	 * see {@link FRLayoutNoMapsNoFrame#numberOfComponents}
	 */
	private int numberOfComponents;
	/**
	 * see {@link FRLayoutNoMapsNoFrame#minDistanceToTheDrawingAreaOfTheNextComponent}
	 */
	private double minDistanceToTheDrawingAreaOfTheNextComponent;
	/**
	 * see {@link FRLayoutNoMapsNoFrame#subsize}
	 */
	private ArrayList<Dimension> subsize;
	/**
	 * see {@link FRLayoutNoMapsNoFrame#subsizeOffset}
	 */
	private ArrayList<Double> subsizeOffset;
	/**
	 * The "real" algos
	 */
	protected ArrayList<SingleComponentMultiLevelLayout> singleComponentMLLayouts;
	
	

	//needed for LayoutWithWSPD (if layoutingAlgorithm is a WSPD-algo)
	protected int currentIteration = 0;
	protected RecomputationOfSplitTreeAndWSPDFunction recomputationFunction;
	
	/**
	 * 
	 * @param graph
	 * @param size
	 * @param layoutingAlgorithmType
	 * @param sOrTheta
	 * 		May be arbitrary value if not needed for {@link AlgorithmReference#getNewInstance(Graph, Dimension, double)}
	 */
	protected MultiLevelLayout(Graph<V,E> graph, Dimension size,
			AlgorithmReference layoutingAlgorithmType, double sOrTheta) {
		this(graph, size, layoutingAlgorithmType, sOrTheta, Constants.random.nextLong());
	}

	/**
	 *
	 * @param graph
	 * @param size
	 * @param layoutingAlgorithmType
	 * @param sOrTheta
	 * 		May be arbitrary value if not needed for {@link AlgorithmReference#getNewInstance(Graph, Dimension, double)}
	 * @param seed
	 */
	protected MultiLevelLayout(Graph<V,E> graph, Dimension size,
							   AlgorithmReference layoutingAlgorithmType, double sOrTheta, long seed) {
		super(graph,size);
		this.layoutingAlgorithmType = layoutingAlgorithmType;
		this.sOrTheta = sOrTheta;
		this.seed = seed;
		
		//Find connected components
		WeakComponentClusterer<V, E> clusterer = new WeakComponentClusterer<V, E>();
		Set<Set<V>> allComponents = clusterer.apply(graph);
		numberOfComponents = allComponents.size();
		
		//init arraylists
		subsize = new ArrayList<Dimension>(numberOfComponents);
		subsizeOffset = new ArrayList<Double>(numberOfComponents);
		singleComponentMLLayouts = new ArrayList<
				SingleComponentMultiLevelLayout>(numberOfComponents);
		
		//a own algo for each connected component (this is taken from FRLayoutNoMapsNoFrame, similar code there)
		int i=0; //countindex
		int offset=0; //offsetindex
		double stripeWidthPerVertex = size.getWidth()/(double)graph.getVertexCount();
		minDistanceToTheDrawingAreaOfTheNextComponent =
				stripeWidthPerVertex/5.0; //5 is chosen arbitrarily - but should be smaller than stripeWidthPerVertex/2
		for(Set<V> component: allComponents){
			subsize.add(i, new Dimension((int)(stripeWidthPerVertex * (double)component.size()), (int)size.getHeight()));
			subsizeOffset.add(i, (double)offset*stripeWidthPerVertex);
			offset += component.size();
			
			Graph<V,E> graphOfThisComponent = new UndirectedSparseGraph<V, E>();
			for(V v: component){
				graphOfThisComponent.addVertex(v);
			}
			//add edges to graph (is done in this second forall V loop because all vertices have to be added before)
			for(V v: component){
				for(E e: graph.getIncidentEdges(v)){
					if(!graphOfThisComponent.containsEdge(e)){
						graphOfThisComponent.addEdge(e, graph.getIncidentVertices(e));
					}
				}
				
			}
			singleComponentMLLayouts.add(i, new SingleComponentMultiLevelLayout(graphOfThisComponent, subsize.get(i)));
			//init locations
			RandomLocationTransformer<V> randomLocationTransformer = new RandomLocationTransformer<V>(subsize.get(i),
					seed);
			for(V v: component){
				Point2D location = randomLocationTransformer.apply(v);
				this.setLocation(v, location);
				singleComponentMLLayouts.get(i).setLocation(v, location);
			}			
			i++;
		}
	}
	
	@Override
	public void initialize() {
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){
			componentLayout.initialize();
		}
	}


	@Override
	public void reset() {
		reset(Constants.random.nextLong());
	}

	public void reset(long seed) {
		int i=0; //counter
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){

			//new locations
			RandomLocationTransformer<V> randomLocationTransformer = new RandomLocationTransformer<V>(subsize.get(i),
					seed);
			for(V v: componentLayout.getGraph().getVertices()){
				Point2D location = randomLocationTransformer.apply(v);
				this.setLocation(v, location);
				componentLayout.setLocation(v, location);
			}
			
			componentLayout.reset();
			++i;
		}
	}


	@Override
	public void step() {
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){
			componentLayout.step();
		}
		
		applyVertexLocationsFromSingleComponentsToThisTotalLayout();
		++currentIteration;
	}
	
	@Override
	public long stepMeasureRepulsiveTime() {
		long sum = 0;
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){
			sum += componentLayout.stepMeasureRepulsiveTime();
		}
		
		applyVertexLocationsFromSingleComponentsToThisTotalLayout();
		++currentIteration;
		return sum;
	}


	@Override
	public boolean done() {
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){
			if(!componentLayout.done()){
				return false;
			}
		}
		return true;
	}
	
	

	private void applyVertexLocationsFromSingleComponentsToThisTotalLayout(){
		//assign positions to current layouting algorithm
		ArrayList<Collection<Point2D>> pointsEachComponent = new ArrayList<Collection<Point2D>>(numberOfComponents);
		int i = 0; //counter
		for(SingleComponentMultiLevelLayout componentLayout: singleComponentMLLayouts){
			ArrayList<Point2D> pointsInThisComponent = new ArrayList<Point2D>(componentLayout.getGraph().getVertexCount());
			pointsEachComponent.add(i, pointsInThisComponent);
			for(V v: componentLayout.getGraph().getVertices()){
				Point2D p = this.apply(v);
				p.setLocation(componentLayout.apply(v));
				pointsInThisComponent.add(p);
			}
			++i;
		}
		scaleComponents(pointsEachComponent, allow90DegreeRotationOfEachComponentLayout);
	}
	
	/**
	 * Copied from {@link FRLayoutNoMapsNoFrame#scaleComponents(List[], boolean)}.
	 * See there.
	 * 
	 * @param pointsEachComponent
	 * @param allow90DegreeRotation
	 */
	private void scaleComponents(ArrayList<Collection<Point2D>> pointsEachComponent, boolean allow90DegreeRotation){
		for(int i=0; i<pointsEachComponent.size(); i++){
			//Find min and max
			Point2DComparator p2DComp = new Point2DComparator();
			double minX = Collections.min(pointsEachComponent.get(i), p2DComp.new X()).getX();
        	double maxX = Collections.max(pointsEachComponent.get(i), p2DComp.new X()).getX();
        	double minY = Collections.min(pointsEachComponent.get(i), p2DComp.new Y()).getY();
        	double maxY = Collections.max(pointsEachComponent.get(i), p2DComp.new Y()).getY();
        	
        	//rotate if it is assumed to be better than (and it is allowed)
        	if(allow90DegreeRotation && subsize.get(i).height>subsize.get(i).width && maxX-minX > maxY-minY){
	        	for(Point2D p: pointsEachComponent.get(i)){
	        		p.setLocation(p.getY(), p.getX());
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
        	double xPostOffset = minDistanceToTheDrawingAreaOfTheNextComponent + subsizeOffset.get(i);
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
        	if(pointsEachComponent.get(i).size()>1){
	        	double xScalFactor = (subsize.get(i).getWidth()-2*minDistanceToTheDrawingAreaOfTheNextComponent)/(maxX-minX);
	        	double yScalFactor = (subsize.get(i).getHeight()-2*minDistanceToTheDrawingAreaOfTheNextComponent)/(maxY-minY);
	        	scalFactor = Math.min(xScalFactor, yScalFactor);
	        	if(yScalFactor<xScalFactor){
	        		xScalIsSmaller = false;
	        	}
        	}
        	//move to the mid of the "weaker" side
        	if(xScalIsSmaller){
        		yPostOffset += ((subsize.get(i).getHeight()-2*minDistanceToTheDrawingAreaOfTheNextComponent)-(maxY-minY)*scalFactor)/2;
        	}
        	else{
        		xPostOffset += ((subsize.get(i).getWidth()-2*minDistanceToTheDrawingAreaOfTheNextComponent)-(maxX-minX)*scalFactor)/2;
        	}
        	
        	//Adjust every vertex acc. to these
        	for(Point2D p: pointsEachComponent.get(i)){
        		p.setLocation((p.getX()+xPreOffset)*scalFactor+xPostOffset,
        				(p.getY()+yPreOffset)*scalFactor+yPostOffset);
        	}
        }
	}
	
	
	
	/**
	 * This is the "real" MultiLevelAlgorithm but only for one connected component.
	 * To break the passed graph down to its connected components this is realized with that intern class.
	 * A different realization was chosen in {@link FRLayoutNoMapsNoFrame} (no subclass but variables as arrays)
	 */
	public class SingleComponentMultiLevelLayout extends AbstractLayout<V, E> implements IterativeContext, AlgorithmMeasureRepulsiveTime {
		
		protected AbstractLayout<V,E> prevLayoutingAlgorithm;
		protected AbstractLayout<V,E> currentLayoutingAlgorithm;
		
		private GalaxyMultiLevel<V, E> currentLevel;
		private GalaxyMultiLevel<V, E> finestLevel;
		private int currentLevelNumber;
		private int coarsestLevelNumber;
		private int totalNrOfLevels;
		
		
		
		/**
		 * 
		 * @param graph
		 * @param size
		 * @param layoutingAlgorithmType
		 * @param sOrTheta
		 * 		May be arbitrary value if not needed for {@link AlgorithmReference#getNewInstance(Graph, Dimension, double)}
		 */
		protected SingleComponentMultiLevelLayout(Graph<V,E> graph, Dimension size) {
			super(graph,size);
		}
	
		
		public AbstractLayout<V, E> getLayoutingAlgorithm() {
			return currentLayoutingAlgorithm;
		}
	
	
		@Override
		public void initialize() {
			createMultiLevelGraphs();
			switchToNextLayoutingLevelAlgorithm(true); //init layoutingAlgorithm for coarsest level (==current level at the beginning)
			super.initialized = true;
		}
		
		public void createMultiLevelGraphs(){
			finestLevel = new GalaxyMultiLevel<V,E>(graph);
			Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V, E>>> levelInfos = finestLevel.initialize();
			
			currentLevel = finestLevel;
			currentLevelNumber = 0;
			totalNrOfLevels = 1;
			
			GalaxyMultiLevelBuilder<V, E> builder = new GalaxyMultiLevelBuilder<V, E>();
			
			while(currentLevel.graph.getVertexCount() > multiLevelNumNodesBound){
				Tuple<GalaxyMultiLevel<V, E>, Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V, E>>>> newLevelPlusLevelInfos =
						builder.build(currentLevel, levelInfos);
				GalaxyMultiLevel<V,E> newLevel = newLevelPlusLevelInfos.get1();
				levelInfos = newLevelPlusLevelInfos.get2();
				currentLevel = newLevel;
				++currentLevelNumber;
				++totalNrOfLevels;
			}
			coarsestLevelNumber = currentLevelNumber;
		}
		
	
		@Override
		public void reset() {
			initialize();
		}
	
	
		@Override
		public void step() {
			/*
			 * if already done BEFORE then return
			 * (because there is post-processing at the end of this method that is done once after it is done the first time)
			 */
			if(done()){
				return;
			}
			
			if (currentLayoutingAlgorithm instanceof IterativeContext) {
				if(!((IterativeContext) currentLayoutingAlgorithm).done()){
					
					((IterativeContext) currentLayoutingAlgorithm).step();
					if(((IterativeContext) currentLayoutingAlgorithm).done()){
						expandNextLevel();
					}
					
				}
				else{
					expandNextLevel();
				}
			}
			
			if(done()){
				applyVertexLocationsFromLayoutingAlgorithmToThisMultiLevelLayout();
			}
		}
		
		public long stepMeasureRepulsiveTime() {
			/*
			 * if already done BEFORE then return
			 * (because there is post-processing at the end of this method that is done once after it is done the first time)
			 */
			if(done()){
				return 0;
			}
			
			long sum = 0;
			
			if (currentLayoutingAlgorithm instanceof IterativeContext) {
				if(!((IterativeContext) currentLayoutingAlgorithm).done()){
					
					if (currentLayoutingAlgorithm instanceof AlgorithmMeasureRepulsiveTime) {
						sum += ((AlgorithmMeasureRepulsiveTime) currentLayoutingAlgorithm).stepMeasureRepulsiveTime();
					} else {
						((IterativeContext) currentLayoutingAlgorithm).step();
					}
					if(((IterativeContext) currentLayoutingAlgorithm).done()){
						expandNextLevel();
					}
					
				}
				else{
					expandNextLevel();
				}
			}
			
			if(done()){
				applyVertexLocationsFromLayoutingAlgorithmToThisMultiLevelLayout();
			}
			
			return sum;
		}
	

		@Override
		public boolean done() {
			if(currentLayoutingAlgorithm instanceof IterativeContext){
				return currentLevelNumber<0 && ((IterativeContext) currentLayoutingAlgorithm).done();
			}
			return currentLevelNumber<0;
		}
		
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void expandNextLevel(){
			currentLevel = currentLevel.finerMultiLevel;
			--currentLevelNumber;
			if(currentLevelNumber<0){
				return;
			}
			switchToNextLayoutingLevelAlgorithm(false);
			
			//assign positions from level before (if not coarsest level)
			if(currentLevelNumber+1 != coarsestLevelNumber){
				for(RelevantLevelNodeInfo<V> relevantLevelNodeInfo: currentLevel.relevantNodeInfos){
					Point2D parentLocation;
					//check cases!
					if(currentLayoutingAlgorithm instanceof FRLayoutNoMaps){
						parentLocation = ((FRLayoutNoMaps) prevLayoutingAlgorithm).transformVertexData(
								relevantLevelNodeInfo.parentRelevantLevelNodeInfo.vertex);
					}
					else{
						parentLocation = prevLayoutingAlgorithm.apply(relevantLevelNodeInfo.parentRelevantLevelNodeInfo.vertex);
					}
					double newX = (parentLocation.getX()
							+ (Randomness.random.nextDouble()-0.5)*multiLevelRandomPlacingSpan) * multiLevelScaleFactor;
					double newY = (parentLocation.getY()
							+ (Randomness.random.nextDouble()-0.5)*multiLevelRandomPlacingSpan) * multiLevelScaleFactor;
					
					Point2D childLocation;
					//check cases!
					if(currentLayoutingAlgorithm instanceof FRLayoutNoMaps){
						childLocation = ((FRLayoutNoMaps) currentLayoutingAlgorithm).transformVertexData(relevantLevelNodeInfo.vertex);
					}
					else{
						childLocation = currentLayoutingAlgorithm.apply(relevantLevelNodeInfo.vertex);
					}
					childLocation.setLocation(newX, newY);
				}
			}
		}
		
		private void switchToNextLayoutingLevelAlgorithm(boolean applyVertexLocationsFromThisMultiLevelLayoutToLayoutingAlgorithm){
			this.prevLayoutingAlgorithm = this.currentLayoutingAlgorithm;
			this.currentLayoutingAlgorithm = layoutingAlgorithmType.getNewInstance(currentLevel.graph, size, sOrTheta
					, seed);
			if(currentLayoutingAlgorithm instanceof LayoutWithWSPD){
				((LayoutWithWSPD<?>) currentLayoutingAlgorithm).setRecomputationOfSplitTreeAndWSPDFunction(recomputationFunction);
			}
			if(applyVertexLocationsFromThisMultiLevelLayoutToLayoutingAlgorithm){
				applyVertexLocationsFromThisMultiLevelLayoutToLayoutingAlgorithm();
			}
			this.currentLayoutingAlgorithm.initialize();
		}
		
		private void applyVertexLocationsFromThisMultiLevelLayoutToLayoutingAlgorithm(){
			//assign positions to current layouting algorithm
			for(V v: currentLayoutingAlgorithm.getGraph().getVertices()){
				//must do case analysis
				if(currentLayoutingAlgorithm instanceof FRLayoutNoMaps){
					//v is of type VertexTriple<V>
					@SuppressWarnings("unchecked")
					double x = this.apply(((VertexTriple<V>)v).getVertexData()).getX();
					@SuppressWarnings("unchecked")
					double y = this.apply(((VertexTriple<V>)v).getVertexData()).getY();
					currentLayoutingAlgorithm.setLocation(v, x, y);
				}
				else{
					//v is of type <V>
					currentLayoutingAlgorithm.setLocation(v, this.apply(v).getX(), this.apply(v).getY());
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void applyVertexLocationsFromLayoutingAlgorithmToThisMultiLevelLayout(){
			//assign positions to this layout
			for(V v: currentLayoutingAlgorithm.getGraph().getVertices()){
				//must do case analysis
				if(currentLayoutingAlgorithm instanceof FRLayoutNoMaps){
					//v is of type VertexTriple<V>
					double x = ((VertexTriple<?>)v).getLocation().getX();
					double y = ((VertexTriple<?>)v).getLocation().getY();
					this.setLocation(((VertexTriple<V>)v).getVertexData(), x, y);
				}
				else{
					//v is of type <V>
					this.setLocation(v, currentLayoutingAlgorithm.apply(v));
				}
			}
		}
	}
}
