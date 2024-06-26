package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDb_b;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDp_b;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRWSPDp_bMultiLevel<V, E> extends FRWSPDMultiLevel<V, E>{
	public FRWSPDp_bMultiLevel(Graph<V, E> graph, double sOrTheta, Dimension size) {
		this(graph, sOrTheta, size, Constants.random.nextLong());
	}


	public FRWSPDp_bMultiLevel(Graph<V, E> graph, double sOrTheta, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(


				FRWSPDb_b.class


		), sOrTheta, seed);
	}
}