package de.uniwue.informatik.praline.datastructure.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.awt.*;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class ShapeStyle extends Style {

    /*==========
     * Default values
     *==========*/

    public static Color DEFAULT_COLOR = Color.BLACK;

    private static final String TEMPLATE_DESCRIPTION = "default shape style";
    public static final ShapeStyle DEFAULT_SHAPE_STYLE = new ShapeStyle(TEMPLATE_DESCRIPTION, DEFAULT_COLOR);

    /*==========
     * Instance variables
     *==========*/

    private Color color;

    //todo: add more properties like border width, ...


    /*==========
     * Constructors
     *==========*/

    public ShapeStyle() {
        this(null, null);
    }

    @JsonCreator
    public ShapeStyle(
            @JsonProperty("description") final String description,
            @JsonProperty("color") final Color color
    ) {
        super(description);
        this.color = color == null ? DEFAULT_COLOR : color;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
