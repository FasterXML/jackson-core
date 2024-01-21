package com.fasterxml.jackson.core.write;

import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @since 2.17
 */
public class JsonWriteFeatureEscapeForwardSlashTest
{

    @Test
    public void testEscapeForwardSlash() throws Exception {
        // Given
        Writer jsonWriter = new StringWriter();
        JsonGenerator generator = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .build()
                .createGenerator(jsonWriter);

        // When
        generator.writeStartObject(); // start object
        generator.writeStringField("url", "http://example.com");
        generator.writeEndObject(); // end object
        generator.close();

        // Then
        assertEquals("{\"url\":\"http:\\/\\/example.com\"}", jsonWriter.toString());
    }
}
