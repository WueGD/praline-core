package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Randomness;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.io.GraphIOException;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.TestConfig.RecomputationFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.SuperGraphReader;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.cPlusPlus.CPlusPlusExternLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.GraphReference;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityTesterForLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.ScaledLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import org.xml.sax.SAXException;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.List;

/**
 * Static methods as outtake from the main()-class ({@link Testclass}).
 */
public class GraphDrawingTestSeries {

	/*
	 * The Java-warnings in this class come from the shift of the used data from
	 * variable vertex-data to {@link VertexTriple}s (that contain also the variable vertex-data).
	 * It is used in {@link FRLayoutNoMaps} and all classes having this as a super-class
	 * (all newly created layout-algorithms).
	 * To handle both together some generics were left out and at some places
	 * it has to be casted to a version with generics.
	 */
	

	/*
	 * Values for output for the series of tests
	 */
	private static int outstandingGraphs = 0;
	private static int successfullySavedGraphs = 0;
	private static int graphsWhereSavingFailed = 0;
	
	
	public static void drawTestSeriesOfGraphs(Scanner sc, TestConfig tc) throws ParserConfigurationException,
	SAXException, IOException, GraphIOException, ClassNotFoundException{
		System.out.println("Test series selected. Needed time and evaluated quality are saved in files "
				+ "at "+Testclass.pathsConfig.pathToComputationData+".");
		System.out.println("Note: Every layout is scaled to a square 600x600 to make the results better comparable.");
		System.out.println("This and the evaluation of of the drawings is done outside the time measuring.");
		/*
		 * 1. Ask frame parameters
		 */
		LinkedList<GraphReference> selectedGraphs;
		LinkedList<AlgorithmReference> selectedAlgorithms;
		LinkedList<String> externCPlusPlusAlgPaths;
		
		int selectedSGrowth = -1;
		double smallestS = -1;
		double increasingValueOfS = -1;
		int numberOfSValues = -1;
		LinkedList<RecomputationOfSplitTreeAndWSPDFunction> selectedRecompFunctions = null;
		
		int selectedThetaGrowth = -1;
		double smallestTheta = -1;
		double increasingValueOfTheta = -1;
		int numberOfThetaValues = -1;
		
		int numberOfRuns = 0;
		
		if (tc != null) {
			// Configuration is given as parameter -> don't ask the user
			
			selectedGraphs = new LinkedList<>();
			for (String graph : tc.selectedGraphs) {
				File specificationDir = new File(Testclass.pathsConfig.pathToGraphSpecifications);
				File f = new File(specificationDir, graph);
				GraphReference specification = DataAsJSonFile.getFileContent(f.getPath(), GraphReference.class);
				selectedGraphs.add(specification);
			}
			
			selectedAlgorithms = new LinkedList<>();
			for (String algo : tc.selectedAlgorithms) {
				Class<? extends Object> algoClass;
				algoClass = Class.forName(algo);
				selectedAlgorithms.add(new AlgorithmReference(algoClass));
			}
			externCPlusPlusAlgPaths = new LinkedList<>();
			for (String algo : tc.externCPlusPlusAlgPaths) {
				externCPlusPlusAlgPaths.add(Testclass.pathsConfig.pathToCPlusPlusResults + File.separator + algo);
				selectedAlgorithms.add(new AlgorithmReference(CPlusPlusExternLayout.getClassToPath(algo)));
			}
			
			
			selectedSGrowth = tc.selectedSGrowth;
			smallestS = tc.smallestS;
			increasingValueOfS = tc.increasingValueOfS;
			numberOfSValues = tc.numberOfSValues;
			selectedRecompFunctions = new LinkedList<>();
			for (final RecomputationFunction fct : tc.selectedRecompFunctions) {
				if (fct == null) {
					selectedRecompFunctions
							.add(new RecomputationOfSplitTreeAndWSPDFunction("Every iteration both new"));
				} else if (!fct.updateBarycenters) {
					selectedRecompFunctions.add(
							new RecomputationOfSplitTreeAndWSPDFunction(
									"After "+fct.a+"ln of ("+fct.b+"+currentIteration) new"+(
											fct.updateBarycenters?", else update barycenters":"")) {
								@Override
								public Boolean apply(LayoutWithWSPD<?> layout) {
									int smallerValue = (int) (Math
											.log(fct.b
													+ layout.getCurrentIteration())
											* fct.a);
									int largerValue = (int) (Math
											.log(fct.b + (layout
													.getCurrentIteration() + 1))
											* fct.a);
									return smallerValue != largerValue;
								}
							});
				} else {
					selectedRecompFunctions.add(
							new RecomputationOfSplitTreeAndWSPDFunction(
									"After "+fct.a+"ln of ("+fct.b+"+currentIteration) new"+(
											fct.updateBarycenters?", else update barycenters":"")) {
								@Override
								public Boolean apply(LayoutWithWSPD<?> layout) {
									int smallerValue = (int) (Math
											.log(fct.b
													+ layout.getCurrentIteration())
											* fct.a);
									int largerValue = (int) (Math.log(fct.b
											+ layout.getCurrentIteration() + 1)
											* fct.a);
									boolean recomputation =
											smallerValue != largerValue;
									if (!recomputation) {
										layout.updateBarycenters();
									}
									return recomputation;
								}
							});
				}
			}
			
			selectedThetaGrowth = tc.selectedThetaGrowth;
			smallestTheta = tc.smallestTheta;
			increasingValueOfTheta = tc.increasingValueOfThetas;
			numberOfThetaValues = tc.numberOfThetaValues;

			numberOfRuns = tc.numberOfTests;

		} else {
			//1.1 select class of graphs
			selectedGraphs = TestOfGraphDrawing.graphManager.assistantForSelectingMultipleObjects(sc);
			//1.2 select algos
			selectedAlgorithms = TestOfGraphDrawing.algorithmManager.assistantForSelectingMultipleObjects(sc);
			externCPlusPlusAlgPaths = new LinkedList<String>();
			if(selectedGraphs.size()==1){
				while(Dialog.assistantConfirm(sc, "Add a series of drawings computed by a algorithm from the C++-project?")){
					System.out.println("Note: The results in the selected directory must base on the same set of graphs "
							+ "as now selected and there must be done as much runs (drawings) per graph as now selected.");
					String relativePath = Dialog.getNextLine(sc, "Please enter the name of a directory relative to "
							+Testclass.pathsConfig.pathToCPlusPlusResults+" with these computation data.");
					externCPlusPlusAlgPaths.add(Testclass.pathsConfig.pathToCPlusPlusResults+File.separator+relativePath);
					selectedAlgorithms.add(new AlgorithmReference(CPlusPlusExternLayout.getClassToPath(relativePath)));
				}
			}
			if(AlgorithmReference.conatinsAlgorithmOfAlgorithmType(selectedAlgorithms, AlgorithmType.WITH_WSPD)){
				//1.3 ask for s of the wspd
				System.out.println("The test can be done for different s for the WSPD.");
				selectedSGrowth = Dialog.getNextInt(sc, "Please select the growth of the different tested s."
						+ "\n0: Linear\n1: Exponential", 0, 1);
				smallestS = Dialog.getNextDouble(sc, "Enter smallest s");
				if(selectedSGrowth==0){ //linear growth
					increasingValueOfS = Dialog.getNextDouble(sc, "Choose a step-width (distance) between two tested s-values.");
				}
				else{
					increasingValueOfS = Dialog.getNextDouble(sc, "Choose a basis for the growth of s "
							+ "(E.g. 1.3 if the next bigger s-value should be 30% greater than the previous one)");
				}
				System.out.println("How many different s-values shall be tested? This number of tests t determines also the biggest tested s-value,");
				if(selectedSGrowth==0){ //linear growth
					numberOfSValues = Dialog.getNextInt(sc, "that is "+smallestS+"+(t-1)*"+increasingValueOfS+" .");
				}
				else{
					numberOfSValues = Dialog.getNextInt(sc, "that is "+smallestS+"*"+increasingValueOfS+"^(t-1) .");
				}
				//1.4 functions for recomp
				System.out.println("Now choose the functions that decide in a layout-algorithm that works with a WSPD "
						+ "in each iteration whether the split tree and the WSPD should be computed new or not.");
				selectedRecompFunctions = TestOfGraphDrawing.recomputationFunctionManager.assistantForSelectingMultipleObjects(sc);
			}
			if(AlgorithmReference.conatinsAlgorithmOfAlgorithmType(selectedAlgorithms, AlgorithmType.WITH_QUADTREE)){
				//1.5 ask for theta
				System.out.println("The test can be done for different theta for the layouts with Quadtree.");
				selectedThetaGrowth = Dialog.getNextInt(sc, "Please select the growth of the different tested theta."
						+ "\n0: Linear\n1: Exponential", 0, 1);
				smallestTheta = Dialog.getNextDouble(sc, "Enter smallest theta");
				if(selectedThetaGrowth==0){ //linear growth
					increasingValueOfTheta = Dialog.getNextDouble(sc, "Choose a step-width (distance) between two tested theta-values.");
				}
				else{
					increasingValueOfTheta = Dialog.getNextDouble(sc, "Choose a basis for the growth of theta "
							+ "(E.g. 1.3 if the next bigger theta-value should be 30% greater than the previous one)");
				}
				System.out.println("How many different theta-values shall be tested? This number of tests t determines also "
						+ "the biggest tested theta-value,");
				if(selectedThetaGrowth==0){ //linear growth
					numberOfThetaValues = Dialog.getNextInt(sc, "that is "+smallestTheta+"+(t-1)*"+increasingValueOfTheta+" .");
				}
				else{
					numberOfThetaValues = Dialog.getNextInt(sc, "that is "+smallestTheta+"*"+increasingValueOfTheta+"^(t-1) .");
				}
			}
			//1.6 number of runs per graph (repititions with different initial V-locations
			System.out.println("The result of the algorithms depends on random initial locations for the vertices.");
			System.out.println("Because of that there can be done several drawings for one graph with the same configuration "
					+ "(algorithm, s/theta, recomputation-function)");
			if(externCPlusPlusAlgPaths.size() > 0){
				System.out.println("C++-algorithms are selected. It is recommended to give now the same number of tests as were done there.");
			}
			numberOfRuns = Dialog.getNextInt(sc, "Enter how many runs per same configuartion and same graph should be done.", 1, Integer.MAX_VALUE);
		}
		
		/*
		 * 2. Do computations and measuring and save results
		 */
		
		
		//infos for comp meta data
		String[] functionsNames = null;
		if(selectedRecompFunctions!=null){
			functionsNames = new String[selectedRecompFunctions.size()];
			for(int i=0; i<selectedRecompFunctions.size(); i++){
				functionsNames[i] = selectedRecompFunctions.get(i).getName();
			}
		}
		ComputationMetaData computationMetaData = new ComputationMetaData(
				selectedAlgorithms.toArray(new AlgorithmReference[selectedAlgorithms.size()]),
				functionsNames, (selectedSGrowth==0) ? true : false, smallestS, increasingValueOfS, numberOfSValues,
				(selectedThetaGrowth==0) ? true : false, smallestTheta, increasingValueOfTheta, numberOfThetaValues,
				numberOfRuns);
	
		
		System.out.println(new Date().toString()+": Test series started.");
		long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		
		//Berechnungsmetadaten abspeichern
		String pathToSaveData = Testclass.pathsConfig.pathToComputationData+File.separator
				+DateAsString.getCurrentTimeInFormatYYYYDotMMDotDD_hhDashmmDashss();
		
		File saveDir = new File(pathToSaveData);
		saveDir.mkdirs();
		
		DataAsJSonFile.writeFile(computationMetaData, pathToSaveData+File.separator+"computationMetaData.json");
		
		System.out.println("Directory "+pathToSaveData+" incl. computation meta data file created");

		
		for(GraphReference gr: selectedGraphs){
			outstandingGraphs += gr.getNumberOfGraphs();
		}
		int counterForGraphsToBeGeneratedFromMethods = 0;
		for(int i=0; i<selectedGraphs.size(); i++){
			String directoryOrMethodCallParameter = selectedGraphs.get(i).getDirectoryOrMethodCallParameter();
			//graph saved on disk
			if(selectedGraphs.get(i).isGraphsAreStoredInDirectory()){
				File graphFileDirectory = new File(Testclass.pathsConfig.pathToGraphs+File.separator+directoryOrMethodCallParameter);
				
				testAllObjectsInOneGraphDirectory(graphFileDirectory, pathToSaveData, directoryOrMethodCallParameter,
						computationMetaData, selectedRecompFunctions, externCPlusPlusAlgPaths, tc);
			}
			//graph gotten by method-call
			else{
				for(UndirectedGraph<String, String> graph: TestOfGraphDrawing.graphManager.getGraphViaMethod(directoryOrMethodCallParameter, sc)){
					doComputationsForOneGraphAndSaveEvaluation(graph, pathToSaveData, directoryOrMethodCallParameter,
							"Graph"+counterForGraphsToBeGeneratedFromMethods, computationMetaData, selectedRecompFunctions,
							externCPlusPlusAlgPaths, tc);
					counterForGraphsToBeGeneratedFromMethods++;
				}
			}
		}
		long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		double neededTimeInHours = (double)(endTime-startTime)/3600/1000/1000/1000;
		System.out.println(new Date().toString()+": Test series completed after "+neededTimeInHours+" hours.");
		System.out.println("Computation data to "+successfullySavedGraphs+" graphs could be saved, saving failed in "+graphsWhereSavingFailed+" cases.");
		if(outstandingGraphs!=0){
			System.out.println("Please note: In the selected graph set there were fewer graphs that could be read, "
					+ "than stated in the specification-file to that graph set.");
		}
	}
	
