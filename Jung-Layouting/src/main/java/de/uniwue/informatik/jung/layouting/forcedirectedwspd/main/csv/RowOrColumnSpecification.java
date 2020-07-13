package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.csv;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

public abstract class RowOrColumnSpecification {
	protected Statistic methodOfCombining;
	protected String nameOfTheRowOrColumn;
	
	/**
	 * @param methodOfCombining
	 * Can be null, then default is taken: {@link Statistic#MEAN}
	 */
	public RowOrColumnSpecification(Statistic methodOfCombining, String nameOfTheRowOrColumn){
		if(methodOfCombining==null){
			this.methodOfCombining = Statistic.MEAN; //default if null
		}
		else{
			this.methodOfCombining = methodOfCombining;
		}
		this.nameOfTheRowOrColumn = nameOfTheRowOrColumn;
	}
	
	public Statistic getMethodOfCombining(){
		return methodOfCombining;
	}
	
	public String getNameOfTheRowOrColumn(){
		return nameOfTheRowOrColumn;
	}
}
