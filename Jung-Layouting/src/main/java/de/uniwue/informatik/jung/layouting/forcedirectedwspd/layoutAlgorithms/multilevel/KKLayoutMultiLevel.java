package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class KKLayoutMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public KKLayoutMultiLevel(Graph<V, E> graph, Dimension size) {
		this(graph, size, Constants.random.nextLong());
	}

	public KKLayoutMultiLevel(Graph<V, E> graph, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				KKLayout.class
				
				
				), 0.0, seed);
	}
}