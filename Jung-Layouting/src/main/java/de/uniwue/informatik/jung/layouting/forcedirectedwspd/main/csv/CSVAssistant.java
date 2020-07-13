package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.TestOfGraphDrawing;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.Testclass;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.Manager;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.StatisticManager;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityCriterion;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationData;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.ComputationMetaData;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.io.File;
import java.util.*;

/**
 * Static methods as outtake from the main()-class ({@link Testclass}).
 * 
 * Creates csv from results from {@link TestOfGraphDrawing#testreiheVonGraphsZeichnen(Scanner)}.
 */
public class CSVAssistant {
	
	
	public static void createCSVFromStoredCompData(Scanner sc){
		/*
		 * 1. Find directory of comp data
		 */
		
		File selectedDirectory = ComputationDataEvaluation.assistantForCompDataDirectorySelection(sc);
		String pathSelectedDirectory = selectedDirectory.getPath();
		
		final LinkedList<String> selectedPaths = new LinkedList<String>();
		selectedPaths.add(pathSelectedDirectory);
		
		
		
		
		
		
		
		
		/*
		 * 2. read CompMetaData
		 */
		ComputationMetaData computationMetaDataNotFinal =
				DataAsJSonFile.getFileContent(pathSelectedDirectory+File.separator+"berechnungsmetadaten.json", ComputationMetaData.class);
		if(computationMetaDataNotFinal==null){
			computationMetaDataNotFinal =
					DataAsJSonFile.getFileContent(pathSelectedDirectory+File.separator+"computationMetaData.json", ComputationMetaData.class);
		}
		final ComputationMetaData computationMetaData = computationMetaDataNotFinal;
		
		
		
		
		
		
		
		/*
		 * 3. select running time/quality evaluation criterions
		 */
		System.out.println("At the beginning the quality evaluation criterions appearing in the csv must be chosen.");
		List<RunningTimeOrQualityCriterions> allQualityEvaluationCriterions = new LinkedList<RunningTimeOrQualityCriterions>();
		Manager<RunningTimeOrQualityCriterions> qualityEvaluationCriterionManager = new Manager<RunningTimeOrQualityCriterions>(){
			@Override
			protected void initialize() {
				for(ComputationDataSelection bwa: ComputationDataSelection.values()){
					if(bwa==ComputationDataSelection.QUALITY_CRITERIONS){
						for(QualityCriterion bk: QualityCriterion.values()){
							for(Statistic st: Statistic.values()){
								String name = bk.toString()+"("+st.toString()+")";								
								addToObjectList(new RunningTimeOrQualityCriterions(name, null, bwa, bk, st), name);
							}
						}
					}
					else{
						String name = bwa.toString();
						addToObjectList(new RunningTimeOrQualityCriterions(name, null, bwa, null, null), name);
					}
				}
			}
		};
		allQualityEvaluationCriterions = qualityEvaluationCriterionManager.assistantForSelectingMultipleObjects(sc);
		
		
		
		
		
		
		
		
		/*
		 * 4. merge results and specify algorithm groups (select algorithms)
		 */
		boolean mergeRunsNotFinal = true; //for computationMetaData.getNumberOfSameTest()<=1 true, otherwise choose
		if(computationMetaData.getNumberOfSameTest()>1){
			System.out.println("For every algorithm (in combination with s/theta and recomputation function) there were "
					+computationMetaData.getNumberOfSameTest()+" runs done.");
			mergeRunsNotFinal = Dialog.assistantConfirm(sc, "Merge these runs? (default: Yes)");
		}
		final boolean mergeRuns = mergeRunsNotFinal;
		final boolean mergeSOrTheta = Dialog.assistantConfirm(sc, "Merge runs of algorithms with WSPD/Quadtree only "
				+ "differing in  s/theta value? (default: No)");
		final boolean mergeFunctions = Dialog.assistantConfirm(sc, "Merge runs of algorithms with WSPD only differing "
				+ "in functions determing if SplitTree and WSPD must be recomputed? (default: No)");
		/*
		 * mergeAlgorithms was taken out (set to always true) because it leads to problems without helping much
		 */
		final boolean mergeAlgorithms = false;
//				 = Dialog.assistantConfirm(sc, "Merge runs of different algorithms with same algorithm type (classic/WSPD/Quadtree/C++)? (default: No)");
//		if(mergeAlgorithms && (!mergeFunctions || !mergeSOrTheta)){ //this combination is no good in this implementation! warn user!
//			System.out.println("Care-note! ...todo");
//		}
		Manager<List<AlgorithmSpecification>> allAlgorithmSpecificationsManager = new Manager<List<AlgorithmSpecification>>(){
			@Override
			protected void initialize() {
				//combine 4 describing elements (algorithm, recomputationFunction, s/theta, run)
				Tuple<String, List<AlgorithmSpecification>>[][][][] asCollection =
						new Tuple[computationMetaData.getAlgorithms().length]
								[computationMetaData.getMaxNumberOfRecomputationOfSplitTreeAndWSPDFunctions()]
								[Math.max(1, Math.max(computationMetaData.getNumberOfDifferentSValues(),
										computationMetaData.getNumberOfDifferentThetaValues()))]
								[computationMetaData.getNumberOfSameTest()];
				for(int i=0; i<computationMetaData.getAlgorithms().length; i++){
					AlgorithmReference algorithm = computationMetaData.getAlgorithms()[i];
					String algorithmName = algorithm.getClassNameOfTheLayout()+" ";
					for(int j=0; j<(algorithm.getAlgorithmType()==AlgorithmType.WITH_WSPD?
							computationMetaData.getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions().length:1); j++){
						String functionName = "";
						if(algorithm.getAlgorithmType()==AlgorithmType.WITH_WSPD){
							functionName = computationMetaData.getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions()[j]+" ";
						}
						for(int k=0; k<(algorithm.getAlgorithmType()==AlgorithmType.WITH_WSPD?computationMetaData.getNumberOfDifferentSValues():(
								algorithm.getAlgorithmType()==AlgorithmType.WITH_QUADTREE?
										computationMetaData.getNumberOfDifferentThetaValues():1)); k++){
							String sOrThetaName = "";
							if(algorithm.getAlgorithmType()==AlgorithmType.WITH_WSPD){
								sOrThetaName = "s="+computationMetaData.getS(k)+" ";
							}
							else if(algorithm.getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
								sOrThetaName = "theta="+computationMetaData.getThetaWert(k)+" ";
							}
							
							
							for(int l=0; l<computationMetaData.getNumberOfSameTest(); l++){
								AlgorithmSpecification as = new AlgorithmSpecification(i, j, k, l);
								int[] index = getArrayFieldByIndices(i, j, k, l);
								//if still null, then init
								if(asCollection[index[0]][index[1]][index[2]][index[3]]==null){
									String asCollectionName = "";
									if(!mergeAlgorithms){
										asCollectionName += algorithmName;
									}
									if(!mergeFunctions){
										asCollectionName += functionName;
									}
									if(!mergeSOrTheta){
										asCollectionName += sOrThetaName;
									}
									if(!mergeRuns){
										asCollectionName += "Run="+l;
									}
									if(mergeAlgorithms && mergeFunctions &&
											mergeSOrTheta && mergeRuns){
										asCollectionName = "All algorithms, recomputation functions, s/theta, runs combined";
									}
									
									asCollection[index[0]][index[1]][index[2]][index[3]] = new Tuple<String, List<AlgorithmSpecification>>(
											asCollectionName, new LinkedList<AlgorithmSpecification>());
								}
								asCollection[index[0]][index[1]][index[2]][index[3]].get2().add(as);
							}
						}
					}
				}
				//add only the gathered lists of algSpec. (with name) to the manager
				for(int i=0; i<asCollection.length; i++){
					for(int j=0; j<asCollection[i].length; j++){
						for(int k=0; k<asCollection[i][j].length; k++){
							for(int l=0; l<asCollection[i][j][k].length; l++){
								if(asCollection[i][j][k][l]!=null){
									addToObjectList(asCollection[i][j][k][l].get2(), asCollection[i][j][k][l].get1());
								}
							}
						}
					}
				}
			}
			
			/**
			 * Array of length 4 with [0]=iCalced, [1]=jCalced, [2]=kCalced, [3]=lCalced.
			 * Needed to merge fields.
			 * @return
			 */
			private int[] getArrayFieldByIndices(int i, int j, int k, int l){
				int[] returnField = new int[4];
				returnField[0] = mergeAlgorithms ? 0 : i;
				returnField[1] = mergeFunctions ? 0 : j;
				returnField[2] = mergeSOrTheta ? 0 : k;
				returnField[3] = mergeRuns ? 0 : l;
				return returnField;
			}
		};
		StatisticManager statisticsManager = new StatisticManager();
		System.out.println("Next the algorithms must be selected.");
		List<AlgorithmGroupSpecification> allAlgorithmGroups = new LinkedList<AlgorithmGroupSpecification>();
		do{
			//case all as single alg
			if(Dialog.assistantConfirm(sc, "Select all algorithms not grouped yet? ("+allAlgorithmSpecificationsManager.getObjectCount()+" found)"
					+ "\nOtherwise distinct algorithms can be selected for merging of their results as a algorithm group."
					+ " Such a group can also consist of only one algorithm or algorithms can be left out.")){

				Statistic st = null;
				if(mergeRuns || mergeSOrTheta || mergeFunctions || mergeAlgorithms){
					System.out.println("Choose the way the combined resulsts (runs) should be merged.");
					if(mergeAlgorithms){
						System.out.println("(Note: Results of different algorithms/recomputation-functions/s/theta/runs"
								+ "are always merged before results of different graphs are merged.)");
					}
					st = statisticsManager.assistantForSelectingOneObject(sc);
				}
				for(int i=0; i<allAlgorithmSpecificationsManager.getObjectCount(); i++){
					List<AlgorithmSpecification> oneAlg = new LinkedList<AlgorithmSpecification>();
					for(AlgorithmSpecification as: allAlgorithmSpecificationsManager.getObjectWithObjectname(i).get1()){
						oneAlg.add(as);
					}
					
					AlgorithmGroupSpecification algorithmGroup = new AlgorithmGroupSpecification(
							allAlgorithmSpecificationsManager.getObjectWithObjectname(i).get2(),
							st, oneAlg);
					allAlgorithmGroups.add(algorithmGroup);
				}
				break;
			}
			//else
			System.out.println("It is possible to merge the results of distinct algorithms.");
			System.out.println("Select now the algorithms that should be grouped together (all results are merged, "
					+ "but you can also select only one). After this further algorithm groups can be specified.");
			List<List<AlgorithmSpecification>> combinedAlgs = allAlgorithmSpecificationsManager.assistantForSelectingMultipleObjects(sc);
			for(List<AlgorithmSpecification> asListe: combinedAlgs){
				allAlgorithmSpecificationsManager.removeObject(asListe);
			}
			
			
			String name = Dialog.getNextLine(sc, "Please enter a name for the group of these algorithms.");
			
			Statistic st = null;
			if(combinedAlgs.size()>1 || mergeRuns || mergeSOrTheta || mergeFunctions || mergeAlgorithms){
				System.out.println("Choose the way the combined resulsts (runs) should be merged.");
				System.out.println("(Note: Results of different algorithms/recomputation-functions/s/theta/runs "
						+ "are always merged before results of different graphs are merged.)");
				st = statisticsManager.assistantForSelectingOneObject(sc);
			}
			
			//unify the selected lists to one list
			List<AlgorithmSpecification> allAlgsThatShouldBeMerged = new LinkedList<AlgorithmSpecification>();
			for(List<AlgorithmSpecification> asList: combinedAlgs){
				allAlgsThatShouldBeMerged.addAll(asList);
			}
			
			AlgorithmGroupSpecification algorithmGroup = new AlgorithmGroupSpecification(name, st, allAlgsThatShouldBeMerged);
			allAlgorithmGroups.add(algorithmGroup);
			
			
			if(allAlgorithmSpecificationsManager.getObjectCount()==0){
				break;
			}
		}
		while(Dialog.assistantConfirm(sc, "Add further algorithms/algorithm-groups?"));
		
		
		
		
		
		
		
		
		/*
		 * 5. select graphs
		 */
		System.out.println();
		System.out.println("Next is the selection of graphs:");
		System.out.println();
		LinkedList<GraphGroupSpecification> allGraphGroups = new LinkedList<GraphGroupSpecification>();
		
		//grouping with respect to the number of vertices
		if(Dialog.assistantConfirm(sc, "Merge the results from graphs with the same number of vertices?")){
			System.out.println("All Files are gone through and their number of vertices is read. This can last a moment.");
			LinkedList<String> allPathsToTheGraphFiles = ComputationDataEvaluation.
					allPathsToCompDataFilesInPassedDirectories(selectedPaths);
			//alle durchlaufen und zuordnen
			//go through and classify
			Map<Integer, LinkedList<String>> numberOfVerticesMap = new LinkedHashMap<Integer, LinkedList<String>>();
			for(String path: allPathsToTheGraphFiles){
				ComputationData compData = DataAsJSonFile.getFileContent(path, ComputationData.class);
				int numberOfVertices = compData.getNumberOfVertices();
				if(!numberOfVerticesMap.containsKey(numberOfVertices)){
					numberOfVerticesMap.put(numberOfVertices, new LinkedList<String>());
				}
				numberOfVerticesMap.get(numberOfVertices).add(path);
			}
			//Put graph-sizes to a list sort it and build graph groups for it
			LinkedList<Integer> allAppearingNumbers = new LinkedList<Integer>();
			for(Integer number: numberOfVerticesMap.keySet()){
				allAppearingNumbers.add(number);
			}
			Collections.sort(allAppearingNumbers);

			System.out.println("Choose the way the combined resulsts (runs) should be merged.");
			Statistic selectedMethodOfMerging = statisticsManager.assistantForSelectingOneObject(sc);

			for(Integer number: allAppearingNumbers){
				GraphGroupSpecification ggs = new GraphGroupSpecification(number+"",
						selectedMethodOfMerging, numberOfVerticesMap.get(number));
				allGraphGroups.add(ggs);
			}
		}
		//else: manual grouping
		else{
			System.out.println("No grouping with respect to the number of vertices was selected, but manual selection/grouping.");
			while(Dialog.assistantConfirm(sc, selectedPaths.size()
					+" files/directories of computation data to be used are selected. Exclude files/subdirectories? Select subdirectories"
					+" as own selection (for combinings in the next step single files/dirs to be combined must be selected)?")){
				
				Manager<String> selectedCompDataPathsManager = new Manager<String>(){
					@Override
					protected void initialize() {
						for(String path: selectedPaths){
							addToObjectList(path, path);
						}
					}
				};
				
				String selectedCompDataPath = selectedCompDataPathsManager.assistantForSelectingOneObject(sc);
				
				System.out.println("Selected Directory is: "+selectedCompDataPath+".");
				File selectedCompDataDirectory = new File(selectedCompDataPath);
				//is directory
				if(selectedCompDataDirectory.isDirectory()){
					selectedPaths.remove(selectedCompDataPath);
					if(Dialog.assistentBoolean(sc, "What shall be done?", "Select set of subdirectories/files to be included",
							"Select set of subdirectories/files to be excluded")){
						selectedPaths.addAll(ComputationDataEvaluation.assistantForSubdirectorySelection(sc, selectedCompDataPath, true));
					}
					else{
						selectedPaths.addAll(ComputationDataEvaluation.assistantForSubdirectorySelection(sc, selectedCompDataPath, false));
					}
				}
				//ist datei
				else{
					if(Dialog.assistantConfirm(sc, "A file was selected. Exclude this file (data to be ignored for creation of the csv-file)?"
							+ " Note: Files like computationMetaData, csv-files, ... need not to be excluded explicitly.")){
						selectedPaths.remove(selectedCompDataPath);
					}
				}
				System.out.println("Currently selected files/directories:");
			}
			//graph selection (combine sub-directories)
			System.out.println("Merge data from different selected directories/files?");
			while(Dialog.assistantConfirm(sc, allGraphGroups.size()+" graph groups built. Combine further graphs "
					+ "(each represented by one computationData-file)?")){
				Manager<String> availableSelectedPaths = new Manager<String>(){
					@Override
					protected void initialize() {
						for(String s: selectedPaths){
							//german and english
							if(!s.endsWith(ComputationMetaData.class.getSimpleName()+".json")
									&& !s.endsWith(CSVSpecification.class.getSimpleName()+".json")
									&& !s.endsWith("berechnungsmetadaten.json")
									&& !s.endsWith("CSVSpezifizierung.json")){
								addToObjectList(s, s);
							}
						}
					}
				};
				List<String> combinedPaths = availableSelectedPaths.assistantForSelectingMultipleObjects(sc);
				for(String s: combinedPaths){
					selectedPaths.remove(s);
				}
				
				String name = Dialog.getNextLine(sc, "Please enter a name for the group of these graphs.");
				
				System.out.println("Choose the way the combined resulsts (runs) should be merged.");
				Statistic selectedMethodOfMerging = statisticsManager.assistantForSelectingOneObject(sc);
				
				GraphGroupSpecification graphGroup = new GraphGroupSpecification(name, selectedMethodOfMerging,
						ComputationDataEvaluation.allPathsToCompDataFilesInPassedDirectories(combinedPaths));
				allGraphGroups.add(graphGroup);
				
				if(selectedPaths.size()==0){ //stop if all paths are assigned to groups
					break;
				}
			}
			
			//All files not grouped yet as single graph-group
			LinkedList<String> ungroupedComputationDataPaths =
					ComputationDataEvaluation.allPathsToCompDataFilesInPassedDirectories(selectedPaths);
			for(String path: ungroupedComputationDataPaths){
				File f = new File(path);
				List<String> listForThePath = new LinkedList<String>();
				listForThePath.add(path);
				GraphGroupSpecification ggs = new GraphGroupSpecification(f.getName(), null, listForThePath);
				allGraphGroups.add(ggs);
			}
		}
				
		
		
		
		
		
		
		
		//TODO maybe innern normalization to make different alg-parameters (like s) more comparable/better combinable
		
		/*
		 * 7. Arrange dimensions
		 */
		Manager<DimensionCSVComputationData> dimensionCSVManager = new Manager<DimensionCSVComputationData>(){
			@Override
			protected void initialize() {
				for(DimensionCSVComputationData d: DimensionCSVComputationData.values()){
					addToObjectList(d, d.toString());
				}
			}
		};
		System.out.println("Now it must be specified how the 3 dimension <Graph, Algorithm, Quality-Evaluation-Criterion> "
				+ "should be arranged in the csv(-table).");
		System.out.println("Two of them must be combined (e.g. algorithms and quality-evaluation-criterions) in the columns"
				+ " (titles as column title in first row). The other one has it's values row-wise (titles as row titles in the first column)");
		System.out.println("Select where the graph groups should be placed.");
		DimensionCSVComputationData graphGropusDimension = dimensionCSVManager.assistantForSelectingOneObject(sc);
		dimensionCSVManager.removeObject(graphGropusDimension);
		System.out.println("Select now where the algorithm groups should be placed.");
		DimensionCSVComputationData algorithmGroupsDimension = dimensionCSVManager.assistantForSelectingOneObject(sc);
		dimensionCSVManager.removeObject(algorithmGroupsDimension);
		System.out.println("Select at last where the quality-evaluation-criterions should be placed.");
		DimensionCSVComputationData qualityEvaluationCriterionsDimension = dimensionCSVManager.assistantForSelectingOneObject(sc);
		
		
		
		
		
		
		
		
		/*
		 * 7. Normalization over all cells at the end?
		 */
		NormalizationInCSV normalizationInCSV = NormalizationInCSV.NO_NORMALIZATION;
		boolean hasNormalization = Dialog.assistantConfirm(sc, "It can be normalized row- or column-wise,"
				+" that is all values in one row/column devided by a statistical value (e.g. maximum) of all values in that row/column."
				+" Normalize in this csv?");
		Statistic normalizationBy = null;
		if(hasNormalization){
			boolean normalizationByColumn = Dialog.assistentBoolean(sc, "Where to normalize?", "column-wise", "row-wise");
			if(normalizationByColumn){
				normalizationInCSV = NormalizationInCSV.COLUMN_NORMALIZATION;
			}
			else{
				normalizationInCSV = NormalizationInCSV.ROW_NORMALIZATION;
			}
			System.out.println("After to what shall be normalized?");
			normalizationBy = statisticsManager.assistantForSelectingOneObject(sc);
		}
		
		
		
		
		
		
		
		
		/*
		 * 8. row-wise numbering?
		 */
		boolean numberingColumn = Dialog.assistantConfirm(sc, "Add a numbering-column to the csv?");
		
		
		
		
		
		
		
		
		/*
		 * 9. Sorting row- or column-wise?
		 */
		//TODO
		
		
		
		
		
		
		
		
		
		/*
		 * 10. Name for csv
		 */
		String csvFileName;
		while(true){
			csvFileName = Dialog.getNextLine(sc, "At last a name for the csv must be entered. The ending (.csv) should not be added"
					+ " because this is done automatically.");
			if(csvFileName.length()==0){
				System.out.println("No name entered. Enter name now:");
				continue;
			}
			File f = new File(pathSelectedDirectory+File.separator+csvFileName+".csv");
			if(f.exists()){
				System.out.println("csv-file with this name exists already -> choose another name");
			}
			else{
				break;
			}
		}
		
		
		
		
		
		
		
		
		/*
		 * 11. Generate csv and write (save) it
		 */
		CSVSpecification csvSpecification = new CSVSpecification(allGraphGroups, graphGropusDimension, allAlgorithmGroups,
				algorithmGroupsDimension, allQualityEvaluationCriterions, qualityEvaluationCriterionsDimension,
				normalizationInCSV, normalizationBy, numberingColumn);
		
		ComputationDataEvaluation.generateAndWriteCSV(pathSelectedDirectory, csvFileName, csvSpecification);
	}
}
