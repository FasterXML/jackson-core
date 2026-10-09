package tools.jackson.core.unittest.async;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// 09-Oct-2026, tatu: [core#1742] feed split inside escaped surrogate pair
//   of a property name; all chunk sizes so every split offset gets covered
class AsyncSplitSurrogateInFieldName1742Test extends AsyncTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();
    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void splitInSurrogatePair(boolean byteBuffer) throws Exception
    {
        _testAllSplits(byteBuffer, FACTORY, "{\"\\uD83D\\uDE00\":1}");
        _testAllSplits(byteBuffer, FACTORY, "{\"ab\\uD83D\\uDE00cd\":1}");
        _testAllSplits(byteBuffer, FACTORY, "{\"x\":{\"\\uD83D\\uDE00\":[true,\"v\"]}}");
        _testAllSplits(byteBuffer, FACTORY, "{\"\\uD83D\\uDE00\\uD834\\uDD1E\":-1.5e3}");
        _testAllSplits(byteBuffer, FACTORY, "{\"a\":1,\"\\ud834\\udd1e\":\"\\ud834\\udd1e\"}");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void splitInSurrogatePairApos(boolean byteBuffer) throws Exception
    {
        _testAllSplits(byteBuffer, APOS_FACTORY, "{'\\uD83D\\uDE00':1}");
        _testAllSplits(byteBuffer, APOS_FACTORY, "{'ab\\uD83D\\uDE00cd':'x'}");
    }

    // End-of-input within surrogate pair escape must fail, not hang or hit internal error
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void eofInSurrogatePair(boolean byteBuffer) throws Exception
    {
        for (String doc : new String[] {
                "{\"\\uD83D", "{\"\\uD83D\\", "{\"\\uD83D\\u", "{\"\\uD83D\\uDE", "{\"\\uD83D\\uDE0"
        }) {
            _testAllSplitsFail(byteBuffer, FACTORY, doc,
                    "Unexpected end-of-input in character escape sequence");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void brokenSurrogatePair(boolean byteBuffer) throws Exception
    {
        _testAllSplitsFail(byteBuffer, FACTORY, "{\"\\uD83D\":1}", "Broken surrogate pair");
        _testAllSplitsFail(byteBuffer, FACTORY, "{\"\\uD83Dx\":1}", "Broken surrogate pair");
        _testAllSplitsFail(byteBuffer, FACTORY, "{\"\\uD83D\\u0041\":1}", "Broken surrogate pair");
        _testAllSplitsFail(byteBuffer, FACTORY, "{\"\\uD83D\\n\":1}", "Broken surrogate pair");
        _testAllSplitsFail(byteBuffer, FACTORY, "{\"\\uD83D\\uDEx0\":1}", "expected a hex-digit");
    }

    private void _testAllSplits(boolean byteBuffer, JsonFactory f, String json) throws Exception
    {
        final byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        final String exp = _blockingTokens(f, doc);
        for (int chunk = 1; chunk <= doc.length; ++chunk) {
            try (AsyncReaderWrapper r = _wrap(byteBuffer, f, chunk, doc)) {
                assertEquals(exp, _asyncTokens(r), "chunk size "+chunk+", byteBuffer="+byteBuffer);
            }
        }
    }

    private void _testAllSplitsFail(boolean byteBuffer, JsonFactory f, String json,
            String expMsg) throws Exception
    {
        final byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        for (int chunk = 1; chunk <= doc.length; ++chunk) {
            try (AsyncReaderWrapper r = _wrap(byteBuffer, f, chunk, doc)) {
                String tokens = _asyncTokens(r);
                fail("Should fail for chunk size "+chunk+", byteBuffer="+byteBuffer+"; got: "+tokens);
            } catch (StreamReadException e) {
                verifyException(e, expMsg);
            }
        }
    }

    private AsyncReaderWrapper _wrap(boolean byteBuffer, JsonFactory f, int chunk, byte[] doc)
        throws Exception
    {
        return byteBuffer ? asyncForByteBuffer(f, chunk, doc, 0)
                : asyncForBytes(f, chunk, doc, 0);
    }

    private String _blockingTokens(JsonFactory f, byte[] doc) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        try (JsonParser p = f.createParser(doc)) {
            JsonToken t;
            while ((t = p.nextToken()) != null) {
                sb.append(t).append(':').append(p.getString()).append('|');
            }
        }
        return sb.toString();
    }

    private String _asyncTokens(AsyncReaderWrapper r) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        JsonToken t;
        while ((t = r.nextToken()) != null) {
            sb.append(t).append(':').append(r.currentText()).append('|');
        }
        return sb.toString();
    }
}
