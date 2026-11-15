package tools.jackson.core.unittest.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonGeneratorBase;
import tools.jackson.core.json.JsonWriteFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JsonWriteFeatureEscapeForwardSlashTest
{
    @Test
    public void testDefaultSettings() {
        JsonFactory jsonF = new JsonFactory();
        // 11-Aug-2025, tatu: [core#1200] was reverted, so...
        assertFalse(jsonF.isEnabled(JsonWriteFeature.ESCAPE_FORWARD_SLASHES));
        try (JsonGeneratorBase g = (JsonGeneratorBase) jsonF.createGenerator(ObjectWriteContext.empty(),
                new StringWriter())) {
            assertFalse(g.isEnabled(JsonWriteFeature.ESCAPE_FORWARD_SLASHES));
        }
        try (JsonGeneratorBase g = (JsonGeneratorBase) jsonF.createGenerator(ObjectWriteContext.empty(),
                new ByteArrayOutputStream())) {
            assertFalse(g.isEnabled(JsonWriteFeature.ESCAPE_FORWARD_SLASHES));
        }
    }

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
    void escapeForwardSlash() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .build();
        final String expJson = "{\"url\":\"http:\\/\\/example.com\"}";

        _testWithStringWriter(jsonF, expJson);
        _testWithByteArrayOutputStream(jsonF, expJson); // Also test with byte-backed output
    }

    // Custom CharacterEscapes must not drop escaping of forward slash
    @Test
    void escapeForwardSlashWithCustomEscapes() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .characterEscapes(new CustomEscapes())
                .build();
        final String expJson = "{\"url\":\"http:\\/\\/example.com\"}";

        _testWithStringWriter(jsonF, expJson);
        _testWithByteArrayOutputStream(jsonF, expJson);
    }

    // ... but should not add escaping if feature disabled
    @Test
    void dontEscapeForwardSlashWithCustomEscapes() throws Exception {
        final JsonFactory jsonF = JsonFactory.builder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .characterEscapes(new CustomEscapes())
                .build();
        final String expJson = "{\"url\":\"http://example.com\"}";

        _testWithStringWriter(jsonF, expJson);
        _testWithByteArrayOutputStream(jsonF, expJson);
    }

    @SuppressWarnings("serial")
    static class CustomEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes;

        CustomEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['<'] = CharacterEscapes.ESCAPE_STANDARD;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return null;
        }
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
