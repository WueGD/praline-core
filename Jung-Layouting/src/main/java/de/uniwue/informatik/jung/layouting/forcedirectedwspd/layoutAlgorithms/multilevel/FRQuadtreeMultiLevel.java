package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Constants;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.AlgorithmReference;

import java.awt.*;

public class FRQuadtreeMultiLevel<V, E> extends MultiLevelLayout<V, E>{
	public FRQuadtreeMultiLevel(Graph<V, E> graph, double sOrTheta, Dimension size) {
		this(graph, sOrTheta, size, Constants.random.nextLong());
	}


	public FRQuadtreeMultiLevel(Graph<V, E> graph, double sOrTheta, Dimension size, long seed) {
		super(graph, size, new AlgorithmReference(
				
				
				FRQuadtree.class
				
				
				), sOrTheta, seed);
	}
}