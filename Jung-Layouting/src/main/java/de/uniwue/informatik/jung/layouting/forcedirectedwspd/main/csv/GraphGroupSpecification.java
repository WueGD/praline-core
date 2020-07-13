package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

import java.util.List;

public class GraphGroupSpecification extends RowOrColumnSpecification {
	List<String> pathToTheCompuationDataForThisGraph;

	public GraphGroupSpecification(String nameOfTheRowOrColumn, Statistic methodOfCombining,
			List<String> pathToTheCompuationDataForThisGraph) {
		super(methodOfCombining, nameOfTheRowOrColumn);
		this.pathToTheCompuationDataForThisGraph = pathToTheCompuationDataForThisGraph;
	}

	public List<String> getPathToTheCompuationDataForThisGraph() {
		return pathToTheCompuationDataForThisGraph;
	}
}
