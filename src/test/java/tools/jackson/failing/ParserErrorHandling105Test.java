package tools.jackson.failing;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;

// Failing tests for non-root-token problem
public class ParserErrorHandling105Test
    extends tools.jackson.core.BaseTest
{
    // Tests for [core#105] ("eager number parsing misses errors")
    public void testMangledIntsBytes() throws Exception {
        // 02-Jun-2017, tatu: Fails to fail; should check whether this is expected
        //   (since DataInput can't do look-ahead)
        _testMangledNonRootInts(MODE_DATA_INPUT);
    }

    public void testMangledFloatsBytes() throws Exception {
//        _testMangledNonRootFloats(MODE_INPUT_STREAM);
//        _testMangledNonRootFloats(MODE_INPUT_STREAM_THROTTLED);

        // 02-Jun-2017, tatu: Fails as expected, unlike int one. Bit puzzling...
        _testMangledNonRootFloats(MODE_DATA_INPUT);
    }

    public void testMangledIntsChars() {
        _testMangledNonRootInts(MODE_READER);
    }

    public void testMangledFloatsChars() {
        _testMangledNonRootFloats(MODE_READER);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testMangledNonRootInts(int mode)
    {
        JsonParser p = createParser(mode, "[ 123true ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }

    private void _testMangledNonRootFloats(int mode)
    {
        JsonParser p = createParser(mode, "[ 1.5false ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }
}
