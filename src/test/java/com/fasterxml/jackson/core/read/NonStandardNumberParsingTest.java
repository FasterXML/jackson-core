package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardNumberParsingTest
    extends com.fasterxml.jackson.core.BaseTest
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
     * The format ".NNN" (as opposed to "0.NNN") is not valid JSON, so this should fail
     */
    public void testLeadingDotInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, " .123 ");
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('.'");
            }
            p.close();
        }
    }

    /*
     * The format "+NNN" (as opposed to "NNN") is not valid JSON, so this should fail
     */
    public void testLeadingPlusSignInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, " +123 ");
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "expected digit (0-9) to follow minus sign, for valid numeric value");
            }
            p.close();
        }
    }

    /**
     * The format "NNN." (as opposed to "NNN") is not valid JSON, so this should fail
     */
    public void testTrailingDotInDecimal() throws Exception {
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

    public void testLeadingDotInDecimalAllowedAsync() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    public void testLeadingDotInDecimalAllowedBytes() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLeadingDotInDecimalAllowedReader() throws Exception {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_READER);
    }

    public void testTrailingDotInDecimalAllowedAsync() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    public void testTrailingDotInDecimalAllowedBytes() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    public void testTrailingDotInDecimalAllowedReader() throws Exception {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_READER);
    }

    public void testLeadingPlusSignInDecimalAllowedAsync() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    public void testLeadingPlusSignInDecimalAllowedBytes() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLeadingPlusSignInDecimalAllowedReader() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_READER);
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
        /*
        try (JsonParser p = createParser(f, mode, " +.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            assertEquals(".125", p.getText());
        }
        */
    }

    private void _testTrailingDotInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        try (JsonParser p = createParser(f, mode, " 125. ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            assertEquals("125.", p.getText());
        }
        try (JsonParser p = createParser(f, mode, " 125. ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            assertEquals("125.", p.getText());
        }
    }
}
