package de.uniwue.informatik.praline.layouting.layered.main.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class NumberDistribution<N extends Number & Comparable> extends ArrayList<N> {

    public NumberDistribution() {
        super();
    }

    public NumberDistribution(int initialCapacity) {
        super(initialCapacity);
    }

    public double get(StatisticParameter statisticParameter) {
        switch (statisticParameter) {
            case MIN:
                if (this.isEmpty()) {
                    return Double.NaN;
                }
                double min = Double.POSITIVE_INFINITY;
                for (N n : this) {
                    if (n instanceof Double && Double.isNaN((Double) n)) {
                        continue;
                    }
                    min = Math.min(min, n.doubleValue());
                }
                if (min == Double.POSITIVE_INFINITY) {
                    return Double.NaN;
                }
                return min;

            case MAX:
                if (this.isEmpty()) {
                    return Double.NaN;
                }
                double max = Double.NEGATIVE_INFINITY;
                for (N n : this) {
                    if (n instanceof Double && Double.isNaN((Double) n)) {
                        continue;
                    }
                    max = Math.max(max, n.doubleValue());
                }
                if (max == Double.NEGATIVE_INFINITY) {
                    return Double.NaN;
                }
                return max;

            case MEAN:
                if (this.isEmpty()) {
                    return Double.NaN;
                }
                double sum = 0;
                int countedEntries = 0;
                for (N n : this) {
                    if (n instanceof Double && Double.isNaN((Double) n)) {
                        continue;
                    }
                    sum += n.doubleValue();
                    ++countedEntries;
                }
                return sum / (double) countedEntries;

            case MEDIAN:
                LinkedList<N> allEntriesSorted = new LinkedList<>(this);
                //filter out NaNs
                while (allEntriesSorted.contains(Double.NaN)) {
                    allEntriesSorted.remove(Double.NaN);
                }
                if (allEntriesSorted.isEmpty()) {
                    return Double.NaN;
                }
                Collections.sort(allEntriesSorted);
                double lowerMedian = allEntriesSorted.get((allEntriesSorted.size() - 1) / 2).doubleValue();
                double upperMedian = allEntriesSorted.get(allEntriesSorted.size() / 2).doubleValue();
                return (lowerMedian + upperMedian) / 2.0;

            case MODE_SMALLEST:
                return determineMode(false);

            case MODE_LARGEST:
                return determineMode(true);


            case VARIANCE:
                double mean = this.get(StatisticParameter.MEAN);
                double sumOfSquaredDeviations = 0;
                countedEntries = 0;
                for (N n : this) {
                    if (n instanceof Double && Double.isNaN((Double) n)) {
                        continue;
                    }
                    sumOfSquaredDeviations += Math.pow(n.doubleValue() - mean, 2);
                    ++countedEntries;
                }
                return sumOfSquaredDeviations / (double) countedEntries;

            case STANDARD_DEVIATION:
                return Math.sqrt(this.get(StatisticParameter.VARIANCE));

            case SUM:
                sum = 0;
                for (N n : this) {
                    if (n instanceof Double && Double.isNaN((Double) n)) {
                        continue;
                    }
                    sum += n.doubleValue();
                }
                return sum;

            case COUNT:
                return this.size();

            case COUNT_MIN:
                double value = this.get(StatisticParameter.MIN);
                return countValue(value);

            case COUNT_MAX:
                value = this.get(StatisticParameter.MAX);
                return countValue(value);

            case COUNT_MODE:
                value = this.get(StatisticParameter.MODE_SMALLEST);
                return countValue(value);

            default:
                return 0;
        }
    }

    private double countValue(double value) {
        int count = 0;
        for (N n : this) {
            if (n.doubleValue() == value) {
                ++count;
            }
        }
        return count;
    }

    private double determineMode(boolean falseSmallestModeTrueLargestMode) {
        LinkedList<N> allEntriesSorted;
        allEntriesSorted = new LinkedList<>(this);
        //filter out NaNs
        while (allEntriesSorted.contains(Double.NaN)) {
            allEntriesSorted.remove(Double.NaN);
        }
        if (allEntriesSorted.isEmpty()) {
            return Double.NaN;
        }
        Collections.sort(allEntriesSorted);
        double mode = allEntriesSorted.getFirst().doubleValue();
        int maxOccurrence = 0;
        int currentOccurrence = 1;
        N prev = null;
        for (N n : allEntriesSorted) {
            if (prev != null) {
                if (prev.equals(n)) {
                    ++currentOccurrence;
                    if (currentOccurrence >= maxOccurrence + (falseSmallestModeTrueLargestMode ? 0 : 1)) {
                        maxOccurrence = currentOccurrence;
                        mode = n.doubleValue();
                    }
                }
                else {
                    currentOccurrence = 1;
                }
            }
            prev = n;
        }
        return mode;
    }
}
