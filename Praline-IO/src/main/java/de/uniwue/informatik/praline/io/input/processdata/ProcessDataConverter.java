package de.uniwue.informatik.praline.io.input.processdata;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for converting process data from a CSV file into a graph structure.
 * It reads the data from the CSV file, processes it, and creates a graph representation of the process.
 * The class provides methods for setting parameters such as the percentage of variants to consider
 * and the style of edge labels. It also provides methods for retrieving the processed data and
 * creating the graph.
 */
public class ProcessDataConverter {

    private List<Case> cases = new ArrayList<>();
    private List<Activity> processActivities = new ArrayList<>();
    private List<ProcessEdge> processEdges = new ArrayList<>();
    private double percentageOfVariants = 1.0;
    private EdgeLabelStyle edgeLabelStyle = EdgeLabelStyle.NONE;

    public ProcessDataConverter() {}

    /**
     * Constructor with parameters. Sets the edge label style and the percentage of variants.
     *
     * @param edgeLabelStyle The style of edge labels.
     * @param percentageOfVariants The percentage of variants to consider.
     */
    public ProcessDataConverter(EdgeLabelStyle edgeLabelStyle, double percentageOfVariants) {
        this.edgeLabelStyle = edgeLabelStyle;
        this.percentageOfVariants = percentageOfVariants;
    }

    /**
     * Converts process data from a CSV file into a graph structure.
     *
     * @param pathToFile The path to the CSV file containing the process data.
     * @return The graph representation of the process.
     */
    public Graph getGraphFromProcessData(String pathToFile) throws IOException {
        try {
            readCasesFromCsvFile(pathToFile);
            addProcessNodesAndEdges(percentageOfVariants);
            return createProcessGraph();
        } catch (Exception e) {
            throw new IOException("Error reading CSV file: " + e.getMessage());
        }
    }

    /**
     * This function creates a process graph based on the activities and edges extracted from the process data.
     *
     * The process graph is created by following these steps:
     * 1. Create vertices for each activity, with their labels indicating the activity name and occurrence count.
     * 2. Create ports for each vertex, based on the number of outgoing edges from the corresponding activity.
     * 3. Create edges between vertices, based on the outgoing edges from the corresponding activities.
     * 4. Set the labels of the edges based on the edge label style (frequency or time).
     *
     * @return The process graph created based on the activities and edges extracted from the process data.
     */
    private Graph createProcessGraph() {
        //create vertices, ports, and port groups
        List<String> vertexNames = getProcessActivityNames();
        List<Integer> vertexOccurance = getProcessActivityOccurence();
        List<Vertex> vertices = new ArrayList<>(vertexNames.size());
        List<List<Port>> ports = new ArrayList<>(vertexNames.size());

        List<List<PortComposition>> vertexPortCompositions = new ArrayList<>(vertexNames.size());
        List<Integer> numberOfPorts = getProcessActivityPorts();
        List<Edge> edges = new ArrayList<>();
        List<Activity> processActivities = getProcessActivities();

        //create for each vertex its ports
        for (int i = 0; i < vertexNames.size(); i++) {
            ArrayList<Port> portList = new ArrayList<>(numberOfPorts.get(i));
            for (int j = 0; j < numberOfPorts.get(i); j++) {
                Port port = new Port(null, new TextLabel((j+1) + ""));
                portList.add(port);
            }
            ports.add(portList);
        }

        //create/save for each vertex its port compositions, i.e. port groups + ports
        for (int i = 0; i < ports.size(); i++) {
            List<Port> portList = ports.get(i);
            List<PortComposition> pcList = new ArrayList<>();
            pcList.addAll(portList);
            vertexPortCompositions.add(pcList);
        }

        //create each vertex
        for (int i = 0; i < vertexNames.size(); i++) {
            vertices.add(new Vertex(
                    vertexPortCompositions.get(i),
                    new TextLabel(vertexNames.get(i) + " (" + vertexOccurance.get(i) + ")")));
        }

        //create port pairings via dummy vertex groups
        List<VertexGroup> dummyVertexGroups = new ArrayList<>(2);

        //create edges
        // Zähle für jeden Knoten die Anzahl der Ports und wie viele Ports bereits belegt sind
        List<Integer> portsCount = new ArrayList<>();
        List<Integer> portsCounter = new ArrayList<>();
        for (List<Port> portlist : ports) {
            portsCount.add(portlist.size());
            portsCounter.add(0);
        }

        int maxEdgeImportance = processActivities.get(0).getOccurenceCount();
        int minEdgeImportance = processActivities.get(0).getOccurenceCount();
        for (Activity activity : processActivities) {
            for (ProcessEdge processEdge : activity.getOutgoingEdges()) {
                if (processEdge.getOccurenceCount() > maxEdgeImportance) {
                    maxEdgeImportance = processEdge.getOccurenceCount();
                }
                if (processEdge.getOccurenceCount() < minEdgeImportance) {
                    minEdgeImportance = processEdge.getOccurenceCount();
                }
            }
        }

        for (int i = 0; i < processActivities.size(); i++) {
            List<ProcessEdge> outgoingEdges = processActivities.get(i).getOutgoingEdges();
            for (ProcessEdge processEdge : outgoingEdges) {

                int outNodeIndex = i;
                int outPortIndex = portsCounter.get(outNodeIndex);
                if (outPortIndex < portsCount.get(outNodeIndex)) {
                    Port outPort = ports.get(outNodeIndex).get(outPortIndex);
                    portsCounter.set(outNodeIndex, outPortIndex + 1);

                    int inNodeIndex = processActivities.indexOf(processEdge.getEndActivity());
                    int inPortIndex = portsCounter.get(inNodeIndex);
                    if (inPortIndex < portsCount.get(inNodeIndex)) {
                        Port inPort = ports.get(inNodeIndex).get(inPortIndex);
                        portsCounter.set(inNodeIndex, inPortIndex + 1);

                        inPort.getLabelManager().setMainLabel(new TextLabel(""));
                        outPort.getLabelManager().setMainLabel(new TextLabel(""));
                        Edge newEdge = new Edge(Arrays.asList(outPort, inPort));
                        newEdge.setDirection(EdgeDirection.OUTGOING);
                        if (edgeLabelStyle.equals(EdgeLabelStyle.FREQUENCY)) {
                            newEdge.getLabelManager().setMainLabel(
                                    new TextLabel(processEdge.getOccurenceCount() + ""));
                        } else if (edgeLabelStyle.equals(EdgeLabelStyle.TIME)) {
                            newEdge.getLabelManager().setMainLabel(
                                    new TextLabel(processEdge.getAvgThroughputTime() + ""));
                        }
                        edges.add(newEdge);
                    }
                }
            }
        }

        //create graph
        return new Graph(vertices, dummyVertexGroups, edges, null);
    }

