package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.Dialog;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.TripleSetGenerator;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.util.VertexTriple;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.LinkedList;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTree;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.SplitTreeNode;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.wspd.WellSeparatedPairDecomposition;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Calendar;
import java.util.Collection;
import java.util.Scanner;

/**
 * Static methods as outtake from the main()-class ({@link Testclass}).
 */
public class TestOfTheSplitTree {

	public static void createRandomPointSetAndConstructAndVisualizeSplitTree(Scanner sc){
		//ask parameteres
		int minNumberOfPoints = Dialog.getNextInt(sc, "minimum number of points");
		int maxNumberOfPoints = Dialog.getNextInt(sc, "maximum number of points");
		double minX = Dialog.getNextDouble(sc, "Smallest possible x-value (bounding value)");
		double maxX = Dialog.getNextDouble(sc, "Largest possible x-value (bounding value)");
		double minY = Dialog.getNextDouble(sc, "Smallest possible y-value (bounding value)");
		double maxY = Dialog.getNextDouble(sc, "Largest possible y-value (bounding value)");
		boolean onlyIntegerValues = Dialog.assistantConfirm(sc, "Only integer values for the coordinates?");
		//create
		Collection<VertexTriple<String>> tripleSet = TripleSetGenerator.
				generateRandomlyDistributedTripleSet(minNumberOfPoints, maxNumberOfPoints, minX, maxX, minY, maxY, onlyIntegerValues);
		SplitTree<String> st = new SplitTree<String>(tripleSet);
		//visualize
		visualizeSplitTree(st.createDelegateTreeToThisSplitTree());
		if(Dialog.assistantConfirm(sc, "Compute a WSPD from this SplitTree?")){
			generateWSPDFromSplitTree(st, sc);
		}
	}
	
	public static void createGridPointSetAndConstructAndVisualizeSplitTree(Scanner sc){
		//ask parameteres
		int numberOfPointsInXDirection = Dialog.getNextInt(sc, "Number of points in x-direction");
		int numberOfPointsInYDirection = Dialog.getNextInt(sc, "Number of points in y-direction");
		double minX = Dialog.getNextDouble(sc, "Lowest x-value for the grid");
		double minY = Dialog.getNextDouble(sc, "Lowest y-value for the grid");
		double distanceX = Dialog.getNextDouble(sc, "minimal distance of x-values (width of grid-columns)");
		double distanceY =  Dialog.getNextDouble(sc, "minimal distance of y-values (height of grid-rows)");
		//create
		Collection<VertexTriple<String>> tripleSet = TripleSetGenerator.generateTripleSetOfGridStructure(
				numberOfPointsInXDirection, numberOfPointsInYDirection, minX, minY, distanceX, distanceY, true);
		SplitTree<String> st = new SplitTree<String>(tripleSet);	
		//visualize
	    visualizeSplitTree(st.createDelegateTreeToThisSplitTree());
		if(Dialog.assistantConfirm(sc, "Compute a WSPD from this SplitTree?")){
			generateWSPDFromSplitTree(st, sc);
		}
	}
	
	
	/**
	 * @param splitTree
	 */
	private static void visualizeSplitTree(Forest<SplitTreeNode<String>, Integer> splitTree){
		Layout<SplitTreeNode<String>, Integer> layout = new TreeLayout<SplitTreeNode<String>, Integer>(splitTree);
		BasicVisualizationServer<SplitTreeNode<String>, Integer> vv = new BasicVisualizationServer<SplitTreeNode<String>,Integer>(layout);
		vv.setPreferredSize(new Dimension(1300, 650));
		vv.scaleToLayout(new LayoutScalingControl());
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		JFrame frame = new JFrame("This is the SplitTree");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv); 
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void generateWSPDFromSplitTree(SplitTree<String> st, Scanner sc){
		WellSeparatedPairDecomposition<String> wspd = new WellSeparatedPairDecomposition<String>(st, 
				Dialog.getNextDouble(sc, "Enter an s for the WSPD. s is a double-value."));
		//complete wspd as console output
		for(SplitTreeNode<String>[] stnArray: wspd.getWellSeparatedPairs()){
			for(SplitTreeNode<String> stn: stnArray){
				System.out.print("[");
				for(VertexTriple<String> t:
						st.getTripleSetInTheSubtreeBelow(stn, new LinkedList<VertexTriple<String>>())){
					System.out.print(t.get1()+": ("+t.get2().getX()+"/"+t.get2().getY()+")");
				}
				System.out.print("; "+st.getBoundingRectangle(stn)+"] ");
			}
			System.out.println();
		}
		System.out.println();
		
		
		System.out.println("WSPD has "+wspd.getWellSeparatedPairs().size()+" pairs.");
		int n = st.getSizeOfThePointSet();
		int nOver2 = n*(n-1)/2;
		System.out.println("With only two elements per pair (one in each pair-part) one would have (n[here: "+n+"] over 2)="+nOver2+" pairs.");
		System.out.println("This is "+(100-100.0*wspd.getWellSeparatedPairs().size()/nOver2)+"% pairs fewer.");
		System.out.println();
		System.out.print("In this WSPD the sum of all elements in all pair-parts is ");
		int sum = 0;
		for(SplitTreeNode<String>[] stnArray: wspd.getWellSeparatedPairs()){
			for(SplitTreeNode<String> stk: stnArray){
				sum += st.getSizeOfTheTripleSetInTheSubtreeBelow(stk);
			}
		}
		System.out.println(sum+".");
		System.out.println("With only two elements per pair one would have (n[here: "+n+"] over 2)*2="+(nOver2*2)+" elements.");
		System.out.println("This is "+(100-100.0*sum/nOver2/2)+"% elements fewer.");
	}
	
