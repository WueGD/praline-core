package de.uniwue.informatik.praline.datastructure.graphs;

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
