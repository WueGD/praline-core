package de.uniwue.informatik.praline.datastructure.utils.subserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import java.awt.*;
import java.io.IOException;

public class ColorDeserializer extends JsonDeserializer<Color> {
    @Override
    public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode root = p.getCodec().readTree(p);
        TextNode rgba = (TextNode) root.get("argb");
        return new Color(Integer.parseUnsignedInt(rgba.textValue(), 16), true);
    }
}
