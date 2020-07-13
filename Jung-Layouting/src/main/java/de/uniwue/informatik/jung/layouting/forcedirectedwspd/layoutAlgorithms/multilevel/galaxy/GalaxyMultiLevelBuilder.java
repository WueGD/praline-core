package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Randomness;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * GalaxyMultiLevelBuilder as Java implementation of
 * C++ GalaxyMultilevelBuilder in OGDF there from Martin Gronemann (class in files GalaxyMultilevel.h + GalaxyMultilevel.cpp).
 *
 * @param <V> Type of vertices
 * @param <E> Type of edges
 */
public class GalaxyMultiLevelBuilder<V,E> {
	
	public final int M_DIST = 2;
	

	private Graph<V,E> prevGraph;
	private List<LevelNodeInfo<V>> sunNodeList;
	private List<LevelNodeInfo<V>> prevNodeInfos;
	private List<LevelEdgeInfo<V,E>> prevEdgeInfos;
	private List<LevelNodeState<V, E>> nodeStates;
	
	
	public Tuple<GalaxyMultiLevel<V,E>,  Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>>> build(
			GalaxyMultiLevel<V,E> prevMultiLevel, Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>> prevLevelInfos){
		this.prevGraph = prevMultiLevel.graph;
		this.prevNodeInfos = prevLevelInfos.get1();
		this.prevEdgeInfos = prevLevelInfos.get2();
		
		nodeStates = new LinkedList<LevelNodeState<V, E>>();
		for(LevelNodeInfo<V> nodeInfo: prevNodeInfos){
			nodeStates.add(new LevelNodeState<V, E>(nodeInfo));
		}
		
		this.computeSystemMass();
		this.sortNodesBySystemMass();
		this.labelSystem();
		GalaxyMultiLevel<V,E> resultingMultiLevel = new GalaxyMultiLevel<V,E>(prevMultiLevel);
		Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V, E>>> levelInfos = this.createResult(resultingMultiLevel);
		resultingMultiLevel.saveRelevantLevelNodeInfos(levelInfos.get1());
		
		return new Tuple<>(resultingMultiLevel, levelInfos);
	}


	private void computeSystemMass() {
		for(LevelNodeState<V, E> nodeState: nodeStates){
			nodeState.sysMass = nodeState.nodeInfo.mass;
			int nodeDegreeCounter = 0;
			for(Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>> incidentEdgeWithItsVertex: nodeState.nodeInfo.neighbors){
				LevelNodeInfo<?> neighborVertex = incidentEdgeWithItsVertex.get2();
				nodeState.sysMass += neighborVertex.mass;
				++nodeDegreeCounter;
			}
			
			if(nodeDegreeCounter==1){
				nodeState.sysMass *= prevGraph.getVertexCount();
			}
		}
	}


	private void sortNodesBySystemMass() {
		/*
		 * Has to be shuffled to a
		 * random order first to avoid a dependency (artifact)
		 * gotten by the order of the vertices in the graph-datastructure
		 * which may not be random but depend on the not random order of nodes
		 * in the graph source file.
		 */
		Collections.shuffle(nodeStates, Randomness.random);
		Collections.sort(nodeStates);
	}


	private void labelSystem() {
		sunNodeList = new ArrayList<LevelNodeInfo<V>>(nodeStates.size()*5/8); //5/8 is some guessed value, may be changed
		for(LevelNodeState<V, E> nodeState: nodeStates){
			nodeState.sysMass = 0; //can be reset after being sorted
			nodeState.label = 0;
			nodeState.lastVisitor = nodeState;
		}
		
		for(LevelNodeState<V, E> nodeState: nodeStates){
			if(nodeState.label==0){
				sunNodeList.add(nodeState.nodeInfo);
				nodeState.label = M_DIST+1;
				nodeState.edgeLengthFromSun = 0.0f;
				labelSystem(nodeState, nodeState, M_DIST, nodeState.edgeLengthFromSun);
			}
		}
	}


	private void labelSystem(LevelNodeState<V, E> u, LevelNodeState<V, E> v,
			int distance, float distanceFloat) {
		if(distance>0){
			for(Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>> incidentEdgeWithItsVertex: v.nodeInfo.neighbors){
				@SuppressWarnings("unchecked")
				LevelNodeState<V,E> w = (LevelNodeState<V, E>) incidentEdgeWithItsVertex.get2().nodeState;
				//original comment from OGDF: this node may have been labeled before but its closer to the current sun
				if(w.label < distance){
					float currDistFromSun = incidentEdgeWithItsVertex.get1().length + distanceFloat;
					//original comment from OGDF: check if we relabeling by a new sun
					if(w.lastVisitor!=u){
						w.lastVisitor = u;
						w.edgeLengthFromSun = currDistFromSun;
					}
					//original comment from OGDF: finally relabel it
					w.edgeLengthFromSun = Math.min(w.edgeLengthFromSun, currDistFromSun);
					w.label = distance;
					labelSystem(u, w, distance-1, currDistFromSun);
				}
			}
		}
	}


