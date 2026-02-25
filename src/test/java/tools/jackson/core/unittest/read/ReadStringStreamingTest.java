package tools.jackson.core.unittest.read;

import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonParser#readString(Writer)} streaming functionality
 * (added for [core#1288]).
 *
 * @since 3.1
 */
class ReadStringStreamingTest extends JacksonCoreTestBase
{
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    // Size of the intermediate output buffer used by _streamString
    private static final int OUT_BUF_SIZE = 1024;

    /*
    /**********************************************************************
    /* Empty and trivial strings
    /**********************************************************************
     */

    @Test
    void emptyString() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testEmptyString(mode);
        }
    }

    private void _testEmptyString(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_FACTORY, mode, "[\"\"]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals(0L, len);
        assertEquals("", w.toString());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    @Test
    void singleCharString() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testSingleCharString(mode);
        }
    }

    private void _testSingleCharString(int mode) throws Exception
    {
        try (JsonParser p = createParser(JSON_FACTORY, mode, "[\"x\"]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
    
            Writer w = new StringWriter();
            assertEquals(1L, p.readString(w));
            assertEquals("x", w.toString());
        }
    }

    /*
    /**********************************************************************
    /* Escape sequences
    /**********************************************************************
     */

    @Test
    void commonEscapeSequences() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testCommonEscapeSequences(mode);
        }
    }

    private void _testCommonEscapeSequences(int mode) throws Exception
    {
        // \", \\, \/, \b, \f, \n, \r, \t
        String json = "[\"\\\"  \\\\  \\/  \\b  \\f  \\n  \\r  \\t\"]";
        String expected = "\"  \\  /  \b  \f  \n  \r  \t";

        try (JsonParser p = createParser(JSON_FACTORY, mode, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
        
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString());
            assertEquals((long) expected.length(), len);
        }
    }

    @Test
    void unicodeEscapeSequences() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testUnicodeEscapeSequences(mode);
        }
    }

    private void _testUnicodeEscapeSequences(int mode) throws Exception
    {
        // \u00e9 = é, \u4e2d = 中, \u0000 = NUL
        String json = "[\"caf\\u00e9 \\u4e2d \\u0000\"]";
        String expected = "caf\u00e9 \u4e2d \u0000";

        try (JsonParser p = createParser(JSON_FACTORY, mode, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
    
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString());
            assertEquals((long) expected.length(), len);
        }
    }

    @Test
    void escapesAtOutputBufferBoundary() throws Exception
    {
        // Put an escape sequence right at the 512-char flush boundary to
        // ensure the flush-and-continue logic handles partially-seen escapes.
        for (int mode : ALL_MODES) {
            _testEscapesAtOutputBufferBoundary(mode);
        }
    }

    private void _testEscapesAtOutputBufferBoundary(int mode) throws Exception
    {
        // Fill to one char before the buffer flush boundary, then place a \n escape
        String prefix = "x".repeat(OUT_BUF_SIZE - 1);
        String json = "[\"" + prefix + "\\n\"]";
        String expected = prefix + "\n";

        try (JsonParser p = createParser(JSON_FACTORY, mode, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
    
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString());
            assertEquals((long) expected.length(), len);
        }
    }

    /*
    /**********************************************************************
    /* Multi-byte UTF-8 (exercises binary-parser code paths)
    /**********************************************************************
     */

    @Test
    void twoByteUtf8() throws Exception
    {
        // é = U+00E9, encoded as 2 bytes in UTF-8
        String value = "caf\u00e9";
        for (int mode : ALL_BINARY_MODES) {
            _testUtf8Value(mode, value);
        }
    }

    @Test
    void threeByteUtf8() throws Exception
    {
        // 中 = U+4E2D, encoded as 3 bytes in UTF-8
        String value = "\u4e2d\u6587";
        for (int mode : ALL_BINARY_MODES) {
            _testUtf8Value(mode, value);
        }
    }

    @Test
    void surrogateRoundTrip() throws Exception
    {
        // 😀 = U+1F600, encoded as 4 bytes in UTF-8, represented as surrogate pair
        // in Java: \uD83D\uDE00
        String value = "\uD83D\uDE00 emoji \uD83D\uDE00";
        for (int mode : ALL_BINARY_MODES) {
            _testUtf8Value(mode, value);
        }
    }

    @Test
    void surrogateAtOutputBufferBoundary() throws Exception
    {
        // Place a surrogate-pair character right at the output buffer boundary
        // to exercise the mid-surrogate flush path in _streamString.
        // Surrogate pair takes 2 Java chars; put one char before flush boundary
        // so the high surrogate falls exactly at position OUT_BUF_SIZE-1.
        String prefix = "x".repeat(OUT_BUF_SIZE - 1);
        String value = prefix + "\uD83D\uDE00" + "tail";
        for (int mode : ALL_BINARY_MODES) {
            _testUtf8Value(mode, value);
        }
    }

    private void _testUtf8Value(int mode, String value) throws Exception
    {
        String json = "[" + q(value) + "]";
        JsonParser p = createParser(JSON_FACTORY, mode, json);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals(value, w.toString(), "mode=" + mode);
        assertEquals((long) value.length(), len, "mode=" + mode);

        p.close();
    }

    /*
    /**********************************************************************
    /* Output-buffer boundary sizes
    /**********************************************************************
     */

    @Test
    void stringExactlyAtOutputBufferSize() throws Exception
    {
        // exactly 512 chars: buffer fills completely, then closing quote arrives
        _testStringOfLength(OUT_BUF_SIZE);
    }

    @Test
    void stringOneOverOutputBufferSize() throws Exception
    {
        _testStringOfLength(OUT_BUF_SIZE + 1);
    }

    @Test
    void stringTwoFullOutputBuffers() throws Exception
    {
        _testStringOfLength(OUT_BUF_SIZE * 2);
    }

    @Test
    void stringTwoFullOutputBuffersPlusOne() throws Exception
    {
        _testStringOfLength(OUT_BUF_SIZE * 2 + 1);
    }

    private void _testStringOfLength(int length) throws Exception
    {
        String value = "a".repeat(length);
        String json = "[" + q(value) + "]";
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_FACTORY, mode, json);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());

            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals((long) length, len, "mode=" + mode + ", length=" + length);
            assertEquals(value, w.toString(), "mode=" + mode + ", length=" + length);

            p.close();
        }
    }

    /*
    /**********************************************************************
    /* Constraint enforcement at boundaries
    /**********************************************************************
     */

    @Test
    void constraintAtExactLimit() throws Exception
    {
        // A string of exactly maxLen chars must NOT throw
        for (int mode : ALL_MODES) {
            _testConstraintAtExactLimit(mode);
        }
    }

    private void _testConstraintAtExactLimit(int mode) throws Exception
    {
        final int maxLen = 1000;
        String value = "x".repeat(maxLen);
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(maxLen).build())
                .build();

        JsonParser p = createParser(f, mode, "[" + q(value) + "]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals((long) maxLen, len, "mode=" + mode);
        assertEquals(value, w.toString(), "mode=" + mode);

        p.close();
    }

    @Test
    void constraintOneLargerThanFlushBoundary() throws Exception
    {
        // maxLen set to OUT_BUF_SIZE - 1, so violation is detected at first flush
        for (int mode : ALL_MODES) {
            _testConstraintOneLargerThanFlushBoundary(mode);
        }
    }

    private void _testConstraintOneLargerThanFlushBoundary(int mode) throws Exception
    {
        final int maxLen = OUT_BUF_SIZE - 1;
        String value = "x".repeat(OUT_BUF_SIZE + 100); // clearly over limit
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(maxLen).build())
                .build();

        JsonParser p = createParser(f, mode, "[" + q(value) + "]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        Writer w = new StringWriter();
        assertThrows(StreamConstraintsException.class, () -> p.readString(w), "mode=" + mode);

        p.close();
    }

    @Test
    void constraintExactlyAtFlushBoundary() throws Exception
    {
        // maxLen set to OUT_BUF_SIZE, so a string of OUT_BUF_SIZE+1 triggers it
        for (int mode : ALL_MODES) {
            _testConstraintExactlyAtFlushBoundary(mode);
        }
    }

    private void _testConstraintExactlyAtFlushBoundary(int mode) throws Exception
    {
        final int maxLen = OUT_BUF_SIZE;
        String value = "x".repeat(OUT_BUF_SIZE + 1);
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(maxLen).build())
                .build();

        JsonParser p = createParser(f, mode, "[" + q(value) + "]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        Writer w = new StringWriter();
        assertThrows(StreamConstraintsException.class, () -> p.readString(w), "mode=" + mode);

        p.close();
    }

    /*
    /**********************************************************************
    /* Other token types
    /**********************************************************************
     */

    @Test
    void readStringOnPropertyName() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testReadStringOnPropertyName(mode);
        }
    }

    private void _testReadStringOnPropertyName(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_FACTORY, mode, "{\"myKey\":\"myValue\"}");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());

        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals("myKey", w.toString());
        assertEquals(5L, len);

        p.close();
    }

    @Test
    void readStringOnNumberTokens() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testReadStringOnNumberTokens(mode);
        }
    }

    private void _testReadStringOnNumberTokens(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_FACTORY, mode, "[42, 3.14]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        Writer w = new StringWriter();
        p.readString(w);
        assertEquals("42", w.toString());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        w = new StringWriter();
        p.readString(w);
        assertEquals("3.14", w.toString());

        p.close();
    }

    @Test
    void readStringOnNullToken() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testReadStringOnNullToken(mode);
        }
    }

    private void _testReadStringOnNullToken(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_FACTORY, mode, "[null]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals("null", w.toString());
        assertEquals(4L, len);

        p.close();
    }

    @Test
    void readStringBeforeFirstToken() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testReadStringBeforeFirstToken(mode);
        }
    }

    private void _testReadStringBeforeFirstToken(int mode) throws Exception
    {
        // No nextToken() called yet: current token is null, should return 0
        JsonParser p = createParser(JSON_FACTORY, mode, "[\"x\"]");
        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals(0L, len);
        assertEquals("", w.toString());
        p.close();
    }

    /*
    /**********************************************************************
    /* Consuming semantics and sequential calls
    /**********************************************************************
     */

    @Test
    void consumingSemantics() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testConsumingSemantics(mode);
        }
    }

    private void _testConsumingSemantics(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_FACTORY, mode, "[\"hello\",\"world\"]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        // First value
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        Writer w = new StringWriter();
        long len = p.readString(w);
        assertEquals("hello", w.toString());
        assertEquals(5L, len);
        // getString() after readString() must return empty
        assertEquals("", p.getString());

        // Second value -- parser must have advanced past "hello"
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        w = new StringWriter();
        len = p.readString(w);
        assertEquals("world", w.toString());
        assertEquals(5L, len);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    @Test
    void multipleStringsInArray() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testMultipleStringsInArray(mode);
        }
    }

    private void _testMultipleStringsInArray(int mode) throws Exception
    {
        String[] values = {"first", "second", "third"};
        String json = "[\"first\",\"second\",\"third\"]";

        JsonParser p = createParser(JSON_FACTORY, mode, json);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        for (String expected : values) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString(), "mode=" + mode);
            assertEquals((long) expected.length(), len, "mode=" + mode);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    /*
    /**********************************************************************
    /* Mixed content (ASCII + escapes + multi-byte)
    /**********************************************************************
     */

    @Test
    void mixedContent() throws Exception
    {
        // ASCII, escape sequences, and non-ASCII chars all in one string
        String expected = "Hello \n\t\u00e9 \u4e2d\u6587 \uD83D\uDE00 end";
        // Build the JSON manually so it round-trips correctly
        String json = "[\"Hello \\n\\t\\u00e9 \\u4e2d\\u6587 \\uD83D\\uDE00 end\"]";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_FACTORY, mode, json);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());

            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString(), "mode=" + mode);
            assertEquals((long) expected.length(), len, "mode=" + mode);

            p.close();
        }
    }

    @Test
    void longMixedContent() throws Exception
    {
        // Long string (> OUT_BUF_SIZE) with interspersed non-ASCII chars.
        // Control characters are excluded since q() doesn't escape them;
        // those code paths are covered by the dedicated escape tests.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("ASCII_block_");
            sb.append("\u00e9"); // 2-byte UTF-8
            sb.append("\u4e2d"); // 3-byte UTF-8
            sb.append("\uD83D\uDE00"); // surrogate pair (4-byte UTF-8)
        }
        String expected = sb.toString();
        // Use getString() to get the parser's own encoding of the value,
        // then verify readString() produces identical output.
        String json = "[" + q(expected) + "]";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_FACTORY, mode, json);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());

            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected.length(), w.toString().length(), "mode=" + mode);
            assertEquals(expected, w.toString(), "mode=" + mode);
            assertEquals((long) expected.length(), len, "mode=" + mode);

            p.close();
        }
    }

    /*
    /**********************************************************************
    /* Throttled (input buffer boundary) mode
    /**********************************************************************
     */

    @Test
    void escapeSplitAcrossInputChunks() throws Exception
    {
        // In throttled mode (1 byte at a time), escape sequences will be split
        // across input loads. Verify correctness with all escape types.
        String json = "[\"\\n\\t\\r\\\"\\\\\\u00e9\"]";
        String expected = "\n\t\r\"\\\u00e9";

        // Use throttled stream mode (reads 1 byte at a time)
        try (JsonParser p = createParser(JSON_FACTORY, MODE_INPUT_STREAM_THROTTLED, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
    
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(expected, w.toString());
            assertEquals((long) expected.length(), len);
        }
    }

    @Test
    void longStringThrottled() throws Exception
    {
        // Long string in throttled mode: stresses input-buffer-boundary logic
        String value = "abcdefghij".repeat(200); // 2000 chars
        String json = "[" + q(value) + "]";

        try (JsonParser p = createParser(JSON_FACTORY, MODE_INPUT_STREAM_THROTTLED, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
    
            Writer w = new StringWriter();
            long len = p.readString(w);
            assertEquals(value, w.toString());
            assertEquals(2000L, len);
        }
    }
}
