package de.uniwue.informatik.praline.datastructure;

import java.util.Map;

/**
 * Allows user-specified properties to be added to a graph element.
 */
public interface PropertyObject {
    /**
     * Gets the specified property contained in the graph element.
     *
     * @param key Property to get
     * @return Property value or null if not set
     */
    String getProperty(String key);

    /**
     * Sets the specified property in the graph element.
     *
     * @param key Property key
     * @param value Property value
     */
    void setProperty(String key, String value);

    /**
     * Gets a map of all properties set in the graph element.
     *
     * @return Map containing all properties
     */
    Map<String, String> getProperties();
}
