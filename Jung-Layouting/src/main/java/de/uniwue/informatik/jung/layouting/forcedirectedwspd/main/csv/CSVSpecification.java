package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

import java.util.List;

public class CSVSpecification {
	
	private List<GraphGroupSpecification> graphGroups;
	private DimensionCSVComputationData graphGroupDimension;
	
	private List<AlgorithmGroupSpecification> algorithmGroups;
	private DimensionCSVComputationData algorithmGroupDimension;
	
	private List<RunningTimeOrQualityCriterions> runningTimeOrQualityCriterions;
	private DimensionCSVComputationData runningTimeOrQualityCriterionsDimension;
	
	private NormalizationInCSV normalizationInCSV;
	private Statistic normalizationBy;
	
	private boolean numberingColumn;
	
	public CSVSpecification(
			List<GraphGroupSpecification> graphGroups,
			DimensionCSVComputationData graphGroupDimension,
			List<AlgorithmGroupSpecification> algorithmGroups,
			DimensionCSVComputationData algorithmGroupDimension,
			List<RunningTimeOrQualityCriterions> runningTimeOrQualityCriterions,
			DimensionCSVComputationData runningTimeOrQualityCriterionsDimension,
			NormalizationInCSV normalizationInCSV,
			Statistic normalizationBy,
			boolean numberingColumn) {
		this.graphGroups = graphGroups;
		this.graphGroupDimension = graphGroupDimension;
		this.algorithmGroups = algorithmGroups;
		this.algorithmGroupDimension = algorithmGroupDimension;
		this.runningTimeOrQualityCriterions = runningTimeOrQualityCriterions;
		this.runningTimeOrQualityCriterionsDimension = runningTimeOrQualityCriterionsDimension;
		this.normalizationInCSV = normalizationInCSV;
		this.normalizationBy = normalizationBy;
		this.numberingColumn = numberingColumn;
	}
	
	public List<? extends RowOrColumnSpecification> getSpecificationForDimension(DimensionCSVComputationData d){
		if(d==graphGroupDimension){
			return graphGroups;
		}
		if(d==algorithmGroupDimension){
			return algorithmGroups;
		}
		if(d==runningTimeOrQualityCriterionsDimension){
			return runningTimeOrQualityCriterions;
		}
		return null;
	}

	public List<GraphGroupSpecification> getGraphGroups() {
		return graphGroups;
	}


	public DimensionCSVComputationData getGraphGroupDimension() {
		return graphGroupDimension;
	}


	public List<AlgorithmGroupSpecification> getAlgorithmGroups() {
		return algorithmGroups;
	}


	public DimensionCSVComputationData getAlgorithmGroupDimension() {
		return algorithmGroupDimension;
	}


	public List<RunningTimeOrQualityCriterions> getRunningTimeOrQualityCriterions() {
		return runningTimeOrQualityCriterions;
	}


	public DimensionCSVComputationData getRunningTimeOrQualityCriterionsDimension() {
		return runningTimeOrQualityCriterionsDimension;
	}

	public NormalizationInCSV getNormalizationInCSV() {
		return normalizationInCSV;
	}

	public Statistic getNormalizationBy() {
		return normalizationBy;
	}
	
	public boolean hasNumberingColumn() {
		return numberingColumn;
	}
}
