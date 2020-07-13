package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.demonstration;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.Testclass;

import java.io.File;

public class DemonstrationConfig {
	
	
	public String pathToDemonstrationGraphs = Testclass.pathsConfig.pathToGraphs+File.separator+"graphs_for_demonstration";
	
	public boolean showDiagramTitle = false;

	public boolean automaticallyNextAfterSpecifiedTime = true;
	
	public int animationStepsPerSecond = 10;
	
	public int secondsUntilNextGraphIsSelectedAutomatically = 15;
	
	/**
	 * How many layouts should be computed in advance?
	 * If this is set to 0, then the drawings are computed not before they are needed
	 * (on demand).
	 * If this value is set to sth >0 it may happen that the animation seems to
	 * be not fluent any more. In that case this should be set to 0.
	 * With a value=0 it might also happen that the animation is faster than
	 * the calculation and the animation has to wait (needs more time).
	 * For that a value >0 here is helpful.
	 */
	public int numberOfGraphsBeingComputedInAdvanceAtMost = 2;
	
	/**
	 * relative value
	 */
	public double scalingOfTheDrawingAreaRelativeToOneThirdOfTheWindowWidth = 0.88;
	
	/**
	 * absolute value (may be changed to a relative value or this
	 * and {@link DemonstrationOfGraphDrawing#numberOfGraphsBeingComputedInAdvanceAtMost}
	 * may be set automatically to good values relative to the window-size, at the moment
	 * this has to happen manually)
	 */
	public int hightOfTheDiagrams = 116;
}
