package com.fasterxml.jackson.core.json;

import java.io.ByteArrayOutputStream;
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
        final byte[] pre = "[0,".getBytes(cs);
        final byte[] slice = (prefix + "[" + new String(padding) + "1").getBytes(cs);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(pre);
        buf.write(slice);
        buf.write(",2]".getBytes(cs));

        try (JsonParser p = f.createParser(buf.toByteArray(), pre.length, slice.length)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            StreamReadException e = assertThrows(StreamReadException.class, p::nextToken);
            verifyException(e, "Unexpected end-of-input");
        }
    }
}
