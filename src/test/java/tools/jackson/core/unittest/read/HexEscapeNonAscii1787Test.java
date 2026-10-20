package tools.jackson.core.unittest.read;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1787]: non-ASCII chars must not be accepted as hex
 * digits of a {@code \}{@code u} escape (they used to alias to ASCII ones with
 * the same low byte, e.g. U+2042 to 'B', with char-based input).
 */
class HexEscapeNonAscii1787Test extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    @Test
    void nonAsciiHexDigitsRejected() throws Exception
    {
        // U+2042 -> 'B', U+0141 -> 'A', U+0130 -> '0'
        _testBroken("\\u\u2042\u2042\u2042\u2042", "'\u2042' (code 8258 / 0x2042)");
        _testBroken("\\u\u0141BCD", "'\u0141' (code 321 / 0x141)");
        _testBroken("\\u00\u01300", "'\u0130' (code 304 / 0x130)");
        _testBroken("\\u\uD83D\uDE00", null);
    }

    // Invalid or truncated UTF-8 in hex escape must still be reported as bad hex digit
    // (lead byte as-is), not as UTF-8 decoding or end-of-input problem
    @Test
    void invalidUtf8HexDigitsRejected() throws Exception
    {
        _testBrokenBytes(new int[] { 0x80 }, "(CTRL-CHAR, code 128)");
        _testBrokenBytes(new int[] { 0xFF }, "'\u00FF' (code 255)");
        // bad continuation byte
        _testBrokenBytes(new int[] { 0xC3, 'A' }, "'\u00C3' (code 195)");
        _testBrokenBytes(new int[] { 0xE2, 0x81, 'B' }, "'\u00E2' (code 226)");
        // invalid lead bytes (overlong 2-byte, beyond U+10FFFF)
        _testBrokenBytes(new int[] { 0xC0, 0x80 }, "'\u00C0' (code 192)");
        _testBrokenBytes(new int[] { 0xF5, 0x80, 0x80, 0x80 }, "'\u00F5' (code 245)");
        // overlong 3-byte; surrogate
        _testBrokenBytes(new int[] { 0xE0, 0x80, 0x80 }, "'\u00E0' (code 224)");
        _testBrokenBytes(new int[] { 0xED, 0xA0, 0x80 }, "'\u00ED' (code 237)");
    }

    @Test
    void truncatedUtf8HexDigitRejected() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            byte[] doc = _bytes("[\"\\u", new int[] { 0xE2, 0x81 }, "");
            try (JsonParser p = createParser(FACTORY, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                p.getText();
                fail("Should not pass for mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('\u00E2' (code 226)): expected a hex-digit");
            }
        }
    }

    // Error location must not depend on whether bad hex digit is multi-byte
    @Test
    void errorLocationForMultiByteChar() throws Exception
    {
        for (int mode : new int[] { MODE_INPUT_STREAM, MODE_INPUT_STREAM_THROTTLED }) {
            TokenStreamLocation asciiLoc = _errorLocation(mode, "[\"\\uZZZZ\"]");
            TokenStreamLocation multiLoc = _errorLocation(mode, "[\"\\u\u2042\u2042\u2042\u2042\"]");
            assertEquals(asciiLoc.getByteOffset(), multiLoc.getByteOffset());
            assertEquals(asciiLoc.getColumnNr(), multiLoc.getColumnNr());
        }
    }

    @Test
    void asciiHexDigitsAccepted() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, "{\"\\u00e9\":\"\\uABcd\"}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("\u00e9", p.currentName());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uABCD", p.getText());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    private void _testBroken(String escape, String expCharDesc) throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String doc : new String[] { "[\"" + escape + "\"]", "{\"" + escape + "\":1}" }) {
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    while (p.nextToken() != null) {
                        p.getText();
                    }
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "expected a hex-digit for character escape sequence");
                    // best-effort decoding needs whole char buffered (DataInput: never decoded)
                    if (expCharDesc != null && mode != MODE_INPUT_STREAM_THROTTLED
                            && mode != MODE_DATA_INPUT) {
                        verifyException(e, "Unexpected character (" + expCharDesc + ")");
                    }
                }
            }
        }
    }

    private void _testBrokenBytes(int[] badBytes, String expCharDesc) throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (byte[] doc : new byte[][] { _bytes("[\"\\u", badBytes, "\"]"),
                    _bytes("{\"\\u", badBytes, "\":1}") }) {
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    while (p.nextToken() != null) {
                        p.getText();
                    }
                    fail("Should not pass for mode " + mode);
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected character (" + expCharDesc
                            + "): expected a hex-digit for character escape sequence");
                }
            }
        }
    }

    private TokenStreamLocation _errorLocation(int mode, String doc) throws Exception
    {
        try (JsonParser p = createParser(FACTORY, mode, doc)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            p.getText();
            fail("Should not pass for mode " + mode + ", doc: " + doc);
            return null;
        } catch (StreamReadException e) {
            verifyException(e, "expected a hex-digit");
            return e.getLocation();
        }
    }

    static byte[] _bytes(String prefix, int[] raw, String suffix) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(prefix.getBytes("UTF-8"));
        for (int b : raw) {
            bytes.write(b);
        }
        bytes.write(suffix.getBytes("UTF-8"));
        return bytes.toByteArray();
    }
}
