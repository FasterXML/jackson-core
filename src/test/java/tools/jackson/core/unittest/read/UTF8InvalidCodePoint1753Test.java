package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for [jackson-core#1753]: 4-byte UTF-8 sequences outside of
 * [U+10000, U+10FFFF] must be rejected when decoding String values.
 */
class UTF8InvalidCodePoint1753Test
    extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    @Test
    void acceptUtf8CodePointsAtRangeBoundaries() throws Exception
    {
        _verifyAccepted(new int[] { 0xF0, 0x90, 0x80, 0x80 }, "𐀀"); // U+10000
        _verifyAccepted(new int[] { 0xF4, 0x8F, 0xBF, 0xBF }, "􏿿"); // U+10FFFF
    }

    @Test
    void rejectUtf8CodePointsOutsideRange() throws Exception
    {
        _verifyRejectedSequences(false);
    }

    @Test
    void rejectUtf8CodePointsOutsideRangeWhenSkipped() throws Exception
    {
        _verifyRejectedSequences(true);
    }

    @Test
    void rejectUtf8CodePointsOutsideRangeAsync() throws Exception
    {
        final String OVERLONG = "overlong encoding";
        final String ABOVE_MAX = "code point exceeds U+10FFFF";

        _verifyRejectedAsync(new int[] { 0xF0, 0x80, 0x80, 0x80 }, OVERLONG);
        _verifyRejectedAsync(new int[] { 0xF0, 0x8F, 0xBF, 0xBF }, OVERLONG);
        _verifyRejectedAsync(new int[] { 0xF0, 0x8D, 0xA0, 0x80 }, OVERLONG); // surrogate U+D800
        _verifyRejectedAsync(new int[] { 0xF4, 0x90, 0x80, 0x80 }, ABOVE_MAX);
        _verifyRejectedAsync(new int[] { 0xF5, 0x80, 0x80, 0x80 }, ABOVE_MAX);
        _verifyRejectedAsync(new int[] { 0xF7, 0xBF, 0xBF, 0xBF }, ABOVE_MAX);
    }

    @Test
    void rejectSplitUtf8CodePointAfterSecondByteAsync() throws Exception
    {
        _verifyRejectedAsync(new int[] { 0xF0, 0x80, 0x41, 0x41 }, "overlong encoding");
        _verifyRejectedAsync(new int[] { 0xF4, 0x90, 0x41, 0x41 }, "code point exceeds U+10FFFF");
    }

    private void _verifyAccepted(int[] sequence, String expected) throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, _quoted(sequence))) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(expected, p.getString(), "mode=" + mode);
            }
        }
    }

    private void _verifyRejectedSequences(boolean skipped) throws Exception
    {
        final String OVERLONG = "overlong encoding";
        final String ABOVE_MAX = "code point exceeds U+10FFFF";

        _verifyRejected(new int[] { 0xF0, 0x80, 0x80, 0x80 }, OVERLONG, skipped);
        _verifyRejected(new int[] { 0xF0, 0x8F, 0xBF, 0xBF }, OVERLONG, skipped);
        _verifyRejected(new int[] { 0xF0, 0x8D, 0xA0, 0x80 }, OVERLONG, skipped); // surrogate U+D800
        _verifyRejected(new int[] { 0xF4, 0x90, 0x80, 0x80 }, ABOVE_MAX, skipped);
        _verifyRejected(new int[] { 0xF5, 0x80, 0x80, 0x80 }, ABOVE_MAX, skipped);
        _verifyRejected(new int[] { 0xF7, 0xBF, 0xBF, 0xBF }, ABOVE_MAX, skipped);
    }

    private void _verifyRejected(int[] sequence, String reason, boolean skipped) throws Exception
    {
        final String expMsg = String.format("Invalid UTF-8 4-byte sequence (0x%02X 0x%02X ...): %s",
                sequence[0], sequence[1], reason);
        for (int mode : ALL_BINARY_MODES) {
            StreamReadException e = assertThrows(StreamReadException.class, () -> {
                try (JsonParser p = createParser(FACTORY, mode,
                        skipped ? _arrayWithString(sequence) : _quoted(sequence))) {
                    if (skipped) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        p.nextToken();
                    } else {
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        p.getString();
                    }
                }
            }, "mode=" + mode);
            verifyException(e, expMsg);
        }
    }

    private void _verifyRejectedAsync(int[] sequence, String reason) throws Exception
    {
        final byte[] doc = _arrayWithString(sequence);
        final String expMsg = String.format("Invalid UTF-8 4-byte sequence (0x%02X 0x%02X ...): %s",
                sequence[0], sequence[1], reason);
        for (int bytesPerRead : new int[] { 1, 100 }) {
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(FACTORY, bytesPerRead, doc, 0)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                StreamReadException e = assertThrows(StreamReadException.class, p::nextToken,
                        "bytesPerRead=" + bytesPerRead);
                verifyException(e, expMsg);
            }
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(FACTORY, bytesPerRead, doc, 0)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                StreamReadException e = assertThrows(StreamReadException.class, p::nextToken,
                        "byteBuffer bytesPerRead=" + bytesPerRead);
                verifyException(e, expMsg);
            }
        }
    }

    private static byte[] _quoted(int[] sequence)
    {
        byte[] json = new byte[sequence.length + 2];
        json[0] = '"';
        for (int i = 0; i < sequence.length; ++i) {
            json[i + 1] = (byte) sequence[i];
        }
        json[json.length - 1] = '"';
        return json;
    }

    private static byte[] _arrayWithString(int[] sequence)
    {
        byte[] json = new byte[sequence.length + 6];
        json[0] = '[';
        json[1] = '"';
        for (int i = 0; i < sequence.length; ++i) {
            json[i + 2] = (byte) sequence[i];
        }
        json[json.length - 4] = '"';
        json[json.length - 3] = ',';
        json[json.length - 2] = '1';
        json[json.length - 1] = ']';
        return json;
    }
}
