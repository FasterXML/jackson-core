package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// '}' right after ':' or an Object ',' must be rejected regardless of
// where feed chunks end (and of white space / comments after the separator)
class AsyncSeparatorAtFeedBoundary1831Test extends AsyncTestBase
{
    private final JsonFactory F_DEFAULT = new JsonFactory();

    private final JsonFactory F_COMMENTS = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();

    private final JsonFactory F_YAML_COMMENTS = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .build();

    private final JsonFactory F_TRAILING_COMMA = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    @Test
    void missingValueAfterColon() throws Exception
    {
        _verifyFails(F_DEFAULT, "{\"a\":}");
        _verifyFails(F_DEFAULT, "{\"a\": }");
        _verifyFails(F_DEFAULT, "[{\"a\":}]");
        _verifyFails(F_DEFAULT, "{\"abc\":1,\"b\":}");
        _verifyFails(F_COMMENTS, "{\"a\":/*x*/}");
        _verifyFails(F_YAML_COMMENTS, "{\"a\":#x\n}");
        // also not a "missing value" (which is only for Arrays)
        _verifyFails(F_TRAILING_COMMA, "{\"a\":}");
    }

    @Test
    void trailingCommaInObject() throws Exception
    {
        _verifyFails(F_DEFAULT, "{\"a\":1,}");
        _verifyFails(F_DEFAULT, "{\"a\":1, }");
        _verifyFails(F_DEFAULT, "[{\"a\":true,}]");
        _verifyFails(F_COMMENTS, "{\"a\":1,/*x*/}");
        _verifyFails(F_YAML_COMMENTS, "{\"a\":1,#x\n}");
    }

    @Test
    void validEmptyAndTrailingComma() throws Exception
    {
        _verifyTokens(F_DEFAULT, "{ }",
                JsonToken.START_OBJECT, JsonToken.END_OBJECT);
        _verifyTokens(F_COMMENTS, "{/*x*/}",
                JsonToken.START_OBJECT, JsonToken.END_OBJECT);
        _verifyTokens(F_YAML_COMMENTS, "{#x\n}",
                JsonToken.START_OBJECT, JsonToken.END_OBJECT);
        _verifyTokens(F_DEFAULT, "{\"a\":{}}",
                JsonToken.START_OBJECT, JsonToken.PROPERTY_NAME,
                JsonToken.START_OBJECT, JsonToken.END_OBJECT, JsonToken.END_OBJECT);
        _verifyTokens(F_TRAILING_COMMA, "{\"a\":1,}",
                JsonToken.START_OBJECT, JsonToken.PROPERTY_NAME,
                JsonToken.VALUE_NUMBER_INT, JsonToken.END_OBJECT);
    }

    private void _verifyFails(JsonFactory f, String doc) throws Exception
    {
        final byte[] data = _jsonDoc(doc);
        for (int bytesPerRead = 1; bytesPerRead <= data.length; ++bytesPerRead) {
            _verifyFails(asyncForBytes(f, bytesPerRead, data, 0), doc, bytesPerRead);
            _verifyFails(asyncForByteBuffer(f, bytesPerRead, data, 1), doc, bytesPerRead);
        }
    }

    private void _verifyFails(AsyncReaderWrapper p, String doc, int bytesPerRead) throws Exception
    {
        try (p) {
            while (p.nextToken() != null) { }
            fail("Should not pass for '"+doc+"' with "+bytesPerRead+" bytes per read");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('}'");
        }
    }

    private void _verifyTokens(JsonFactory f, String doc, JsonToken... expected)
        throws Exception
    {
        final byte[] data = _jsonDoc(doc);
        for (int bytesPerRead = 1; bytesPerRead <= data.length; ++bytesPerRead) {
            _verifyTokens(asyncForBytes(f, bytesPerRead, data, 0), expected);
            _verifyTokens(asyncForByteBuffer(f, bytesPerRead, data, 1), expected);
        }
    }

    private void _verifyTokens(AsyncReaderWrapper p, JsonToken... expected) throws Exception
    {
        try (p) {
            for (JsonToken t : expected) {
                assertToken(t, p.nextToken());
            }
            assertNull(p.nextToken());
        }
    }
}
