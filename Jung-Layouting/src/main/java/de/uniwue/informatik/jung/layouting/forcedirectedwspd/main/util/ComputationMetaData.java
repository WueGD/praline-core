package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;


/**
 * This class is there for defining the frame parameters for the instances of the clas {@link ComputationData}.
 * This shall avoid saving redundant information that remain the same in one computation run.
 * Instead the general information for one run can be saved in one file.
 * This makes it necessary to have the {@link ComputationMetaData} to interpret the {@link ComputationData} correctly.
 */
public class ComputationMetaData {
	
	private AlgorithmReference[] algorithms;
	private String[] namesOfTheRecomputationOfSplitTreeAndWSPDFunctions;
	/**
	 * else exponential growth
	 */
	private boolean linearGrowthOfS;
	private double sMin;
	private double sStepWidthOrBaseForExpGrowth;
	private int numberOfDifferentSValues;
	/**
	 * else exponential growth
	 */
	private boolean linearGrowthOfTheta;
	private double thetaMin;
	private double thetaStepWidthOrBaseForExpGrowth;
	private int numberOfDifferentThetaValues;
	/**
	 * Same in the sense of same conditions but different starting points for the vertices.
	 * So this determines the number of repetitions. 
	 */
	private int numberOfSameTest;
	
	
	/** 
	 * All values are set by calling of the constructor.
	 * There are no set()-methods.
	 * 
	 * @param algorithms
	 * Selected algorithms (is not checked)
	 * <p>
	 * {
	 * @param namesOfTheRecomputationOfSplitTreeAndWSPDFunctions
	 * @param linearGrowthOfS	else exponential growth
	 * @param sMin
	 * @param sStepWidthOrBaseForExpGrowth
	 * @param numberOfDifferentSValues
	 * <br>
	 * }:
	 * <br>
	 * Only relevant if there are algorithms with {@link AlgorithmType#WITH_WSPD}.
	 * Can have arbitrary values or null-values else, but
	 * then it is recommended to put null respectively -1 to show there
	 * is no such WSPD-drawing-algorithm selected.
	 * <p>
	 * {
	 * @param linearGrowthOfTheta	else exponential growth
	 * @param thetaMin
	 * @param thetaStepWidthOrBaseForExpGrowth
	 * @param numberOfDifferentThetaValues
	 * <br>
	 * }:
	 * <br>
	 * Only relevant if there are algorithms with {@link AlgorithmType#WITH_QUADTREE}.
	 * Can have arbitrary values else, but
	 * then it is recommended to put -1 to show there
	 * is no such Quadtree-drawing-algorithm selected.
	 * <p>
	 * @param numberOfSameTest
	 * Same in the sense of same conditions but different starting points for the vertices.
	 * So this determines the number of repetitions. 
	 */
	public ComputationMetaData(AlgorithmReference[] algorithms, String[] namesOfTheRecomputationOfSplitTreeAndWSPDFunctions,
			boolean linearGrowthOfS, double sMin, double sStepWidthOrBaseForExpGrowth,
			int numberOfDifferentSValues, boolean linearGrowthOfTheta, double thetaMin, double thetaStepWidthOrBaseForExpGrowth,
			int numberOfDifferentThetaValues, int numberOfSameTest) {
		this.algorithms = algorithms;
		this.namesOfTheRecomputationOfSplitTreeAndWSPDFunctions = namesOfTheRecomputationOfSplitTreeAndWSPDFunctions;
		this.linearGrowthOfS = linearGrowthOfS;
		this.sMin = sMin;
		this.sStepWidthOrBaseForExpGrowth = sStepWidthOrBaseForExpGrowth;
		this.numberOfDifferentSValues = numberOfDifferentSValues;
		this.linearGrowthOfTheta = linearGrowthOfTheta;
		this.thetaMin = thetaMin;
		this.thetaStepWidthOrBaseForExpGrowth = thetaStepWidthOrBaseForExpGrowth;
		this.numberOfDifferentThetaValues = numberOfDifferentThetaValues;
		this.numberOfSameTest = numberOfSameTest;
	}
	
	
	
	public AlgorithmReference[] getAlgorithms(){
		return algorithms;
	}


	public String[] getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions() {
		return namesOfTheRecomputationOfSplitTreeAndWSPDFunctions;
	}

	
	/**
	 * else: exponential growth
	 * 
	 * @return
	 */
	public boolean isLinearGrowthOfS() {
		return linearGrowthOfS;
	}


	public double getSMin() {
		return sMin;
	}

	/**
	 * StepWidthOrBaseForExpGrowth for the s of the {@link WellSeparatedPairDecomposition}
	 * 
	 * @return
	 */
	public double getSStepWidthOrBaseForExpGrowth() {
		return sStepWidthOrBaseForExpGrowth;
	}


	public int getNumberOfDifferentSValues() {
		return numberOfDifferentSValues;
	}
	
	/**
	 *  Returns the k-th s-Value (used for a {@link WellSeparatedPairDecomposition}).
	 *  Valid k-values are integers in the span [0; numberOfDifferentSValues -1 ] .
	 *  E.g. if k=0, then sMin is returned.
	 * 
	 * @param k
	 * @return
	 */
	public double getS(int k){
		//exp. growth
		if(!linearGrowthOfS){
			return sMin*Math.pow(sStepWidthOrBaseForExpGrowth, k);
		}
		//lin. growth
		return sMin+k*sStepWidthOrBaseForExpGrowth;
	}

	
	/**
	 * else: exponential growth
	 * 
	 * @return
	 */
	public boolean isLinearGrowthOfTheta() {
		return linearGrowthOfTheta;
	}



