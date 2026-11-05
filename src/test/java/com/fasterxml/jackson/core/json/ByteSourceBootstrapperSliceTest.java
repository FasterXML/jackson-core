package com.fasterxml.jackson.core.json;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.*;

// Input decoded via Reader and given as a byte[] slice (over 8 KiB, so not
// decoded via StringReader) must not be read past offset + length
class ByteSourceBootstrapperSliceTest extends JUnit5TestBase
{
    private final static String BOM = "﻿";

    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void utf16BESliceNotReadPastEnd() throws Exception
    {
        _testSliceNotReadPastEnd(JSON_F, StandardCharsets.UTF_16BE, "");
    }

    @Test
    void utf16BEWithBOMSliceNotReadPastEnd() throws Exception
    {
        _testSliceNotReadPastEnd(JSON_F, StandardCharsets.UTF_16BE, BOM);
    }

    @Test
    void utf16LESliceNotReadPastEnd() throws Exception
    {
        _testSliceNotReadPastEnd(JSON_F, StandardCharsets.UTF_16LE, "");
    }

    @Test
    void utf16LEWithBOMSliceNotReadPastEnd() throws Exception
    {
        _testSliceNotReadPastEnd(JSON_F, StandardCharsets.UTF_16LE, BOM);
    }

    // UTF-8 only goes through Reader if canonicalization is disabled
    @Test
    void utf8NoCanonicalizationSliceNotReadPastEnd() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
                .build();
        _testSliceNotReadPastEnd(f, StandardCharsets.UTF_8, "");
    }

    private void _testSliceNotReadPastEnd(JsonFactory f, Charset cs, String prefix) throws Exception
    {
        // Incomplete document in slice; remainder is outside of it
        final char[] padding = new char[9000];
        Arrays.fill(padding, ' ');
        final String doc = prefix + "[" + new String(padding) + "1";
        final byte[] pre = "[0,".getBytes(cs);
        final byte[] slice = doc.getBytes(cs);
        final byte[] post = ",2]".getBytes(cs);
        final byte[] buf = new byte[pre.length + slice.length + post.length];
        System.arraycopy(pre, 0, buf, 0, pre.length);
        System.arraycopy(slice, 0, buf, pre.length, slice.length);
        System.arraycopy(post, 0, buf, pre.length + slice.length, post.length);

        try (JsonParser p = f.createParser(buf, pre.length, slice.length)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            try {
                p.nextToken();
                fail("Should not read beyond end of slice");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected end-of-input");
            }
        }
    }
}
