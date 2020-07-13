package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.Tuple;

import java.util.Collection;

/**
 * Create new {@link LevelEdgeInfo}s by calling {@link LevelNodeInfo#createLevelEdgeInfo(Object, LevelNodeInfo, LevelNodeInfo)}
 * Constructor here is private
 */
public class LevelEdgeInfo<V,E> {
	public static final float INITIAL_EDGE_LENGTH = 0f;
	
	public E edge;
	public float length = INITIAL_EDGE_LENGTH;
	public LevelNodeInfo<V> v1Info;
	public LevelNodeInfo<V> v2Info;
	
	private LevelEdgeInfo(E edge, LevelNodeInfo<V> v1Info, LevelNodeInfo<V> v2Info) {
		this.edge = edge;
		this.v1Info = v1Info;
		this.v2Info = v2Info;
	}
	
	/**
	 * 
	 * @param edge
	 * @param v1Info
	 * @param v2Info
	 * @param levelEdgeInfoCollection
	 * If not null then the created {@link LevelEdgeInfo} (see {@link LevelNodeInfo#neighbors}) is added to the passed {@link Collection}	
	 * @return
	 */
	public static <V,E> LevelEdgeInfo<V,E> createLevelEdgeInfo(E edge, LevelNodeInfo<V> v1Info, LevelNodeInfo<V> v2Info,
			Collection<LevelEdgeInfo<V,E>> levelEdgeInfoCollection){
		LevelEdgeInfo<V,E> edgeInfo = new LevelEdgeInfo<V,E>(edge, v1Info, v2Info);
		edgeInfo.length += v1Info.radius + v2Info.radius;
		v1Info.neighbors.add(new Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>>(edgeInfo, v2Info));
		v2Info.neighbors.add(new Tuple<LevelEdgeInfo<V,?>, LevelNodeInfo<V>>(edgeInfo, v1Info));
		if(levelEdgeInfoCollection!=null){
			levelEdgeInfoCollection.add(edgeInfo);
		}
		return edgeInfo;
	}
}