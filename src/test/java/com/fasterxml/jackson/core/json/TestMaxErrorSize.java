package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test size of parser error messages
 */
public class TestMaxErrorSize
    extends com.fasterxml.jackson.core.JUnit5TestBase
{
    private final static int EXPECTED_MAX_TOKEN_LEN = 256; // ParserBase.MAX_ERROR_TOKEN_LENGTH

    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void longErrorMessage() throws Exception
    {
        _testLongErrorMessage(MODE_INPUT_STREAM);
        _testLongErrorMessage(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void longErrorMessageReader() throws Exception
    {
        _testLongErrorMessage(MODE_READER);
    }

    private void _testLongErrorMessage(int mode) throws Exception
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertTrue(DOC.length() > 256);
        JsonParser jp = createParser(JSON_F, mode, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
        	String msg = jpe.getMessage();
          final String expectedPrefix = "Unrecognized token '";
          final String expectedSuffix = "...': was expecting";
        	verifyException(jpe,  expectedPrefix);
          verifyException(jpe,  expectedSuffix);
        	assertTrue(msg.contains(expectedSuffix));
          int tokenLen = msg.indexOf (expectedSuffix) - expectedPrefix.length();
        	assertEquals(EXPECTED_MAX_TOKEN_LEN, tokenLen);
        }
        jp.close();
    }

    @Test
    void shortErrorMessage() throws Exception
    {
        _testShortErrorMessage(MODE_INPUT_STREAM);
        _testShortErrorMessage(MODE_INPUT_STREAM_THROTTLED);
        _testShortErrorMessage(MODE_READER);
    }

    public void _testShortErrorMessage(int mode) throws Exception
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertTrue(DOC.length() < 256);
        JsonParser jp = createParser(JSON_F, mode, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
            String msg = jpe.getMessage();
            final String expectedPrefix = "Unrecognized token '";
            final String expectedSuffix = "': was expecting";
            verifyException(jpe,  expectedPrefix);
            verifyException(jpe,  expectedSuffix);
            int tokenLen = msg.indexOf(expectedSuffix) - expectedPrefix.length();
            assertEquals(DOC.length(), tokenLen);
        }
        jp.close();
    }
}

