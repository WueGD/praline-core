package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms;

import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import java.awt.*;
import java.util.ConcurrentModificationException;

/**
 * Same as {@link FRLayoutNoMapsNoFrame}.
 * Only difference:
 * Repulsive forces are calculated in every iteration.
 * (Thus it is quasi also {@link FRLayoutNoMaps} with only one modification: No frame is used)
 */
public class FRLayoutNoMapsNoFrameAlwaysRep<V, E> extends FRLayoutNoMapsNoFrame<V, E>{
	
	public FRLayoutNoMapsNoFrameAlwaysRep(Graph<V, E> graph, Dimension d) {
		super(graph, d);
	}

	/**
	 * Almost the same as {@link FRLayoutNoMapsNoFrame#calcRepulsion()}
	 * with only one line being left away.
	 */
	@Override
	protected void calcRepulsion(){
		//Individually done for every component, see calcRepulsion(t)
        for(int i=0; i<vertices.length; i++){
//        	if(converged[i]==true) continue;
	        while(true) {
	            try {
	                for(VertexTriple<V> t : vertices[i]) {
	                    calcRepulsion(t, i);
	                }
	                break;
	            } catch(ConcurrentModificationException cme) {}
	        }
        }
	}
}
