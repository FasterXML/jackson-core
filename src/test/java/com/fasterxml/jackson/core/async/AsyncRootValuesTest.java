package com.fasterxml.jackson.core.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.*;

public class AsyncRootValuesTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testTokenRootSequence() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(100);

        // Let's simply concatenate documents...
        bytes.write(_jsonDoc("\n[ true, false,\nnull  ,null\n,true,false]"));
        byte[] input = bytes.toByteArray();

        JsonFactory f = JSON_F;
        _testTokenRootSequence(f, input, 0, 900);
        _testTokenRootSequence(f, input, 0, 3);
        _testTokenRootSequence(f, input, 0, 1);

        _testTokenRootSequence(f, input, 1, 900);
        _testTokenRootSequence(f, input, 1, 3);
        _testTokenRootSequence(f, input, 1, 1);
    }

    private void _testTokenRootSequence(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
/*
    public void testSimpleRootSequence() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(100);

        // Let's simply concatenate documents...
        bytes.write(_jsonDoc("[ true, false ]"));
        bytes.write(_jsonDoc("{ \"a\" : 4 }"));
        bytes.write(_jsonDoc(" 12356"));
        bytes.write(_jsonDoc(" true"));
        byte[] input = bytes.toByteArray();

        JsonFactory f = JSON_F;
        _testSimpleRootSequence(f, input, 0, 100);
        _testSimpleRootSequence(f, input, 0, 3);
        _testSimpleRootSequence(f, input, 0, 1);

        _testSimpleRootSequence(f, input, 1, 100);
        _testSimpleRootSequence(f, input, 1, 3);
        _testSimpleRootSequence(f, input, 1, 1);
    }

    private void _testSimpleRootSequence(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("a", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(4, r.getIntValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        assertNull(r.nextToken());
        assertFalse(r.isClosed());
        
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(12356, r.getIntValue());
        assertNull(r.nextToken());
        assertFalse(r.isClosed());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        // but this is the real end:
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
    */
}
