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

    // Long enough (> 64 bytes as UTF-8) to require growing quad buffer
    @Test
    void longNameWithEscapedSurrogatePairs() throws Exception
    {
        StringBuilder esc = new StringBuilder();
        StringBuilder exp = new StringBuilder();
        for (int i = 0; i < 40; ++i) {
            esc.append(ESC_SMILEY);
            exp.append(SMILEY);
        }
        for (int mode : ALL_MODES) {
            for (String prefix : new String[] { "", "a", "ab", "abc" }) {
                _testName(FACTORY, mode, '"', prefix + esc, prefix + exp);
                _testName(APOS_FACTORY, mode, '\'', prefix + esc, prefix + exp);
            }
        }
    }

    // Backslash-escaped raw supplementary character must not be truncated to 16 bits
    @Test
    void backslashEscapedSupplementaryCharInFieldName() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();
        // U+1D800 truncates to 0xD800, a high surrogate
        final String odd = new String(Character.toChars(0x1D800));
        for (int mode : ALL_MODES) {
            _testName(f, mode, '"', "\\" + SMILEY, SMILEY);
            _testName(f, mode, '"', "a\\" + SMILEY + "b", "a" + SMILEY + "b");
            _testName(f, mode, '"', "\\" + odd, odd);
            _testName(f, mode, '"', ESC_SMILEY + "\\" + odd, SMILEY + odd);
            // and escaped low surrogate must not be faked either (U+1DC00 -> 0xDC00)
            _testBroken(f, mode, "{\"\\uD83D\\" + new String(Character.toChars(0x1DC00)) + "\":1}",
                    "Broken surrogate pair");
        }
    }

    // Same for String values
    @Test
    void backslashEscapedSupplementaryCharInValue() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        final String odd = new String(Character.toChars(0x1D800));
        // long enough to span multiple text buffer segments
        StringBuilder esc = new StringBuilder();
        StringBuilder exp = new StringBuilder();
        for (int i = 0; i < 3000; ++i) {
            esc.append("a\\").append(SMILEY);
            exp.append('a').append(SMILEY);
        }
        for (int mode : ALL_MODES) {
            for (char q : new char[] { '"', '\'' }) {
                _testValue(f, mode, q, "\\" + SMILEY, SMILEY);
                _testValue(f, mode, q, "a\\" + SMILEY + "b", "a" + SMILEY + "b");
                _testValue(f, mode, q, "\\" + odd, odd);
                _testValue(f, mode, q, esc.toString(), exp.toString());
            }
        }
    }

    // Validation must use full code point, not truncated one: U+10027 truncates to '\''
    @Test
    void backslashEscapedSupplementaryCharNotAllowed() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        final String ch = new String(Character.toChars(0x10027));
        for (int mode : ALL_MODES) {
            _testBroken(f, mode, "{\"\\" + ch + "\":1}", "Unrecognized character escape");
            _testBroken(f, mode, "[\"\\" + ch + "\"]", "Unrecognized character escape");
            _testBroken(FACTORY, mode, "{\"\\" + SMILEY + "\":1}", "Unrecognized character escape");
        }
    }

    // Invalid UTF-8 after backslash must not get through
    @Test
    void backslashEscapedInvalidUTF8() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();
        // beyond U+10FFFF
        final int[] tooBig = { 0xF4, 0x90, 0x80, 0x80 };
        // CESU-8 style encoded high surrogate U+D83D
        final int[] cesu = { 0xED, 0xA0, 0xBD };
        for (int mode : ALL_BINARY_MODES) {
            _testBroken(f, mode, concat("{\"\\", tooBig, "\":1}"), "Invalid UTF-8");
            _testBroken(f, mode, concat("[\"\\", tooBig, "\"]"), "Invalid UTF-8");
            _testBroken(f, mode, concat("{\"\\", cesu, "\\uDE00\":1}"), "Invalid UTF-8");
            _testBroken(f, mode, concat("[\"\\", cesu, "\\uDE00\"]"), "Invalid UTF-8");
        }
    }

    @Test
    void brokenSurrogatePairInFieldName() throws Exception
    {
        for (int mode : ALL_MODES) {
            // high surrogate not followed by escape
            _testBroken(FACTORY, mode, "{\"\\uD83Dx\":1}", "Broken surrogate pair");
            // high surrogate right before closing quote
            _testBroken(FACTORY, mode, "{\"\\uD83D\":1}", "Broken surrogate pair");
            // high surrogate followed by non-low-surrogate escape
            _testBroken(FACTORY, mode, "{\"\\uD83D\\u0041\":1}", "Broken surrogate pair");
            // high surrogate followed by another high surrogate
            _testBroken(FACTORY, mode, "{\"\\uD83D\\uD83D\":1}", "Broken surrogate pair");
            // lone low surrogate
            _testBroken(FACTORY, mode, "{\"\\uDE00\":1}", "Unexpected low surrogate");
            _testBroken(APOS_FACTORY, mode, "{'\\uD83Dx':1}", "Broken surrogate pair");
            _testBroken(APOS_FACTORY, mode, "{'\\uDE00':1}", "Unexpected low surrogate");
            // hex-escaped high surrogate followed by raw low surrogate
            _testBroken(FACTORY, mode, "{\"\\uD83D\uDE00\":1}", "Broken surrogate pair");
        }
    }

    @Test
    void eofWithinSurrogatePairInFieldName() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testBroken(FACTORY, mode, "{\"\\uD83D", "Unexpected end-of-input");
            _testBroken(FACTORY, mode, "{\"\\uD83D\\", "Unexpected end-of-input");
            _testBroken(FACTORY, mode, "{\"\\uD83D\\uDE", "Unexpected end-of-input");
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

    private void _testValue(JsonFactory f, int mode, char q, String escValue, String expValue)
        throws Exception
    {
        String doc = "[" + q + escValue + q + "]";
        try (JsonParser p = createParser(f, mode, doc)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(expValue, p.getText(), "mode " + mode);
            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private void _testBroken(JsonFactory f, int mode, String doc, String expMsg)
        throws Exception
    {
        _testBroken(createParser(f, mode, doc), mode, expMsg);
    }

    private void _testBroken(JsonFactory f, int mode, byte[] doc, String expMsg)
        throws Exception
    {
        _testBroken(createParser(f, mode, doc), mode, expMsg);
    }

    private void _testBroken(JsonParser parser, int mode, String expMsg) throws Exception
    {
        try (JsonParser p = parser) {
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    p.getText();
                }
            }
            fail("Should not pass, mode " + mode);
        } catch (StreamReadException e) {
            verifyException(e, expMsg);
        }
    }

    private static byte[] concat(String a, int[] bytes, String b) {
        byte[] ab = utf8Bytes(a), bb = utf8Bytes(b);
        byte[] result = new byte[ab.length + bytes.length + bb.length];
        System.arraycopy(ab, 0, result, 0, ab.length);
        for (int i = 0; i < bytes.length; ++i) {
            result[ab.length + i] = (byte) bytes[i];
        }
        System.arraycopy(bb, 0, result, ab.length + bytes.length, bb.length);
        return result;
    }
}
