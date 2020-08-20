package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.FRLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;

/**
 * FR+WSPD
 * <p>
 * Modifies the {@link FRLayout}-algorithm (respectively the {@link FRLayoutNoMapsNoFrame})
 * by approximating the repulsive forces with a {@link WellSeparatedPairDecomposition}
 * (which is based on a {@link SplitTree}).
 * These two ({@link SplitTree} and {@link WellSeparatedPairDecomposition) are computed
 * always in the first iteration and in every following iteration according to the
 * used {@link RecomputationOfSplitTreeAndWSPDFunction}.
 * <p>
 * This class is abstract, because it remains open how the repulsive forces on
 * every point (vertex resp. {@link VertexTriple}) are computed exactyl.
 * <br>
 * The 2 available implementations of it are the ones in
 * {@link FRWSPDb_b} and {@link FRWSPDp_b}.
 * See there for more information.
 */
public abstract class FRWSPD<V, E> extends FRLayoutNoMapsNoFrame<V, E> implements LayoutWithWSPD<V> {
	
	
    private double sForTheWSPD;
    /**
     * For every connected graph-component one {@link SplitTree}
     */
	protected SplitTree<V>[] splitTree;
    /**
     * For every connected graph-component one {@link WellSeparatedPairDecomposition}
     */
	private WellSeparatedPairDecomposition<V>[] wspd;
	/**
	 * can be used for {@link RecomputationOfSplitTreeAndWSPDFunction}.
	 * (Get via its getter)
	 */
	private int iterationInWhichTheSplitTreeWasBuildNewLastTime = 1;
	private RecomputationOfSplitTreeAndWSPDFunction recomputationOfSplitTreeAndWSPDFunction =
			new RecomputationOfSplitTreeAndWSPDFunction(); //init with default recomp-function
	
	
	
    /*
     * Constructors
     */
	
	public FRWSPD(Graph<V, E> g, double sForTheWSPD, Dimension d) {
		super(g, d);
		this.sForTheWSPD = sForTheWSPD;
	}

