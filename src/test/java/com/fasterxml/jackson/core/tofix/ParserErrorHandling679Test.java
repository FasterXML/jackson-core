package com.fasterxml.jackson.core.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ParserErrorHandling679Test
    extends com.fasterxml.jackson.core.JUnit5TestBase
{
    // [core#679]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledFloats679Bytes() throws Exception {
        _testNonRootMangledFloats679(MODE_INPUT_STREAM);
        _testNonRootMangledFloats679(MODE_INPUT_STREAM_THROTTLED);
        _testNonRootMangledFloats679(MODE_DATA_INPUT);
    }

    // [core#679]
    @Test
    @JacksonTestFailureExpected
    void nonRootMangledFloats679Chars() throws Exception {
        _testNonRootMangledFloats679(MODE_READER);
    }

    // [core#679]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledInts679Bytes() throws Exception {
        _testNonRootMangledInts(MODE_INPUT_STREAM);
        _testNonRootMangledInts(MODE_INPUT_STREAM_THROTTLED);
        _testNonRootMangledInts(MODE_DATA_INPUT);
        _testNonRootMangledInts(MODE_READER);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testNonRootMangledFloats679(int mode) throws Exception {
        _testNonRootMangledFloats679(mode, "1.5x");
        _testNonRootMangledFloats679(mode, "1.5.00");
    }

    private void _testNonRootMangledFloats679(int mode, String value) throws Exception
    {
        // Also test with floats
        try (JsonParser p = createParser(mode, "[ "+value+" ]")) {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            Double v = p.getDoubleValue();
            fail("Should have gotten an exception for '"+value+"'; instead got ("+t+") number: "+v);
        } catch (StreamReadException e) {
            verifyException(e, "expected ");
        }
    }

    private void _testNonRootMangledInts(int mode) throws Exception {
        _testNonRootMangledInts(mode, "100k");
        _testNonRootMangledInts(mode, "100/");
    }

    private void _testNonRootMangledInts(int mode, String value) throws Exception
    {
        // Also test with floats
        try (JsonParser p = createParser(mode, "[ "+value+" ]")) {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            try {
                JsonToken t = p.nextToken();
                int v = p.getIntValue();
                fail("Should have gotten an exception for '" + value + "'; instead got (" + t + ") number: " + v);
            } catch (StreamReadException e) {
                verifyException(e, "expected ");
            }
        }
    }
}
