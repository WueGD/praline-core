package de.uniwue.informatik.praline.layouting.layered.algorithm.cyclebreaking;

import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.Vertex;
import de.uniwue.informatik.praline.layouting.layered.algorithm.SugiyamaLayouter;

import java.util.*;

public class CycleBreaker {

    public void greedyFas(SugiyamaLayouter sugy) {
        List<Vertex> vertices = new LinkedList<>(sugy.getGraph().getVertices());
        List<Edge> edges = new LinkedList<>(sugy.getGraph().getEdges());
        linearArrange(vertices, edges);
        assignEdgeDirectionsWithoutCycles(sugy, vertices, edges);
    }

    public void sortFas(SugiyamaLayouter sugy) {

        List<Vertex> vertices = new ArrayList<>(sugy.getGraph().getVertices());
        LinkedList<Vertex> verticesOrdered = new LinkedList<>();
        List<Edge> edges = new LinkedList<>(sugy.getGraph().getEdges());
        Map<Vertex, List<Vertex>> outgoingEdges = new HashMap<>();

        for (Vertex vertex : vertices)
            outgoingEdges.put(vertex, new ArrayList<>());
        for (Edge edge : edges)
            outgoingEdges.get(edge.getStartPort().getVertex()).add(edge.getEndPort().getVertex());
        linearArrange(vertices, edges);

        for (int i = 0; i < vertices.size(); i++) {
            int val = 0, min = 0, loc = i;
            for (int j = i - 1; j >= 0; j--) {
                if (outgoingEdges.get(vertices.get(i)).contains(vertices.get(j))) { val -= 1; }
                if (outgoingEdges.get(vertices.get(j)).contains(vertices.get(i))) { val += 1; }
                if (val <= min) {
                    min = val;
                    loc = j;
                }
            }
            // Insert vertex i at position loc
            verticesOrdered.add(loc, vertices.get(i));
        }
        if (vertices.size() != verticesOrdered.size()) {
            System.out.println("Size doesn't fit");
        }
        vertices = verticesOrdered;
        assignEdgeDirectionsWithoutCycles(sugy, vertices, edges);
    }

    public void pageRankFas(SugiyamaLayouter sugy) {

        List<Vertex> vertices = new LinkedList<>(sugy.getGraph().getVertices());
        int countVertices = vertices.size();
        List<Edge> edges = new LinkedList<>(sugy.getGraph().getEdges());
        Map<Vertex, List<Edge>> incomingEdges = new HashMap<>();
        Map<Vertex, List<Edge>> outgoingEdges = new HashMap<>();
        List<Edge> fas = new ArrayList<>();

        for (Vertex vertex : vertices) {
            incomingEdges.put(vertex, new ArrayList<>());
            outgoingEdges.put(vertex, new ArrayList<>());
        }

        // Den Knoten all ihre ein- und ausgehenden Kanten zuordnen
        for (Edge edge : edges) {
            incomingEdges.get(edge.getEndPort().getVertex()).add(edge);
            outgoingEdges.get(edge.getStartPort().getVertex()).add(edge);
        }

        while (hasCycles(vertices, edges, incomingEdges, outgoingEdges)) {
            // Create a line digraph L(si) with every edge of si as a node
            List<Vertex> lgSscVertices = new ArrayList<>();
            List<Edge> lgSscEdges = new ArrayList<>();
            Map<Vertex, Boolean> verticesVisited = new HashMap<>();
            Map<Vertex, Edge> correspondingVertices = new HashMap<>();

            for (Vertex vertex : vertices) {
                verticesVisited.put(vertex, false);
            }

            getLineGraph(verticesVisited, lgSscVertices, lgSscEdges, vertices.get(0),
                null, outgoingEdges, correspondingVertices);

            //PageRank(L(si))
            Map<Vertex, List<Edge>> lgIncomingEdges = new HashMap<>();
            Map<Vertex, List<Edge>> lgOutgoingEdges = new HashMap<>();

            for (Vertex vertex : lgSscVertices) {
                lgIncomingEdges.put(vertex, new ArrayList<>());
                lgOutgoingEdges.put(vertex, new ArrayList<>());
            }

            // Den Knoten all ihre ein- und ausgehenden Kanten zuordnen
            for (Edge edge : lgSscEdges) {
                lgIncomingEdges.get(edge.getEndPort().getVertex()).add(edge);
                lgOutgoingEdges.get(edge.getStartPort().getVertex()).add(edge);
            }
            Map<Vertex, Double> pageRanks = pageRank(lgSscVertices, lgSscEdges, 5,
                    lgIncomingEdges, lgOutgoingEdges);
            Vertex highestRankedVertex = lgSscVertices.get(0);
            for (Vertex vertex : pageRanks.keySet()) {
                if (pageRanks.get(vertex) > pageRanks.get(highestRankedVertex)) {
                    highestRankedVertex = vertex;
                }
            }
            Edge edgeToRemove = correspondingVertices.get(highestRankedVertex);
            fas.add(edgeToRemove);
            edges.remove(edgeToRemove);
            for (Vertex vertex : vertices) {
                incomingEdges.get(vertex).remove(edgeToRemove);
                outgoingEdges.get(vertex).remove(edgeToRemove);
            }
        }

        // Richtungen zuweisen: Richtige für übrige Kanten und umgekehrte für Kanten aus dem fas
        for (Edge edge : edges) {
            sugy.assignDirection(edge, edge.getStartPort().getVertex(), edge.getEndPort().getVertex());
        }
        //sugy.setSizeOfFas(fas.size());
        for (Edge edge : fas) {
            sugy.assignDirection(edge, edge.getEndPort().getVertex(), edge.getStartPort().getVertex());
        }
        if (vertices.size() != countVertices) { System.out.println("Size of vertices doesn't fit!"); }
        edges.addAll(fas);
    }

