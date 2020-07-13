package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * The Rome-graphs (large set of graphs with 10-100 vertices) are available in this format (example):
 * <p>
 *<br>1 0
 *<br>2 0
 *<br>3 0
 *<br>4 0
 *<br>5 0
 *<br>6 0
 *<br>7 0
 *<br>8 0
 *<br>9 0
 *<br>10 0
 *<br>11 0
 *<br>#
 *<br>1 0 1 6
 *<br>2 0 2 7
 *<br>3 0 7 3
 *<br>4 0 7 4
 *<br>5 0 4 8
 *<br>6 0 8 5
 *<br>7 0 9 1
 *<br>8 0 4 10
 *<br>9 0 10 9
 *<br>10 0 11 2
 * 
 * 
 * <p>
 * First numbers (indices) to the vertices (with name or annotation or weight or sth behind.
 * It seems to be always "0" and is thus ignored here).
 * Then the edges separated from the vertices by a "#".
 * The edges have numbers (indices), too. Behind again a value that is ignored here (seems to be always 0, too).
 * Then the indices of the vertices this edge connects.
 * <br>
 * The parsed graph has Stings as vertices and edges and is undirected.
 */
public class RomeGraphFormatGraphReader extends MyGraphReader {
	
	public RomeGraphFormatGraphReader() {
		super();
	}
	
	public RomeGraphFormatGraphReader(File graphFile) {
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
		//V first
		while(!currentLine.equals("#")){
			graph.addVertex("v"+getKthNumberFromLine(1, currentLine));
			currentLine = readNextLine(sc);
		}
		//E then
		while(sc.hasNextLine()){
			currentLine = readNextLine(sc);
			graph.addEdge("e"+getKthNumberFromLine(1, currentLine), getIthVertexInG(graph, getKthNumberFromLine(3, currentLine)), 
					getIthVertexInG(graph, getKthNumberFromLine(4, currentLine)));
		}
		
		
		sc.close();
		try {
			fis.close();
		} catch (IOException e) {
			throw new GraphIOException(e);
		}
		
		return graph;
	}
}
