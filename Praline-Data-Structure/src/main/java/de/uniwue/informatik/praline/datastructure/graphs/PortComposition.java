package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;

/**
 * This interface represents either a {@link PortGroup} or a {@link Port}.
 * We use this interface so that a {@link PortGroup} can store {@link PortGroup}s and {@link Port}s without
 * distinguishing between them.
 * That way we can build trees of {@link PortGroup}s ({@link PortGroup}s are the inner nodes of the tree) and these
 * trees have {@link Port}s as leaves and leaves can appear in every level of the tree.
 */
@JsonIgnoreProperties({ "portGroup", "vertex" })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PortGroup.class, name = "portGroup"),
        @JsonSubTypes.Type(value = Port.class, name = "port"),
})
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public interface PortComposition {

    Vertex getVertex();
    /**
     * Should not be manually called.
     * This method is primarily made for the class {@link Vertex}
     * which calls it when a {@link PortComposition} is added or removed to it
     *
     * @param vertex
     */
    void setVertex(Vertex vertex);

    /**
     * @return
     *      null if it is contained in no {@link PortGroup}
     */
    PortGroup getPortGroup();
    /**
     * Should not be manually called.
     * This method is primarily made for the class {@link PortGroup}
     * which calls it when a {@link PortComposition} is added or removed to it
     *
     * @param portGroup
     */
    void setPortGroup(PortGroup portGroup);

    boolean equalLabeling(PortComposition portComposition);
}
