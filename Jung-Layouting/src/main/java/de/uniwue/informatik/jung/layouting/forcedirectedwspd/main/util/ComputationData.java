package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.TestConfig;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityCriterion;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityTesterForLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

import java.util.LinkedList;
import java.util.List;

/**
 * In one instance of this class there are informations saved that are measured/computed while/after drawing exactly one graph.
 * The information can come from different drawings of this graph. The run one drawing was gotten can be specified by
 * <ul>
 *  <li>Algorithm</li>
 *  <li>{@link RecomputationOfSplitTreeAndWSPDFunction} if the drawing algorithm works with {@link WellSeparatedPairDecomposition}
 *  (then it is a {@link LayoutWithWSPD} and it is {@link AlgorithmType#WITH_WSPD})</li>
 *  <li>A s for the {@link WellSeparatedPairDecomposition} if the drawing algorithm is {@link LayoutWithWSPD} / {@link AlgorithmType#WITH_WSPD}
 *  or a theta if the drawing algorithm is {@link FRQuadtree} / {@link AlgorithmType#WITH_QUADTREE}</li>
 *  <li>By the number of the run with same conditions (same values for the 3 points listed before that but different starting
 *  positions for the points in the drawing algorithm in each such run)</li>
 * </ul>
 * <p>
 * Note:<br>
 * The information which algorithm, {@link RecomputationOfSplitTreeAndWSPDFunction} and s/theta is used is not saved here.
 * For that an instance of {@link ComputationMetaData} is needed.
 * One instance of {@link ComputationData} can have only one instance of {@link ComputationMetaData} as its
 * object defining the frame parameters, but one instance of {@link ComputationMetaData} can be the meta data object
 * for many {@link ComputationData} instances.
 */
public class ComputationData {
	/*
	 * graph specific properties
	 */
	
	private int numberOfVertices;
	private int numberOfEdges;	
	
	
	
	
	/*
	 * Arrays in which the computation data is written
	 */
	
	private Long[][][][] cpuTimeInNs;
	private Long[][][][] cpuTimeRepulsionInNs;
	private Integer[][][][] numberOfCrossings;
	private Double[][][][][][] qualityProperties;
	private Double[][][][] smallestAngle;
	
	
	
	
	
	
	/*
	 * Constructor
	 */
	
	
	public ComputationData(int numberOfVertices, int numberOfEdges, ComputationMetaData metadata) {
		
		//1. save parameters
		this.numberOfVertices = numberOfVertices;
		this.numberOfEdges = numberOfEdges;
		
		//2. init arrays
		//2.1 array sizes according to number of algorithms
		cpuTimeInNs = new Long[metadata.getAlgorithms().length][0][0][0];
		cpuTimeRepulsionInNs = new Long[metadata.getAlgorithms().length][0][0][0];
		numberOfCrossings = new Integer[metadata.getAlgorithms().length][0][0][0];
		//in the following line statistics.values.length-1 is used because standard deviation is not saved explicitly but implicitly in the variance
		qualityProperties = new Double[QualityCriterion.values().length][Statistic.values().length-1][metadata.getAlgorithms().length][0][0][0];
		smallestAngle = new Double[metadata.getAlgorithms().length][0][0][0];
		//2.2 array sizes according to the type of the algorithms
		for(int i=0; i<metadata.getAlgorithms().length; i++){
			//obtain sizes
			int jLength = 1; //basic case (if it is no algorithm with WSPD)
			int kLength = 1; //basic case (if it is no algorithm with WSPD or quadtree)
			if(metadata.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.WITH_WSPD){
				jLength = metadata.getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions().length;
				kLength = metadata.getNumberOfDifferentSValues();
			}
			else if(metadata.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
				kLength = metadata.getNumberOfDifferentThetaValues();
			}
			//use sizes to define the length of the arrays
			cpuTimeInNs[i] = new Long[jLength][kLength][metadata.getNumberOfSameTest()];
			cpuTimeRepulsionInNs[i] = new Long[jLength][kLength][metadata.getNumberOfSameTest()];
			numberOfCrossings[i] = new Integer[jLength][kLength][metadata.getNumberOfSameTest()];
			for(QualityCriterion qc: QualityCriterion.values()){
				for(Statistic st: Statistic.values()){
					//following line because standard deviation is not saved explicitly but implicitly in the variance
					if(st!=Statistic.STANDARD_DEVIATION){
						qualityProperties[qc.ordinal()][st.ordinal()][i] = new Double[jLength][kLength][metadata.getNumberOfSameTest()];
					}
				}
			}
			smallestAngle[i] = new Double[jLength][kLength][metadata.getNumberOfSameTest()];
		}
		
	}
	
	
	
	
	/*
	 * Getters and Setters
	 */
	
