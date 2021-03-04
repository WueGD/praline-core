package de.uniwue.informatik.praline.layouting.layered.main.testsforpaper;

import de.uniwue.informatik.praline.layouting.layered.main.util.NumberDistribution;
import de.uniwue.informatik.praline.layouting.layered.main.util.StatisticParameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Stream;

public class CsvDataExtraction {

    private static final String DATA_PATH =
            "Praline-Layouting/results/" +
//                    "paper-all-tests-2020-06-10_06-18-04";
//                    "paper-all-tests-2020-08-20_16-01-53";
//                    "2020-11-26_04-07-39";
//                    "2021-01-25_19-14-23"; //regular run
//                    "2021-01-27_02-34-35"; //regular run (new: draw each component individually)
//                    "2021-01-28_01-59-11"; //no back-moving of ports on wrong sides
//                    "2021-01-28_02-24-56"; //no back-moving of ports on wrong sides and no re-unpacking of vertex groups
//                    "2021-01-28_04-40-34"; //same as above + own port group for each wrong-assigned instead of 1 for all
//                    "2021-01-28_16-46-40"; //with DummyNodeCreation.createTurningDummiesAndSelfLoopDummies2()
//                    "2021-01-29_04-47-27"; //regular run with intermediate layers being removed in EdgeRouting
//                    "2021-01-29_13-59-15"; //rr with preference of edges non-incident to turning dummies in NodePlacement
//                    "2021-02-01_02-14-59"; //rr with extra layer sweep vertex sorting phase after fixing port positions
//                    "2021-02-02_02-14-46"; //rr with changes in NodePlacement to prefer longer edges in making them straight
//                    "2021-02-03_04-51-33"; //rr with final sorting + in CM with assignment acc. to other side ports
//                    "2021-02-03_14-54-05"; //same as above but only 1 run (1 run is new default also for the next)
//                    "2021-02-03_19-32-04"; //rr with final sorting + in CM with keeping nodes w/o edges at curr pos [CURRENT BEST]
//                    "2021-02-04_00-16-18"; //same as before but with computing pseudo barycenters from curr pos for nodes w/o edges
//                    "2021-02-04_00-45-10"; //same as 2 before but w/o final sorting
//                    "2021-02-04_01-06-35"; //same as 2 before but w/o final sorting
//                    "2021-02-04_01-30-09"; //same as 4 before but with also ignoring nodes with multiple port groups in port sorting
//                    "2021-02-04_02-27-04"; //same as 4 before but with also ignoring nodes with multiple port groups in port sorting
//                    "2021-02-04_04-23-07"; //same as 2021-02-03_19-32-04, but dummy turning points are weighted 2:1 towards their nodes
//                    "2021-02-05_18-44-21"; //should be the same as 2021-02-03_19-32-04 (changed only code style in NodePlacement)
//                    "2021-02-05_19-06-26"; //same as above
//                    "2021-02-05_19-49-53"; //same as above
//                    "2021-02-08_17-53-15"; //NodePlacement without maps
//                    "2021-02-08_18-55-28"; //in NodePlacement average of 2 median x-coordinates (instead of all 4 x-coordinates)
//                    "2021-02-09_20-27-21"; //rr with smaller vertices: max stretch = 4.0
//                    "2021-02-09_23-38-16"; //same as before but unbounded stretch
//                    "2021-02-10_00-16-05"; //same as 2 before but gaps btw. real ports and bounding ports are not closed
//                    "2021-02-10_00-49-22"; //rr with smaller vertices: max stretch = 8.0
//                    "2021-02-19_10-24-13"; //rr with individual-size ports & smaller dummy vertices in NodePlacement
//                    "2021-02-20_02-51-39"; //separating pairs in NodePlacement only at non-dummy nodes
//                    "2021-03-02_17-45-56"; //NodePlacement only top-left, variable width padding ports
//                    "2021-03-02_17-46-51"; //NodePlacement only top-left, unit width padding ports
//                    "2021-03-03_03-23-24"; //rr with NodePlacement back to grid-like arrangement + unit width padding ports
//                    "2021-03-03_04-24-45"; //same as before + in NodePlacement use flags for first ports of nodes
//                    "2021-03-04_00-52-49"; //same as before + allow broader gaps for the node side having fewer ports
                    "2021-03-04_01-45-45"; //rr (without broader gaps for the node side having fewer ports)

