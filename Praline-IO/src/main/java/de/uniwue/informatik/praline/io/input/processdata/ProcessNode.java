package de.uniwue.informatik.praline.io.input.processdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessNode {

    private String name;
    private int occurenceCount;
    private List<ProcessEdge> incomingEdges;
    private List<ProcessEdge> outgoingEdges;
    private int portsCount;

    public ProcessNode(String name) {
        this.name = name;
        this.occurenceCount = 1;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.portsCount = 0;
    }

    public void addOccurance() {
        occurenceCount += 1;
    }
    public void addIncomingEdge(ProcessEdge processEdge) {
        if (!incomingEdges.contains(processEdge)) {
            incomingEdges.add(processEdge);
            portsCount += 1;
        }
    }
    public void addOutgoingEdge(ProcessEdge processEdge) {
        if (!outgoingEdges.contains(processEdge)) {
            outgoingEdges.add(processEdge);
            portsCount += 1;
        }
    }

    public String getName() {
        return name;
    }

    public List<ProcessEdge> getIncomingEdges() {
        return incomingEdges;
    }

    public List<ProcessEdge> getOutgoingEdges() {
        return outgoingEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessNode)) return false;
        ProcessNode that = (ProcessNode) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public int getOccurenceCount() {
        return occurenceCount;
    }

    public int getPortsCount() {
        return portsCount;
    }

    @Override
    public String toString() {
        return name + " | Occurence:" + occurenceCount + " | Ports: " + portsCount;
    }
}
