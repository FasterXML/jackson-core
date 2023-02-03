package com.fasterxml.jackson.core.json.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncRootValuesTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    /*
    /**********************************************************************
    /* Simple token (true, false, null) tests
    /**********************************************************************
     */

    public void testTokenRootTokens() throws Exception {
        _testTokenRootTokens(JsonToken.VALUE_TRUE, "true");
        _testTokenRootTokens(JsonToken.VALUE_FALSE, "false");
        _testTokenRootTokens(JsonToken.VALUE_NULL, "null");

        _testTokenRootTokens(JsonToken.VALUE_TRUE, "true  ");
        _testTokenRootTokens(JsonToken.VALUE_FALSE, "false  ");
        _testTokenRootTokens(JsonToken.VALUE_NULL, "null  ");
    }

    private void _testTokenRootTokens(JsonToken expToken, String doc) throws Exception
    {
        byte[] input = _jsonDoc(doc);
        JsonFactory f = JSON_F;
        _testTokenRootTokens(expToken, f, input, 0, 90);
        _testTokenRootTokens(expToken, f, input, 0, 3);
        _testTokenRootTokens(expToken, f, input, 0, 2);
        _testTokenRootTokens(expToken, f, input, 0, 1);

        _testTokenRootTokens(expToken, f, input, 1, 90);
        _testTokenRootTokens(expToken, f, input, 1, 3);
        _testTokenRootTokens(expToken, f, input, 1, 1);
    }

    private void _testTokenRootTokens(JsonToken expToken, JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(expToken, r.nextToken());
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }

    /*
    /**********************************************************************
    /* Root-level sequences
    /**********************************************************************
     */

    public void testTokenRootSequence() throws Exception
    {
        byte[] input = _jsonDoc("\n[ true, false,\nnull  ,null\n,true,false]");

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

    public void testMixedRootSequence() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(100);

        // Let's simply concatenate documents...
        bytes.write(_jsonDoc("{ \"a\" : 4 }"));
        bytes.write(_jsonDoc("[ 12, -987,false ]"));
        bytes.write(_jsonDoc(" 12356"));
        bytes.write(_jsonDoc(" true"));
        byte[] input = bytes.toByteArray();

        JsonFactory f = JSON_F;
        _testMixedRootSequence(f, input, 0, 100);
        _testMixedRootSequence(f, input, 0, 3);
        _testMixedRootSequence(f, input, 0, 1);

        _testMixedRootSequence(f, input, 1, 100);
        _testMixedRootSequence(f, input, 1, 3);
        _testMixedRootSequence(f, input, 1, 1);
    }

    private void _testMixedRootSequence(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        // { "a":4 }
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("a", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(4, r.getIntValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // [ 12, -987, false]
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(12, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(-987, r.getIntValue());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(12356, r.getIntValue());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
}
