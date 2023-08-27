package com.fasterxml.jackson.failing.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncTokenErrorTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testInvalidKeywordsStartOk() throws Exception
    {
        _doTestInvalidKeyword("nul");
        _doTestInvalidKeyword("nulla");
        _doTestInvalidKeyword("fal");
        _doTestInvalidKeyword("fals0");
        _doTestInvalidKeyword("falsett0");
        _doTestInvalidKeyword("tr");
        _doTestInvalidKeyword("truE");
        _doTestInvalidKeyword("treu");
        _doTestInvalidKeyword("trueenough");
    }

    public void testInvalidKeywordsStartFail() throws Exception
    {
        _doTestInvalidKeyword("Null");
        _doTestInvalidKeyword("False");
        _doTestInvalidKeyword("C");
    }

    private void _doTestInvalidKeyword(String value) throws IOException
    {
        String doc = "{ \"key1\" : "+value+" }";
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try (AsyncReaderWrapper p = _createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        try (AsyncReaderWrapper p = _createParser(doc)) {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        }
    }

    public void testMangledRootInts() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("123true")) {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t+"; number: "+p.getNumberValue());
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }

    public void testMangledRootFloats() throws Exception
    {
        // Also test with floats
        try (AsyncReaderWrapper p = _createParser("1.5false")) {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t+"; number: "+p.getNumberValue());
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }

    public void testMangledNonRootInts() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("[ 123true ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }

    public void testMangledNonRootFloats() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("[ 1.5false ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }

    private AsyncReaderWrapper _createParser(String doc) throws IOException
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