	/**
	 * Returns the time, better said CPU-time, in nanoseconds that was needed by the thread drawing the graph.
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
	public long getCpuTimeInNs(int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return cpuTimeInNs[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}
	
	public void setCpuTimeInNs(long cpuTimeInNS, int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		this.cpuTimeInNs[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun] = cpuTimeInNS;
	}
	
	public long getCpuTimeRepulsionInNs(int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return cpuTimeRepulsionInNs[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}
	
	public void setCpuTimeRepulsionInNs(long cpuTimeRepulsionInNS, int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		this.cpuTimeRepulsionInNs[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun] = cpuTimeRepulsionInNS;
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
	public double getStatisticFromCpuTimeInNS(Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return get(st, cpuTimeInNs, indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
	}
	
	/**
	 * Returns the number of crossings in one straight-line-drawing of the graph this {@link ComputationData}-instance belongs to.
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
	public int getNumberOfCrossings(int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return numberOfCrossings[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}
	
	public void setNumberOfCrossings(int numberOfCrossings, int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		this.numberOfCrossings[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun] = numberOfCrossings;
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
	public double getStatisticFromNumberOfCrossings(Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return get(st, numberOfCrossings, indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
	}
	
	/**
	 * Returns the property of one drawing regarding the {@link Statistic} st in the {@link QualityCriterion} qc.
	 * 
	 * The parameters of the desired drawing must be specified by these indices:
	 * 
	 * @param qc
	 * @param st
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 * @return
	 */
	public double getQualityCriterionStatistic(QualityCriterion qc, Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		//following line because standard deviation is not saved explicitly but implicitly in the variance
		if(st==Statistic.STANDARD_DEVIATION){
			double variance = qualityProperties[qc.ordinal()][Statistic.VARIANCE.ordinal()]
					[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
			return Math.sqrt(variance); //return standard deviation
		}
		return qualityProperties[qc.ordinal()][st.ordinal()][indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}
	
	/**
	 * Warning! For st = {@link Statistic#STANDARD_DEVIATION} nothing is saved (set).
	 * This is because {@link ComputationData} does not save {@link Statistic#STANDARD_DEVIATION} explicitely
	 * but implicitely via {@link Statistic#VARIANCE} (It is the squareroot of the variance).
	 * To change both set desired value via st = {@link Statistic#VARIANCE}.
	 * 
	 * @param newValueToBeSet
	 * @param qc
	 * @param st
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 */
	public void setQualityCriterionStatistic(double newValueToBeSet, QualityCriterion qc, Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		if(st==Statistic.STANDARD_DEVIATION){
			return;
		}
		
		this.qualityProperties[qc.ordinal()][st.ordinal()][indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun]
				= newValueToBeSet;
	}
	
	/**
	 * Computes the selected statistic stForTheDesiredComputationNow from the following data:
	 * 
	 * All values specified by qcForSpecificationOfSelectedSet, stForSpecificationOfSelectedSet
	 * and those 4 indices.
	 * Thereby an index can either be an exact value (index>=0)
	 * or can be kept open and all valid values for it are included (index<0).
	 * 
	 * To compute the statistic from more than one value at least one index should be <0.
	 * 
	 * @param stForTheDesiredComputationNow
	 * @param qcForSpecificationOfSelectedSet
	 * @param stForSpecificationOfSelectedSet
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 * @return
	 */
	public double getStatisticFromQualityCriterionStatistic(Statistic stForTheDesiredComputationNow,
			QualityCriterion qcForSpecificationOfSelectedSet, Statistic stForSpecificationOfSelectedSet,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		//following line because standard deviation is not saved explicitly but implicitly in the variance
		if(stForSpecificationOfSelectedSet==Statistic.STANDARD_DEVIATION){
			return Math.sqrt(get(stForTheDesiredComputationNow,
				qualityProperties[qcForSpecificationOfSelectedSet.ordinal()][Statistic.VARIANCE.ordinal()],
				indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun));
		}
		return get(stForTheDesiredComputationNow,
				qualityProperties[qcForSpecificationOfSelectedSet.ordinal()][stForSpecificationOfSelectedSet.ordinal()],
				indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
	}
	
	/**
	 * Returns the smallest angle in the radian measure
	 * in one straight-line-drawing of the graph this {@link ComputationData}-instance belongs to.
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
	public double getSmallestAngle(int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return smallestAngle[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun];
	}

	public void setSmallestAngle(double smallestAngleInTheRadianMeasure, int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction,
			int indexSOrTheta, int indexNumberOfTheRun){
		this.smallestAngle[indexAlgorithm][indexRecomputationOfSplitTreeAndWSPDFunction][indexSOrTheta][indexNumberOfTheRun] = smallestAngleInTheRadianMeasure;
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
	public double getStatisticFromSmallestAngle(Statistic st,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		return get(st, smallestAngle, indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
	}
	
	/**
	 * Computes the selected statistic st from the following data:
	 * 
	 * All values specified by those 4 indices and the passed array.
	 * Thereby an index can either be an exact value (index>=0)
	 * or can be kept open and all valid values for it are included (index<0).
	 * 
	 * To compute the statistic from more than one value at least one index should be <0.
	 * 
	 * @param st
	 * @param array
	 * @param indexAlgorithm
	 * @param indexRecomputationOfSplitTreeAndWSPDFunction	functions determining in which iteration of the {@link FRWSPD}-algorithm
	 * 										{@link SplitTree} and {@link WellSeparatedPairDecomposition} should be computet new
	 * @param indexSOrTheta
	 * @param indexNumberOfTheRun	Every specification of the 3 parameters before can be taken for more than one drawing.
	 * 								This index must be in [0; {@link ComputationMetaData#getNumberOfSameTest()} - 1].
	 * @return
	 */
	protected double get(Statistic st, Number[][][][] array,
			int indexAlgorithm, int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun){
		
		int objectCounter = 0;
		double[] computationValues = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0};
		List<Double> allValues = new LinkedList<Double>();
		int numberOfNaNValues = 0;
		
		int iMin = indexAlgorithm<0 ? 0 : indexAlgorithm;
		int iMax = indexAlgorithm<0 ? array.length : indexAlgorithm+1;
		for(int i=iMin; i<iMax; i++){
			int jMin = indexRecomputationOfSplitTreeAndWSPDFunction<0 ? 0 : indexRecomputationOfSplitTreeAndWSPDFunction;
			int jMax = indexRecomputationOfSplitTreeAndWSPDFunction<0 ? array[i].length : indexRecomputationOfSplitTreeAndWSPDFunction+1;
			for(int j=jMin; j<jMax; j++){
				int kMin = indexSOrTheta<0 ? 0 : indexSOrTheta;
				int kMax = indexSOrTheta<0 ? array[i][j].length : indexSOrTheta+1;
				for(int k=kMin; k<kMax; k++){
					int lMin = indexNumberOfTheRun<0 ? 0 : indexNumberOfTheRun;
					int lMax = indexNumberOfTheRun<0 ? array[i][j][k].length : indexNumberOfTheRun+1;
					for(int l=lMin; l<lMax; l++){
						objectCounter++;
						if(!Statistic.doCalculationsInTheLoop(
								st, (double) array[i][j][k][l], computationValues, allValues)){
							numberOfNaNValues++;
						}
					}
				}
			}
		}
		
		return Statistic.doFinalCalculationsOutsideTheLoop(
				st, objectCounter, computationValues, allValues, numberOfNaNValues);
	}
	

	public void readAllQualityInformationsAndSaveThem(QualityTesterForLayout<?, ?> qfg, int indexAlgorithm,
			int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun) {
		readAllQualityInformationsAndSaveThem(qfg, indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction,
				indexSOrTheta, indexNumberOfTheRun, null);
	}
	
	public void readAllQualityInformationsAndSaveThem(QualityTesterForLayout<?, ?> qfg, int indexAlgorithm,
			int indexRecomputationOfSplitTreeAndWSPDFunction, int indexSOrTheta, int indexNumberOfTheRun, TestConfig tc){
		QualityCriterion[] crits = QualityCriterion.values();
		boolean calcAngle = true;
		boolean calcCrossings = true;
		if (tc != null) {
			crits = new QualityCriterion[tc.qualityCriterions.length];
			for (int i = 0; i < tc.qualityCriterions.length; i++) {
				crits[i] = QualityCriterion.valueOf(tc.qualityCriterions[i]);
			}
			calcAngle = tc.qualityCriterionAngle;
			calcCrossings = tc.qualityCriterionCrossings;
		}
		
		for(QualityCriterion qc: crits){
			for(Statistic st: Statistic.values()){
				//following line because standard deviation is not saved explicitly but implicitly in the variance
				if(st!=Statistic.STANDARD_DEVIATION){
					this.setQualityCriterionStatistic(qfg.get(qc, st), qc, st,
							indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction, indexSOrTheta, indexNumberOfTheRun);
				}
			}
		}
		if (calcAngle) {
			this.setSmallestAngle(qfg.getSmallestAngle(), indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction,
					indexSOrTheta, indexNumberOfTheRun);
		}
		if (calcCrossings) {
			this.setNumberOfCrossings(qfg.getNumberOfCrossings(), indexAlgorithm, indexRecomputationOfSplitTreeAndWSPDFunction,
					indexSOrTheta, indexNumberOfTheRun);
		}
	}
	
	
	
	
	
	
	/*
	 * Getters for properties of the drawn graph
	 */
	
	public int getNumberOfVertices() {
		return numberOfVertices;
	}


	public int getNumberOfEdges() {
		return numberOfEdges;
	}
}
