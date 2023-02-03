package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
public class AsyncScopeMatchingTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testUnclosedArray(int mode) throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc("[ 1, 2 "), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed ARRAY (mode: "+mode+")");
        } catch (JsonParseException pe) {
            verifyException(pe, "expected close marker for ARRAY");
        }
    }

    public void testUnclosedObject(int mode) throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc("{ \"key\" : 3  "), 0);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed OBJECT (mode: "+mode+")");
        } catch (JsonParseException pe) {
            verifyException(pe, "expected close marker for OBJECT");
        }
    }

    public void testEOFInName(int mode) throws Exception
    {
        final String JSON = "{ \"abcd";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for EOF");
        } catch (JsonParseException pe) {
            verifyException(pe, "Unexpected end-of-input");
        } catch (IOException ie) {
            // DataInput behaves bit differently
            if (mode == MODE_DATA_INPUT) {
                verifyException(ie, "end-of-input");
                return;
            }
        }
    }

    public void testMismatchArrayToObject() throws Exception
    {
        final String JSON = "[ 1, 2 }";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for incorrectly closed ARRAY");
        } catch (JsonParseException pe) {
            verifyException(pe, "Unexpected close marker '}': expected ']'");
        }
        p.close();
    }

    public void testMismatchObjectToArray() throws Exception
    {
        final String JSON = "{ ]";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for incorrectly closed OBJECT");
        } catch (JsonParseException pe) {
            verifyException(pe, "Unexpected close marker ']': expected '}'");
        }
        p.close();
    }

    public void testMisssingColon(int mode) throws Exception
    {
        final String JSON = "{ \"a\" \"b\" }";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            // can be either here, or with next one...
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for missing semicolon");
        } catch (JsonParseException pe) {
            verifyException(pe, "was expecting a colon");
        }
        p.close();
    }
}
