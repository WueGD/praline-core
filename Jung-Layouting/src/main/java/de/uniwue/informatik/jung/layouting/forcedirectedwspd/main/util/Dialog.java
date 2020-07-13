package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import java.util.Scanner;

public class Dialog {
	/**
	 * Equivalent to {@link Dialog#assistentBoolean(sc, question, "Yes", "No")}
	 * 
	 * @param sc
	 * @param question
	 * @return
	 */
	public static boolean assistantConfirm(Scanner sc, String question){
		return assistentBoolean(sc, question, "Yes", "No");
	}
	
	public static boolean assistentBoolean(Scanner sc, String question, String trueOption, String falseOption){
		int input = getNextInt(sc, question+"\n1: "+trueOption+"\n0: "+falseOption, 0, 1);		
		return (input==1) ? true : false;
	}
	
	public static String getNextLine(Scanner sc, String question){
		/* 
		 * Maybe one line was inserted before. That is catched with the following to lines of code
		 */
		System.out.println("[Press Enter if and only if this is the last line of the output.]");
		sc.nextLine();
		
		System.out.println("");
		System.out.println(question);
		return sc.nextLine();
	}
	
	
	
	public static int getNextInt(Scanner sc, String question){
		return getNextInt(sc, question, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns Integer-value in range [lowerBound,upperBound] gotten through Scanner sc.
	 * sc is checked against invalid inputs.
	 * 
	 * @param sc
	 * @param question
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public static int getNextInt(Scanner sc, String question, int lowerBound, int upperBound){
		if(lowerBound == Integer.MIN_VALUE){
			lowerBound = lowerBound + 1;
		}
		int input = lowerBound-1;
		System.out.println(question);
		while(true){
			while(sc.hasNext()){
				if(sc.hasNextInt()){
					input = sc.nextInt();
					break;
				}
				else{
					sc.next();
				}
			}
			if(input<lowerBound || input>upperBound){
				System.out.println("Invalid Input. (Input must be in bounds ["+lowerBound+","+upperBound+"])");
			}
			else{
				break;
			}
		}
		return input;
	}
	
	public static double getNextDouble(Scanner sc, String question){
		return getNextDouble(sc, question, Double.NEGATIVE_INFINITY, Double.MAX_VALUE);
	}
	
	/**
	 * Returns Double-value in range ]lowerBound,upperBound] gotten through Scanner sc.
	 * So lowerBound is no valid input, only greater values!
	 * sc is checked against invalid inputs.
	 * 
	 * @param sc
	 * @param question
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public static double getNextDouble(Scanner sc, String question, double lowerBound, double upperBound){
		double input = lowerBound;
		System.out.println(question);
		while(true){
			while(sc.hasNext()){
				if(sc.hasNextDouble()){
					input = sc.nextDouble();
					break;
				}
				else{
					sc.next();
				}
			}
			if(input<=lowerBound || input>upperBound){
				System.out.println("Invalid Input. (Input must be in bounds ]"+lowerBound+","+upperBound+"])");
			}
			else{
				break;
			}
		}
		return input;
	}
}
