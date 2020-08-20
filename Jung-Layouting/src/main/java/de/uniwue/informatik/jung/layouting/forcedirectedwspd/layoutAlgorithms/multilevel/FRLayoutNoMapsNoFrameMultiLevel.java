package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRLayoutNoMapsNoFrameMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRLayoutNoMapsNoFrameMultiLevel(Graph<V, E> graph, Dimension size) {
		this(graph, size, Constants.random.nextLong());
	}

	public FRLayoutNoMapsNoFrameMultiLevel(Graph<V, E> graph, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				FRLayoutNoMapsNoFrame.class
				
				
				), 0.0, seed);
	}
}