    private static final String[] DATA_DIRS =
            {
//                    "DA_lc-praline-package-2020-05-18",
//                    "DA_generated_2020-08-20_04-42-39",
//                    "CM_lc-praline-package-2020-05-18",
//                    "CM_generated_2020-08-20_04-42-39"
                    "CM_praline-package-2020-05-18"
            };


    private static final boolean TABLE_CONTENT_OUTPUT = false; //otherwise better readable for humans

    private static final DecimalFormat OUTPUT_FORMAT_MEAN =
            new DecimalFormat("#.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat OUTPUT_FORMAT_MEAN_RELATIVE_TO =
            new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat OUTPUT_FORMAT_SD =
            new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat OUTPUT_PERCENT_FORMAT =
            new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String PERCENT_SYMBOL = " \\%"; //"";
    private static final String INSTEAD_OF_LEADING_ZERO = "~";
    private static final boolean IGNORE_SD = true;

    private static final String[] CONSIDER_FILES = {"noc", "nob", "nodn", "space", "time"};

    private static final String[] SORT_ENTRIES_ORDER = {"fd", "bfs", "ran"};

    //set to an unkonwn value or null to have relative to the best
    private static final String ENTRIES_RELATIVE_TO =
//            "ran";
            "kieler";
//            null;

    private static final Map<String, String> KNOWN_NAMES = new LinkedHashMap<>() {
        {
            put("noc", "\\ncr");
            put("nob", "\\nbp");
            put("nodn", "\\ndv");
            put("ratio", "w:h");
            put("#vtcs", "vtcs");
//            put("ports-noMove-noPlaceTurnings", "ports");
//            put("mixed-noMove-noPlaceTurnings", "mixed");
//            put("nodes-noMove-noPlaceTurnings", "nodes");
            put("ran", "rand");
        }
    };

    private static final List<String> IGNORE_FIELDS_CONTAINING_STRING =
            Arrays.asList("#vtcs"); //, "-move", "-placeTurnings", "-area", "-ratio");

    public static void main(String[] args) {
        for (String dataDir : DATA_DIRS) {
            analyzeCsvDir(DATA_PATH + File.separator + dataDir);
        }
    }

    private static void analyzeCsvDir(String dataDirPath) {
        System.out.println("============================");
        System.out.println("Read " + dataDirPath);
        System.out.println("============================");
        System.out.println();

        boolean firstEntry = true;
        Map<String, NumberDistribution<Integer>> method2absoluteTime = null;
        List<String> methodsForTime = null;
        for (String testCase : CONSIDER_FILES) {
            //read data
            List<File> files = new LinkedList<>();
            File dir = new File(dataDirPath);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (child.getName().contains(testCase)) {
                        files.add(child);
                    }
                }
            }

            //create data structure
            List<Map<String, NumberDistribution<Integer>>> results = new ArrayList<>();
            for (File file : files) {
                Map<String, NumberDistribution<Integer>> result = readFile(file.getPath(), testCase);
                if (result == null) {
                    continue;
                }
                results.add(result);
            }


            List<String> methods = new ArrayList<>(results.get(0).keySet());

            //special handling: space
            if (testCase.equals("space")) {
                evaluateSpace(results, methods);
                continue;
            }

            //compute resulting values
            Map<String, NumberDistribution<Double>> method2relativeQuality = new LinkedHashMap<>();
            Map<String, NumberDistribution<Integer>> method2best = new LinkedHashMap<>();
            if (testCase.equals("time")) {
                method2absoluteTime = new LinkedHashMap<>();
                methodsForTime = methods;
            }
            for (String method : methods) {
                method2relativeQuality.put(method, new NumberDistribution<>());
                method2best.put(method, new NumberDistribution<>());
                if (testCase.equals("time")) {
                    method2absoluteTime.put(method, new NumberDistribution<>());
                }
            }
            //for each graph do evaluation, add it to all methods
            for (Map<String, NumberDistribution<Integer>> result : results) {
                Map<String, Integer> candidates = new LinkedHashMap<>(methods.size());
                //find the values of each individual method
                for (String method : methods) {
                    int value = (int) result.get(method).get(StatisticParameter.MIN);
                    candidates.put(method, value);
                    if (testCase.equals("time")) {
                        method2absoluteTime.get(method).add(value);
                    }
                }
                findBestValues(methods, method2relativeQuality, method2best, candidates);
            }

            //text output
            if (TABLE_CONTENT_OUTPUT) {
                if (firstEntry) {
                    tableHeadLineOutput(methods);
                    firstEntry = false;
                }
                tableTextOutput(testCase, methods, method2relativeQuality, method2best);
            }
            else {
                regularTextOutput(testCase, results.size(), methods, method2relativeQuality, method2best);
            }
        }

