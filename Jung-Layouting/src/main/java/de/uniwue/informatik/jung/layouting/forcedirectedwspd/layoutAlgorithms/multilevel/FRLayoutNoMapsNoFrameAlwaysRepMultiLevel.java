package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrameAlwaysRep;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRLayoutNoMapsNoFrameAlwaysRepMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRLayoutNoMapsNoFrameAlwaysRepMultiLevel(Graph<V, E> graph, Dimension size) {
		this(graph, size, Constants.random.nextLong());
	}

	public FRLayoutNoMapsNoFrameAlwaysRepMultiLevel(Graph<V, E> graph, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				FRLayoutNoMapsNoFrameAlwaysRep.class
				
				
				), 0.0, seed);
	}
}