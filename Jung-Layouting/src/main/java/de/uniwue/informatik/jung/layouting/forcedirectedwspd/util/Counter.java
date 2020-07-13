package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util;

/**
 * A Counter starting at 0
 */
public class Counter {
	private int value;
	
	public Counter(){
		value = 0;
	}
	
	/**
	 * Returns the current value.
	 * Does Not increment this value.
	 */
	public int get(){
		return value;
	}
	
	public void incrementBy1(){
		++value;
	}
}
