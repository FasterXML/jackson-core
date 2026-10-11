package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for [jackson-core#1753]: 4-byte UTF-8 sequences outside of
 * [U+10000, U+10FFFF] must be rejected when decoding String values,
 * skipping String values and skipping comments.
 */
class UTF8InvalidCodePoint1753Test
    extends JacksonCoreTestBase
{
    private final static String OVERLONG = "overlong encoding";
    private final static String ABOVE_MAX = "code point exceeds U+10FFFF";

    private record Rejected(int[] bytes, String reason) { }

    private final static Rejected[] REJECTED = {
        new Rejected(new int[] { 0xF0, 0x80, 0x80, 0x80 }, OVERLONG),
        new Rejected(new int[] { 0xF0, 0x8F, 0xBF, 0xBF }, OVERLONG),
        new Rejected(new int[] { 0xF0, 0x8D, 0xA0, 0x80 }, OVERLONG), // surrogate U+D800
        new Rejected(new int[] { 0xF4, 0x90, 0x80, 0x80 }, ABOVE_MAX),
        new Rejected(new int[] { 0xF5, 0x80, 0x80, 0x80 }, ABOVE_MAX),
        new Rejected(new int[] { 0xF7, 0xBF, 0xBF, 0xBF }, ABOVE_MAX),
        // Range must be checked before (invalid) 3rd byte
        new Rejected(new int[] { 0xF0, 0x80, 0x41, 0x41 }, OVERLONG),
        new Rejected(new int[] { 0xF4, 0x90, 0x41, 0x41 }, ABOVE_MAX),
    };

    // Content around invalid sequence; all documents are `[ ... 1]` with
    // either String value or comment before the number
    private final static String[][] CONTEXTS = {
        { "[\"", "\",1]" },
        { "[/*", "*/1]" },
        { "[//", "\n1]" },
        { "[#", "\n1]" },
    };

    private final static int[] BYTES_PER_READ = { 1, 2, 3, 100 };

    private final JsonFactory FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .build();

    @Test
    void acceptUtf8CodePointsAtRangeBoundaries() throws Exception
    {
        _verifyAccepted(new int[] { 0xF0, 0x90, 0x80, 0x80 }, "𐀀"); // U+10000
        _verifyAccepted(new int[] { 0xF4, 0x8F, 0xBF, 0xBF }, "􏿿"); // U+10FFFF
    }

    @Test
    void rejectUtf8CodePointsOutsideRange() throws Exception
    {
        for (Rejected r : REJECTED) {
            for (String[] ctxt : CONTEXTS) {
                final byte[] doc = _doc(ctxt, r.bytes);
                final boolean isString = (ctxt == CONTEXTS[0]);
                _verifyRejected(r, doc, isString, false);
                if (isString) {
                    _verifyRejected(r, doc, true, true);
                }
            }
        }
    }

    @Test
    void rejectUtf8CodePointsOutsideRangeAsync() throws Exception
    {
        for (Rejected r : REJECTED) {
            for (String[] ctxt : CONTEXTS) {
                _verifyRejectedAsync(r, _doc(ctxt, r.bytes));
            }
        }
    }

    // Error location must point to invalid sequence, not start of String
    @Test
    void errorLocationForInvalidUtf8() throws Exception
    {
        _verifyErrorLocation(REJECTED[3].bytes, _expMessage(REJECTED[3]));
        // and same for other invalid multi-byte sequences
        _verifyErrorLocation(new int[] { 0xC3, 0x41 }, "Invalid UTF-8 middle byte 0x41");
        _verifyErrorLocation(new int[] { 0xE2, 0x41, 0x80 }, "Invalid UTF-8 middle byte 0x41");
        _verifyErrorLocation(new int[] { 0xED, 0xA0, 0x80 }, "Illegal surrogate character 0xd800");
    }

    private void _verifyErrorLocation(int[] sequence, String expMsg) throws Exception
    {
        // Leading multi-byte char and trailing content to get async parser to decode
        // invalid sequence on its fast (non-split) path, with `_inputPtr` not synced
        final String prefix = "[\"\u00E9" + "a".repeat(40);
        final byte[] doc = _doc(new String[] { prefix, "\",1,2,3,4,5,6,7,8,9]" }, sequence);
        final int minColumn = prefix.length() + 1;
        // (DataInput-backed parser does not track location)
        for (int mode : new int[] { MODE_INPUT_STREAM, MODE_INPUT_STREAM_THROTTLED }) {
            StreamReadException e = assertThrows(StreamReadException.class, () -> {
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    p.getString();
                }
            });
            _verifyColumn(e, expMsg, minColumn, "mode=" + mode + ", doc=" + _desc(doc));
        }
        for (boolean byteBuffer : new boolean[] { false, true }) {
            try (AsyncReaderWrapper p = byteBuffer
                    ? AsyncTestBase.asyncForByteBuffer(FACTORY, 1000, doc, 0)
                    : AsyncTestBase.asyncForBytes(FACTORY, 1000, doc, 0)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
                _verifyColumn(e, expMsg, minColumn, "byteBuffer=" + byteBuffer + ", doc=" + _desc(doc));
            }
        }
    }

    private void _verifyColumn(StreamReadException e, String expMsg, int minColumn, String desc)
    {
        verifyException(e, expMsg);
        final int column = e.getLocation().getColumnNr();
        assertTrue(column >= minColumn, desc + ": column " + column + " < " + minColumn);
    }

    private void _verifyAccepted(int[] sequence, String expected) throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, _doc(CONTEXTS[0], sequence))) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(expected, p.getString(), "mode=" + mode);
            }
            for (String[] ctxt : CONTEXTS) {
                try (JsonParser p = createParser(FACTORY, mode, _doc(ctxt, sequence))) {
                    _verifyAcceptedTokens(p.nextToken(), p.nextToken(), p.nextToken());
                }
            }
        }
        for (int bytesPerRead : BYTES_PER_READ) {
            for (String[] ctxt : CONTEXTS) {
                final byte[] doc = _doc(ctxt, sequence);
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(FACTORY, bytesPerRead, doc, 0)) {
                    _verifyAcceptedTokens(p.nextToken(), p.nextToken(), p.nextToken());
                }
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(FACTORY, bytesPerRead, doc, 0)) {
                    _verifyAcceptedTokens(p.nextToken(), p.nextToken(), p.nextToken());
                }
            }
        }
    }

    private void _verifyAcceptedTokens(JsonToken t1, JsonToken t2, JsonToken t3)
    {
        assertToken(JsonToken.START_ARRAY, t1);
        if (t2 == JsonToken.VALUE_STRING) {
            t2 = t3;
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, t2);
    }

    private void _verifyRejected(Rejected r, byte[] doc, boolean isString, boolean skipString)
        throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            StreamReadException e = assertThrows(StreamReadException.class, () -> {
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    JsonToken t = p.nextToken();
                    if (isString) {
                        assertToken(JsonToken.VALUE_STRING, t);
                        if (skipString) {
                            p.nextToken();
                        } else {
                            p.getString();
                        }
                    }
                }
            }, "mode=" + mode + ", doc=" + _desc(doc));
            verifyException(e, _expMessage(r));
        }
    }

    private void _verifyRejectedAsync(Rejected r, byte[] doc) throws Exception
    {
        for (int bytesPerRead : BYTES_PER_READ) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = byteBuffer
                        ? AsyncTestBase.asyncForByteBuffer(FACTORY, bytesPerRead, doc, 0)
                        : AsyncTestBase.asyncForBytes(FACTORY, bytesPerRead, doc, 0)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    StreamReadException e = assertThrows(StreamReadException.class, p::nextToken,
                            "byteBuffer=" + byteBuffer + ", bytesPerRead=" + bytesPerRead
                            + ", doc=" + _desc(doc));
                    verifyException(e, _expMessage(r));
                }
            }
        }
    }

    private static String _expMessage(Rejected r)
    {
        return String.format("Invalid UTF-8 4-byte sequence (0x%02X 0x%02X ...): %s",
                r.bytes[0], r.bytes[1], r.reason);
    }

    private static byte[] _doc(String[] ctxt, int[] sequence)
    {
        final byte[] prefix = utf8Bytes(ctxt[0]);
        final byte[] suffix = utf8Bytes(ctxt[1]);
        final byte[] doc = new byte[prefix.length + sequence.length + suffix.length];
        System.arraycopy(prefix, 0, doc, 0, prefix.length);
        for (int i = 0; i < sequence.length; ++i) {
            doc[prefix.length + i] = (byte) sequence[i];
        }
        System.arraycopy(suffix, 0, doc, prefix.length + sequence.length, suffix.length);
        return doc;
    }

    private static String _desc(byte[] doc)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : doc) {
            int i = b & 0xFF;
            if (i >= 0x20 && i < 0x7F) {
                sb.append((char) i);
            } else {
                sb.append(String.format("<%02X>", i));
            }
        }
        return sb.toString();
    }
}
