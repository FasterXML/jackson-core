package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.fail;

// Failing tests for non-root-token problem
class ParserErrorHandling105Test
        extends com.fasterxml.jackson.core.JUnit5TestBase
{
    // Tests for [core#105] ("eager number parsing misses errors")
    @Test
    void mangledIntsBytes() throws Exception {
        // 02-Jun-2017, tatu: Fails to fail; should check whether this is expected
        //   (since DataInput can't do look-ahead)
        _testMangledNonRootInts(MODE_DATA_INPUT);
    }

    @Test
    void mangledFloatsBytes() throws Exception {
//        _testMangledNonRootFloats(MODE_INPUT_STREAM);
//        _testMangledNonRootFloats(MODE_INPUT_STREAM_THROTTLED);

        // 02-Jun-2017, tatu: Fails as expected, unlike int one. Bit puzzling...
        _testMangledNonRootFloats(MODE_DATA_INPUT);
    }

    @Test
    void mangledIntsChars() throws Exception {
        _testMangledNonRootInts(MODE_READER);
    }

    @Test
    void mangledFloatsChars() throws Exception {
        _testMangledNonRootFloats(MODE_READER);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testMangledNonRootInts(int mode) throws Exception
    {
        try (JsonParser p = createParser(mode, "[ 123true ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }

    private void _testMangledNonRootFloats(int mode) throws Exception
    {
        try (JsonParser p = createParser(mode, "[ 1.5false ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
    }
}