        if (method2absoluteTime != null) {
            regularTextOutputTime(methodsForTime, method2absoluteTime);
        }
    }

    private static void evaluateSpace(List<Map<String, NumberDistribution<Integer>>> results,
                                      Collection<String> methods) {
        int scaleDownDivisor = 1; //1000;
        List<String> newMethods = new ArrayList<>(methods.size());
        for (String method : methods) {
            if (method.contains("-ratio") || method.contains("-area")) {
                continue;
            }
            String newMethod = withoutWidthOrHeight(method);
            if (!newMethods.contains(newMethod)) {
                newMethods.add(newMethod);
            }
        }

        //compute resulting values
        Map<String, NumberDistribution<Double>> widthMethod2relativeQuality = new LinkedHashMap<>();
        Map<String, NumberDistribution<Integer>> widthMethod2best = new LinkedHashMap<>();
        Map<String, NumberDistribution<Double>> heightMethod2relativeQuality = new LinkedHashMap<>();
        Map<String, NumberDistribution<Integer>> heightMethod2best = new LinkedHashMap<>();
        Map<String, NumberDistribution<Double>> areaMethod2relativeQuality = new LinkedHashMap<>();
        Map<String, NumberDistribution<Integer>> areaMethod2best = new LinkedHashMap<>();
        Map<String, NumberDistribution<Double>> ratioMethod2relativeQuality = new LinkedHashMap<>();
        Map<String, NumberDistribution<Integer>> ratioMethod2best = new LinkedHashMap<>();
        for (String method : newMethods) {
            widthMethod2relativeQuality.put(method, new NumberDistribution<>());
            widthMethod2best.put(method, new NumberDistribution<>());
            heightMethod2relativeQuality.put(method, new NumberDistribution<>());
            heightMethod2best.put(method, new NumberDistribution<>());
            areaMethod2relativeQuality.put(method, new NumberDistribution<>());
            areaMethod2best.put(method, new NumberDistribution<>());
            ratioMethod2relativeQuality.put(method, new NumberDistribution<>());
            ratioMethod2best.put(method, new NumberDistribution<>());
        }

        //for each graph do evaluation, add it to all methods
        for (Map<String, NumberDistribution<Integer>> result : results) {
            Map<String, NumberDistribution<Integer>> width = new LinkedHashMap<>();
            Map<String, NumberDistribution<Integer>> height = new LinkedHashMap<>();
            for (String method : newMethods) {
                width.put(method, new NumberDistribution<>());
                height.put(method, new NumberDistribution<>());
            }

            Map<String, Integer> widthCandidates = new LinkedHashMap<>(newMethods.size());
            Map<String, Integer> heightCandidates = new LinkedHashMap<>(newMethods.size());
            Map<String, Integer> areaCandidates = new LinkedHashMap<>(newMethods.size());
            Map<String, Double> candidatesRatio = new LinkedHashMap<>(newMethods.size());
            //first find best of each individual method
            for (String method : methods) {
                if (method.contains("-width")) {
                    width.put(withoutWidthOrHeight(method), result.get(method));
                }
                else if (method.contains("-height")) {
                    height.put(withoutWidthOrHeight(method), result.get(method));
                }
            }
            for (String method : newMethods) {
                NumberDistribution<Integer> w = width.get(method);
                NumberDistribution<Integer> h = height.get(method);
                if (w == null || h == null) {
                    continue;
                }
                int minWidth = Integer.MAX_VALUE;
                int minHeight = Integer.MAX_VALUE;
                int minArea = Integer.MAX_VALUE;
                double bestRatio = Double.POSITIVE_INFINITY;
                for (int i = 0; i < w.size(); i++) {
                    minWidth = Math.min(minWidth, w.get(i));
                    minHeight = Math.min(minHeight, h.get(i));
                    int area = (w.get(i) / scaleDownDivisor) * (h.get(i) / scaleDownDivisor);
                    minArea = Math.min(minArea, area);
                    double ratio = (double) w.get(i) / (double) h.get(i);
                    bestRatio = 1.0 + Math.min(Math.abs(bestRatio - 1.0),
                            Math.abs((Double.isNaN(ratio) ? bestRatio : ratio) - 1.0));
                }
                widthCandidates.put(method, minWidth);
                heightCandidates.put(method, minHeight);
                areaCandidates.put(method, minArea);
                candidatesRatio.put(method, bestRatio);
            }

            //find best for categories width, height, area
            findBestValues(newMethods, widthMethod2relativeQuality, widthMethod2best, widthCandidates);
            findBestValues(newMethods, heightMethod2relativeQuality, heightMethod2best, heightCandidates);
            findBestValues(newMethods, areaMethod2relativeQuality, areaMethod2best, areaCandidates);

            //separate handling for category ratio, because we treat it a bit differently
            double bestRatio = Double.POSITIVE_INFINITY;
            double referenceRatio = Double.POSITIVE_INFINITY;
            boolean hasReferenceRatio = false;
            for (String method : newMethods) {
                Double entry = candidatesRatio.get(method);
                if (method.equals(ENTRIES_RELATIVE_TO)) {
                    referenceRatio = entry;
                    hasReferenceRatio = true;
                }
                bestRatio =
                        1.0 + Math.min(Math.abs(bestRatio - 1.0), Math.abs((entry == null ? bestRatio : entry) - 1.0));
                if (!hasReferenceRatio) {
                    referenceRatio = bestRatio;
                }
            }
            //specify quality of everyone relative to best
            for (String method : newMethods) {
                Double valueRatio = candidatesRatio.get(method);
                if (valueRatio == null) {
                    continue;
                }
                ratioMethod2best.get(method).add(valueRatio == bestRatio ? 1 : 0);
                //we treat reference value == 0 as reference value == 1 to avoid a relative value of infinity
                ratioMethod2relativeQuality.get(method).add(valueRatio == 1.0 ? 1.0 :
                        valueRatio >= 1 ? valueRatio / referenceRatio : referenceRatio / valueRatio);
            }
        }

        //text output
        if (TABLE_CONTENT_OUTPUT) {
            tableTextOutput("width", newMethods, widthMethod2relativeQuality, widthMethod2best);
            tableTextOutput("height", newMethods, heightMethod2relativeQuality, heightMethod2best);
            tableTextOutput("area", newMethods, areaMethod2relativeQuality, areaMethod2best);
            tableTextOutput("ratio", newMethods, ratioMethod2relativeQuality, ratioMethod2best);
        }
        else {
            regularTextOutput("width", results.size(), newMethods, widthMethod2relativeQuality, widthMethod2best);
            regularTextOutput("height", results.size(), newMethods, heightMethod2relativeQuality, heightMethod2best);
            regularTextOutput("area", results.size(), newMethods, areaMethod2relativeQuality, areaMethod2best);
            regularTextOutput("ratio", results.size(), newMethods, ratioMethod2relativeQuality, ratioMethod2best);
        }
    }

    private static void findBestValues(List<String> methods,
                                       Map<String, NumberDistribution<Double>> method2relativeQuality,
                                       Map<String, NumberDistribution<Integer>> method2best,
                                       Map<String, Integer> candidates) {
        //now the reference value (as specified or best total)
        int referenceValue = Integer.MAX_VALUE;
        int bestValue = Integer.MAX_VALUE;
        boolean hasReferenceValue = false;
        for (String method : methods) {
            Integer entry = candidates.get(method);
            if (method.equals(ENTRIES_RELATIVE_TO)) {
                referenceValue = entry;
                hasReferenceValue = true;
            }
            bestValue = Math.min(bestValue, entry == null ? bestValue : entry);
            if (!hasReferenceValue) {
                referenceValue = bestValue;
            }
        }
        //specify quality of everyone relative to reference value
        for (String method : methods) {
            Integer value = candidates.get(method);
            if (value == null) {
                continue;
            }
            method2best.get(method).add(value == bestValue ? 1 : 0);
            //we treat reference value == 0 as reference value == 1 to avoid a relative value of infinity
            method2relativeQuality.get(method).add(
                    value == 0 ? 1.0 : referenceValue == 0 ? value : (double) value / (double) referenceValue);
        }
    }

    private static void sortMethods(List<String> methods) {
        for (int i = SORT_ENTRIES_ORDER.length - 1; i >= 0; i--) {
            //remove and append first
            if (methods.contains(SORT_ENTRIES_ORDER[i])) {
                methods.remove(SORT_ENTRIES_ORDER[i]);
                methods.add(0, SORT_ENTRIES_ORDER[i]);
            }
        }
    }

    private static void tableHeadLineOutput(List<String> methods) {
        System.out.print("\\multicolumn{1}{c|}{}");
        String numberColumns = "" + (IGNORE_SD ? 2 : 3);
        String headLineSD = IGNORE_SD ? "" : " & sd";
        for (String method : methods) {
            System.out.print(" & \\multicolumn{" + numberColumns + "}{c}{" + string(method) + "}");
        }
        System.out.println(" \\\\");
        System.out.println(" & mean" + headLineSD + " & best & mean" + headLineSD
                + " & best & mean" + headLineSD + " & best \\\\");
        System.out.println("\\hline");
    }

    private static void tableTextOutput(String testCase, List<String> methods,
                                        Map<String, NumberDistribution<Double>> method2relativeQuality,
                                        Map<String, NumberDistribution<Integer>> method2best) {
        sortMethods(methods);
        System.out.print(string(testCase));
        //find best method to make it bold face
        int bestMethodIndex = -1;
        double valueBest = 0;
        for (int i = 0; i < methods.size(); i++) {
            String method = methods.get(i);
            double valueMethod = method2best.get(method).get(StatisticParameter.MEAN);
            if (valueMethod > valueBest) {
                bestMethodIndex = i;
                valueBest = valueMethod;
            }
        }
        for (int i = 0; i < methods.size(); i++) {
            String method = methods.get(i);
            NumberDistribution<Double> relativeQuality = method2relativeQuality.get(method);
            System.out.print(" & " + formatMean(relativeQuality.get(StatisticParameter.MEAN), method));
            if (!IGNORE_SD) {
                System.out.print(" & " + formatSD(relativeQuality.get(StatisticParameter.STANDARD_DEVIATION)));
            }
            System.out.print(" & ");
            if (bestMethodIndex == i) {
                System.out.print("\\textbf{");
            }
            System.out.print(formatPercent(method2best.get(method).get(StatisticParameter.MEAN))
                    + PERCENT_SYMBOL);
            if (bestMethodIndex == i) {
                System.out.print("}");
            }
        }
        System.out.println(" \\\\");
    }

    private static void regularTextOutput(String testCase, int numberOfEvaluatedFiles, List<String> methods,
                                          Map<String, NumberDistribution<Double>> method2relativeQuality,
                                          Map<String, NumberDistribution<Integer>> method2best) {
        sortMethods(methods);
        System.out.println();
        System.out.println("---" + testCase + "---");
        System.out.println("evaluated " + numberOfEvaluatedFiles + " files");
        System.out.println();
        for (String method : methods) {
            NumberDistribution<Double> relativeQuality = method2relativeQuality.get(method);
            System.out.println(string(method)
                    + ": mean " + formatMean(relativeQuality.get(StatisticParameter.MEAN), method));
            if (!IGNORE_SD) {
                System.out.println(string(method)
                        + ": sd " + formatSD(relativeQuality.get(StatisticParameter.STANDARD_DEVIATION)));
            }
            System.out.println(string(method)
                    + ": best in % " + formatPercent(method2best.get(method).get(StatisticParameter.MEAN)));
        }
        System.out.println();
    }

    private static void regularTextOutputTime(List<String> methods,
                                              Map<String, NumberDistribution<Integer>> method2absoluteTime) {
        sortMethods(methods);
        System.out.println();
        System.out.println("---analyzing absolute time---");
        System.out.println();
        for (String method : methods) {
            System.out.println();
            System.out.println(string(method));
            System.out.println("min: " + method2absoluteTime.get(method).get(StatisticParameter.MIN) + " ms");
            System.out.println("max: " + method2absoluteTime.get(method).get(StatisticParameter.MAX) + " ms");
            System.out.println("mean: " + method2absoluteTime.get(method).get(StatisticParameter.MEAN) + " ms");
            System.out.println("median: " + method2absoluteTime.get(method).get(StatisticParameter.MEDIAN) + " ms");
            System.out.println("sd: " + method2absoluteTime.get(method).get(StatisticParameter.STANDARD_DEVIATION) +
                    " ms");
        }
        System.out.println();
        System.out.println();
    }

    private static String formatMean(double number, String method) {
        String meanAsText = method.equals(ENTRIES_RELATIVE_TO) ?
                OUTPUT_FORMAT_MEAN_RELATIVE_TO.format(number) : OUTPUT_FORMAT_MEAN.format(number);
        if (meanAsText.startsWith(".")) {
            meanAsText = INSTEAD_OF_LEADING_ZERO + meanAsText;
        }
        return meanAsText;
    }

    private static String formatSD(double number) {
        String sdAsText = OUTPUT_FORMAT_SD.format(number);
        if (sdAsText.startsWith(".")) {
            sdAsText = INSTEAD_OF_LEADING_ZERO + sdAsText;
        }
        return sdAsText;
    }

    private static String formatPercent(double number) {
        return OUTPUT_PERCENT_FORMAT.format(number * 100.0);
    }

    private static String string(String s) {
        if (KNOWN_NAMES.containsKey(s)) {
            return KNOWN_NAMES.get(s);
        }
        return s;
    }

    private static String withoutWidthOrHeight(String s) {
        if (s.endsWith("-height")) {
            return s.substring(0, s.length() - "-height".length());
        }
        if (s.endsWith("-width")) {
            return s.substring(0, s.length() - "-width".length());
        }
        return s;
    }

    private static Map<String, NumberDistribution<Integer>> readFile (String filePath, String testCase) {
        //if it contains a negative value we ignore this file
        boolean containsNegative = false;
        Map<String, NumberDistribution<Integer>> result = new LinkedHashMap<>();
        Map<Integer, String> index2method = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> lines.add(s));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        int i = 0;
        for (String s : lines.remove(0).split(";")) {
            index2method.put(i++, s);
            if (!ignore(s, testCase)) {
                result.put(s, new NumberDistribution<>());
            }
        }
        for (String s : lines) {
            String[] values = s.split(";");
            int ii = 0;
            for (String value : values) {
                String method = index2method.get(ii++);
                if (!ignore(method, testCase)) {
                    int intValue;
                    try {
                        intValue = Integer.parseInt(value);
                    }
                    catch (NumberFormatException e) {
                        intValue = (int) Double.parseDouble(value);
                    }
                    if (intValue < 0) {
                        containsNegative = true;
                    }
                    result.get(method).add(intValue);
                }
            }
        }
        //we must withdraw it
        if (containsNegative) {
            return null;
        }
        return result;
    }

    private static boolean ignore(String method, String testCase) {
        return (testCase.equals("nodn") && method.equals("kieler")) || constainsIgnoringString(method);
    }

    private static boolean constainsIgnoringString(String s) {
        for (String ignore : IGNORE_FIELDS_CONTAINING_STRING) {
            if (s.contains(ignore)) {
                return true;
            }
        }
        return false;
    }
}
