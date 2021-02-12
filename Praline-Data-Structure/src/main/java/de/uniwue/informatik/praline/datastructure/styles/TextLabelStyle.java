package de.uniwue.informatik.praline.datastructure.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;

import java.awt.*;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class TextLabelStyle extends LabelStyle {

    /*==========
     * Default values
     *==========*/

    public static final Font DEFAULT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    public static final boolean DEFAULT_NO_BREAK = false;
    public static final Color DEFAULT_COLOR = Color.BLACK;

    private static final String TEMPLATE_DESCRIPTION = "default text label style";
    public static final TextLabelStyle DEFAULT_TEXT_LABEL_STYLE = new TextLabelStyle(TEMPLATE_DESCRIPTION, DEFAULT_FONT,
            DEFAULT_NO_BREAK, DEFAULT_COLOR, DEFAULT_SHOW_LABEL, DEFAULT_PLACEMENT, DEFAULT_HORIZONTAL_PLACEMENT,
            DEFAULT_VERTICAL_PLACEMENT);

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
        this(null, null);
    }

    public TextLabelStyle(String description, Font font) {
        this(description, font, DEFAULT_NO_BREAK, DEFAULT_COLOR);
    }

    public TextLabelStyle(String description, Font font, boolean noBreak, Color color) {
        this(description, font, noBreak, color, DEFAULT_SHOW_LABEL, DEFAULT_PLACEMENT, DEFAULT_HORIZONTAL_PLACEMENT,
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
