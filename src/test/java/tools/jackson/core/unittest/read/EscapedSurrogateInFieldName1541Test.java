package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1541]: JSON-escaped surrogate pairs (e.g. {@code \ud83d\udc4d})
 * in field names should work correctly, same as they do in string values.
 *<p>
 * See also {@code AsyncEscapedSurrogateInFieldName1541Test} for async parser tests.
 */
class EscapedSurrogateInFieldName1541Test extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // U+1F44D THUMBS UP SIGN = \ud83d\udc4d
    private static final String THUMBS_UP = "\uD83D\uDC4D";

    // JSON with escaped surrogate pair in field name: {"\\ud83d\\udc4d":"value"}
    private static final String DOC_FIELD = "{\"\\ud83d\\udc4d\":\"value\"}";

    // Same but with apostrophe-quoted name: {'\\ud83d\\udc4d':'value'}
    private static final String DOC_FIELD_APOS = "{'\\ud83d\\udc4d':'value'}";

    // JSON with escaped surrogate pair in value: {"field":"\\ud83d\\udc4d"}
    private static final String DOC_VALUE = "{\"field\":\"\\ud83d\\udc4d\"}";

    /*
    /**********************************************************************
    /* Test methods, success cases across all parser modes
    /**********************************************************************
     */

    @Test
    void surrogateInFieldNameStream() throws Exception
    {
        _testSurrogateInFieldName(MODE_INPUT_STREAM);
    }

    @Test
    void surrogateInFieldNameStreamThrottled() throws Exception
    {
        _testSurrogateInFieldName(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void surrogateInFieldNameReader() throws Exception
    {
        _testSurrogateInFieldName(MODE_READER);
    }

    @Test
    void surrogateInFieldNameReaderThrottled() throws Exception
    {
        _testSurrogateInFieldName(MODE_READER_THROTTLED);
    }

    @Test
    void surrogateInFieldNameDataInput() throws Exception
    {
        _testSurrogateInFieldName(MODE_DATA_INPUT);
    }

    private void _testSurrogateInFieldName(int mode) throws Exception
    {
        try (JsonParser p = createParser(FACTORY, mode, DOC_FIELD)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(THUMBS_UP, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Test methods, apostrophe-quoted field names
    /**********************************************************************
     */

    @Test
    void surrogateInAposFieldNameStream() throws Exception
    {
        _testSurrogateInAposFieldName(MODE_INPUT_STREAM);
    }

    @Test
    void surrogateInAposFieldNameStreamThrottled() throws Exception
    {
        _testSurrogateInAposFieldName(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void surrogateInAposFieldNameReader() throws Exception
    {
        _testSurrogateInAposFieldName(MODE_READER);
    }

    @Test
    void surrogateInAposFieldNameDataInput() throws Exception
    {
        _testSurrogateInAposFieldName(MODE_DATA_INPUT);
    }

    private void _testSurrogateInAposFieldName(int mode) throws Exception
    {
        try (JsonParser p = createParser(APOS_FACTORY, mode, DOC_FIELD_APOS)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(THUMBS_UP, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    void multipleSurrogatePairsInAposFieldNameStream() throws Exception
    {
        _testMultipleSurrogatePairsApos(MODE_INPUT_STREAM);
    }

    @Test
    void multipleSurrogatePairsInAposFieldNameDataInput() throws Exception
    {
        _testMultipleSurrogatePairsApos(MODE_DATA_INPUT);
    }

    private void _testMultipleSurrogatePairsApos(int mode) throws Exception
    {
        String doc = "{'\\ud83d\\udc4d\\ud83d\\udc4d':'value'}";
        String expectedName = THUMBS_UP + THUMBS_UP;
        try (JsonParser p = createParser(APOS_FACTORY, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(expectedName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Test methods, string value sanity check
    /**********************************************************************
     */

    @Test
    void surrogateInStringValueStream() throws Exception
    {
        _testSurrogateInStringValue(MODE_INPUT_STREAM);
    }

    @Test
    void surrogateInStringValueDataInput() throws Exception
    {
        _testSurrogateInStringValue(MODE_DATA_INPUT);
    }

    private void _testSurrogateInStringValue(int mode) throws Exception
    {
        try (JsonParser p = createParser(FACTORY, mode, DOC_VALUE)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("field", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(THUMBS_UP, p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Test methods, error cases
    /**********************************************************************
     */

    @Test
    void loneHighSurrogateInFieldNameStream() throws Exception
    {
        _testLoneHighSurrogate(MODE_INPUT_STREAM);
    }

    @Test
    void loneHighSurrogateInFieldNameDataInput() throws Exception
    {
        _testLoneHighSurrogate(MODE_DATA_INPUT);
    }

    private void _testLoneHighSurrogate(int mode) throws Exception
    {
        // Lone high surrogate followed by closing quote: {"\\ud83d":"value"}
        String doc = "{\"\\ud83d\":\"value\"}";
        try (JsonParser p = createParser(FACTORY, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            p.nextToken();
            fail("Should have thrown for lone high surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }

    @Test
    void loneLowSurrogateInFieldNameStream() throws Exception
    {
        _testLoneLowSurrogate(MODE_INPUT_STREAM);
    }

    @Test
    void loneLowSurrogateInFieldNameDataInput() throws Exception
    {
        _testLoneLowSurrogate(MODE_DATA_INPUT);
    }

    private void _testLoneLowSurrogate(int mode) throws Exception
    {
        // Lone low surrogate: {"\\udc4d":"value"}
        String doc = "{\"\\udc4d\":\"value\"}";
        try (JsonParser p = createParser(FACTORY, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            p.nextToken();
            fail("Should have thrown for lone low surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, multiple surrogate pairs
    /**********************************************************************
     */

    @Test
    void multipleSurrogatePairsInFieldNameStream() throws Exception
    {
        _testMultipleSurrogatePairs(MODE_INPUT_STREAM);
    }

    @Test
    void multipleSurrogatePairsInFieldNameDataInput() throws Exception
    {
        _testMultipleSurrogatePairs(MODE_DATA_INPUT);
    }

    @Test
    void multipleSurrogatePairsInFieldNameReader() throws Exception
    {
        _testMultipleSurrogatePairs(MODE_READER);
    }

    private void _testMultipleSurrogatePairs(int mode) throws Exception
    {
        // Two thumbs up: {"\\ud83d\\udc4d\\ud83d\\udc4d":"value"}
        String doc = "{\"\\ud83d\\udc4d\\ud83d\\udc4d\":\"value\"}";
        String expectedName = THUMBS_UP + THUMBS_UP;
        try (JsonParser p = createParser(FACTORY, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(expectedName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
