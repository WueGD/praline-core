package de.uniwue.informatik.praline.datastructure.paths;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolygonalPath.class, name = "polygonalPath")
})
public abstract class Path {

    /*==========
     * Default values
     *==========*/

    public static final double UNSPECIFIED_THICKNESS = -1;


    /*==========
     * Instance variables
     *==========*/

    /**
     * -1 for unspecified
     */
    private double thickness;


    /*==========
     * Constructors
     *==========*/

    protected Path() {
        this(Path.UNSPECIFIED_THICKNESS);
    }

    protected Path(double thickness) {
        this.thickness = thickness;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }
}
