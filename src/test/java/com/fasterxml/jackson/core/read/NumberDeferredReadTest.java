package com.fasterxml.jackson.core.read;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;

public class NumberDeferredReadTest
    extends com.fasterxml.jackson.core.BaseTest
{
    protected JsonFactory jsonFactory() {
        return sharedStreamFactory();
    }

    /*
    /**********************************************************************
    /* Tests, integral types
    /**********************************************************************
     */

    // Int, long eagerly decoded, always
    public void testDeferredInt() throws Exception
    {
        _testDeferredInt(MODE_INPUT_STREAM);
        _testDeferredInt(MODE_INPUT_STREAM_THROTTLED);
        _testDeferredInt(MODE_READER);
        _testDeferredInt(MODE_DATA_INPUT);
    }

    private void _testDeferredInt(int mode) throws Exception
    {
        // trailing space to avoid problems with DataInput
        try (JsonParser p = createParser(jsonFactory(), mode, " 12345 ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(Integer.valueOf(12345), p.getNumberValueDeferred());
            assertEquals(NumberType.INT, p.getNumberType());
            assertNull(p.nextToken());
        }
    }

    public void testDeferredLong() throws Exception
    {
        _testDeferredLong(MODE_INPUT_STREAM);
        _testDeferredLong(MODE_INPUT_STREAM_THROTTLED);
        _testDeferredLong(MODE_READER);
        _testDeferredLong(MODE_DATA_INPUT);
    }

    private void _testDeferredLong(int mode) throws Exception
    {
        final long value = 100L + Integer.MAX_VALUE;
        try (JsonParser p = createParser(jsonFactory(), mode, " "+value+" ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(Long.valueOf(value), p.getNumberValueDeferred());
            assertEquals(NumberType.LONG, p.getNumberType());
            assertNull(p.nextToken());
        }
    }

    public void testDeferredBigInteger() throws Exception
    {
        _testDeferredBigInteger(MODE_INPUT_STREAM);
        _testDeferredBigInteger(MODE_INPUT_STREAM_THROTTLED);
        _testDeferredBigInteger(MODE_READER);
        _testDeferredBigInteger(MODE_DATA_INPUT);
    }

    private void _testDeferredBigInteger(int mode) throws Exception
    {
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN);
        try (JsonParser p = createParser(jsonFactory(), mode, " "+value+" ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            Object nr = p.getNumberValueDeferred();
            assertEquals(String.class, nr.getClass());
            assertEquals(value.toString(), nr);

            // But if forced to, we'll get BigInteger
            assertEquals(value, p.getBigIntegerValue());
            assertEquals(value, p.getNumberValueDeferred());
        }
    }

    /*
    /**********************************************************************
    /* Tests, floating point types
    /**********************************************************************
     */

    public void testDeferredFloatingPoint() throws Exception
    {
        _testDeferredFloatingPoint(MODE_INPUT_STREAM);
        _testDeferredFloatingPoint(MODE_INPUT_STREAM_THROTTLED);
        _testDeferredFloatingPoint(MODE_READER);
        _testDeferredFloatingPoint(MODE_DATA_INPUT);
    }

    private void _testDeferredFloatingPoint(int mode) throws Exception
    {
        // Try with BigDecimal/Double/Float; work very similarly
        try (JsonParser p = createParser(jsonFactory(), mode, " 0.25 ")) {
            BigDecimal value = new BigDecimal("0.25");
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

            // NOTE! Important NOT to call "p.getNumberType()" as that'll fix
            // type to Double...
            Object nr = p.getNumberValueDeferred();
            assertEquals(String.class, nr.getClass());
            assertEquals(value.toString(), nr);

            // But if forced to, we'll get BigInteger
            assertEquals(value, p.getDecimalValue());
            assertEquals(value, p.getNumberValueDeferred());
            assertEquals(NumberType.BIG_DECIMAL, p.getNumberType());
        }

        try (JsonParser p = createParser(jsonFactory(), mode, " 0.25 ")) {
            Double value = 0.25d;
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

            Object nr = p.getNumberValueDeferred();
            assertEquals(String.class, nr.getClass());
            assertEquals(value.toString(), nr);

            // But if forced to, we'll get BigInteger
            assertEquals(value, p.getDoubleValue());
            assertEquals(value, p.getNumberValueDeferred());
            assertEquals(NumberType.DOUBLE, p.getNumberType());
        }

        try (JsonParser p = createParser(jsonFactory(), mode, " 0.25 ")) {
            Float value = 0.25f;
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

            Object nr = p.getNumberValueDeferred();
            assertEquals(String.class, nr.getClass());
            assertEquals(value.toString(), nr);

            // But if forced to, we'll get BigInteger
            assertEquals(value, p.getFloatValue());
            assertEquals(value, p.getNumberValueDeferred());
            assertEquals(NumberType.FLOAT, p.getNumberType());
        }
    }
}
