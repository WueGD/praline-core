package de.uniwue.informatik.praline.datastructure.utils.subserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.awt.*;
import java.io.IOException;

public class FontDeserializer extends JsonDeserializer<Font> {
    @Override
    public Font deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode root = p.getCodec().readTree(p);
        TextNode fontName = (TextNode) root.get("fontName");
        IntNode style = (IntNode) root.get("style");
        IntNode size = (IntNode) root.get("size");
        return new Font(fontName.textValue(), style.intValue(), size.intValue());
    }
}
