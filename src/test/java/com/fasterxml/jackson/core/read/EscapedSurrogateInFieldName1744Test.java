package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1744]: JSON-escaped surrogate pairs in field names
 * must be decoded by all blocking parsers, including {@code UTF8DataInputJsonParser}.
 */
class EscapedSurrogateInFieldName1744Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // U+1F600 GRINNING FACE
    private static final String SMILEY = "\uD83D\uDE00";
    private static final String ESC_SMILEY = "\\uD83D\\uDE00";

    @Test
    void escapedSurrogatePairInFieldName() throws Exception
    {
        for (int mode : ALL_MODES) {
            // vary prefix length to cover all quad alignments
            for (String prefix : new String[] { "", "a", "ab", "abc", "abcd", "\u00e9", "\u20ac" }) {
                _testName(FACTORY, mode, '"', prefix + ESC_SMILEY + "xyz", prefix + SMILEY + "xyz");
            }
            _testName(FACTORY, mode, '"', ESC_SMILEY + ESC_SMILEY, SMILEY + SMILEY);
            _testName(FACTORY, mode, '"', "\\ud834\\udd1e", "\uD834\uDD1E");
        }
    }

    @Test
    void escapedSurrogatePairInAposFieldName() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String prefix : new String[] { "", "a", "ab", "abc", "abcd" }) {
                _testName(APOS_FACTORY, mode, '\'', prefix + ESC_SMILEY + "xyz", prefix + SMILEY + "xyz");
            }
        }
    }

    @Test
    void brokenSurrogatePairInFieldName() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            // high surrogate not followed by escape
            _testBroken(FACTORY, mode, "{\"\\uD83Dx\":1}", "Broken surrogate pair");
            // high surrogate followed by non-low-surrogate escape
            _testBroken(FACTORY, mode, "{\"\\uD83D\\u0041\":1}", "Broken surrogate pair");
            // lone low surrogate
            _testBroken(FACTORY, mode, "{\"\\uDE00\":1}", "Unexpected low surrogate");
            _testBroken(APOS_FACTORY, mode, "{'\\uD83Dx':1}", "Broken surrogate pair");
        }
    }

    private void _testName(JsonFactory f, int mode, char q, String escName, String expName)
        throws Exception
    {
        String doc = "{" + q + escName + q + ":1}";
        try (JsonParser p = createParser(f, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(expName, p.currentName(), "mode " + mode);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private void _testBroken(JsonFactory f, int mode, String doc, String expMsg)
        throws Exception
    {
        try (JsonParser p = createParser(f, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            p.nextToken();
            fail("Should not pass, mode " + mode);
        } catch (StreamReadException e) {
            verifyException(e, expMsg);
        }
    }
}