	private static void testAllObjectsInOneGraphDirectory(File graphFileDirectory, String pathToSaveData, String relativePath,
			ComputationMetaData computationMetaData, List<RecomputationOfSplitTreeAndWSPDFunction> selectedRecompFunctions,
			List<String> externCPlusPlusAlgPaths, TestConfig tc) throws ParserConfigurationException, SAXException, IOException, GraphIOException{
		File[] objectsInDir = graphFileDirectory.listFiles();
		for(File f: objectsInDir){
			if(f.isDirectory()){
				String dirName = f.getName();
				testAllObjectsInOneGraphDirectory(f, pathToSaveData, relativePath+File.separator+dirName,
						computationMetaData, selectedRecompFunctions, externCPlusPlusAlgPaths, tc);
			}
			else{
				UndirectedGraph<String, String> graph = SuperGraphReader.getGraph(f.getPath());
				if(graph==null){
					continue; //either no graph-file or error when reading
				}
				
				System.out.println(new Date().toString()+": Graph "+relativePath+File.separator+f.getName()+" tested.");
				
				doComputationsForOneGraphAndSaveEvaluation(graph, pathToSaveData, relativePath, f.getName(),
						computationMetaData, selectedRecompFunctions, externCPlusPlusAlgPaths, tc);
			}
		}
	}
	
	
	private static void doComputationsForOneGraphAndSaveEvaluation(UndirectedGraph<String, String> graph, String pathToSaveData, 
			String relativePath,  String graphFileName, ComputationMetaData computationMetaData,
			List<RecomputationOfSplitTreeAndWSPDFunction> selectedRecompFunctions, List<String> externCPlusPlusAlgPaths,
			TestConfig tc){
		
		//In order to pass the highest directory to doComputationsForOneGraph(...) remove everything but the last element
		//from the relative path. That is done so that extern C++-runs can be handled (only read and evalutated, not drawn)
		int indexLastSeparatorChar = relativePath.indexOf(File.separatorChar);
		// use complete string if there is no file seperator in the string
		if (indexLastSeparatorChar == -1){
			indexLastSeparatorChar = relativePath.length()-1;
		}
		
		
		ComputationData computationData = doComputationsForOneGraph(graph, computationMetaData,
				selectedRecompFunctions, externCPlusPlusAlgPaths, relativePath.substring(indexLastSeparatorChar+1), graphFileName, tc);
		
		File dirToSave = new File(pathToSaveData+File.separator+relativePath);
		dirToSave.mkdirs();
		
		if(DataAsJSonFile.writeFile(computationData, pathToSaveData+File.separator+relativePath+File.separator+graphFileName+".json")){
			System.out.println(new Date().toString()+": Saved computation data.");
			successfullySavedGraphs++;
			outstandingGraphs--;
		}
		else{
			System.out.println(new Date().toString()+": Computation data to "+relativePath+File.separator+graphFileName+" could not be saved.");
			graphsWhereSavingFailed++;
			outstandingGraphs--;
		}
		int notAnyMoreOutstandingGraphs = (successfullySavedGraphs+graphsWhereSavingFailed);
		int all = outstandingGraphs+notAnyMoreOutstandingGraphs;
		System.out.println(successfullySavedGraphs+" saved successfully, "+graphsWhereSavingFailed+" could not be saved, "
				+outstandingGraphs+" outstanding ("+(100*(double)notAnyMoreOutstandingGraphs/(double)all)+"% completed).");
	}
	
