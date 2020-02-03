package de.uniwue.informatik.praline.datastructure.utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.subserializer.ColorDeserializer;
import de.uniwue.informatik.praline.datastructure.utils.subserializer.ColorSerializer;
import de.uniwue.informatik.praline.datastructure.utils.subserializer.FontDeserializer;
import de.uniwue.informatik.praline.datastructure.utils.subserializer.FontSerializer;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Here are some static methods for serialization provided.
 * It uses jackson to transform to and from JSON.
 *
 * For {@link Font} and {@link Color} we use the serializer and deserializer that are in subpackage utils.subserializer
 */
public class Serialization {

    private final static ObjectMapper mapper =
            new ObjectMapper()
                    .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                    .registerModule(new SimpleModule()
                            .addSerializer(Color.class, new ColorSerializer())
                            .addDeserializer(Color.class, new ColorDeserializer())
                            .addSerializer(Font.class, new FontSerializer())
                            .addDeserializer(Font.class, new FontDeserializer())
                    );

    /**
     * Read a graph from a JSON file.
     *
     * @param file JSON file to read diagram from
     * @param klass Concrete class to instantiate
     * @return Instance
     * @throws IOException if reading the file fails
     */
    public static <T extends Graph> T read(File file, Class<T> klass) throws IOException {
        return mapper.readValue(file, klass);
    }

    /**
     * Writes the graph to a JSON string.
     *
     * @param graph Diagram to write
     * @return JSON string
     * @throws JsonProcessingException if subserializer failed
     */
    public static String write(Graph graph) throws JsonProcessingException {
        return mapper.writeValueAsString(graph);
    }
}
