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
 * ByteBuffer async parser tests for [jackson-core#1581]: JSON-escaped surrogate pairs
 * (e.g. {@code \ud83d\udc4d}) in field names should work for NonBlockingByteBufferParser,
 * same as they do for NonBlockingByteArrayParser (tested in
 * {@link AsyncEscapedSurrogateInFieldName1541Test}).
 */
class AsyncByteBufferEscapedSurrogateInFieldName1581Test extends AsyncTestBase
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
    void surrogateInFieldNameByteBuffer1Byte() throws Exception
    {
        _testSurrogateInFieldNameByteBuffer(1);
    }

    @Test
    void surrogateInFieldNameByteBuffer2Bytes() throws Exception
    {
        _testSurrogateInFieldNameByteBuffer(2);
    }

    @Test
    void surrogateInFieldNameByteBuffer3Bytes() throws Exception
    {
        _testSurrogateInFieldNameByteBuffer(3);
    }

    @Test
    void surrogateInFieldNameByteBuffer7Bytes() throws Exception
    {
        _testSurrogateInFieldNameByteBuffer(7);
    }

    @Test
    void surrogateInFieldNameByteBuffer100Bytes() throws Exception
    {
        _testSurrogateInFieldNameByteBuffer(100);
    }

    private void _testSurrogateInFieldNameByteBuffer(int bytesPerRead) throws Exception
    {
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, apostrophe-quoted field names
    /**********************************************************************
     */

    @Test
    void surrogateInAposFieldNameByteBuffer1Byte() throws Exception
    {
        _testSurrogateInAposFieldNameByteBuffer(1);
    }

    @Test
    void surrogateInAposFieldNameByteBuffer3Bytes() throws Exception
    {
        _testSurrogateInAposFieldNameByteBuffer(3);
    }

    @Test
    void surrogateInAposFieldNameByteBuffer100Bytes() throws Exception
    {
        _testSurrogateInAposFieldNameByteBuffer(100);
    }

    private void _testSurrogateInAposFieldNameByteBuffer(int bytesPerRead) throws Exception
    {
        _testAposFieldNameByteBuffer(bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, name variations (quad boundaries, long names)
    /**********************************************************************
     */

    @Test
    void nameVariationsByteBuffer1Byte() throws Exception
    {
        _testNameVariationsByteBuffer(1);
    }

    @Test
    void nameVariationsByteBuffer3Bytes() throws Exception
    {
        _testNameVariationsByteBuffer(3);
    }

    @Test
    void nameVariationsByteBuffer100Bytes() throws Exception
    {
        _testNameVariationsByteBuffer(100);
    }

    private void _testNameVariationsByteBuffer(int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-5 (varying quad offset when escape is hit)
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, "abcde" + ESC_THUMBS, "abcde" + THUMBS_UP);

        // Suffix after surrogate pair
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");

        // Sandwiched: ASCII + surrogate + ASCII
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");

        // Two consecutive surrogate pairs
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_THUMBS, THUMBS_UP + THUMBS_UP);

        // Two different supplementary characters
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Different supplementary character alone
        _testFieldNameByteBuffer(FACTORY, bytesPerRead, ESC_GCLEF, G_CLEF);

        // Long prefix (>12 bytes)
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long prefix + suffix
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS + "n",
                "abcdefghijklm" + THUMBS_UP + "n");

        // Long name with surrogate in the middle and more ASCII after
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ijklmnop",
                "abcdefgh" + THUMBS_UP + "ijklmnop");

        // Long name with multiple different surrogates
        _testFieldNameByteBuffer(FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    @Test
    void nameVariationsAposByteBuffer1Byte() throws Exception
    {
        _testNameVariationsAposByteBuffer(1);
    }

    @Test
    void nameVariationsAposByteBuffer100Bytes() throws Exception
    {
        _testNameVariationsAposByteBuffer(100);
    }

    private void _testNameVariationsAposByteBuffer(int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-4
        _testAposFieldNameByteBuffer(bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testAposFieldNameByteBuffer(bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testAposFieldNameByteBuffer(bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testAposFieldNameByteBuffer(bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testAposFieldNameByteBuffer(bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);

        // Suffix, sandwiched, multiple
        _testAposFieldNameByteBuffer(bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");
        _testAposFieldNameByteBuffer(bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");
        _testAposFieldNameByteBuffer(bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Long prefix
        _testAposFieldNameByteBuffer(bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long with mixed surrogates
        _testAposFieldNameByteBuffer(bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    /*
    /**********************************************************************
    /* Test methods, error cases
    /**********************************************************************
     */

    @Test
    void loneHighSurrogateInFieldNameByteBuffer() throws Exception
    {
        String doc = "{\"\\ud83d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForByteBuffer(FACTORY, 1, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            r.nextToken();
            fail("Should have thrown for lone high surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }

    @Test
    void loneLowSurrogateInFieldNameByteBuffer() throws Exception
    {
        String doc = "{\"\\udc4d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForByteBuffer(FACTORY, 1, data, 0)) {
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

    private void _testFieldNameByteBuffer(JsonFactory f, int bytesPerRead,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{\"" + escapedName + "\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForByteBuffer(f, bytesPerRead, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    private void _testAposFieldNameByteBuffer(int bytesPerRead,
            String escapedName, String expectedName) throws Exception
    {
        String doc = "{'" + escapedName + "':'value'}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = asyncForByteBuffer(APOS_FACTORY, bytesPerRead, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }
}
