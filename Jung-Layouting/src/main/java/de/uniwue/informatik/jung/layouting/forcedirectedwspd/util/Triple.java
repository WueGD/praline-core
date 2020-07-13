package de.uniwue.informatik.jung.layouting.forcedirectedwspd.util;


/**
 * Tuple for 3 objects, all of variable type.
 * Analog to {@link Tuple}.
 */
public class Triple<T1, T2, T3> {
	private T1 t1;
	private T2 t2;
	private T3 t3;
	
	public Triple(T1 t1, T2 t2, T3 t3){
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
	}

	public T1 get1() {
		return t1;
	}

	public T2 get2() {
		return t2;
	}
	
	public T3 get3() {
		return t3;
	}
}
