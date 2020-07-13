package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.galaxy;

/**
 * Compared acc. to {@link LevelNodeState#sysMass}
 */
public class LevelNodeState<V, E> implements Comparable<LevelNodeState<V, E>> {
	public LevelNodeInfo<V> nodeInfo;
	public LevelNodeState<V, E> lastVisitor;
	public double sysMass;
	public int label;
	public float edgeLengthFromSun;
	
	public LevelNodeState(LevelNodeInfo<V> nodeInfo) {
		this.nodeInfo = nodeInfo;
		nodeInfo.nodeState = this;
	}
	

	@Override
	public int compareTo(LevelNodeState<V, E> o) {
		return Double.compare(this.sysMass, o.sysMass);
	}
}