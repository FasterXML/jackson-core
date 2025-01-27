package com.fasterxml.jackson.core.read;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TokenStreamFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberParsingDb4917Test extends JUnit5TestBase
{
    private TokenStreamFactory JSON_F = newStreamFactory();

    final String INPUT_JSON = a2q("{'decimalHolder':100.00,'number':50}");

    // [jackson-databind#4917]
    @Test
    public void bigDecimal4917Integers() throws Exception
    {
        for (int mode : ALL_MODES) {
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.BIG_INTEGER);
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.INT);
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.LONG);
        }
    }

    public void bigDecimal4917Floats() throws Exception
    {
        for (int mode : ALL_MODES) {
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, false, JsonParser.NumberType.DOUBLE);
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.DOUBLE);
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.FLOAT);
            testBigDecimal4917(JSON_F, mode, INPUT_JSON, true, JsonParser.NumberType.BIG_DECIMAL);
        }
    }
    
    private void testBigDecimal4917(final TokenStreamFactory jsonF,
            final int mode,
            final String json,
            final boolean checkFirstNumValues,
            final JsonParser.NumberType secondNumTypeCheck) throws Exception
    {
        // checkFirstNumValues=false reproduces the issue in https://github.com/FasterXML/jackson-databind/issues/4917
        // it is useful to check the second number value while requesting different number types
        // but the call adjusts state of the parser, so it is better to redo the test and then test w
        try (JsonParser p = createParser(jsonF, mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("decimalHolder", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());
            if (checkFirstNumValues) {
                assertEquals(Double.valueOf(100.0), p.getNumberValueDeferred());
                assertEquals(new BigDecimal("100.00"), p.getDecimalValue());
            }
            assertEquals("100.00", p.getText());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("number", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals(Integer.valueOf(50), p.getNumberValueDeferred());
            if (secondNumTypeCheck == JsonParser.NumberType.BIG_DECIMAL) {
                assertEquals(new BigDecimal("50"), p.getDecimalValue());
            } else if (secondNumTypeCheck == JsonParser.NumberType.BIG_INTEGER) {
                assertEquals(new BigInteger("50"), p.getBigIntegerValue());
            } else if (secondNumTypeCheck == JsonParser.NumberType.FLOAT) {
                assertEquals(50.0f, p.getFloatValue());
            } else if (secondNumTypeCheck == JsonParser.NumberType.LONG) {
                assertEquals(50L, p.getLongValue());
            } else if (secondNumTypeCheck == JsonParser.NumberType.INT) {
                assertEquals(50, p.getIntValue());
            } else {
                assertEquals(50.0d, p.getDoubleValue());
            }
            assertEquals(50, p.getIntValue());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
