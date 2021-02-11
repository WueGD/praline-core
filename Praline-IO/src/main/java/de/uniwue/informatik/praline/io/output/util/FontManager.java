package de.uniwue.informatik.praline.io.output.util;

import de.uniwue.informatik.praline.datastructure.labels.TextLabel;

import java.awt.*;

public class FontManager {

    public static final Font DEFAULT_FONT = new Font("Courier", Font.PLAIN, 10);

    public static final boolean REPLACE_SANS_SERIF = false;
    public static final Font SANS_SERIF_REPLACER = DEFAULT_FONT;

    public static Font fontOf(TextLabel mainLabel) {
        Font font = mainLabel.getLabelStyle().getFont();
        if (font == null) {
            font = DEFAULT_FONT;
        }
        if (REPLACE_SANS_SERIF && font.getName().contains(Font.SANS_SERIF)) {
            font = SANS_SERIF_REPLACER;
        }
        return font;
    }
}
