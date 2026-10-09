package com.fasterxml.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.fail;

// [core#1506]: Fixed in 3.1; backported to 2.21.8
// Tests for handling token decoding fails for Root values
class AsyncTokenRootError1506Test extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

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

    private AsyncReaderWrapper _createParser(String doc) throws Exception
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
