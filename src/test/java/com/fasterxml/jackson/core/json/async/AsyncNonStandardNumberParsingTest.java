package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AsyncNonStandardNumberParsingTest extends AsyncTestBase
{
    private final JsonFactory DEFAULT_F = new JsonFactory();

    @Test
    void hexadecimal() throws Exception
    {
        final String JSON = "[ 0xc0ffee ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('x'");
        } finally {
            p.close();
        }
    }

    @Test
    void hexadecimalBigX() throws Exception
    {
        final String JSON = "[ 0XC0FFEE ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('X'");
        } finally {
            p.close();
        }
    }

    @Test
    void negativeHexadecimal() throws Exception
    {
        final String JSON = "[ -0xc0ffee ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('x'");
        } finally {
            p.close();
        }
    }

    @Test
    void floatMarker() throws Exception
    {
        final String JSON = "[ -0.123f ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('f'");
        } finally {
            p.close();
        }
    }

    @Test
    void doubleMarker() throws Exception
    {
        final String JSON = "[ -0.123d ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('d'");
        } finally {
            p.close();
        }
    }

    @Test
    void test2DecimalPoints() throws Exception {
        final String JSON = "[ -0.123.456 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('.'");
        } finally {
            p.close();
        }
    }

    /**
     * The format ".NNN" (as opposed to "0.NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingDotInDecimal() throws Exception {
        final String JSON = "[ .123 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('.'");
        } finally {
            p.close();
        }
    }

    @Test
    void leadingDotInDecimalEnabled() throws Exception {
        final String JSON = "[ .123 ]";
        final JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();

        AsyncReaderWrapper p = createParser(factory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.123, p.getDoubleValue());
            assertEquals("0.123", p.getDecimalValue().toString());
            assertEquals(".123", p.currentText());
        } finally {
            p.close();
        }
    }

    /*
     * The format "+NNN" (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingPlusSignInDecimalDefaultFail() throws Exception {
        final String JSON = "[ +123 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('+'");
        } finally {
            p.close();
        }
    }

    @Test
    void leadingPlusSignInDecimalDefaultFail2() throws Exception {
        final String JSON = "[ +0.123 ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('+'");
        } finally {
            p.close();
        }
    }

    @Test
    void leadingPlusSignInDecimalEnabled() throws Exception {
        final String JSON = "[ +123 ]";

        JsonFactory jsonFactory =
                JsonFactory.builder().enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS).build();
        AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(123.0, p.getDoubleValue());
            assertEquals("123", p.getDecimalValue().toString());
            assertEquals("+123", p.currentText());
        } finally {
            p.close();
        }
    }

    @Test
    void leadingPlusSignInDecimalEnabled2() throws Exception {
        final String JSON = "[ +0.123 ]";

        JsonFactory jsonFactory =
                JsonFactory.builder().enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS).build();
        AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.123, p.getDoubleValue());
            assertEquals("0.123", p.getDecimalValue().toString());
            assertEquals("0.123", p.currentText());
        } finally {
            p.close();
        }
    }

    @Test
    void leadingPlusSignInDecimalEnabled3() throws Exception {
        final String JSON = "[ +123.123 ]";

        JsonFactory jsonFactory =
                JsonFactory.builder().enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS).build();
        AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(123.123, p.getDoubleValue());
            assertEquals("123.123", p.getDecimalValue().toString());
            assertEquals("+123.123", p.currentText());
        } finally {
            p.close();
        }
    }

    @Test
    void leadingPlusSignNoLeadingZeroDisabled() throws Exception {
        final String JSON = "[ +.123 ]";

        JsonFactory jsonFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .build();
        try (AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                p.nextToken();
                fail("Expected exception");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('.'");
            }
        }
    }

    @Test
    void leadingPlusSignNoLeadingZeroEnabled() throws Exception {
        final String JSON = "[ +.123 ]";

        JsonFactory jsonFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.123, p.getDoubleValue());
            assertEquals("0.123", p.getDecimalValue().toString());
            assertEquals("+0.123", p.currentText());
        } finally {
            p.close();
        }
    }

    @Test
    void leadingDotInNegativeDecimalEnabled() throws Exception {
        final String JSON = "[ -.123 ]";
        final JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();

        AsyncReaderWrapper p = createParser(factory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-0.123, p.getDoubleValue());
            assertEquals("-0.123", p.getDecimalValue().toString());
            assertEquals("-0.123", p.currentText());
        } finally {
            p.close();
        }
    }

    /**
     * The format "NNN." (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void trailingDotInDecimal() throws Exception {
        final String JSON = "[ 123. ]";

        // without enabling, should get an exception
        AsyncReaderWrapper p = createParser(DEFAULT_F, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (StreamReadException e) {
            //the message does not match non-async parsers
            verifyException(e, "Unexpected character (' '");
        } finally {
            p.close();
        }
    }

    @Test
    void trailingDotInDecimalEnabled() throws Exception {
        final String JSON = "[ 123. ]";
        final JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();

        AsyncReaderWrapper p = createParser(factory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            //may want to work on this to get this match JsonToken.VALUE_NUMBER_INT instead
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(123.0, p.getDoubleValue());
            assertEquals("123", p.getDecimalValue().toString());
            assertEquals("123.", p.currentText());
        } finally {
            p.close();
        }
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return asyncForBytes(f, readBytes, _jsonDoc(doc), 1);
    }
}
