package com.fasterxml.jackson.core.base64;

import static org.junit.Assert.assertArrayEquals;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class Base64BinaryParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testBase64UsingInputStream() throws Exception
    {
        _testBase64Text(MODE_INPUT_STREAM);
        _testBase64Text(MODE_INPUT_STREAM_THROTTLED);
        _testBase64Text(MODE_DATA_INPUT);
    }

    public void testBase64UsingReader() throws Exception
    {
        _testBase64Text(MODE_READER);
    }

    public void testStreaming() throws IOException
    {
        _testStreaming(MODE_INPUT_STREAM);
        _testStreaming(MODE_INPUT_STREAM_THROTTLED);
        _testStreaming(MODE_DATA_INPUT);
        _testStreaming(MODE_READER);
    }

    public void testSimple() throws IOException
    {
        for (int mode : ALL_MODES) {
            _testSimple(mode);
        }
    }

    public void testInArray() throws IOException
    {
        for (int mode : ALL_MODES) {
            _testInArray(mode);
        }
    }

    public void testWithEscaped() throws IOException {
        for (int mode : ALL_MODES) {
            _testEscaped(mode);
        }
    }

    /*
    /**********************************************************
    /* Test helper methods
    /**********************************************************
     */

    @SuppressWarnings("resource")
    public void _testBase64Text(int mode) throws Exception
    {
        // let's actually iterate over sets of encoding modes, lengths
        
        final int[] LENS = { 1, 2, 3, 4, 7, 9, 32, 33, 34, 35 };
        final Base64Variant[] VARIANTS = {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        };

        JsonFactory jsonFactory = new JsonFactory();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = null;
        for (int len : LENS) {
            byte[] input = new byte[len];
            for (int i = 0; i < input.length; ++i) {
                input[i] = (byte) i;
            }
            for (Base64Variant variant : VARIANTS) {
                JsonGenerator g;

                if (mode == MODE_READER) {
                    chars = new StringWriter();
                    g = jsonFactory.createGenerator(chars);
                } else {
                    bytes.reset();
                    g = jsonFactory.createGenerator(bytes, JsonEncoding.UTF8);
                }
                g.writeBinary(variant, input, 0, input.length);
                g.close();
                JsonParser p;
                if (mode == MODE_READER) {
                    p = jsonFactory.createParser(chars.toString());
                } else {
                    p = createParser(jsonFactory, mode, bytes.toByteArray());
                }
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                byte[] data = null;
                try {
                    data = p.getBinaryValue(variant);
                } catch (Exception e) {
                    IOException ioException = new IOException("Failed (variant "+variant+", data length "+len+"): "+e.getMessage());
                    ioException.initCause(e);
                    throw ioException;
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
                if (mode != MODE_DATA_INPUT) { // no look-ahead for DataInput
                    assertNull(p.nextToken());
                }
                p.close();
            }
        }
    }

    private byte[] _generateData(int size)
    {
        byte[] result = new byte[size];
        for (int i = 0; i < size; ++i) {
            result[i] = (byte) (i % 255);
        }
        return result;
    }

    private void _testStreaming(int mode) throws IOException
    {
        final int[] SIZES = new int[] {
            1, 2, 3, 4, 5, 6,
            7, 8, 12,
            100, 350, 1900, 6000, 19000, 65000,
            139000
        };

        JsonFactory jsonFactory = new JsonFactory();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = null;

        for (int size : SIZES) {
            byte[] data = _generateData(size);
            JsonGenerator g;
            if (mode == MODE_READER) {
                chars = new StringWriter();
                g = jsonFactory.createGenerator(chars);
            } else {
                bytes.reset();
                g = jsonFactory.createGenerator(bytes, JsonEncoding.UTF8);
            }

            g.writeStartObject();
            g.writeFieldName("b");
            g.writeBinary(data);
            g.writeEndObject();
            g.close();

            // and verify
            JsonParser p;
            if (mode == MODE_READER) {
                p = jsonFactory.createParser(chars.toString());
            } else {
                p = createParser(jsonFactory, mode, bytes.toByteArray());
            }
            assertToken(JsonToken.START_OBJECT, p.nextToken());
    
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("b", p.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            ByteArrayOutputStream result = new ByteArrayOutputStream(size);
            int gotten = p.readBinaryValue(result);
            assertEquals(size, gotten);
            assertArrayEquals(data, result.toByteArray());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            if (mode != MODE_DATA_INPUT) { // no look-ahead for DataInput
                assertNull(p.nextToken());
            }
            p.close();
        }
    }

    private void _testSimple(int mode)
        throws IOException
    {
        /* The usual sample input string, from Thomas Hobbes's "Leviathan"
         * (via Wikipedia)
         */
        final String RESULT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] RESULT_BYTES = RESULT.getBytes("US-ASCII");

        // And here's what should produce it...
        final String INPUT_STR = 
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;

        final String DOC = "\""+INPUT_STR+"\"";
        JsonParser p = createParser(mode, DOC);

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(RESULT_BYTES, data);
        p.close();
    }

    private void _testInArray(int mode) throws IOException
    {
        JsonFactory f = new JsonFactory();

        final int entryCount = 7;

        StringWriter sw = new StringWriter();
        JsonGenerator jg = f.createGenerator(sw);
        jg.writeStartArray();

        byte[][] entries = new byte[entryCount][];
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = new byte[200 + i * 100];
            for (int x = 0; x < b.length; ++x) {
                b[x] = (byte) (i + x);
            }
            entries[i] = b;
            jg.writeBinary(b);
        }

        jg.writeEndArray();
        jg.close();

        JsonParser p = createParser(f, mode, sw.toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());

        for (int i = 0; i < entryCount; ++i) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            byte[] b = p.getBinaryValue();
            assertArrayEquals(entries[i], b);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testEscaped(int mode) throws IOException
    {
        // Input: "Test!" -> "VGVzdCE="

        // First, try with embedded linefeed half-way through:

        String DOC = quote("VGVz\\ndCE="); // note: must double-quote to get linefeed
        JsonParser p = createParser(mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();

        // and then with escaped chars
//            DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        p = createParser(mode, DOC);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        b = p.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }
}
