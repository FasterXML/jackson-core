package com.fasterxml.jackson.core.async;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1756]: non-blocking parsers must decode multi-byte
 * UTF-8 characters after a backslash the same way as blocking parsers
 * (see [jackson-core#1744]), regardless of where input is split.
 */
class AsyncEscapedMultiByteChar1756Test extends AsyncTestBase
{
    enum Variant {
        BYTE_ARRAY, BYTE_BUFFER;

        AsyncReaderWrapper wrap(JsonFactory f, int bytesPerRead, byte[] data)
            throws IOException
        {
            switch (this) {
            case BYTE_ARRAY: return asyncForBytes(f, bytesPerRead, data, 0);
            case BYTE_BUFFER: return asyncForByteBuffer(f, bytesPerRead, data, 0);
            default: throw new IllegalStateException();
            }
        }
    }

    interface Check {
        void check(AsyncReaderWrapper r) throws Exception;
    }

    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory ANY_ESCAPE = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

    private final JsonFactory ANY_ESCAPE_APOS = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // 2-byte and 3-byte UTF-8, including highest BMP character (not U+FFFD,
    // which lossy decoding would produce)
    private static final String[] BMP_CHARS = { "é", "€", "中", "\uFFFF" };

    // vary quad alignment for names
    private static final String[] PREFIXES = { "", "a", "ab", "abc", "abcd", "abcdefghijklm" };

    // long suffix so that whole-buffer input takes the non-split ("fast") path
    private static final String[] SUFFIXES = { "", "z", "zzzzzzzzzz" };

    // U+1F600 GRINNING FACE
    private static final String SMILEY = "😀";

    /*
    /**********************************************************************
    /* Test methods, decoding
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedBmpCharInValue(Variant v) throws Exception
    {
        for (String ch : BMP_CHARS) {
            for (String prefix : PREFIXES) {
                for (String suffix : SUFFIXES) {
                    final String esc = prefix + "\\" + ch + suffix;
                    final String exp = prefix + ch + suffix;
                    Check check = r -> {
                        assertToken(JsonToken.START_ARRAY, r.nextToken());
                        assertToken(JsonToken.VALUE_STRING, r.nextToken());
                        assertEquals(exp, r.currentText());
                        assertToken(JsonToken.END_ARRAY, r.nextToken());
                    };
                    _forAllSplits(v, ANY_ESCAPE, utf8Bytes("[\"" + esc + "\"]"), check);
                    _forAllSplits(v, ANY_ESCAPE_APOS, utf8Bytes("['" + esc + "']"), check);
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedBmpCharInName(Variant v) throws Exception
    {
        for (String ch : BMP_CHARS) {
            for (String prefix : PREFIXES) {
                for (String suffix : SUFFIXES) {
                    final String esc = prefix + "\\" + ch + suffix;
                    final String exp = prefix + ch + suffix;
                    Check check = r -> {
                        assertToken(JsonToken.START_OBJECT, r.nextToken());
                        assertToken(JsonToken.FIELD_NAME, r.nextToken());
                        assertEquals(exp, r.currentName());
                        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                        assertToken(JsonToken.END_OBJECT, r.nextToken());
                    };
                    _forAllSplits(v, ANY_ESCAPE, utf8Bytes("{\"" + esc + "\":1}"), check);
                    _forAllSplits(v, ANY_ESCAPE_APOS, utf8Bytes("{'" + esc + "':1}"), check);
                }
            }
        }
    }

    // Escaped multi-byte char right after a high surrogate escape is not a valid pair
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedBmpCharAfterHighSurrogateInName(Variant v) throws Exception
    {
        _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\uD83D\\é\":1}"), "Broken surrogate pair");
    }

    /*
    /**********************************************************************
    /* Test methods, rejection
    /**********************************************************************
     */

    // Without the feature, escaped char is reported as decoded, not by its lead byte
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedBmpCharRejectedByDefault(Variant v) throws Exception
    {
        for (String suffix : SUFFIXES) {
            _testBroken(v, FACTORY, utf8Bytes("[\"\\é" + suffix + "\"]"),
                    "Unrecognized character escape 'é' (code 233");
            _testBroken(v, FACTORY, utf8Bytes("{\"\\€" + suffix + "\":1}"),
                    "Unrecognized character escape '€' (code 8364");
        }
    }

    // Supplementary character cannot be decoded as single char: must be rejected,
    // not truncated (U+10027 would become apostrophe)
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedSupplementaryCharRejected(Variant v) throws Exception
    {
        final String apos = new String(Character.toChars(0x10027));
        for (String suffix : SUFFIXES) {
            for (JsonFactory f : new JsonFactory[] { FACTORY, ANY_ESCAPE }) {
                // full character in message, not truncated to 16 bits
                _testBroken(v, f, utf8Bytes("{\"\\" + SMILEY + suffix + "\":1}"),
                        "Unrecognized character escape '" + SMILEY + "' (code 128512 / 0x1f600)");
                _testBroken(v, f, utf8Bytes("[\"\\" + SMILEY + suffix + "\"]"),
                        "Unrecognized character escape '" + SMILEY + "' (code 128512 / 0x1f600)");
            }
            _testBroken(v, APOS_FACTORY, utf8Bytes("{'\\" + apos + suffix + "':1}"),
                    "Unrecognized character escape");
            _testBroken(v, APOS_FACTORY, utf8Bytes("['\\" + apos + suffix + "']"),
                    "Unrecognized character escape");
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedInvalidUTF8Rejected(Variant v) throws Exception
    {
        // beyond U+10FFFF
        final int[] tooBig = { 0xF4, 0x90, 0x80, 0x80 };
        // CESU-8 style encoded high surrogate U+D83D
        final int[] cesu = { 0xED, 0xA0, 0xBD };
        for (String suffix : SUFFIXES) {
            _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", tooBig, suffix + "\":1}"),
                    "Unrecognized character escape");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", tooBig, suffix + "\"]"),
                    "Unrecognized character escape");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", cesu, "\\uDE00" + suffix + "\":1}"),
                    "Unrecognized character escape");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", cesu, "\\uDE00" + suffix + "\"]"),
                    "Unrecognized character escape");

            // continuation byte expected but not found
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xC3, 0x41 }, suffix + "\"]"),
                    "Invalid UTF-8 middle byte 0x41");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { 0xE2, 0x82, 0x41 }, suffix + "\":1}"),
                    "Invalid UTF-8 middle byte 0x41");
            // not a valid lead byte
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0x80 }, suffix + "\"]"),
                    "Invalid UTF-8 start byte 0x80");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { 0xF8 }, suffix + "\":1}"),
                    "Invalid UTF-8 start byte 0xf8");
        }
    }

    // Overlong encodings must not be accepted as (different) character, especially
    // not as quote, apostrophe or backslash
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedOverlongUTF8Rejected(Variant v) throws Exception
    {
        for (String suffix : SUFFIXES) {
            // 0xC0 and 0xC1 can only start overlong 2-byte encodings
            for (JsonFactory f : new JsonFactory[] { FACTORY, ANY_ESCAPE }) {
                _testBroken(v, f, utf8Bytes("[\"\\", new int[] { 0xC0, 0xA2 }, suffix + "\"]"),
                        "Invalid UTF-8 start byte 0xc0");
                _testBroken(v, f, utf8Bytes("{\"\\", new int[] { 0xC1, 0x9C }, suffix + "\":1}"),
                        "Invalid UTF-8 start byte 0xc1");
            }
            // 3-byte encoding of '"', 4-byte encodings of '\'' and NUL
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xE0, 0x80, 0xA2 }, suffix + "\"]"),
                    "Invalid UTF-8: overlong 3-byte encoding of 0x22");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { 0xE0, 0x80, 0xA2 }, suffix + "\":1}"),
                    "Invalid UTF-8: overlong 3-byte encoding of 0x22");
            _testBroken(v, APOS_FACTORY, utf8Bytes("['\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, suffix + "']"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x27");
            _testBroken(v, APOS_FACTORY, utf8Bytes("{'\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, suffix + "':1}"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x27");
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xF0, 0x80, 0x80, 0x80 }, suffix + "\"]"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x0");
            // 3-byte encoding of 'é' (needs only 2 bytes)
            _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xE0, 0x83, 0xA9 }, suffix + "\"]"),
                    "Invalid UTF-8: overlong 3-byte encoding of 0xe9");
        }
    }

    // 0xF5 - 0xF7 can only start encodings beyond U+10FFFF
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedLeadByteBeyondUnicodeRejected(Variant v) throws Exception
    {
        for (String suffix : SUFFIXES) {
            for (int lead : new int[] { 0xF5, 0xF6, 0xF7 }) {
                final String exp = "Invalid UTF-8 start byte 0x" + Integer.toHexString(lead);
                _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { lead, 0x80, 0x80, 0x80 }, suffix + "\"]"),
                        exp);
                _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { lead, 0x80, 0x80, 0x80 }, suffix + "\":1}"),
                        exp);
            }
        }
    }

    // Rejected escaped character should be reported at its lead byte: supplementary
    // one always, BMP one if escaping any character not enabled
    @ParameterizedTest
    @EnumSource(Variant.class)
    void escapedCharErrorLocation(Variant v) throws Exception
    {
        _testErrorLocation(v, ANY_ESCAPE, SMILEY);
        _testErrorLocation(v, FACTORY, "€");
        _testErrorLocation(v, FACTORY, "é");
    }

    private void _testErrorLocation(Variant v, JsonFactory f, String ch) throws Exception
    {
        for (String doc : new String[] { "{\"\\" + ch + "\":1}", "[\"\\" + ch + "\"]" }) {
            final byte[] data = utf8Bytes(doc);
            for (int bytesPerRead = 1; bytesPerRead <= data.length; ++bytesPerRead) {
                try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, data)) {
                    while (r.nextToken() != null) {
                        r.currentText();
                    }
                    fail("Should not pass with bytesPerRead=" + bytesPerRead);
                } catch (StreamReadException e) {
                    verifyException(e, "Unrecognized character escape");
                    final String desc = "bytesPerRead=" + bytesPerRead + ", doc " + doc;
                    assertEquals(3L, e.getLocation().getByteOffset(), desc);
                    assertEquals(1, e.getLocation().getLineNr(), desc);
                    assertEquals(4, e.getLocation().getColumnNr(), desc);
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void eofWithinEscapedMultiByteChar(Variant v) throws Exception
    {
        _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xC3 }, ""), "Unexpected end-of-input");
        _testBroken(v, ANY_ESCAPE, utf8Bytes("[\"\\", new int[] { 0xE2, 0x82 }, ""), "Unexpected end-of-input");
        _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { 0xC3 }, ""), "Unexpected end-of-input");
        _testBroken(v, ANY_ESCAPE, utf8Bytes("{\"\\", new int[] { 0xE2, 0x82 }, ""), "Unexpected end-of-input");
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    // Feeds input in chunks of every size from 1 byte to whole document
    private void _forAllSplits(Variant v, JsonFactory f, byte[] doc, Check check)
        throws Exception
    {
        for (int bytesPerRead = 1; bytesPerRead <= doc.length; ++bytesPerRead) {
            try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, doc)) {
                check.check(r);
            } catch (Exception | AssertionError e) {
                throw new AssertionError("Failed with bytesPerRead=" + bytesPerRead
                        + " for doc of " + doc.length + " bytes: " + e, e);
            }
        }
    }

    private void _testBroken(Variant v, JsonFactory f, byte[] doc, String expMsg)
        throws Exception
    {
        for (int bytesPerRead = 1; bytesPerRead <= doc.length; ++bytesPerRead) {
            try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, doc)) {
                JsonToken t;
                while ((t = r.nextToken()) != null) {
                    if (t == JsonToken.VALUE_STRING) {
                        r.currentText();
                    }
                }
                fail("Should not pass with bytesPerRead=" + bytesPerRead);
            } catch (StreamReadException e) {
                try {
                    verifyException(e, expMsg);
                } catch (AssertionError ae) {
                    throw new AssertionError("bytesPerRead=" + bytesPerRead + ": " + ae.getMessage(), e);
                }
            }
        }
    }

}
