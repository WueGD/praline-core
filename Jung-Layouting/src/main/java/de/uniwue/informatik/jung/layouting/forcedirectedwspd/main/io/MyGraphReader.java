package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphReader;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.LinkedList;

import java.io.File;
import java.util.*;

public abstract class MyGraphReader implements GraphReader<UndirectedGraph<String, String>, String, String>{
	
	protected File graphFile;
	
	public MyGraphReader(){
		
	}
	
	public MyGraphReader(File graphFile){
		this.graphFile = graphFile;
	}
	
	public void setFile(File graphFile){
		this.graphFile = graphFile;
	}
	

	
	protected String readNextLine(Scanner sc) throws GraphIOException{
		if(sc.hasNextLine()){
			return sc.nextLine();
		}
		throw new GraphIOException("Expected another line in the graph file. File does not fit the scheme of the known graph-file-formats.");
	}
	

	
	/**
	 * k starts here with 1!
	 * 
	 * @param k
	 * @param line
	 * @return
	 * @throws GraphIOException 
	 */
	protected int getKthNumberFromLine(int k, String line) throws GraphIOException{
		int counter = 0;
		int number = 0;
		int stringPointer = 0;
		while(counter<k){
			String numberAsString = "";
			while(stringPointer<line.length() && line.charAt(stringPointer) != ' '){
				numberAsString += line.charAt(stringPointer);
				stringPointer++;
			}
			try{
				number = Integer.parseInt(numberAsString);
			}
			catch(NumberFormatException e){
				throw new GraphIOException();
			}
			stringPointer++;
			counter++;
		}
		return number;
	}
	
	protected List<Integer> getAllNumbersFromLine(String line){
		LinkedList<Integer> allNumbers = new LinkedList<Integer>();
		int stringPointer = 0;
		int currentNumber = 0;
		while(stringPointer<line.length()){
			char currentChar = line.charAt(stringPointer);
			
			if(currentChar==' '){
				if(currentNumber!=0){
					allNumbers.add(currentNumber);
					currentNumber = 0;
				}
			}
			else{
				currentNumber = 10*currentNumber + Integer.parseInt(currentChar+"");
			}
			
			stringPointer++;
		}
		if(currentNumber!=0){
			allNumbers.add(currentNumber);
		}
		return allNumbers;
	}
	
	/**
	 * Care: The string "v1" is not the same as the string "v1" also it is written
	 * identically (different Java-objects).
	 * 
	 * So that only the number is relevant this method returns always the same string-object
	 * to one index i.
	 * So this is a mapping from a vertexNumber to a vertex (because graphs created here use
	 * Strings as vertices).
	 * 
	 * @param graph
	 * @param i
	 * @return
	 */
	protected String getIthVertexInG(UndirectedSparseGraph<String, String> graph, int i){
		if (vertexNumberToVertex.isEmpty()) {
			// fill map
			for (String s : graph.getVertices()) {
				Integer nr = Integer.parseInt(s.substring(1));
				vertexNumberToVertex.put(nr, s);
			}
		}
		while(true){
			try{
				String v = vertexNumberToVertex.get(i);
				
				if(v==null){
					for(String s: graph.getVertices()){
						if(s.equals("v"+i)){
							vertexNumberToVertex.put(i, s);
							return s;
						}
					}
					return null;
				}
				return v;
			}
			catch(ConcurrentModificationException e){}
		}
	}
	
	
	protected Map<Integer, String> vertexNumberToVertex = new LinkedHashMap<Integer, String>();
	

	@Override
	public void close() throws GraphIOException {
		//nothing to write in here so far
	}
	
}
