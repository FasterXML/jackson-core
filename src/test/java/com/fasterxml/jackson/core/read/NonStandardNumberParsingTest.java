package com.fasterxml.jackson.core.read;

import java.math.BigDecimal;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.*;

// easier to read on IDE
@TestMethodOrder(MethodName.class)
class NonStandardNumberParsingTest
        extends JUnit5TestBase
{
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    protected JsonFactory jsonFactory() {
        return JSON_F;
    }

    /**
     * The format "0xc0ffee" is not valid JSON, so this should fail
     */
    @Test
    void hexadecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " 0xc0ffee ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    @Test
    void hexadecimalBigX() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " 0XC0FFEE ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    @Test
    void negativeHexadecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0xc0ffee ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    /**
     * Test for checking that Overflow for Double value does not lead
     * to bogus NaN information.
     */
    @Test
    void largeDecimal() throws Exception {
        final String biggerThanDouble = "7976931348623157e309";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " " + biggerThanDouble + " ")) {
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(new BigDecimal(biggerThanDouble), p.getDecimalValue());
                assertFalse(p.isNaN());
                assertEquals(Double.POSITIVE_INFINITY, p.getValueAsDouble());
                // 01-Dec-2023, tatu: [core#1137] NaN only from explicit value
                assertFalse(p.isNaN());
            }
        }
    }

    // JSON does not allow numbers to have f or d suffixes
    @Test
    void floatMarker() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123f ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('f'");
            }
        }
    }

    //JSON does not allow numbers to have f or d suffixes
    @Test
    void doubleMarker() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123d ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('d'");
            }
        }
    }

    @Test
    void test2DecimalPoints() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123.456 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('.'");
            }
        }
    }

    /**
     * The format ".NNN" (as opposed to "0.NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingDotInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " .123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('.'");
            }
        }
    }

    /*
     * The format "+NNN" (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingPlusSignInDecimalDefaultFail() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " +123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('+' (code 43)) in numeric value: JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
            }
            try (JsonParser p = createParser(mode, " +0.123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('+' (code 43)) in numeric value: JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
            }
        }
    }

    /**
     * The format "NNN." (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void trailingDotInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, " 123. ");
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Decimal point not followed by a digit");
            }
            p.close();
        }
    }

    @Test
    void leadingDotInDecimalAllowedDataInput() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingDotInDecimalAllowedBytes() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingDotInDecimalAllowedReader() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_READER);
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void trailingDotInDecimalAllowedDataInput() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void trailingDotInDecimalAllowedBytes() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void trailingDotInDecimalAllowedReader() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_READER);
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void leadingPlusSignInDecimalAllowedDataInput() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingPlusSignInDecimalAllowedBytes() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingPlusSignInDecimalAllowedReader() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_READER);
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedAsync() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedBytes() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedReader() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_READER);
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    private void _testLeadingDotInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        try (JsonParser p = createParser(f, mode, " .125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            assertEquals(".125", p.getText());
        }
    }

    private void _testLeadingPlusSignInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        try (JsonParser p = createParser(f, mode, " +125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            assertEquals("125", p.getText());
        }
        try (JsonParser p = createParser(f, mode, " +0.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            assertEquals("0.125", p.getText());
        }
        try (JsonParser p = createParser(f, mode, " +.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            assertEquals(".125", p.getText());
        }
    }

    private void _testTrailingDotInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        try (JsonParser p = createParser(f, mode, " 125. ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            assertEquals("125.", p.getText());
        }
    }

    private void _testLeadingDotInNegativeDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        try (JsonParser p = createParser(f, mode, " -.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-0.125, p.getValueAsDouble());
            assertEquals("-0.125", p.getDecimalValue().toString());
            assertEquals("-.125", p.getText());
        }
    }
}
