package de.uniwue.informatik.praline.datastructure.labels;

import de.uniwue.informatik.praline.datastructure.placements.HorizontalPlacement;
import de.uniwue.informatik.praline.datastructure.placements.Placement;
import de.uniwue.informatik.praline.datastructure.placements.VerticalPlacement;
import de.uniwue.informatik.praline.datastructure.shapes.Shape;

import java.awt.*;

/**
 * Currentely there is no transformation from {@link TextLabel#inputText}
 * to {@link TextLabel#layoutText} handled internally.
 * So if not touched, {@link TextLabel#getLayoutText()} will return null.
 * It should be determined/computed and set via {@link TextLabel#setLayoutText(String)}
 * from outside (e.g. by the layouting algorithm).
 */
public class TextLabel extends Label {

    /*==========
     * Default values
     *==========*/

    public static final Font DEFAULT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    public static final boolean DEFAULT_NO_BREAK = false;
    public static final Color DEFAULT_COLOR = Color.BLACK;

    /*==========
     * Instance variables
     *==========*/

    private String inputText;
    private Font font;
    private boolean noBreak;
    private Color color;
    private String layoutText;

    /*==========
     * Constructors
     *==========*/

    public TextLabel() {
        this("", TextLabel.DEFAULT_FONT, TextLabel.DEFAULT_NO_BREAK, TextLabel.DEFAULT_COLOR, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText) {
        this(inputText, TextLabel.DEFAULT_FONT, TextLabel.DEFAULT_NO_BREAK, TextLabel.DEFAULT_COLOR, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, Font font) {
        this(inputText, font, TextLabel.DEFAULT_NO_BREAK, TextLabel.DEFAULT_COLOR, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, boolean noBreak) {
        this(inputText, TextLabel.DEFAULT_FONT, noBreak, TextLabel.DEFAULT_COLOR, Placement.FREE,
                HorizontalPlacement.FREE, VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, Font font, boolean noBreak) {
        this(inputText, font, noBreak, TextLabel.DEFAULT_COLOR, Placement.FREE, HorizontalPlacement.FREE,
                VerticalPlacement.FREE, Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, Font font, boolean noBreak, Color color) {
        this(inputText, font, noBreak, color, Placement.FREE, HorizontalPlacement.FREE, VerticalPlacement.FREE,
                Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, Font font, boolean noBreak, Color color, Placement placement,
                     HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement) {
        this(inputText, font, noBreak, color, placement, horizontalPlacement, verticalPlacement,
                Label.DEFAULT_SHOW_LABEL, null);
    }

    public TextLabel(String inputText, Font font, boolean noBreak, Color color, Placement placement,
                     HorizontalPlacement horizontalPlacement, VerticalPlacement verticalPlacement,
                     boolean showLabel, Shape shape) {
        super(placement, horizontalPlacement, verticalPlacement, showLabel, shape);
        this.inputText = inputText;
        this.font = font;
        this.noBreak = noBreak;
        this.color = color;
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

    public String getLayoutText() {
        return layoutText;
    }

    public void setLayoutText(String layoutText) {
        this.layoutText = layoutText;
    }
}
