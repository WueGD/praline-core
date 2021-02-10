package de.uniwue.informatik.praline.datastructure.labels.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;

import java.awt.*;

public class TextLabelStyle extends LabelStyle {

    /*==========
     * Default values
     *==========*/

    public static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 10);
    public static final boolean DEFAULT_NO_BREAK = false;
    public static final Color DEFAULT_COLOR = Color.BLACK;


    /*==========
     * Instance variables
     *==========*/

    private Font font;
    private boolean noBreak;
    private Color color;


    /*==========
     * Constructors
     *==========*/

    public TextLabelStyle() {
        this(DEFAULT_FONT);
    }

    public TextLabelStyle(Font font) {
        this(font, DEFAULT_NO_BREAK, DEFAULT_COLOR);
    }

    public TextLabelStyle(Font font, boolean noBreak, Color color) {
        this(null, font, noBreak, color, DEFAULT_SHOW_LABEL, DEFAULT_PLACEMENT, DEFAULT_HORIZONTAL_PLACEMENT,
                DEFAULT_VERTICAL_PLACEMENT);
    }

    @JsonCreator
    public TextLabelStyle(
            @JsonProperty("description") final String description,
            @JsonProperty("font") final Font font,
            @JsonProperty("noBreak") final boolean noBreak,
            @JsonProperty("color") final Color color,
            @JsonProperty("showLabel") final boolean showLabel,
            @JsonProperty("placement") final Placement placement,
            @JsonProperty("horizontalPlacement") final HorizontalPlacement horizontalPlacement,
            @JsonProperty("verticalPlacement") final VerticalPlacement verticalPlacement
    ) {
        super(description, showLabel, placement, horizontalPlacement, verticalPlacement);
        this.font = font == null ? DEFAULT_FONT : font;
        this.noBreak = noBreak;
        this.color = color == null ? DEFAULT_COLOR : color;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public boolean isNoBreak() {
        return noBreak;
    }

    public void setNoBreak(boolean noBreak) {
        this.noBreak = noBreak;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
