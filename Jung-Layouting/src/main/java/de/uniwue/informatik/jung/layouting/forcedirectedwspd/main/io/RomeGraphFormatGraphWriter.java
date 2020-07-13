package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Writer analog to {@link RomeGraphFormatGraphReader}
 *
 */
public class RomeGraphFormatGraphWriter {
	
	
	/**
	 * No vertex-/edge-names are saved, only the structure.
	 * 
	 * @param path
	 * Path where the graph-file will be saved.
	 * Only until the directory in which this file will be (no file-name).
	 * Path must exist, if not it is not created but the writing is aborted.
	 * @param fileName
	 * Name of the file to be created. An ending is not added (because no ending is known for Rome-graphs)
	 * @param graph
	 * @return
	 * false, if aborted (or unsuccessful more general), true if successful
	 */
	public boolean writeFile(String path, String fileName, UndirectedGraph<String, String> graph){
		File dir = new File(path);
		if(!dir.exists()){
			System.err.println("Path ("+path+") not found. Did not write RomeGraphFormat-file.");
			return false;
		}
		
		try {
			File f = new File(path+File.separator+fileName);
			while(f.exists()){
				fileName += "0";
				f = new File(path+File.separator+fileName);
			}
			FileWriter fw = new FileWriter(path+File.separator+fileName, false);
			
			Map<String, Integer> vertexToIndex = new LinkedHashMap<String, Integer>();
			Iterator<String> vertexIterator = graph.getVertices().iterator();
			for(int i=1; i<=graph.getVertexCount(); i++){
				fw.append(i+" 0"+System.lineSeparator());
				vertexToIndex.put(vertexIterator.next(), i);
			}
			
			fw.append("#");
			
			int i=1; 
			for(String e: graph.getEdges()){
				Pair<String> endpoints = graph.getEndpoints(e);
				fw.append(System.lineSeparator()+i+" 0 "+vertexToIndex.get(endpoints.getFirst())+" "+vertexToIndex.get(endpoints.getSecond()));
				i++;
			}
			fw.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
