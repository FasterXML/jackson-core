package com.fasterxml.jackson.core.read;

import java.io.StringReader;

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
 * paths; UTF-8 / async parsers are covered separately.
 */
class EscapedSurrogateInStringValue1683Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = new JsonFactory();

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

    // ---- string-value coverage --------------------------------------------

    @Test
    void loneLeadingSurrogateInStringValue_stringInput() throws Exception {
        assertRejects(LONE_LEADING_VALUE, /*fromString=*/true);
    }

    @Test
    void loneLeadingSurrogateInStringValue_readerInput() throws Exception {
        assertRejects(LONE_LEADING_VALUE, /*fromString=*/false);
    }

    @Test
    void loneTrailingSurrogateInStringValue_stringInput() throws Exception {
        assertRejects(LONE_TRAILING_VALUE, /*fromString=*/true);
    }

    @Test
    void loneTrailingSurrogateInStringValue_readerInput() throws Exception {
        assertRejects(LONE_TRAILING_VALUE, /*fromString=*/false);
    }

    @Test
    void reversedSurrogatePairInStringValue_stringInput() throws Exception {
        assertRejects(REVERSED_PAIR_VALUE, /*fromString=*/true);
    }

    @Test
    void reversedSurrogatePairInStringValue_readerInput() throws Exception {
        assertRejects(REVERSED_PAIR_VALUE, /*fromString=*/false);
    }

    @Test
    void validSurrogatePairInStringValue_stringInput() throws Exception {
        assertAcceptsValidPair(VALID_PAIR_VALUE, /*fromString=*/true);
    }

    @Test
    void validSurrogatePairInStringValue_readerInput() throws Exception {
        assertAcceptsValidPair(VALID_PAIR_VALUE, /*fromString=*/false);
    }

    // ---- field-name coverage ----------------------------------------------

    @Test
    void loneTrailingSurrogateInFieldName_stringInput() throws Exception {
        assertRejects(LONE_TRAILING_NAME, /*fromString=*/true);
    }

    @Test
    void loneTrailingSurrogateInFieldName_readerInput() throws Exception {
        assertRejects(LONE_TRAILING_NAME, /*fromString=*/false);
    }

    @Test
    void loneLeadingSurrogateInFieldName_stringInput() throws Exception {
        assertRejects(LONE_LEADING_NAME, /*fromString=*/true);
    }

    @Test
    void loneLeadingSurrogateInFieldName_readerInput() throws Exception {
        assertRejects(LONE_LEADING_NAME, /*fromString=*/false);
    }

    @Test
    void reversedSurrogatePairInFieldName_stringInput() throws Exception {
        assertRejects(REVERSED_PAIR_NAME, /*fromString=*/true);
    }

    @Test
    void reversedSurrogatePairInFieldName_readerInput() throws Exception {
        assertRejects(REVERSED_PAIR_NAME, /*fromString=*/false);
    }

    // ---- single-quoted string coverage (exercises _handleApos) ------------

    private static final String APOS_LONE_LEADING   = "{'k':'\\uD800'}";
    private static final String APOS_LONE_TRAILING  = "{'k':'\\uDC00'}";
    private static final String APOS_REVERSED_PAIR  = "{'k':'\\uDC00\\uD800'}";
    private static final String APOS_VALID_PAIR     = "{'k':'\\uD800\\uDC00'}";

    private JsonFactory factoryWithSingleQuotes() {
        return JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
    }

    @Test
    void singleQuotedStringWithLoneLeadingSurrogate_stringInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_LONE_LEADING, /*fromString=*/true);
    }

    @Test
    void singleQuotedStringWithLoneLeadingSurrogate_readerInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_LONE_LEADING, /*fromString=*/false);
    }

    @Test
    void singleQuotedStringWithLoneTrailingSurrogate_stringInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_LONE_TRAILING, /*fromString=*/true);
    }

    @Test
    void singleQuotedStringWithLoneTrailingSurrogate_readerInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_LONE_TRAILING, /*fromString=*/false);
    }

    @Test
    void singleQuotedStringWithReversedSurrogatePair_stringInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_REVERSED_PAIR, /*fromString=*/true);
    }

    @Test
    void singleQuotedStringWithReversedSurrogatePair_readerInput() throws Exception {
        assertRejects(factoryWithSingleQuotes(), APOS_REVERSED_PAIR, /*fromString=*/false);
    }

    @Test
    void singleQuotedStringWithValidSurrogatePair_stringInput() throws Exception {
        assertAcceptsValidPair(factoryWithSingleQuotes(), APOS_VALID_PAIR, /*fromString=*/true);
    }

    @Test
    void singleQuotedStringWithValidSurrogatePair_readerInput() throws Exception {
        assertAcceptsValidPair(factoryWithSingleQuotes(), APOS_VALID_PAIR, /*fromString=*/false);
    }

    // ---- skipChildren coverage (exercises _skipString) --------------------

    @Test
    void skipChildrenPastMalformedString_fromString() throws Exception {
        String doc = "{\"outer\":{\"k\":\"\\uDC00\"}}";
        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            try {
                p.skipChildren();
                fail("expected JsonParseException");
            } catch (JsonParseException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                assertTrue(msg.contains("surrogate"),
                        "expected surrogate error, got: " + e.getMessage());
            }
        }
    }

    @Test
    void skipChildrenPastMalformedString_fromReader() throws Exception {
        String doc = "{\"outer\":{\"k\":\"\\uDC00\"}}";
        try (JsonParser p = FACTORY.createParser(new StringReader(doc))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            try {
                p.skipChildren();
                fail("expected JsonParseException");
            } catch (JsonParseException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                assertTrue(msg.contains("surrogate"),
                        "expected surrogate error, got: " + e.getMessage());
            }
        }
    }

    @Test
    void skipChildrenPastMalformedString_highSurrogateFollowedByNonEscape() throws Exception {
        // High surrogate followed by a plain character rather than another escape
        String doc = "{\"outer\":{\"k\":\"\\uD800x\"}}";
        try (JsonParser p = FACTORY.createParser(new StringReader(doc))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            try {
                p.skipChildren();
                fail("expected JsonParseException");
            } catch (JsonParseException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                assertTrue(msg.contains("surrogate"),
                        "expected surrogate error, got: " + e.getMessage());
            }
        }
    }

    // ---- segment-boundary coverage (exercises _parseName2 bounds check) ---

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
    void longFieldNameWithSurrogatePairAtSegmentBoundary_readerInput() throws Exception {
        // Sweep sizes so that at least one lands the hi write on the segment boundary
        int[] padSizes = { 199, 200, 201, 399, 400, 401, 4000 };
        for (int pad : padSizes) {
            StringBuilder sb = new StringBuilder("{\"");
            for (int i = 0; i < pad; i++) sb.append('a');
            sb.append("\\uD83D\\uDE00\":1}");
            String doc = sb.toString();
            try (JsonParser p = FACTORY.createParser(new StringReader(doc))) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                String name = p.currentName();
                int expectedCodePoints = pad + 1;
                assertEquals(expectedCodePoints, name.codePointCount(0, name.length()),
                        "codepoint count for pad=" + pad);
                assertEquals(0x1F600, name.codePointAt(pad),
                        "astral cp at index " + pad + " for pad=" + pad);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void longFieldNameWithLoneTrailingSurrogateAtSegmentBoundary_readerInput() throws Exception {
        // Same padding sweep, but lone-trailing-surrogate escape — must still
        // reject cleanly at the boundary rather than overflowing.
        int[] padSizes = { 199, 200, 201, 399, 400, 401, 4000 };
        for (int pad : padSizes) {
            StringBuilder sb = new StringBuilder("{\"");
            for (int i = 0; i < pad; i++) sb.append('a');
            sb.append("\\uDC00\":1}");
            String doc = sb.toString();
            try (JsonParser p = FACTORY.createParser(new StringReader(doc))) {
                try {
                    while (p.nextToken() != null) {
                        if (p.currentToken() == JsonToken.FIELD_NAME) {
                            p.getText();
                        }
                    }
                    fail("expected JsonParseException for lone trailing surrogate at pad=" + pad);
                } catch (JsonParseException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    assertTrue(msg.contains("surrogate"),
                            "expected surrogate error at pad=" + pad + ", got: " + e.getMessage());
                }
            }
        }
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
        String doc = "{\"k\":\"\\uD800" + '\uDC00' + "\"}";
        for (boolean fromString : new boolean[]{true, false}) {
            try (JsonParser p = open(doc, fromString)) {
                try {
                    while (p.nextToken() != null) { p.getText(); }
                    fail("expected JsonParseException (fromString=" + fromString + ")");
                } catch (JsonParseException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    assertTrue(msg.contains("surrogate"),
                            "expected surrogate error, got: " + e.getMessage());
                }
            }
        }
    }

    @Test
    void literalHighFollowedByEscapedLowSurrogate_isRejected() throws Exception {
        String doc = "{\"k\":\"" + '\uD800' + "\\uDC00\"}";
        for (boolean fromString : new boolean[]{true, false}) {
            try (JsonParser p = open(doc, fromString)) {
                try {
                    while (p.nextToken() != null) { p.getText(); }
                    fail("expected JsonParseException (fromString=" + fromString + ")");
                } catch (JsonParseException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    assertTrue(msg.contains("surrogate"),
                            "expected surrogate error, got: " + e.getMessage());
                }
            }
        }
    }

    @Test
    void bothLiteralSurrogatesFormingValidPair_isAccepted() throws Exception {
        // Literal char pair — parser does not decode escapes, so no surrogate
        // guard fires. This exercises the pre-existing accept path for
        // literal chars in the input stream and locks that we do not
        // regress it while validating the escape paths.
        String doc = "{\"k\":\"" + '\uD800' + '\uDC00' + "\"}";
        for (boolean fromString : new boolean[]{true, false}) {
            try (JsonParser p = open(doc, fromString)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                String v = p.getText();
                assertEquals(0x10000, v.codePointAt(0),
                        "expected U+10000 from literal surrogate pair (fromString=" + fromString + ")");
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    // ---- helpers ----------------------------------------------------------

    private JsonParser open(String doc, boolean fromString) throws Exception {
        return open(FACTORY, doc, fromString);
    }

    private JsonParser open(JsonFactory factory, String doc, boolean fromString) throws Exception {
        return fromString
                ? factory.createParser(doc)
                : factory.createParser(new StringReader(doc));
    }

    private void assertRejects(String doc, boolean fromString) throws Exception {
        assertRejects(FACTORY, doc, fromString);
    }

    private void assertRejects(JsonFactory factory, String doc, boolean fromString) throws Exception {
        try (JsonParser p = open(factory, doc, fromString)) {
            try {
                // Drive tokens until either the parser throws or the doc ends.
                while (p.nextToken() != null) {
                    if (p.currentToken() == JsonToken.VALUE_STRING
                            || p.currentToken() == JsonToken.FIELD_NAME) {
                        // Force decode; some paths defer surrogate work until
                        // the caller pulls the text.
                        p.getText();
                    }
                }
                fail("Expected JsonParseException for malformed surrogate escape in: " + doc);
            } catch (JsonParseException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                assertTrue(msg.contains("surrogate"),
                        "Expected surrogate error message, got: " + e.getMessage());
            }
        }
    }

    private void assertAcceptsValidPair(String doc, boolean fromString) throws Exception {
        assertAcceptsValidPair(FACTORY, doc, fromString);
    }

    private void assertAcceptsValidPair(JsonFactory factory, String doc, boolean fromString) throws Exception {
        try (JsonParser p = open(factory, doc, fromString)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("k", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            String text = p.getText();
            // U+10000 encodes as the surrogate pair D800 DC00 in Java strings.
            assertEquals(2, text.length(), "expected two UTF-16 code units");
            int cp = text.codePointAt(0);
            assertEquals(0x10000, cp,
                    "expected code point U+10000, got U+" + Integer.toHexString(cp));
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
