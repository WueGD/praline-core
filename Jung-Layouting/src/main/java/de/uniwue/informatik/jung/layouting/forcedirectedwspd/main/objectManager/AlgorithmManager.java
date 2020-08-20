package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.FRLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.jungmodify.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRGrid;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrameAlwaysRep;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDb_b;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDp_b;

import java.awt.*;
import java.util.LinkedList;

/** 
 * If you want to change the set of algorithms that can be chosen,
 * then change this source code and also adjust it in {@link AlgorithmReference}!
 *
 */
public class AlgorithmManager extends Manager<AlgorithmReference>{
	
	/**
	 * Modify the source code of this method if wanted.
	 * Care: Check also in {@link AlgorithmReference#getNewInstance(Graph, Dimension, double)} and in the
	 * constructor {@link AlgorithmReference#Algorithmusreferenz(Class)} if it has to be
	 * adjusted also then.
	 * <p>
	 * Add layouts to {@link Manager#allObjects}.
	 * Names to the layouts are added afterwards automatically.
	 * All layouts added must have {@link AbstractLayout} as super-class.
	 */
	@Override
	protected void initialize() {
		LinkedList<AlgorithmReference> algorithmsToBeAdded = new LinkedList<AlgorithmReference>();
		/*
		 * FRLayout in these Variations
		 *  - original Jung
		 *  - without maps
		 *  - without maps and frame (in 2 variants)
		 *  - without maps and frame and with grid
		 *  - without maps and frame and with WSPD (in 2 variants)
		 *  - without maps and frame and with Quadtree
		 */
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayout.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMaps.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMapsNoFrame.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMapsNoFrameAlwaysRep.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRGrid.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRWSPDp_b.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRWSPDb_b.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRQuadtree.class));
		/*
		 * FRLayout2 in these Variations:
		 *  - original Jung
		 */
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayout2.class));
		/*
		 * KKLayout
		 */
		algorithmsToBeAdded.add(new AlgorithmReference(KKLayout.class));

		/*
		 * All as Multilevel (except FRLayout2)
		 */
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMapsMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMapsNoFrameMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRLayoutNoMapsNoFrameAlwaysRepMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRGridMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRWSPDp_bMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRWSPDb_bMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(FRQuadtreeMultiLevel.class));
		algorithmsToBeAdded.add(new AlgorithmReference(KKLayoutMultiLevel.class));
		
		//read all class names and use them as object-names
		for(AlgorithmReference ar: algorithmsToBeAdded){
			super.addToObjectList(ar, ar.getClassOfTheLayout().getSimpleName());
		}
	}
}
