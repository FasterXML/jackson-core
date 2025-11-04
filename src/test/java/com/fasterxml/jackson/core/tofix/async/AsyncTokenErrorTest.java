package com.fasterxml.jackson.core.tofix.async;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.fail;

class AsyncTokenErrorTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void invalidKeywordsAfterMatching1st() throws Exception
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

    @Test
    void invalidKeywordsAfterNonMatching1st() throws Exception
    {
        _doTestInvalidKeyword("Null");
        _doTestInvalidKeyword("False");
        _doTestInvalidKeyword("C");
        _doTestInvalidKeyword("xy");
    }

    private void _doTestInvalidKeyword(String value) throws IOException
    {
        final String EXP_MAIN = "Unrecognized token '"+value+"'";
        final String EXP_ALT = "Unexpected character ('"+value.charAt(0)+"' (code";
        
        String doc = "{ \"key1\" : "+value+" }";
        try (AsyncReaderWrapper p = _createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, EXP_MAIN, EXP_ALT);
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        try (AsyncReaderWrapper p = _createParser(doc)) {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, EXP_MAIN, EXP_ALT);
        }
    }
    
    @JacksonTestFailureExpected
    @Test
    void mangledRootInts() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("123true")) {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t+"; number: "+p.getNumberValue());
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }

    @JacksonTestFailureExpected
    @Test
    void mangledRootFloats() throws Exception
    {
        // Also test with floats
        try (AsyncReaderWrapper p = _createParser("1.5false")) {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t+"; number: "+p.getNumberValue());
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }

    @JacksonTestFailureExpected
    @Test
    void mangledNonRootInts() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("[ 123true ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }

    @JacksonTestFailureExpected
    @Test
    void mangledNonRootFloats() throws Exception
    {
        try (AsyncReaderWrapper p = _createParser("[ 1.5false ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }

    private AsyncReaderWrapper _createParser(String doc) throws IOException
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