	public FRWSPD(Graph<V, E> g, double sForTheWSPD, Dimension d, long seed) {
		super(g, d, seed);
		this.sForTheWSPD = sForTheWSPD;
	}

	
	
	
	/*
	 * Some Setters and Getters
	 */
	
	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}
	
	@Override
    public double getSForTheWSPD(){
    	return sForTheWSPD;
    }
    
	@Override
	public WellSeparatedPairDecomposition<V>[] getWSPD(){
		return wspd;
	}
	
	@Override
	public SplitTree<V>[] getSplitTree(){
		return splitTree;
	}
	
	@Override
	public int getIterationInWhichTheSplitTreeWasBuildNewLastTime(){
		return iterationInWhichTheSplitTreeWasBuildNewLastTime;
	}
    
	@Override
	public void setRecomputationOfSplitTreeAndWSPDFunction(RecomputationOfSplitTreeAndWSPDFunction function) {
		this.recomputationOfSplitTreeAndWSPDFunction = function;
	}
	
	@Override
	public RecomputationOfSplitTreeAndWSPDFunction getRecomputationOfSplitTreeAndWSPDFunction() {
		return recomputationOfSplitTreeAndWSPDFunction;
	}
	
	
	/*
	 * Update-Methods
	 */
	

	protected void recomputeSplitTreeAndWSPD(){
		splitTree = new SplitTree[vertices.length];
		for(int i=0; i<vertices.length; i++){
			splitTree[i] = new SplitTree<V>(vertices[i]);
		}
        iterationInWhichTheSplitTreeWasBuildNewLastTime = currentIteration;
        wspd = new WellSeparatedPairDecomposition[vertices.length];
		for(int i=0; i<vertices.length; i++){
			wspd[i] = new WellSeparatedPairDecomposition<V>(splitTree[i], sForTheWSPD);
		}
	}
	
	@Override
	public void updateBarycenters() {
		for(SplitTree<V> st: splitTree){
			st.recalculateAllBarycenters();
		}
	};
	
	@Override
	public void updateBoundingRectangles() {
		for(SplitTree<V> st: splitTree){
			st.recalculateAllBoundingRectangles();
		}
	};
	
	@Override
	public void recomputeWSPD(){
		//frist update bounding rectangles
		for(SplitTree<V> st: splitTree){
			st.recalculateAllBoundingRectangles();
		}
		//then new wspd
		for(int i=0; i<wspd.length; i++){
			wspd[i] = new WellSeparatedPairDecomposition<V>(splitTree[i], sForTheWSPD);
		}
	}
	
	
	
	
	
	
	
	
	
	/*
	 * Methods for calculation
	 */
	
	/*
	 * Change:
	 * Do not calculate for every possible tuple of vertices, instead use WSPD-pairs
     */
	@Override
	protected void calcRepulsion() {
        /*
         * At first all fvd-values must be reset.
         * In the original alg. this is done in "step()" and there in "Calculate repulsion".
         * As the repulsive forces are not calculated point by point using the WSPD,
         * this necessary step is done here initially
         */
        for(int i=0; i<vertices.length; i++){
	        while(true) {
	            try {
	                for(VertexTriple<V> t : vertices[i]) {
	                    t.get3().setLocation(0, 0);
	                }
	                break;
	            } catch(ConcurrentModificationException cme) {}
	        }
        }
        /**
         * Calculate repulsion via WSPD
         */
        //1. calculate splitTree and WSPD if this is the 1st iteration or the function returns true
    	if(currentIteration==1 || recomputationOfSplitTreeAndWSPDFunction.apply(this)){
    		recomputeSplitTreeAndWSPD();
    	}
    	else{
    		for(SplitTree<V> st: splitTree){
    			st.resetRepulsiveForceOfAllSplitTreeNodes();
    		}
    	}
		if(currentIteration%intervalForLeavingOutCalculationOfRepulsiveForces!=0){ //In some iteartions no repulsive forces are computed
			//2. Calc repulsive forces by going throug all WSPD-pairs
	        while(true) {
	            try {
	            	for(int i=0; i<wspd.length; i++){
		                for(SplitTreeNode<V>[] paar: wspd[i].getWellSeparatedPairs()) {
		                	calcAndRecordRepulsiveForces(paar, i);
		                }
	            	}
	                break;
	            } catch(ConcurrentModificationException cme) {}
	        }
	        //3. recored the calculated forces
	        while(true) {
	            try {
	            	propagateRepulsiveForcesToLeaves();
	                break;
	            } catch(ConcurrentModificationException cme) {}
	        }
		}
	}
	
	/**
	 * This method has to be implemented by every non-abstract class inheriting from this class.
	 * For all points of the two pair-parts (a pair is passed via SpliTreeNode[] wsPair) the repulsive force
	 * has to be calculated.
	 * The result has either to be recorded via {@link SplitTree#addRepulsiveForce(SplitTreeNode, double, double)}
	 * in every {@link SplitTreeNode} and propageted via {@link FRWSPD#propagateRepulsiveForcesToLeaves()} afterwards
	 * or to be written in for every vertex directly via {@link FRWSPD#recordRepulsion(Point2D, double, double)}.
	 * The first possibility should allow lying in a better running time class.
	 * 
	 * @param wsPair
	 * Array of length 2
	 */
	protected abstract void calcAndRecordRepulsiveForces(SplitTreeNode<V>[] wsPair, int indexConnectedComponent);
	
	/**
	 * This method has to be implemented by every non-abstract class inheriting from this class.
	 * It can be left empty there.
	 * It is called once in every iteration after repulsive forces were calculated via
	 * {@link FRWSPD#calcAndRecordRepulsiveForces(SplitTreeNode[], int)}.
	 * If there the repulsive forces were only recorded via {@link SplitTree#addRepulsiveForce(SplitTreeNode, double, double)}
	 * in the {@link SplitTree}, then they should be propageted downwards to the {@link VertexTriple}s now.
	 * For that call {@link FRWSPD#propagateRecordedRepulsiveForceInTheSplitTree(SplitTreeNode, Point2D, int)}
	 * for the root of the {@link SplitTree} and a new point-object with coordinates (0,0).
	 */
	protected abstract void propagateRepulsiveForcesToLeaves();
	
	/**
	 * Recursive method to bring the calculated repulsive force recorded in {@link SplitTreeNode}s
	 * to the {@link VertexTriple}s.
	 * Call it for the whole {@link SplitTree} only once for stn={@link SplitTree#getRoot()}
	 * and totalRepulsiveForceInParent=new Point2D.Double(0,0).
	 * 
	 * @param stn
	 * @param totalRepulsiveForceInParent
	 * @param indexConnectedComponent
	 */
	protected void propagateRecordedRepulsiveForceInTheSplitTree(SplitTreeNode<V> stn,
			Point2D totalRepulsiveForceInParent, int indexConnectedComponent){
		double xSum = totalRepulsiveForceInParent.getX() + stn.repulsiveForce.getX();
		double ySum = totalRepulsiveForceInParent.getY() + stn.repulsiveForce.getY();
		if(splitTree[indexConnectedComponent].isLeaf(stn)){
			recordRepulsion(stn.getTriple(), xSum, ySum);
		}
		else{
			Point2D totalRepulsiveForceOfThisNode = new Point2D.Double(xSum, ySum);
			for(SplitTreeNode<V> child: splitTree[indexConnectedComponent].getChildren(stn)){
				propagateRecordedRepulsiveForceInTheSplitTree(child,
						totalRepulsiveForceOfThisNode, indexConnectedComponent);
			}
		}
	}
	
	protected void recordRepulsion(VertexTriple<V> triple, double xOffset, double yOffset){
		Point2D fvd = triple.get3(); //FRData from v
        if(fvd == null){ //Why? I can not say. It is taken from FRLayout. To see cases this occurs -> console output
        	System.out.println("fvd == null in FRLayoutMitWSPD, please check!\n\r"+Thread.currentThread().getStackTrace()[1]);
        	return;
        }
        try {
        	fvd.setLocation(fvd.getX()+xOffset, fvd.getY()+yOffset);
        } catch(ConcurrentModificationException cme) {
        	recordRepulsion(triple, xOffset, yOffset);
        }
	}
}
