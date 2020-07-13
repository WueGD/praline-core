package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import com.google.common.base.Function;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityCriterion;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationData;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationDataPlusNumberOfIterations;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.LinkedList;
import java.util.List;

public class RunningTimeOrQualityCriterions extends RowOrColumnSpecification 
		implements Function<Tuple<ComputationData, AlgorithmGroupSpecification>, Number>{
	
	private ComputationDataSelection evaluationCriterionSelection;
	private QualityCriterion qualityCriterion;
	private Statistic statistic;
	
	
	
	public RunningTimeOrQualityCriterions(String nameOfTheRowOrColumn,
			Statistic methodOfCombining, 
			ComputationDataSelection evaluationCriterionSelection,
			QualityCriterion qualityCriterion,
			Statistic statistic) {
		super(methodOfCombining, nameOfTheRowOrColumn);
		this.evaluationCriterionSelection = evaluationCriterionSelection;
		this.qualityCriterion = qualityCriterion;
		this.statistic = statistic;
	}



	@Override
	public Number apply(Tuple<ComputationData, AlgorithmGroupSpecification> input) {
		
		if(evaluationCriterionSelection == ComputationDataSelection.CPU_TIME_IN_NS){
			List<Double> allValues = new LinkedList<Double>();
			for(AlgorithmSpecification as: input.get2().getCombinedAlgorithmSpecifications()){
				allValues.add((double) input.get1().getCpuTimeInNs(as.getIndexAlgorithm(), as.getIndexRecomputationFunction(),
						as.getIndexSOrTheta(), as.getIndexRun()));
			}
			return Statistic.computeStatistic(input.get2().getMethodOfCombining(), allValues);
		}
		
		if(evaluationCriterionSelection == ComputationDataSelection.NUMBER_OF_CROSSINGS){
			List<Double> allValues = new LinkedList<Double>();
			for(AlgorithmSpecification as: input.get2().getCombinedAlgorithmSpecifications()){
				allValues.add((double) input.get1().getNumberOfCrossings(as.getIndexAlgorithm(), as.getIndexRecomputationFunction(),
						as.getIndexSOrTheta(), as.getIndexRun()));
			}
			return Statistic.computeStatistic(input.get2().getMethodOfCombining(), allValues);
		}
		
		if(evaluationCriterionSelection == ComputationDataSelection.QUALITY_CRITERIONS){
			List<Double> allValues = new LinkedList<Double>();
			for(AlgorithmSpecification as: input.get2().getCombinedAlgorithmSpecifications()){
				allValues.add(input.get1().getQualityCriterionStatistic(qualityCriterion, statistic, as.getIndexAlgorithm(),
						as.getIndexRecomputationFunction(), as.getIndexSOrTheta(), as.getIndexRun()));
			}
			return Statistic.computeStatistic(input.get2().getMethodOfCombining(), allValues);
		}		
		
		if(evaluationCriterionSelection == ComputationDataSelection.SMALLEST_ANGLE){
			List<Double> allValues = new LinkedList<Double>();
			for(AlgorithmSpecification as: input.get2().getCombinedAlgorithmSpecifications()){
				allValues.add(input.get1().getSmallestAngle(as.getIndexAlgorithm(), as.getIndexRecomputationFunction(),
						as.getIndexSOrTheta(), as.getIndexRun()));
			}
			return Statistic.computeStatistic(input.get2().getMethodOfCombining(), allValues);
		}
		
		if(evaluationCriterionSelection == ComputationDataSelection.NUMBER_OF_ITERATIONS){
			List<Double> allValues = new LinkedList<Double>();
			for(AlgorithmSpecification as: input.get2().getCombinedAlgorithmSpecifications()){
				if(ComputationDataPlusNumberOfIterations.class.isAssignableFrom(input.get1().getClass())){
					int numberOfIterations = ((ComputationDataPlusNumberOfIterations)input.get1()).getNumberOfIterations(
							as.getIndexAlgorithm(), as.getIndexRecomputationFunction(), as.getIndexSOrTheta(), as.getIndexRun());
					if(numberOfIterations != -1){
						//the saved number of iterations is not -1 and thus a valid, saved value
						allValues.add((double) numberOfIterations);
						continue; //all ok -> valid iteration-numbers are treated
					}
				}
				//else{
				/*
				 * min. 1 not valid number of iterations
				 * -> abort whole statistical evaluation and return -1
				 */
				return -1;
				//}
			}
			return Statistic.computeStatistic(input.get2().getMethodOfCombining(), allValues);
		}
		
		System.err.println("Should never reach this. "+Thread.currentThread().getStackTrace()[1]);
		return null;
	}



	public ComputationDataSelection getEvaluationCriterionSelection() {
		return evaluationCriterionSelection;
	}



	public QualityCriterion getQualityCriterion() {
		return qualityCriterion;
	}



	public Statistic getStatistic() {
		return statistic;
	}
}