    public boolean hasCycles(List<Vertex> vertices, List<Edge> edges,
                             Map<Vertex, List<Edge>> incomingEdges,
                             Map<Vertex, List<Edge>> outgoingEdges) {

        List<Vertex> removedVertices = new ArrayList<>();
        List<Edge> removedEdges = new ArrayList<>();
        boolean sourcesPresent = true;
        boolean sinksPresent = true;

        while (sourcesPresent || sinksPresent) {

            Optional<Vertex> source = findSinkSourceOrMaxDegVertex(vertices, edges, CycleBreakingNodeType.SOURCE);
            if (source.isPresent()) {
                vertices.remove(source.get());
                removedVertices.add(source.get());
                for (Edge edge : outgoingEdges.get(source.get())) {
                    edges.remove(edge);
                    if (!removedEdges.contains(edge)) { removedEdges.add(edge); }
                }
            } else {
                sourcesPresent = false;
            }

            Optional<Vertex> sink = findSinkSourceOrMaxDegVertex(vertices, edges, CycleBreakingNodeType.SINK);
            if (sink.isPresent()) {
                vertices.remove(sink.get());
                removedVertices.add(sink.get());
                for (Edge edge : incomingEdges.get(sink.get())) {
                    edges.remove(edge);
                    if (!removedEdges.contains(edge)) { removedEdges.add(edge); }
                }
            } else {
                sinksPresent = false;
            }
        }

        boolean edgesLeft = !edges.isEmpty();
        edges.addAll(removedEdges);
        vertices.addAll(removedVertices);
        return edgesLeft;
    }

    public Map<Vertex, Double> pageRank(List<Vertex> vertices, List<Edge> edges, int iterations,
                                        Map<Vertex, List<Edge>> incomingEdges,
                                        Map<Vertex, List<Edge>> outgoingEdges) {
        Map<Vertex, Double> pageRanks = new HashMap<>();

        // Allen Knoten initial den PageRank 1/|V| zuweisen
        for (Vertex vertex : vertices) {
            pageRanks.put(vertex, (double) 1/vertices.size());
        }

        // Für jeden Knoten den Pagerank durch einen neuen ersetzen
        for (int i = 0; i < iterations; i++) {
            for (Vertex vertex : vertices) {
                double pageRankOld = pageRanks.get(vertex);
                double pageRankNew = 0;

                // Alle Knoten durchlaufen, die eine Kante zum aktuellen Knoten haben
                // Deren ausgehende Kanten (#) durch den alten PR dividieren und zum neuen PR addieren
                for (Edge inEdge : incomingEdges.get(vertex)) {
                    Vertex inVertex = inEdge.getStartPort().getVertex();
                    int inVertexSize = outgoingEdges.get(inVertex).size();
                    pageRankNew += pageRankOld / inVertexSize;
                }
                pageRanks.replace(vertex, pageRankNew);
            }
        }
        return pageRanks;
    }

