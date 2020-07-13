package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRGrid;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.FRLayoutNoMapsNoFrame;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.FRWSPDb_b;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.LayoutWithWSPD;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms.wspd.RecomputationOfSplitTreeAndWSPDFunction;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.DataAsJSonFile;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.io.SuperGraphReader;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.AffineTransformModifiedStartingPositions;
import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util.DefaultVisualizationModelForDemonstration;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class DemonstrationOfGraphDrawing {

	public static final String DEMONSTRATION_CONFIG_PATH = "Jung-Layouting/target/config.settings.demonstration.json";

	//public:

	public static DemonstrationConfig demonstrationConfig;

	/**
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static JLabel[] allCpuTimeJLabels;
	/**
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static JLabel[] allEdgeLengthStandardDeviationJLabels;
	/**
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static JLabel[] allNumberOfEdgeCrossingsJLabelsJLabels;
	/**
	 * Index, which graph is currently shown.
	 * If this is -1 or {@link DemonstrationOfGraphDrawing#objectsInDirectory}.length,
	 * then no graph is selected and the ende (or beginning) is reached.
	 *
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static int graphIndex = -1;

	/**
	 * All files in the referenced path. Currently selected is the one with
	 * {@link DemonstrationOfGraphDrawing#graphIndex}.
	 *
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static File[] objectsInDirectory;
	/**
	 * public because this addressed by {@link DefaultVisualizationModelForDemonstration}
	 */
	public static int counterHowOftenNextOrPrevOrNewWasClicked = 0;


	//{Var. to the diagrams:
	public static DefaultCategoryDataset dataset[];
	public static String[] categoriesStringForDiagram = {"Used CPU time in s", "Number of crossings", "St. dev. edge length"};
	public static String[] algorithmsStringForDiagram = {"FRLayout", "FR+WSPD", "FR+Grid"};



	//private:

	//Chart
	private static JFreeChart[] chart;
	//:Var. to the diagrams}

	/**
	 * Layout to empty graph, that is shown at start and end
	 */
	private static DemonstrationLayout<String, String> emptyLayout;

	private static double screenWidth;

	private static Dimension drawingArea;
	private static Dimension shownArea;

	private static DefaultVisualizationModelForDemonstration<String,String>[] allDVM;

	private static VisualizationViewer<String, String>[] allVV;

	private static DemonstrationLayout<String, String>[][] allLayoutsComputedInAdvance;

	private static JLabel graphTitleJLabel;
//	private static JLabel graphSubtitleJLabel;

	private static final String graphtitleTextBegin = "<html><font size=6>Press \"next\" to start</font></html>";
	private static final String graphtitleTextEnd = "<html><font size=6>Reached end</font></html>";

	private static JButton next;

	private static int numberOfRunningAnimations = 0;
	/**
	 * This is the timer internally taken for going to next
	 */
	private static Timer timerForAutomaticallyNext;
	/**
	 * This timer is only used for showing the time to the user
	 */
	private static Timer timerForJLabelAutomaticallyNext;
	private static JLabel jLabelForAutomaticallyNext;
	private static final String stringToAutomaticallyNextPart1 = "Next Graph in ";
	private static int remainingSecondsToAutomaticallyNext;
	private static final String stringToAutomaticallyNextPart2 = " seconds";
	private static JButton cancelAutomaticallyNext;

	private static ChartPanel[] diagrams;
	private static JPanel[] diagramPanels;


	public static void startDemonstration(){
		//load demonstrationConfig
		demonstrationConfig = DataAsJSonFile.getFileContent(DEMONSTRATION_CONFIG_PATH, DemonstrationConfig.class);
		if(demonstrationConfig==null){ //if there is no such file
			//create it (with default values)
			demonstrationConfig = new DemonstrationConfig();
			DataAsJSonFile.writeFile(demonstrationConfig, DEMONSTRATION_CONFIG_PATH);
		}
		File dirToGraphs = new File(demonstrationConfig.pathToDemonstrationGraphs);
		if(!dirToGraphs.exists()){
			System.out.println("The path to the graphs specified in "+ DEMONSTRATION_CONFIG_PATH +" does not exist.");
			System.out.println("Demonstration can not be started");
			return;
		}






		String[] algorithmsNames = {"<html><font size=6>FRLayout", "<html><font size=6>FR+WSPD", "<html><font size=6>FR+Grid"};


		//prepare GUI
		allDVM = new DefaultVisualizationModelForDemonstration[3];
		allVV = new VisualizationViewer[3];
		AffineTransformModifiedStartingPositions[] allAffineTransforms = new AffineTransformModifiedStartingPositions[3];
//		GraphZoomScrollPane[] allGZSP = new GraphZoomScrollPane[3];
		allCpuTimeJLabels = new JLabel[3];
		allEdgeLengthStandardDeviationJLabels = new JLabel[3];
		allNumberOfEdgeCrossingsJLabelsJLabels = new JLabel[3];
		JButton[] allResetButtons = new JButton[3];
		JPanel[] allJPanels = new JPanel[3];
		final JFrame frame = new JFrame("Demonstration");
		GridBagLayout frameGridBag = new GridBagLayout();
		GridBagConstraints frameConstraints = new GridBagConstraints();
		int frameGridYPos = 0;
//		frameConstraints.fill = GridBagConstraints.CENTER;
		frame.setLayout(frameGridBag);
		frameConstraints.gridx = 0;
		frameConstraints.gridy = frameGridYPos;
		frameConstraints.gridwidth = 3;
		frameConstraints.anchor = GridBagConstraints.CENTER;
		frameConstraints.insets = new Insets(0, 0, 10, 0);
		graphTitleJLabel = new JLabel(graphtitleTextBegin);
		frame.getContentPane().add(graphTitleJLabel, frameConstraints);

		//graphsubtitle was taken out to have more space for other things (-> now all is in graphtitle)
//		frameConstraints.insets = new Insets(0, 0, 10, 0);
//		frameConstraints.gridx = 0;
//		frameConstraints.gridy = ++frameGridYPos;
//		frameConstraints.gridwidth = 3;
//		frameConstraints.anchor = GridBagConstraints.CENTER;
//		graphSubtitleJLabel = new JLabel(" ");
//		frame.getContentPane().add(graphSubtitleJLabel, frameConstraints);

		//reset frame-Constraints
		frameConstraints.gridwidth = 1;
		frameConstraints.insets = new Insets(0, 0, 0, 0);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		//define sizes
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		screenWidth = gd.getDisplayMode().getWidth();
		//screenwidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		rescaleDrawingArea();
		SparseGraph<String,String> emptyGraph = new SparseGraph<String, String>();
		FRLayoutNoMapsNoFrame<String, String> emptyReferencelayout = new FRLayoutNoMapsNoFrame<String, String>(
				emptyGraph, drawingArea);
		emptyReferencelayout.setMaxIterations(0);
		emptyLayout = new DemonstrationLayout<String, String>(emptyReferencelayout, emptyGraph, -1);

		//values to the diagram
		dataset = new DefaultCategoryDataset[3];
		chart = new JFreeChart[3];
		for(int i=0; i<categoriesStringForDiagram.length; i++){
			dataset[i] = new DefaultCategoryDataset();
			for(int j=0; j<algorithmsStringForDiagram.length; j++){
				dataset[i].addValue(0, algorithmsStringForDiagram[j], categoriesStringForDiagram[i]);
			}
		}

		//build GUI
		frameGridYPos++; //is not done in the loop because there it would be done 3 times
		for(int i=0; i<3; i++){
			allAffineTransforms[i] = new AffineTransformModifiedStartingPositions<String>(shownArea, emptyLayout);
			allDVM[i] = new DefaultVisualizationModelForDemonstration<String,String>(emptyLayout, shownArea, i,
					allAffineTransforms[i]);
			allVV[i] = new VisualizationViewer<String,String>(allDVM[i], shownArea);
			final VisualizationViewer<String, String> vv = allVV[i];
			vv.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(emptyGraph));
			vv.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<String>() {
				@Override
				public Shape apply(String input) {
					return factory.getEllipse(null);
				}
			});
			vv.getRenderContext().setVertexLabelTransformer(new Function<String,String>(){
				@Override
				public String apply(String input) {
					return ""; //Do not show a label. That might also be done nicer, but I have not seen yet how to.
				}
			});
			vv.getRenderContext().setVertexShapeTransformer(Functions.<Shape>constant(new Ellipse2D.Float(-2,-2,4,4)));

//			allGZSP[i] = new GraphZoomScrollPane(vv);
			final int iFinal = i;
			DefaultModalGraphMouse<String, Number> dmgm = new DefaultModalGraphMouse<String,Number>();
			vv.setGraphMouse(dmgm);
			vv.getRenderContext().getMultiLayerTransformer().setTransformer(Layer.LAYOUT,
					new MutableAffineTransformer(allAffineTransforms[i]));
			//When interacting cancel automatically next
			vv.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) { }
				@Override
				public void mousePressed(MouseEvent e) {
					cancelAutomaticallyNext();
				}
				@Override
				public void mouseExited(MouseEvent e) {	}
				@Override
				public void mouseEntered(MouseEvent e) { }
				@Override
				public void mouseClicked(MouseEvent e) {
					cancelAutomaticallyNext();
				}
			});
			vv.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					cancelAutomaticallyNext();
				}
			});

			allResetButtons[i] = new JButton("Reset position and zoom");
			allResetButtons[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
					vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
				}
			});

			allJPanels[i] = new JPanel();
			GridBagLayout gridbag = new GridBagLayout();
			int gridYPos = 0;
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.CENTER;
			gridbag.setConstraints(allJPanels[i], constraints);
			allJPanels[i].setLayout(gridbag);
			JLabel label = new JLabel(algorithmsNames[i]);
			label.setHorizontalAlignment(JLabel.CENTER);
			constraints.gridx = 0;
			constraints.gridy = gridYPos;
			allJPanels[i].add(label, constraints);
