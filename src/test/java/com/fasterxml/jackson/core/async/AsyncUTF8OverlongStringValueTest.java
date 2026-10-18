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
 * Tests that non-blocking parsers reject overlong (non-shortest form) 2- and 3-byte
 * UTF-8 sequences in String values, regardless of where input is split.
 */
class AsyncUTF8OverlongStringValueTest extends AsyncTestBase
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

    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private static final int[][] OVERLONG = {
        { 0xC0, 0x80 }, // U+0000
        { 0xC0, 0xAF }, // '/'
        { 0xC1, 0xA1 }, // 'a'
        { 0xC1, 0xBF }, // U+007F
        { 0xE0, 0x80, 0x80 }, // U+0000
        { 0xE0, 0x80, 0xAF }, // '/'
        { 0xE0, 0x9F, 0xBF }, // U+07FF
    };

    private static final int[][] VALID = {
        { 0xC2, 0x80 }, // U+0080
        { 0xDF, 0xBF }, // U+07FF
        { 0xE0, 0xA0, 0x80 }, // U+0800
        { 0xEF, 0xBF, 0xBF }, // U+FFFF
    };

    private static final String[] VALID_DECODED = { "\u0080", "߿", "ࠀ", "￿" };

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
                        assertToken(JsonToken.FIELD_NAME, r.nextToken());
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
        for (int[] seq : OVERLONG) {
            for (String suffix : SUFFIXES) {
                byte[] doc = _doc("{\"a\":" + quote, seq, suffix + quote + "}");
                for (int bytesPerRead : BYTES_PER_READ) {
                    try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, doc)) {
                        assertToken(JsonToken.START_OBJECT, r.nextToken());
                        assertToken(JsonToken.FIELD_NAME, r.nextToken());
                        r.nextToken();
                        r.currentText();
                        fail("Should not pass for sequence " + seq.length
                                + " bytes, bytesPerRead " + bytesPerRead);
                    } catch (StreamReadException e) {
                        verifyException(e, "Invalid UTF-8: overlong " + seq.length + "-byte encoding");
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
