package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TestParserErrorHandling
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testInvalidKeywordsStream() throws Exception {
        _testInvalidKeywords(true);
    }
    
    public void testInvalidKeywordsReader() throws Exception {
        _testInvalidKeywords(false);
    }

    // Tests for #105 ("eager number parsing misses errors")
    public void testMangledNumbersBytes() throws Exception {
        _testMangledNumbers(true);
    }

    public void testMangledNumbersChars() throws Exception {
        _testMangledNumbers(false);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _testInvalidKeywords(boolean useStream) throws Exception
    {
        doTestInvalidKeyword1(useStream, "nul");
        doTestInvalidKeyword1(useStream, "Null");
        doTestInvalidKeyword1(useStream, "nulla");
        doTestInvalidKeyword1(useStream, "fal");
        doTestInvalidKeyword1(useStream, "False");
        doTestInvalidKeyword1(useStream, "fals0");
        doTestInvalidKeyword1(useStream, "falsett0");
        doTestInvalidKeyword1(useStream, "tr");
        doTestInvalidKeyword1(useStream, "truE");
        doTestInvalidKeyword1(useStream, "treu");
        doTestInvalidKeyword1(useStream, "trueenough");
        doTestInvalidKeyword1(useStream, "C");
    }

    private void doTestInvalidKeyword1(boolean useStream, String value)
        throws IOException
    {
        final String doc = "{ \"key1\" : "+value+" }";
        JsonParser jp = useStream ? createParserUsingStream(doc, "UTF-8")
                : createParserUsingReader(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        /* 24-Nov-2008, tatu: Note that depending on parser impl, we may
         *   get the exception early or late...
         */
        try {
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            jp.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            jp.close();
        }

        // Try as root-level value as well:
        jp = useStream ? createParserUsingStream(value, "UTF-8")
                : createParserUsingReader(value);
        try {
            jp.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            jp.close();
        }
    }

    private void _testMangledNumbers(boolean useStream) throws Exception
    {
        String doc = "123true";
        JsonParser jp = useStream ? createParserUsingStream(doc, "UTF-8")
                : createParserUsingReader(doc);
        try {
            JsonToken t = jp.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        jp.close();

        // Also test with floats
        doc = "1.5false";
        jp = useStream ? createParserUsingStream(doc, "UTF-8")
                : createParserUsingReader(doc);
        try {
            JsonToken t = jp.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        jp.close();
    }
}
