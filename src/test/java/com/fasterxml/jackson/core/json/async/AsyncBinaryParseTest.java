package com.fasterxml.jackson.core.json.async;

import java.io.*;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncBinaryParseTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    final static int[] SIZES = new int[] {
        1, 2, 3, 4, 5, 7, 11,
        90, 350, 1900, 6000, 19000, 65000,
        139000
    };

    public void testRawAsRootValue() throws IOException {
        _testBinaryAsRoot(JSON_F);
    }

    public void testRawAsArray() throws IOException {
        _testBinaryAsArray(JSON_F);
    }

    public void testRawAsObject() throws IOException {
        _testBinaryAsObject(JSON_F);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testBinaryAsRoot(JsonFactory f) throws IOException {
        _testBinaryAsRoot2(f, 1, Integer.MAX_VALUE);
        _testBinaryAsRoot2(f, 0, 3);
        _testBinaryAsRoot2(f, 1, 1);
    }

    private void _testBinaryAsObject(JsonFactory f) throws IOException {
        _testBinaryAsObject2(f, 1, Integer.MAX_VALUE);
        _testBinaryAsObject2(f, 0, 3);
        _testBinaryAsObject2(f, 1, 1);
    }

    private void _testBinaryAsArray(JsonFactory f) throws IOException {
        _testBinaryAsArray2(f, 1, Integer.MAX_VALUE);
        _testBinaryAsArray2(f, 0, 3);
        _testBinaryAsArray2(f, 1, 1);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testBinaryAsRoot2(JsonFactory f, int offset, int readSize) throws IOException
    {
        for (int size : SIZES) {
            byte[] binary = _generateData(size);
            ByteArrayOutputStream bo = new ByteArrayOutputStream(size+10);
            JsonGenerator g = f.createGenerator(bo);
            g.writeBinary(binary);
            g.close();
            byte[] smile = bo.toByteArray();

            // and verify
            AsyncReaderWrapper p = asyncForBytes(f, readSize, smile, offset);

            // JSON has no native binary type so
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            byte[] result = p.getBinaryValue();
            assertArrayEquals(binary, result);
            assertNull(p.nextToken());
            p.close();

            // and second time around, skipping
            p = asyncForBytes(f, readSize, smile, offset);
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }

    private void _testBinaryAsArray2(JsonFactory f, int offset, int readSize) throws IOException
    {
        for (int size : SIZES) {
            byte[] binary = _generateData(size);
            ByteArrayOutputStream bo = new ByteArrayOutputStream(size+10);
            JsonGenerator g = f.createGenerator(bo);
            g.writeStartArray();
            g.writeBinary(binary);
            g.writeNumber(1); // just to verify there's no overrun
            g.writeEndArray();
            g.close();
            byte[] smile = bo.toByteArray();

            // and verify
            AsyncReaderWrapper p = asyncForBytes(f, readSize, smile, offset);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());

            byte[] result = p.getBinaryValue();

            assertArrayEquals(binary, result);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            // and second time around, skipping
            p = asyncForBytes(f, readSize, smile, offset);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }

    private void _testBinaryAsObject2(JsonFactory f, int offset, int readSize) throws IOException
    {
        for (int size : SIZES) {
            byte[] data = _generateData(size);
            ByteArrayOutputStream bo = new ByteArrayOutputStream(size+10);
            JsonGenerator g = f.createGenerator(bo);
            g.writeStartObject();
            g.writeFieldName("binary");
            g.writeBinary(data);
            g.writeEndObject();
            g.close();
            byte[] smile = bo.toByteArray();

            AsyncReaderWrapper p = asyncForBytes(f, readSize, smile, offset);
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("binary", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            byte[] result = p.getBinaryValue();
            assertArrayEquals(data, result);

            // also, via different accessor
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(result.length);
            assertEquals(result.length, p.parser().readBinaryValue(bytes));
            assertArrayEquals(data, bytes.toByteArray());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            // and second time around, skipping
            p = asyncForBytes(f, readSize, smile, offset);
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();
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
}
