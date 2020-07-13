package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

public class TestConfig {
	public static class RecomputationFunction {
		int a;
		int b;
		boolean updateBarycenters;
	}
	
	public int mode = 0;

	public String[] selectedGraphs;
	public String[] selectedAlgorithms;
	public String[] externCPlusPlusAlgPaths;

	public int selectedSGrowth = -1;
	public double smallestS = -1;
	public double increasingValueOfS = -1;
	public int numberOfSValues = -1;
	public RecomputationFunction[] selectedRecompFunctions;

	public int selectedThetaGrowth = -1;
	public double smallestTheta = -1;
	public double increasingValueOfThetas = -1;
	public int numberOfThetaValues = -1;

	/**
	 * number of runs (number of tests with all other parameters kept the same)
	 */
	public int numberOfTests = 0;
	
	public String[] qualityCriterions;
	public boolean qualityCriterionAngle;
	public boolean qualityCriterionCrossings;

	public int maxNodesFRExact;
}
