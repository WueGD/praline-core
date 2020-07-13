package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

/**
 * Manger/Controller for the functions that determine if in the current iteration {@link SplitTree}
 * and {@link WellSeparatedPairDecomposition} must be recomputed or can still be used (see {@link RecomputationOfSplitTreeAndWSPDFunction}s).
 * These functions are used in {@link LayoutWithWSPD}-algorithms.
 * <p>
 * Adapt this source code if other functions should included or other excluded.
 */
public class RecomputationOfSplitTreeAndWSPDFunctionManager extends Manager<RecomputationOfSplitTreeAndWSPDFunction> {
	
	
	/**
	 * Change this code if wanted.
	 * In the last step the names given to the function in {@link Manager#allObjectnames} are
	 * also given to the functions via {@link RecomputationOfSplitTreeAndWSPDFunction#setName(String)}.
	 */
	@Override
	protected void initialize(){		
		/*
		 * Always new
		 */
		//apply need not be changed because that function is default
		super.addToObjectList(new RecomputationOfSplitTreeAndWSPDFunction(), "Every iteration both new");
		
		/*
		 * Logarithmically often new
		 * 
		 * Please set a and b in the for-loops as wanted.
		 * a = prefactor for the logarithm [the greater the more often it is computed new]
		 * b = presummand for currentIteration (this sum is the argument for/within the logarithm,
		 * thus b is the horizontal shift of the function mathematically)
		 * [the greater the rarer it is computed new, must not be smaller than 0]
		 */
		for(double a=4; a<=10; a=a+1){
			for(double b=0; b<=5; b=b+5){
				final double aFinal = a;
				final double bFinal = b;
				
				/*
				 * 1. Implemenation: Without update of barycenters
				 */
				super.addToObjectList(
						new RecomputationOfSplitTreeAndWSPDFunction(){
							@Override
							public Boolean apply(LayoutWithWSPD<?> layout) {
								int smallerValue = (int) (Math.log(bFinal+layout.getCurrentIteration())*aFinal);
								int largerValue = (int) (Math.log(bFinal+(layout.getCurrentIteration()+1))*aFinal);
								return smallerValue!=largerValue;
							}
						}, "After "+a+"ln("+b+"+currIter) new");
				/*
				 * 2. Implemenation: With update of barycenters
				 */
				super.addToObjectList(
						new RecomputationOfSplitTreeAndWSPDFunction(){
							@Override
							public Boolean apply(LayoutWithWSPD<?> layout) {
								int smallerValue = (int) (Math.log(bFinal+layout.getCurrentIteration())*aFinal);
								int largerValue = (int) (Math.log(bFinal+layout.getCurrentIteration()+1)*aFinal);
								boolean computeNew = smallerValue!=largerValue;
								if(!computeNew){
									layout.updateBarycenters();
								}
								return computeNew;
							}
						}, "After "+a+"ln("+b+"+currIter) new, else update barycenters");
			}
		}
		
		/*
		 * A sMin in depending on the factor c and the s used in the layout is determined.
		 * Every well-separated pair of the WSPD should be well-separated regarding sMin.
		 * If it is so, the SplitTree and the WSPD remain in use, otherwise they are computed new.
		 * For that the bounding rectangles in the SplitTree must be updated anyway.
		 * 
		 * Idea for the future:
		 * Change it that is not dependend only on one value, but compute new as soon as
		 * a rate of "bad" (too close) distances is reached, or make it depending on sth like the mean/variance/...
		 */
		//c = Factor, how much worse (portion of s) the s is allowed to become until st and WSPD are computed new
		for(double c=0.6; c<=0.95; c=c+0.5*(1-c)){
			final double cFinal = c;
			/*
			 * 1. Implemenation: Without update of barycenters
			 */
			super.addToObjectList(
					new RecomputationOfSplitTreeAndWSPDFunction(){			
						@Override
						public Boolean apply(LayoutWithWSPD<?> layout) {
							double sMin = layout.getSForTheWSPD()*cFinal;
							layout.updateBoundingRectangles();
							//check all distances
							for(WellSeparatedPairDecomposition<?> wspd: layout.getWSPD()){
								for(SplitTreeNode<?>[] pair: wspd.getWellSeparatedPairs()){
									if(!WellSeparatedPairDecomposition.isWellSeparated(pair[0], pair[1], sMin)){
										return true;
									}
								}
							}
							return false;
						}
					}, "When 1 ws-pair not well-sparated w.r. to s*"+c+" new");
			/*
			 * 2. Implemenation: With update of barycenters
			 */
			super.addToObjectList(
					new RecomputationOfSplitTreeAndWSPDFunction(){			
						@Override
						public Boolean apply(LayoutWithWSPD<?> layout) {
							double sMin = layout.getSForTheWSPD()*cFinal;
							layout.updateBoundingRectangles();
							//check all distances
							for(WellSeparatedPairDecomposition<?> wspd: layout.getWSPD()){
								for(SplitTreeNode<?>[] pair: wspd.getWellSeparatedPairs()){
									if(!WellSeparatedPairDecomposition.isWellSeparated(pair[0], pair[1], sMin)){
										return true;
									}
								}
							}
							layout.updateBarycenters();
							return false;
						}
					}, "When 1 ws-pair not well-sparated w.r. to s*"+c+" new, else update barycenters");
		}
		
		
		
		
		
		
		
		
		
		
		//At last set the names also to all functions (automatically)
		for(int i=0; i<super.getObjectCountWithoutInitializing(); i++){
			Tuple<RecomputationOfSplitTreeAndWSPDFunction, String> t = super.getObjectWithObjectnameWithoutInitializing(i);
			t.get1().setName(t.get2());
		}
	}
}
