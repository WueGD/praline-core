package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.qualityMeasurement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public enum Statistic {
	/**
	 * min. value
	 */
	SMALLEST_VALUE,
	/**
	 * max. value
	 */
	LARGEST_VALUE,
	SUM,
	/**
	 * arithmetic mean
	 */
	MEAN,
	MEDIAN,
	/**
	 * == ({@link Statistic#STANDARD_DEVIATION})^2
	 */
	VARIANCE,
	/**
	 * == sqrt({@link Statistic#VARIANCE})
	 */
	STANDARD_DEVIATION;
	
	
	public static double computeStatistic(Statistic st, Number[] setOfValues){
		/**
		 * These values are:
		 * {min, max, sum, sumOfTheSquares}
		 */
		double[] calculatedValues = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0};
		List<Double> allValues = new LinkedList<Double>();
		
		/**
		 * Single NaN-values (not a number) make the whole statistical value being NaN.
		 * To avoid that those values are excluded (not considered in the computation)
		 * and only if all values are NaN then the whole statistical value is NaN, too.
		 */
		int numberOfNaNValues = 0;
		
		for(Number x: setOfValues){
			if(!doCalculationsInTheLoop(st, x, calculatedValues, allValues)){
				numberOfNaNValues++;
			}
		}
		
		return doFinalCalculationsOutsideTheLoop(st, setOfValues.length, calculatedValues, allValues, numberOfNaNValues);
	}
	
	public static double computeStatistic(Statistic sg, List<Double> setOfValues){
		/**
		 * These values are:
		 * {min, max, sum, sumOfTheSquares}
		 */
		double[] calculatedValues = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0};
		
		/**
		 * Single NaN-values (not a number) make the whole statistical value being NaN.
		 * To avoid that those values are excluded (not considered in the computation)
		 * and only if all values are NaN then the whole statistical value is NaN, too.
		 */
		int numberOfNaNValues = 0;
		
		for(Number x: setOfValues){
			if(!doCalculationsInTheLoop(sg, x, calculatedValues, null)){
				numberOfNaNValues++;
			}
		}
		
		return doFinalCalculationsOutsideTheLoop(sg, setOfValues.size(), calculatedValues, setOfValues, numberOfNaNValues);
	}
	
	/**
	 * Primary for intern computation.
	 * But it is public so that it can be called outside this Enum as well.
	 * <p>
	 * No value is returned, instead the array calculatedValues and the list allValues are edited.
	 * This array has length 4 contains the values {min, max, sum, sumOfTheSquares}.
	 * Overview which value is needed for which {@link Statistic} (marked with "x") and which is not needed
	 * (that can be null or some arbitrary value):
	 * 
	 * <table>
	 *  <tr>
	 *   <th> st: </th>
	 *   <th> SMALLEST_VALUE </th>
	 *   <th> LARGEST_VALUE </th>
	 *   <th> SUM </th>
	 *   <th> MEAN </th>
	 *   <th> MEDIAN </th>
	 *   <th> VARIANCE </th>
	 *   <th> STANDARD_DEVIATION </th>
	 *  </tr>
	 *  <tr>
	 *   <th> currentNumber: </th>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> min: </th>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *  </tr>
	 *  <tr>
	 *   <th> max: </th>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *  </tr>
	 *  <tr>
	 *   <th> sum: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> sumOfTheSquares: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> allValues: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> (x) </td>
	 *   <td>  </td>
	 *  </tr>
	 * </table>
	 * <p>
	 * 
	 * @param st specifies what shall be computed
	 * @param currentNumber the current number (when going through all values in a loop)
	 * @param calculatedValues Array that must have length 4. The values of it are:
	 * <br> { 
	 * <br>calculatedValues[0] =
	 * @param min smallest value found till now
	 * <br> calculatedValues[1] =
	 * @param max largest value found till now
	 * <br> calculatedValues[2] =
	 * @param sum sum of the values gone through so far
	 * <br> calculatedValues[3] =
	 * @param sumOfTheSquares sum of the squared values gone through so far
	 * <br> }
	 * @param allValues	List of all values gone through so far.
	 * 				Only relevant for the calculation of the {@link Statistic#MEDIAN}.
	 * 				But can be null even then if the median is calculated via a another already existing list.
	 * 				To this list no NaN-values are added.
	 * @return true iff the currentNumber has influence on the calculation, that is if it is not NaN.
	 * 				(Thus it returns false if it is NaN)
	 */
	public static boolean doCalculationsInTheLoop(Statistic st, Number currentNumber,
			double[] calculatedValues, List<Double> allValues){
		
		if(Double.isNaN(currentNumber.doubleValue())){
			return false;
		}
		
			
		if(st==SMALLEST_VALUE){
			calculatedValues[0] = Math.min((double) currentNumber, calculatedValues[0]);
		}
		else if(st==LARGEST_VALUE){
			calculatedValues[1] = Math.max((double) currentNumber, calculatedValues[1]);
		}
		else if(st==SUM){
			calculatedValues[2] += (double) currentNumber;
		}
		else if(st==MEAN){
			calculatedValues[2] += (double) currentNumber;
		}
		else if(st==MEDIAN){
			if(allValues!=null){
				allValues.add((double) currentNumber);
			}
		}
		else if(st==VARIANCE || st==STANDARD_DEVIATION){
			calculatedValues[2] += (double) currentNumber;
			calculatedValues[3] += (double) currentNumber* (double) currentNumber;
		}
		
		return true;
	}
	
	/**
	 * Primary for intern computation.
	 * But it is public so that it can be called outside this Enum as well.
	 * <p>
	 * Overview which value is needed for which {@link Statistic} (marked with "x") and which is not needed
	 * (that can be null or some arbitrary value):
	 * 
	 * <table>
	 *  <tr>
	 *   <th> st: </th>
	 *   <th> SMALLEST_VALUE </th>
	 *   <th> LARGEST_VALUE </th>
	 *   <th> SUM </th>
	 *   <th> MEAN </th>
	 *   <th> MEDIAN </th>
	 *   <th> VARIANCE </th>
	 *   <th> STANDARD_DEVIATION </th>
	 *  </tr>
	 *  <tr>
	 *   <th> sizeOfTheValueSet: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> min: </th>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *  </tr>
	 *  <tr>
	 *   <th> max: </th>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *  </tr>
	 *  <tr>
	 *   <th> summe: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> summeDerQuadrate: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td> x </td>
	 *  </tr>
	 *  <tr>
	 *   <th> alleWerte: </th>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *   <td> x </td>
	 *   <td>  </td>
	 *   <td>  </td>
	 *  </tr>
	 * </table>
	 * <p>
	 * 
	 * @param st specifies what shall be computed
	 * @param sizeOfTheValueSet size of the set of values from which this {@link Statistic} shall be computed
	 * @param calculatedValues Array that must have length 4. The values of it are:
	 * <br> { 
	 * <br>calculatedValues[0] =
	 * @param min smallest value found
	 * <br> calculatedValues[1] =
	 * @param max largest value found
	 * <br> calculatedValues[2] =
	 * @param sum sum of all values
	 * <br> calculatedValues[3] =
	 * @param sumOfTheSquares sum of the square of each value
	 * <br> }
	 * @param allValues list of all values (only relevant for the {@link Statistic#MEDIAN}, should not contain NaN-values)
	 * @param numberOfNaNValues	Single NaN-values (not a number) make the whole statistical value being NaN.
	 * 				To avoid that those values are excluded (not considered in the computation)
	 * 				and only if all values are NaN then the whole statistical value is NaN, too.
	 */
	public static double doFinalCalculationsOutsideTheLoop(Statistic st, int sizeOfTheValueSet,
			double[] calculatedValues, List<Double> allValues, int numberOfNaNValues){
		if(st==SMALLEST_VALUE){
			return calculatedValues[0];
		}
		if(st==LARGEST_VALUE){
			return calculatedValues[1];
		}
		if(st==SUM){
			return calculatedValues[2];
		}
		if(st==MEAN){
			return calculatedValues[2]/(sizeOfTheValueSet-numberOfNaNValues);
		}
		if(st==MEDIAN){
			Collections.sort(allValues);
			double storedValue = 0;
			int middleIndex = allValues.size()/2;
			boolean evenNumberOfValues = ((double)(allValues.size()/2)==(double)allValues.size()/2);
			int counter = 0;
			for(double x: allValues){
				if(evenNumberOfValues){
					if(counter==middleIndex-1){
						storedValue = x;
					}
					else if(counter==middleIndex){
						return (storedValue+x)/2;
					}
				}
				else{
					if(counter==middleIndex){
						return x;
					}
				}
				counter++;
			}
		}
		if(st==VARIANCE){
			//Formula for the calculation of the variance (uncorrected sample variance):
			//{s'}^2=\frac{1}{n} \sum_{i=1}^n (x_i - \bar{x})^2=\overline{x^2}-\bar{x}^2 "
			//Where {s'}^2 is the variance (and s' is the standard deviation)
			//n is the number of elements in the set
			//x_i is the i-th element of the set
			//\bar{x} is the arithmetic mean of the set
			//\overline{x^2} is the arithmetic mean of the squared values of the set
			return calculatedValues[3]/(sizeOfTheValueSet-numberOfNaNValues)-calculatedValues[2]/(sizeOfTheValueSet-numberOfNaNValues)
					*calculatedValues[2]/(sizeOfTheValueSet-numberOfNaNValues);
		}
		if(st==STANDARD_DEVIATION){
			//For the formula see (st==VARIANCE)
			return Math.sqrt(calculatedValues[3]/(sizeOfTheValueSet-numberOfNaNValues)-calculatedValues[2]/(sizeOfTheValueSet-numberOfNaNValues)
					*calculatedValues[2]/(sizeOfTheValueSet-numberOfNaNValues));
		}
		return -1; //can never reach this (if no new Statistic-enums are added)
	}
}