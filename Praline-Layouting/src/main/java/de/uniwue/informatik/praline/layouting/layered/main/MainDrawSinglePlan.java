package de.uniwue.informatik.praline.layouting.layered.main;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.Sugiyama;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.main.util.BendsCounting;
import de.uniwue.informatik.praline.layouting.layered.main.util.CrossingsCounting;

import java.io.File;
import java.io.IOException;

public class MainDrawSinglePlan {

    private static final String SOURCE_PATH =
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-1dda4e2a-ae64-4e76-916a-822c4e838c41.json";
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-5c5becad-d634-4081-b7c1-8a652fc6d023.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-0488185b-18b4-4780-a6c8-1d9ece91252e.json";
            "Praline-Layouting/data/praline-package-2020-05-18/praline-06001ee4-a1ee-4d84-85ff-93792f56e5c7.json";
//            "Praline-Layouting/data/generated_2020-06-04_18-39-49/praline-pseudo-plan-2dfedde45c413b8f.json";
//            "Praline-Layouting/data/example-very-small/praline-a0b0b5a2-2c23-43b0-bb87-4ddeb34d5a02.json";
//            "Praline-Layouting/data/example-very-small/praline-pseudo-plan-0e59d04df679e020.json";

    private static final String TARGET_PATH =
            "Praline-Layouting/results/singleTest.svg";

    public static void main(String[] args) {

        File file = new File(SOURCE_PATH);
        Graph graph = null;
        try {
            graph = Serialization.read(file, Graph.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Read graph " + SOURCE_PATH);
        System.out.println();

        Sugiyama sugy = new Sugiyama(graph);

        sugy.construct();
        sugy.assignDirections(DirectionMethod.BFS);
        sugy.assignLayers();
        sugy.createDummyNodes();
        sugy.crossingMinimization(CrossingMinimizationMethod.PORTS, 1);
        sugy.nodePositioning();
        sugy.edgeRouting();
        sugy.prepareDrawing();

        System.out.println("Computed drawing with " + CrossingsCounting.countNumberOfCrossings(graph) + " crossings " +
                "and " + BendsCounting.countNumberOfBends(graph) + " bends.");
        System.out.println();

        sugy.drawResult(TARGET_PATH);

        System.out.println("Created svg " + TARGET_PATH);
    }
}