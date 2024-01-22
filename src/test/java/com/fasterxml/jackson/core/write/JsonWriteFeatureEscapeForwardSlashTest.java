package com.fasterxml.jackson.core.write;

import java.io.*;

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
    public void testDontEscapeForwardSlash() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .build();
        final String expJson = "{\"url\":\"http://example.com\"}";

        // Given
        Writer jsonWriter = new StringWriter();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(jsonWriter)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, jsonWriter.toString());

        // Also test with byte-backed output
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(bytes)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, bytes.toString("UTF-8"));
    }

    @Test
    public void testEscapeForwardSlash() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .build();
        final String expJson = "{\"url\":\"http:\\/\\/example.com\"}";

        // Given
        Writer jsonWriter = new StringWriter();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(jsonWriter)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, jsonWriter.toString());

        // Also test with byte-backed output
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(bytes)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, bytes.toString("UTF-8"));
    }

    private void _writeDoc(JsonGenerator generator) throws Exception
    {
        generator.writeStartObject(); // start object
        generator.writeStringField("url", "http://example.com");
        generator.writeEndObject(); // end object
    }
}
