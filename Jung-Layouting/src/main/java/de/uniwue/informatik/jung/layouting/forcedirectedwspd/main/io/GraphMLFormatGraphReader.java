package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLReader;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Wrapper to read graphs in GraphML format. Uses the GraphMLReader class from Jung.
 */
public class GraphMLFormatGraphReader extends MyGraphReader{

	@Override
	public UndirectedGraph<String, String> readGraph() throws GraphIOException {
		UndirectedGraph<String, String> g = new UndirectedSparseMultigraph<String, String>(); 
		try {
			GraphMLReader<UndirectedGraph<String, String>, String, String> gmlr = new GraphMLReader<UndirectedGraph<String, String>, String, String>();
			Reader reader = new FileReader(graphFile);
			gmlr.load(reader, g);
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			e1.printStackTrace();
		}
		return g;
	}
}
