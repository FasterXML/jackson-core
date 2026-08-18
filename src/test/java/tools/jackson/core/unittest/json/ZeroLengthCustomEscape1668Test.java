package tools.jackson.core.unittest.json;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// [core#1668]: a zero-length custom escape at the last character of a string
// must not throw ArrayIndexOutOfBoundsException when that character lands
// exactly at the Writer-backed output buffer boundary.
class ZeroLengthCustomEscape1668Test
    extends JacksonCoreTestBase
{
    @SuppressWarnings("serial")
    static class ZeroLengthNulEscape extends CharacterEscapes
    {
        private static final SerializableString EMPTY = new SerializedString("");
        private final int[] escapes;

        ZeroLengthNulEscape() {
            escapes = standardAsciiEscapesForJSON();
            escapes[0] = ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return ch == 0 ? EMPTY : null;
        }
    }

    @Test
    void writerStringAtBufferBoundary() throws Exception {
        _testAtBufferBoundary(false, false);
    }

    @Test
    void writerCharsAtBufferBoundary() throws Exception {
        _testAtBufferBoundary(false, true);
    }

    @Test
    void utf8StringAtBufferBoundary() throws Exception {
        _testAtBufferBoundary(true, false);
    }

    @Test
    void utf8CharsAtBufferBoundary() throws Exception {
        _testAtBufferBoundary(true, true);
    }

    private void _testAtBufferBoundary(boolean useStream, boolean stringAsChars)
        throws Exception
    {
        JsonFactory factory = JsonFactory.builder()
                .characterEscapes(new ZeroLengthNulEscape())
                .build();
        final String value = "Istio LFS144" + '\0';
        final String expectedValue = "Istio LFS144";

        // Default concat buffer is 4000 chars; sweep alignments around that edge.
        for (int pad = 3900; pad <= 4100; pad++) {
            String json;
            if (useStream) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (JsonGenerator gen = factory.createGenerator(
                        ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8)) {
                    _writeDoc(gen, pad, value, stringAsChars);
                }
                json = bytes.toString("UTF-8");
            } else {
                StringWriter w = new StringWriter();
                try (JsonGenerator gen = factory.createGenerator(
                        ObjectWriteContext.empty(), w)) {
                    _writeDoc(gen, pad, value, stringAsChars);
                }
                json = w.toString();
            }
            String expected = "[\"" + "x".repeat(pad) + "\"," + q(expectedValue) + "]";
            assertEquals(expected, json, "pad=" + pad);
            assertFalse(json.contains("\0"), "NUL should be omitted, pad=" + pad);
        }
    }

    private void _writeDoc(JsonGenerator gen, int pad, String value, boolean stringAsChars)
        throws Exception
    {
        gen.writeStartArray();
        _writeString(gen, "x".repeat(pad), stringAsChars);
        _writeString(gen, value, stringAsChars);
        gen.writeEndArray();
    }

    private void _writeString(JsonGenerator gen, String str, boolean stringAsChars)
        throws Exception
    {
        if (stringAsChars) {
            char[] ch = str.toCharArray();
            gen.writeString(ch, 0, ch.length);
        } else {
            gen.writeString(str);
        }
    }
}
