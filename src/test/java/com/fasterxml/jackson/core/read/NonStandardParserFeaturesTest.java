package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardParserFeaturesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory STD_F = sharedStreamFactory();

    private final JsonFactory LEADING_ZERO_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .build();

    @SuppressWarnings("deprecation")
    public void testDefaults() {
        assertFalse(STD_F.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
        assertFalse(STD_F.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));
    }

    public void testNonStandardAnyCharQuoting() throws Exception
    {
        _testNonStandardBackslashQuoting(MODE_INPUT_STREAM);
        _testNonStandardBackslashQuoting(MODE_INPUT_STREAM_THROTTLED);
        _testNonStandardBackslashQuoting(MODE_DATA_INPUT);
        _testNonStandardBackslashQuoting(MODE_READER);
        _testNonStandardBackslashQuoting(MODE_READER_THROTTLED);
    }

    public void testLeadingZeroesUTF8() throws Exception {
        _testLeadingZeroes(MODE_INPUT_STREAM, false);
        _testLeadingZeroes(MODE_INPUT_STREAM, true);
        _testLeadingZeroes(MODE_INPUT_STREAM_THROTTLED, false);
        _testLeadingZeroes(MODE_INPUT_STREAM_THROTTLED, true);

        // 17-May-2016, tatu: With DataInput, must have trailing space
        //   since there's no way to detect end of input
        _testLeadingZeroes(MODE_DATA_INPUT, true);
    }

    public void testLeadingZeroesReader() throws Exception {
        _testLeadingZeroes(MODE_READER, false);
        _testLeadingZeroes(MODE_READER, true);
        _testLeadingZeroes(MODE_READER_THROTTLED, false);
        _testLeadingZeroes(MODE_READER_THROTTLED, true);
    }

    // allow NaN
    public void testAllowNaN() throws Exception {
        _testAllowNaN(MODE_INPUT_STREAM);
        _testAllowNaN(MODE_INPUT_STREAM_THROTTLED);
        _testAllowNaN(MODE_DATA_INPUT);
        _testAllowNaN(MODE_READER);
        _testAllowNaN(MODE_READER_THROTTLED);
    }

    // allow +Inf/-Inf
    public void testAllowInfinity() throws Exception {
        _testAllowInf(MODE_INPUT_STREAM);
        _testAllowInf(MODE_INPUT_STREAM_THROTTLED);
        _testAllowInf(MODE_DATA_INPUT);
        _testAllowInf(MODE_READER);
        _testAllowInf(MODE_READER_THROTTLED);
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    private void _testNonStandardBackslashQuoting(int mode) throws Exception
    {
        // first: verify that we get an exception
        final String JSON = q("\\'");
        JsonParser p = createParser(STD_F, mode, JSON);
        try {
            p.nextToken();
            p.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized character escape");
        } finally {
            p.close();
        }
        // and then verify it's ok...
        JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
                .build();
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("'", p.getText());
        p.close();
    }

    private void _testLeadingZeroes(int mode, boolean appendSpace) throws Exception
    {
        // first: verify that we get an exception
        String JSON = "00003";
        if (appendSpace) {
            JSON += " ";
        }
        JsonParser p = createParser(STD_F, mode, JSON);
        try {
            p.nextToken();
            p.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        }
        p.close();

        // but not just as root value
        p = createParser(STD_F, mode, "[ -000 ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        }
        p.close();

        // and then verify it's ok when enabled
        p = createParser(LEADING_ZERO_F, mode, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertEquals("3", p.getText());
        p.close();

        // Plus, also: verify that leading zero magnitude is ok:
        JSON = "0"+Integer.MAX_VALUE;
        if (appendSpace) {
            JSON += " ";
        }
        p = createParser(LEADING_ZERO_F, mode, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(String.valueOf(Integer.MAX_VALUE), p.getText());
        assertEquals(Integer.MAX_VALUE, p.getIntValue());
        Number nr = p.getNumberValue();
        assertSame(Integer.class, nr.getClass());
        p.close();

        // and also check non-root "long zero"
        p = createParser(LEADING_ZERO_F, mode, "[000]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        // 03-Jan-2020, tatu: Actually not 100% sure if we ought to retain invalid
        //   representation or not? Won't, for now:
        assertEquals("0", p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testAllowNaN(int mode) throws Exception
    {
        final String JSON = "[ NaN]";

        // without enabling, should get an exception
        JsonParser p = createParser(STD_F, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "non-standard");
        } finally {
            p.close();
        }

        // we can enable it dynamically (impl detail)
        JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
                .build();
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

        double d = p.getDoubleValue();
        assertTrue(Double.isNaN(d));
        assertEquals("NaN", p.getText());

        // [Issue#98]
        try {
            /*BigDecimal dec =*/ p.getDecimalValue();
            fail("Should fail when trying to access NaN as BigDecimal");
        } catch (NumberFormatException e) {
            verifyException(e, "can not be represented as `java.math.BigDecimal`");
        }

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testAllowInf(int mode) throws Exception
    {
        final String JSON = "[ -INF, +INF, +Infinity, Infinity, -Infinity ]";

        // without enabling, should get an exception
        JsonParser p = createParser(STD_F, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Non-standard token '-INF'");
        } finally {
            p.close();
        }

        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        double d = p.getDoubleValue();
        assertEquals("-INF", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+INF", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("-Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // should also work with skipping
        p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }
}
