package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1756]: blocking byte-based parsers must reject
 * ill-formed (overlong, invalid lead byte) UTF-8 after a backslash, instead of
 * decoding it as a different character; same as non-blocking parsers
 * (see {@code AsyncEscapedMultiByteChar1756Test}).
 */
class EscapedMultiByteChar1756Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory ANY_ESCAPE = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

    // U+1F600 GRINNING FACE
    private static final String SMILEY = "😀";

    @Test
    void escapedOverlongUTF8Rejected() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            // 0xC0 and 0xC1 can only start overlong 2-byte encodings
            for (JsonFactory f : new JsonFactory[] { FACTORY, ANY_ESCAPE }) {
                _testBroken(f, mode, utf8Bytes("[\"\\", new int[] { 0xC0, 0xA2 }, "\"]"),
                        "Invalid UTF-8 start byte 0xc0");
                _testBroken(f, mode, utf8Bytes("{\"\\", new int[] { 0xC1, 0x9C }, "\":1}"),
                        "Invalid UTF-8 start byte 0xc1");
            }
            // 3-byte encoding of '"', 4-byte encodings of '\'' and NUL
            _testBroken(ANY_ESCAPE, mode, utf8Bytes("[\"\\", new int[] { 0xE0, 0x80, 0xA2 }, "\"]"),
                    "Invalid UTF-8: overlong 3-byte encoding of 0x22");
            _testBroken(ANY_ESCAPE, mode, utf8Bytes("{\"\\", new int[] { 0xE0, 0x80, 0xA2 }, "\":1}"),
                    "Invalid UTF-8: overlong 3-byte encoding of 0x22");
            _testBroken(APOS_FACTORY, mode, utf8Bytes("['\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, "']"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x27");
            _testBroken(APOS_FACTORY, mode, utf8Bytes("{'\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, "':1}"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x27");
            _testBroken(ANY_ESCAPE, mode, utf8Bytes("[\"\\", new int[] { 0xF0, 0x80, 0x80, 0x80 }, "\"]"),
                    "Invalid UTF-8: overlong 4-byte encoding of 0x0");
        }
    }

    // 0xF5 - 0xF7 can only start encodings beyond U+10FFFF
    @Test
    void escapedLeadByteBeyondUnicodeRejected() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (int lead : new int[] { 0xF5, 0xF6, 0xF7 }) {
                final String exp = "Invalid UTF-8 start byte 0x" + Integer.toHexString(lead);
                _testBroken(ANY_ESCAPE, mode, utf8Bytes("[\"\\", new int[] { lead, 0x80, 0x80, 0x80 }, "\"]"),
                        exp);
                _testBroken(ANY_ESCAPE, mode, utf8Bytes("{\"\\", new int[] { lead, 0x80, 0x80, 0x80 }, "\":1}"),
                        exp);
            }
        }
    }

    // Rejected escaped supplementary character should be reported at its lead byte
    // (DataInput-backed parser does not track offsets)
    @Test
    void escapedCharErrorLocation() throws Exception
    {
        _testErrorLocation(ANY_ESCAPE, SMILEY);
        _testErrorLocation(FACTORY, SMILEY);
    }

    private void _testErrorLocation(JsonFactory f, String ch) throws Exception
    {
        for (int mode : new int[] { MODE_INPUT_STREAM, MODE_INPUT_STREAM_THROTTLED }) {
            for (String doc : new String[] { "{\"\\" + ch + "\":1}", "[\"\\" + ch + "\"]" }) {
                try (JsonParser p = createParser(f, mode, utf8Bytes(doc))) {
                    while (p.nextToken() != null) {
                        p.getText();
                    }
                    fail("Should not pass for mode " + mode);
                } catch (StreamReadException e) {
                    verifyException(e, "Unrecognized character escape");
                    assertEquals(3L, e.getLocation().getByteOffset());
                    assertEquals(1, e.getLocation().getLineNr());
                    assertEquals(4, e.getLocation().getColumnNr());
                }
            }
        }
    }

    private void _testBroken(JsonFactory f, int mode, byte[] doc, String expMsg) throws Exception
    {
        try (JsonParser p = createParser(f, mode, doc)) {
            while (p.nextToken() != null) {
                p.getText();
            }
            fail("Should not pass for mode " + mode);
        } catch (StreamReadException e) {
            verifyException(e, expMsg);
        }
    }

}
