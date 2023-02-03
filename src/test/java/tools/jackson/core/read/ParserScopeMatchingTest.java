package tools.jackson.core.read;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
public class ParserScopeMatchingTest extends BaseTest
{
    public void testUnclosedArray()
    {
        for (int mode : ALL_MODES) {
            _testUnclosedArray(mode);
        }
    }

    public void _testUnclosedArray(int mode)
    {
        JsonParser p = createParser(mode, "[ 1, 2 ");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed ARRAY (mode: "+mode+")");
        } catch (StreamReadException pe) {
            verifyException(pe, "expected close marker for ARRAY");
        }
        p.close();
    }

    public void testUnclosedObject()
    {
        for (int mode : ALL_MODES) {
            _testUnclosedObject(mode);
        }
    }

    private void _testUnclosedObject(int mode)
    {
        JsonParser p = createParser(mode, "{ \"key\" : 3  ");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for unclosed OBJECT (mode: "+mode+")");
        } catch (StreamReadException pe) {
            verifyException(pe, "expected close marker for OBJECT");
        }
        p.close();
    }

    public void testEOFInName()
    {
        for (int mode : ALL_MODES) {
            _testEOFInName(mode);
        }
    }

    public void _testEOFInName(int mode)
    {
        final String JSON = "{ \"abcd";
        JsonParser p = createParser(mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for EOF");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected end-of-input");
        } catch (JacksonException ie) {
            // DataInput behaves bit differently
            if (mode == MODE_DATA_INPUT) {
                verifyException(ie, "end-of-input");
                return;
            }
            fail("Should not end up in here");
        }
        p.close();
    }

    public void testWeirdToken()
    {
        for (int mode : ALL_MODES) {
            _testWeirdToken(mode);
        }
    }

    private void _testWeirdToken(int mode)
    {
        final String JSON = "[ nil ]";
        JsonParser p = createParser(mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected an exception for weird token");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unrecognized token");
        }
        p.close();
    }

    public void testMismatchArrayToObject()
    {
        for (int mode : ALL_MODES) {
            _testMismatchArrayToObject(mode);
        }
    }

    private void _testMismatchArrayToObject(int mode)
    {
        final String JSON = "[ 1, 2 }";
        JsonParser p = createParser(mode, JSON);
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

    public void testMismatchObjectToArray()
    {
        for (int mode : ALL_MODES) {
            _testMismatchObjectToArray(mode);
        }
    }

    private void _testMismatchObjectToArray(int mode)
    {
        final String JSON = "{ ]";
        JsonParser p = createParser(mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        try {
            p.nextToken();
            fail("Expected an exception for incorrectly closed OBJECT");
        } catch (StreamReadException pe) {
            verifyException(pe, "Unexpected close marker ']': expected '}'");
        }
        p.close();
    }

    public void testMisssingColon()
    {
        for (int mode : ALL_MODES) {
            _testMisssingColon(mode);
        }
    }

    private void _testMisssingColon(int mode)
    {
        final String JSON = "{ \"a\" \"b\" }";
        JsonParser p = createParser(mode, JSON);

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
