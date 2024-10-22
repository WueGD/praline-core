package de.uniwue.informatik.praline.io.input.processdata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Activity {
    private String name;
    private LocalDateTime date;
    private String user;
    private List<ProcessEdge> incomingEdges;
    private List<ProcessEdge> outgoingEdges;
    private int portsCount;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Constructor for Activity class.
     *
     * @param name The name of the activity.
     * @param date The date and time of the activity.
     * @param user The user who performed the activity.
     */
    public Activity(String name, LocalDateTime date, String user) {
        this.name = name;
        this.user = user;
        this.date = date;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.portsCount = 0;
    }

    /**
     * Constructor for Activity class.
     *
     * @param name The name of the activity.
     * @param dateString The date and time of the activity as a string.
     * @param user The user who performed the activity.
     */
    public Activity(String name, String dateString, String user) {
        this.name = name;
        this.user = user;
        this.date = LocalDateTime.parse(dateString, formatter2);
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.portsCount = 0;
    }
    
    /**
     * Adds an incoming edge to the activity.
     *
     * @param processEdge The incoming edge to be added.
     */
    public void addIncomingEdge(ProcessEdge processEdge) {
        if (!incomingEdges.contains(processEdge)) {
            incomingEdges.add(processEdge);
            portsCount += 1;
        }
        incomingEdges.get(incomingEdges.indexOf(processEdge)).addOccurence(
                processEdge.getStartActivity().getDate(),
                processEdge.getEndActivity().getDate()
        );
    }

    /**
     * Adds an outgoing edge to the activity.
     *
     * @param processEdge The outgoing edge to be added.
     */
    public void addOutgoingEdge(ProcessEdge processEdge) {
        if (!outgoingEdges.contains(processEdge)) {
            outgoingEdges.add(processEdge);
            portsCount += 1;
        }
        outgoingEdges.get(outgoingEdges.indexOf(processEdge)).addOccurence(
                processEdge.getStartActivity().getDate(),
                processEdge.getEndActivity().getDate()
        );
    }

    /**
     * Returns the list of incoming edges for the activity.
     *
     * @return The list of incoming edges.
     */
    public List<ProcessEdge> getIncomingEdges() {
        return incomingEdges;
    }

    /**
     * Returns the list of outgoing edges for the activity.
     *
     * @return The list of outgoing edges.
     */
    public List<ProcessEdge> getOutgoingEdges() {
        return outgoingEdges;
    }

    /**
     * Returns the occurrence count of the activity.
     *
     * @return The occurrence count.
     */
    public int getOccurenceCount() {
        int incomingPathsCount = 0;
        for (ProcessEdge processEdge : incomingEdges) {
            incomingPathsCount += processEdge.getOccurenceCount();
        }
        int outgoingPathsCount = 0;
        for (ProcessEdge processEdge : outgoingEdges) {
            outgoingPathsCount += processEdge.getOccurenceCount();
        }
        if (incomingPathsCount != outgoingPathsCount) {
            if (incomingPathsCount > 0 && outgoingPathsCount > 0) {
                System.out.println(name + " | in: " + incomingPathsCount + ", out: " + outgoingPathsCount);
            }
        }
        return Math.max(incomingPathsCount, outgoingPathsCount);
    }

    /**
     * Returns the number of ports for the activity.
     *
     * @return The number of ports.
     */
    public int getPortsCount() {
        return portsCount;
    }

    /**
     * Returns the name of the activity.
     *
     * @return The name of the activity.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the date and time of the activity.
     *
     * @return The date and time of the activity.
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Returns the user who performed the activity.
     *
     * @return The user who performed the activity.
     */
    public String getUser() {
        return user;
    }

    /**
     * Overrides the equals method to compare Activity objects based on their name.
     *
     * @param o The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        return name.equals(activity.name);
    }

    /**
     * Overrides the hashCode method to generate a hash code based on the name of the activity.
     *
     * @return The hash code of the activity.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * Overrides the toString method to provide a string representation of the activity.
     *
     * @return The string representation of the activity.
     */
    @Override
    public String toString() {
        return "(" + date + ") " + name + " | " + user;
    }
}