    /**
     * Reads process data from a CSV file and stores it in a list of cases.
     * Each case contains a list of activities, where each activity is represented by its name, date, and user.
     * The function also adds a start and end activity to each case.
     *
     * @param pathToFile The path to the CSV file containing the process data.
     *                   The CSV file should have a header row and the following format: caseId;name;user;date.
     *                   The caseId column should contain unique identifiers for each case.
     *                   The name column should contain the names of the activities.
     *                   The user column should contain the users responsible for each activity.
     *                   The date column should contain the dates when each activity occurred.
     *
     * @throws NullPointerException If the provided pathToFile is null.
     * @throws IOException If an error occurs while reading the CSV file.
     */
    public void readCasesFromCsvFile(String pathToFile) {

        cases = new ArrayList<>();
        if (pathToFile == null) {
            throw new NullPointerException();
        }

        String line = "";
        String cvsSplitBy = ";";

        try (BufferedReader br = new BufferedReader(new FileReader(pathToFile))) {

            int csvIndex = 0;
            while ((line = br.readLine()) != null && csvIndex < 25000) {

                // Überspringe die erste Zeile (Header)
                if (csvIndex == 0) {
                    csvIndex += 1;
                    continue;
                }

                // Verwende das Trennzeichen, um die Zeile in Felder zu trennen
                String[] fields = line.split(cvsSplitBy);

                String caseId = fields[0];
                String name = fields[1];
                String user = fields[2];
                String date = fields[3];

                // Erstelle die Aktivität
                Activity activity = new Activity(name, date, user);

                // Prüfe, ob der Fall bereits existiert
                boolean caseExists = false;
                Case currentCase = new Case(caseId);
                for (Case _case : cases) {
                    if (_case.getCaseId().equals(caseId)) {
                        caseExists = true;
                        currentCase = _case;
                    }
                }

                // Füge die Aktivität dem Fall hinzu
                currentCase.addActivity(activity);

                // Wenn der Fall noch nicht erfasst wurde, füge ihn der Fallliste hinzu
                if (!caseExists) {
                    cases.add(currentCase);
                }
                csvIndex += 1;
            }

            // Füge jedem Fall eine Start- und Endaktivität hinzu
            for (Case _case : cases) {
                _case.addStartAndEndActivity();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function adds process nodes and edges to the process model based on the given percentage.
     * If the percentage is less than 1, it captures variants and calculates their frequency.
     * It then sorts the variants by frequency and filters the most frequent variants based on the given percentage.
     * Finally, it creates process nodes and edges for the filtered cases.
     *
     * @param percentage The percentage of variants to consider. It should be a value between 0 and 1.
     *                    If the value is less than 0 or greater than 1, it is set to 1.
     *
     * @throws Exception If an error occurs during the process of adding process nodes and edges.
     */
    public void addProcessNodesAndEdges(double percentage) {

        if (percentage <= 0 || percentage > 1) {
            percentage = 1;
        }

        try {

            if (percentage < 1) {
                // Varianten erfassen und deren Häufigkeit berechnen
                Map<List<Activity>, Integer> variants = new HashMap<>();
                for (Case _case : cases) {
                    if (!variants.containsKey(_case.getActivities())) {
                        variants.put(_case.getActivities(), 0);
                    }
                    variants.replace(_case.getActivities(), variants.get(_case.getActivities()) + 1);
                }

                // Varianten-Map nach Häufigkeit sortieren
                int numberOfVariants = variants.keySet().size() * (int) (percentage * 100) / 100;
                if (numberOfVariants > 0) {
                    List<Map.Entry<List<Activity>, Integer>> variantsList = new ArrayList<>(variants.entrySet());
                    variantsList.sort(Map.Entry.<List<Activity>, Integer>comparingByValue().reversed());
                    Map<List<Activity>, Integer> sortedVariants = new LinkedHashMap<>();
                    for (Map.Entry<List<Activity>, Integer> entry : variantsList) {
                        sortedVariants.put(entry.getKey(), entry.getValue());
                    }

                    // Die numberOfVariants häufigsten Varianten filtern und in eine List schreiben
                    int i = 0;
                    List<List<Activity>> relevantVariants = new ArrayList<>();
                    for (List<Activity> variant : sortedVariants.keySet()) {
                        if (i < numberOfVariants) {
                            relevantVariants.add(variant);
                        }
                        i += 1;
                    }

                    // Alle Fälle filtern, die einer der häufigsten Varianten entsprechen
                    List<Case> relevantCases = new ArrayList<>();
                    for (Case _case : cases) {
                        for (List<Activity> activityList : relevantVariants) {
                            if (_case.getActivities().equals(activityList)) {
                                relevantCases.add(_case);
                            }
                        }
                    }
                    cases = relevantCases;
                }
            }

            processActivities = new ArrayList<>();
            processEdges = new ArrayList<>();

            for (Case _case : cases) {
                for (int i = 0; i < _case.getActivities().size() - 1; i++) {

                    Activity startActivity = _case.getActivities().get(i);
                    int startActivityIndex = processActivities.indexOf(startActivity);

                    if (startActivityIndex == -1) { processActivities.add(startActivity); }
                    else { startActivity = processActivities.get(startActivityIndex); }

                    Activity endActivity = _case.getActivities().get(i + 1);
                    int endActivityIndex = processActivities.indexOf(endActivity);

                    if (endActivityIndex == -1) { processActivities.add(endActivity); }
                    else { endActivity = processActivities.get(endActivityIndex); }

                    ProcessEdge newEdge = new ProcessEdge(startActivity, endActivity);
                    startActivity.addOutgoingEdge(newEdge);
                    endActivity.addIncomingEdge(newEdge);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printCases() {
        System.out.println("\n---- FÄLLE ----\n");
        for (Case _case : cases) {
            System.out.println(_case);
        }
    }

    public void printProcessActivities() {
        System.out.println("\n---- KNOTEN ----\n");
        for (Activity activity : processActivities) {
            System.out.println(activity);
        }
    }

    public void printProcessEdges() {
        System.out.println("\n---- KANTEN ----\n");
        for (ProcessEdge edge : processEdges) {
            System.out.println(edge);
        }
    }

    public List<Case> getCases() {
        return cases;
    }

    public List<Activity> getProcessActivities() {
        return processActivities;
    }

    /**
     * Retrieves the names of the activities in the process model.
     *
     * @return A list of strings representing the names of the activities.
     *         Each string corresponds to the name of an activity in the process model.
     */
    public List<String> getProcessActivityNames() {
        List<String> names = new ArrayList<>();
        for (Activity activity: processActivities) {
            names.add(activity.getName());
        }
        return names;
    }

        /**
     * Retrieves the occurrence count of each activity in the process model.
     *
     * @return A list of integers representing the occurrence count of each activity.
     *         Each integer corresponds to the occurrence count of an activity in the process model.
     *         The order of the integers in the list matches the order of the activities in the processActivities list.
     */
    public List<Integer> getProcessActivityOccurence() {
        List<Integer> labels = new ArrayList<>();
        for (Activity activity: processActivities) {
            labels.add(activity.getOccurenceCount());
        }
        return labels;
    }

        /**
     * Retrieves the number of ports for each activity in the process model.
     *
     * @return A list of integers representing the number of ports for each activity.
     *         Each integer corresponds to the number of ports for an activity in the process model.
     *         The order of the integers in the list matches the order of the activities in the processActivities list.
     */
    public List<Integer> getProcessActivityPorts() {
        List<Integer> ports = new ArrayList<>();
        for (Activity activity: processActivities) {
            ports.add(activity.getPortsCount());
        }
        return ports;
    }

    public List<ProcessEdge> getProcessEdges() {
        return processEdges;
    }

    public void setPercentageOfVariants(double percentageOfVariants) {
        this.percentageOfVariants = percentageOfVariants;
    }

    public double getPercentageOfVariants() {
        return percentageOfVariants;
    }

    public void setEdgeLabelStyle(EdgeLabelStyle edgeLabelStyle) {
        this.edgeLabelStyle = edgeLabelStyle;
    }

    public EdgeLabelStyle getEdgeLabelStyle() {
        return edgeLabelStyle;
    }
}
