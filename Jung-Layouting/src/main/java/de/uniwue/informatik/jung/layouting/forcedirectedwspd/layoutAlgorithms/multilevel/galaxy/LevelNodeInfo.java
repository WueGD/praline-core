package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class LevelNodeInfo<V> {
	
	public static final float INITIAL_VERTEX_RADIUS = 0.5f;
	
	private LevelNodeInfo<V> parent;
	private RelevantLevelNodeInfo<V> relevantLevelNodeInfo;


	public float mass;
	public float radius = INITIAL_VERTEX_RADIUS;
	public List<Tuple<V,Integer>> nearSuns;
	public List<Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>>> neighbors;
	public LevelNodeState<V, ?> nodeState = null; //may be set from outside
	
	
	/**
	 * 
	 * @param vertex
	 * @param graph
	 * If null then it is not checked for neigbouring edges
	 * @param levelEdgeInfoCollection
	 * If null then the created {@link LevelEdgeInfo} (see {@link LevelNodeInfo#neighbors}) is not added to a list
	 */
	public <E> LevelNodeInfo(V vertex) {
		this.relevantLevelNodeInfo = new RelevantLevelNodeInfo<V>();
		this.relevantLevelNodeInfo.vertex = vertex;
		neighbors = new ArrayList<Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>>>();
	}
	

	public V getVertex() {
		return this.relevantLevelNodeInfo.vertex;
	}


	public void setVertex(V vertex) {
		this.relevantLevelNodeInfo.vertex = vertex;
	}



	public LevelNodeInfo<V> getParent() {
		return parent;
	}


	public void setParent(LevelNodeInfo<V> parent) {
		this.parent = parent;
		this.relevantLevelNodeInfo.parentRelevantLevelNodeInfo = parent.relevantLevelNodeInfo;
	}

	public RelevantLevelNodeInfo<V> getRelevantLevelNodeInfo() {
		return relevantLevelNodeInfo;
	}
}