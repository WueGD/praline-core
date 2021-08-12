package de.uniwue.informatik.praline.layouting.layered.main.testsforpaper;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.datastructure.shapes.Rectangle;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.HandlingDeadEnds;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.LayerAssignmentMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.AlignmentParameters;
import de.uniwue.informatik.praline.layouting.layered.algorithm.util.Constants;
import de.uniwue.informatik.praline.layouting.layered.kieleraccess.KielerLayouter;
import de.uniwue.informatik.praline.layouting.layered.main.util.BendsCounting;
import de.uniwue.informatik.praline.layouting.layered.main.util.CrossingsCounting;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AllTests {

    public static final String PATH_DATA_SET = "Praline-Layouting/data";
    public static final String[] DATA_SETS =
            {
//                    "generated_2020-06-04_18-39-49",
//                    "generated_2020-08-20_04-42-39",
//                    "lc-praline-package-2020-05-18"
//                    "praline-package-2020-05-18"
//                    "praline-readable-2020-09-04"
                    "denkbares_08_06_2021/praline"
            };
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final String PATH_RESULTS =
            "Praline-Layouting/results" + File.separator + DATE_FORMAT.format(new Date()); //"results"




    private static final Test[] CURRENT_TESTS =
            {
//                    Test.DIRECTION_ASSIGNMENT_PHASE,
                    Test.CROSSING_MINIMIZATION_PHASE
            };

    private static final int NUMBER_OF_REPETITIONS_PER_GRAPH = 1; //5; //10; //50 //200

    private static final int NUMBER_OF_FORCE_DIRECTED_ITERATIONS = 1; //10;

    private static final int NUMBER_OF_CROSSING_REDUCTION_ITERATIONS = 1; //3; //10; //5; //50



    //the following values are taken for CROSSING_MINIMIZATION_PHASE

    private static final DirectionMethod DEFAULT_DIRECTION_METHOD = DirectionMethod.FORCE;

    private static final LayerAssignmentMethod DEFAULT_LAYER_ASSIGNMENT_METHOD = LayerAssignmentMethod.NETWORK_SIMPLEX;


    //the following values are taken for DIRECTION_ASSIGNMENT_PHASE

    private static final CrossingMinimizationMethod DEFAULT_CROSSING_MINIMIZATION_METHOD =
            CrossingMinimizationMethod.PORTS;

    private static final boolean DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE = true;

    private static final boolean DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX = true;

    private static final HandlingDeadEnds DEFAULT_HANDLING_DEAD_ENDS = HandlingDeadEnds.PREV_RELATIVE_POSITIONS;

    private static final AlignmentParameters.Method DEFAULT_ALIGNMENT_METHOD = AlignmentParameters.Method.FIRST_COMES;

    private static final AlignmentParameters.Preference DEFAULT_ALIGNMENT_PREFERENCE =
            AlignmentParameters.Preference.LONG_EDGE;






    private static final int NUMBER_OF_PARALLEL_THREADS = 8; //16

    private enum Test {
        DIRECTION_ASSIGNMENT_PHASE {
            @Override
            public String toString() {
                return "DA";
            }
        },
        CROSSING_MINIMIZATION_PHASE {
            @Override
            public String toString() {
                return "CM";
            }
        };

        public static List<String> getMethods(Test test) {
            switch (test) {
                case DIRECTION_ASSIGNMENT_PHASE:
                    ArrayList<String> daMethods = new ArrayList<>();
                    for (DirectionMethod dm : DirectionMethod.values()) {
                        for (LayerAssignmentMethod lam : LayerAssignmentMethod.values()) {
                            //we can use LayerAssignmentMethod.FD_POSITION only if DirectionMethod.FORCE is used
                            if (!lam.equals(LayerAssignmentMethod.FD_POSITION) || dm.equals(DirectionMethod.FORCE)) {
                                daMethods.add(dm + "-" + lam);
                            }
                        }
                    }
                    return daMethods;
                case CROSSING_MINIMIZATION_PHASE:
                    ArrayList<String> methods = new ArrayList<>();
                    String[] movePortsToTurningDummies = {"noMove", "move"}; //{"-noMove"};
                    String[] placeTurningDummiesCloseToVertex = {"noPlaceTurnings", "placeTurnings"}; //{"-noPlaceTurnings"};
                    for (CrossingMinimizationMethod cmm : CrossingMinimizationMethod.values()) {
                        for (String m : movePortsToTurningDummies) {
                            for (String ptd : placeTurningDummiesCloseToVertex) {
                                for (HandlingDeadEnds hde : HandlingDeadEnds.values()) {
                                    for (AlignmentParameters.Method alignMethod : AlignmentParameters.Method.values()) {
                                        for (AlignmentParameters.Preference alignPref : AlignmentParameters.Preference
                                                .values()) {
                                            methods.add(cmm + "-" + m + "-" + ptd + "-" + hde + "-" + alignMethod +
                                                            "-" + alignPref);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    methods.add("kieler");
                    return methods;
            }
            return null;
        }

        private static <E> String[] namesArray(E[] enumValues) {
            String[] names = new String[enumValues.length];
            for (int i = 0; i < enumValues.length; i++) {
                names[i] = enumValues[i].toString();
            }
            return names;
        }
    }

    private enum Criterion {
        NUMBER_OF_CROSSINGS {
            @Override
            public String toString() {
                return "noc";
            }
        },
        NUMBER_OF_BENDS {
            @Override
            public String toString() {
                return "nob";
            }
        },
        NUMBER_OF_DUMMY_VERTICES {
            @Override
            public String toString() {
                return "nodn";
            }
        },
        DRAWING_AREA {
            @Override
            public String toString() {
                return "space";
            }
        },
        CPU_TIME {
            @Override
            public String toString() {
                return "time";
            }
        }
    }

    private static ThreadMXBean mxBean;

    public static void main(String[] args) throws InterruptedException {


        int noi = NUMBER_OF_REPETITIONS_PER_GRAPH;

        mxBean = ManagementFactory.getThreadMXBean();

        List<Callable<String>> tasks = new ArrayList<>();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_PARALLEL_THREADS);
        progressCounter = 0;
        totalSteps = 0;
        for (String pathDataSet : DATA_SETS) {
            for (Test currentTest : CURRENT_TESTS) {

                String targetPath = PATH_RESULTS + File.separator + currentTest + "_" + pathDataSet;

                new File(targetPath).mkdirs();

                List<File> files = new ArrayList<>();

                File dir = new File(PATH_DATA_SET + File.separator + pathDataSet);
                File[] directoryListing = dir.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        if (child.getName().endsWith(".json") &&
                                (!PATH_DATA_SET.endsWith("readable-2020-09-04") || child.getName().endsWith("-praline.json"))) {
                            files.add(child);
                        }
                    }
                }

                totalSteps += files.size() * noi * Test.getMethods(currentTest).size();

                for (File file : files) {
                    tasks.add(() -> {
                        try {
                            parallelio(file, noi, currentTest, targetPath);
                        }
                        catch (Exception e) {
                            System.out.println("Exception has been thrown!");
                            e.printStackTrace();
                        }
                        return null;
                    });
                }
            }
        }
        executor.invokeAll(tasks);
        executor.shutdown();
    }


    private static int progressCounter = 0;
    private static int totalSteps;

    private static synchronized int progress() {
        return ++progressCounter;
    }

    public static void parallelio(File file, int noi, Test currentTest, String targetPath) throws IOException {

        Graph graph = null;

        //init our maps/lists for collection the data
        LinkedHashMap<Criterion, LinkedHashMap<String, ArrayList<Integer>>> criterion2method2values = new LinkedHashMap<>();
        LinkedHashMap<String, ArrayList<List<Double>>> spaceMethod2values = new LinkedHashMap<>();

        List<String> methods = Test.getMethods(currentTest);
        for (Criterion criterion : Criterion.values()) {
            LinkedHashMap<String, ArrayList<Integer>> method2values = new LinkedHashMap<>();
            if (!criterion.equals(Criterion.DRAWING_AREA)) {
                criterion2method2values.put(criterion, method2values);
            }
            //method is e.g. fd, bfs, ran or on the other side nodes, ports, kieler
            for (String method : methods) {
                if (criterion.equals(Criterion.DRAWING_AREA)) {
                    spaceMethod2values.put(method, new ArrayList<>());
                }
                else {
                    method2values.put(method, new ArrayList<>());
                }
            }
        }

        int numberVtcs = 0;
        for (int i = 0; i < noi ; i++) {
//            SugiyamaLayouter lastSugiy = null;

            //go through the methods each time in a different order s.t. the first method has no disadvantages in the
            // running time because of time-consuming swapinng/cleaning operations by the JVM and os in the background
            Collections.shuffle(methods, Constants.random);

            for (String method : methods) {
                System.out.println("Progress: " + progress() + "/" + totalSteps);
//                System.out.println(method);

                graph = Serialization.read(file, Graph.class);
                numberVtcs = graph.getVertices().size();

//                ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
                long startTime = mxBean.getThreadCpuTime(Thread.currentThread().getId());

                SugiyamaLayouter sugiy = new SugiyamaLayouter(graph);

                sugiy.construct();

                //commented out because somehow the graph gets blown up after every iteration...
                //easiest fix: just compute for each method its own directed graph... statistically that should
                // average out

//                if (lastSugiy != null && currentTest.equals(Test.CROSSING_MINIMIZATION_PHASE)) {
//                    sugiy.copyDirections(lastSugiy);
//                }
//                else {
                    DirectionMethod directionMethod = DEFAULT_DIRECTION_METHOD;
                    LayerAssignmentMethod layerAssignmentMethod = DEFAULT_LAYER_ASSIGNMENT_METHOD;
                    if (currentTest.equals(Test.DIRECTION_ASSIGNMENT_PHASE)) {
                        directionMethod = DirectionMethod.string2Enum(method);
                        layerAssignmentMethod = LayerAssignmentMethod.string2Enum(method);
                    }
//                    System.out.println("da_precomp: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));
                    sugiy.assignDirections(directionMethod, NUMBER_OF_FORCE_DIRECTED_ITERATIONS);

//                  System.out.println("da: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));

                    sugiy.assignLayers(layerAssignmentMethod, directionMethod);

//                    System.out.println("al: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));
//                }

                if (method.equals("kieler")) {
                    KielerLayouter kielerLayouter = new KielerLayouter(sugiy);

//                    System.out.println("kieler_init: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));
                    kielerLayouter.computeLayout();
                    //save number of dummy nodes
                    criterion2method2values.get(Criterion.NUMBER_OF_DUMMY_VERTICES).get(method).add(0); //for kieler
                    // we don't know and we also don't care so much about dummy vertices

//                    System.out.println("kieler_draw: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));
                }
                else {

//                    sugiy.createDummyNodes();

//                    System.out.println("cd: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));


                    if (currentTest.equals(Test.CROSSING_MINIMIZATION_PHASE)) {
                        sugiy.createDummyNodesAndDoCrossingMinimization(CrossingMinimizationMethod.string2Enum(method),
                                method.contains("-move"), method.contains("-placeTurnings"),
                                HandlingDeadEnds.string2Enum(method), NUMBER_OF_CROSSING_REDUCTION_ITERATIONS);
                    }
                    else {
                        sugiy.createDummyNodesAndDoCrossingMinimization(DEFAULT_CROSSING_MINIMIZATION_METHOD,
                                DEFAULT_MOVE_PORTS_ADJ_TO_TURNING_DUMMIES_TO_THE_OUTSIDE,
                                DEFAULT_PLACE_TURNING_DUMMIES_NEXT_TO_THEIR_VERTEX,
                                DEFAULT_HANDLING_DEAD_ENDS, NUMBER_OF_CROSSING_REDUCTION_ITERATIONS);
                    }

                    //save number of dummy nodes
                    criterion2method2values.get(Criterion.NUMBER_OF_DUMMY_VERTICES).get(method).add(sugiy.getNumberOfDummys());

//                    System.out.println("cm: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));

                    if (currentTest.equals(Test.CROSSING_MINIMIZATION_PHASE)) {
                        sugiy.nodePositioning(AlignmentParameters.Method.string2Enum(method),
                                AlignmentParameters.Preference.string2Enum(method));
                    }
                    else {
                        sugiy.nodePositioning(DEFAULT_ALIGNMENT_METHOD, DEFAULT_ALIGNMENT_PREFERENCE);
                    }

//                    System.out.println("np: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));

                    sugiy.edgeRouting();

//                    System.out.println("er: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));

                    sugiy.prepareDrawing();

//                    System.out.println("er: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));
                }

                long endTime = mxBean.getThreadCpuTime(Thread.currentThread().getId());

                //save number of crossings
                criterion2method2values.get(Criterion.NUMBER_OF_CROSSINGS).get(method).add(CrossingsCounting.countNumberOfCrossings(graph));

                //save cpu time in ms (we divide by a million to have milliseconds instead of nanoseconds)
                criterion2method2values.get(Criterion.CPU_TIME).get(method).add((int) ((endTime - startTime) / 1000000l));

                //save number of bends
                criterion2method2values.get(Criterion.NUMBER_OF_BENDS).get(method).add(BendsCounting.countNumberOfBends(graph));

                //save drawing area
                List<Double> drawingAreaStatistics = analyzeDrawingArea(graph);
                spaceMethod2values.get(method).add(drawingAreaStatistics);

//                lastSugiy = sugiy;
//                System.out.println("rest: " + ((double) (mxBean.getThreadCpuTime(Thread.currentThread().getId()) - startTime) / 1000000000.0));

            }
        }

        //save results
        for (Criterion criterion : Criterion.values()) {
            StringBuilder csvFileContent = new StringBuilder();
            //csv top line
            for (int j = 0; j < methods.size(); j++) {
                if (criterion.equals(Criterion.DRAWING_AREA)) {
                    List<String> subCategories = getNamesOfCategoriesForAnalyzeDrawingArea();
                    for (int jj = 0; jj < subCategories.size(); jj++) {
                        csvFileContent.append(methods.get(j) + "-" + subCategories.get(jj));
                        if (jj != subCategories.size() - 1) {
                            csvFileContent.append(";");
                        }
                    }
                }
                else {
                    csvFileContent.append(methods.get(j));
                }
                if (j == methods.size() - 1){
                    csvFileContent.append(";#vtcs\n");
                }
                else {
                    csvFileContent.append(";");
                }
            }
            //csv value lines
            LinkedHashMap<String, ArrayList<Integer>> method2values = criterion2method2values.get(criterion);
            for (int i = 0; i < noi; i++) {
                for (int j = 0; j < methods.size(); j++) {
                    if (criterion.equals(Criterion.DRAWING_AREA)) {
                        List<Double> subValues = spaceMethod2values.get(methods.get(j)).get(i);
                        for (int jj = 0; jj < subValues.size(); jj++) {
                            csvFileContent.append(subValues.get(jj));
                            if (jj != subValues.size() - 1) {
                                csvFileContent.append(";");
                            }
                        }
                    }
                    else {
                        csvFileContent.append(method2values.get(methods.get(j)).get(i));
                    }
                    if (j == methods.size() - 1){
                        csvFileContent.append(";" + numberVtcs + "\n");
                    }
                    else {
                        csvFileContent.append(";");
                    }
                }
            }
            //write csv file
            String filename = file.getName();
            filename = filename.substring(0, filename.length() - 5); //remove ".json" ending (last 5 chars)
            Files.write(Paths.get(targetPath + File.separator + criterion.toString() + "_" + filename + ".csv"),
                    csvFileContent.toString().getBytes());
        }
    }

    private static List<String> getNamesOfCategoriesForAnalyzeDrawingArea() {
        return Arrays.asList("area", "width", "height", "ratio");
    }

    private static List<Double> analyzeDrawingArea(Graph graph) {
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (Vertex node : graph.getVertices()) {
            Rectangle shape = (Rectangle) node.getShape();
            if (shape.getX() < minx) minx = shape.getX();
            if (shape.getY() < miny) miny = shape.getY();
            if (shape.getX() + shape.getWidth() > maxx) maxx = shape.getX() + shape.getWidth();
            if (shape.getY() + shape.getHeight() > maxy) maxy = shape.getY() + shape.getHeight();
        }

        double width = maxx - minx;
        double height = maxy - miny;
        List<Double> space = new ArrayList<>(4);
        space.add(width * height);
        space.add(width);
        space.add(height);
        space.add(width / height);
        return space;
    }
}
