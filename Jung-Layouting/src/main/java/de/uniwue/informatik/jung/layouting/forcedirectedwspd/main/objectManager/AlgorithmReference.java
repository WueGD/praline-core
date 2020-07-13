package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.jungmodify.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.multilevel.FRQuadtreeMultiLevel;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.quadtree.FRQuadtree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.cPlusPlus.CPlusPlusExternLayout;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Object for Specification of one algorithm.
 * <br>
 * Stores those information:
 * <ul>
 *  <li>
 *   ({@link AbstractLayout}-) {@link Class}
 *  </li>
 *  <li>
 *   {@link AlgorithmType} of this class
 *  </li>
 * </ul>
 *
 */
public class AlgorithmReference {
	
	/**
	 * static-method for checking
	 */
	public static boolean conatinsAlgorithmOfAlgorithmType(List<AlgorithmReference> list, AlgorithmType algorithmType){
		for(AlgorithmReference ar: list){
			if(ar.getAlgorithmType() == algorithmType){
				return true;
			}
		}
		return false;
	}
	
	
	
	
	/*
	 * Instance-variables and Instance-methods
	 */
	
	
	private String className;
	private AlgorithmType algorithmType;
	
	
	/**
	 * The class is saved in a new instance and the correct {@link AlgorithmType} is
	 * assigned automatically.
	 * <p>
	 * For modification of automatical assignment of the {@link AlgorithmType} change the
	 * source code here in this method
	 * 
	 * @param layoutClass
	 */
	public AlgorithmReference(Class<?> layoutClass) {
		this.className = layoutClass.getName();
		//Automatic determination of the type
		if(LayoutWithWSPD.class.isAssignableFrom(layoutClass)){
			this.algorithmType = AlgorithmType.WITH_WSPD;
		}
		else if(FRQuadtree.class.isAssignableFrom(layoutClass) || FRQuadtreeMultiLevel.class.isAssignableFrom(layoutClass)){
			this.algorithmType = AlgorithmType.WITH_QUADTREE;
		}
		else if(CPlusPlusExternLayout.class.isAssignableFrom(layoutClass)){
			this.algorithmType = AlgorithmType.EXTERN_C_PLUS_PLUS;
		}
		else{
			this.algorithmType = AlgorithmType.OTHER;
		}
	}

	
	/**
	 * Returns the class of the algorithm referenced by this {@link AlgorithmReference}.
	 * Should not be confused with {@link AlgorithmReference#getClass()}.
	 * 
	 * @return
	 */
	public Class<?> getClassOfTheLayout() {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getClassNameOfTheLayout() {
		return className;
	}

	public AlgorithmType getAlgorithmType() {
		return algorithmType;
	}
	
	/**
	 * With that method a class-object referenced with this {@link AlgorithmReference} is instantiated and returned.
	 * These class-objects must be at least from the class {@link AbstractLayout} (should be already true for
	 * all algorithms specified in {@link AlgorithmManager#initialize()}).
	 * 
	 * @param graph
	 * Graph for the layout that shall be instantiated
	 * @param size
	 * Size for the layout that shall be instantiated
	 * @param sOrTheta
	 * Only relevant if a layout of type {@link AlgorithmType#WITH_WSPD} (as s-value for the WSPD)
	 * or of type {@link AlgorithmType#WITH_QUADTREE} (as theta-value for the algorithm)
	 * is specified by this {@link AlgorithmReference}.
	 * If it is not of those types it can have an arbitrary value (is never used).
	 * To query a algorithm of which type is referenced here call {@link AlgorithmReference#getAlgorithmType()}
	 * 
	 * @return
	 */
	public <V,E> AbstractLayout<V, E> getNewInstance(Graph<V, E> graph, Dimension size,
			double sOrTheta) {
		
		AbstractLayout<V, E> layout = null;
		
		Class<?> layoutClass = null;
		try {
			layoutClass = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		
		if(algorithmType==AlgorithmType.WITH_WSPD){
			try {
				Constructor<?> con = layoutClass.getConstructor(Graph.class, double.class, Dimension.class);
				layout = (AbstractLayout<V, E>) con.newInstance(graph, sOrTheta, size);
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
					IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		else if(algorithmType==AlgorithmType.WITH_QUADTREE){
			try {
				Constructor<?> con = layoutClass.getConstructor(Graph.class, double.class, Dimension.class);
				layout = (AbstractLayout<V, E>) con.newInstance(graph, sOrTheta, size);
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
					IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		//other layout-type
		else{
			
			
			if(layoutClass == KKLayout.class){
				try {
					Constructor<?> con = layoutClass.getConstructor(Graph.class);
					layout = (KKLayout<V, E>) con.newInstance(graph);
					layout.setSize(size);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
						IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			//other ones that are not KKLayout
			else{
				try {
					Constructor<?> con = layoutClass.getConstructor(Graph.class, Dimension.class);
					layout = (AbstractLayout<V, E>) con.newInstance(graph, size);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
						IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			
			
		}
			
		//Because of the implementation of FRLayout2 (other than FRLayout) it is needed to do this extra step
		if(FRLayout2.class.isInstance(layout)){
			layout.setSize(size);
		}
		
		return layout;
	}
}
