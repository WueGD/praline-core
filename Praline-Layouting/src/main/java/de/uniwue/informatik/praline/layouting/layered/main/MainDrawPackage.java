package de.uniwue.informatik.praline.layouting.layered.main;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.cyclebreaking.CycleBreakingMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.LayerAssignmentMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.AlignmentParameters;
import de.uniwue.informatik.praline.layouting.layered.main.util.CrossingsCounting;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MainDrawPackage {

    public static final String PATH_DATA_SET =
//            "Praline-Layouting/data/topology-zoo";
//            "Praline-Layouting/data/generated_2020-06-04_18-39-49";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39";
//            "Praline-Layouting/data/generated_2021-03-15_17-32-05";
//            "Praline-Layouting/data/lc-praline-package-2020-05-18";
//            "Praline-Layouting/data/praline-package-2020-05-18";
//            "Praline-Layouting/data/generated_2021-08-06_17-27-03"; //based on "praline-package-2020-05-18"
//            "Praline-Layouting/data/praline-readable-2020-09-04";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline";
//            "Praline-Layouting/data/generated_2021-08-07_15-24-08"; //based on "denkbares_08_06_2021/praline"
//            "Praline-Layouting/data/5plansOriginalPseudo";
            "Praline-Layouting/data/example-cgta";
//            "Praline-Layouting/data/example-very-small";


    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    public static final String PATH_RESULTS =
            "Praline-Layouting/results/all-svgs-" + DATE_FORMAT.format(new Date());

    private static final boolean CHECK_COMPLETENESS_OF_GRAPH = true;

    private static final CycleBreakingMethod CYCLE_BREAKING_METHOD = CycleBreakingMethod.IGNORE;

    private static final DirectionMethod DIRECTION_METHOD = DirectionMethod.FORCE;

    private static final LayerAssignmentMethod LAYER_ASSIGNMENT_METHOD = LayerAssignmentMethod.NETWORK_SIMPLEX;

    private static final CrossingMinimizationMethod CROSSING_MINIMIZATION_METHOD = CrossingMinimizationMethod.PORTS;

    private static final AlignmentParameters.Method ALIGNMENT_METHOD = AlignmentParameters.Method.FIRST_COMES;

    private static final AlignmentParameters.Preference ALIGNMENT_PREFERENCE = AlignmentParameters.Preference.LONG_EDGE;

    private static final int NUMBER_OF_REPETITIONS_PER_GRAPH = 10; //5;

    private static final int NUMBER_OF_FORCE_DIRECTED_ITERATIONS = 1; //10;

    private static final int NUMBER_OF_CROSSING_REDUCTION_ITERATIONS = 1; //3;

    private static final int NUMBER_OF_PARALLEL_THREADS = 8;


    private static int progressCounter = 0;
    private static int totalSteps;

    private static synchronized int progress() {
        return ++progressCounter;
    }

    /**
     *
     * @param args
     *      optional: you may add a path to a directory containing praline json files as first parameter. All the graphs
     *      contained there will be drawn.
     */
    public static void main(String[] args) throws InterruptedException {
        List<File> files = new ArrayList<>();
        String pathToGraph = args.length > 0 && args[0].length() > 0 ? args[0] : PATH_DATA_SET;

        File dir = new File(pathToGraph);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().endsWith(".json") &&
                        (!pathToGraph.endsWith("readable-2020-09-04") || child.getName().endsWith("-praline.json"))) {
                    files.add(child);
                }
            }
        }

        new File(PATH_RESULTS).mkdirs();

        List<Callable<String>> tasks = new ArrayList<>();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_PARALLEL_THREADS);
        int jj = 0;
        progressCounter = 0;
        totalSteps = files.size();
        for (File file : files) {
            int k = jj;
            tasks.add(() -> {
                try {
                    parallelio(file);
                }
                catch (Exception e) {
                    System.out.println("Exception has been thrown!");
                    e.printStackTrace();
                }
                return null;
            });
            jj++;
        }
        executor.invokeAll(tasks);
        executor.shutdown();
    }

    public static void parallelio (File file) throws IOException {
        int numberOfVertices = -1;
        int bestNumberOfCrossings = Integer.MAX_VALUE;
        SugiyamaLayouter bestRunSugy = null;

        for (int i = 0; i < NUMBER_OF_REPETITIONS_PER_GRAPH; i++) {
            Graph graph = Serialization.read(file, Graph.class);

            //ProcessDataConverter converter = new ProcessDataConverter(EdgeLabelStyle.FREQUENCY, 1.0);
            //ProcessDataConverter converter = new ProcessDataConverter();
            //graph = converter.getGraphFromProcessData(pathToGraph);

            numberOfVertices = graph.getVertices().size();

            SugiyamaLayouter sugy = new SugiyamaLayouter(graph);

            /*
            sugy.getDrawingInformation().setLineShape(SVGLineShape.BEZIER2D);
            sugy.getDrawingInformation().setShowEdgeLabels(true);
            sugy.getDrawingInformation().setShowEdgeDirection(true);
             */

            sugy.computeLayout(CYCLE_BREAKING_METHOD, DIRECTION_METHOD, LAYER_ASSIGNMENT_METHOD, NUMBER_OF_FORCE_DIRECTED_ITERATIONS,
                    CROSSING_MINIMIZATION_METHOD, NUMBER_OF_CROSSING_REDUCTION_ITERATIONS, ALIGNMENT_METHOD, ALIGNMENT_PREFERENCE);

            int numberOfCrossings = CrossingsCounting.countNumberOfCrossings(sugy.getGraph());

            if (bestNumberOfCrossings > numberOfCrossings) {
                bestNumberOfCrossings = numberOfCrossings;
                bestRunSugy = sugy;
            }
        }

        if (CHECK_COMPLETENESS_OF_GRAPH) {
            Graph sameGraphReloaded = null;
            try {
                sameGraphReloaded = Serialization.read(file, Graph.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!bestRunSugy.getGraph().equalLabeling(sameGraphReloaded)) {
                System.out.println("Warning! Drawn graph and input graph differ.");
            }
        }

        String filename = file.getName();
        filename = filename.substring(0, filename.length() - 5); //remove ".json"
        filename = "n" + numberOfVertices + "cr" + bestNumberOfCrossings + filename + ".svg";
        bestRunSugy.drawResult(PATH_RESULTS + File.separator + filename);
        System.out.println("Progress: " + progress() + "/" + totalSteps);
    }
}
