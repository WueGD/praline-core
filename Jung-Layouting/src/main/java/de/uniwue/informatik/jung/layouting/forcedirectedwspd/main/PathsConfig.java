package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

import java.io.File;

/**
 * In one instance there are the paths used to read and write relative to this project location defined.
 * Here there are also the default values defined (in the source code).
 * 
 * These paths can be set to values relative to the location of this Java-project (do not start with File.separator)
 */
public class PathsConfig {
	
	/*
	 * Default-values are specified here!
	 */
	public static final String DEFAULT_PATH_TO_GRAPHS = "Jung-Layouting/data/graphs";
	public static final String DEFAULT_PATH_TO_GRAPH_SPECIFICATIONS = "Jung-Layouting/data/graphs/specifications";
	public static final String DEFAULT_PATH_FOR_IPE_EXPORT = "Jung-Layouting/target/ipe-files";
	public static final String DEFAULT_PATH_TO_COMPUTATION_DATA = "Jung-Layouting/target/computation_data";
	public static final String DEFAULT_PATH_TO_C_PLUS_PLUS_RESULTS = "Jung-Layouting/data/C++_results";
	public static final String
			DEFAULT_PATH_TO_SAVE_CSV_FILES_FROM_SPLIT_TREE_TEST = "Jung-Layouting/target/split-tree-test";

	public String pathToGraphs = DEFAULT_PATH_TO_GRAPHS;
	public String pathToGraphSpecifications = DEFAULT_PATH_TO_GRAPH_SPECIFICATIONS;
	public String pathForIpeExport = DEFAULT_PATH_FOR_IPE_EXPORT;
	public String pathToComputationData = DEFAULT_PATH_TO_COMPUTATION_DATA;
	public String pathToCPlusPlusResults = DEFAULT_PATH_TO_C_PLUS_PLUS_RESULTS;
	public String pathToSaveCSVFilesFromSplitTreeTest = DEFAULT_PATH_TO_SAVE_CSV_FILES_FROM_SPLIT_TREE_TEST;
	
	
	public boolean containsNullValues(){
		if(pathToGraphs==null){
			return true;
		}
		if(pathToGraphSpecifications==null){
			return true;
		}
		if(pathForIpeExport==null){
			return true;
		}
		if(pathToComputationData==null){
			return true;
		}
		if(pathToCPlusPlusResults==null){
			return true;
		}
		if(pathToSaveCSVFilesFromSplitTreeTest==null){
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 * Number of replaced values
	 */
	public int replaceNullValuesWithDefaultValues(){
		PathsConfig defaultValues = new PathsConfig();
		int counter = 0;
		
		if(pathToGraphs==null){
			this.pathToGraphs = defaultValues.pathToGraphs;
			++counter;
		}
		if(pathToGraphSpecifications==null){
			this.pathToGraphSpecifications = defaultValues.pathToGraphSpecifications;
			++counter;
		}
		if(pathForIpeExport==null){
			this.pathForIpeExport = defaultValues.pathForIpeExport;
			++counter;
		}
		if(pathToComputationData==null){
			this.pathToComputationData = defaultValues.pathToComputationData;
			++counter;
		}
		if(pathToCPlusPlusResults==null){
			this.pathToCPlusPlusResults = defaultValues.pathToCPlusPlusResults;
			++counter;
		}
		if(pathToSaveCSVFilesFromSplitTreeTest==null){
			this.pathToSaveCSVFilesFromSplitTreeTest = defaultValues.pathToSaveCSVFilesFromSplitTreeTest;
			++counter;
		}
		return counter;
	}
	
	public void createSaveDirectoriesThatDoNotExist(){
		File dirForIpeExport = new File(pathForIpeExport);
		if(!dirForIpeExport.exists()){
			dirForIpeExport.mkdirs();
		}
		
		File dirToComputationData = new File(pathToComputationData);
		if(!dirToComputationData.exists()){
			dirToComputationData.mkdirs();
		}

		File dirToCPlusPlusResults = new File(pathToCPlusPlusResults);
		if(!dirToCPlusPlusResults.exists()){
			dirToCPlusPlusResults.mkdirs();
		}
		
		File dirToSaveCSVFilesFromSplitTreeTest = new File(pathToSaveCSVFilesFromSplitTreeTest);
		if(!dirToSaveCSVFilesFromSplitTreeTest.exists()){
			dirToSaveCSVFilesFromSplitTreeTest.mkdirs();
		}
	}
}