	private Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>> createResult(GalaxyMultiLevel<V,E> resultingMultiLevel) {
		resultingMultiLevel.graph = new UndirectedSparseGraph<V,E>();
		Graph<V,E> resultingGraph = resultingMultiLevel.graph;
		
		List<LevelNodeInfo<V>> resultingMultiLevelNodeInfos = new ArrayList<LevelNodeInfo<V>>(sunNodeList.size());
		for(LevelNodeInfo<V> sun: sunNodeList){
			//original comment from OGDF: create all sun nodes
			resultingGraph.addVertex(sun.getVertex());
			//original comment from OGDF: calculate the real system mass. this may not be the same as calculated before
			LevelNodeInfo<V> newNodeInfoToThisVertex = new LevelNodeInfo<V>(sun.getVertex()); //no edges in the graph so far->2 null-values
			newNodeInfoToThisVertex.radius = 0.0f; //must be set to 0 as this may be set to some other initial default value
			newNodeInfoToThisVertex.mass = 0.0f; //must be set to 0 as this may be set to some other initial default value
			sun.setParent(newNodeInfoToThisVertex);
			resultingMultiLevelNodeInfos.add(newNodeInfoToThisVertex);
		}
		
		for(LevelNodeInfo<V> prevVertexInfo: prevNodeInfos){
			LevelNodeState<V, ?> sunOfThisVertex = prevVertexInfo.nodeState.lastVisitor;
			LevelNodeInfo<V> resultingNodeAsParentOfTheSun = sunOfThisVertex.lastVisitor.nodeInfo.getParent();
			prevVertexInfo.setParent(resultingNodeAsParentOfTheSun);
			resultingNodeAsParentOfTheSun.mass += prevVertexInfo.mass;
			resultingNodeAsParentOfTheSun.radius = Math.max(resultingNodeAsParentOfTheSun.radius,
					prevVertexInfo.nodeState.edgeLengthFromSun);
		}
		
		List<LevelEdgeInfo<V,E>> resultingMultiLevelEdgeInfos = new ArrayList<LevelEdgeInfo<V,E>>();
		for(LevelEdgeInfo<V,E> prevEdgeInfo: prevEdgeInfos){
			LevelNodeState<V, ?> v1Stat = prevEdgeInfo.v1Info.nodeState;
			LevelNodeState<V, ?> v2Stat = prevEdgeInfo.v2Info.nodeState;
			LevelNodeState<V, ?> sunV1Stat = v1Stat.lastVisitor;
			LevelNodeState<V, ?> sunV2Stat = v2Stat.lastVisitor;
			if(sunV1Stat!=sunV2Stat){
				E edge = prevEdgeInfo.edge;
				/*
				 * Following step could seem to be time-relevant.
				 * UndirectedSparseGraph uses HashMaps and executes an
				 * .containsKey-operation that is as fast as any other query
				 * (O(1) expected, O(n) w.c.) to these Maps.
				 * So this is seems not to be an exclusive critical point
				 */
				if(resultingGraph.containsEdge(edge)){
					continue; //not adding multiple edges
				}
				LevelNodeInfo<V> resultingSunV1Info = sunV1Stat.nodeInfo.getParent();
				LevelNodeInfo<V> resultingSunV2Info = sunV2Stat.nodeInfo.getParent();
				resultingGraph.addEdge(edge, resultingSunV1Info.getVertex(), resultingSunV2Info.getVertex());
				LevelEdgeInfo<V, E> resultingEdgeInfo = LevelEdgeInfo.createLevelEdgeInfo(edge,
						resultingSunV1Info, resultingSunV2Info, resultingMultiLevelEdgeInfos);
				resultingEdgeInfo.length = v1Stat.edgeLengthFromSun + prevEdgeInfo.length + v2Stat.edgeLengthFromSun;
			}
		}
		return new Tuple<List<LevelNodeInfo<V>>, List<LevelEdgeInfo<V,E>>>(resultingMultiLevelNodeInfos, resultingMultiLevelEdgeInfos);
	}
}
