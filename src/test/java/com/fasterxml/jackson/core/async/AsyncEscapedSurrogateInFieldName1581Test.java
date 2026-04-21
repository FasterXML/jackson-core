package com.fasterxml.jackson.core.async;

import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Async parser tests for [jackson-core#1581]: JSON-escaped surrogate pairs
 * (e.g. {@code 👍}) in field names must be decoded correctly by
 * both {@code NonBlockingByteArrayParser} and {@code NonBlockingByteBufferParser}.
 * Both parsers share {@code NonBlockingUtf8JsonParserBase}, so tests are
 * parameterized over both variants to guard against regressions in either path.
 */
class AsyncEscapedSurrogateInFieldName1581Test extends AsyncTestBase
{
    enum Variant {
        BYTE_ARRAY, BYTE_BUFFER;

        AsyncReaderWrapper wrap(JsonFactory f, int bytesPerRead, byte[] data)
            throws IOException
        {
            switch (this) {
            case BYTE_ARRAY: return asyncForBytes(f, bytesPerRead, data, 0);
            case BYTE_BUFFER: return asyncForByteBuffer(f, bytesPerRead, data, 0);
            default: throw new IllegalStateException();
            }
        }
    }

    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // U+1F44D THUMBS UP SIGN = 👍 (UTF-8: F0 9F 91 8D)
    private static final String THUMBS_UP = "👍";

    // U+1D11E MUSICAL SYMBOL G CLEF = 𝄞 (UTF-8: F0 9D 84 9E)
    private static final String G_CLEF = "𝄞";

    // Escaped form of surrogate pairs for use in JSON
    private static final String ESC_THUMBS = "\\ud83d\\udc4d";
    private static final String ESC_GCLEF = "\\ud834\\udd1e";

    /*
    /**********************************************************************
    /* Test methods, success cases with various bytesPerRead
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInFieldName1Byte(Variant v) throws Exception
    {
        _testSurrogateInFieldName(v, 1);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInFieldName2Bytes(Variant v) throws Exception
    {
        _testSurrogateInFieldName(v, 2);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInFieldName3Bytes(Variant v) throws Exception
    {
        _testSurrogateInFieldName(v, 3);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInFieldName7Bytes(Variant v) throws Exception
    {
        _testSurrogateInFieldName(v, 7);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInFieldName100Bytes(Variant v) throws Exception
    {
        _testSurrogateInFieldName(v, 100);
    }

    private void _testSurrogateInFieldName(Variant v, int bytesPerRead) throws Exception
    {
        _testFieldName(v, FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, apostrophe-quoted field names
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInAposFieldName1Byte(Variant v) throws Exception
    {
        _testSurrogateInAposFieldName(v, 1);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInAposFieldName3Bytes(Variant v) throws Exception
    {
        _testSurrogateInAposFieldName(v, 3);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void surrogateInAposFieldName100Bytes(Variant v) throws Exception
    {
        _testSurrogateInAposFieldName(v, 100);
    }

    private void _testSurrogateInAposFieldName(Variant v, int bytesPerRead) throws Exception
    {
        _testAposFieldName(v, bytesPerRead, ESC_THUMBS, THUMBS_UP);
    }

    /*
    /**********************************************************************
    /* Test methods, name variations (quad boundaries, long names)
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Variant.class)
    void nameVariations1Byte(Variant v) throws Exception
    {
        _testNameVariations(v, 1);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void nameVariations3Bytes(Variant v) throws Exception
    {
        _testNameVariations(v, 3);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void nameVariations100Bytes(Variant v) throws Exception
    {
        _testNameVariations(v, 100);
    }

    private void _testNameVariations(Variant v, int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-5 (varying quad offset when escape is hit)
        _testFieldName(v, FACTORY, bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testFieldName(v, FACTORY, bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testFieldName(v, FACTORY, bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testFieldName(v, FACTORY, bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testFieldName(v, FACTORY, bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);
        _testFieldName(v, FACTORY, bytesPerRead, "abcde" + ESC_THUMBS, "abcde" + THUMBS_UP);

        // Suffix after surrogate pair
        _testFieldName(v, FACTORY, bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");

        // Sandwiched: ASCII + surrogate + ASCII
        _testFieldName(v, FACTORY, bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");

        // Two consecutive surrogate pairs
        _testFieldName(v, FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_THUMBS, THUMBS_UP + THUMBS_UP);

        // Two different supplementary characters
        _testFieldName(v, FACTORY, bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Different supplementary character alone
        _testFieldName(v, FACTORY, bytesPerRead, ESC_GCLEF, G_CLEF);

        // Long prefix (>12 bytes)
        _testFieldName(v, FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long prefix + suffix
        _testFieldName(v, FACTORY, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS + "n",
                "abcdefghijklm" + THUMBS_UP + "n");

        // Long name with surrogate in the middle and more ASCII after
        _testFieldName(v, FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ijklmnop",
                "abcdefgh" + THUMBS_UP + "ijklmnop");

        // Long name with multiple different surrogates
        _testFieldName(v, FACTORY, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void nameVariationsApos1Byte(Variant v) throws Exception
    {
        _testNameVariationsApos(v, 1);
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void nameVariationsApos100Bytes(Variant v) throws Exception
    {
        _testNameVariationsApos(v, 100);
    }

    private void _testNameVariationsApos(Variant v, int bytesPerRead) throws Exception
    {
        // Prefix lengths 0-4
        _testAposFieldName(v, bytesPerRead, ESC_THUMBS, THUMBS_UP);
        _testAposFieldName(v, bytesPerRead, "a" + ESC_THUMBS, "a" + THUMBS_UP);
        _testAposFieldName(v, bytesPerRead, "ab" + ESC_THUMBS, "ab" + THUMBS_UP);
        _testAposFieldName(v, bytesPerRead, "abc" + ESC_THUMBS, "abc" + THUMBS_UP);
        _testAposFieldName(v, bytesPerRead, "abcd" + ESC_THUMBS, "abcd" + THUMBS_UP);

        // Suffix, sandwiched, multiple
        _testAposFieldName(v, bytesPerRead, ESC_THUMBS + "z", THUMBS_UP + "z");
        _testAposFieldName(v, bytesPerRead,
                "x" + ESC_THUMBS + "y", "x" + THUMBS_UP + "y");
        _testAposFieldName(v, bytesPerRead,
                ESC_THUMBS + ESC_GCLEF, THUMBS_UP + G_CLEF);

        // Long prefix
        _testAposFieldName(v, bytesPerRead,
                "abcdefghijklm" + ESC_THUMBS,
                "abcdefghijklm" + THUMBS_UP);

        // Long with mixed surrogates
        _testAposFieldName(v, bytesPerRead,
                "abcdefgh" + ESC_THUMBS + "ij" + ESC_GCLEF + "klmn",
                "abcdefgh" + THUMBS_UP + "ij" + G_CLEF + "klmn");
    }

    /*
    /**********************************************************************
    /* Test methods, error cases
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Variant.class)
    void loneHighSurrogateInFieldName(Variant v) throws Exception
    {
        String doc = "{\"\\ud83d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = v.wrap(FACTORY, 1, data)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            r.nextToken();
            fail("Should have thrown for lone high surrogate in field name");
        } catch (StreamReadException e) {
            verifyException(e, "surrogate");
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void loneLowSurrogateInFieldName(Variant v) throws Exception
    {
        String doc = "{\"\\udc4d\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = v.wrap(FACTORY, 1, data)) {
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

    private void _testFieldName(Variant v, JsonFactory f, int bytesPerRead,
                                String escapedName, String expectedName) throws Exception
    {
        String doc = "{\"" + escapedName + "\":\"value\"}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = v.wrap(f, bytesPerRead, data)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.FIELD_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    private void _testAposFieldName(Variant v, int bytesPerRead,
                                    String escapedName, String expectedName) throws Exception
    {
        String doc = "{'" + escapedName + "':'value'}";
        byte[] data = _jsonDoc(doc);
        try (AsyncReaderWrapper r = v.wrap(APOS_FACTORY, bytesPerRead, data)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.FIELD_NAME, r.nextToken());
            assertEquals(expectedName, r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }
}
