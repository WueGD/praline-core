package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

public class AlgorithmSpecification {
	private int indexAlgorithm;
	private int indexRecomputationFunction;
	private int indexSOrTheta;
	private int indexRun;
	
	public AlgorithmSpecification(int indexAlgorithm,
			int indexRecomputationFunction, int indexSOrTheta,
			int indexRun) {
		this.indexAlgorithm = indexAlgorithm;
		this.indexRecomputationFunction = indexRecomputationFunction;
		this.indexSOrTheta = indexSOrTheta;
		this.indexRun = indexRun;
	}

	public int getIndexAlgorithm() {
		return indexAlgorithm;
	}

	public int getIndexRecomputationFunction() {
		return indexRecomputationFunction;
	}

	public int getIndexSOrTheta() {
		return indexSOrTheta;
	}

	public int getIndexRun() {
		return indexRun;
	}
}
