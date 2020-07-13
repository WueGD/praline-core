package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.Testclass;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.Manager;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationData;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationDataPlusNumberOfIterations;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ComputationDataEvaluation {
	
	public static File assistantForCompDataDirectorySelection(Scanner sc){
		Manager<File> compDataDirectoryManager = new Manager<File>() {
			@Override
			protected void initialize() {
				File compDataDirectory = new File(Testclass.pathsConfig.pathToComputationData);
				File[] objectsInDir = compDataDirectory.listFiles();
				for(File f: objectsInDir){
					addToObjectList(f, f.getName());
				}
			}
		};
		
		return compDataDirectoryManager.assistantForSelectingOneObject(sc);
	}
	
	public static LinkedList<String> assistantForSubdirectorySelection(Scanner sc, final String path, boolean trueInclusenFalseExclusion){
		Manager<File> compDataDirectoryManager = new Manager<File>() {
			@Override
			protected void initialize() {
				File compDataDirectory = new File(path);
				File[] objectsInDir = compDataDirectory.listFiles();
				for(File f: objectsInDir){
					addToObjectList(f, f.getName());
				}
			}
		};
		
		List<File> selectedFiles;
		if(trueInclusenFalseExclusion){
			selectedFiles = compDataDirectoryManager.assistantForSelectingMultipleObjects(sc);
		}
		else{
			selectedFiles = compDataDirectoryManager.assistantForExcludingMultipleObjects(sc);
		}
		LinkedList<String> selectedPaths = new LinkedList<String>();
		for(File f: selectedFiles){
			selectedPaths.add(f.getPath());
		}
		return selectedPaths;
	}
	
	public static LinkedList<String> allPathsToCompDataFilesInPassedDirectories(List<String> directories){
		LinkedList<String> allPathsToCompDataFiles = new LinkedList<String>();
		for(String path: directories){
			File fileOrDir = new File(path);
			if(fileOrDir.isDirectory()){
				File[] subdirAndFiles = fileOrDir.listFiles();
				LinkedList<String> listSubdirAndFiles = new LinkedList<String>(); 
				for(File f: subdirAndFiles){
					listSubdirAndFiles.add(f.getPath());
				}
				allPathsToCompDataFiles.addAll(allPathsToCompDataFilesInPassedDirectories(listSubdirAndFiles));
			}
			else{
				//check english and german names
				if(path.endsWith(".json")
						&& !path.endsWith("berechnungsmetadaten.json")
						&& !path.endsWith("computationMetadata.json") && !path.endsWith("computationMetaData.json")
						&& !path.endsWith("CSVSpezifizierung.json") && !path.endsWith(CSVSpecification.class.getSimpleName()+".json")){
					allPathsToCompDataFiles.add(path);
				}
			}
		}
		return allPathsToCompDataFiles;
	}
	
	
	/**
	 * 
	 * @param pathCompDataDirectory
	 * @param csvFilename
	 * without ".csv" - that will be added automatically
	 * @param csvSpecification
	 */
	public static void generateAndWriteCSV(String pathCompDataDirectory, String csvFilename, CSVSpecification csvSpecification){
		/**
		 * That array is created for the values that have to be written in the file at the end.
		 * allCSVValues[numberOfLines][numberOfRows]
		 */
		double[][] allCSVValues = new double[csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.ROW_DESCRIPTION).size()]
				[csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1).size()
				 *csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART2).size()];
		
		/*
		 * read data and calc
		 */
		
		//graphs as row-description (row-dimension)
		if(csvSpecification.getGraphGroupDimension()==DimensionCSVComputationData.ROW_DESCRIPTION){
			for(int i=0; i<csvSpecification.getGraphGroups().size(); i++){
				GraphGroupSpecification ggs = csvSpecification.getGraphGroups().get(i);
				
				List<double[]> allRowVectors = new LinkedList<double[]>();
				for(String path: ggs.getPathToTheCompuationDataForThisGraph()){
					double[] rowVector = new double[allCSVValues[0].length];
					int index = 0;
					allRowVectors.add(rowVector);
					//Try first if it is of type with iterationsCount
					ComputationData compData = DataAsJSonFile.getFileContent(path, ComputationDataPlusNumberOfIterations.class);
					//if not then the number of it. can not be queried -> then data is simply of type compData
					try{
						((ComputationDataPlusNumberOfIterations)compData).getNumberOfIterations(0, 0, 0, 0);
					}
					catch(NullPointerException e){
						compData = DataAsJSonFile.getFileContent(path, ComputationData.class);
					}
					//in which order are the for-loops? That is found out with that if
					if(csvSpecification.getAlgorithmGroupDimension()==DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1){
						for(AlgorithmGroupSpecification ags: csvSpecification.getAlgorithmGroups()){
							for(RunningTimeOrQualityCriterions rqc: csvSpecification.getRunningTimeOrQualityCriterions()){
								rowVector[index] =
										rqc.apply(new Tuple<ComputationData, AlgorithmGroupSpecification>(compData, ags)).doubleValue();
								index++;
							}
						}
					}
					else{
						for(RunningTimeOrQualityCriterions rqc: csvSpecification.getRunningTimeOrQualityCriterions()){
							for(AlgorithmGroupSpecification ags: csvSpecification.getAlgorithmGroups()){
								rowVector[index] =
										rqc.apply(new Tuple<ComputationData, AlgorithmGroupSpecification>(compData, ags)).doubleValue();
								index++;
							}
						}
					}
				}
				
				//merge results
				for(int j=0; j<allCSVValues[i].length; j++){
					List<Double> jThPositionOfRowVectors = new LinkedList<Double>();
					for(double[] rowVector: allRowVectors){
						jThPositionOfRowVectors.add(rowVector[j]);
					}
					//save csv-value
					allCSVValues[i][j] = Statistic.computeStatistic(ggs.getMethodOfCombining(),
							jThPositionOfRowVectors);
				}
			}
		}
		//graphs as column-description (column-dimension)
		else{
			//algorithms as row-description (row-dimension)
			if(csvSpecification.getAlgorithmGroupDimension()==DimensionCSVComputationData.ROW_DESCRIPTION){
				for(int i=0; i<csvSpecification.getGraphGroups().size(); i++){
					GraphGroupSpecification ggs = csvSpecification.getGraphGroups().get(i);
					
					List<double[][]> allCombinations = new LinkedList<double[][]>();
					for(String path: ggs.getPathToTheCompuationDataForThisGraph()){
						double[][] combinationsMatrix = new double[csvSpecification.getAlgorithmGroups().size()]
								[csvSpecification.getRunningTimeOrQualityCriterions().size()];
						allCombinations.add(combinationsMatrix);
						//Try first if it is of type with iterationsCount
						ComputationData compData = DataAsJSonFile.getFileContent(path, ComputationDataPlusNumberOfIterations.class);
						//if not then the number of it. can not be queried -> then data is simply of type compData
						try{
							((ComputationDataPlusNumberOfIterations)compData).getNumberOfIterations(0, 0, 0, 0);
						}
						catch(NullPointerException e){
							compData = DataAsJSonFile.getFileContent(path, ComputationData.class);
						}
						int indexJ = 0;
						for(AlgorithmGroupSpecification ags: csvSpecification.getAlgorithmGroups()){
							int indexK = 0;
							for(RunningTimeOrQualityCriterions rqc: csvSpecification.getRunningTimeOrQualityCriterions()){
								combinationsMatrix[indexJ][indexK] =
										rqc.apply(new Tuple<ComputationData, AlgorithmGroupSpecification>(compData, ags)).doubleValue();
								indexK++;
							}
							indexJ++;
						}
					}
					
					//merge results
					for(int j=0; j<csvSpecification.getAlgorithmGroups().size(); j++){
						for(int k=0; k<csvSpecification.getRunningTimeOrQualityCriterions().size(); k++){
							List<Double> cellsInTheMatrices = new LinkedList<Double>();
							for(double[][] combinationsMatrix: allCombinations){
								cellsInTheMatrices.add(combinationsMatrix[j][k]);
							}
							//save csv-value
							//in which order are the for-loops? That is found out with that if
							if(csvSpecification.getGraphGroupDimension()==DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1){
								allCSVValues[j][i*csvSpecification.getRunningTimeOrQualityCriterions().size()+k] =
										Statistic.computeStatistic(ggs.getMethodOfCombining(), cellsInTheMatrices);
							}
							else{
								allCSVValues[j][k*csvSpecification.getGraphGroups().size()+i] =
										Statistic.computeStatistic(ggs.getMethodOfCombining(), cellsInTheMatrices);
							}
						}
					}
				}
			}
			//running time/quality criterions as row-description (row-dimension)
			else{
				for(int i=0; i<csvSpecification.getGraphGroups().size(); i++){
					GraphGroupSpecification ggs = csvSpecification.getGraphGroups().get(i);
					
					List<double[][]> allCombinations = new LinkedList<double[][]>();
					for(String path: ggs.getPathToTheCompuationDataForThisGraph()){
						double[][] combinationsMatrix = new double[csvSpecification.getRunningTimeOrQualityCriterions().size()]
								[csvSpecification.getAlgorithmGroups().size()];
						allCombinations.add(combinationsMatrix);
						//Try first if it is of type with iterationsCount
						ComputationData compData = DataAsJSonFile.getFileContent(path, ComputationDataPlusNumberOfIterations.class);
						//if not then the number of it. can not be queried -> then data is simply of type compData
						try{
							((ComputationDataPlusNumberOfIterations)compData).getNumberOfIterations(0, 0, 0, 0);
						}
						catch(NullPointerException e){
							compData = DataAsJSonFile.getFileContent(path, ComputationData.class);
						}
						int indexJ = 0;
						for(RunningTimeOrQualityCriterions rqc: csvSpecification.getRunningTimeOrQualityCriterions()){
							int indexK = 0;
							for(AlgorithmGroupSpecification ags: csvSpecification.getAlgorithmGroups()){
								combinationsMatrix[indexJ][indexK] =
										rqc.apply(new Tuple<ComputationData, AlgorithmGroupSpecification>(compData, ags)).doubleValue();
								indexK++;
							}
							indexJ++;
						}
					}
					
					//merge results
					for(int j=0; j<csvSpecification.getRunningTimeOrQualityCriterions().size(); j++){
						for(int k=0; k<csvSpecification.getAlgorithmGroups().size(); k++){
							List<Double> cellsInTheMatrices = new LinkedList<Double>();
							for(double[][] combinationsMatrix: allCombinations){
								cellsInTheMatrices.add(combinationsMatrix[j][k]);
							}
							//save csv-value
							//in which order are the for-loops? That is found out with that if
							if(csvSpecification.getGraphGroupDimension()==DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1){
								allCSVValues[j][i*csvSpecification.getAlgorithmGroups().size()+k] =
										Statistic.computeStatistic(ggs.getMethodOfCombining(), cellsInTheMatrices);
							}
							else{
								allCSVValues[j][k*csvSpecification.getGraphGroups().size()+i] =
										Statistic.computeStatistic(ggs.getMethodOfCombining(), cellsInTheMatrices);
							}
						}
					}
				}
			}
		}
		
		
		
		/*
		 * normalization (optional)
		 */
					
		if(csvSpecification.getNormalizationInCSV() == NormalizationInCSV.COLUMN_NORMALIZATION){
			for(int j=0; j<allCSVValues[0].length; j++){
				List<Double> listOfValuesInTheColumn = new LinkedList<Double>();
				for(int i=0; i<allCSVValues.length; i++){
					listOfValuesInTheColumn.add(allCSVValues[i][j]);
				}
				double normalizationValue = Statistic.computeStatistic(
						csvSpecification.getNormalizationBy(), listOfValuesInTheColumn);
				//old value is overwritten by normalized one
				for(int i=0; i<allCSVValues.length; i++){
					allCSVValues[i][j] = allCSVValues[i][j]/normalizationValue;
				}
			}
		}
		else if(csvSpecification.getNormalizationInCSV() == NormalizationInCSV.ROW_NORMALIZATION){
			for(int i=0; i<allCSVValues.length; i++){
				List<Double> listOfValuesInTheRow = new LinkedList<Double>();
				for(int j=0; j<allCSVValues[i].length; j++){
					listOfValuesInTheRow.add(allCSVValues[i][j]);
				}
				double normalizationValue = Statistic.computeStatistic(
						csvSpecification.getNormalizationBy(), listOfValuesInTheRow);
				//old value is overwritten by normalized one
				for(int j=0; j<allCSVValues[i].length; j++){
					allCSVValues[i][j] = allCSVValues[i][j]/normalizationValue;
				}
			}
		}
		
		
		
		/*
		 * write csv-file
		 */
		String csvPath = pathCompDataDirectory+File.separator+"csv";
		File csvDirectory = new File(csvPath);
		if(!csvDirectory.exists()){
			csvDirectory.mkdirs();
		}
		try {
			FileWriter fw = new FileWriter(csvPath+File.separator+csvFilename+".csv", false);
			//Change title of column
			String titleNumbering = "";
			if(csvSpecification.hasNumberingColumn()){
				titleNumbering = "Nr;";
			}
			//columnTitle for the row-descriptions. Put number of vertices if, this was selected.
			String columnTitle = titleNumbering
					+csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.ROW_DESCRIPTION).get(0).getClass().getSimpleName();
			for(int j=0; j<csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1).size(); j++){
				for(int k=0; k<csvSpecification.getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART2).size(); k++){
					String part1 = ((RowOrColumnSpecification)csvSpecification.
							getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART1).get(j)).getNameOfTheRowOrColumn();
					String part2 = ((RowOrColumnSpecification)csvSpecification.
							getSpecificationForDimension(DimensionCSVComputationData.COLUMN_DESCRIPTION_PART2).get(k)).getNameOfTheRowOrColumn();
					String separationString = "+";
					if(part1.length()==0 || part2.length()==0){
						separationString = "";
					}
					columnTitle += ";"+part1+separationString+part2;
				}
			}
			fw.append(columnTitle);
			//append all rows
			for(int i=0; i<allCSVValues.length; i++){
				String numbering = "";
				if(csvSpecification.hasNumberingColumn()){
					numbering = (i+1)+";";
				}
				//numbering+rowTitle(=row description)
				String rowString = numbering+((RowOrColumnSpecification)csvSpecification.
						getSpecificationForDimension(DimensionCSVComputationData.ROW_DESCRIPTION).get(i)).getNameOfTheRowOrColumn();
				for(int j=0; j<allCSVValues[i].length; j++){
					rowString += String.format(";%f", allCSVValues[i][j]);
				}
				fw.append(System.lineSeparator()+rowString);
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * To (formalized) infromation how the csv was built this csvSpecifiction is also saved (as json via Gson)
		 */
		DataAsJSonFile.writeFile(csvSpecification,
				csvPath+File.separator+csvFilename+csvSpecification.getClass().getSimpleName()+".json");
		
		System.out.println("CSV-creation ("+csvFilename+".csv) completed.");
	}
}
