package com.fasterxml.jackson.core.base64;

import static org.junit.Assert.assertArrayEquals;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestBase64Parsing
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testBase64UsingInputStream() throws Exception
    {
        _testBase64Text(true);
    }

    public void testBase64UsingReader() throws Exception
    {
        _testBase64Text(false);
    }

    // [Issue-15] (streaming binary reads)
    public void testStreaming() throws IOException
    {
        _testStreaming(false);
        _testStreaming(true);
    }

    /*
    /**********************************************************
    /* Test helper methods
    /**********************************************************
     */
    
    // Test for [JACKSON-631]
    @SuppressWarnings("resource")
    public void _testBase64Text(boolean useBytes) throws Exception
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
                JsonGenerator jgen;
                if (useBytes) {
                    bytes.reset();
                    jgen = jsonFactory.createGenerator(bytes, JsonEncoding.UTF8);
                } else {
                    chars = new StringWriter();
                    jgen = jsonFactory.createGenerator(chars);
                }
                jgen.writeBinary(variant, input, 0, input.length);
                jgen.close();
                JsonParser jp;
                if (useBytes) {
                    jp = jsonFactory.createParser(bytes.toByteArray());
                } else {
                    jp = jsonFactory.createParser(chars.toString());
                }
                assertToken(JsonToken.VALUE_STRING, jp.nextToken());
                byte[] data = null;
                try {
                    data = jp.getBinaryValue(variant);
                } catch (Exception e) {
                    IOException ioException = new IOException("Failed (variant "+variant+", data length "+len+"): "+e.getMessage());
                    ioException.initCause(e);
                    throw ioException;
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
                assertNull(jp.nextToken());
                jp.close();
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

    @SuppressWarnings("resource")
    private void _testStreaming(boolean useBytes) throws IOException
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
            if (useBytes) {
                bytes.reset();
                g = jsonFactory.createGenerator(bytes, JsonEncoding.UTF8);
            } else {
                chars = new StringWriter();
                g = jsonFactory.createGenerator(chars);
            }

            g.writeStartObject();
            g.writeFieldName("b");
            g.writeBinary(data);
            g.writeEndObject();
            g.close();

            // and verify
            JsonParser p;
            if (useBytes) {
                p = jsonFactory.createParser(bytes.toByteArray());
            } else {
                p = jsonFactory.createParser(chars.toString());
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
            assertNull(p.nextToken());
            p.close();
        }
    }
}