    public void getLineGraph(Map<Vertex, Boolean> verticesVisited,
                             List<Vertex> lgVertices, List<Edge> lgEdges,
                             Vertex vertex, Vertex prevVertex,
                             Map<Vertex, List<Edge>> outgoingEdges,
                             Map<Vertex, Edge> correspondingVertices) {

        verticesVisited.replace(vertex, true);

        for (Edge edge : outgoingEdges.get(vertex)) {
            Vertex z = new Vertex();
            correspondingVertices.put(z, edge);
            lgVertices.add(z);
            if (prevVertex != null) {
                Port inPort = new Port();
                Port outPort = new Port();
                Edge e = Edge.mkSimple(outPort, inPort, true);
                lgEdges.add(e);
                z.addPortComposition(inPort);
                prevVertex.addPortComposition(outPort);
            }

            Vertex u = edge.getEndPort().getVertex();
            if (!verticesVisited.get(u)) {
                getLineGraph(verticesVisited, lgVertices, lgEdges,
                        u, z, outgoingEdges, correspondingVertices);
            } else {
                for (Vertex k : lgVertices) {
                    if (k.getPorts().stream().anyMatch(p -> p.getVertex().equals(u))) {
                        Port inPort = new Port();
                        Port outPort = new Port();
                        Edge e = Edge.mkSimple(outPort, inPort, true);
                        lgEdges.add(e);
                        z.addPortComposition(inPort);
                        k.addPortComposition(outPort);
                    }
                }
            }
        }
    }


    public void assignEdgeDirectionsWithoutCycles(SugiyamaLayouter sugy, List<Vertex> vertices, List<Edge> edges) {
        // Weise den Kanten die richtigen Richtungen zu
        for (Edge edge : edges) {
            Vertex node0 = edge.getStartPort().getVertex();
            Vertex node1 = edge.getEndPort().getVertex();
            if (vertices.indexOf(node0) < vertices.indexOf(node1)) {
                sugy.assignDirection(edge, node0, node1);
            } else {
                sugy.assignDirection(edge, node1, node0);
            }
        }
    }

    public void linearArrange(List<Vertex> vertices, List<Edge> edges) {

        List<Edge> edgesToRemove = new ArrayList<>();
        List<Edge> removedEdges = new ArrayList<>();
        LinkedList<Vertex> s1 = new LinkedList<>();
        LinkedList<Vertex> s2 = new LinkedList<>();
        LinkedList<Vertex> s3 = new LinkedList<>();
        boolean sinksPresent = true;
        boolean sourcesPresent = true;

        // check for sinks in the graph
        while (sinksPresent) {
            Optional<Vertex> sink = findSinkSourceOrMaxDegVertex(vertices, edges, CycleBreakingNodeType.SINK);
            if (sink.isPresent()) {
                Vertex presentSink = sink.get();
                s3.addFirst(presentSink);
                vertices.remove(presentSink);

                // for the found sink, remove all incoming edges
                for (Edge edge : edges) {
                    if (edge.getEndPort().getVertex() == presentSink) {
                        edgesToRemove.add(edge);
                    }
                }
                for (Edge edge : edgesToRemove) {
                    edges.remove(edge);
                    removedEdges.add(edge);
                }
                edgesToRemove.clear();
            } else {
                sinksPresent = false;
            }
        }

        // Prüfe, ob der Graph sources hat
        while (sourcesPresent) {
            Optional<Vertex> source = findSinkSourceOrMaxDegVertex(vertices, edges, CycleBreakingNodeType.SOURCE);
            if (source.isPresent()) {
                Vertex presentSource = source.get();
                s1.add(presentSource);
                vertices.remove(presentSource);

                // Entferne alle ausgehenden Kanten der Quelle
                for (Edge edge : edges) {
                    if (edge.getStartPort().getVertex() == presentSource) {
                        edgesToRemove.add(edge);
                    }
                }
                for (Edge edge : edgesToRemove) {
                    edges.remove(edge);
                    removedEdges.add(edge);
                }
                edgesToRemove.clear();
            } else {
                sourcesPresent = false;
            }
        }

        while (!vertices.isEmpty()) {
            // find the vertex where the difference between indeg and outdeg is highest
            Optional<Vertex> maxDegVertex = findSinkSourceOrMaxDegVertex(vertices, edges, CycleBreakingNodeType.OTHER);
            if (maxDegVertex.isPresent()) {
                Vertex presentMaxDegVertex = maxDegVertex.get();
                s2.add(presentMaxDegVertex);
                vertices.remove(presentMaxDegVertex);

                // for this vertex, remove all adjacend edges
                for (Edge edge : edges) {
                    if (edge.getStartPort().getVertex() == presentMaxDegVertex
                            || edge.getEndPort().getVertex() == presentMaxDegVertex) {
                        edgesToRemove.add(edge);
                    }
                }
                for (Edge edge : edgesToRemove) {
                    edges.remove(edge);
                    removedEdges.add(edge);
                }
                edgesToRemove.clear();
            }
        }

        // Hänge die beiden geordneten Listen von Knoten aneinander
        //List<Vertex> verticesOrdered = new ArrayList<>();
        vertices.addAll(s1);
        vertices.addAll(s2);
        vertices.addAll(s3);

        //vertices = verticesOrdered;
        edges.addAll(removedEdges);
    }

