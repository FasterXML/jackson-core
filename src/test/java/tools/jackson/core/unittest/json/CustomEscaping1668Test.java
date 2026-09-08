package tools.jackson.core.unittest.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [core#1668]: zero-length custom escape ending exactly at output buffer boundary
class CustomEscaping1668Test extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    @SuppressWarnings("serial")
    static class ZeroLengthNulEscape extends CharacterEscapes
    {
        private final static SerializableString EMPTY = new SerializedString("");

        private final int[] _asciiEscapes;

        public ZeroLengthNulEscape() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes[0] = CharacterEscapes.ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() { return _asciiEscapes; }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return (ch == 0) ? EMPTY : null;
        }
    }

    private final JsonFactory JSON_F = JsonFactory.builder()
            .characterEscapes(new ZeroLengthNulEscape())
            .build();

    // Escaped char is last of the String, and at various offsets straddling
    // the output buffer boundary (4000 chars for Writer-backed, 8000 bytes for byte-backed)
    @Test
    void zeroLengthEscapeAtBufferBoundaryChars() throws Exception
    {
        for (int pad = 3900; pad <= 4100; ++pad) {
            StringWriter w = new StringWriter();
            try (JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), w)) {
                _writeDoc(g, pad);
            }
            assertEquals(_expected(pad), w.toString(), "Failure with pad="+pad);
        }
    }

    @Test
    void zeroLengthEscapeAtBufferBoundaryBytes() throws Exception
    {
        for (int pad = 7900; pad <= 8100; ++pad) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8)) {
                _writeDoc(g, pad);
            }
            assertEquals(_expected(pad), out.toString("UTF-8"), "Failure with pad="+pad);
        }
    }

    private void _writeDoc(JsonGenerator g, int pad) throws Exception
    {
        g.writeStartArray();
        g.writeString(_repeat('x', pad));
        g.writeString("abc\0"); // last char is the custom-escaped one
        g.writeEndArray();
    }

    private String _expected(int pad) {
        return "[\"" + _repeat('x', pad) + "\",\"abc\"]";
    }

    private String _repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }
}
