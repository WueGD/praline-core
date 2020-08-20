package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.util.data2semantics.tools.graphs.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.io.GraphIOException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * After having problems with org.data2semantics.tools.graphs.{@link GML#read(java.io.File)} this tool is
 * not in use here any more. Instead a self-written reading and parsing of the graph files is done.
 * It made for the hachul graphs and is NO GENERAL GML-READER. It can not handle many features
 * like a directed graph or graph labels, here an exception might be thrown.
 * To handle general gml files adapt this source code.
 * 
 * <p>
 * Note:
 * The parsed graph has Stings as vertices and edges and is made undirected although the graph file
 * may describe a directed graph.
 */
public class GmlFormatGraphReader extends MyGraphReader{
	
	protected static final String NODE_NAME = "node";
	protected static final String EDGE_NAME = "edge";
	protected static final String delimiterString = "\\s*[\\[\\]]\\s*";
	protected static final Pattern NODE_ID_PATTERN = Pattern.compile("\\s*id\\s*(\\d+)\\s*");
	protected static final Pattern EDGE_PATTERN = Pattern.compile("\\s*source\\s*(\\d+)\\s*target\\s*(\\d+)\\s*");
	
	/**
	 * Previous method "readGraph()" that used the data2semantics package.
	 * It did not parse every hachul graph correctly (left out some nodes) and was thus sorted out
	 * and replaced by a hand-written new read method.
	 * 
	 * @return
	 * @throws GraphIOException
	 */
	//@Override
	@Deprecated
	public UndirectedGraph<String, String> readGraphWithData2Semantics() throws GraphIOException {
		Graph<GML.LVertex, Edge<String>> gmlGraph;
		try {
			gmlGraph = GML.read(graphFile);
		} catch (IOException e) {
			throw new GraphIOException();
		}
		//make this gml-graph a undirected graph with vertices and edges as strings.
		UndirectedGraph<String, String> graph = new UndirectedSparseGraph<String, String>();
		//add vertices
		for(GML.LVertex lv: gmlGraph.getVertices()){
			String vName = lv.label;
			if(vName==null || vName.equals("") || vName.equals(" ")){
				vName = "v"+lv.id();
			}
			vertexNumberToVertex.put(lv.id(), vName);
			graph.addVertex(vName);
		}
		//add edges
		int edgeCounter = 0;
		for(Edge<String> e: gmlGraph.getEdges()){
			String eName = e.getLabel();
			if(eName==null || eName=="" || eName==" "){
				eName = "e"+edgeCounter;
			}
			graph.addEdge(eName,
					vertexNumberToVertex.get(gmlGraph.getEndpoints(e).getFirst().id()),
					vertexNumberToVertex.get(gmlGraph.getEndpoints(e).getSecond().id()));
			edgeCounter++;
		}
		return graph;
	}
	
	@Override
	public UndirectedGraph<String, String> readGraph() throws GraphIOException {
		Scanner sc;
		try {
			sc = new Scanner(graphFile).useDelimiter(delimiterString);
		} catch (FileNotFoundException e1) {
			throw new GraphIOException("File not found: "+graphFile.getAbsolutePath());
		}
		
		UndirectedGraph<String, String> graph = new UndirectedSparseGraph<String, String>();
		
		boolean nextIsNode = false;
		boolean nextIsEdge = false;
		int edgeCounter = 0;
		while(sc.hasNext()){
			String element = sc.next();
			if(element.endsWith(NODE_NAME)){
				nextIsNode = true;
			}
			else if(element.endsWith(EDGE_NAME)){
				nextIsEdge = true;
			}
			else if(nextIsNode){
				Matcher nodeIdMatcher = NODE_ID_PATTERN.matcher(element);
				if(nodeIdMatcher.find() && nodeIdMatcher.groupCount()==1){
					int id = Integer.parseInt(nodeIdMatcher.group(1));
					//add vertex
					String vName = "v"+id;
					vertexNumberToVertex.put(id, vName);
					graph.addVertex(vName);
				}
				else{
					throw new GraphIOException("Expected node ID, but found: \""+element+"\"");
				}
				nextIsNode = false;
			}
			else if(nextIsEdge){
				Matcher edgeSpecificationMatcher = EDGE_PATTERN.matcher(element);
				if(edgeSpecificationMatcher.find() && edgeSpecificationMatcher.groupCount()==2){
					int idSrc = Integer.parseInt(edgeSpecificationMatcher.group(1));
					int idTrg = Integer.parseInt(edgeSpecificationMatcher.group(2));
					//add edge
					String eName = "e"+edgeCounter;
					graph.addEdge(eName, vertexNumberToVertex.get(idSrc), vertexNumberToVertex.get(idTrg));
					++edgeCounter;
				}
				else{
					throw new GraphIOException("Expected edge specification, but found: \""+element+"\"");
				}
				nextIsEdge = false;
			}
		}
		sc.close();
		return graph;
	}
	
	
}
