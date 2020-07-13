package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import edu.uci.ics.jung.algorithms.util.IterativeContext;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

public class ComputationDataPlusNumberOfIterations extends ComputationData {

	/*
	 * Arrays, where the computed values should be written in
	 */
	
	private Integer[][][][] numberOfIterations;

	
	public ComputationDataPlusNumberOfIterations(int numberOfVertices,
			int numberOfEdges, ComputationMetaData metadata) {
		super(numberOfVertices, numberOfEdges, metadata);
		
		//set length of array in the same way as in the super-constructor
		numberOfIterations = new Integer[metadata.getAlgorithms().length][0][0][0];
		for(int i=0; i<metadata.getAlgorithms().length; i++){
			int jLength = 1; //basic case (no WSPD)
			int kLength = 1; //basic case (not WSPD or Quadtree)
			if(metadata.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.WITH_WSPD){
				jLength = metadata.getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions().length;
				kLength = metadata.getNumberOfDifferentSValues();
			}
			else if(metadata.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
				kLength = metadata.getNumberOfDifferentThetaValues();
			}
			numberOfIterations[i] = new Integer[jLength][kLength][metadata.getNumberOfSameTest()];
		}
	}

	/**
	 * Returns the number of iterations that were needed for drawing the graph (by an graphdrawing-algorithm of {@link IterativeContext})
	 * 
	 * The parameters of the desired drawing must be specified by these indices:
	 * 
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 * @return
	 */
	public int getNumberOfIterations(int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return numberOfIterations[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}


	public void setNumberOfIterations(int numberOfIterations,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		this.numberOfIterations[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun] = numberOfIterations;
	}
	
	
	/**
	 * Computes the selected statistic st from the following data:
	 * 
	 * All values specified by those 4 indices.
	 * Thereby an index can either be an exact value (index>=0)
	 * or can be kept open and all valid values for it are included (index<0).
	 * 
	 * To compute the statistic from more than one value at least one index should be <0.
	 * 
	 * @param st
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 * @return
	 */
	public double getStatisticFromNumberOfIterations(Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return get(st, numberOfIterations, indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
	}
}
