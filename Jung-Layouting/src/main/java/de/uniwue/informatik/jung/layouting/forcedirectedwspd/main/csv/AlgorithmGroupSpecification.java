package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

import java.util.List;

public class AlgorithmGroupSpecification extends RowOrColumnSpecification {
	
	
	private List<AlgorithmSpecification> combinedAlgorithmSpecifications;
	
	
	public AlgorithmGroupSpecification(String nameOfTheRowOrColumn, Statistic methodOfCombining,
			List<AlgorithmSpecification> combinedAlgorithmSpecifications) {
		super(methodOfCombining, nameOfTheRowOrColumn);
		this.combinedAlgorithmSpecifications = combinedAlgorithmSpecifications;
	}


	public List<AlgorithmSpecification> getCombinedAlgorithmSpecifications() {
		return combinedAlgorithmSpecifications;
	}
	
}