	/**
	 * 
	 * @param graph
	 * @param computationMetaData
	 * @param selectedRecompFunctions
	 * @param externCPlusPlusAlgPaths
	 * @param directoryName
	 * Is only needed to be able to include extern C++-runs (if there are) to the same graph
	 * @param graphFileName Hier dasselbe
	 * @return
	 */
	private static ComputationDataPlusNumberOfIterations doComputationsForOneGraph(UndirectedGraph<String, String> graph,
			ComputationMetaData computationMetaData, List<RecomputationOfSplitTreeAndWSPDFunction> selectedRecompFunctions,
			List<String> externCPlusPlusAlgPaths, String directoryName, String graphFileName, TestConfig tc){
		
		
		ComputationDataPlusNumberOfIterations computationData =
				new ComputationDataPlusNumberOfIterations(graph.getVertexCount(), graph.getEdgeCount(), computationMetaData);
		
		//Define size for all algorithms
		Dimension dimension = new Dimension(600, 600);
		
		//Map the extern C++-Layouts to their i-values
		LinkedHashMap<Integer, String> iToPathMapCPPExtern = new LinkedHashMap<Integer, String>();
		Iterator<String> iteratorC = externCPlusPlusAlgPaths.iterator();
		for(int i=0; i<computationMetaData.getAlgorithms().length; i++){
			if(computationMetaData.getAlgorithms()[i].getAlgorithmType() == AlgorithmType.EXTERN_C_PLUS_PLUS){
				iToPathMapCPPExtern.put(i, iteratorC.next());
			}
		}		
		
		//the tests on all 4 levels (combinations of algorithm x s/theta x recompFunc x runsWithSamePrevious3Configuration)
		for(int l=0; l<computationMetaData.getNumberOfSameTest(); l++){
			//initial point locations must be the same for all, it is defined after the start of the first alg and is saved here then
			Map<String, Point2D> initPointLocations = null;
			
			boolean[] iAlreadyDone = new boolean[computationMetaData.getAlgorithms().length];
			for(int iSubstitute=0; iSubstitute<computationMetaData.getAlgorithms().length; iSubstitute++){
				/*
				 * There is the open question if one algorithm has a disadvantage in terms of runtime if it has to
				 * draw the current graph before the other algorithms do so (computes first).
				 * As I can not definitely say I find a random order in which the algorithms have to draw the graph in that run.
				 * That is the reasion the for-variable here is not i, but iSubstitute.
				 * The real i (index of the algorithm to be used to draw as next) is found
				 * afterwards randomly using the boolean-array iAlreadyDone.
				 */
				int whichIOfTheStillAvailable = (int) (
						Randomness.random.nextDouble()*(double)(computationMetaData.getAlgorithms().length-iSubstitute));
				int i = -1; //real index is found with the next for-loop
				while(whichIOfTheStillAvailable>-1){
					whichIOfTheStillAvailable--;
					i++;
					while(iAlreadyDone[i]==true){
						i++;
					}
				}
				iAlreadyDone[i] = true;
				
				AlgorithmType algorithmtype = computationMetaData.getAlgorithms()[i].getAlgorithmType();
				int jMax = algorithmtype==AlgorithmType.WITH_WSPD ?
						computationMetaData.getNamesOfTheRecomputationOfSplitTreeAndWSPDFunctions().length : 1;
				for(int j=0; j<jMax; j++){
					int kMax = (algorithmtype==AlgorithmType.WITH_WSPD || algorithmtype==AlgorithmType.WITH_QUADTREE)?
							(algorithmtype==AlgorithmType.WITH_WSPD ?
							computationMetaData.getNumberOfDifferentSValues() : computationMetaData.getNumberOfDifferentThetaValues() ) : 1;
					for(int k=0; k<kMax; k++){
						
						//create alg-instance
						AbstractLayout layout = computationMetaData.getAlgorithms()[i].getNewInstance(
								graph, dimension, computationMetaData.getSOrThetaOrMinus1(i, k));
						
						//Alg with WSPD
						if(computationMetaData.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.WITH_WSPD){
							((LayoutWithWSPD<String>) layout).setRecomputationOfSplitTreeAndWSPDFunction(
									selectedRecompFunctions.get(j));
						}
						
						
						//Externe C++-Alg (no init poin locations relevant (see else))
						if(computationMetaData.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.EXTERN_C_PLUS_PLUS){
							try {
								((CPlusPlusExternLayout) layout).initialize(iToPathMapCPPExtern.get(i)+File.separator
										+directoryName+File.separator+graphFileName, l);
							} catch (IOException e) {
								System.err.println("Problem with reading coordinates of the extern C++-run with path "
										+iToPathMapCPPExtern.get(i)+File.separator+directoryName+File.separator+graphFileName
										+" and test-run-number "+l+". Action failed. Path correct?");
							}
						}
						//no init points determined yet
						else if(initPointLocations==null){
							initPointLocations = new LinkedHashMap<String, Point2D>();
							//init all points and save
							AbstractLayout helperLayout = layout;
							if(FRLayoutNoMaps.class.isAssignableFrom(layout.getClass())){
								helperLayout = ((FRLayoutNoMaps)layout).getCorresponding_V_E_Layout();
							}
							for(String v: graph.getVertices()){
								initPointLocations.put(v, helperLayout.apply(v));
							}
						}
						//there are already init points
						else{
							if(!FRLayoutNoMaps.class.isAssignableFrom(layout.getClass())){
								for(String v: graph.getVertices()){
									Point2D newPoint = layout.apply(v);
									Point2D initPoint = initPointLocations.get(v);
									newPoint.setLocation(initPoint.getX(), initPoint.getY());
								}
							}
							else{
								for(VertexTriple<String> t: ((FRLayoutNoMaps<String,String>)layout).getVertices()){
									t.get2().setLocation(initPointLocations.get(t.get1()));
								}
							}
						}
						
						//<Draw and Evaluate>
						//Extern C++-Layout
						if(computationMetaData.getAlgorithms()[i].getAlgorithmType()==AlgorithmType.EXTERN_C_PLUS_PLUS){
							//read cpu-time
							try {
								computationData.setCpuTimeInNs(((CPlusPlusExternLayout) layout).getCPUTimeInNs(), i, j, k, l);
							} catch (IOException e) {
								System.err.println("Problem with reading cpu-time of the extern C++-run with path "
										+iToPathMapCPPExtern.get(i)+File.separator+directoryName+File.separator+graphFileName
										+" and test-run-number "+l+". Action failed. Path correct?");
							}
							//read number of iterations (This value is only in files of [ItCount]SpringEmbedderFRExact in C++ and there
							//only in newer files -> can be that there is no such file)
							try {
								computationData.setNumberOfIterations(((CPlusPlusExternLayout) layout).getNumberOfIterations(), i, j, k, l);
							} catch (IOException e) {
								//If there is no set to -1
								computationData.setNumberOfIterations(-1, i, j, k, l);
							}
						}
						//else java-layout (standard case)
						else{
							boolean runAlgo = (
									tc == null
									|| graph.getVertexCount() <= tc.maxNodesFRExact
									|| !( layout.getClass().equals(FRLayout.class)
										|| layout.getClass().equals(FRLayoutNoMaps.class)
										|| layout.getClass().equals(FRLayoutNoMapsNoFrame.class)
										|| layout.getClass().equals(FRLayoutNoMapsNoFrameAlwaysRep.class)
										|| layout.getClass().equals(FRLayout2.class)
										|| layout.getClass().equals(KKLayout.class)
										|| layout.getClass().equals(FRLayoutMultiLevel.class)
										|| layout.getClass().equals(FRLayoutNoMapsMultiLevel.class)
										|| layout.getClass().equals(FRLayoutNoMapsNoFrameMultiLevel.class)
										|| layout.getClass().equals(FRLayoutNoMapsNoFrameAlwaysRepMultiLevel.class)
										|| layout.getClass().equals(KKLayoutMultiLevel.class)
										)
									);
							long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
							layout.initialize();
							int iterationCounter = 0;
							long runtimeRepulsion = 0;
							if (runAlgo) {
								while(!((IterativeContext) layout).done()){
									if (layout instanceof AlgorithmMeasureRepulsiveTime) {
										runtimeRepulsion += ((AlgorithmMeasureRepulsiveTime) layout).stepMeasureRepulsiveTime();
									} else {
										((IterativeContext) layout).step();
									}
									iterationCounter++;
								}
							}
							long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
							
							computationData.setCpuTimeInNs(endTime-startTime, i, j, k, l);
							computationData.setCpuTimeRepulsionInNs(runtimeRepulsion, i, j, k, l);
							computationData.setNumberOfIterations(iterationCounter, i, j, k, l);
						}
						computationData.readAllQualityInformationsAndSaveThem(
								new QualityTesterForLayout<String, String>(
										new ScaledLayout<String, String>(!FRLayoutNoMaps.class.isAssignableFrom(layout.getClass()) ?
												layout : ((FRLayoutNoMaps<String, String>)layout).getCorresponding_V_E_Layout()
										)
								), i, j, k, l, tc);
								
						//</Draw and Evaluate>
					}
				}
			}
		}
		
		return computationData;
	}
}
