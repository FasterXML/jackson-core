package tools.jackson.failing.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.fail;

class AsyncTokenErrorTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void invalidKeywordsStartOk() throws Exception
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
    void invalidKeywordsStartFail() throws Exception
    {
        _doTestInvalidKeyword("Null");
        _doTestInvalidKeyword("False");
        _doTestInvalidKeyword("C");
    }

    private void _doTestInvalidKeyword(String value)
    {
        String doc = "{ \"key1\" : "+value+" }";
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try (AsyncReaderWrapper p = _createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        try (AsyncReaderWrapper p = _createParser(doc)) {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        }
    }

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

    private AsyncReaderWrapper _createParser(String doc)
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
