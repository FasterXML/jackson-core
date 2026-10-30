package com.fasterxml.jackson.core.json;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.*;

// Non-UTF-8 input given as a byte[] slice (over 8 KiB, so not decoded
// via StringReader) must not be read past offset + length
class ByteSourceBootstrapperSliceTest extends JUnit5TestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void utf16SliceNotReadPastEnd() throws Exception
    {
        // Incomplete document in slice; remainder is outside of it
        final char[] padding = new char[5000];
        Arrays.fill(padding, ' ');
        final String doc = "[" + new String(padding) + "1";
        final byte[] pre = "[0,".getBytes(StandardCharsets.UTF_16BE);
        final byte[] slice = doc.getBytes(StandardCharsets.UTF_16BE);
        final byte[] post = ",2]".getBytes(StandardCharsets.UTF_16BE);
        final byte[] buf = new byte[pre.length + slice.length + post.length];
        System.arraycopy(pre, 0, buf, 0, pre.length);
        System.arraycopy(slice, 0, buf, pre.length, slice.length);
        System.arraycopy(post, 0, buf, pre.length + slice.length, post.length);

        try (JsonParser p = JSON_F.createParser(buf, pre.length, slice.length)) {
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
