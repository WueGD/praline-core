package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.AlgorithmType;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMaps;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.cPlusPlus.CPlusPlusExternLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager.*;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityCriterion;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.QualityTesterForLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.ScaledLayout;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.DefaultVisualizationModelWithoutReiterating;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.IpeFileWriter;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Static methods as outtake from the main()-class ({@link Testclass}).
 */
public class TestOfGraphDrawing {
	
	/*
	 * The Java-warnings in this class come from the shift of the used data from
	 * variable vertex-data to {@link VertexTriple}s (that contain also the variable vertex-data).
	 * It is used in {@link FRLayoutNoMaps} and all classes having this as a super-class
	 * (all newly created layout-algorithms).
	 * To handle both together some generics were left out and at some places
	 * it has to be casted to a version with generics.
	 */
	
	
	/*
	 * object-managers
	 */
	protected static GraphManager graphManager = new GraphManager();
	protected static AlgorithmManager algorithmManager = new AlgorithmManager();
	protected static RecomputationOfSplitTreeAndWSPDFunctionManager recomputationFunctionManager =
			new RecomputationOfSplitTreeAndWSPDFunctionManager();
	
	
	
	public static void drawAGraph(Scanner sc){
		//get a graph
		Graph<String, String> g = graphManager.assistantForSelectingOneGraph(sc);
		if(g==null){
			return;
		}
		
		//select layout-algorithms
		LinkedList<AlgorithmReference> selectedAlgorithms = algorithmManager.assistantForSelectingMultipleObjects(sc);
		LinkedList<Double> sForTheWSPD = new LinkedList<Double>();
		LinkedList<RecomputationOfSplitTreeAndWSPDFunction> recomputationFunctions = null;
		if(AlgorithmReference.conatinsAlgorithmOfAlgorithmType(selectedAlgorithms, AlgorithmType.WITH_WSPD)){
			//get s
			do{
				sForTheWSPD.add(Dialog.getNextDouble(sc, "Enter an s for the WSPD."));
			} while(Dialog.assistantConfirm(sc, "Add another s-value?"));
			//get recomp-function
			System.out.println("Now choose the functions that decide in a layout-algorithm that works with a WSPD "
					+ "in each iteration whether the split tree and the WSPD should be computed new or not.");
			recomputationFunctions = recomputationFunctionManager.assistantForSelectingMultipleObjects(sc);
		}
		LinkedList<Double> thetaForLayoutsWithQuadtree = new LinkedList<Double>();
		if(AlgorithmReference.conatinsAlgorithmOfAlgorithmType(selectedAlgorithms, AlgorithmType.WITH_QUADTREE)){
			//get theta
			do{
				thetaForLayoutsWithQuadtree.add(Dialog.getNextDouble(sc, "Enter a theta for the layout-algorithm with quadtree."));
			} while(Dialog.assistantConfirm(sc, "Add another theta-value?"));
		}
		/*
		 * Define the layouts and set the same initial points for them.
		 */
		//define layouts
		final LinkedList<AbstractLayout> allLayouts = new LinkedList<AbstractLayout>();
		final LinkedList<String> allLayoutNames = new LinkedList<String>();
		Dimension size = new Dimension(600, 600); //standard size used for every layout here
		for(AlgorithmReference ar: selectedAlgorithms){
			if(ar.getAlgorithmType()==AlgorithmType.WITH_WSPD){
				for(RecomputationOfSplitTreeAndWSPDFunction f: recomputationFunctions){
					for(double s: sForTheWSPD){
						LayoutWithWSPD<String> layoutWithWSPD = (LayoutWithWSPD<String>) ar.getNewInstance(g, size, s);
						layoutWithWSPD.setRecomputationOfSplitTreeAndWSPDFunction(f);
						allLayouts.add((AbstractLayout<String, String>) layoutWithWSPD);
						allLayoutNames.add(ar.getClassOfTheLayout().getSimpleName()+" ("+f.getName()+", s="+s+")");
					}
				}
			}
			else if(ar.getAlgorithmType()==AlgorithmType.WITH_QUADTREE){
				for(double theta: thetaForLayoutsWithQuadtree){
					allLayouts.add(ar.getNewInstance(g, size, theta));
					allLayoutNames.add(ar.getClassOfTheLayout().getSimpleName()+" (theta="+theta+")");
				}
			}
			else{
				allLayouts.add(ar.getNewInstance(g, size, 0));
				allLayoutNames.add(ar.getClassOfTheLayout().getSimpleName());
			}
		}
		
		//prepare initial random point locations
		//this is done in the constructor of FRLayoutNoMaps or classes basing on this
		//if the first is not of this type, get the random locations via apply from the first layout
		List<VertexTriple<String>> tripleSet = null;
		AbstractLayout layoutingAlgo = allLayouts.get(0);
		if(layoutingAlgo instanceof FRLayoutNoMaps){
			tripleSet = ((FRLayoutNoMaps<String, String>)layoutingAlgo).getVertices();
		}
		else{
			for(String s: g.getVertices()){
				((AbstractLayout<String,String>)layoutingAlgo).apply(s);
			}
		}
		//now set these locations as vertex locations for the other layouts
		Iterator<VertexTriple<String>> iterator = null;
		for(int i=1; i<allLayouts.size(); i++){
			if(tripleSet!=null){
				iterator = tripleSet.iterator();
			}
			if(FRLayoutNoMaps.class.isAssignableFrom(allLayouts.get(i).getClass())){
				for(VertexTriple<String> t: ((FRLayoutNoMaps<String, String>)allLayouts.get(i)).getVertices()){
					Point2D pointToBePositioned = t.get2();
					if(!FRLayoutNoMaps.class.isAssignableFrom(layoutingAlgo.getClass())){
						pointToBePositioned.setLocation(layoutingAlgo.apply(t.get1()).getX(),
								layoutingAlgo.apply(t.get1()).getY());
					}
					else{
						pointToBePositioned.setLocation(iterator.next().get2());
					}
				}
			}
			else{
				for(String v: g.getVertices()){
					Point2D pointToBePositioned = allLayouts.get(i).apply(v);
					if(!FRLayoutNoMaps.class.isAssignableFrom(layoutingAlgo.getClass())){
						pointToBePositioned.setLocation(layoutingAlgo.apply(v).getX(), layoutingAlgo.apply(v).getY());
					}
					else{
						pointToBePositioned.setLocation(iterator.next().get2());
					}
				}
			}
		}
		//draw (execute layout algorithms)
		LinkedList<Integer> numberOfIterations = new LinkedList<Integer>();
		LinkedList<Double> cpuTime = new LinkedList<Double>();
		for(int i=0; i<allLayouts.size(); i++){
			AbstractLayout<String, String> layout = allLayouts.get(i);
			System.out.println(allLayoutNames.get(i));
			int iterationCounter = 0;
			long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			layout.initialize();
			while(!((IterativeContext) layout).done()){
				iterationCounter++;
				System.out.println("Step "+iterationCounter);
				((IterativeContext) layout).step();
			}
			long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			cpuTime.add((double)(endTime - startTime)/1000000); //conversion from ns to ms
			numberOfIterations.add(iterationCounter);
		}
		
		//For FRLayoutNoMaps (and layouts basing on it) convert to classical V-E-layout for visualization
		for(int i=0; i<allLayouts.size(); i++){
			if(FRLayoutNoMaps.class.isAssignableFrom(allLayouts.get(i).getClass())){
				//insert complement-layout
				allLayouts.add(i+1, ((FRLayoutNoMaps)allLayouts.get(i)).getCorresponding_V_E_Layout());
				//remove old one
				allLayouts.remove(i);
			}
		}
		
		while(Dialog.assistantConfirm(sc, "Add drawings from C++-project?")){
			String path = Dialog.getNextLine(sc, "Enter path to directory where the results from it for one graph are saved "
					+ "(it is a directory that contains a 0.coords file)\n relative to "+Testclass.pathsConfig.pathToCPlusPlusResults
							+" (without '/' at the beginning or the end).");
			int run = Dialog.getNextInt(sc, "The files in this directory must start with the number of the test-run "
					+ "(e.g. 0.coords to the first run or 3.cpuTimeInNs). Select one number being there.");
			String naming = Dialog.getNextLine(sc, "Name this drawing (name only used this time, name is not saved)");
			//create new cpp-layout
			CPlusPlusExternLayout newLayout;
			//set coordinates
			try {
				newLayout = CPlusPlusExternLayout.getCorrectlyAssignedInstance(
						g, size, Testclass.pathsConfig.pathToCPlusPlusResults+File.separator+path, run);
			} catch (IOException | NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e1) {
				System.err.println("Failed to read coordinates. Path correct? Object was not added.");
				continue;
			}
			try {
				//conversion from ns to ms here, too
				cpuTime.add((double)newLayout.getCPUTimeInNs()/1000000.0);
			} catch (IOException e1) {
				System.err.println("Failed to read CPU-time. Path correct? Object was not added.");
				continue;
			}
			allLayoutNames.add(naming);
			allLayouts.add(newLayout);
			numberOfIterations.add(-1);
		}
		
		boolean evaluateQuality = Dialog.assistantConfirm(sc, "Measure quality of resulting drawings?");
		boolean showLayouts = Dialog.assistantConfirm(sc, "Show created layouts?");
		
		//print results
		System.out.println("**********************");
		System.out.println("* Comparison of time:");
		System.out.println("**********************");
		System.out.print("Name of the algorithm: | time in ms | number of iterations | comparison of time to [");
		for(String layoutName: allLayoutNames){
			System.out.print(" "+layoutName+" ");
		}
		System.out.println("]");
		for(int i=0; i<allLayouts.size(); i++){
			System.out.print(allLayoutNames.get(i)+":   "+cpuTime.get(i)+"ms   "+numberOfIterations.get(i)+"   [   ");
			for(int j=0; j<allLayouts.size(); j++){
				if(i==j){
					System.out.print("-   ");
				}
				else{
					double timeRelative = cpuTime.get(i)/cpuTime.get(j);
					if(timeRelative>1){
						System.out.print((100.0*timeRelative-100)+"% slower   ");
					}
					else{
						System.out.print((100-100.0*timeRelative)+"% faster   ");
					}
				}
			}
			System.out.println("]");
		}
		
		if(showLayouts){
			//Visualize created drawings
			//first layout
			final VisualizationViewer<String, String> vv1 = new VisualizationViewer<String, String>(
					new DefaultVisualizationModelWithoutReiterating<String, String>(allLayouts.get(0), size));
			vv1.getRenderContext().setEdgeShapeTransformer(new EdgeShape<>(allLayouts.get(0).getGraph()). new Line());
			vv1.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<String>() {
				@Override
				public Shape apply(String input) {
					return factory.getEllipse("");
				}
			});
			vv1.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
			vv1.getRenderer().getVertexLabelRenderer().setPosition(Position.N);
			GraphZoomScrollPane scrollPaneOld = new GraphZoomScrollPane(vv1);
	        vv1.setGraphMouse(new DefaultModalGraphMouse<String,Number>());
			//second layout
	        AbstractLayout<String, String> nextLayout = allLayouts.get(0); //catch case that only 1 was drawn
	        if(allLayouts.size()>1){ //case that more than 1 was drawn
	        	nextLayout = allLayouts.get(1);
	        }
			final VisualizationViewer<String, String> vv2 = new VisualizationViewer<String, String>(
					new DefaultVisualizationModelWithoutReiterating<String, String>(nextLayout, size));
			vv2.getRenderContext().setEdgeShapeTransformer(new EdgeShape<>(nextLayout.getGraph()). new Line());
			vv2.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<String>() {
				@Override
				public Shape apply(String input) {
					return factory.getEllipse("");
				}
			});
			vv2.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
			vv2.getRenderer().getVertexLabelRenderer().setPosition(Position.N);
			GraphZoomScrollPane scrollPaneNeu = new GraphZoomScrollPane(vv2);
	        vv2.setGraphMouse(new DefaultModalGraphMouse<String,Number>());
			JFrame frame = new JFrame("These are the drawings");
			String[] options = new String[allLayouts.size()];
			for(int i=0; i<allLayoutNames.size(); i++) options[i] = allLayoutNames.get(i);
			final JComboBox<String> selectDrawing1 = new JComboBox<String>(options);
			selectDrawing1.setSelectedIndex(0);
			selectDrawing1.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					vv1.setGraphLayout(allLayouts.get(selectDrawing1.getSelectedIndex()));
				}
			});
			JButton reset1 = new JButton("Reset position and zoom");
	        reset1.addActionListener(new ActionListener() {
	        	public void actionPerformed(ActionEvent e) {
					vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
					vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
				}
	        });
			JPanel pane1 = new JPanel();
			pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
			pane1.add(selectDrawing1);
			pane1.add(reset1);
			pane1.add(scrollPaneOld);
			final JComboBox<String> selectDrawing2 = new JComboBox<String>(options);
			selectDrawing2.setSelectedIndex(0);
	        if(allLayouts.size()>1){ //again catch if it's only 1 alg
	        	selectDrawing2.setSelectedIndex(1);
	        }
			selectDrawing2.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					vv2.setGraphLayout(allLayouts.get(selectDrawing2.getSelectedIndex()));
				}
			});
			JButton reset2 = new JButton("Reset position and zoom");
	        reset2.addActionListener(new ActionListener() {
	        	public void actionPerformed(ActionEvent e) {
					vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
					vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
				}
	        });
			JPanel pane2 = new JPanel();
			pane2.setLayout(new BoxLayout(pane2, BoxLayout.Y_AXIS));
			pane2.add(selectDrawing2);
			pane2.add(reset2);
			pane2.add(scrollPaneNeu);
			frame.setLayout(new FlowLayout());
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			scrollPaneOld.setBorder(new TitledBorder(null, "Layout 1", TitledBorder.CENTER, TitledBorder.TOP));
			scrollPaneNeu.setBorder(new TitledBorder(null, "Layout 2", TitledBorder.CENTER, TitledBorder.TOP));
			frame.getContentPane().add(pane1);
			frame.getContentPane().add(pane2);
			frame.pack();
			frame.setVisible(true);
		}
		
		if(evaluateQuality){
			//evaluate drawings acc. to all available quality criterions
			QualityTesterForLayout<String, String>[] qt = new QualityTesterForLayout[allLayouts.size()];
			for(int i=0; i<qt.length; i++){
				qt[i] = new QualityTesterForLayout<String, String>(new ScaledLayout<String, String>(allLayouts.get(i)));
			}
			System.out.println("****************************");
			System.out.println("*  Comparison of quality:");
			System.out.println("* With algorithms in this order");
			System.out.print("*");
			for(String name: allLayoutNames){
				System.out.print(" >"+name+"<");
			}		
			System.out.println("");
			System.out.println("* Note: Every layout is scaled to a square 600x600 to make the results better comparable.");
			System.out.println("****************************");
			for(QualityCriterion bk: QualityCriterion.values()){
				System.out.print("\n>"+bk+"<");
				for(Statistic st: Statistic.values()){
					System.out.print("\n"+st+": ");
					for(int i=0; i<qt.length; i++){
						System.out.print(qt[i].get(bk, st)+", ");
					}
				}
			}
			System.out.print("\n>SMALLEST_ANGLE: ");
			for(int i=0; i<qt.length; i++){
				System.out.print(qt[i].getSmallestAngle()+", ");
			}
			System.out.print("\nNUMBER_OF_EDGE_CROSSINGS: ");
			for(int i=0; i<qt.length; i++){
				System.out.print(qt[i].getNumberOfCrossings()+", ");
			}
		}
		
		//optional IPE export
		Manager<AbstractLayout<String, String>> drawingsManager = new Manager<AbstractLayout<String,String>>() {
			@Override
			protected void initialize() {
				Iterator<AbstractLayout> iteratorLayout = allLayouts.iterator();
				Iterator<String> iteratorLayoutName = allLayoutNames.iterator();
				while(iteratorLayout.hasNext() && iteratorLayoutName.hasNext()){
					addToObjectList((AbstractLayout<String, String>) iteratorLayout.next(), iteratorLayoutName.next());
				}
			}
		};
		System.out.println();
		System.out.println();
		while(Dialog.assistantConfirm(sc, "Export a drawing to Ipe?")){
			AbstractLayout<String, String> selectedLayout = drawingsManager.assistantForSelectingOneObject(sc);
			String fileName = Dialog.getNextLine(sc, "Enter a name for the file. (.ipe will be added automatically)");
			if(fileName.length()==0){
				System.out.println("No name found. Did not export drawing.");
				continue;
			}
			IpeFileWriter.writeFile(Testclass.pathsConfig.pathForIpeExport, fileName, selectedLayout);
			System.out.println("Ipe-export completed.");
			System.out.println();
		}
	}
	
}
