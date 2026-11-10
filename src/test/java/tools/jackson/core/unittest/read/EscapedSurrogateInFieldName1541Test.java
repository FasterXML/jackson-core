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
class EscapedSurrogateInFieldName1541Test
    extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // U+1F44D THUMBS UP SIGN = \ud83d\udc4d (UTF-8: F0 9F 91 8D)
    private static final String THUMBS_UP = "\uD83D\uDC4D";

    // U+1D11E MUSICAL SYMBOL G CLEF = \ud834\udd1e (UTF-8: F0 9D 84 9E)
    private static final String G_CLEF = "\uD834\uDD1E";

    // Escaped form of surrogate pairs for use in JSON
    private static final String ESC_THUMBS = "\\ud83d\\udc4d";
    private static final String ESC_GCLEF = "\\ud834\\udd1e";

    // JSON with escaped surrogate pair in field name: {"\\ud83d\\udc4d":"value"}
    private static final String DOC_FIELD = "{\"" + ESC_THUMBS + "\":\"value\"}";

    // Same but with apostrophe-quoted name: {'\\ud83d\\udc4d':'value'}
    private static final String DOC_FIELD_APOS = "{'" + ESC_THUMBS + "':'value'}";

    // JSON with escaped surrogate pair in value: {"field":"\\ud83d\\udc4d"}
    private static final String DOC_VALUE = "{\"field\":\"" + ESC_THUMBS + "\"}";

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
    /* Test methods, name variations (quad boundary alignment, long names)
    /**********************************************************************
     */

    // Different ASCII prefix lengths exercise different quad boundary
    // alignments for the 4-byte UTF-8 supplementary character:
    //   0 bytes -> quad offset 0
    //   1 byte  -> quad offset 1, straddles boundary
    //   2 bytes -> quad offset 2, straddles boundary
    //   3 bytes -> quad offset 3, straddles boundary
    //   4 bytes -> full quad flushed, offset 0 again

    @Test
    void nameVariationsStream() throws Exception
    {
        _testNameVariations(MODE_INPUT_STREAM);
    }

    @Test
    void nameVariationsStreamThrottled() throws Exception
    {
        _testNameVariations(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void nameVariationsReader() throws Exception
    {
        _testNameVariations(MODE_READER);
    }

    @Test
    void nameVariationsDataInput() throws Exception
    {
        _testNameVariations(MODE_DATA_INPUT);
    }

    private void _testNameVariations(int mode) throws Exception
    {
        // Prefix lengths 0-5 (varying quad offset when escape is hit)
        _testFieldName(FACTORY, mode, ESC_THUMBS, THUMBS_UP);
        _testFieldName(FACTORY, mode, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testFieldName(FACTORY, mode, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testFieldName(FACTORY, mode, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testFieldName(FACTORY, mode, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);
        _testFieldName(FACTORY, mode, "abcde" + ESC_THUMBS, "abcde" + THUMBS_UP);

        // Suffix after surrogate pair
        _testFieldName(FACTORY, mode, ESC_THUMBS + "z", THUMBS_UP + "z");

        // Sandwiched: ASCII + surrogate + ASCII
        _testFieldName(FACTORY, mode, "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");

        // Two consecutive surrogate pairs
        _testFieldName(FACTORY, mode,
                ESC_THUMBS + ESC_THUMBS,
                THUMBS_UP + THUMBS_UP);

        // Two different supplementary characters
        _testFieldName(FACTORY, mode,
                ESC_THUMBS + ESC_GCLEF,
                THUMBS_UP + G_CLEF);

        // Different supplementary character alone
        _testFieldName(FACTORY, mode, ESC_GCLEF, G_CLEF);

        // Long prefix (>12 bytes to exercise parseLongName path)
        _testFieldName(FACTORY, mode,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long prefix + suffix
        _testFieldName(FACTORY, mode,
                "abcdefghijklm" + ESC_THUMBS + "n",
                "abcdefghijklm" + THUMBS_UP + "n");

        // Long name with surrogate in the middle and more ASCII after
        _testFieldName(FACTORY, mode,
                "abcdefgh" + ESC_THUMBS + "ijklmnop",
                "abcdefgh" + THUMBS_UP + "ijklmnop");

        // Long name with multiple different surrogates
        _testFieldName(FACTORY, mode,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    @Test
    void nameVariationsAposStream() throws Exception
    {
        _testNameVariationsApos(MODE_INPUT_STREAM);
    }

    @Test
    void nameVariationsAposDataInput() throws Exception
    {
        _testNameVariationsApos(MODE_DATA_INPUT);
    }

    private void _testNameVariationsApos(int mode) throws Exception
    {
        // Prefix lengths 0-4
        _testAposFieldName(mode, ESC_THUMBS, THUMBS_UP);
        _testAposFieldName(mode, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testAposFieldName(mode, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testAposFieldName(mode, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testAposFieldName(mode, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);

        // Suffix, sandwiched, multiple
        _testAposFieldName(mode, ESC_THUMBS + "z", THUMBS_UP + "z");
        _testAposFieldName(mode, "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");
        _testAposFieldName(mode, ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Long prefix
        _testAposFieldName(mode,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long with mixed surrogates
        _testAposFieldName(mode,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testFieldName(JsonFactory f, int mode,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{\"" + escapedName + "\":\"value\"}";
        try (JsonParser p = createParser(f, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(expectedName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private void _testAposFieldName(int mode,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{'" + escapedName + "':'value'}";
        try (JsonParser p = createParser(APOS_FACTORY, mode, doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(expectedName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
