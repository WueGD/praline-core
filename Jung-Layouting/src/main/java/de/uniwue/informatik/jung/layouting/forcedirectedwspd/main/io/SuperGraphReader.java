package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphReader;

import java.io.File;

/**
 * Applys a {@link GraphReader} according to the ending of a file.
 * Only a few {@link GraphReader}s being relevant in this project are included.
 * If a graph has not the correct ending or is no Rome-Format-Graph (see {@link RomeGraphFormatGraphReader})
 * then null is returned but no exception is thrown.
 * The name "super" in the class name is no indication for that it shall be a super-class for other classes.
 * <p>
 * {@link GraphReader}s being used by this {@link SuperGraphReader} are:
 * <ul>
 *  <li>
 *   {@link GraphFormatGraphReader}
 *  </li>
 *  <li>
 *   {@link GraphMLFormatGraphReader}
 *  </li>
 *  <li>
 *   {@link GmlFormatGraphReader}
 *  </li>
 *  <li>
 *   {@link RomeGraphFormatGraphReader}
 *  </li>
 * </ul>
 */
public class SuperGraphReader implements GraphReader<UndirectedGraph<String, String>, String, String> {
	
	/**
	 * Static method creating an instance of this class (intern) and returning an {@link UndirectedGraph} if possible.
	 * If not null is returned and no exception is thrown.
	 * 
	 * @param path
	 * @return
	 */
	public static UndirectedGraph<String, String> getGraph(String path){
		SuperGraphReader superGraphReader = new SuperGraphReader();
		superGraphReader.setPath(path);
		UndirectedGraph<String, String> graph = null;
		try {
			graph = superGraphReader.readGraph();
		} catch (GraphIOException e) {
			graph = null;
		}
		return graph;
	}
	
	
	
	protected GraphFormatGraphReader graphFormatGraphReader;
	protected GraphMLFormatGraphReader grapMLGraphReader;
	protected GmlFormatGraphReader gmlReader;
	protected RomeGraphFormatGraphReader romeGraphFormatGraphReader;
	
	private String path;
	
	
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * Returns null if it could not be read (and catching the known exceptions)
	 */
	@Override
	public UndirectedGraph<String, String> readGraph() throws GraphIOException {
		//no path available
		if(path==null){
			return null;
		}
		//path available
		
		try{ //Catch known exceptions in order to return 0 instead
			
			//1. Find the correct GraphReader because of the file ending
			MyGraphReader graphReaderToBeUsed = null;
			if(path.endsWith(".gml")){
				if(gmlReader==null){
					gmlReader = new GmlFormatGraphReader();
				}
				graphReaderToBeUsed = gmlReader;
			}
			else if(path.endsWith(".graph")){
				if(graphFormatGraphReader==null){
					graphFormatGraphReader = new GraphFormatGraphReader();
				}
				graphReaderToBeUsed = graphFormatGraphReader;
			}
			else if(path.endsWith(".graphml")){
				if(grapMLGraphReader==null){
					grapMLGraphReader = new GraphMLFormatGraphReader();
				}
				graphReaderToBeUsed = grapMLGraphReader;
			}
			else{
				if(romeGraphFormatGraphReader==null){
					romeGraphFormatGraphReader = new RomeGraphFormatGraphReader();
				}
				graphReaderToBeUsed = romeGraphFormatGraphReader;
			}
			
			//2. set path to this reader and return its returned graph
			graphReaderToBeUsed.setFile(new File(path));
			return graphReaderToBeUsed.readGraph();
		}
		catch(GraphIOException e){
			return null;
		}
	}

	@Override
	public void close() throws GraphIOException {
		//close all
		if(graphFormatGraphReader!=null){
			graphFormatGraphReader.close();
		}
		if(gmlReader!=null){
			gmlReader.close();
		}
		if(grapMLGraphReader!=null){
			grapMLGraphReader.close();
		}
		if(romeGraphFormatGraphReader!=null){
			romeGraphFormatGraphReader.close();
		}
		//and set them to null
		graphFormatGraphReader = null;
		gmlReader = null;
		grapMLGraphReader = null;
		romeGraphFormatGraphReader = null;
	}

}
