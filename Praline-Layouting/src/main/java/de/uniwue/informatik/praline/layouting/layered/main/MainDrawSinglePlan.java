package de.uniwue.informatik.praline.layouting.layered.main;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.io.input.processdata.EdgeLabelStyle;
import de.uniwue.informatik.praline.io.input.processdata.ProcessDataConverter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.cyclebreaking.CycleBreakingMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment.LayerAssignmentMethod;
import de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement.AlignmentParameters;
import de.uniwue.informatik.praline.layouting.layered.main.util.BendsCounting;
import de.uniwue.informatik.praline.layouting.layered.main.util.CrossingsCounting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainDrawSinglePlan {

    private static final String SOURCE_PATH =
//            "Praline-Layouting/data/example-very-small/praline-a0b0b5a2-2c23-43b0-bb87-4ddeb34d5a02.json";
//            "Praline-Layouting/data/example-very-small/praline-pseudo-plan-0e59d04df679e020.json";
            "Praline-Layouting/data/example-cgta/diagram_0058-praline.json";
//            "Praline-Layouting/data/example-cgta/praline-pseudo-plan-4b40acee3123f5b8.json";
//            "Praline-Layouting/data/example-cgta/praline-pseudo-plan-57b438bed9d27bc8.json";
//            "Praline-Layouting/data/example-cgta/praline-pseudo-plan-29891a903037261c.json";
//            "MINIMAL_RUNNING_EXAMPLE";

    private static final String PATH_RESULTS = "Praline-Layouting/results";
    private static final String TARGET_FILE_NAME = "singleTest.svg";

    private static final boolean CHECK_COMPLETENESS_OF_GRAPH = true;

    private static final CycleBreakingMethod CYCLE_BREAKING_METHOD = CycleBreakingMethod.IGNORE;

    private static final DirectionMethod DIRECTION_METHOD = DirectionMethod.FORCE;

    private static final LayerAssignmentMethod LAYER_ASSIGNMENT_METHOD = LayerAssignmentMethod.NETWORK_SIMPLEX;

    private static final CrossingMinimizationMethod CROSSING_MINIMIZATION_METHOD = CrossingMinimizationMethod.PORTS;

    private static final AlignmentParameters.Method ALIGNMENT_METHOD = AlignmentParameters.Method.FIRST_COMES;

    private static final AlignmentParameters.Preference ALIGNMENT_PREFERENCE = AlignmentParameters.Preference.LONG_EDGE;

    private static final int NUMBER_OF_REPETITIONS_PER_GRAPH = 1; //5;

    private static final int NUMBER_OF_FORCE_DIRECTED_ITERATIONS = 1; //10;

    private static final int NUMBER_OF_CROSSING_REDUCTION_ITERATIONS = 1; //3;

    /**
     *
     * @param args
     *      optional: you may add a path to a praline json file as first parameter. This graph will be drawn.
     */
    public static void main(String[] args) {
        SugiyamaLayouter bestRun = null;
        int fewestCrossings = Integer.MAX_VALUE;
        String pathToGraph = args.length > 0 && !args[0].isEmpty() ? args[0] : SOURCE_PATH;

        for (int i = 0; i < NUMBER_OF_REPETITIONS_PER_GRAPH; i++) {
            Graph graph = null;

            File file = new File(pathToGraph);
            try {
                graph = Serialization.read(file, Graph.class);

                //ProcessDataConverter converter = new ProcessDataConverter(EdgeLabelStyle.FREQUENCY, 1.0);
                //ProcessDataConverter converter = new ProcessDataConverter();
                //graph = converter.getGraphFromProcessData(pathToGraph);

                System.out.println("Read graph " + pathToGraph);
                System.out.println();

            } catch (IOException e) {
                graph = createMinimalRunningExampleForJournalArticle();

                System.out.println("Failed to read graph at " + pathToGraph + ". Path correct? Correct json format?");
                System.out.println("Used minimal running example for journal article instead.");
            }

            SugiyamaLayouter sugy = new SugiyamaLayouter(graph);

            /*
            sugy.getDrawingInformation().setLineShape(SVGLineShape.BEZIER2D);
            sugy.getDrawingInformation().setShowEdgeLabels(true);
            sugy.getDrawingInformation().setShowEdgeDirection(true);
             */

            sugy.computeLayout(CYCLE_BREAKING_METHOD, DIRECTION_METHOD, LAYER_ASSIGNMENT_METHOD, NUMBER_OF_FORCE_DIRECTED_ITERATIONS,
                    CROSSING_MINIMIZATION_METHOD, NUMBER_OF_CROSSING_REDUCTION_ITERATIONS, ALIGNMENT_METHOD, ALIGNMENT_PREFERENCE);

            int crossings = CrossingsCounting.countNumberOfCrossings(graph);
            System.out.println("Computed drawing with " + crossings + " crossings " +
                    "and " + BendsCounting.countNumberOfBends(graph) + " bends.");
            System.out.println();

            if (crossings < fewestCrossings) {
                bestRun = sugy;
                fewestCrossings = crossings;
            }

            if (i == NUMBER_OF_REPETITIONS_PER_GRAPH - 1) {
                new File(PATH_RESULTS).mkdirs();
                String targetPath = PATH_RESULTS + File.separator + TARGET_FILE_NAME;
                bestRun.drawResult(targetPath);

                if (i > 1) {
                    System.out.println();
                    System.out.println("Best run had " + fewestCrossings + " crossings -> to be saved as svg");
                }
                System.out.println("Created svg " + targetPath);
                System.out.println();
            }

            if (CHECK_COMPLETENESS_OF_GRAPH) {
                Graph sameGraphReloaded = null;
                try {
                    sameGraphReloaded = Serialization.read(file, Graph.class);
                } catch (IOException e) {
                    sameGraphReloaded = createMinimalRunningExampleForJournalArticle();
                }
                if (!graph.equalLabeling(sameGraphReloaded)) {
                    System.out.println("Warning! Drawn graph and input graph differ.");
                }
                else {
                    System.out.println("Checked: drawn graph contains the same objects as the input graph");
                }
            }
            System.out.println();
            System.out.println();
        }
    }

    private static Graph createMinimalRunningExampleForJournalArticle() {
        /*
        create vertices, ports, and port groups
         */
        List<Vertex> vertices = new ArrayList<>(5);
        List<List<Port>> ports = new ArrayList<>(5);
        List<List<PortComposition>> vertexPortCompositions = new ArrayList<>(5);
        List<String> vertexNames = Arrays.asList("Alexander", "Joachim", "Johannes", "Julian", "Walter");
        List<Integer> numberOfPorts = Arrays.asList(4, 4, 2, 1, 8);

        //create for each vertex its ports
        for (int i = 0; i < vertexNames.size(); i++) {
            ArrayList<Port> portList = new ArrayList<>(numberOfPorts.get(i));
            for (int j = 0; j < numberOfPorts.get(i); j++) {
                portList.add(new Port(null, new TextLabel((j+1) + "")));
            }
            ports.add(portList);
        }

        //create/save for each vertex its port compositions, i.e. port groups + ports
        for (int i = 0; i < ports.size(); i++) {
            List<Port> portList = ports.get(i);
            List<PortComposition> pcList = new ArrayList<>();
            // add port groups (and ports)
            //Alexander
            if (i == 0) {
                pcList.add(portList.get(0));
                pcList.add(new PortGroup(Arrays.asList(portList.get(1), portList.get(2), portList.get(3))));
            }
            //Joachim
            else if (i == 1) {
                pcList.add(new PortGroup(Arrays.asList(portList.get(0), portList.get(1)), false));
                pcList.add(new PortGroup(Arrays.asList(portList.get(2), portList.get(3)), false));
            }
            //Johannes
            else if (i == 2) {
                pcList.addAll(portList);
            }
            //Julian
            else if (i == 3) {
                pcList.addAll(portList);
            }
            //Walter
            else if (i == 4) {
                PortGroup portGroup12 = new PortGroup(Arrays.asList(portList.get(0), portList.get(1)), false);
                PortGroup portGroup56 = new PortGroup(Arrays.asList(portList.get(4), portList.get(5)), false);
                PortGroup portGroup78 = new PortGroup(Arrays.asList(portList.get(6), portList.get(7)), false);
                pcList.add(new PortGroup(Arrays.asList(portGroup12, portList.get(2), portList.get(3)), false));
                pcList.add(new PortGroup(Arrays.asList(portGroup56, portGroup78), false));
            }
            vertexPortCompositions.add(pcList);
        }

        //create each vertex
        for (int i = 0; i < vertexNames.size(); i++) {
            vertices.add(new Vertex(vertexPortCompositions.get(i), new TextLabel(vertexNames.get(i))));
        }

        /*
        create port pairings via dummy vertex groups
         */
        List<VertexGroup> dummyVertexGroups = new ArrayList<>(2);
        // 1 port pairing of Johannes
        dummyVertexGroups.add(new VertexGroup(Collections.singleton(vertices.get(2)), null, null,
                Collections.singleton(new PortPairing(ports.get(2).get(0), ports.get(2).get(1))), null, null, null,
                false, null));
        // 2 port pairings of Walter
        dummyVertexGroups.add(new VertexGroup(Collections.singleton(vertices.get(4)), null, null,
                Arrays.asList(new PortPairing(ports.get(4).get(1), ports.get(4).get(5)),
                        new PortPairing(ports.get(4).get(2), ports.get(4).get(6))), null, null, null, false, null));

        /*
        create edges
         */
        List<Edge> edges = Arrays.asList(
                //edges adjacent to Joachim (vertex index 1)
                new Edge(Arrays.asList(ports.get(1).get(1), ports.get(4).get(2))), // 2 to Walter.3
                new Edge(Arrays.asList(ports.get(1).get(2), ports.get(0).get(0))), // 3 to Alexander.1
                new Edge(Arrays.asList(ports.get(1).get(3), ports.get(4).get(6))), // 4 to Walter.7
                //edges adjacent to Walter (vertex index 4)
                new Edge(Arrays.asList(ports.get(4).get(0), ports.get(0).get(2))), // 1 to Alexander.3
                new Edge(Arrays.asList(ports.get(4).get(1), ports.get(2).get(1))), // 2 to Johannes.2
                new Edge(Arrays.asList(ports.get(4).get(3), ports.get(3).get(0))), // 4 to Julian.1
                new Edge(Arrays.asList(ports.get(4).get(4), ports.get(0).get(1))), // 5 to Alexander.2
                new Edge(Arrays.asList(ports.get(4).get(5), ports.get(0).get(3))), // 6 to Alexander.4
                new Edge(Arrays.asList(ports.get(4).get(7), ports.get(2).get(0)))  // 8 to Johannes.1
        );

        /*
        create graph
         */
        return new Graph(vertices, dummyVertexGroups, edges, null);
    }
}