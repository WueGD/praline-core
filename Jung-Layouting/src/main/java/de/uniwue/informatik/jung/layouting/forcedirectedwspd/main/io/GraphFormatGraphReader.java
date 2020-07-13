package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Scanner;

/**
 * Reads graphs in the .graph format.
 * <br>
 * .graph means:
 * <br>
 * First line: Number of vertices (let it be n) <space> number of edges
 * <br>
 * Next n lines (in line i): List of vertices space-separated as numbers (ids) to which the i-th vertex is adjacent
 * <p>
 * Note:
 * The parsed graph has Stings as vertices and edges and is undirected although
 * it might be thinkable that directed graphs are saved in the files.
 */
public class GraphFormatGraphReader extends MyGraphReader {
	
	public GraphFormatGraphReader() {
		super();
	}
	
	public GraphFormatGraphReader(File graphFile) {
		super(graphFile);
	}
	
	
	@Override
	public UndirectedGraph<String, String> readGraph() throws GraphIOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(graphFile);
		} catch (FileNotFoundException e) {
			throw new GraphIOException(e);
		}
		Scanner sc = new Scanner(fis);
		
		
		UndirectedSparseGraph<String, String> graph = new UndirectedSparseGraph<String, String>();
		
		String currentLine = readNextLine(sc);
		int numberOfVertices = getKthNumberFromLine(1, currentLine);
		int numberOfEdges = getKthNumberFromLine(2, currentLine);
		
		
		//V first
		for(int i=1; i<=numberOfVertices; i++){
			String newVertex = "v"+i;
			graph.addVertex(newVertex);
			while(true){
				try{
					vertexNumberToVertex.put(i, newVertex);
					break;
				}
				catch(ConcurrentModificationException e){}
			}
		}
		//E then
		int edgeCounter = 1;
		for(int i=1; i<=numberOfVertices; i++){
			currentLine = readNextLine(sc);
			for(int otherEndpoint: getAllNumbersFromLine(currentLine)){
				graph.addEdge("e"+edgeCounter, getIthVertexInG(graph, i),
						getIthVertexInG(graph, otherEndpoint));
				edgeCounter++;
			}
		}
		
		sc.close();
		
		if(numberOfEdges!=graph.getEdgeCount()){
			System.out.println("Warning, number of edges in "+this.graphFile.getPath()+" might not fit to the number of vertices stated "
					+ "there to be.");
		}
		
		try {
			fis.close();
		} catch (IOException e) {
			throw new GraphIOException(e);
		}
		
		return graph;
	}
}
