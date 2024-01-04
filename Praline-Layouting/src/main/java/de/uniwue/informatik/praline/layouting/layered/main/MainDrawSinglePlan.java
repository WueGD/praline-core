package de.uniwue.informatik.praline.layouting.layered.main;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;
import de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction.CrossingMinimizationMethod;
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
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-1dda4e2a-ae64-4e76-916a-822c4e838c41.json";
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-5c5becad-d634-4081-b7c1-8a652fc6d023.json";
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-1f8afa02-a5a7-4646-abe1-83d91173cff4.json";
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-9c4d6586-fff2-4e06-bcbf-bf0922467654.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-0488185b-18b4-4780-a6c8-1d9ece91252e.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-efde956f-b3ea-43da-b736-35c65463525a.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-d4cbdce2-3a35-4fc1-9ed8-69e196371ee5.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-7c84fecd-8d2c-4f71-b95e-c496c68b8109.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-d5311cb8-84d5-45e6-afcb-7a28c4451b89.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-7ecbc1c4-3458-4769-8f58-e52e8fa4a5b8.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-06001ee4-a1ee-4d84-85ff-93792f56e5c7.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-a01bd68a-b853-480a-b2bc-58851f52c673.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-d4cbdce2-3a35-4fc1-9ed8-69e196371ee5.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-6fc3a807-fb4f-4108-a732-e91efc0872c7.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-a18320ab-dfe2-4497-a62b-53002fb45baa.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0000-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0001-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0002-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0003-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0004-praline.json"; //1 component
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0005-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0006-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0007-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0008-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0009-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0010-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0011-praline.json"; //1 component
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0019-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0024-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0282-praline.json";
            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0414-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0434-praline.json";
//            "Praline-Layouting/data/praline-readable-2020-09-04/diagram_0462-praline.json"; //1 component
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0001-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0002-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0011-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0140-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0107-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0109-praline.json";
//            "Praline-Layouting/data/denkbares_08_06_2021/praline/diagram_0104-praline.json";
//            "Praline-Layouting/data/node-size-test.json";
//            //small, but many vertex groups
//            //several loops
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-bc0a6948-ff41-4c1f-9b98-c2042b575adb.json";
//            //not clear if several connectors? device connectors? several loops?
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-491452a2-600e-4482-9981-8f2204e77ab8.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-8e12ddec-870d-49ac-bd12-f67300df8166.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-58f687b4-cce6-4938-8e08-a5d0155790da.json";
//            "Praline-Layouting/data/generated_2020-06-04_18-39-49/praline-pseudo-plan-2dfedde45c413b8f.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-a7118c7293f015c8.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-c6a79cfbd489d5fa.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-5b31f9595a8f60aa.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-d50158b62a468d14.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-9b97204a7ea50e02.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-41f94a913b9edf8d.json";
//            "Praline-Layouting/data/example-very-small/praline-a0b0b5a2-2c23-43b0-bb87-4ddeb34d5a02.json";
//            "Praline-Layouting/data/example-very-small/praline-pseudo-plan-0e59d04df679e020.json";
//            "Praline-Layouting/data/fabian-problemgraphen/send-johannes.json";
//            "Praline-Layouting/data/fabian-problemgraphen/send-johannes2.json";
//            "Praline-Layouting/data/fabian-04.05.2021/send-johannes2.json";
//            "MINIMAL_RUNNING_EXAMPLE";

    private static final String PATH_RESULTS = "Praline-Layouting/results";
    private static final String TARGET_FILE_NAME = "singleTest.svg";

    private static final boolean CHECK_COMPLETENESS_OF_GRAPH = true;

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
        String pathToGraph = args.length > 0 && args[0].length() > 0 ? args[0] : SOURCE_PATH;

        for (int i = 0; i < NUMBER_OF_REPETITIONS_PER_GRAPH; i++) {
            Graph graph = null;


            File file = new File(pathToGraph);
            try {
                graph = Serialization.read(file, Graph.class);

                System.out.println("Read graph " + pathToGraph);
                System.out.println();

            } catch (IOException e) {
                graph = createMinimalRunningExampleForJournalArticle();

                System.out.println("Failed to read graph at " + pathToGraph + ". Path correct? Correct json format?");
                System.out.println("Used minimal running example for journal article instead.");
            }

            SugiyamaLayouter sugy = new SugiyamaLayouter(graph);

            sugy.computeLayout(DIRECTION_METHOD, LAYER_ASSIGNMENT_METHOD, NUMBER_OF_FORCE_DIRECTED_ITERATIONS,
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