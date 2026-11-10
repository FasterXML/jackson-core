package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.JsonpCharacterEscapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [core#1680]: scan-then-bulk-copy fast path for longer String values in UTF8JsonGenerator;
// verify output matches WriterBasedJsonGenerator (which does not use it)
class StringBulkCopy1680Test extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    // Chars to inject: escapes, non-ASCII Latin-1, and non-Latin-1 chars whose low byte
    // is plain ASCII (would be corrupted if copied by truncation), plus a surrogate pair
    private final static String[] SPECIALS = {
            "\"", "\\", "\n", "\u0000", "/", "\u007F", "\u00E9", "\u00FF",
            "\u0141", "\u0122", "\u015C", "\u20AC", "\uD83D\uDE00"
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
            _verify(f, _ascii(40) + "\u00E9" + _ascii(40) + "\"" + _ascii(40) + "\u0141" + _ascii(40));
            // Longer than output buffer max contiguous segment, to go through segmented writes
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 300; ++i) {
                sb.append(_ascii(37)).append(SPECIALS[i % SPECIALS.length]);
            }
            _verify(f, sb.toString());
            _verify(f, _ascii(20000));
        }
    }

    // Every char around the bounds of the escape checks (0x1F/0x20, 0x7F/0x80), for values
    // both below and above the bulk-copy threshold
    @Test
    void allCharsThroughLatin1() throws Exception
    {
        for (JsonFactory f : FACTORIES) {
            for (int len : new int[] { 20, 64 }) {
                for (int ch = 0; ch <= 0x100; ++ch) {
                    for (int pos : new int[] { 0, len / 2, len }) {
                        _verify(f, _ascii(pos) + (char) ch + _ascii(len - pos));
                    }
                }
            }
        }
    }

    // Value that barely fits (or just doesn't) in what is left of the output buffer
    @Test
    void valueAtEndOfOutputBuffer() throws Exception
    {
        final int bufLen = new BufferRecycler()
                .allocByteBuffer(BufferRecycler.BYTE_WRITE_ENCODING_BUFFER).length;
        final int maxContiguous = bufLen >> 3;
        for (JsonFactory f : FACTORIES) {
            for (int len : new int[] { 32, 33, 100, maxContiguous - 1, maxContiguous, maxContiguous + 1 }) {
                final String[] values = {
                        _ascii(len),
                        _ascii(len - 1) + "\n",
                        _ascii(len / 2) + "\u00E9" + _ascii(len - (len / 2) - 1)
                };
                for (String value : values) {
                    // Sweep end position of the value across end of buffer; gap is large
                    // enough to cover structural chars and quotes around filler and value
                    for (int gap = 0; gap <= 16; ++gap) {
                        _verifyAfterFiller(f, _ascii(bufLen - len - gap), value);
                    }
                }
            }
        }
    }

    private void _verifyAfterFiller(JsonFactory f, String filler, String value) throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), w)) {
            _writeAfterFiller(g, filler, value);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            _writeAfterFiller(g, filler, value);
        }
        assertEquals(w.toString(), bytes.toString(StandardCharsets.UTF_8),
                "Mismatch for value of length "+value.length()+" after filler of length "+filler.length());
    }

    // Covers both String value and name paths; flush so that the name case starts from
    // an empty buffer too
    private void _writeAfterFiller(JsonGenerator g, String filler, String value)
    {
        g.writeStartArray();
        g.writeString(filler);
        g.writeString(value);
        g.writeEndArray();

        g.flush();
        g.writeStartObject();
        g.writeName(filler);
        g.writeNull();
        g.writeName(value);
        g.writeNull();
        g.writeEndObject();
    }

    private void _verify(JsonFactory f, String value) throws Exception
    {
        _verify(f, value, false);
        // pretty-printing writes names via a different path
        _verify(f, value, true);
    }

    private void _verify(JsonFactory f, String value, boolean pretty) throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = _createGenerator(f, w, null, pretty)) {
            _write(g, value);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = _createGenerator(f, null, bytes, pretty)) {
            _write(g, value);
        }
        assertEquals(w.toString(), bytes.toString(StandardCharsets.UTF_8),
                "Mismatch for value of length "+value.length()+" (pretty: "+pretty+")");
    }

    private JsonGenerator _createGenerator(JsonFactory f, StringWriter w, ByteArrayOutputStream out,
            boolean pretty)
    {
        ObjectWriteContext ctxt = pretty
                ? new ObjectWriteContext.Base() {
                    @Override
                    public PrettyPrinter getPrettyPrinter() {
                        return new DefaultPrettyPrinter();
                    }
                }
                : ObjectWriteContext.empty();
        return (w != null) ? f.createGenerator(ctxt, w) : f.createGenerator(ctxt, out);
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