	public static void createSplitTreeFromRandomPointsetsAndMeasureTime(Scanner sc){
		//csv-file where all should be written in is cleared initally and a head is inserted
		try {
			Calendar currentTime = Calendar.getInstance();
			FileWriter fw = new FileWriter(Testclass.pathsConfig.pathToSaveCSVFilesFromSplitTreeTest+File.separator
					+"timeForSplitTreeCreation.csv", false);
			fw.append("Statistic_of_the_time_need_for_creation_(computation)_of_a_SplitTree" +System.lineSeparator()
					+"From_"+currentTime.get(Calendar.YEAR)+"/"+(currentTime.get(Calendar.MONTH)+1)
					+"/"+currentTime.get(Calendar.DAY_OF_MONTH)+"_"+currentTime.get(Calendar.HOUR_OF_DAY)
					+":"+currentTime.get(Calendar.MINUTE)+":"+currentTime.get(Calendar.SECOND)+System.lineSeparator()
					+"Number_of_points;time_needed_in_ns");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		boolean randomDistribution = Dialog.assistentBoolean(sc, "Please select:",
				"Randomly distributed point set", "distribution in grid-structure");
		boolean linearGrowth = Dialog.assistentBoolean(sc, "Please select:",
				"linear growth of size of the point set",
				"exponential growth of size of the point set");
		if(linearGrowth){
			/*
			 * linear
			 */
			for(int i=0; i<3000000; i+=1000){
				int n=i;
				if(!randomDistribution){
					n=(int)Math.sqrt(i)*(int)Math.sqrt(i);
				}
				System.out.println("Point set size "+n+":");
				long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				if(randomDistribution){
					new SplitTree<String>(TripleSetGenerator.generateRandomlyDistributedTripleSet(
							n, n, 0, 1, 0, 1, false));
				}
				else{
					new SplitTree<String>(TripleSetGenerator.generateTripleSetOfGridStructure(
							(int)Math.sqrt(n), (int)Math.sqrt(n), 0, 0, 1, 1, false));
				}
				long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				long lasted = endTime-startTime;
				System.out.println("Construction needed "+lasted+"ns.");
				//append value to csv
				try {
					FileWriter fw = new FileWriter(Testclass.pathsConfig.pathToSaveCSVFilesFromSplitTreeTest+File.separator
							+"timeForSplitTreeCreation.csv", false);
					fw.append(System.lineSeparator()+n+";"+lasted);
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else{
			/*
			 * exponential
			 */
			double basis = 1.2;
			int previousN = 0; //that there is no value calced twice (relevant for small numbers)
			for(int i=0; i<200; i++){
				int n = (int)Math.pow(basis, i);
				if(!randomDistribution){
					n=(int)Math.sqrt(n)*(int)Math.sqrt(n);
				}
				if(previousN!=n){ //skip if already done
					System.out.println("Point set size "+basis+"^"+i+"="+n+":");
					long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
					if(randomDistribution){
						new SplitTree<String>(TripleSetGenerator.generateRandomlyDistributedTripleSet(
								n, n, 0, 1, 0, 1, false));
					}
					else{
						new SplitTree<String>(TripleSetGenerator.generateTripleSetOfGridStructure(
								(int)Math.sqrt(n), (int)Math.sqrt(n), 0, 0, 1, 1, false));
					}
					long endTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
					long lasted = endTime-startTime;
					System.out.println("Construction needed "+lasted+"ns.");
					//append values to csv-file
					try {
						FileWriter fw = new FileWriter(Testclass.pathsConfig.pathToSaveCSVFilesFromSplitTreeTest+File.separator
								+"timeForSplitTreeCreation.csv", false);
						fw.append(System.lineSeparator()+n+";"+lasted);
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					previousN = n;
				}
			}
		}
	}
}
