package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncNumberDeferredReadTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    /*
    /**********************************************************************
    /* Tests, integral types
    /**********************************************************************
     */

    // Int, long eagerly decoded, always
    public void testDeferredInt() throws Exception
    {
        // trailing space to avoid problems with DataInput
        try (AsyncReaderWrapper p = createParser(" 12345 ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(Integer.valueOf(12345), p.getNumberValueDeferred());
            assertEquals(NumberType.INT, p.getNumberType());
            assertNull(p.nextToken());
        }
    }

    public void testDeferredLong() throws Exception
    {
        final long value = 100L + Integer.MAX_VALUE;
        try (AsyncReaderWrapper p = createParser(" "+value+" ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(Long.valueOf(value), p.getNumberValueDeferred());
            assertEquals(NumberType.LONG, p.getNumberType());
            assertNull(p.nextToken());
        }
    }

    public void testDeferredBigInteger() throws Exception
    {
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN);
        try (AsyncReaderWrapper p = createParser(" "+value+" ")) {
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
        // Try with BigDecimal/Double/Float; work very similarly
        try (AsyncReaderWrapper p = createParser(" 0.25 ")) {
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

        try (AsyncReaderWrapper p = createParser(" 0.25 ")) {
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

        try (AsyncReaderWrapper p = createParser(" 0.25 ")) {
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

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private AsyncReaderWrapper createParser(String doc) throws IOException
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }

}
