package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import java.io.IOException;

public class AsyncNonStandardNumberParsingTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    public void testHexadecimal() throws Exception
    {
        final String JSON = "[ 0xc0ffee ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('x'");
        } finally {
            p.close();
        }
    }

    public void testHexadecimalBigX() throws Exception
    {
        final String JSON = "[ 0XC0FFEE ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('X'");
        } finally {
            p.close();
        }
    }

    public void testNegativeHexadecimal() throws Exception
    {
        final String JSON = "[ -0xc0ffee ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('x'");
        } finally {
            p.close();
        }
    }

    public void testFloatMarker() throws Exception
    {
        final String JSON = "[ -0.123f ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('f'");
        } finally {
            p.close();
        }
    }

    public void testDoubleMarker() throws Exception
    {
        final String JSON = "[ -0.123d ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('d'");
        } finally {
            p.close();
        }
    }

    /**
     * The format ".NNN" (as opposed to "0.NNN") is not valid JSON, so this should fail
     */
    public void testLeadingDotInDecimal() throws Exception {
        final String JSON = "[ .123 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Unexpected character ('.'");
        } finally {
            p.close();
        }
    }

    /*
     * The format "+NNN" (as opposed to "NNN") is not valid JSON, so this should fail
     */
    public void testLeadingPlusSignInDecimalDefaultFail() throws Exception {
        final String JSON = "[ +123 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            //the message does not match non-async parsers
            verifyException(e, "Unrecognized token '+123'");
        } finally {
            p.close();
        }
    }

    /**
     * The format "NNN." (as opposed to "NNN") is not valid JSON, so this should fail
     */
    public void testTrailingDotInDecimal() throws Exception {
        final String JSON = "[ 123. ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            //the message does not match non-async parsers
            verifyException(e, "Unexpected character ((CTRL-CHAR, code 0))");
        } finally {
            p.close();
        }
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return asyncForBytes(f, readBytes, _jsonDoc(doc), 1);
    }
}
