package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import com.google.common.base.Supplier;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.Testclass;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.RomeGraphFormatGraphWriter;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.SuperGraphReader;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.InvokeMethod;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.generators.GraphGenerator;
import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

import java.io.File;
import java.util.*;


public class GraphManager extends Manager<GraphReference> {


	@Override
	protected void initialize() {

		File specificationDirectory = new File(Testclass.pathsConfig.pathToGraphSpecifications);
		File[] specifications = specificationDirectory.listFiles();

		//load specifications
		for(File f: specifications){
			GraphReference specification = DataAsJSonFile.getFileContent(f.getPath(), GraphReference.class);
			super.addToObjectList(specification, specification.getShortDescription()+" ("+specification.getNumberOfGraphs()+" graphs)");
		}
	}


	/**
	 * see {@link GraphReference#isGraphsAreStoredInDirectory()}
	 * and {@link GraphReference#getDirectoryOrMethodCallParameter()}
	 *
	 * @param key
	 * @param sc
	 * For interaction (dialog) with the user
	 * @return
	 */
	public List<UndirectedGraph<String, String>> getGraphViaMethod(String key, Scanner sc){
		List<UndirectedGraph<String, String>> returnList = new LinkedList<UndirectedGraph<String, String>>();

		try {
			returnList.add((UndirectedGraph<String, String>) InvokeMethod
					.invokeMethod(key, new Class[] {Scanner.class}, new Object[] {sc}, GraphManager.class, this));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return returnList;
	}

	/**
	 *
	 * @param sc
	 * For interaction (dialog) with the user
	 * @return
	 * Graph selected by the user
	 */
	public UndirectedGraph<String, String> assistantForSelectingOneGraph(Scanner sc) {
		GraphReference gr = super.assistantForSelectingOneObject(sc);

		if(gr.isGraphsAreStoredInDirectory()){
			UndirectedGraph<String, String> graph = null;
			while(graph==null){
				String pathToTheSetOfGraphs = Testclass.pathsConfig.pathToGraphs+File.separator+gr.getDirectoryOrMethodCallParameter();

				String question = "Please enter a path to one graph-file relative to >>"+new File(pathToTheSetOfGraphs).getAbsolutePath()+"<<"
						+ "\nThis relative path must not start with "+File.separator+" (the separator-character)";

				String pathToFileRelative = Dialog.getNextLine(sc, question);

				graph = SuperGraphReader.getGraph(pathToTheSetOfGraphs+File.separator+pathToFileRelative);
				if(graph==null){
					System.err.println("Failed to read graph. Path correct? File-type is known by this program?");
				}
			}
			return graph;
		}

		final List<UndirectedGraph<String, String>> listOfGraphs =
				getGraphViaMethod(gr.getDirectoryOrMethodCallParameter(), sc);
		if(listOfGraphs.size()==1){
			return listOfGraphs.get(0);
		}
		else if(listOfGraphs.size()>1){
			Manager<UndirectedGraph<String, String>> graphSelectionManager = new Manager<UndirectedGraph<String,String>>() {

				@Override
				protected void initialize() {
					for(int i=0; i<listOfGraphs.size(); i++){
						addToObjectList(listOfGraphs.get(i), listOfGraphs.get(i).toString());
					}
				}
			};
			return graphSelectionManager.assistantForSelectingOneObject(sc);
		}
		System.err.println("Failed to create or load a graph!");
		return null;
	}


	/*
	 * static-Methods
	 */


	/**
	 * Uses the {@link EppsteinPowerLawGenerator}<String, String> with 1000 Iterations
	 *
	 * @param sc
	 * For interaction (dialog) with the user (to ask properties of the graph to be created)
	 * @return
	 */
	public static UndirectedGraph<String, String> getRandomGraphFromEppsteinPowerLawGenerator(Scanner sc){
		int numberOfVertices = Dialog.getNextInt(sc, "Enter the number of vertices of the random graph", 0, Integer.MAX_VALUE);
		int numberOfEdges = Dialog.getNextInt(sc, "Enter the number of edges of the random graph", 0, Integer.MAX_VALUE);
		UndirectedGraph<String, String> graph = getRandomGraphFromEppsteinPowerLawGenerator(numberOfVertices, numberOfEdges);
		System.out.println("Created new random graph successfully.");
		return graph;
	}

	/**
	 * see {@link GraphManager#getRandomGraphFromEppsteinPowerLawGenerator(Scanner)}
	 *
	 * @param numberOfVertices
	 * @param numberOfEdges
	 * @return
	 */
	public static UndirectedGraph<String, String> getRandomGraphFromEppsteinPowerLawGenerator(int numberOfVertices, int numberOfEdges){
		GraphGenerator<String, String> graphGenerator = new EppsteinPowerLawGenerator<String, String>(
				new Supplier<Graph<String, String>>(){
					@Override
					public Graph<String, String> get() {
						return new UndirectedSparseGraph<String, String>();
					}
				},
				new Supplier<String>(){
					int vertexCounter =-1;
					@Override
					public String get() {
						vertexCounter++;
						return "v"+vertexCounter;
					}
				},
				new Supplier<String>(){
					int edgeCounter =-1;
					@Override
					public String get() {
						edgeCounter++;
						return "e"+edgeCounter;
					}
				}
				, numberOfVertices, numberOfEdges, 1000);
		Graph<String, String> g = graphGenerator.get();
		return (UndirectedGraph<String, String>) g;
	}



	public static void saveRandomGraphsAsNewGraphSet(Scanner sc){
		System.out.println("New random graphs are created via (EppsteinPowerLawGenerator<String, String>  with 1000 iterations),");
		System.out.println("that are not necessarily connected (in practice they are not, there is mostly just one largest component).");
		System.out.println("To have a connected graph, from the created graph only the largest connected component is taken.");
		System.out.println("Furthermore self-edges (self-loops; an edge from one vertex to itself) are removed if there are some.");
		System.out.println("_____________");
		int numberOfGraphs = Dialog.getNextInt(sc, "Enter the number of graphs to be created");
		int minV = Dialog.getNextInt(sc, "Enter the minimal number of vertices (Note: The finally saved graph might have fewer vertices, "
				+ "because only the largest component is taken (This minimum holds before reduction to one component.)");
		int maxV = Dialog.getNextInt(sc, "Enter the maximal number of vertices.");
		double minEFactor = Dialog.getNextDouble(sc, "Enter a minimal factor for the number of edges relative the number of vertices.");
		double maxEFactor = Dialog.getNextDouble(sc, "Enter a maximal factor for the number of edges relative the number of vertices.");

		String directoryName = Dialog.getNextLine(sc, "Enter a name for the folder in which the new graph set will be saved");
		String descriptionToTheNewGraphSet = Dialog.getNextLine(sc, "Enter a Name/short description to the new set of graphs");

		File directory = new File(Testclass.pathsConfig.pathToGraphs+File.separator+directoryName);
		directory.mkdirs();

		DataAsJSonFile.writeFile(new GraphReference(true, directoryName, numberOfGraphs, descriptionToTheNewGraphSet),
				Testclass.pathsConfig.pathToGraphSpecifications+File.separator+directoryName+".json");

		RomeGraphFormatGraphWriter rgrgw = new RomeGraphFormatGraphWriter();

		for(int i=0; i<numberOfGraphs; i++){
			//int numberOfVertices = (int) (minV + Math.random()*(double)(maxV-minV));
			//int numberOfEdges = (int) (numberOfVertices*(minEFactor + Math.random()*(maxEFactor-minEFactor)));
			int numberOfVertices = (int) (minV + i * ( ((double)(maxV-minV)) / (numberOfGraphs-1) ));
			int numberOfEdges = (int) (numberOfVertices * (minEFactor + i * ((maxEFactor-minEFactor)/(numberOfGraphs-1))));

			UndirectedGraph<String, String> graph = getRandomGraphFromEppsteinPowerLawGenerator(numberOfVertices, numberOfEdges);


			while(true){
				try{
					//find connected components
					WeakComponentClusterer<String, String> clusterer = new WeakComponentClusterer<String, String>();
					Set<Set<String>> allComponents = clusterer.apply(graph);

					//find largest one - remove smaller one immediately
					Set<String> largestComponent = null;
					int sizeOfLargestComponent = -1;
					for(Set<String> component: allComponents){
						if(component.size()>sizeOfLargestComponent){
							//remove prev largest
							removeSetOfVerticesFromGraph(largestComponent, graph);
							sizeOfLargestComponent = component.size();
							largestComponent = component;
						}
						//if not larger -> kick out
						else{
							removeSetOfVerticesFromGraph(component, graph);
						}
					}

					//remove self-loops
					for(String e: graph.getEdges()){
						Pair<String> endpoints = graph.getEndpoints(e);
						if(endpoints.getFirst()==endpoints.getSecond()){
							graph.removeEdge(e);
						}
					}



					break;
				}
				catch(ConcurrentModificationException c){}
			}



			//write it to the file system
			rgrgw.writeFile(Testclass.pathsConfig.pathToGraphs+File.separator+directoryName,
					"V"+graph.getVertexCount()+"E"+graph.getEdgeCount(), graph);

			System.out.println("File "+(i+1)+"/"+numberOfGraphs+" created");
		}
	}

	private static void removeSetOfVerticesFromGraph(Set<String> vertices, UndirectedGraph<String, String> graph){
		if(graph==null) return;
		if(vertices==null) return;
		for(String v: vertices){
			graph.removeVertex(v);
		}
	}



	public static void checkGraphSetForConnectedComponents(Scanner sc, boolean onlyTextOutputOfVertexCountAndEdgeCount){
		numberOfGraphsWithMoreThanOneComponent = 0;
		numberOfGraphsWithNoComponentWithAtLeastXVertices = 0;
		numberOfGraphsWithMoreThanOneComponentWithAtLeastXVertices = 0;
		numberOfConnectedComponents = new HashMap<Integer, Integer>();
		sizeOfTheConnectedComponents = new HashMap<Integer, Integer>();
		sizeOfTheGraphs = new HashMap<Integer, Integer>();
		rgfgw = new RomeGraphFormatGraphWriter();


		GraphManager graphManager = new GraphManager();
		GraphReference gr = graphManager.assistantForSelectingOneObject(sc);
		if(!gr.isGraphsAreStoredInDirectory()){
			System.out.println("This is no set of graphs stored in the file system. Analysis not possible.");
			return;
		}

		if(!onlyTextOutputOfVertexCountAndEdgeCount){
			extractGraphFiles = Dialog.assistantConfirm(sc, "Save all components with a special minimum size "
					+ "(specified in the next step) as a new graph set in your file system?");
			if(extractGraphFiles){
				x = Dialog.getNextInt(sc, "Specify the minimal number x of vertices needed per component", 0, Integer.MAX_VALUE);
			}
			while(extractGraphFiles){
				nameOfNewFolder = Dialog.getNextLine(sc, "Enter a name for the new folder.");
				File newDir = new File(Testclass.pathsConfig.pathToGraphs+File.separator+nameOfNewFolder);
				if(newDir.exists()){
					System.out.println("Name exists already. Choose another one.");
				}
				else{
					break;
				}
			}
		}

		String dirName = gr.getDirectoryOrMethodCallParameter();
		//check graphs
		File graphDir = new File(Testclass.pathsConfig.pathToGraphs+File.separator+dirName);

		explorDirectoryRecursively(graphDir, "", onlyTextOutputOfVertexCountAndEdgeCount);

		System.out.println();
		if(!onlyTextOutputOfVertexCountAndEdgeCount){
			System.out.println("Check completed. "+numberOfGraphsWithMoreThanOneComponent+" graphs with more than 1 connected component found. "
					+ "So often so many components:");
			for(Integer numberOfComponents: numberOfConnectedComponents.keySet()){
				System.out.println("With "+numberOfComponents+" components: "+numberOfConnectedComponents.get(numberOfComponents));
			}
			System.out.println();
			System.out.println("Listing of component-sizes (In brackets how many graphs with that size are in the graph set):");
			for(Integer numberOfVertices: sizeOfTheConnectedComponents.keySet()){
				int numberOfGraphs = 0;
				if(sizeOfTheGraphs.containsKey(numberOfVertices)){
					numberOfGraphs = sizeOfTheGraphs.get(numberOfVertices);
				}
				System.out.println("With "+numberOfVertices+" vertices: "+sizeOfTheConnectedComponents.get(numberOfVertices)+" ("+numberOfGraphs+")");
			}
			System.out.println();
			System.out.println(numberOfGraphsWithNoComponentWithAtLeastXVertices+" graphs have no component with min. "+x+" vertices.");
			System.out.println(numberOfGraphsWithMoreThanOneComponentWithAtLeastXVertices+" graphs have more than 1 component with min. "+x+" vertices.");
		}
		else{
			System.out.println("Listing of numbers of graphs with their number of vertices:");
			LinkedList<Integer> sortedKeys = new LinkedList<Integer>(sizeOfTheGraphs.keySet());
			Collections.sort(sortedKeys);
			for(Integer numberOfVertices: sortedKeys){
				System.out.println("With "+numberOfVertices+" vertices: "+sizeOfTheGraphs.get(numberOfVertices));
			}
		}
	}
	//vars for the values of this routine
	private static boolean extractGraphFiles;
	private static String nameOfNewFolder;
	private static int numberOfGraphsWithMoreThanOneComponent;
	private static int numberOfGraphsWithNoComponentWithAtLeastXVertices;
	private static int numberOfGraphsWithMoreThanOneComponentWithAtLeastXVertices;
	private static int x = 10;
	private static Map<Integer, Integer> numberOfConnectedComponents;
	private static Map<Integer, Integer> sizeOfTheConnectedComponents;
	private static Map<Integer, Integer> sizeOfTheGraphs;
	private static RomeGraphFormatGraphWriter rgfgw;


	private static void explorDirectoryRecursively(File directory, String relativePathToThisDir,
												   boolean onlyTextOutputOfVertexCountAndEdgeCount){
		File[] children = directory.listFiles();
		for(File child: children){
			//is file, no dir
			if(!child.isDirectory()){
				UndirectedGraph<String, String> graph = SuperGraphReader.getGraph(child.getPath());
				if(graph==null){
					continue; //either no graph-file or failed reading
				}

				if(!sizeOfTheGraphs.containsKey(graph.getVertexCount())){
					sizeOfTheGraphs.put(graph.getVertexCount(), 1);
				}
				else{
					sizeOfTheGraphs.put(graph.getVertexCount(),
							sizeOfTheGraphs.get(graph.getVertexCount())+1);
				}

				if(onlyTextOutputOfVertexCountAndEdgeCount){
					System.out.println(child.getPath()+" has "+graph.getVertexCount()+" vertices and "+graph.getEdgeCount()+" edges.");
				}
				else{
					WeakComponentClusterer<String, String> clusterer = new WeakComponentClusterer<String, String>();
					Set<Set<String>> components = clusterer.apply(graph);


					if(components.size()>1){
						numberOfGraphsWithMoreThanOneComponent++;
					}
					System.out.print(child.getPath()+" has "+components.size()+" components. (sizes:");
					int numberOfComponentsWithMinXVertices = 0;
					int countindex = 0;
					for(Set<String> component: components){
						System.out.print(" "+component.size());
						if(!sizeOfTheConnectedComponents.containsKey(component.size())){
							sizeOfTheConnectedComponents.put(component.size(), 1);
						}
						else{
							sizeOfTheConnectedComponents.put(component.size(),
									sizeOfTheConnectedComponents.get(component.size())+1);
						}

						if(component.size()>=x){
							if(extractGraphFiles){
								File dirToSave = new File(Testclass.pathsConfig.pathToGraphs+File.separator
										+nameOfNewFolder+File.separator+"V"+component.size()+relativePathToThisDir);
								dirToSave.mkdirs();
								rgfgw.writeFile(dirToSave.getPath(), child.getName()+"(comp"+countindex+")", graph);
							}

							numberOfComponentsWithMinXVertices++;
						}
						countindex++;
					}

					System.out.println(")");
					if(numberOfComponentsWithMinXVertices>1){
						numberOfGraphsWithMoreThanOneComponentWithAtLeastXVertices++;
					}
					else if(numberOfComponentsWithMinXVertices==0){
						numberOfGraphsWithNoComponentWithAtLeastXVertices++;
					}

					if(!numberOfConnectedComponents.containsKey(components.size())){
						numberOfConnectedComponents.put(components.size(), 1);
					}
					else{
						numberOfConnectedComponents.put(components.size(),
								numberOfConnectedComponents.get(components.size())+1);
					}
				}
			}
			//is dir, no file
			else{
				explorDirectoryRecursively(child, relativePathToThisDir+File.separator+child.getName(),
						onlyTextOutputOfVertexCountAndEdgeCount);
			}
		}
	}
}