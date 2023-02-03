package tools.jackson.core.json.async;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
public class AsyncScopeMatchingTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    public void testUnclosedArray() throws Exception
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
            verifyException(pe, "expected a value token");
        }
    }

    public void testUnclosedObject() throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc("{ \"key\" : 3  "), 0);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed OBJECT");
        } catch (StreamReadException pe) {
            verifyException(pe, "expected an Object property name or END_ARRAY");
        }
    }

    public void testEOFInName() throws Exception
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
        } catch (StreamReadException pe) {
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
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected close marker ']': expected '}'");
        }
        p.close();
    }

    public void testMisssingColon() throws Exception
    {
        final String JSON = "{ \"a\" \"b\" }";
        AsyncReaderWrapper p = asyncForBytes(JSON_F, 3, _jsonDoc(JSON), 0);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            // can be either here, or with next one...
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for missing semicolon");
        } catch (StreamReadException pe) {
            verifyException(pe, "was expecting a colon");
        }
        p.close();
    }
}
