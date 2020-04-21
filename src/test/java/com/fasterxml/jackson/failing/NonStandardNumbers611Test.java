package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardNumbers611Test
    extends com.fasterxml.jackson.core.BaseTest
{
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

    public void testLeadingDotInDecimalAllowed() throws Exception {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();

// TODO:
/*
        for (int mode : ALL_MODES) {
            _testLeadingDotInDecimalAllowed(f, mode);
        }
        */


        _testLeadingDotInDecimalAllowed(f, MODE_INPUT_STREAM);
        _testLeadingDotInDecimalAllowed(f, MODE_INPUT_STREAM_THROTTLED);
        _testLeadingDotInDecimalAllowed(f, MODE_READER);
        _testLeadingDotInDecimalAllowed(f, MODE_DATA_INPUT);
    }

    private void _testLeadingDotInDecimalAllowed(JsonFactory f, int mode) throws Exception
    {
        JsonParser p = createParser(f, mode, " .123 ");
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(0.123, p.getValueAsDouble());
        assertEquals("0.123", p.getDecimalValue().toString());
        p.close();
    }
}
