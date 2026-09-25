package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@code UTF8JsonGenerator} copying {@code String} content through
 * its {@code char[]} copy buffer: chunk boundaries must not split surrogate
 * pairs, and a copy buffer smaller than the output segment size must work.
 */
class UTF8StringWriteChunkingTest extends JacksonCoreTestBase
{
    // Default sizes: 4000-char copy buffer, 8000-byte output buffer (so 1000-char segments)
    private final static int COPY_BUFFER_LEN = 4000;

    private final JsonFactory JSON_F = newStreamFactory();

    private final JsonFactory SURROGATE_COMBINING_JSON_F = JsonFactory.builder()
            .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    // BufferRecycler with a 100-char copy buffer, smaller than the default
    // 1000-char output segment
    @SuppressWarnings("serial")
    static class SmallCopyBufferRecycler extends BufferRecycler {
        @Override
        protected int charBufferLength(int ix) {
            return (ix == CHAR_CONCAT_BUFFER) ? 100 : super.charBufferLength(ix);
        }
    }

    @SuppressWarnings("serial")
    static class SmallCopyBufferPool implements RecyclerPool<BufferRecycler> {
        @Override
        public BufferRecycler acquirePooled() { return new SmallCopyBufferRecycler(); }
        @Override
        public void releasePooled(BufferRecycler r) { }
        @Override
        public int pooledCount() { return 0; }
        @Override
        public boolean clear() { return true; }
    }

    private final JsonFactory SMALL_COPY_BUFFER_JSON_F = JsonFactory.builder()
            .recyclerPool(new SmallCopyBufferPool())
            .build();

    @Test
    void surrogatePairAtCopyBufferBoundary() throws Exception
    {
        for (JsonFactory f : new JsonFactory[] { JSON_F, SURROGATE_COMBINING_JSON_F }) {
            // pair straddling first chunk boundary, then second one
            for (int prefixLen : new int[] { COPY_BUFFER_LEN - 1, 2 * COPY_BUFFER_LEN - 1 }) {
                String value = _repeat('x', prefixLen) + "🫡" + "yz";
                _verifyStringRoundTrip(f, value);
                _verifyNameRoundTrip(f, value);
            }
        }
    }

    @Test
    void longNonAsciiString() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3 * COPY_BUFFER_LEN; ++i) {
            switch (i % 7) {
            case 0: sb.append('é'); break; // 2-byte
            case 3: sb.append('€'); break; // 3-byte
            case 5: sb.append('"'); break;
            default: sb.append('a');
            }
        }
        String value = sb.toString();
        for (JsonFactory f : new JsonFactory[] { JSON_F, SURROGATE_COMBINING_JSON_F, SMALL_COPY_BUFFER_JSON_F }) {
            _verifyStringRoundTrip(f, value);
            _verifyNameRoundTrip(f, value);
        }
    }

    @Test
    void copyBufferSmallerThanOutputSegment() throws Exception
    {
        // lengths below, between and above the 100-char copy buffer and 1000-char segment
        for (int len : new int[] { 1, 99, 100, 101, 500, 999, 1000, 1001, 4001 }) {
            String value = _repeat('x', len - 1) + "é";
            _verifyStringRoundTrip(SMALL_COPY_BUFFER_JSON_F, value);
            _verifyNameRoundTrip(SMALL_COPY_BUFFER_JSON_F, value);
        }
        // and surrogate pair straddling the small copy buffer boundary
        String value = _repeat('x', 99) + "🫡";
        _verifyStringRoundTrip(SMALL_COPY_BUFFER_JSON_F, value);
        _verifyNameRoundTrip(SMALL_COPY_BUFFER_JSON_F, value);
    }

    private void _verifyStringRoundTrip(JsonFactory f, String value) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            g.writeStartArray();
            g.writeString(value);
            g.writeEndArray();
        }
        assertEquals("[\"" + _escaped(value) + "\"]",
                new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), bytes.toByteArray())) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(value, p.getString());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
        }
    }

    private void _verifyNameRoundTrip(JsonFactory f, String name) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            g.writeStartObject();
            g.writeName(name);
            g.writeNumber(1);
            g.writeEndObject();
        }
        assertEquals("{\"" + _escaped(name) + "\":1}",
                new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), bytes.toByteArray())) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(name, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
        }
    }

    private static String _escaped(String value) {
        return value.replace("\"", "\\\"");
    }

    private static String _repeat(char c, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }
}
