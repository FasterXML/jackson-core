package com.fasterxml.jackson.core.main;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.test.BaseTest;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
public class TestScopeMatching
    extends BaseTest
{
    public void testUnclosedArray() throws Exception
    {
        @SuppressWarnings("resource")
        JsonParser jp = createParserUsingReader("[ 1, 2");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for unclosed ARRAY");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "expected close marker for ARRAY");
        }
    }

    public void testUnclosedObject() throws Exception
    {
        @SuppressWarnings("resource")
        JsonParser jp = createParserUsingReader("{ \"key\" : 3  ");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for unclosed OBJECT");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "expected close marker for OBJECT");
        }
    }

    public void testEOFInName()
        throws Exception
    {
        final String JSON = "{ \"abcd";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp = (i == 0) ? createParserUsingReader(JSON)
                : createParserUsingStream(JSON, "UTF-8");
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            try {
                jp.nextToken();
                fail("Expected an exception for EOF");
            } catch (JsonParseException jpe) {
                verifyException(jpe, "Unexpected end-of-input");
            }
            jp.close();
        }
    }

    public void testWeirdToken()
        throws Exception
    {
        final String JSON = "[ nil ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp = (i == 0) ? createParserUsingReader(JSON)
                : createParserUsingStream(JSON, "UTF-8");
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            try {
                jp.nextToken();
                fail("Expected an exception for weird token");
            } catch (JsonParseException jpe) {
                verifyException(jpe, "Unrecognized token");
            }
            jp.close();
        }
    }

    public void testMismatchArrayToObject()
        throws Exception
    {
        final String JSON = "[ 1, 2 }";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp = (i == 0) ? createParserUsingReader(JSON)
                : createParserUsingStream(JSON, "UTF-8");
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            try {
                jp.nextToken();
                fail("Expected an exception for incorrectly closed ARRAY");
            } catch (JsonParseException jpe) {
                verifyException(jpe, "Unexpected close marker '}': expected ']'");
            }
            jp.close();
        }
    }

    public void testMismatchObjectToArray()
        throws Exception
    {
        final String JSON = "{ ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp = (i == 0) ? createParserUsingReader(JSON)
                : createParserUsingStream(JSON, "UTF-8");
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            
            try {
                jp.nextToken();
                fail("Expected an exception for incorrectly closed OBJECT");
            } catch (JsonParseException jpe) {
                verifyException(jpe, "Unexpected close marker ']': expected '}'");
            }
            jp.close();
        }
    }

    public void testMisssingColon()
        throws Exception
    {
        final String JSON = "{ \"a\" \"b\" }";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp = (i == 0) ? createParserUsingReader(JSON)
                : createParserUsingStream(JSON, "UTF-8");
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            try {
                // can be either here, or with next one...
                assertToken(JsonToken.FIELD_NAME, jp.nextToken());
                jp.nextToken();
                fail("Expected an exception for missing semicolon");
            } catch (JsonParseException jpe) {
                verifyException(jpe, "was expecting a colon");
            }
            jp.close();
        }
    }
}
