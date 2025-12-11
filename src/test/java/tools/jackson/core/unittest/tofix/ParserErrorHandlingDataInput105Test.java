package tools.jackson.core.unittest.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.unittest.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.fail;

// Failing tests for non-root-token problem
class ParserErrorHandlingDataInput105Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    // Tests for [core#105] ("eager number parsing misses errors")
    @JacksonTestFailureExpected
    @Test
    void mangledIntsDataInput() throws Exception {
        // 02-Jun-2017, tatu: Fails to fail; should check whether this is expected
        //   (since DataInput can't do look-ahead)
        _testMangledNonRootInts(MODE_DATA_INPUT);
    }

    @JacksonTestFailureExpected
    @Test
    void mangledFloatsDataInput() throws Exception {
        _testMangledNonRootFloats(MODE_DATA_INPUT);
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
