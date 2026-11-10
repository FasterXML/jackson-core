package tools.jackson.core.unittest.async;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that non-blocking parsers reject invalid UTF-8 sequences (overlong forms,
 * surrogates, code points beyond U+10FFFF) in String values, regardless of where
 * input is split.
 */
class AsyncUTF8Overlong1782Test extends AsyncTestBase
{
    enum Variant {
        BYTE_ARRAY, BYTE_BUFFER;

        AsyncReaderWrapper wrap(JsonFactory f, int bytesPerRead, byte[] data)
        {
            switch (this) {
            case BYTE_ARRAY: return asyncForBytes(f, bytesPerRead, data, 0);
            case BYTE_BUFFER: return asyncForByteBuffer(f, bytesPerRead, data, 0);
            default: throw new IllegalStateException();
            }
        }
    }

    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private static final int[][] INVALID = {
        { 0xC0, 0x80 }, // U+0000
        { 0xC0, 0xAF }, // '/'
        { 0xC1, 0xA1 }, // 'a'
        { 0xC1, 0xBF }, // U+007F
        { 0xE0, 0x80, 0x80 }, // U+0000
        { 0xE0, 0x80, 0xAF }, // '/'
        { 0xE0, 0x9F, 0xBF }, // U+07FF
        { 0xED, 0xA0, 0x80 }, // U+D800 (surrogate)
        { 0xED, 0xBF, 0xBF }, // U+DFFF (surrogate)
        { 0xF0, 0x80, 0x80, 0xAF }, // '/'
        { 0xF0, 0x8F, 0xBF, 0xBF }, // U+FFFF
        { 0xF0, 0x8D, 0xA0, 0x80 }, // U+D800: overlong, not (just) surrogate
        { 0xF4, 0x90, 0x80, 0x80 }, // U+110000
        { 0xF5, 0x80, 0x80, 0x80 }, // U+140000
        { 0xF7, 0xBF, 0xBF, 0xBF }, // U+1FFFFF
    };

    private static final String[] INVALID_MSG = {
        "Invalid UTF-8 start byte 0xc0",
        "Invalid UTF-8 start byte 0xc0",
        "Invalid UTF-8 start byte 0xc1",
        "Invalid UTF-8 start byte 0xc1",
        "Invalid UTF-8: overlong 3-byte encoding of 0x0",
        "Invalid UTF-8: overlong 3-byte encoding of 0x2f",
        "Invalid UTF-8: overlong 3-byte encoding of 0x7ff",
        "Invalid UTF-8: Illegal surrogate character 0xd800",
        "Invalid UTF-8: Illegal surrogate character 0xdfff",
        "Invalid UTF-8 4-byte sequence (0xF0 0x80 ...): overlong encoding",
        "Invalid UTF-8 4-byte sequence (0xF0 0x8F ...): overlong encoding",
        "Invalid UTF-8 4-byte sequence (0xF0 0x8D ...): overlong encoding",
        "Invalid UTF-8 4-byte sequence (0xF4 0x90 ...): code point exceeds U+10FFFF",
        "Invalid UTF-8 start byte 0xf5",
        "Invalid UTF-8 start byte 0xf7",
    };

    private static final int[][] VALID = {
        { 0xC2, 0x80 }, // U+0080
        { 0xDF, 0xBF }, // U+07FF
        { 0xE0, 0xA0, 0x80 }, // U+0800
        { 0xED, 0x9F, 0xBF }, // U+D7FF
        { 0xEE, 0x80, 0x80 }, // U+E000
        { 0xEF, 0xBF, 0xBF }, // U+FFFF
        { 0xF0, 0x90, 0x80, 0x80 }, // U+10000
        { 0xF4, 0x8F, 0xBF, 0xBF }, // U+10FFFF
    };

    private static final String[] VALID_DECODED = { "\u0080", "\u07FF", "\u0800",
            "\uD7FF", "\uE000", "\uFFFF", "\uD800\uDC00", "\uDBFF\uDFFF" };

