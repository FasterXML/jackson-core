package tools.jackson.core.unittest.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.unittest.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

//Failing tests for non-root-token problem [core#1557]
class ParserErrorHandling1557Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    // [core#1557]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledFloats1557Bytes() throws Exception {
        _testNonRootMangledFloats1557(MODE_INPUT_STREAM);
        _testNonRootMangledFloats1557(MODE_INPUT_STREAM_THROTTLED);
    }

    // [core#1557]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledFloats1557DataInput() throws Exception {
        _testNonRootMangledFloats1557(MODE_DATA_INPUT);
    }

    // [core#1557]
    @Test
    @JacksonTestFailureExpected
    void nonRootMangledFloats1557Chars() throws Exception {
        _testNonRootMangledFloats1557(MODE_READER);
    }

    // [core#1557]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledInts1557Bytes() throws Exception {
        _testNonRootMangledInts(MODE_INPUT_STREAM);
        _testNonRootMangledInts(MODE_INPUT_STREAM_THROTTLED);
    }

    // [core#1557]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledInts1557DataInput() throws Exception {
        _testNonRootMangledInts(MODE_DATA_INPUT);
    }

    // [core#1557]
    @JacksonTestFailureExpected
    @Test
    void nonRootMangledInts1557Chars() throws Exception {
        _testNonRootMangledInts(MODE_READER);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testNonRootMangledFloats1557(int mode) throws Exception {
        _testNonRootMangledFloats1557(mode, "1.5x");
    }

    private void _testNonRootMangledFloats1557(int mode, String value) throws Exception
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
