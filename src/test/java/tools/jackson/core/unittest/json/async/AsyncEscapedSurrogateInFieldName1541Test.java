package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
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
        _testFieldNameAsync(FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, apostrophe-quoted field names
    /**********************************************************************
     */

    @Test
    void surrogateInAposFieldNameAsync1Byte() throws Exception
    {
        _testSurrogateInAposFieldNameAsync(1);
    }

    @Test
    void surrogateInAposFieldNameAsync3Bytes() throws Exception
    {
        _testSurrogateInAposFieldNameAsync(3);
    }

    @Test
    void surrogateInAposFieldNameAsync100Bytes() throws Exception
    {
        _testSurrogateInAposFieldNameAsync(100);
    }

    private void _testSurrogateInAposFieldNameAsync(int bytesPerRead) throws Exception
    {
        _testAposFieldNameAsync(bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, name variations (quad boundaries, long names)
    /**********************************************************************
     */

    // 1-byte-at-a-time exercises the slow (split escape) path;
    // 100-byte reads exercise the fast (inline) path

    @Test
    void nameVariationsAsync1Byte() throws Exception
    {
        _testNameVariationsAsync(1);
    }

    @Test
    void nameVariationsAsync3Bytes() throws Exception
    {
        _testNameVariationsAsync(3);
    }

    @Test
    void nameVariationsAsync100Bytes() throws Exception
    {
        _testNameVariationsAsync(100);
    }

    private void _testNameVariationsAsync(int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-5 (varying quad offset when escape is hit)
        _testFieldNameAsync(FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testFieldNameAsync(FACTORY, bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testFieldNameAsync(FACTORY, bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testFieldNameAsync(FACTORY, bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testFieldNameAsync(FACTORY, bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);
        _testFieldNameAsync(FACTORY, bytesPerRead, "abcde" + ESC_THUMBS, "abcde" + THUMBS_UP);

        // Suffix after surrogate pair
        _testFieldNameAsync(FACTORY, bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");

        // Sandwiched: ASCII + surrogate + ASCII
        _testFieldNameAsync(FACTORY, bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");

        // Two consecutive surrogate pairs
        _testFieldNameAsync(FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_THUMBS, THUMBS_UP + THUMBS_UP);

        // Two different supplementary characters
        _testFieldNameAsync(FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Different supplementary character alone
        _testFieldNameAsync(FACTORY, bytesPerRead, ESC_GCLEF, G_CLEF);

        // Long prefix (>12 bytes)
        _testFieldNameAsync(FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long prefix + suffix
        _testFieldNameAsync(FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS + "n",
                "abcdefghijklm" + THUMBS_UP + "n");

        // Long name with surrogate in the middle and more ASCII after
        _testFieldNameAsync(FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ijklmnop",
                "abcdefgh" + THUMBS_UP + "ijklmnop");

        // Long name with multiple different surrogates
        _testFieldNameAsync(FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    @Test
    void nameVariationsAposAsync1Byte() throws Exception
    {
        _testNameVariationsAposAsync(1);
    }

    @Test
    void nameVariationsAposAsync100Bytes() throws Exception
    {
        _testNameVariationsAposAsync(100);
    }

    private void _testNameVariationsAposAsync(int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-4
        _testAposFieldNameAsync(bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testAposFieldNameAsync(bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testAposFieldNameAsync(bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testAposFieldNameAsync(bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testAposFieldNameAsync(bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);

        // Suffix, sandwiched, multiple
        _testAposFieldNameAsync(bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");
        _testAposFieldNameAsync(bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");
        _testAposFieldNameAsync(bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Long prefix
        _testAposFieldNameAsync(bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long with mixed surrogates
        _testAposFieldNameAsync(bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
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

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testFieldNameAsync(JsonFactory f, int bytesPerRead,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{\"" + escapedName + "\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForBytes(f, bytesPerRead, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    private void _testAposFieldNameAsync(int bytesPerRead,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{'" + escapedName + "':'value'}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForBytes(APOS_FACTORY, bytesPerRead, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }
}
