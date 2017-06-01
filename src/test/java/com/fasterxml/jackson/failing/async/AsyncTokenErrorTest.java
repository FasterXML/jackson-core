package com.fasterxml.jackson.failing.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncTokenErrorTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    public void testInvalidKeywords() throws Exception
    {
        doTestInvalidKeyword1("nul");
        doTestInvalidKeyword1("Null");
        doTestInvalidKeyword1("nulla");
        doTestInvalidKeyword1("fal");
        doTestInvalidKeyword1("False");
        doTestInvalidKeyword1("fals0");
        doTestInvalidKeyword1("falsett0");
        doTestInvalidKeyword1("tr");
        doTestInvalidKeyword1("truE");
        doTestInvalidKeyword1("treu");
        doTestInvalidKeyword1("trueenough");
        doTestInvalidKeyword1("C");
    }

    private void doTestInvalidKeyword1(String value) throws IOException
    {
        String doc = "{ \"key1\" : "+value+" }";
        AsyncReaderWrapper p = createParser(doc);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        p = createParser(doc);
        try {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }
    }

    public void testMangledNumbers() throws Exception
    {
        String doc = "123true";
        AsyncReaderWrapper p = createParser(doc);
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        p.close();

        // Also test with floats
        doc = "1.5false";
        p = createParser(doc);
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }

    private AsyncReaderWrapper createParser(String doc) throws IOException
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
