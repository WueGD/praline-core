package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd;

import com.google.common.base.Function;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

/**
 * This class implements the interface {@link Function}< {@link LayoutWithWSPD} , {@link Boolean} >, in the variant
 * com.google.common.base.Function (but should also work with {@link java.util.function.Function}).
 * <br>
 * Intended use is the use as function in {@link LayoutWithWSPD} to determine there
 * if in the current iteration a {@link SplitTree} and a {@link WellSeparatedPairDecomposition}
 * should be computed new or if the {@link SplitTree} and the {@link WellSeparatedPairDecomposition}
 * from the previous iteration should still be used in this iteration.
 * If it shall be constructed new then true is returned otherwise false is returned.
 * <p>
 * Default is to return always true (new built in every iteration).
 * If that should be changed overwrite the {@link RecomputationOfSplitTreeAndWSPDFunction#apply(LayoutWithWSPD)}-method.
 * In this overwriting properties of the {@link LayoutWithWSPD} can be read.
 * These are e.g. the number of the current iteration, the {@link WellSeparatedPairDecomposition}
 * (from the last iteration [or older]).
 * {@link SplitTree} and {@link WellSeparatedPairDecomposition} can also be modified (and then "false" should be returned).
 * In the first iteration (at the beginng) {@link SplitTree} and {@link WellSeparatedPairDecomposition} are always build
 * new in {@link FRWSPD} as there are no previous ones (does not depend on the used instance of this function).
 * <p>
 * Set a function to an algorithm working with WSPD by calling
 * {@link LayoutWithWSPD#setRecomputationOfSplitTreeAndWSPDFunction(RecomputationOfSplitTreeAndWSPDFunction)}.
 */
public class RecomputationOfSplitTreeAndWSPDFunction implements Function<LayoutWithWSPD<?>, Boolean> {
	
	private String name;
	
	/*
	 * Getters and Setters
	 */
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/*
	 * Constructors
	 */
	
	/**
	 * Warning. No name added by this constructor.
	 * Can be done afterwards via {@link RecomputationOfSplitTreeAndWSPDFunction#setName(String)}
	 */
	public RecomputationOfSplitTreeAndWSPDFunction() {
		super();
	}
	
	public RecomputationOfSplitTreeAndWSPDFunction(String name) {
		super();
		this.name = name;
	}
	
	/*
	 * apply()
	 */
	
	//Default-Function is defiened here. It is: Return always true
	@Override
	public Boolean apply(LayoutWithWSPD<?> layout) {
		return true;
	}
}
