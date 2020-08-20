package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;
import org.apache.commons.lang.NotImplementedException;

import java.awt.*;

/**
 * TODO: Problem remaining that the methods of {@link LayoutWithWSPD} (and the whole {@link LayoutWithWSPD}-interface)
 * is designed for one {@link SplitTree} and one {@link WellSeparatedPairDecomposition} covering the whole graph/layout.
 * But in {@link MultiLevelLayout} there is one {@link SingleComponentMultiLevelLayout} for every component of the graph.
 * As this graph may consist of more than one component that may lead to inconsistent states,
 * e.g. {@link LayoutWithWSPD#getIterationInWhichTheSplitTreeWasBuildNewLastTime()} may have different
 * values for each component as these are independent.
 * That might also be a problem with {@link MultiLevelLayout#recomputationFunction}
 * (and also with recomputation function in {@link FRWSPD}) when these functions store information
 * of one (and only one) {@link SplitTree} or {@link WellSeparatedPairDecomposition}.
 */
public abstract class FRWSPDMultiLevel<V, E> extends MultiLevelLayout<V, E> implements LayoutWithWSPD<V>{
	public FRWSPDMultiLevel(Graph<V, E> graph, Dimension size, AlgorithmReference algo, double sOrTheta) {
		super(graph, size, algo, sOrTheta);
	}

	public FRWSPDMultiLevel(Graph<V, E> graph, Dimension size, AlgorithmReference algo, double sOrTheta, long seed) {
		super(graph, size, algo, sOrTheta, seed);
	}

	@Override
	public int getCurrentIteration() {
		return super.currentIteration;
	}

	@Override
	public void setRecomputationOfSplitTreeAndWSPDFunction(
			RecomputationOfSplitTreeAndWSPDFunction function) {
		this.recomputationFunction = function;
	}

	@Override
	public RecomputationOfSplitTreeAndWSPDFunction getRecomputationOfSplitTreeAndWSPDFunction() {
		return this.recomputationFunction;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SplitTree<V>[] getSplitTree() {
		SplitTree<V>[] splitTrees = new SplitTree[super.singleComponentMLLayouts.size()];
		for(int i=0; i<super.singleComponentMLLayouts.size(); i++){
			//only [0] because it is only one component
			splitTrees[i] = ((LayoutWithWSPD)singleComponentMLLayouts.get(i).currentLayoutingAlgorithm).getSplitTree()[0];
		}
		return splitTrees;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public WellSeparatedPairDecomposition<V>[] getWSPD() {
		WellSeparatedPairDecomposition<V>[] wspds = new WellSeparatedPairDecomposition[super.singleComponentMLLayouts.size()];
		for(int i=0; i<super.singleComponentMLLayouts.size(); i++){
			//only [0] because it is only one component
			wspds[i] = ((LayoutWithWSPD)singleComponentMLLayouts.get(i).currentLayoutingAlgorithm).getWSPD()[0];
		}
		return wspds;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateBarycenters() {
		for(int i=0; i<super.singleComponentMLLayouts.size(); i++){
			((LayoutWithWSPD)singleComponentMLLayouts.get(i).currentLayoutingAlgorithm).updateBarycenters();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateBoundingRectangles() {
		for(int i=0; i<super.singleComponentMLLayouts.size(); i++){
			((LayoutWithWSPD)singleComponentMLLayouts.get(i).currentLayoutingAlgorithm).updateBoundingRectangles();;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void recomputeWSPD() {
		for(int i=0; i<super.singleComponentMLLayouts.size(); i++){
			((LayoutWithWSPD)singleComponentMLLayouts.get(i).currentLayoutingAlgorithm).recomputeWSPD();
		}
	}

	@Override
	public double getSForTheWSPD() {
		return sOrTheta;
	}
	
	/**
	 * Value absolute to the complete {@link MultiLevelLayout} not relative to the concrete layouting algorithm
	 * used at each level
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public int getIterationInWhichTheSplitTreeWasBuildNewLastTime() {
		if(super.singleComponentMLLayouts.size()==1){
			return this.currentIteration - ((LayoutWithWSPD)singleComponentMLLayouts.get(0).currentLayoutingAlgorithm).getCurrentIteration() 
					+ ((LayoutWithWSPD)singleComponentMLLayouts.get(0).currentLayoutingAlgorithm).getIterationInWhichTheSplitTreeWasBuildNewLastTime();
		}
		//irregular case! see class javadoc
		
		try {
			throw new NotImplementedException();
		} catch (NotImplementedException e) {
			System.err.println("Problem: Calling a function designed for one SplitTree but there is more than one component!");
			e.printStackTrace();
		}
		return -1; //must return something, so return -1 what makes no real sense
	}
}