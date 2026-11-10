package tools.jackson.core.unittest.read;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1705]: Raw (non-escaped) UTF-16 surrogates in ReaderBasedJsonParser
 * must be validated for both property names and string values.
 * Valid surrogate pairs (high surrogate + low surrogate) representing supplementary Unicode
 * characters must be accepted, while lone high, lone low, reversed pairs, and high surrogates
 * followed by non-low characters must be rejected with StreamReadException.
 */
class RawSurrogateValidation1705Test extends JacksonCoreTestBase
{
    private final static int MODE_STRING = -1;

    private final static int[] MODES = new int[] {
        MODE_STRING, MODE_READER, MODE_READER_THROTTLED
    };

    private final static int INPUT_BUFFER_LEN = 4000;

    // Raw UTF-16 code units for U+1F600 (GRINNING FACE: \uD83D \uDE00)
    private final static String RAW_HI = "\uD83D";
    private final static String RAW_LO = "\uDE00";
    private final static String RAW_PAIR = "\uD83D\uDE00";

    // Lone surrogates
    private final static String RAW_LONE_HI = "\uD800";
    private final static String RAW_LONE_LO = "\uDC00";

    private final JsonFactory FACTORY = new JsonFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // =======================================================================
    // 1. String values - valid cases
    // =======================================================================

    @Test
    void validSurrogatePairInStringValue() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "[\"" + RAW_PAIR + "\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(RAW_PAIR, p.getString(), "mode=" + mode);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void validSurrogatePairWithSurroundingTextInStringValue() throws Exception {
        String expected = "Hello " + RAW_PAIR + " World!";
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "[\"" + expected + "\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(expected, p.getString(), "mode=" + mode);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void validSurrogatePairInSingleQuotedStringValue() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(APOS_FACTORY, mode, "['" + RAW_PAIR + "']")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(RAW_PAIR, p.getString(), "mode=" + mode);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    // =======================================================================
    // 2. String values - invalid cases
    // =======================================================================

    @Test
    void loneHighSurrogateInStringValue() throws Exception {
        assertRejects(FACTORY, "[\"" + RAW_LONE_HI + "\"]");
    }

    @Test
    void loneLowSurrogateInStringValue() throws Exception {
        assertRejects(FACTORY, "[\"" + RAW_LONE_LO + "\"]");
    }

    @Test
    void reversedSurrogatePairInStringValue() throws Exception {
        assertRejects(FACTORY, "[\"" + RAW_LONE_LO + RAW_LONE_HI + "\"]");
    }

    @Test
    void highSurrogateFollowedByOrdinaryCharInStringValue() throws Exception {
        assertRejects(FACTORY, "[\"" + RAW_LONE_HI + "A\"]");
    }

    @Test
    void highSurrogateFollowedByClosingQuoteInStringValue() throws Exception {
        assertRejects(FACTORY, "[\"" + RAW_LONE_HI + "\"]");
    }

    @Test
    void loneHighSurrogateInSingleQuotedStringValue() throws Exception {
        assertRejects(APOS_FACTORY, "['" + RAW_LONE_HI + "']");
    }

    @Test
    void loneLowSurrogateInSingleQuotedStringValue() throws Exception {
        assertRejects(APOS_FACTORY, "['" + RAW_LONE_LO + "']");
    }

    // =======================================================================
    // 3. Property names - valid cases
    // =======================================================================