//	        constraints.gridx = 0;
//	        constraints.gridy = ++gridYPos;
			allCpuTimeJLabels[i] = new JLabel(" ");
			allCpuTimeJLabels[i].addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) { }
				@Override
				public void mousePressed(MouseEvent e) { }
				@Override
				public void mouseExited(MouseEvent e) { }
				@Override
				public void mouseEntered(MouseEvent e) { }
				@Override
				public void mouseClicked(MouseEvent e) {
					showDiagramm(0);
				}
			});
//	        allJPanels[i].add(allCpuTimeJLabels[i], constraints);
//	        constraints.gridx = 0;
//	        constraints.gridy = ++gridYPos;
			allEdgeLengthStandardDeviationJLabels[i] = new JLabel(" ");
			allEdgeLengthStandardDeviationJLabels[i].addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) { }
				@Override
				public void mousePressed(MouseEvent e) { }
				@Override
				public void mouseExited(MouseEvent e) { }
				@Override
				public void mouseEntered(MouseEvent e) { }
				@Override
				public void mouseClicked(MouseEvent e) {
					showDiagramm(2);
				}
			});
//	        allJPanels[i].add(allEdgeLengthStandardDeviationJLabels[i], constraints);
//	        constraints.gridx = 0;
//	        constraints.gridy = ++gridYPos;
			allNumberOfEdgeCrossingsJLabelsJLabels[i] = new JLabel(" ");
			allNumberOfEdgeCrossingsJLabelsJLabels[i].addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) { }
				@Override
				public void mousePressed(MouseEvent e) { }
				@Override
				public void mouseExited(MouseEvent e) { }
				@Override
				public void mouseEntered(MouseEvent e) { }
				@Override
				public void mouseClicked(MouseEvent e) {
					showDiagramm(1);
				}
			});
