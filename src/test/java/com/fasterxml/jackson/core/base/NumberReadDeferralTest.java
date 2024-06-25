package com.fasterxml.jackson.core.base;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

// Tests to verify [core#1284]: no buffering for specific
// access
public class NumberReadDeferralTest extends JUnit5TestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    private final String NUM_DOC = "[ 0.1 ]";

    @Test
    public void testDoubleWrtBuffering() throws Exception
    {
        try (ParserBase p = (ParserBase) JSON_F.createParser(NUM_DOC)) {
            assertNull(p._numberString);
            assertToken(JsonToken.START_ARRAY, p.nextToken());

            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(NumberType.DOUBLE, p.getNumberType());
//            assertNull(p._numberString);
            assertEquals(0.1, p.getDoubleValue());
            assertNull(p._numberString);
        }
    }

    @Test
    public void testFloatWrtBuffering() throws Exception
    {
        
    }

    @Test
    public void testBigDecimalWrtBuffering() throws Exception
    {
        
    }
}
