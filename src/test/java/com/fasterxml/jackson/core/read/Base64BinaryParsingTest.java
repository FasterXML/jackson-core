package com.fasterxml.jackson.core.read;

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
}