//	        allJPanels[i].add(allNumberOfEdgeCrossingsJLabelsJLabels[i], constraints);
			constraints.gridx = 0;
			constraints.gridy = ++gridYPos;
			allJPanels[i].add(allVV[i], constraints);
			constraints.gridx = 0;
			constraints.gridy = ++gridYPos;
			allJPanels[i].add(allResetButtons[i], constraints);
			frameConstraints.gridx = i;
			frameConstraints.gridy = frameGridYPos;
			frame.getContentPane().add(allJPanels[i], frameConstraints);
		}


		//insert buttons
		JButton previous = new JButton("Previous");
		previous.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				counterHowOftenNextOrPrevOrNewWasClicked++;
				if(graphIndex==-1){ //already reached min -> no further reducing
					return;
				}
				if(graphIndex==0){ //if at first layout then "back" to end (closed ring)
					graphIndex = objectsInDirectory.length-1;
				}
				else{
					graphIndex--;
				}
				updateTitles();
				resetDiagramValues();
				showGraph();
			}
		});
		frameConstraints.gridx = 0;
		frameConstraints.gridy = ++frameGridYPos;
		frameConstraints.insets = new Insets(10, 0, 0, 0);
		frameConstraints.anchor = GridBagConstraints.EAST;
		frame.getContentPane().add(previous, frameConstraints);
		JButton again = new JButton("Draw again");
		again.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				counterHowOftenNextOrPrevOrNewWasClicked++;
				updateTitles();
				resetDiagramValues();
				showGraph();
			}
		});
		frameConstraints.gridx = 1;
		frameConstraints.gridy = frameGridYPos;
		frameConstraints.anchor = GridBagConstraints.CENTER;
		frame.getContentPane().add(again, frameConstraints);

		//next - a bit longer because the automatically-next-function is also added
		next = new JButton("Next");
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				counterHowOftenNextOrPrevOrNewWasClicked++;
				if(graphIndex==objectsInDirectory.length-1){ //max reached -> go back to start
					graphIndex = 0;
				}
				else{
					graphIndex++;
				}
				updateTitles();
				resetDiagramValues();
				showGraph();
			}
		});
		JPanel nextBar = new JPanel();
		GridBagLayout gridbagNextleiste = new GridBagLayout();
		GridBagConstraints constraintsNextleiste = new GridBagConstraints();
		constraintsNextleiste.fill = GridBagConstraints.CENTER;
		gridbagNextleiste.setConstraints(nextBar, constraintsNextleiste);
		nextBar.setLayout(gridbagNextleiste);
		constraintsNextleiste.gridx=0;
		constraintsNextleiste.gridy=0;
		nextBar.add(next, constraintsNextleiste);
		jLabelForAutomaticallyNext = new JLabel("");
		constraintsNextleiste.insets = new Insets(0, 10, 0, 0);
		constraintsNextleiste.gridx=1;
		constraintsNextleiste.gridwidth=2;
		nextBar.add(jLabelForAutomaticallyNext, constraintsNextleiste);
		cancelAutomaticallyNext = new JButton("Cancel");
		cancelAutomaticallyNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelAutomaticallyNext();
			}
		});
		constraintsNextleiste.gridx=3;
		constraintsNextleiste.gridwidth=1;
		nextBar.add(cancelAutomaticallyNext, constraintsNextleiste);
		cancelAutomaticallyNext.setVisible(false);


		frameConstraints.gridx = 2;
		frameConstraints.gridy = frameGridYPos;
		frameConstraints.anchor = GridBagConstraints.WEST;
		frame.getContentPane().add(nextBar, frameConstraints);


		frameConstraints.gridx = 0;
		frameConstraints.gridy = frameGridYPos;
		frameConstraints.gridwidth = 0;
		frameConstraints.anchor = GridBagConstraints.WEST;
		JButton settingsButton = new JButton("Settings");
		frame.getContentPane().add(settingsButton, frameConstraints);
		settingsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//in any case do go on automatically
				cancelAutomaticallyNext();

				//create dialog
				JDialog optionsDialog = new JDialog(frame, true);

				GridBagLayout optionsDialogLayout = new GridBagLayout();
				GridBagConstraints optionsDialogConstraints = new GridBagConstraints();
				optionsDialogConstraints.fill = GridBagConstraints.CENTER;
				optionsDialogLayout.setConstraints(optionsDialog, optionsDialogConstraints);
				optionsDialog.setLayout(optionsDialogLayout);
				final JCheckBox automaticallyNextCheckBox = new JCheckBox("Go automatically to next graph after specified time");
				automaticallyNextCheckBox.setSelected(demonstrationConfig.automaticallyNextAfterSpecifiedTime);
				automaticallyNextCheckBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if(automaticallyNextCheckBox.isSelected()){
							demonstrationConfig.automaticallyNextAfterSpecifiedTime = true;
							if(numberOfRunningAnimations==0){
								startTimerForAutomaticallyNextGraph();
							}
						}
						else{
							cancelAutomaticallyNext();
							demonstrationConfig.automaticallyNextAfterSpecifiedTime = false;
						}
					}
				});
				optionsDialogConstraints.anchor = GridBagConstraints.WEST;
				optionsDialogConstraints.gridx = 0;
				optionsDialogConstraints.gridy = 0;
				optionsDialog.add(automaticallyNextCheckBox, optionsDialogConstraints);

				final JCheckBox showDiagramTitleCheckBox =
						new JCheckBox("Show chart title (will be done after the next event, e.g. new graph)");
				showDiagramTitleCheckBox.setSelected(demonstrationConfig.showDiagramTitle);
				showDiagramTitleCheckBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if(showDiagramTitleCheckBox.isSelected()){
							demonstrationConfig.showDiagramTitle = true;
							createDiagrams();
						}
						else{
							demonstrationConfig.showDiagramTitle = false;
							createDiagrams();
						}
					}
				});
				optionsDialogConstraints.anchor = GridBagConstraints.WEST;
				optionsDialogConstraints.gridx = 0;
				optionsDialogConstraints.gridy = 1;
				optionsDialog.add(showDiagramTitleCheckBox, optionsDialogConstraints);

				final JTextField timeForAutomaticallyNextField = new JTextField(demonstrationConfig.secondsUntilNextGraphIsSelectedAutomatically+"");
				timeForAutomaticallyNextField.addKeyListener(new KeyListener() {
					@Override
					public void keyTyped(KeyEvent e) { }
					@Override
					public void keyReleased(KeyEvent e) {
						try{
							demonstrationConfig.secondsUntilNextGraphIsSelectedAutomatically =
									Integer.parseInt(timeForAutomaticallyNextField.getText());
						}
						catch(NumberFormatException nfe){
							//do nothing
						}
					}
					@Override
					public void keyPressed(KeyEvent e) { }
				});
				optionsDialogConstraints.gridx = 0;
				optionsDialogConstraints.gridy = 2;
				optionsDialog.add(new JLabel("Seconds until automatic forwarding"), optionsDialogConstraints);
				optionsDialogConstraints.gridx = 1;
				optionsDialogConstraints.gridy = 2;
				optionsDialog.add(timeForAutomaticallyNextField, optionsDialogConstraints);

				final JTextField animationStepsPerSecondField = new JTextField(demonstrationConfig.animationStepsPerSecond+"");
				animationStepsPerSecondField.addKeyListener(new KeyListener() {
					@Override
					public void keyTyped(KeyEvent e) { }
					@Override
					public void keyReleased(KeyEvent e) {
						try{
							demonstrationConfig.animationStepsPerSecond =
									Integer.parseInt(animationStepsPerSecondField.getText());
						}
						catch(NumberFormatException nfe){
							//do nothing
						}
					}
					@Override
					public void keyPressed(KeyEvent e) { }
				});
				optionsDialogConstraints.gridx = 0;
				optionsDialogConstraints.gridy = 3;
				optionsDialog.add(new JLabel("Iterations animated per second"), optionsDialogConstraints);
				optionsDialogConstraints.gridx = 1;
				optionsDialogConstraints.gridy = 3;
				optionsDialog.add(animationStepsPerSecondField, optionsDialogConstraints);

				final JTextField numberOfGraphsBeingComputedInAdvanceAtMostField =
						new JTextField(demonstrationConfig.numberOfGraphsBeingComputedInAdvanceAtMost+"");
				numberOfGraphsBeingComputedInAdvanceAtMostField.addKeyListener(new KeyListener() {
					@Override
					public void keyTyped(KeyEvent e) { }
					@Override
					public void keyReleased(KeyEvent e) {
						try{
							demonstrationConfig.numberOfGraphsBeingComputedInAdvanceAtMost =
									Integer.parseInt(numberOfGraphsBeingComputedInAdvanceAtMostField.getText());
						}
						catch(NumberFormatException nfe){
							//do nothing
						}
					}
					@Override
					public void keyPressed(KeyEvent e) { }
				});
				optionsDialogConstraints.gridx = 0;
				optionsDialogConstraints.gridy = 4;
				optionsDialog.add(new JLabel("Max. number of graphs for which drawings are pre-calculated "), optionsDialogConstraints);
				optionsDialogConstraints.gridx = 1;
				optionsDialogConstraints.gridy = 4;
				optionsDialog.add(numberOfGraphsBeingComputedInAdvanceAtMostField, optionsDialogConstraints);


				optionsDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				optionsDialog.setSize(450, 170);
				optionsDialog.setVisible(true);
			}
		});

		//add diagrams
		frameConstraints.gridy = ++frameGridYPos;
		frameConstraints.anchor = GridBagConstraints.WEST;

		diagramPanels = new JPanel[3];
		for(int i=0; i<3; i++){
			frameConstraints.gridx = i;
			diagramPanels[i] = new JPanel();
			frame.getContentPane().add(diagramPanels[i], frameConstraints);
		}
		createDiagrams();





		frame.pack();
		frame.setVisible(true);
		frame.setExtendedState(JFrame.ICONIFIED);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);


		//save reference to all demonstration graph files
		File graphsDirectory = new File(demonstrationConfig.pathToDemonstrationGraphs);
		objectsInDirectory = graphsDirectory.listFiles();
		//To calculation in advance for the layouts
		allLayoutsComputedInAdvance = new DemonstrationLayout[objectsInDirectory.length][3];
		computeLayoutsInAdvance(true);

		startTimerForAutomaticallyNextGraph();
	}














	private static void updateTitles(){

		if(graphIndex==-1){
			graphTitleJLabel.setText(graphtitleTextBegin);
//			graphSubtitleJLabel.setText("<html><font size=6> </html></font>");

		}
		else if(graphIndex==objectsInDirectory.length){
			graphTitleJLabel.setText(graphtitleTextEnd);
//			graphSubtitleJLabel.setText("<html><font size=6> </html></font>");
		}
		else{
			UndirectedGraph<String, String> graph = getCurrentGraph();
			String graphName = objectsInDirectory[graphIndex].getName();
			int indexOfDot = graphName.lastIndexOf('.');
			if (indexOfDot != -1) {
				graphName = graphName.substring(0, indexOfDot);
			}
			graphTitleJLabel.setText("<html><font size=6>"+ (graphIndex+1)+"/"+objectsInDirectory.length+": " + graphName
					+" [n="+graph.getVertexCount()+", m="+graph.getEdgeCount()+"]</font></html>");
//			graphSubtitleJLabel.setText("<html><font size=6>n="+graph.getVertexCount()+", m="+graph.getEdgeCount()+"</font></html>");
		}
		//in any case hide values cpu-time, ...
		for(int i=0; i<3; i++){
			allCpuTimeJLabels[i].setText(" ");
			allEdgeLengthStandardDeviationJLabels[i].setText(" ");
			allNumberOfEdgeCrossingsJLabelsJLabels[i].setText(" ");
		}
	}

	private static void showGraph(){
		numberOfRunningAnimations = 3;
		cancelAutomaticallyNext();

		//special case: no graph selected, but end or start reached
		if(graphIndex==-1 || graphIndex==objectsInDirectory.length){
			for(int i=0; i<3; i++){
				((AffineTransformModifiedStartingPositions<String>)((MutableAffineTransformer)allVV[i].getRenderContext()
						.getMultiLayerTransformer().getTransformer(Layer.LAYOUT)).getTransform()).setReferenceLayout(emptyLayout);
				allDVM[i].setGraphLayout(emptyLayout, shownArea);
				allVV[i].getRenderContext().setEdgeShapeTransformer(EdgeShape.line(emptyLayout.getGraph()));
			}

			return;
		}

		//else

		//draw
		for(int i=0; i<3; i++){
			DemonstrationLayout<String, String> dl = null;
			while(true){
				dl = allLayoutsComputedInAdvance[graphIndex][i];
				if(dl==null){
					computeLayoutsInAdvance(true);
				}
				else{
					break;
				}
			}
			((AffineTransformModifiedStartingPositions<String>)((MutableAffineTransformer)allVV[i].getRenderContext()
					.getMultiLayerTransformer().getTransformer(Layer.LAYOUT)).getTransform()).setReferenceLayout(dl);
			allDVM[i].setGraphLayout(dl, shownArea);
			allVV[i].getRenderContext().setEdgeShapeTransformer(EdgeShape.line(dl.getGraph()));
			allVV[i].repaint();

			//rest view
			allVV[i].getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
			allVV[i].getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
		}
		for(int i=0; i<3; i++){
			allLayoutsComputedInAdvance[graphIndex][i] = null; //Take out of the buffer, place is cleared
		}
	}



	private static UndirectedGraph<String, String> getCurrentGraph(){
		return getGraph(graphIndex);
	}

	private static UndirectedGraph<String, String> getGraph(int graphIndex){
		File currentGraphFile = objectsInDirectory[graphIndex];
		UndirectedGraph<String, String> graph = SuperGraphReader.getGraph(currentGraphFile.getPath());

		if(graph==null){ //either no graph-file or error when reading
			System.err.println("From "+currentGraphFile.getAbsolutePath()+" no graph could be read.");
		}
		return graph;
	}



	/**
	 * May be called also by {@link DefaultVisualizationModelForDemonstration} when drawn ready.
	 *
	 * When not everything is not drawn, with this one can repaint.
	 */
	public static <V,E> void repaintVV(int algorithmenindex){

		allVV[algorithmenindex].repaint();
	}




	/**
	 * Is called by {@link DefaultVisualizationModelForDemonstration} to tell
	 * this class animation is completed.
	 *
	 * Is neede for automatically next.
	 */
	public static void animationCompleted(){
		numberOfRunningAnimations = Math.max(0, numberOfRunningAnimations-1);
		if(numberOfRunningAnimations==0){
			startTimerForAutomaticallyNextGraph();
		}
	}

	/**
	 * Timer for automatically next graph
	 */
	private static void startTimerForAutomaticallyNextGraph(){

		if(!demonstrationConfig.automaticallyNextAfterSpecifiedTime){
			return;
		}

		cancelAutomaticallyNext(); //if one is stil running

		timerForAutomaticallyNext = new Timer();
		timerForAutomaticallyNext.schedule(new TimerTask() {

			@Override
			public void run() {
				if(timerForJLabelAutomaticallyNext!=null){
					timerForJLabelAutomaticallyNext.cancel();
				}
				jLabelForAutomaticallyNext.setText("");
				cancelAutomaticallyNext.setVisible(false);

				next.doClick();
			}

		}, demonstrationConfig.secondsUntilNextGraphIsSelectedAutomatically*1000);

		remainingSecondsToAutomaticallyNext = demonstrationConfig.secondsUntilNextGraphIsSelectedAutomatically;
		timerForJLabelAutomaticallyNext = new Timer();
		timerForJLabelAutomaticallyNext.schedule(new TimerTask() {

			@Override
			public void run() {
				jLabelForAutomaticallyNext.setText(stringToAutomaticallyNextPart1
						+remainingSecondsToAutomaticallyNext+stringToAutomaticallyNextPart2);
				remainingSecondsToAutomaticallyNext = Math.max(0, remainingSecondsToAutomaticallyNext-1);
			}

		}, 0, 1000); //one second, then value is updated

		cancelAutomaticallyNext.setVisible(true);
	}

	private static void cancelAutomaticallyNext(){

		if(timerForAutomaticallyNext!=null){
			timerForAutomaticallyNext.cancel();
		}
		if(timerForJLabelAutomaticallyNext!=null){
			timerForJLabelAutomaticallyNext.cancel();
		}
		jLabelForAutomaticallyNext.setText("");
		cancelAutomaticallyNext.setVisible(false);
	}

	public static void computeLayoutsInAdvance(boolean computeAlsoCurrentLayout){
		//determine number of max. layouts to be computed before
		int startOffset = 0;
		if(!computeAlsoCurrentLayout){
			startOffset = 1;
		}
		for(int i=startOffset; i<=demonstrationConfig.numberOfGraphsBeingComputedInAdvanceAtMost; i++){ //starts at i=1 because current layout
			//shall not be computed again if not "again" is pressed
			//so that not -1 in objectsInDirectory.length
			int validGraphIndex = Math.max(0, graphIndex+i)%objectsInDirectory.length; //modulo, so that ring (from last to first) is closed

			FRLayoutNoMapsNoFrame<String, String> layoutFromWhichInitialPointsAreRead = null;

			while((allLayoutsComputedInAdvance[validGraphIndex][0]==null ^ allLayoutsComputedInAdvance[validGraphIndex][1]==null)
					|| (allLayoutsComputedInAdvance[validGraphIndex][0]==null ^ allLayoutsComputedInAdvance[validGraphIndex][2]==null)
					|| (allLayoutsComputedInAdvance[validGraphIndex][1]==null ^ allLayoutsComputedInAdvance[validGraphIndex][2]==null)){
				try {
					Thread.currentThread().sleep(20);
				} catch (InterruptedException e) {
					//do nothing
				}
			}

			//concerns all 3 layouts (this is a other 3 than in i<3)
			for(int j=0; j<3; j++){


				if(allLayoutsComputedInAdvance[validGraphIndex][j]==null){
					//is null -> calculate
					UndirectedGraph<String, String> graph = getGraph(validGraphIndex);
					FRLayoutNoMapsNoFrame<String, String> referenceLayout;
					if(j==0){
						referenceLayout = new FRLayoutNoMapsNoFrame<String, String>(graph, drawingArea);
						layoutFromWhichInitialPointsAreRead = referenceLayout;
					}
					else if(j==1){
						//////////////////////////////////////////////
						// Parameter to FR+WSPD are determined here //
						//////////////////////////////////////////////
						referenceLayout = new FRWSPDb_b<String, String>(graph,
								/*|||||s-Wert (currently 0.1):|||||*/ 0.1,
								drawingArea);
						/*|||||RecomputationFunction(currently: 5ln + update barycenters):|||||*/
						((FRWSPDb_b)referenceLayout).setRecomputationOfSplitTreeAndWSPDFunction(
								new RecomputationOfSplitTreeAndWSPDFunction(){
									@Override
									public Boolean apply(LayoutWithWSPD<?> layout) {
										int smallerValue = (int) (Math.log(layout.getCurrentIteration())*5);
										int largerValue = (int) (Math.log(layout.getCurrentIteration()+1)*5);
										boolean computeNew = smallerValue!=largerValue;
										if(!computeNew){
											layout.updateBarycenters();
										}
										return computeNew;
									}
								});
						////////////////////////////////////////////////
					}
					else{
						referenceLayout = new FRGrid<String, String>(graph, drawingArea);
					}
					if(j!=0 && layoutFromWhichInitialPointsAreRead!=null){ //it is referenceLayout!=layoutFromWhichInitialPointsAreRead
						for(int k=0; k<layoutFromWhichInitialPointsAreRead.getVertices().size(); k++){
							double xNew = layoutFromWhichInitialPointsAreRead.getVertices().get(k).get2().getX();
							double yNew = layoutFromWhichInitialPointsAreRead.getVertices().get(k).get2().getY();
							referenceLayout.getVertices().get(k).get2().setLocation(xNew, yNew);
						}
					}
					//to avoid "flickering" of the points before/after scaling and to avoid rotations
					referenceLayout.setScaleToDrawingAreaAtTheEnd(false);

					allLayoutsComputedInAdvance[validGraphIndex][j]
							= new DemonstrationLayout<String, String>(referenceLayout, graph, j);
					Thread t = new Thread(allLayoutsComputedInAdvance[validGraphIndex][j]);
					t.start();

					//in this case do not draw further graphs because it is last of the 3 and compute in advance further layouts
					//not before these drawings are complete
					if(j==2){
						return;
					}
				}
			}
		}
	}

	public static void updateDiagramName(){
		for(int i=0; i<chart.length; i++){
			if(chart[i]!=null){
				String graphName = "";
				if(graphIndex!=-1 && graphIndex!=objectsInDirectory.length){
					graphName = objectsInDirectory[graphIndex].getName();
				}
				chart[i].setTitle(graphName);
			}
		}
	}

	private static void createDiagrams(){
		//remove old ones
		for(int i=0; i<3; i++){
			for(Component comp: diagramPanels[i].getComponents()){
				diagramPanels[i].remove(comp);
			}
		}

		//all 3 types (cpu-time, std.dev. edge-length, nmbr. edge crossings)
		diagrams = new ChartPanel[3];
		for(int i=0; i<3; i++){
			diagrams[i] = getChartPanel(i);
			diagramPanels[i].add(diagrams[i]);
		}
	}

	private static ChartPanel getChartPanel(int criterionIndex){
		String graphName = "";
		if(graphIndex!=-1 && graphIndex!=objectsInDirectory.length){
			graphName = objectsInDirectory[graphIndex].getName();
		}
		chart[criterionIndex] = ChartFactory.createBarChart(graphName, null, null, dataset[criterionIndex], PlotOrientation.HORIZONTAL,
				true, true, false);
		if(demonstrationConfig.showDiagramTitle){
			chart[criterionIndex].setTitle(categoriesStringForDiagram[criterionIndex]);
		}

		ChartPanel cp = new ChartPanel(chart[criterionIndex],
				(int) (screenWidth/3.0*demonstrationConfig.scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth),
				demonstrationConfig.hightOfTheDiagrams, 1, 1, 1000000, 1000000, true, false, false, true, true, true);
		return cp;
	}

	private static void showDiagramm(int criterionIndex){
		cancelAutomaticallyNext();

		String graphNname = "";
		if(graphIndex!=-1 && graphIndex!=objectsInDirectory.length){
			graphNname = objectsInDirectory[graphIndex].getName();
		}
		chart[criterionIndex] = ChartFactory.createBarChart(graphNname, null, null, dataset[criterionIndex]);

		ChartFrame cf = new ChartFrame("", chart[criterionIndex], false);
		cf.setSize(400, 300);
		cf.setVisible(true);
	}

	private static void resetDiagramValues(){
		for(int i=0; i<categoriesStringForDiagram.length; i++){
			for(int j=0; j<algorithmsStringForDiagram.length; j++){
				dataset[i].setValue(0, algorithmsStringForDiagram[j], categoriesStringForDiagram[i]);
			}
		}
	}

	private static void rescaleDrawingArea(){
		drawingArea = new Dimension((int)(screenWidth/3*demonstrationConfig.scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth),
				(int)(screenWidth/3*demonstrationConfig.scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth));
		shownArea = new Dimension((int)(screenWidth/3*demonstrationConfig.scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth),
				(int)(screenWidth/3*demonstrationConfig.scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth));

		if(allVV!=null && allVV[0]!=null){
			for(VisualizationViewer<String, String> vv: allVV){
				vv.setSize(shownArea);
			}
			for(int i=0; i<allVV.length; i++){
				repaintVV(i);
			}
		}
	}
}