    @Test
    void validSurrogatePairInPropertyName() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"" + RAW_PAIR + "\":1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals(RAW_PAIR, p.currentName(), "mode=" + mode);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void validSurrogatePairWithSurroundingTextInPropertyName() throws Exception {
        String name = "key_" + RAW_PAIR + "_prop";
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"" + name + "\":1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals(name, p.currentName(), "mode=" + mode);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void validSurrogatePairInSingleQuotedPropertyName() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(APOS_FACTORY, mode, "{'" + RAW_PAIR + "':1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals(RAW_PAIR, p.currentName(), "mode=" + mode);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    // =======================================================================
    // 4. Property names - invalid cases
    // =======================================================================

    @Test
    void loneHighSurrogateInPropertyName() throws Exception {
        assertRejects(FACTORY, "{\"" + RAW_LONE_HI + "\":1}");
    }

    @Test
    void loneLowSurrogateInPropertyName() throws Exception {
        assertRejects(FACTORY, "{\"" + RAW_LONE_LO + "\":1}");
    }

    @Test
    void reversedSurrogatePairInPropertyName() throws Exception {
        assertRejects(FACTORY, "{\"" + RAW_LONE_LO + RAW_LONE_HI + "\":1}");
    }

    @Test
    void highSurrogateFollowedByOrdinaryCharInPropertyName() throws Exception {
        assertRejects(FACTORY, "{\"" + RAW_LONE_HI + "x\":1}");
    }

    @Test
    void loneHighSurrogateInSingleQuotedPropertyName() throws Exception {
        assertRejects(APOS_FACTORY, "{'" + RAW_LONE_HI + "':1}");
    }

    @Test
    void loneLowSurrogateInSingleQuotedPropertyName() throws Exception {
        assertRejects(APOS_FACTORY, "{'" + RAW_LONE_LO + "':1}");
    }

    // =======================================================================
    // 5. Input-buffer boundary coverage
    // =======================================================================

    @Test
    void validSurrogatePairSplitAcrossInputBuffersInStringValue() throws Exception {
        // Place high surrogate at the last slot of the first 4000-char input buffer
        final String lead = "[\"" + pad(INPUT_BUFFER_LEN - 2 - 1) + RAW_HI;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            try (JsonParser p = open(FACTORY, mode, lead + RAW_LO + "\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                String val = p.getString();
                assertEquals(0x1F600, val.codePointAt(val.length() - 2), "mode=" + mode);
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void loneHighSurrogateAtInputBufferEndInStringValue() throws Exception {
        final String lead = "[\"" + pad(INPUT_BUFFER_LEN - 2 - 1) + RAW_HI;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            // High surrogate followed by ordinary char in next buffer
            try (JsonParser p = open(FACTORY, mode, lead + "x\"]")) {
                p.nextToken();
                p.nextToken();
                p.getString();
                fail("Expected StreamReadException (mode=" + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "surrogate");
            }
            // High surrogate at end of input
            try (JsonParser p = open(FACTORY, mode, lead)) {
                p.nextToken();
                p.nextToken();
                p.getString();
                fail("Expected StreamReadException (mode=" + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "end-of-input");
            }
        }
    }

    @Test
    void validSurrogatePairSplitAcrossInputBuffersInPropertyName() throws Exception {
        final String lead = "{\"" + pad(INPUT_BUFFER_LEN - 2 - 1) + RAW_HI;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            try (JsonParser p = open(FACTORY, mode, lead + RAW_LO + "\":1}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                String name = p.currentName();
                assertEquals(0x1F600, name.codePointAt(name.length() - 2), "mode=" + mode);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void loneHighSurrogateAtInputBufferEndInPropertyName() throws Exception {
        final String lead = "{\"" + pad(INPUT_BUFFER_LEN - 2 - 1) + RAW_HI;
        assertEquals(INPUT_BUFFER_LEN, lead.length());

        for (int mode : ALL_TEXT_MODES) {
            try (JsonParser p = open(FACTORY, mode, lead + "x\":1}")) {
                p.nextToken();
                p.nextToken();
                fail("Expected StreamReadException (mode=" + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "surrogate");
            }
            try (JsonParser p = open(FACTORY, mode, lead)) {
                p.nextToken();
                p.nextToken();
                fail("Expected StreamReadException (mode=" + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "end-of-input");
            }
        }
    }

    // =======================================================================
    // 6. Skipping & streaming
    // =======================================================================

    @Test
    void skipChildrenWithValidSurrogates() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"outer\":{\"k\":\"" + RAW_PAIR + "\"}}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                p.skipChildren();
                assertToken(JsonToken.END_OBJECT, p.currentToken());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    @Test
    void skipChildrenRejectsLoneSurrogates() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "{\"outer\":{\"k\":\"" + RAW_LONE_HI + "\"}}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                try {
                    p.skipChildren();
                    fail("Expected StreamReadException for skipChildren with lone surrogate (mode=" + mode + ")");
                } catch (StreamReadException e) {
                    verifyException(e, "surrogate");
                }
            }
        }
    }

    @Test
    void streamStringWithValidSurrogates() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "[\"" + RAW_PAIR + "\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                StringWriter sw = new StringWriter();
                long count = p.getString(sw);
                assertEquals(2L, count);
                assertEquals(RAW_PAIR, sw.toString());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void streamStringRejectsLoneSurrogates() throws Exception {
        for (int mode : MODES) {
            try (JsonParser p = open(FACTORY, mode, "[\"" + RAW_LONE_HI + "x\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                StringWriter sw = new StringWriter();
                try {
                    p.getString(sw);
                    fail("Expected StreamReadException (mode=" + mode + ")");
                } catch (StreamReadException e) {
                    verifyException(e, "surrogate");
                }
            }
        }
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private JsonParser open(JsonFactory f, int mode, String doc) throws Exception {
        if (mode == MODE_STRING) {
            return f.createParser(ObjectReadContext.empty(), doc);
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
                while (p.nextToken() != null) {
                    // Force complete decoding of strings
                    p.getString();
                }
                fail("Expected StreamReadException for malformed surrogate (mode="
                        + mode + ") in: " + doc);
            } catch (StreamReadException e) {
                verifyException(e, "surrogate");
            }
        }
    }
}
