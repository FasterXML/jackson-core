package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1683]: JSON-escaped lone surrogates in field names
 * must be rejected by the Reader-based parser, matching the fix applied for
 * the UTF-8 stream parser in [jackson-core#1541]. String values are
 * (as with UTF-8 parser) not validated, and must pass through as-is.
 *
 * <p>Only exercises {@link com.fasterxml.jackson.core.json.ReaderBasedJsonParser}
 * paths. Every test runs over {@link #MODES}: {@code String} input (whole
 * content in one buffer), plain {@code Reader} and 1-char-at-a-time throttled
 * {@code Reader}; the latter forces every escape to span input-buffer boundaries.
 */
class EscapedSurrogateInFieldName1683Test extends JUnit5TestBase
{
    // Local pseudo-mode for `JsonFactory.createParser(String)`, in addition
    // to Reader-backed ALL_TEXT_MODES
    private final static int MODE_STRING = -1;

    private final static int[] MODES = new int[] {
        MODE_STRING, MODE_READER, MODE_READER_THROTTLED
    };

    // Size of char input buffer `ReaderBasedJsonParser` reads into
    // (BufferRecycler.CHAR_TOKEN_BUFFER)
    private final static int INPUT_BUFFER_LEN = 4000;

    // 6-char escape for high surrogate of U+1F600
    private final static String HI_ESCAPE = "\\uD83D";
    private final static String LO_ESCAPE = "\\uDE00";

    private final JsonFactory FACTORY = new JsonFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // ---- field-name coverage ----------------------------------------------

    // Each backslash-u escape is written using an explicit '\\' + 'u' + hex
    // prefix so the Java pre-lexer doesn't fold it into a single UTF-16 code
    // unit before it reaches the parser.

    @Test
    void loneLeadingSurrogateInFieldName() throws Exception {
        assertRejects(FACTORY, "{\"\\uD800\":1}");
    }

    @Test
    void loneTrailingSurrogateInFieldName() throws Exception {
        assertRejects(FACTORY, "{\"\\uDC00\":1}");
    }

    @Test
    void reversedSurrogatePairInFieldName() throws Exception {
        assertRejects(FACTORY, "{\"\\uDC00\\uD800\":1}");
    }

    @Test
    void highSurrogateFollowedByNonEscapeInFieldName() throws Exception {
        assertRejects(FACTORY, "{\"\\uD800x\":1}");
    }

    @Test
    void validSurrogatePairInFieldName() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"\\uD800\\uDC00\":1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertEquals("\uD800\uDC00", p.currentName(), "mode=" + mode);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void loneSurrogateInSingleQuotedFieldName() throws Exception {
        assertRejects(APOS_FACTORY, "{'\\uDC00':1}");
    }

    // ---- string values: not validated -------------------------------------

    @Test
    void escapedSurrogatesInStringValuePassThrough() throws Exception {
        _testStringValuePassThrough(FACTORY, "[\"\\uD800\",\"\\uDC00\",\"\\uDC00\\uD800\",\"\\uD800\\uDC00\"]");
    }

    @Test
    void escapedSurrogatesInSingleQuotedStringValuePassThrough() throws Exception {
        _testStringValuePassThrough(APOS_FACTORY, "['\\uD800','\\uDC00','\\uDC00\\uD800','\\uD800\\uDC00']");
    }

    @Test
    void escapedSurrogatesInSkippedStringValue() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"outer\":{\"k\":\"\\uDC00\\uD800\"}}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                p.skipChildren();
                assertToken(JsonToken.END_OBJECT, p.currentToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    private void _testStringValuePassThrough(JsonFactory f, String doc) throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(f, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uD800", p.getText(), "mode=" + mode);
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uDC00", p.getText(), "mode=" + mode);
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uDC00\uD800", p.getText(), "mode=" + mode);
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uD800\uDC00", p.getText(), "mode=" + mode);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    // ---- text-buffer segment-boundary coverage ----------------------------

    /**
     * Regression test for the buffer overflow that would occur if the high
     * surrogate write in {@code _parseName2} landed on the last slot of the
     * current text-buffer segment. Without the bounds check between the hi
     * and lo writes, the fall-through path would then write the low
     * surrogate at index {@code outBuf.length}, throwing
     * {@link ArrayIndexOutOfBoundsException} instead of returning a valid
     * astral field name.
     *
     * <p>The default first-segment size in {@code TextBuffer} is 200 UTF-16
     * code units; we sweep several padding sizes so at least one lands the
     * high-surrogate write on {@code outBuf.length - 1}.
     */
    @Test
    void longFieldNameWithSurrogatePairAtSegmentBoundary() throws Exception {
        int[] padSizes = { 199, 200, 201, 399, 400, 401, 4000 };
        for (int mode : MODES) {
            for (int pad : padSizes) {
                String doc = "{\"" + pad(pad) + HI_ESCAPE + LO_ESCAPE + "\":1}";
                try (JsonParser p = open(FACTORY, mode, doc)) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                    String name = p.currentName();
                    assertEquals(pad + 1, name.codePointCount(0, name.length()),
                            "codepoint count for pad=" + pad + ", mode=" + mode);
                    assertEquals(0x1F600, name.codePointAt(pad),
                            "astral cp at index " + pad + " for pad=" + pad + ", mode=" + mode);
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                }
            }
        }
    }

    @Test
    void longFieldNameWithLoneTrailingSurrogateAtSegmentBoundary() throws Exception {
        // Same padding sweep, but lone-trailing-surrogate escape — must still
        // reject cleanly at the boundary rather than overflowing.
        int[] padSizes = { 199, 200, 201, 399, 400, 401, 4000 };
        for (int pad : padSizes) {
            assertRejects(FACTORY, "{\"" + pad(pad) + "\\uDC00\":1}");
        }
    }

    // ---- input-buffer boundary coverage -----------------------------------

    /**
     * High-surrogate escape ending exactly at the end of the first input
     * buffer, so the following low-surrogate escape (or other char, or EOF)
     * is only seen after {@code _loadMore()}. Applies to Reader-backed input
     * only: String input is never refilled.
     */
    @Test
    void highSurrogateAtInputBufferEndInFieldName() throws Exception {
        final String lead = "{\"" + pad(INPUT_BUFFER_LEN - 2 - HI_ESCAPE.length()) + HI_ESCAPE;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            // Valid pair split across buffers: accepted
            try (JsonParser p = open(FACTORY, mode, lead + LO_ESCAPE + "\":1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                String name = p.currentName();
                assertEquals(0x1F600, name.codePointAt(name.length() - 2), "mode=" + mode);
            }
            // High surrogate followed by non-escape char in next buffer
            try (JsonParser p = open(FACTORY, mode, lead + "x\":1}")) {
                p.nextToken();
                p.nextToken();
                fail("Expected JsonParseException (mode=" + mode + ")");
            } catch (JsonParseException e) {
                verifyException(e, "surrogate");
            }
            // High surrogate as the very last content
            try (JsonParser p = open(FACTORY, mode, lead)) {
                p.nextToken();
                p.nextToken();
                fail("Expected JsonParseException (mode=" + mode + ")");
            } catch (JsonParseException e) {
                verifyException(e, "end-of-input");
            }
        }
    }

    // ---- helpers ----------------------------------------------------------

    private JsonParser open(JsonFactory f, int mode, String doc) throws Exception {
        if (mode == MODE_STRING) {
            return f.createParser(doc);
        }
        return createParser(f, mode, doc);
    }

    private static String pad(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    private void assertRejects(JsonFactory f, String doc) throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(f, mode, doc)) {
                while (p.nextToken() != null) { }
                fail("Expected JsonParseException for malformed surrogate escape (mode="
                        + mode + ") in: " + doc);
            } catch (JsonParseException e) {
                verifyException(e, "surrogate");
            }
        }
    }
}
