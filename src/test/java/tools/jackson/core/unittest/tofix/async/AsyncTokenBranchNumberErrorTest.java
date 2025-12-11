package tools.jackson.core.unittest.tofix.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;
import tools.jackson.core.unittest.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.fail;

// Tests for handling token decoding fails for non-Root values
// (within Object / Array)
class AsyncTokenBranchNumberErrorTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @JacksonTestFailureExpected
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

    @JacksonTestFailureExpected
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
