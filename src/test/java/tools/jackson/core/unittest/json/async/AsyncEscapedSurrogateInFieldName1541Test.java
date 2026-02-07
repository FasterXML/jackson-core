package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Async parser tests for [jackson-core#1541]: JSON-escaped surrogate pairs
 * (e.g. {@code \ud83d\udc4d}) in field names.
 */
class AsyncEscapedSurrogateInFieldName1541Test extends AsyncTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    // U+1F44D THUMBS UP SIGN = \ud83d\udc4d
    private static final String THUMBS_UP = "\uD83D\uDC4D";

    // JSON with escaped surrogate pair in field name: {"\\ud83d\\udc4d":"value"}
    private static final String DOC_FIELD = "{\"\\ud83d\\udc4d\":\"value\"}";

    /*
    /**********************************************************************
    /* Test methods, success cases with various bytesPerRead
    /**********************************************************************
     */

    @Test
    void surrogateInFieldNameAsync1Byte() throws Exception
    {
        _testSurrogateInFieldNameAsync(1);
    }

    @Test
    void surrogateInFieldNameAsync2Bytes() throws Exception
    {
        _testSurrogateInFieldNameAsync(2);
    }

    @Test
    void surrogateInFieldNameAsync3Bytes() throws Exception
    {
        _testSurrogateInFieldNameAsync(3);
    }

    @Test
    void surrogateInFieldNameAsync7Bytes() throws Exception
    {
        _testSurrogateInFieldNameAsync(7);
    }

    @Test
    void surrogateInFieldNameAsync100Bytes() throws Exception
    {
        _testSurrogateInFieldNameAsync(100);
    }

    private void _testSurrogateInFieldNameAsync(int bytesPerRead) throws Exception
    {
        byte[] data = _jsonDoc(DOC_FIELD);
        try (AsyncReaderWrapper r = asyncForBytes(FACTORY, bytesPerRead, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(THUMBS_UP, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Test methods, multiple surrogate pairs
    /**********************************************************************
     */

    @Test
    void multipleSurrogatePairsAsync1Byte() throws Exception
    {
        String doc = "{\"\\ud83d\\udc4d\\ud83d\\udc4d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        String expectedName = THUMBS_UP + THUMBS_UP;
        try (AsyncReaderWrapper r = asyncForBytes(FACTORY, 1, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Test methods, error cases
    /**********************************************************************
     */

    @Test
    void loneHighSurrogateInFieldNameAsync() throws Exception
    {
        String doc = "{\"\\ud83d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForBytes(FACTORY, 1, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            r.nextToken();
            fail("Should have thrown for lone high surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }

    @Test
    void loneLowSurrogateInFieldNameAsync() throws Exception
    {
        String doc = "{\"\\udc4d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForBytes(FACTORY, 1, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            r.nextToken();
            fail("Should have thrown for lone low surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }
}
