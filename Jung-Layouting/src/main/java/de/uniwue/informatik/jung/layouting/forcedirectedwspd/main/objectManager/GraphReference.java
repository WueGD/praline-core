package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.TestOfGraphDrawing;

/**
 * Object for specification of a set of graphs.
 * <br>
 * Stores those information:
 * <ul>
 *  <li>
 *   file system location or instruction for creation
 *  </li>
 *  <li>
 *   number of graphs
 *  </li>
 *  <li>
 *   short description of the graph set
 *  </li>
 * </ul>
 *
 */
public class GraphReference {
	
	/**
	 * If true, then {@link GraphReference#directoryOrMethodCallParameter} is the name
	 * of the directory in the file system relative to {@link TestOfGraphDrawing#pfadDerTestgraphen},
	 * where these graphs are saved.
	 * <p>
	 * If false, then there is no directory with that name.
	 * Instead {@link GraphReference#directoryOrMethodCallParameter} is the String-instruction
	 * for {@link GraphManager#getGraphViaMethod(String, java.util.Scanner)} (must be passed as parameter).
	 * By calling this the graphs are created there (Can be a different set every time it is called with that String).
	 */
	private boolean graphsAreStoredInDirectory;
	
	/**
	 * Name of the directory (relative to {@link TestOfGraphDrawing#pfadDerTestgraphen}) where
	 * the set of graphs is saved or the insturction code for {@link GraphManager#getGraphViaMethod(String, java.util.Scanner)}.
	 * See {@link GraphReference#isGraphsAreStoredInDirectory()} for that.
	 */
	private String directoryOrMethodCallParameter;
	
	private int numberOfGraphs;
	
	private String shortDescription;

	/**
	 * 
	 * @param graphsAreStoredInDirectory <br>
	 * 		See {@link GraphReference#isGraphsAreStoredInDirectory()}
	 * @param directoryOrMethodCallParameter <br>
	 * 		See {@link GraphReference#getDirectoryOrMethodCallParameter()}
	 * @param numberOfGraphs
	 * @param shortDescription
	 */
	public GraphReference(boolean graphsAreStoredInDirectory,
			String directoryOrMethodCallParameter, int numberOfGraphs, String shortDescription) {
		this.graphsAreStoredInDirectory = graphsAreStoredInDirectory;
		this.directoryOrMethodCallParameter = directoryOrMethodCallParameter;
		this.numberOfGraphs = numberOfGraphs;
		this.shortDescription = shortDescription;
	}

	/**
	 * If true, then {@link GraphReference#directoryOrMethodCallParameter} is the name
	 * of the directory in the file system relative to {@link TestOfGraphDrawing#pfadDerTestgraphen},
	 * where these graphs are saved.
	 * <p>
	 * If false, then there is no directory with that name.
	 * Instead {@link GraphReference#directoryOrMethodCallParameter} is the String-instruction
	 * for {@link GraphManager#getGraphViaMethod(String, java.util.Scanner)} (must be passed as parameter).
	 * By calling this the graphs are created there (Can be a different set every time it is called with that String).
	 */
	public boolean isGraphsAreStoredInDirectory() {
		return graphsAreStoredInDirectory;
	}

	/**
	 * Name of the directory (relative to {@link TestOfGraphDrawing#pfadDerTestgraphen}) where
	 * the set of graphs is saved or the insturction code for {@link GraphManager#getGraphViaMethod(String, java.util.Scanner)}.
	 * See {@link GraphReference#isGraphsAreStoredInDirectory()} for that.
	 */
	public String getDirectoryOrMethodCallParameter() {
		return directoryOrMethodCallParameter;
	}

	public int getNumberOfGraphs() {
		return numberOfGraphs;
	}

	public String getShortDescription() {
		return shortDescription;
	}
	
	
}
