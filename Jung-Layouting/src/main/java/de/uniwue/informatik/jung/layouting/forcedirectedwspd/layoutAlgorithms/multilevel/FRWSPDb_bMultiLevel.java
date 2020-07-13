package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDb_b;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRWSPDb_bMultiLevel<V, E> extends FRWSPDMultiLevel<V, E>{
	public FRWSPDb_bMultiLevel(Graph<V, E> graph, double sOrTheta, Dimension size) {
		super(graph, size, new AlgorithmReference(
				
				
				FRWSPDb_b.class
				
				
				), sOrTheta);
	}
}