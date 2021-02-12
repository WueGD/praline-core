package de.uniwue.informatik.praline.datastructure.oldUnstyledObjects;

import com.fasterxml.jackson.annotation.*;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;
import de.uniwue.informatik.praline.datastructure.styles.TextLabelStyle;

import java.awt.*;

/**
 * Wrapper class to provide support back to when the style template
 * {@link TextLabelStyle}
 * did not yet exist (and each text label had its own full info of styling)
 */
public class OldUnstyledTextLabel extends TextLabel {

    /*==========
     * Constructors
     *==========*/

    @JsonCreator
    public OldUnstyledTextLabel(
            @JsonProperty("inputText") final String inputText,
            @JsonProperty("font") final Font font,
            @JsonProperty("noBreak") final boolean noBreak,
            @JsonProperty("color") final Color color,
            @JsonProperty("placement") final Placement placement,
            @JsonProperty("horizontalPlacement") final HorizontalPlacement horizontalPlacement,
            @JsonProperty("verticalPlacement") final VerticalPlacement verticalPlacement,
            @JsonProperty("showLabel") final boolean showLabel,
            @JsonProperty("shape") final Shape shape
    ) {
        super(inputText, new TextLabelStyle(null, font, noBreak, color, showLabel, placement, horizontalPlacement,
            verticalPlacement), shape);
    }
}
