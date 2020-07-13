package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.objectManager;

import de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement.Statistic;

public class StatisticManager extends Manager<Statistic> {

	@Override
	protected void initialize() {
		for(Statistic st: Statistic.values()){
			super.addToObjectList(st, st.toString());
		}
	}

}
