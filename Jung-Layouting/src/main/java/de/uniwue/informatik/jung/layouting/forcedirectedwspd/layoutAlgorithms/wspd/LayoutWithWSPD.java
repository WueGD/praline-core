package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

/**
 * The methods defined here may also be called by a {@link RecomputationOfSplitTreeAndWSPDFunction}. 
 */
public interface LayoutWithWSPD<V> {
	
	public int getCurrentIteration();
	
	/**
	 * See {@link RecomputationOfSplitTreeAndWSPDFunction}.
	 * <br>
	 * If not set with this method the default-function (recompuation in every iteration) is in use.
	 * 
	 * @param function
	 */
	public void setRecomputationOfSplitTreeAndWSPDFunction(RecomputationOfSplitTreeAndWSPDFunction function);
	
	/**
	 * See {@link RecomputationOfSplitTreeAndWSPDFunction}.
	 * <br>
	 * If not set by {@link LayoutWithWSPD#setRecomputationOfSplitTreeAndWSPDFunction(RecomputationOfSplitTreeAndWSPDFunction)}
	 * the default-function (recompuation in every iteration) is in use.
	 * 
	 * @return
	 */
	public RecomputationOfSplitTreeAndWSPDFunction getRecomputationOfSplitTreeAndWSPDFunction();
	
	public SplitTree<V>[] getSplitTree();
	
	public WellSeparatedPairDecomposition<V>[] getWSPD();
		
	/**
	 * Updates all barycenters in every {@link SplitTreeNode} in the {@link SplitTree}
	 * belonging to the {@link WellSeparatedPairDecomposition} currently in use
	 * in this {@link LayoutWithWSPD}.
	 */
	public void updateBarycenters();
	
	/**
	 * Updates all bounding rectangles in every {@link SplitTreeNode} in the {@link SplitTree}
	 * belonging to the {@link WellSeparatedPairDecomposition} currently in use
	 * in this {@link LayoutWithWSPD}.
	 */
	public void updateBoundingRectangles();
	
	/**
	 * In this method at first the bounding rectangles saved in every {@link SplitTreeNode}
	 * to the {@link SplitTree} currently in use are updated,
	 * because as the vertices got new locations in the last iteration the old
	 * bounding rectangles may be outdated.
	 * The new {@link SplitTree} might fulfill the properties it usually
	 * guarentees no longer after that!
	 * Thus this method should be used carefully.
	 * <p>
	 * After it the {@link WellSeparatedPairDecomposition} is computed new.
	 * That is a possiblity to construct the {@link WellSeparatedPairDecomposition}
	 * new without also computing a new {@link SplitTree}.
	 * <p>
	 * To do no work not being used a {@link RecomputationOfSplitTreeAndWSPDFunction}
	 * calling that method should return false.
	 */
	public void recomputeWSPD();
	
	public double getSForTheWSPD();
	
	public int getIterationInWhichTheSplitTreeWasBuildNewLastTime();
}
