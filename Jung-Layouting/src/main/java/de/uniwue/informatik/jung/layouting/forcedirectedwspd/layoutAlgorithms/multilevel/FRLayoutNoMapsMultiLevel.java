package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRLayoutNoMapsMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRLayoutNoMapsMultiLevel(Graph<V, E> graph, Dimension size) {
		this(graph, size, Constants.random.nextLong());
	}

	public FRLayoutNoMapsMultiLevel(Graph<V, E> graph, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				FRLayoutNoMaps.class
				
				
				), 0.0, seed);
	}
}