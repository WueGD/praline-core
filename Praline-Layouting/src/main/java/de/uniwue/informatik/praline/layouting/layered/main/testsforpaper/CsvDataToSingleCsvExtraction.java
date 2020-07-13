package de.uniwue.informatik.praline.layouting.layered.main.testsforpaper;

import de.uniwue.informatik.praline.layouting.layered.main.util.NumberDistribution;
import de.uniwue.informatik.praline.layouting.layered.main.util.StatisticParameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class CsvDataToSingleCsvExtraction {

    private static final String DATA_PATH =
            "Praline-Layouting/results/" +
//                    "paper-all-tests-2020-06-10_06-18-04";
                    "2020-07-09_18-07-13";

    private static final String[] DATA_DIRS =
            {
//                    "DA_lc-praline-package-2020-05-18",
//                    "DA_generated_2020-06-04_18-39-49",
                    "CM_lc-praline-package-2020-05-18",
//                    "CM_generated_2020-06-04_18-39-49"
                    "CM_generated_2020-07-09_05-07-43"
            };

    private static final String TARGET_PATH =
            "Praline-Layouting/results/2020-07-09_18-07-13/csv-summary";

    private static final String[] TARGET_FILE_PREFIX =
            {
                    "ncr",
                    "nbp"
            };
    private static final String[] TARGET_FILES =
            {
//                    "DA_lc-praline-package-2020-05-18",
//                    "DA_generated_2020-06-04_18-39-49",
                    "CM_lc-praline-package-2020-05-18",
//                    "CM_generated_2020-06-04_18-39-49"
                    "CM_generated_2020-07-09_05-07-43"
            };
    private static final String TARGET_FILE_SUFFIX = ".csv";

    private static final String[] CONSIDER_FILES =
            {
                    "noc",
                    "nob"
            }; //"nodn", "time", "space"

    private static final Map<String, String> KNOWN_NAMES = new LinkedHashMap<>() {
        {
            put("#vtcs", "vtcs");
            put("ports-noMove-noPlaceTurnings", "ports");
            put("mixed-noMove-noPlaceTurnings", "mixed");
            put("nodes-noMove-noPlaceTurnings", "nodes");
            put("ran", "rand");
        }
    };

    private static final List<String> IGNORE_FIELDS_CONTAINING_STRING =
            Arrays.asList("-move", "-placeTurnings", "-area", "-ratio");

    public static void main(String[] args) {
        File targetDir = new File(TARGET_PATH);
        targetDir.mkdirs();
        for (int i = 0; i < DATA_DIRS.length; i++) {
            for (int j = 0; j < CONSIDER_FILES.length; j++) {
                String sourcePath = DATA_PATH + File.separator + DATA_DIRS[i];
                String targetFilePath =
                        TARGET_PATH + File.separator + TARGET_FILE_PREFIX[j] + TARGET_FILES[i] + TARGET_FILE_SUFFIX;
                generateSummaryCsv(CONSIDER_FILES[j], sourcePath, targetFilePath);
            }
        }
    }

    private static void generateSummaryCsv(String considerFile, String sourcePath, String targetFilePath) {
        System.out.println("============================");
        System.out.println("Read " + sourcePath);

        //read data
        List<File> files = new LinkedList<>();
        File dir = new File(sourcePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().contains(considerFile)) {
                    files.add(child);
                }
            }
        }

        //create data structure
        List<Map<String, NumberDistribution<Integer>>> results = new ArrayList<>();
        for (File file : files) {
            Map<String, NumberDistribution<Integer>> result = readFile(file.getPath(), considerFile);
            if (result == null) {
                continue;
            }
            results.add(result);
        }


        List<String> methods = new ArrayList<>(results.get(0).keySet());

        //compute resulting values
        Map<String, List<Integer>> entries = new LinkedHashMap<>(methods.size());
        for (String method : methods) {
            entries.put(method, new NumberDistribution<>());
        }
        //for each graph do evaluation, add it to all methods
        for (Map<String, NumberDistribution<Integer>> result : results) {
            //first find best of each individual method
            for (String method : methods) {
                int entry = 0;
                if (result.get(method) != null) {
                    entry = (int) result.get(method).get(StatisticParameter.MIN);
                }
                entries.get(method).add(entry);
            }
        }

        try {
            StringBuilder csvFileContent = new StringBuilder();
            //csv top line
            for (int j = 0; j < methods.size(); j++) {
                    csvFileContent.append(string(methods.get(j)));
                if (j == methods.size() - 1){
                    csvFileContent.append("\n");
                }
                else {
                    csvFileContent.append("\t");
                }
            }
            //csv value lines
            for (int i = 0; i < entries.get(entries.keySet().iterator().next()).size(); i++) {
                for (int j = 0; j < methods.size(); j++) {
                        csvFileContent.append(entries.get(methods.get(j)).get(i));
                    if (j == methods.size() - 1){
                        csvFileContent.append("\n");
                    }
                    else {
                        csvFileContent.append("\t");
                    }
                }
            }
            //write csv file
            Files.write(Paths.get(targetFilePath), csvFileContent.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Wrote file " + targetFilePath);
        System.out.println("============================");
        System.out.println();
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

    private static String string(String s) {
        if (KNOWN_NAMES.containsKey(s)) {
            return KNOWN_NAMES.get(s);
        }
        return s;
    }
}