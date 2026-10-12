package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1756]: blocking byte-based parsers must reject
 * ill-formed (overlong, invalid lead byte) UTF-8 after a backslash, instead of
 * decoding it as a different character; same as non-blocking parsers
 * (see {@code AsyncEscapedMultiByteChar1756Test}).
 */
class EscapedMultiByteChar1756Test extends JacksonCoreTestBase
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
                    "Invalid UTF-8: overlong encoding (lead byte 0xe0, second byte 0x80)");
            _testBroken(ANY_ESCAPE, mode, utf8Bytes("{\"\\", new int[] { 0xE0, 0x80, 0xA2 }, "\":1}"),
                    "Invalid UTF-8: overlong encoding (lead byte 0xe0, second byte 0x80)");
            _testBroken(APOS_FACTORY, mode, utf8Bytes("['\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, "']"),
                    "Invalid UTF-8: overlong encoding (lead byte 0xf0, second byte 0x80)");
            _testBroken(APOS_FACTORY, mode, utf8Bytes("{'\\", new int[] { 0xF0, 0x80, 0x80, 0xA7 }, "':1}"),
                    "Invalid UTF-8: overlong encoding (lead byte 0xf0, second byte 0x80)");
            _testBroken(ANY_ESCAPE, mode, utf8Bytes("[\"\\", new int[] { 0xF0, 0x80, 0x80, 0x80 }, "\"]"),
                    "Invalid UTF-8: overlong encoding (lead byte 0xf0, second byte 0x80)");
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

    // Rejected escaped supplementary character reported at its last byte, like other
    // unrecognized escapes
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
                    assertEquals(6L, e.getLocation().getByteOffset());
                    assertEquals(1, e.getLocation().getLineNr());
                    assertEquals(7, e.getLocation().getColumnNr());
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