    public Optional<Vertex> findSinkSourceOrMaxDegVertex(List<Vertex> vertices, List<Edge> edges, CycleBreakingNodeType type) {

        // Bestimme die Anzahl der ein- und ausgehenden Kanten für jeden Knoten
        Map<Vertex, Integer> inDeg = new HashMap<>();
        Map<Vertex, Integer> outDeg = new HashMap<>();
        List<Vertex> sinks = new ArrayList<>();
        List<Vertex> sources = new ArrayList<>();

        for (Edge edge : edges) {
            // Zähle eine eingehende Kante für den aktuellen Knoten
            Vertex inVertex = edge.getEndPort().getVertex();
            if (!inDeg.containsKey(inVertex)) {
                inDeg.put(inVertex, 1);
            } else {
                inDeg.replace(inVertex, inDeg.get(inVertex) + 1);
            }

            // Zähle eine ausgehende Kante für den aktuellen Knoten
            Vertex outVertex = edge.getStartPort().getVertex();
            if (!outDeg.containsKey(outVertex)) {
                outDeg.put(outVertex, 1);
            } else {
                outDeg.replace(outVertex, outDeg.get(outVertex) + 1);
            }
        }

        if (type.equals(CycleBreakingNodeType.SINK) || type.equals(CycleBreakingNodeType.SOURCE)) {
            // Unterteile alle Knoten in Quellen, Senken und andere
            for (Vertex vertex : vertices) {
                // Keine eingehenden Kanten: Knoten ist eine Quelle
                if (!inDeg.containsKey(vertex)) {
                    sources.add(vertex);
                } else if (!outDeg.containsKey(vertex)) {
                    // Keine ausgehenden Kanten: Knoten ist eine Senke
                    sinks.add(vertex);
                }
            }
        }

        // Es wurde nach einer Senke gefragt
        if (type.equals(CycleBreakingNodeType.SINK)) {
            if (!sinks.isEmpty()) {
                // Gib die erste Senke aus der Liste zurück
                return Optional.of(sinks.get(0));
            } else {
                return Optional.empty();
            }
        }

        // Es wurde nach einer Quelle gefragt
        if (type.equals(CycleBreakingNodeType.SOURCE)) {
            if (!sources.isEmpty()) {
                return Optional.of(sources.get(0));
            } else {
                return Optional.empty();
            }
        }

        // Es wurde nicht nach einer Quelle oder Senke gefragt: Behandle diese wieder als normale Knoten
        // Setze den Grad der Knoten ohne ein-/ ausgehende Kanten (reine Hygiene)
        for (Vertex vertex : vertices) {
            if (!inDeg.containsKey(vertex)) {
                inDeg.put(vertex, 0);
            }
            if (!outDeg.containsKey(vertex)) {
                outDeg.put(vertex, 0);
            }
        }

        // Suche den Knoten für den # ausgehende Kanten - # eingehende Kanten maximal ist
        Vertex maxVertex = vertices.get(0);
        for (Vertex vertex : vertices) {
            if (outDeg.get(vertex) - inDeg.get(vertex) > outDeg.get(maxVertex) - inDeg.get(maxVertex)) {
                maxVertex = vertex;
            }
        }

        return Optional.of(maxVertex);
    }

}