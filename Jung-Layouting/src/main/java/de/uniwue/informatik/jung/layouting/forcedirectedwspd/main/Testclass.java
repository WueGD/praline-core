package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

import com.google.gson.Gson;
import edu.uci.ics.jung.io.GraphIOException;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv.CSVAssistant;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration.DemonstrationOfGraphDrawing;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.GraphManager;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.Manager;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Class that contains the main()-function of the project.
 * 
 * User selects here what will be done.
 */
public class Testclass {

	public static final String PATHS_CONFIG_PATH = "Jung-Layouting/target/config.PATHS.json";

	public static TestConfig testConfig = null;
	public static PathsConfig pathsConfig;

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, GraphIOException, ClassNotFoundException{
		
		
		
		Scanner sc = new Scanner(System.in);

		
		//1. load paths from config-json-file (if there are no, use default values and save them in a json-file)
		pathsConfig = DataAsJSonFile.getFileContent(PATHS_CONFIG_PATH, PathsConfig.class);
		if(pathsConfig==null){ //if there is no such file
			//create it (with default values)
			pathsConfig = new PathsConfig();
			DataAsJSonFile.writeFile(pathsConfig, PATHS_CONFIG_PATH);
		}
		//check that some dirs that are needed exist
		File dirToGraphs = new File(pathsConfig.pathToGraphs);
		if(!dirToGraphs.exists()){
			if(Dialog.assistantConfirm(sc, "The path to the graphs specified in "+ PATHS_CONFIG_PATH +" does not exist. "
					+ "Set value to default?")){
				pathsConfig.pathToGraphs = null; //set to null in the next step it is set to default
			}
			else{
				System.out.println("No graphs found. If a mode that needs graphs is selected, this may lead to errors.");
			}
		}
		File dirToGraphSpecifications = new File(pathsConfig.pathToGraphSpecifications);
		if(!dirToGraphSpecifications.exists()){
			if(Dialog.assistantConfirm(sc, "The path to the graph-specifications specified in "+ PATHS_CONFIG_PATH +" does not exist. "
					+ "Set value to default?")){
				pathsConfig.pathToGraphSpecifications = null; //set to null in the next step it is set to default
			}
			else{
				System.out.println("No graph-specifications found. If a mode that needs graphs is selected, this may lead to errors.");
			}
		}
		if(pathsConfig.containsNullValues()){ //or a invalid one
			//replace null-values by default-values
			int numberOfReplacedValues = pathsConfig.replaceNullValuesWithDefaultValues();
			DataAsJSonFile.writeFile(pathsConfig, PATHS_CONFIG_PATH);
			System.out.println(PATHS_CONFIG_PATH +" had invalid values (Did not have expected structure)."
					+ "\n"+numberOfReplacedValues+"/6 values had to be replaced by default-values. Overwrote old file.");
			System.out.println();
		}
		//create missing dirs
		pathsConfig.createSaveDirectoriesThatDoNotExist();
		
		
		//2. load test config if there is one
		if (args.length > 0) {
			// use args[0] as test config in JSON format
			Gson gson = new Gson();
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			testConfig = gson.fromJson(br, TestConfig.class);
		}

		
		
		System.out.println("Welcome to test of SplitTree, WSPD and graph drawing");
		System.out.println();
		
		Manager<Integer> modeManager = new Manager<Integer>() {
			@Override
			protected void initialize() {
				String[] allModeDescriptions = {
					"Exit",
					"Create randomly distributed point set, construct a split tree from it, and visualize the split tree. "
							+ "Afterwards a well-separated pair decomposition can be generated from the split tree.",
					"Create point set with points in grid-structure, construct a split tree from it, and visualize the split tree. "
							+ "Afterwards a well-separated pair decomposition can be generated from the split tree.",
					"Create point sets of increasing size, construct split trees from them, and measure the time.",
					"Draw a graph and visualize it.",
					"Do a series of tests for drawing different graphs with different parameters. "
							+ "The results are saved on your file-system.",
					"Create a csv-file from a series of tests done before.",
					"Create and save a new set of randomly generated graphs.",
					"Read number of vertices and edges of a graph set.",
					"Check graph set for connected components.",
					"Demonstration with visualization of different graphs."
				};
				//Add mode-number + mode-discription
				for(int i=0; i<allModeDescriptions.length; i++){
					addToObjectList(i, allModeDescriptions[i]);
				}
			}
		};
		
		int input = -1;
		if (testConfig != null && testConfig.mode >= 0 && testConfig.mode <= 10) {
			input = testConfig.mode;
		}
		else {
			input = modeManager.assistantForSelectingOneObject(sc);
		}
		switch(input){
			case(0):
			break;
			case(1):
				TestOfTheSplitTree.createRandomPointSetAndConstructAndVisualizeSplitTree(sc);
			break;
			case(2):
				TestOfTheSplitTree.createGridPointSetAndConstructAndVisualizeSplitTree(sc);
			break;
			case(3):
				TestOfTheSplitTree.createSplitTreeFromRandomPointsetsAndMeasureTime(sc);
			break;
			case(4):
				TestOfGraphDrawing.drawAGraph(sc);
			break;
			case(5):
				GraphDrawingTestSeries.drawTestSeriesOfGraphs(sc, testConfig);
			break;
			case(6):
				CSVAssistant.createCSVFromStoredCompData(sc);
			break;
			case(7):
				GraphManager.saveRandomGraphsAsNewGraphSet(sc);
			break;
			case(8):
				GraphManager.checkGraphSetForConnectedComponents(sc, true);
			break;
			case(9):
				GraphManager.checkGraphSetForConnectedComponents(sc, false);
			break;
			case(10):
				DemonstrationOfGraphDrawing.startDemonstration();
			break;
			default:
				System.out.println("Invalid input."); //Should never be reached
			break;
		}
	}	
}
