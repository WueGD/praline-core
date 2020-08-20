package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRGrid;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRGridMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRGridMultiLevel(Graph<V, E> graph, Dimension size) {
		this(graph, size, Constants.random.nextLong());
	}

	public FRGridMultiLevel(Graph<V, E> graph, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				FRGrid.class
				
				
				), 0.0, seed);
	}
}