	public double getThetaMin() {
		return thetaMin;
	}



	public double getThetaStepWidthOrBaseForExpGrowth() {
		return thetaStepWidthOrBaseForExpGrowth;
	}



	public int getNumberOfDifferentThetaValues() {
		return numberOfDifferentThetaValues;
	}

	
	/**
	 *  Returns the k-th theta-Value (used for the {@link FRQuadtree}-algorithm).
	 *  Valid k-values are integers in the span [0; numberOfDifferentSValues -1 ] .
	 *  E.g. if k=0, then thetaMin is returned.
	 * 
	 * @param k
	 * @return
	 */
	public double getThetaWert(int k){
		//exp. wachstum
		if(!linearGrowthOfTheta){
			return thetaMin*Math.pow(thetaStepWidthOrBaseForExpGrowth, k);
		}
		//lin. wachstum
		return thetaMin+k*thetaStepWidthOrBaseForExpGrowth;
	}
	
	/**
	 * Same in the sense of same conditions but different starting points for the vertices. So this determines the number of repetitions. 
	 * 
	 * @return
	 */
	public int getNumberOfSameTest() {
		return numberOfSameTest;
	}
	
	/**
	 * Expectes an index (index of a desired algorithm in the algorithms-array one can get that by calling
	 * {@link ComputationMetaData#getAlgorithms()}) and returns for the algorithm determined by
	 * this index...
	 * <ul>
	 *  <li>
	 *  ... the k-th s if {@link AlgorithmReference#getAlgorithmType()}=={@link AlgorithmType#WITH_WSPD}
	 *  </li>
	 *  <li>
	 *  ... the k-th theta if {@link AlgorithmReference#getAlgorithmType()}=={@link AlgorithmType#WITH_QUADTREE}
	 *  </li>
	 *  <li>
	 *  ... -1 else
	 *  </li>
	 * 
	 * @param indexAlgorithm index of the desired algorithm in {@link ComputationMetaData#getAlgorithms()}
	 * @param k
	 * @return
	 */
	public double getSOrThetaOrMinus1(int indexAlgorithm, int k){
		if(algorithms[indexAlgorithm].getAlgorithmType()==AlgorithmType.WITH_WSPD){
			return getS(k);
		}
		if(algorithms[indexAlgorithm].getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
			return getThetaWert(k);
		}
		return -1;
	}
	
	/**
	 * Expectes an index (index of a desired algorithm in the algorithms-array one can get that by calling
	 * {@link ComputationMetaData#getAlgorithms()}) and returns for the algorithm determined by
	 * this index...
	 * <ul>
	 *  <li>
	 *  ... the number of different functions that determine if in the current iteration split
	 *  tree and WSPD are recomputed (computed again) if {@link AlgorithmReference#getAlgorithmType()}=={@link AlgorithmType#WITH_WSPD}
	 *  </li>
	 *  <li>
	 *  ... 1 else
	 *  </li>
	 * 
	 * @param indexAlgorithm index of the desired algorithm in {@link ComputationMetaData#getAlgorithms()}
	 * @return
	 */
	public int getNumberOfRecomputationOfSplitTreeAndWSPDFunctions(int indexAlgorithm){
		if(algorithms[indexAlgorithm].getAlgorithmType()==AlgorithmType.WITH_WSPD){
			return namesOfTheRecomputationOfSplitTreeAndWSPDFunctions.length;
		}
		return 1;
	}
	
	/**
	 * Expectes an index (index of a desired algorithm in the algorithms-array one can get that by calling
	 * {@link ComputationMetaData#getAlgorithms()}) and returns for the algorithm determined by
	 * this index...
	 * <ul>
	 *  <li>
	 *  ... the number of different s values used in this computation run
	 *  if {@link AlgorithmReference#getAlgorithmType()}=={@link AlgorithmType#WITH_WSPD}
	 *  </li>
	 *  <li>
	 *  ... the number of different theta values used in this computation run
	 *  if {@link AlgorithmReference#getAlgorithmType()}=={@link AlgorithmType#WITH_QUADTREE}
	 *  </li>
	 *  <li>
	 *  ... 1 else
	 *  </li>
	 * 
	 * @param indexAlgorithm index of the desired algorithm in {@link ComputationMetaData#getAlgorithms()}
	 * @return
	 */
	public int getSOrThetaLength(int indexAlgorithm){
		if(algorithms[indexAlgorithm].getAlgorithmType()==AlgorithmType.WITH_WSPD){
			return numberOfDifferentSValues;
		}
		if(algorithms[indexAlgorithm].getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
			return numberOfDifferentThetaValues;
		}
		return 1;
	}
	
	public int getMaxNumberOfRecomputationOfSplitTreeAndWSPDFunctions(){
		//if alg with wspd is contained
		for(AlgorithmReference ar: algorithms){
			if(ar.getAlgorithmType()==AlgorithmType.WITH_WSPD){
				return namesOfTheRecomputationOfSplitTreeAndWSPDFunctions.length;
			}
		}
		return 1;
	}
	public int getMaxSOrThetaLength(){
		int max = 1;
		for(AlgorithmReference ar: algorithms){
			if(ar.getAlgorithmType()==AlgorithmType.WITH_WSPD){
				max = Math.max(max, numberOfDifferentSValues);
			}
			else if(ar.getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
				max = Math.max(max, numberOfDifferentThetaValues);
			}
		}
		return max;
	}
}
