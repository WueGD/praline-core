package de.uniwue.informatik.praline.layouting.layered.main;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.Sugiyama;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;

import java.io.File;
import java.io.IOException;

public class SingleTest {

    private static final String SOURCE_PATH = "testData/forSingleTest/some.json";

    public static void main(String[] args) {

        File file = new File(SOURCE_PATH);
        Graph graph = null;
        try {
            graph = Serialization.read(file, Graph.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sugiyama sugy = new Sugiyama(graph);

        sugy.construct();
        sugy.assignDirections(DirectionMethod.FORCE);
        sugy.assignLayers();
        sugy.createDummyNodes();
        sugy.crossingMinimization(CrossingMinimizationMethod.PORTS, 100);
        sugy.nodePositioning();
        sugy.drawResult("singleTest1.svg");
        sugy.edgeRouting();
        sugy.drawResult("singleTest2.svg");
        sugy.prepareDrawing();
        sugy.drawResult("singleTest3.svg");
    }
}