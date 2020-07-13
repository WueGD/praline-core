package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.cPlusPlus;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.LongValueFileReader;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Analog to the common {@link Layout}s this is a pseudo-layout (no graph is drawn,
 * but a drawing (graph + locations of the vertices) are saved and represented).
 * It is used for bringing the data from the graphs drawn in C++
 * with the algorithms from the OGDF-library to the JUNG-{@link Layout}-format.
 * 
 * Can be treated like other {@link AbstractLayout}s when visualized or quality evaluated.
 */
public class CPlusPlusExternLayout extends AbstractLayout<String, String>{
	private String pathToCPPDrawingsOfThisGraph;
	private int numberOfTheDrawing;
	
	
	
	/*
	 * 
	 * 
	 * 
	 * **************************************************************************** *
	 * The next 10 classes represent the 10 different OGDF-graph-drawing-algorithms *
	 * that are selectable in the corresponding C++-environment                     *
	 * **************************************************************************** *
	 * 
	 * 
	 * 
	 */
	
	public static class DavidsonHarelLayout extends CPlusPlusExternLayout {
		public DavidsonHarelLayout(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public DavidsonHarelLayout(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class FastMultipoleEmbedder extends CPlusPlusExternLayout {
		public FastMultipoleEmbedder(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public FastMultipoleEmbedder(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class FMMMLayout_single_Level extends CPlusPlusExternLayout {
		public FMMMLayout_single_Level(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public FMMMLayout_single_Level(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class FMMMLayout extends CPlusPlusExternLayout {
		public FMMMLayout(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public FMMMLayout(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class GEMLayout extends CPlusPlusExternLayout {
		public GEMLayout(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public GEMLayout(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class PivotMDS extends CPlusPlusExternLayout {
		public PivotMDS(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public PivotMDS(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class SpringEmbedderFRExact extends CPlusPlusExternLayout {
		public SpringEmbedderFRExact(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public SpringEmbedderFRExact(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class SpringEmbedderFRGridVariant extends CPlusPlusExternLayout {
		public SpringEmbedderFRGridVariant(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public SpringEmbedderFRGridVariant(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class SpringEmbedderKK extends CPlusPlusExternLayout {
		public SpringEmbedderKK(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public SpringEmbedderKK(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	public static class StressMinimization extends CPlusPlusExternLayout {
		public StressMinimization(Graph<String, String> graph, Dimension size) {
			super(graph, size);
		}
		public StressMinimization(Graph<String, String> graph, Dimension size,
				String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException {
			super(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
	}
	
	
	/*
	 * End of the 10 sub-classes
	 */
	
	
	/*
	 * public-static methods for the 10 sub-classes resp. automatic correct assignment/initialization
	 */
	public static List<Class<? extends CPlusPlusExternLayout>> getAllConcreteClasses(){
		ArrayList<Class<? extends CPlusPlusExternLayout>> returnList = new ArrayList<Class<? extends CPlusPlusExternLayout>>(10);
		
		returnList.add(CPlusPlusExternLayout.DavidsonHarelLayout.class);
		returnList.add(CPlusPlusExternLayout.FastMultipoleEmbedder.class);
		returnList.add(CPlusPlusExternLayout.FMMMLayout.class);
		returnList.add(CPlusPlusExternLayout.FMMMLayout_single_Level.class);
		returnList.add(CPlusPlusExternLayout.GEMLayout.class);
		returnList.add(CPlusPlusExternLayout.PivotMDS.class);
		returnList.add(CPlusPlusExternLayout.SpringEmbedderFRExact.class);
		returnList.add(CPlusPlusExternLayout.SpringEmbedderFRGridVariant.class);
		returnList.add(CPlusPlusExternLayout.SpringEmbedderKK.class);
		returnList.add(CPlusPlusExternLayout.StressMinimization.class);

		return returnList;
	}


	/**
	 *
	 * @return
	 *
	 * A concrete sub-class of this (e.g. {@link CPlusPlusExternLayout.GEMLayout}, {@link CPlusPlusExternLayout.PivotMDS}, ...
	 * [all can be gotten via {@link CPlusPlusExternLayout#getAllConcreteClasses()}]).
	 * This is assumed by reading the passed path-string and checking if a concrete class-name is contained in it.
	 * If not the basic class {@link CPlusPlusExternLayout} is returned.
	 *
	 */
	public static Class<? extends CPlusPlusExternLayout> getClassToPath(String pathToCPPDrawingsOfThisGraph){
		for(Class<? extends CPlusPlusExternLayout> c: getAllConcreteClasses()){
			if(pathToCPPDrawingsOfThisGraph.contains(c.getSimpleName())){
				return c;
			}
		}
		return CPlusPlusExternLayout.class;
	}

	/**
	 *
	 *
	 * @return
	 *
	 * A instance of a sub-class of this (e.g. {@link CPlusPlusExternLayout.GEMLayout}, {@link CPlusPlusExternLayout.PivotMDS}, ...
	 * [all can be gotten via {@link CPlusPlusExternLayout#getAllConcreteClasses()}]).
	 * The concrete class is assigned according to the name of the layout appearing in the pathToCPPDrawingsOfThisGraph-String.
	 * If no String of these is contained in that path, the basic-class {@link CPlusPlusExternLayout} is returned.
	 * 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static CPlusPlusExternLayout getCorrectlyAssignedInstance(Graph<String, String> graph, Dimension size,
			String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		//check all concrete classes
		Class<? extends CPlusPlusExternLayout> classToBeCreated = getClassToPath(pathToCPPDrawingsOfThisGraph);
		if(classToBeCreated==CPlusPlusExternLayout.class){
			Constructor<? extends CPlusPlusExternLayout> con =
					getClassToPath(pathToCPPDrawingsOfThisGraph).getConstructor(Graph.class, Dimension.class, String.class, int.class);
			return con.newInstance(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
		}
		//else it is sub-class of CPlusPlusExternLayout
		Constructor<? extends CPlusPlusExternLayout> con =
				getClassToPath(pathToCPPDrawingsOfThisGraph).getConstructor(CPlusPlusExternLayout.class, Graph.class, Dimension.class, String.class, int.class);
		return con.newInstance(new CPlusPlusExternLayout(graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing),
				graph, size, pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
	}
	
	
	
	
	
	
	
	/**
	 * {@link CPlusPlusExternLayout#initialize(String, int)} is called automatically within this
	 * 
	 * @param graph
	 * @param size
	 * @param pathToCPPDrawingsOfThisGraph
	 * @param numberOfTheDrawing
	 * @throws IOException
	 */
	public CPlusPlusExternLayout(Graph<String, String> graph, Dimension size, String pathToCPPDrawingsOfThisGraph,
			int numberOfTheDrawing) throws IOException {
		super(graph, size);
		
		initialize(pathToCPPDrawingsOfThisGraph, numberOfTheDrawing);
	}
	
	/**
	 * If this constructor is called, initialize(String, int) must still be called.
	 */
	public CPlusPlusExternLayout(Graph<String, String> graph, Dimension size){
		super(graph, size);
	}
	
	/**
	 * Necessary infos are saved and coords are read and assigned.
	 * 
	 * @param pathToCPPDrawingsOfThisGraph
	 * @param numberOfTheDrawing
	 * @throws IOException
	 */
	public void initialize(String pathToCPPDrawingsOfThisGraph, int numberOfTheDrawing) throws IOException{
		
		this.pathToCPPDrawingsOfThisGraph = pathToCPPDrawingsOfThisGraph;
		this.numberOfTheDrawing = numberOfTheDrawing;
		
		try{ //try english and german ending
			CoordsFileReader.readAndSaveInLayout(pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".coords", this);
		}
		catch(IOException e){
			CoordsFileReader.readAndSaveInLayout(pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".koords", this);
		}
		
	}
	
	
	public long getCPUTimeInNs() throws IOException{
		try{ //try english and german ending
			return LongValueFileReader.readLongValueFile(pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".CpuTimeInNs");
		}
		catch(IOException e){
			return LongValueFileReader.readLongValueFile(pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".CpuZeitInNs");
		}
	}
	
	public int getNumberOfIterations() throws IOException{
		try{ //try english and german ending
			return (int) LongValueFileReader.readLongValueFile(
					pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".iterationsCount");
		}
		catch(IOException e){
			return (int) LongValueFileReader.readLongValueFile(
					pathToCPPDrawingsOfThisGraph+File.separator+numberOfTheDrawing+".iterationsAnzahl");
		}
	}
	
	
	@Override
	public void initialize() {
		return;
	}

	@Override
	public void reset() {
		return;
	}
	
}
