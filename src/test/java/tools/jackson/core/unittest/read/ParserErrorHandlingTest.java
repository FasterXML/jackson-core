package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.fail;

class ParserErrorHandlingTest
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void invalidKeywordsBytes() throws Exception {
        _testInvalidKeywords(MODE_INPUT_STREAM);
        _testInvalidKeywords(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidKeywords(MODE_DATA_INPUT);
    }

    @Test
    void invalidKeywordsChars() throws Exception {
        _testInvalidKeywords(MODE_READER);
    }

    // Tests for [core#105] ("eager number parsing misses errors")
    @Test
    void mangledRootIntsBytes() throws Exception {
        _testMangledRootNumbersInt(MODE_INPUT_STREAM);
        _testMangledRootNumbersInt(MODE_INPUT_STREAM_THROTTLED);
        _testMangledRootNumbersInt(MODE_DATA_INPUT);
    }

    @Test
    void mangledRootFloatsBytes() throws Exception {
        _testMangledRootNumbersFloat(MODE_INPUT_STREAM);
        _testMangledRootNumbersFloat(MODE_INPUT_STREAM_THROTTLED);
        _testMangledRootNumbersFloat(MODE_DATA_INPUT);
    }

    @Test
    void mangledRootNumbersChars() throws Exception {
        _testMangledRootNumbersInt(MODE_READER);
        _testMangledRootNumbersFloat(MODE_READER);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testInvalidKeywords(int mode)
    {
        doTestInvalidKeyword1(mode, "nul");
        doTestInvalidKeyword1(mode, "Null");
        doTestInvalidKeyword1(mode, "nulla");
        doTestInvalidKeyword1(mode, "fal");
        doTestInvalidKeyword1(mode, "False");
        doTestInvalidKeyword1(mode, "fals0");
        doTestInvalidKeyword1(mode, "falsett0");
        doTestInvalidKeyword1(mode, "tr");
        doTestInvalidKeyword1(mode, "truE");
        doTestInvalidKeyword1(mode, "treu");
        doTestInvalidKeyword1(mode, "trueenough");
        doTestInvalidKeyword1(mode, "C");

        // Multi-byte (non-ASCII) identifier chars right after keyword
        doTestInvalidKeyword1(mode, "trueé");
        doTestInvalidKeyword1(mode, "nullé");
        doTestInvalidKeyword1(mode, "false中x");

        // Multi-byte (non-ASCII) identifier char at start of token
        doTestInvalidKeyword1(mode, "éabc");
        doTestInvalidKeyword1(mode, "中x");

        // Multi-byte char that is NOT part of token (NBSP): after keyword, or at value start
        _testNonTokenChar(mode, "[true\u00A0]");
        _testNonTokenChar(mode, "{\"a\":null\u00A0}");
        _testNonTokenChar(mode, "[\u00A0]");
        _testNonTokenChar(mode, "{\"a\":\u00A0}");
    }

    private void doTestInvalidKeyword1(int mode, String value)
    {
        String doc = "{ \"key1\" : "+value+" }";
        JsonParser p = createParser(JSON_F, mode, doc);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try {
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        p = createParser(JSON_F, mode, doc);
        try {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }
    }

    // Must be reported as an error: not skipped, nor mis-decoded
    private void _testNonTokenChar(int mode, String doc)
    {
        try (JsonParser p = createParser(JSON_F, mode, doc)) {
            while (p.nextToken() != null) { }
            fail("Expected an exception for invalid non-token char; doc: "+doc);
        } catch (StreamReadException jex) {
            verifyException(jex, "Unexpected character");
        }
    }

    private void _testMangledRootNumbersInt(int mode)
    {
        JsonParser p = createParser(JSON_F, mode, "123true");
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }

    private void _testMangledRootNumbersFloat(int mode)
    {
        // Also test with floats
        JsonParser p = createParser(JSON_F, mode, "1.5false");
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }
}
