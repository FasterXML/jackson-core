package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
class AsyncScopeMatchingTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void unclosedArray() throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc("[ 1, 2 "), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed ARRAY");
        } catch (StreamReadException pe) {
            // 21-Mar-2024, tatu: Not ideal but has to do -- test was not run
            //   for a while so exact message was not checked
            //verifyException(pe, "expected close marker for ARRAY");
            verifyException(pe, "Unexpected end-of-input");
        }
    }

    @Test
    void unclosedObject() throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc("{ \"key\" : 3  "), 0);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed OBJECT");
        } catch (StreamReadException pe) {
            // 21-Mar-2024, tatu: Not ideal but has to do -- test was not run
            //   for a while so exact message was not checked
            //verifyException(pe, "expected close marker for OBJECT");
            verifyException(pe, "Unexpected end-of-input");
        }
    }

    @Test
    void eofInName() throws Exception
    {
        final String JSON = "{ \"abcd";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for EOF");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected end-of-input");
        }
    }

    @Test
    void mismatchArrayToObject() throws Exception
    {
        final String JSON = "[ 1, 2 }";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for incorrectly closed ARRAY");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected close marker '}': expected ']'");
        }
        p.close();
    }

    @Test
    void mismatchObjectToArray() throws Exception
    {
        final String JSON = "{ ]";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for incorrectly closed OBJECT");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected close marker ']': expected '}'");
        }
        p.close();
    }

    @Test
    void misssingColon() throws Exception
    {
        final String JSON = "{ \"a\" \"b\" }";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            // can be either here, or with next one...
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for missing semicolon");
        } catch (StreamReadException pe) {
            verifyException(pe, "was expecting a colon");
        }
        p.close();
    }
}