    // long suffix so that whole-buffer input takes the non-split ("fast") path
    private static final String[] SUFFIXES = { "", "zzzzzzzzzz" };

    private static final int[] BYTES_PER_READ = { 1, 2, 3, 1000 };

    @ParameterizedTest
    @EnumSource(Variant.class)
    void rejectOverlongInValue(Variant v) throws Exception
    {
        _rejectOverlong(v, FACTORY, '"');
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void rejectOverlongInAposValue(Variant v) throws Exception
    {
        _rejectOverlong(v, APOS_FACTORY, '\'');
    }

    // Error location must point past invalid sequence, also when decoded
    // without split (fast path)
    @ParameterizedTest
    @EnumSource(Variant.class)
    void invalidSequenceLocation(Variant v) throws Exception
    {
        final String prefix = "{\"a\":\"abcdefghij";
        final int[] seq = { 0xED, 0xA0, 0x80 };
        byte[] doc = _doc(prefix, seq, "zzzzzzzzzz\"}");
        try (AsyncReaderWrapper r = v.wrap(FACTORY, 1000, doc)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            r.nextToken();
            r.currentText();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Illegal surrogate character 0xd800");
            assertEquals(prefix.length() + seq.length, e.getLocation().getByteOffset());
        }
        // and invalid continuation byte just past itself, not rest of sequence
        doc = _doc(prefix, new int[] { 0xE4, 0x41, 0x42 }, "zzzzzzzzzz\"}");
        try (AsyncReaderWrapper r = v.wrap(FACTORY, 1000, doc)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            r.nextToken();
            r.currentText();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8 middle byte 0x41");
            assertEquals(prefix.length() + 2, e.getLocation().getByteOffset());
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void acceptShortestFormBoundaries(Variant v) throws Exception
    {
        for (int i = 0; i < VALID.length; ++i) {
            for (String suffix : SUFFIXES) {
                byte[] doc = _doc("{\"a\":\"", VALID[i], suffix + "\"}");
                for (int bytesPerRead : BYTES_PER_READ) {
                    try (AsyncReaderWrapper r = v.wrap(FACTORY, bytesPerRead, doc)) {
                        assertToken(JsonToken.START_OBJECT, r.nextToken());
                        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
                        assertToken(JsonToken.VALUE_STRING, r.nextToken());
                        assertEquals(VALID_DECODED[i] + suffix, r.currentText());
                        assertToken(JsonToken.END_OBJECT, r.nextToken());
                    }
                }
            }
        }
    }

    private void _rejectOverlong(Variant v, JsonFactory f, char quote) throws Exception
    {
        for (int i = 0; i < INVALID.length; ++i) {
            final int[] seq = INVALID[i];
            for (String suffix : SUFFIXES) {
                byte[] doc = _doc("{\"a\":" + quote, seq, suffix + quote + "}");
                for (int bytesPerRead : BYTES_PER_READ) {
                    try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, doc)) {
                        assertToken(JsonToken.START_OBJECT, r.nextToken());
                        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
                        r.nextToken();
                        r.currentText();
                        fail("Should not pass for sequence " + seq.length
                                + " bytes, bytesPerRead " + bytesPerRead);
                    } catch (StreamReadException e) {
                        verifyException(e, INVALID_MSG[i]);
                    }
                }
            }
        }
    }

    private static byte[] _doc(String prefix, int[] seq, String suffix) {
        byte[] pre = utf8Bytes(prefix);
        byte[] post = utf8Bytes(suffix);
        byte[] doc = new byte[pre.length + seq.length + post.length];
        System.arraycopy(pre, 0, doc, 0, pre.length);
        for (int i = 0; i < seq.length; ++i) {
            doc[pre.length + i] = (byte) seq[i];
        }
        System.arraycopy(post, 0, doc, pre.length + seq.length, post.length);
        return doc;
    }
}
