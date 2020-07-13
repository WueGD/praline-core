package de.uniwue.informatik.jung.layouting.forcedirectedwspd.layoutAlgorithms;

import edu.uci.ics.jung.algorithms.util.IterativeContext;

/**
 * Used for algorithms that allow a special variant of the step-method that
 * yields the runtime needed for the computation of the repulsive forces.
 * 
 * @author Fabian Lipp
 *
 */
public interface AlgorithmMeasureRepulsiveTime extends IterativeContext {
	long stepMeasureRepulsiveTime();
}
