package de.uniwue.informatik.praline.datastructure.graphs;

import com.fasterxml.jackson.annotation.*;

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
}
