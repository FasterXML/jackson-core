package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1683]: JSON-escaped lone surrogates in string values
 * (and field names) must be rejected by the Reader-based parser, matching the
 * fix applied for the UTF-8 stream parser in [jackson-core#1541].
 *
 * <p>Only exercises {@link com.fasterxml.jackson.core.json.ReaderBasedJsonParser}
 * paths; UTF-8 / async parsers are covered separately. Every test runs over
 * {@link #MODES}: {@code String} input (whole content in one buffer), plain
 * {@code Reader} and 1-char-at-a-time throttled {@code Reader}; the latter
 * forces every escape to span input-buffer boundaries.
 */
class EscapedSurrogateInStringValue1683Test extends JUnit5TestBase
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

    // JSON documents. Each backslash-u escape is written using an explicit
    // '\\' + 'u' + hex prefix so the Java pre-lexer doesn't fold it into a
    // single UTF-16 code unit before it reaches the parser.
    private static final String LONE_LEADING_VALUE   = "{\"k\":\"\\uD800\"}";
    private static final String LONE_TRAILING_VALUE  = "{\"k\":\"\\uDC00\"}";
    private static final String REVERSED_PAIR_VALUE  = "{\"k\":\"\\uDC00\\uD800\"}";
    private static final String VALID_PAIR_VALUE     = "{\"k\":\"\\uD800\\uDC00\"}";
    private static final String LONE_TRAILING_NAME   = "{\"\\uDC00\":1}";
    private static final String LONE_LEADING_NAME    = "{\"\\uD800\":1}";
    private static final String REVERSED_PAIR_NAME   = "{\"\\uDC00\\uD800\":1}";

    private static final String APOS_LONE_LEADING   = "{'k':'\\uD800'}";
    private static final String APOS_LONE_TRAILING  = "{'k':'\\uDC00'}";
    private static final String APOS_REVERSED_PAIR  = "{'k':'\\uDC00\\uD800'}";
    private static final String APOS_VALID_PAIR     = "{'k':'\\uD800\\uDC00'}";

    // ---- string-value coverage --------------------------------------------

    @Test
    void loneLeadingSurrogateInStringValue() throws Exception {
        assertRejects(FACTORY, LONE_LEADING_VALUE);
    }

    @Test
    void loneTrailingSurrogateInStringValue() throws Exception {
        assertRejects(FACTORY, LONE_TRAILING_VALUE);
    }

    @Test
    void reversedSurrogatePairInStringValue() throws Exception {
        assertRejects(FACTORY, REVERSED_PAIR_VALUE);
    }

    @Test
    void validSurrogatePairInStringValue() throws Exception {
        assertAcceptsValidPair(FACTORY, VALID_PAIR_VALUE);
    }

    // ---- field-name coverage ----------------------------------------------

    @Test
    void loneTrailingSurrogateInFieldName() throws Exception {
        assertRejects(FACTORY, LONE_TRAILING_NAME);
    }

    @Test
    void loneLeadingSurrogateInFieldName() throws Exception {
        assertRejects(FACTORY, LONE_LEADING_NAME);
    }

    @Test
    void reversedSurrogatePairInFieldName() throws Exception {
        assertRejects(FACTORY, REVERSED_PAIR_NAME);
    }

    // ---- single-quoted string coverage (exercises _handleApos) ------------

    @Test
    void singleQuotedStringWithLoneLeadingSurrogate() throws Exception {
        assertRejects(APOS_FACTORY, APOS_LONE_LEADING);
    }

    @Test
    void singleQuotedStringWithLoneTrailingSurrogate() throws Exception {
        assertRejects(APOS_FACTORY, APOS_LONE_TRAILING);
    }

    @Test
    void singleQuotedStringWithReversedSurrogatePair() throws Exception {
        assertRejects(APOS_FACTORY, APOS_REVERSED_PAIR);
    }

    @Test
    void singleQuotedStringWithValidSurrogatePair() throws Exception {
        assertAcceptsValidPair(APOS_FACTORY, APOS_VALID_PAIR);
    }

    // ---- skipChildren coverage (exercises _skipString) --------------------

    @Test
    void skipChildrenPastLoneTrailingSurrogate() throws Exception {
        assertSkipChildrenRejects("{\"outer\":{\"k\":\"\\uDC00\"}}");
    }

    @Test
    void skipChildrenPastHighSurrogateFollowedByNonEscape() throws Exception {
        // High surrogate followed by a plain character rather than another escape
        assertSkipChildrenRejects("{\"outer\":{\"k\":\"\\uD800x\"}}");
    }

    // ---- text-buffer segment-boundary coverage (exercises _parseName2) ----

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
    //
    // High-surrogate escape ending exactly at the end of the first input
    // buffer, so the following low-surrogate escape (or other char, or EOF)
    // is only seen after `_loadMore()`. Applies to Reader-backed input only:
    // String input is never refilled.

    @Test
    void highSurrogateAtInputBufferEnd_fieldName() throws Exception {
        _testHighSurrogateAtInputBufferEnd(FACTORY, "{\"", "\":1}", false);
    }

    @Test
    void highSurrogateAtInputBufferEnd_stringValue() throws Exception {
        _testHighSurrogateAtInputBufferEnd(FACTORY, "[\"", "\"]", false);
    }

    @Test
    void highSurrogateAtInputBufferEnd_aposStringValue() throws Exception {
        _testHighSurrogateAtInputBufferEnd(APOS_FACTORY, "['", "']", false);
    }

    @Test
    void highSurrogateAtInputBufferEnd_skippedStringValue() throws Exception {
        _testHighSurrogateAtInputBufferEnd(FACTORY, "[\"", "\"]", true);
    }

    private void _testHighSurrogateAtInputBufferEnd(JsonFactory f,
            String prefix, String suffix, boolean skip) throws Exception
    {
        final String lead = prefix + pad(INPUT_BUFFER_LEN - prefix.length() - HI_ESCAPE.length())
                + HI_ESCAPE;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            // Valid pair split across buffers: accepted
            try (JsonParser p = open(f, mode, lead + LO_ESCAPE + suffix)) {
                String text = _readFirstString(p, skip);
                if (!skip) {
                    assertEquals(0x1F600, text.codePointAt(text.length() - 2), "mode=" + mode);
                }
            }
            // High surrogate followed by non-escape char in next buffer
            try (JsonParser p = open(f, mode, lead + "x" + suffix)) {
                _readFirstString(p, skip);
                fail("Expected JsonParseException (mode=" + mode + ")");
            } catch (JsonParseException e) {
                verifyException(e, "surrogate");
            }
            // High surrogate as the very last content
            try (JsonParser p = open(f, mode, lead)) {
                _readFirstString(p, skip);
                fail("Expected JsonParseException (mode=" + mode + ")");
            } catch (JsonParseException e) {
                verifyException(e, "end-of-input");
            }
        }
    }

    // Returns first field name or String value; or, if `skip`, skips value
    // (without accessing text) and returns null
    private String _readFirstString(JsonParser p, boolean skip) throws Exception
    {
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT) {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            return p.currentName();
        }
        assertToken(JsonToken.START_ARRAY, t);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (skip) {
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            return null;
        }
        return p.getText();
    }

    // ---- mixed escape / literal-surrogate coverage (locks §5.6 behavior) --

    /**
     * The Reader path can accept a Java {@code String} input that contains a
     * literal (unescaped) surrogate {@code char}. Verify the deliberate
     * strict-rejection behavior when an escape surrogate is followed by, or
     * preceded by, a literal surrogate char: both cases MUST reject with
     * {@code JsonParseException}, even though a lenient reader might treat
     * the escape+literal combination as a valid UTF-16 pair.
     */
    @Test
    void escapedHighFollowedByLiteralLowSurrogate_isRejected() throws Exception {
        assertRejects(FACTORY, "{\"k\":\"\\uD800" + '\uDC00' + "\"}");
    }

    @Test
    void literalHighFollowedByEscapedLowSurrogate_isRejected() throws Exception {
        assertRejects(FACTORY, "{\"k\":\"" + '\uD800' + "\\uDC00\"}");
    }

    @Test
    void bothLiteralSurrogatesFormingValidPair_isAccepted() throws Exception {
        // Literal char pair — parser does not decode escapes, so no surrogate
        // guard fires. This exercises the pre-existing accept path for
        // literal chars in the input stream and locks that we do not
        // regress it while validating the escape paths.
        String doc = "{\"k\":\"" + '\uD800' + '\uDC00' + "\"}";
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, doc)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(0x10000, p.getText().codePointAt(0),
                        "expected U+10000 from literal surrogate pair (mode=" + mode + ")");
                assertToken(JsonToken.END_OBJECT, p.nextToken());
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
                // Drive tokens until either the parser throws or the doc ends.
                while (p.nextToken() != null) {
                    if (p.currentToken() == JsonToken.VALUE_STRING
                            || p.currentToken() == JsonToken.FIELD_NAME) {
                        // Force decode; some paths defer surrogate work until
                        // the caller pulls the text.
                        p.getText();
                    }
                }
                fail("Expected JsonParseException for malformed surrogate escape (mode="
                        + mode + ") in: " + doc);
            } catch (JsonParseException e) {
                verifyException(e, "surrogate");
            }
        }
    }

    private void assertSkipChildrenRejects(String doc) throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, doc)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                p.skipChildren();
                fail("Expected JsonParseException (mode=" + mode + ")");
            } catch (JsonParseException e) {
                verifyException(e, "surrogate");
            }
        }
    }

    private void assertAcceptsValidPair(JsonFactory f, String doc) throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(f, mode, doc)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertEquals("k", p.currentName());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                String text = p.getText();
                // U+10000 encodes as the surrogate pair D800 DC00 in Java strings.
                assertEquals(2, text.length(), "expected two UTF-16 code units (mode=" + mode + ")");
                assertEquals(0x10000, text.codePointAt(0), "mode=" + mode);
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }
}
