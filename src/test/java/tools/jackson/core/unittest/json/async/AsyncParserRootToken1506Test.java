package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.fail;

// [core#1506] Async parser: invalid root-level tokens not reported properly
class AsyncParserRootToken1506Test extends AsyncTestBase
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

    private void _doTestInvalidKeyword(String value)
    {
        final String EXP_MAIN = "Unrecognized token '"+value+"'";
        final String EXP_ALT = "Unexpected character ('"+value.charAt(0)+"' (code";

        // Try as root-level value as well:
        String doc = value + " "; // may need space after for DataInput
        try (AsyncReaderWrapper p = _createParser(doc)) {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, EXP_MAIN, EXP_ALT);
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

    private AsyncReaderWrapper _createParser(String doc)
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
