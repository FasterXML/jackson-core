package tools.jackson.core.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;

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

        _testWithStringWriter(jsonF, expJson);
        _testWithByteArrayOutputStream(jsonF, expJson); // Also test with byte-backed output
    }

    @Test
    public void testEscapeForwardSlash() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .build();
        final String expJson = "{\"url\":\"http:\\/\\/example.com\"}";

        _testWithStringWriter(jsonF, expJson);
        _testWithByteArrayOutputStream(jsonF, expJson); // Also test with byte-backed output
    }

    private void _testWithStringWriter(JsonFactory jsonF, String expJson) throws Exception {
        // Given
        Writer jsonWriter = new StringWriter();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(ObjectWriteContext.empty(), jsonWriter)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, jsonWriter.toString());
    }

    private void _testWithByteArrayOutputStream(JsonFactory jsonF, String expJson) throws Exception {
        // Given
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // When
        try (JsonGenerator generator = jsonF.createGenerator(ObjectWriteContext.empty(), bytes)) {
            _writeDoc(generator);
        }
        // Then
        assertEquals(expJson, bytes.toString());
    }

    private void _writeDoc(JsonGenerator generator) throws Exception
    {
        generator.writeStartObject(); // start object
        generator.writeStringProperty("url", "http://example.com");
        generator.writeEndObject(); // end object
    }
}
