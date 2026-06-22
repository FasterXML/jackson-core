package com.fasterxml.jackson.core.base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [core#1622]: When a binary content length is known, the encoding/read buffer
// should be sized from that hint (capped) so large content needs far fewer
// InputStream reads than the small default buffer would require.
class BinaryWriteBufferSize1622Test
        extends com.fasterxml.jackson.core.JUnit5TestBase
{
    // Cap mirrored from JsonGeneratorImpl.MAX_BASE64_ENCODE_BUFFER_LENGTH
    private final static int MAX_BUFFER = 64 * 1024;

    private final JsonFactory JSON_F = new JsonFactory();

    private final Base64Variant VARIANT = Base64Variants.MIME;

    /**
     * {@link ByteArrayInputStream} that records the largest {@code len} ever
     * requested via {@link #read(byte[], int, int)}, which equals the size of
     * the read buffer the generator allocated.
     */
    static class ReadSizeRecordingInputStream extends ByteArrayInputStream {
        int maxRequestedRead = 0;

        ReadSizeRecordingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            if (len > maxRequestedRead) {
                maxRequestedRead = len;
            }
            return super.read(b, off, len);
        }
    }

    @Test
    void sizeHintAppliedByteBacked() throws Exception {
        // 50_000 is below the 64kB cap, so the read buffer should be sized to it
        _testSizeHint(true, 50_000, 50_000);
    }

    @Test
    void sizeHintAppliedCharBacked() throws Exception {
        _testSizeHint(false, 50_000, 50_000);
    }

    @Test
    void sizeHintCappedByteBacked() throws Exception {
        // 200_000 exceeds the cap, so the read buffer should be limited to it
        _testSizeHint(true, 200_000, MAX_BUFFER);
    }

    @Test
    void sizeHintCappedCharBacked() throws Exception {
        _testSizeHint(false, 200_000, MAX_BUFFER);
    }

    private void _testSizeHint(boolean useBytes, int dataLength, int expectedMaxRead)
        throws Exception
    {
        byte[] input = new byte[dataLength];
        for (int i = 0; i < input.length; ++i) {
            input[i] = (byte) (i * 31 + 7);
        }
        ReadSizeRecordingInputStream in = new ReadSizeRecordingInputStream(input);

        byte[] rawJson;
        if (useBytes) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator g = JSON_F.createGenerator(out, JsonEncoding.UTF8)) {
                g.writeBinary(VARIANT, in, dataLength);
            }
            rawJson = out.toByteArray();
        } else {
            StringWriter sw = new StringWriter();
            try (JsonGenerator g = JSON_F.createGenerator(sw)) {
                g.writeBinary(VARIANT, in, dataLength);
            }
            rawJson = sw.toString().getBytes("UTF-8");
        }

        // The generator should have requested reads as large as the (capped) hint,
        // which is much bigger than the small default buffer used before the fix.
        assertEquals(expectedMaxRead, in.maxRequestedRead,
                "read buffer should be sized from the length hint (capped)");
        assertTrue(in.maxRequestedRead <= MAX_BUFFER,
                "read buffer must never exceed the cap");

        // ...and the produced base64 must still decode back to the original bytes.
        try (JsonParser p = JSON_F.createParser(rawJson)) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            byte[] decoded = p.getBinaryValue(VARIANT);
            assertArrayEquals(input, decoded);
        }
    }
}
