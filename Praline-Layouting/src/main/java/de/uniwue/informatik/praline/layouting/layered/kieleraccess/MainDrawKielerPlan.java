package de.uniwue.informatik.praline.layouting.layered.kieleraccess;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting.DirectionMethod;

import java.io.IOException;

public class MainDrawKielerPlan {

    public static final String JSON_PATH =
//            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-1dda4e2a-ae64-4e76-916a-822c4e838c41.json";
            "Praline-Layouting/data/lc-praline-package-2020-05-18/lc-praline-908ca3a2-f1c3-42d2-a87a-4994bb2dee61.json";
//            "Praline-Layouting/data/generated_2020-08-20_04-42-39/praline-pseudo-plan-c656947dfd441fd3.json";
//            "Praline-Layouting/data/example-very-small/praline-a0b0b5a2-2c23-43b0-bb87-4ddeb34d5a02.json";
//            "Praline-Layouting/data/example-pseudo-plans/praline-pseudo-plan-0a94e4bf6d729042.json";
//            "Praline-Layouting/data/praline-package-2020-05-18/praline-0488185b-18b4-4780-a6c8-1d9ece91252e.json";
//            "Praline-Layouting/data/example-pseudo-plans/praline-pseudo-plan-0f90e022f10bae3f.json";

    public static final String SVG_TARGET_PATH = "Praline-Layouting/results/testKIELER.svg";

    public static void main(String[] args) throws IOException {
        //test this class here
        Graph graph = Serialization.read(JSON_PATH, Graph.class); //creator.createTestGraph();
        System.out.println("Graph read");

        KielerLayouter kielerLayouter = new KielerLayouter(graph, DirectionMethod.FORCE, 1);

        kielerLayouter.computeLayout();
        Graph resultGraph = kielerLayouter.getGraph();
        System.out.println("Number of crossings = " + kielerLayouter.getNumberOfCrossings());

        kielerLayouter.generateSVG(SVG_TARGET_PATH);

        System.out.println("KIELER test done successfully");
    }
}
