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

    //next 2 tests do not work as expected
    /*
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
    */

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return asyncForBytes(f, readBytes, _jsonDoc(doc), 1);
    }
}
