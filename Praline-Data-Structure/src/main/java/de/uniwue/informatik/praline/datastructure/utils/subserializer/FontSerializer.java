package de.uniwue.informatik.praline.datastructure.utils.subserializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.*;
import java.io.IOException;

public class FontSerializer extends JsonSerializer<Font> {
    @Override
    public void serialize(Font value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("fontName", value.getFontName());
        gen.writeNumberField("style", value.getStyle());
        gen.writeNumberField("size", value.getSize());
        gen.writeEndObject();
    }
}
