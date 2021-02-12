package de.uniwue.informatik.praline.datastructure.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.awt.*;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class PathStyle extends Style {

    /*==========
     * Default values
     *==========*/

    public static final double UNSPECIFIED_THICKNESS = -1;
    public static Color DEFAULT_COLOR = Color.BLACK;

    private static final String TEMPLATE_DESCRIPTION = "default path style";
    public static final PathStyle DEFAULT_PATH_STYLE = new PathStyle(TEMPLATE_DESCRIPTION, UNSPECIFIED_THICKNESS,
            DEFAULT_COLOR);


    /*==========
     * Instance variables
     *==========*/

    private double thickness;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public PathStyle() {
        this(null, UNSPECIFIED_THICKNESS, null);
    }

    @JsonCreator
    public PathStyle(
            @JsonProperty("description") final String description,
            @JsonProperty("thickness") final double thickness,
            @JsonProperty("color") final Color color
    ) {
        super(description);
        this.thickness = thickness < 0 || !Double.isFinite(thickness) ? UNSPECIFIED_THICKNESS : thickness;
        this.color = color == null ? DEFAULT_COLOR : color;
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

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
