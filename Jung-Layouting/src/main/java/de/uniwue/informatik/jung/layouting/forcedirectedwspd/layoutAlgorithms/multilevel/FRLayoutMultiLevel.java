package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRLayoutMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRLayoutMultiLevel(Graph<V, E> graph, Dimension size) {
		super(graph, size, new AlgorithmReference(
				
				
				FRLayout.class
				
				
				), 0.0);
	}
}