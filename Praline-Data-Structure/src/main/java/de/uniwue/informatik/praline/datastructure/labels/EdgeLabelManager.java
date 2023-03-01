package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.graphs.Edge;
import de.uniwue.informatik.praline.datastructure.graphs.Port;
import de.uniwue.informatik.praline.datastructure.graphs.PortComposition;
import de.uniwue.informatik.praline.datastructure.styles.LabelStyle;
import de.uniwue.informatik.praline.datastructure.utils.EqualLabeling;

import java.util.*;

import static de.uniwue.informatik.praline.datastructure.utils.GraphUtils.newArrayListNullSafe;

/**
 * Special type of {@link LabelManager} to handle the more options you have when placing {@link Label}s along
 * {@link Edge}s.
 * More concrete, an {@link EdgeLabelManager} has a list of  {@link Label}s assigned to any of the {@link Port}s of the
 * corresponding {@link Edge} and a list of {@link Label}s that should be drawn at the inner course of an edge (e. g.
 * in the middle of an edge).
 * The former are called port labels and the latter inner labels of an {@link Edge}.
 *
 * The list of {@link Label}s from the super class {@link LabelManager} becomes
 * the list of inner {@link Label}s of this object.
 */
@JsonIgnoreProperties({ "labels", "portLabels", "registeredPorts", "managedLabeledObject", "stringForLabeledObject" })
//Note for
// @JsonIgnoreProperties: instead of "labels" we use "innerLabels" and instead of "portLabels" we use "allPortLabels"
public class EdgeLabelManager extends LabelManager {

    /*==========
     * Instance variables
     *==========*/

    private final Map<Port, List<Label<? extends LabelStyle>>> portLabels;


    /*==========
     * Constructors
     *==========*/

    public EdgeLabelManager(Edge managedLabeledObject) {
        this(managedLabeledObject,null, null, null);
    }

    public EdgeLabelManager(Edge managedLabeledObject, Collection<Label<? extends LabelStyle>> innerLabels,
                            Map<Port, List<Label<? extends LabelStyle>>> portLabels) {
        this(managedLabeledObject, innerLabels, portLabels, null);
        findNewMainLabel();
    }

    @JsonCreator
    private EdgeLabelManager(
            @JsonProperty("innerLabels") final List<Label<? extends LabelStyle>> innerLabels,
            @JsonProperty("allPortLabels") final Set<PairPort2Labels> allPortLabels,
            @JsonProperty("mainLabel") final Label<? extends LabelStyle> mainLabel
    ) {
        this(null, innerLabels, null, mainLabel);
        for (PairPort2Labels pair : allPortLabels) {
            this.addPortLabels(pair.port, pair.labels);
        }
    }

    /**
     *
     * @param managedLabeledObject
     *      should not be null and should be set correctly initially
     * @param innerLabels
     *      can be null if there are no labels initially
     * @param portLabels
     *      can be null if there are no labels initially
     * @param mainLabel
     *      can be null if there is no label that should become the main label
     */
    public EdgeLabelManager(Edge managedLabeledObject, Collection<Label<? extends LabelStyle>> innerLabels,
                        Map<Port, List<Label<? extends LabelStyle>>> portLabels, Label<? extends LabelStyle> mainLabel) {

        super(managedLabeledObject, innerLabels, mainLabel);
        this.portLabels = new LinkedHashMap<>(portLabels == null ? 2 : portLabels.size());
        if (portLabels != null) {
            for (Port port : portLabels.keySet()) {
                if (portLabels.get(port) != null) {
                    addPortLabels(port, portLabels.get(port));
                }
            }
        }

        if (mainLabel == null) {
            findNewMainLabel();
        }
    }


    /*==========
     * Getters
     *==========*/

    public List<Label<? extends LabelStyle>> getInnerLabels() {
        return Collections.unmodifiableList(super.labels);
    }

    public List<Label<? extends LabelStyle>> getPortLabels(Port p) {
        if (portLabels.containsKey(p)) {
            return Collections.unmodifiableList(portLabels.get(p));
        }
        return null;
    }

