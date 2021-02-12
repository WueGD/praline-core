package de.uniwue.informatik.praline.datastructure.labels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniwue.informatik.praline.datastructure.styles.TextLabelStyle;
import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

import java.awt.*;
import java.util.Objects;

/**
 * Version of {@link Label} that provides text (see {@link Label} for more).
 * So in this sense maybe the most typical "label".
 * The text may be formatted via {@link TextLabelStyle#getFont()} and {@link TextLabelStyle#getColor()}.
 *
 * The user should set the {@link TextLabel#inputText} and from this the algorithm should generate something for
 * {@link TextLabel#setLayoutText(String)} (respecting {@link TextLabelStyle#isNoBreak()}.
 *
 * Currently there is no transformation from {@link TextLabel#inputText}
 * to {@link TextLabel#layoutText} handled internally.
 * So if not touched, {@link TextLabel#getLayoutText()} will return null.
 * It should be determined/computed and set via {@link TextLabel#setLayoutText(String)}
 * from outside (e.g. by the layouting algorithm).
 */
public class TextLabel extends Label<TextLabelStyle> {


    /*==========
     * Instance variables
     *==========*/

    private String inputText;
    private String layoutText;


    /*==========
     * Constructors
     *==========*/

    public TextLabel(String inputText) {
        this(inputText, null, null);
    }

    public TextLabel(String inputText, TextLabelStyle labelStyle) {
        this(inputText, labelStyle, null);
    }

    @JsonCreator
    public TextLabel(
            @JsonProperty("inputText") final String inputText,
            @JsonProperty("labelStyle") final TextLabelStyle labelStyle,
            @JsonProperty("shape") final Shape shape
    ) {
        super(labelStyle == null ? TextLabelStyle.DEFAULT_TEXT_LABEL_STYLE : labelStyle, shape);
        this.inputText = inputText;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getLayoutText() {
        return layoutText;
    }

    public void setLayoutText(String layoutText) {
        this.layoutText = layoutText;
    }


    /*==========
     * toString
     *==========*/

    @Override
    public String toString() {
        return inputText;
    }


    /*==========
     * equalLabeling
     *==========*/

    @Override
    public boolean equalLabeling(Label o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextLabel textLabel = (TextLabel) o;
        return Objects.equals(inputText, textLabel.inputText);
    }
}
