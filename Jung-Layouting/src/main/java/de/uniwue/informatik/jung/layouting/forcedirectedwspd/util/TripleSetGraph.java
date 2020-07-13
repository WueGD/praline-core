package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Graph-object using the {@link VertexTriple}s from {@link FRLayoutNoMaps}
 * <p>
 * By its information this object can be interpreted as drawing
 * instead of the more general concept of a graph.
 * This is because {@link VertexTriple} contains information
 * about the location of a vertex within a drawing.
 */
public class TripleSetGraph<V,E> implements Graph<VertexTriple<V>,
		EdgeTriple<V,E>>{

	List<VertexTriple<V>> vertices;
	List<EdgeTriple<V,E>> edges;
	
	/**
	 * Warning! If the vertexSet matches the edgeSet is not checked!
	 * May lead to inconsistences if it does not fit.
	 * 
	 * @param vertexSet
	 * @param edgeSet
	 */
	public TripleSetGraph(List<VertexTriple<V>> vertexSet,
			List<EdgeTriple<V,E>> edgeSet) {
		this.vertices = vertexSet;
		this.edges = edgeSet;
	}
	
	@Override
	public Collection<EdgeTriple<V,E>> getEdges() {
		return (Collection<EdgeTriple<V,E>>)edges;
	}

	@Override
	public Collection<VertexTriple<V>> getVertices() {
		return (Collection<VertexTriple<V>>)vertices;
	}

	@Override
	public boolean containsVertex(VertexTriple<V> vertex) {
		return vertices.contains(vertex);
	}

	@Override
	public boolean containsEdge(
			EdgeTriple<V,E> edge) {
		return edges.contains(edge);
	}

	@Override
	public int getEdgeCount() {
		return edges.size();
	}

	@Override
	public int getVertexCount() {
		return vertices.size();
	}

	@Override
	public Collection<VertexTriple<V>> getNeighbors(
			VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<EdgeTriple<V,E>> getIncidentEdges(
			VertexTriple<V> vertex) {
		ArrayList<EdgeTriple<V,E>> returnList = 
				new ArrayList<EdgeTriple<V,E>>(8);
		for(EdgeTriple<V,E> e: edges){
			if(e.get2() == vertex){
				returnList.add(e);
			}
			else if(e.get3() == vertex){
				returnList.add(e);
			}
		}
		return returnList;
	}

	@Override
	public Collection<VertexTriple<V>> getIncidentVertices(
			EdgeTriple<V,E> edge) {
		ArrayList<VertexTriple<V>> returnList = new ArrayList<VertexTriple<V>>(2);
		returnList.add(edge.get2());
		returnList.add(edge.get3());
		return returnList;
	}

	@Override
	public EdgeTriple<V,E> findEdge(
			VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<EdgeTriple<V,E>> findEdgeSet(
			VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean addVertex(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addEdge(
			EdgeTriple<V,E> edge,
			Collection<? extends VertexTriple<V>> vertices) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addEdge(
			EdgeTriple<V,E> edge,
			Collection<? extends VertexTriple<V>> vertices,
			EdgeType edge_type) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removeVertex(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removeEdge(
			EdgeTriple<V,E> edge) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isNeighbor(VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isIncident(
			VertexTriple<V> vertex,
			EdgeTriple<V,E> edge) {
		return edge.get2()==vertex || edge.get3()==vertex;
	}

	@Override
	public int degree(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int getNeighborCount(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int getIncidentCount(
			EdgeTriple<V,E> edge) {
		return 2; //With "normal" (not the more general hypergraphs) it is always 2(, or not?)
	}

	@Override
	public EdgeType getEdgeType(
			EdgeTriple<V,E> edge) {
		return EdgeType.UNDIRECTED;
	}

	@Override
	public EdgeType getDefaultEdgeType() {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<EdgeTriple<V,E>> getEdges(
			EdgeType edge_type) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getEdgeCount(EdgeType edge_type) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public Collection<EdgeTriple<V,E>> getInEdges(
			VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<EdgeTriple<V,E>> getOutEdges(
			VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<VertexTriple<V>> getPredecessors(
			VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<VertexTriple<V>> getSuccessors(
			VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int inDegree(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int outDegree(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public boolean isPredecessor(VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isSuccessor(VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int getPredecessorCount(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int getSuccessorCount(VertexTriple<V> vertex) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public VertexTriple<V> getSource(
			EdgeTriple<V,E> directed_edge) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public VertexTriple<V> getDest(
			EdgeTriple<V,E> directed_edge) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isSource(
			VertexTriple<V> vertex,
			EdgeTriple<V,E> edge) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isDest(
			VertexTriple<V> vertex,
			EdgeTriple<V,E> edge) {
		//TODO
		try {
			throw new Exception("Not implemented yet!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addEdge(
			EdgeTriple<V,E> e,
			VertexTriple<V> v1,
			VertexTriple<V> v2) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addEdge(
			EdgeTriple<V,E> e,
			VertexTriple<V> v1,
			VertexTriple<V> v2, EdgeType edgeType) {
		//TODO
		try {
			throw new Exception("Not implemented yet resp. a modification of the graph is currently not planned!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public Pair<VertexTriple<V>> getEndpoints(
			EdgeTriple<V,E> edge) {
		return new Pair<VertexTriple<V>>(edge.get2(), edge.get3());
	}

	@Override
	public VertexTriple<V> getOpposite(
			VertexTriple<V> vertex,
			EdgeTriple<V,E> edge) {
		return edge.get2()==vertex ? edge.get3() : edge.get2(); //TODO Note: It is not checked if the passed vertex belongs to the passed edge
	}
	

}
