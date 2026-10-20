package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.util.JsonpCharacterEscapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [core#1680]: scan-then-bulk-copy fast path for longer String values in UTF8JsonGenerator;
// verify output matches WriterBasedJsonGenerator (which does not use it)
class StringBulkCopy1680Test extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    // Chars to inject: escapes, non-ASCII Latin-1, and non-Latin-1 chars whose low byte
    // is plain ASCII (would be corrupted if copied by truncation), plus a surrogate pair
    private final static String[] SPECIALS = {
            "\"", "\\", "\n", "\u0000", "/", "\u007F", "é", "ÿ",
            "Ł", "Ģ", "Ŝ", "€", "😀"
    };

    private final static JsonFactory[] FACTORIES = {
            new JsonFactory(),
            JsonFactory.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build(),
            JsonFactory.builder().enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES).build(),
            JsonFactory.builder().quoteChar('\'').build(),
            JsonFactory.builder().highestNonEscapedChar(0xFF).build(),
            JsonFactory.builder().characterEscapes(JsonpCharacterEscapes.instance()).build()
    };

    @Test
    void specialCharAtEachPosition() throws Exception
    {
        for (JsonFactory f : FACTORIES) {
            for (int len : new int[] { 31, 32, 33, 100 }) {
                for (String special : SPECIALS) {
                    for (int pos = 0; pos <= len; ++pos) {
                        String value = _ascii(pos) + special + _ascii(len - pos);
                        _verify(f, value);
                    }
                }
            }
        }
    }

    @Test
    void cleanAndMixedValues() throws Exception
    {
        for (JsonFactory f : FACTORIES) {
            for (int len = 0; len < 200; ++len) {
                _verify(f, _ascii(len));
            }
            _verify(f, _ascii(40) + "é" + _ascii(40) + "\"" + _ascii(40) + "Ł" + _ascii(40));
            // Longer than output buffer max contiguous segment, to go through segmented writes
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 300; ++i) {
                sb.append(_ascii(37)).append(SPECIALS[i % SPECIALS.length]);
            }
            _verify(f, sb.toString());
            _verify(f, _ascii(20000));
        }
    }

    private void _verify(JsonFactory f, String value) throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), w)) {
            _write(g, value);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            _write(g, value);
        }
        assertEquals(w.toString(), bytes.toString(StandardCharsets.UTF_8),
                "Mismatch for value of length "+value.length());
    }

    private void _write(JsonGenerator g, String value)
    {
        final char[] chars = value.toCharArray();
        g.writeStartObject();
        g.writeName(value);
        g.writeString(value);
        g.writeName("chars");
        g.writeString(chars, 0, chars.length);
        g.writeEndObject();
    }

    private static String _ascii(int len)
    {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }
}
