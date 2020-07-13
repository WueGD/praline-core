package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.*;

/**
 * GalaxyMultiLevel as Java implementation of
 * C++ GalaxyMultilevel in OGDF there from Martin Gronemann (class in files GalaxyMultilevel.h + GalaxyMultilevel.cpp).
 *
 * @param <V> Type of vertices
 * @param <E> Type of edges
 */
public class GalaxyMultiLevel<V,E> {	
	
	public GalaxyMultiLevel<V,E> finerMultiLevel;
	public GalaxyMultiLevel<V,E> coarserMultiLevel;
	public Graph<V, E> graph;
	public Collection<RelevantLevelNodeInfo<V>> relevantNodeInfos;
	private int levelNumber;
	
	public int getLevelNumber(){
		return levelNumber;
	}
	
	
	
	public GalaxyMultiLevel(Graph<V, E> graph) {
		this.graph = graph;
		finerMultiLevel = null;
		coarserMultiLevel = null;
		levelNumber = 0;
	}
	
	public GalaxyMultiLevel(GalaxyMultiLevel<V,E> prev){
		coarserMultiLevel = null;
		finerMultiLevel = prev;
		finerMultiLevel.coarserMultiLevel = this;
		levelNumber = prev.levelNumber + 1;
	}
	
	public Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>> initialize(){
		List<LevelNodeInfo<V>> nodeInfos = new ArrayList<LevelNodeInfo<V>>(graph.getVertexCount());
		List<LevelEdgeInfo<V,E>> edgeInfos = new ArrayList<LevelEdgeInfo<V,E>>(graph.getEdgeCount());
		Map<V, LevelNodeInfo<V>> temporaryVToLevelNodeInfoMap = new LinkedHashMap<>(graph.getVertexCount());
		for(V v: graph.getVertices()){
			LevelNodeInfo<V> levelNodeInfoToV = new LevelNodeInfo<V>(v);
			levelNodeInfoToV.mass = 1.0f;
			nodeInfos.add(levelNodeInfoToV);
			temporaryVToLevelNodeInfoMap.put(v, levelNodeInfoToV);
		}
		//add level edge infos
		for(E e: graph.getEdges()){
			Iterator<V> incidentVerticesIterator = graph.getIncidentVertices(e).iterator();
			LevelEdgeInfo.createLevelEdgeInfo(e, temporaryVToLevelNodeInfoMap.get(incidentVerticesIterator.next()),
					temporaryVToLevelNodeInfoMap.get(incidentVerticesIterator.next()), edgeInfos);
		}

		saveRelevantLevelNodeInfos(nodeInfos);
		
		return new Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>>(nodeInfos, edgeInfos);
	}
	
	public void saveRelevantLevelNodeInfos(Collection<LevelNodeInfo<V>> levelNodeInfos){
		relevantNodeInfos = new ArrayList<RelevantLevelNodeInfo<V>>(levelNodeInfos.size());
		for(LevelNodeInfo<V> levelNodeInfo: levelNodeInfos){
			relevantNodeInfos.add(levelNodeInfo.getRelevantLevelNodeInfo());
		}
	}
}
