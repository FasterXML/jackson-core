package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardNumberParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

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

    public void testLeadingDotInDecimalAllowedAsync() throws Exception {
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_DATA_INPUT);
    }

    public void testLeadingDotInDecimalAllowedBytes() throws Exception {
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_INPUT_STREAM);
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLeadingDotInDecimalAllowedReader() throws Exception {
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_READER);
    }

    private void _testLeadingDotInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        JsonParser p = createParser(f, mode, " .125 ");
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(0.125, p.getValueAsDouble());
        assertEquals("0.125", p.getDecimalValue().toString());
        assertEquals(".125", p.getText());
        p.close();
    }
}
