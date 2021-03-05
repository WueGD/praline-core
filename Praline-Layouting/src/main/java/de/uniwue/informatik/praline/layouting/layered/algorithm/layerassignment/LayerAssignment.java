package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

import de.uniwue.informatik.praline.datastructure.graphs.Vertex;

import java.util.Map;

public interface LayerAssignment {
    Map<Vertex, Integer> assignLayers();
}
