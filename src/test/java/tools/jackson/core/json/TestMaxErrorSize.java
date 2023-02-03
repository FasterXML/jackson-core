package tools.jackson.core.json;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;

/**
 * Test size of parser error messages
 */
public class TestMaxErrorSize
    extends tools.jackson.core.BaseTest
{
    private final static int EXPECTED_MAX_TOKEN_LEN = 256; // ParserBase.MAX_ERROR_TOKEN_LENGTH

    private final JsonFactory JSON_F = newStreamFactory();

    public void testLongErrorMessage()
    {
        _testLongErrorMessage(MODE_INPUT_STREAM);
        _testLongErrorMessage(MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLongErrorMessageReader()
    {
        _testLongErrorMessage(MODE_READER);
    }

    private void _testLongErrorMessage(int mode)
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
        } catch (StreamReadException jpe) {
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

    public void testShortErrorMessage()
    {
        _testShortErrorMessage(MODE_INPUT_STREAM);
        _testShortErrorMessage(MODE_INPUT_STREAM_THROTTLED);
        _testShortErrorMessage(MODE_READER);
    }

    public void _testShortErrorMessage(int mode)
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertTrue(DOC.length() < 256);
        JsonParser jp = createParser(JSON_F, mode, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (StreamReadException jpe) {
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

