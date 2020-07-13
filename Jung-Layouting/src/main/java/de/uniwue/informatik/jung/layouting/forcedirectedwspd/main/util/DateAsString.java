package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import java.util.Calendar;

public abstract class DateAsString {
	/**
	 * Generates a String of the current time as
	 * "YYYY.MM.DD_hh-mm-ss".
	 * This format with dots, underlines and dashes was chosen to have
	 * only charcters that can be used in the file-system.
	 */
	public static String getCurrentTimeInFormatYYYYDotMMDotDD_hhDashmmDashss(){
		Calendar now = Calendar.getInstance();
		String year = now.get(Calendar.YEAR)+"";
		String month = (now.get(Calendar.MONTH)+1)+"";
		month = month.length()<2 ? "0"+month : month;
		String day = now.get(Calendar.DAY_OF_MONTH)+"";
		day = day.length()<2 ? "0"+day : day;
		String hour = now.get(Calendar.HOUR_OF_DAY)+"";
		hour = hour.length()<2 ? "0"+hour : hour;
		String minute = now.get(Calendar.MINUTE)+"";
		minute = minute.length()<2 ? "0"+minute : minute;
		String second = now.get(Calendar.SECOND)+"";
		second = second.length()<2 ? "0"+second : second;
		
		return year+"."+month+"."+day+"_"+hour+"-"+minute+"-"+second;
	}
}