    public static class PairPort2Labels {
        @JsonProperty
        public final Port port;
        @JsonProperty
        public final List<Label<? extends LabelStyle>> labels;
        @JsonCreator
        PairPort2Labels(
                @JsonProperty("port") final Port p,
                @JsonProperty("labels") final Collection<Label<? extends LabelStyle>> l
        ){
            port = p;
            labels = newArrayListNullSafe(l);
        }
    }

    /**
     *
     * @return
     *      A new set of pairs of ports with their port labels.
     *      If you do not need this complete set, you should rather call {@link EdgeLabelManager#getPortLabels(Port)}
     *      to avoid unnecessary full copying!
     */
    public Set<PairPort2Labels> getAllPortLabels() {
        LinkedHashSet<PairPort2Labels> allPortLabels = new LinkedHashSet<>();
        for (Port port : this.getRegisteredPorts()) {
            if (this.getPortLabels(port) != null) {
                allPortLabels.add(new PairPort2Labels(port, this.getPortLabels(port)));
            }
        }
        return allPortLabels;
    }

    public Set<Port> getRegisteredPorts() {
        return portLabels.keySet();
    }

    @Override
    public List<Label<? extends LabelStyle>> getLabels() {
        ArrayList<Label<? extends LabelStyle>> labels = new ArrayList<>(getInnerLabels());
        if (portLabels != null) {
            for (Port port : portLabels.keySet()) {
                if (portLabels.get(port) != null) {
                    labels.addAll(portLabels.get(port));
                }
            }
        }
        return labels;
    }


    /*==========
     * Modifiers
     *==========*/

    /**
     * Use this method to add multiple inner labels (different from port labels)
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    @Override
    public boolean addAllLabels(Collection<Label<? extends LabelStyle>> labels) {
        return super.addAllLabels(labels);
    }

    /**
     * Use this method to add an inner label (different from port labels)
     *
     * @param l
     * @return
     */
    @Override
    public boolean addLabel(Label<? extends LabelStyle> l){
        return super.addLabel(l);
    }

    /**
     *
     * @param labels
     * @return
     *      true if all labels are added
     *      false if not all (but maybe some!) are added
     */
    public boolean addPortLabels(Port p, Collection<Label<? extends LabelStyle>> labels) {
        if (!this.portLabels.containsKey(p)) {
            portLabels.put(p, new ArrayList<>(labels.size()));
        }
        return super.addAllLabelsInternally(portLabels.get(p), labels);
    }

    public boolean addPortLabel(Port p, Label<? extends LabelStyle> l) {
        if (!this.portLabels.containsKey(p)) {
            portLabels.put(p, new ArrayList<>(labels.size()));
        }
        return super.addLabelInternally(this.portLabels.get(p), l, true);
    }

    public boolean removePortLabel(Port p, Label<? extends LabelStyle> l) {
        if (this.portLabels.containsKey(p)) {
            List<Label<? extends LabelStyle>> labelsOfThisPort = this.portLabels.get(p);
            return super.removeLabelInternally(labelsOfThisPort, l);
        }
        return false;
    }

    public boolean removeInnerLabel(Label<? extends LabelStyle> l) {
        return super.removeLabelInternally(this.labels, l);
    }

    @Override
    public boolean removeLabel(Label<? extends LabelStyle> l) {
        boolean success = removeInnerLabel(l);
        for (Port port : portLabels.keySet()) {
            success = success | removePortLabel(port, l);
        }
        return success;
    }

    @Override
    protected void findNewMainLabel() {
        if (getLabels().size() == 1) {
            mainLabel = getLabels().get(0);
        }
        else if (getInnerLabels().size() == 1) {
            mainLabel = getInnerLabels().get(0);
        }
        else {
            mainLabel = null;
        }
    }

    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equalLabeling(o)) return false;
        EdgeLabelManager that = (EdgeLabelManager) o;
        for (Port port : portLabels.keySet()) {
            Port otherPort = null;
            for (Port thatPort : that.portLabels.keySet()) {
                if (port.equalLabeling((PortComposition) thatPort)) {
                    otherPort = thatPort;
                    break;
                }
            }
            if (otherPort == null || !EqualLabeling.equalLabelingLists(new ArrayList<>(portLabels.get(port)),
                    new ArrayList<>(that.portLabels.get(otherPort)))) {
                return false;
            }
        }
        return true;
    }
}
