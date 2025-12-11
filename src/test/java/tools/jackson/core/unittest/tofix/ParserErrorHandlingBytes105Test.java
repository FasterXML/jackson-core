package tools.jackson.core.unittest.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.unittest.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.fail;

// Failing tests for non-root-token problem
class ParserErrorHandlingBytes105Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    // Tests for [core#105] ("eager number parsing misses errors")
    @JacksonTestFailureExpected
    @Test
    void mangledIntsBytes() throws Exception {
        _testMangledNonRootInts(MODE_INPUT_STREAM);
        _testMangledNonRootInts(MODE_INPUT_STREAM_THROTTLED);
    }

    @JacksonTestFailureExpected
    @Test
    void mangledFloatsBytes() throws Exception {
        _testMangledNonRootFloats(MODE_INPUT_STREAM);
        _testMangledNonRootFloats(MODE_INPUT_STREAM_THROTTLED);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testMangledNonRootInts(int mode)
    {
        try (JsonParser p = createParser(mode, "[ 123true ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }

    private void _testMangledNonRootFloats(int mode)
    {
        try (JsonParser p = createParser(mode, "[ 1.5false ]")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
    }
}
