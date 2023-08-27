package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.JsonToken;

public class AsyncNumberCoercionTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    /*
    /**********************************************************
    /* Numeric coercions, integral
    /**********************************************************
     */

    public void testToIntCoercion() throws Exception
    {
        AsyncReaderWrapper p;

        // long->int
        p = createParser("1");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1L, p.getLongValue());
        assertEquals(1, p.getIntValue());
        p.close();

        // BigInteger->int
        p = createParser("10");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigInteger.TEN, p.getBigIntegerValue());
        assertEquals(10, p.getIntValue());
        p.close();

        // double->int
        p = createParser("2");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2.0, p.getDoubleValue());
        assertEquals(2, p.getIntValue());
        p.close();

        p = createParser("0.1");
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(0.1, p.getDoubleValue());
        assertEquals(0, p.getIntValue());
        p.close();

        // BigDecimal->int
        p = createParser("10");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigDecimal.TEN, p.getDecimalValue());
        assertEquals(10, p.getIntValue());
        p.close();
    }

    public void testToIntFailing() throws Exception
    {
        // long -> error
        long big = 1L + Integer.MAX_VALUE;
        try (AsyncReaderWrapper p = createParser(String.valueOf(big))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(big, p.getLongValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }

        long small = -1L + Integer.MIN_VALUE;
        try (AsyncReaderWrapper p = createParser(String.valueOf(small))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(Long.valueOf(small), p.getNumberValue());
            assertEquals(small, p.getLongValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }

        // double -> error
        try (AsyncReaderWrapper p = createParser(String.valueOf(big)+".0")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals((double) big, p.getDoubleValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }

        try (AsyncReaderWrapper p = createParser(String.valueOf(small)+".0")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals((double) small, p.getDoubleValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }

        // BigInteger -> error
        try (AsyncReaderWrapper p = createParser(String.valueOf(big))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.valueOf(big), p.getBigIntegerValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }

        try (AsyncReaderWrapper p = createParser(String.valueOf(small))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.valueOf(small), p.getBigIntegerValue());
            try {
                p.getIntValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of int");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Integer.TYPE, e.getTargetType());
            }
        }
    }

    public void testToLongCoercion() throws Exception
    {
        // int->long
        try (AsyncReaderWrapper p = createParser("1")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertEquals(1L, p.getLongValue());
        }

        // BigInteger->long
        long biggish = 12345678901L;
        try (AsyncReaderWrapper p = createParser(String.valueOf(biggish))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.valueOf(biggish), p.getBigIntegerValue());
            assertEquals(biggish, p.getLongValue());
        }

        // double->long
        try (AsyncReaderWrapper p = createParser("2")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2.0, p.getDoubleValue());
            assertEquals(2L, p.getLongValue());
        }

        // BigDecimal->long
        try (AsyncReaderWrapper p = createParser("10")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigDecimal.TEN, p.getDecimalValue());
            assertEquals(10, p.getLongValue());
        }
    }

    public void testToLongFailing() throws Exception
    {
        // BigInteger -> error
        BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN);
        try (AsyncReaderWrapper p = createParser(String.valueOf(big))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(big, p.getBigIntegerValue());
            assertEquals(big, p.getNumberValue());
            try {
                p.getLongValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of long");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Long.TYPE, e.getTargetType());
            }
        }

        BigInteger small = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.TEN);
        try (AsyncReaderWrapper p = createParser(String.valueOf(small))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(small, p.getBigIntegerValue());
            try {
                p.getLongValue();
                fail("Should not pass");
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of long");
                assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
                assertEquals(Long.TYPE, e.getTargetType());
            }
        }
    }

    public void testToBigIntegerCoercion() throws Exception
    {
        try (AsyncReaderWrapper p = createParser("1")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // int to BigInteger
            assertEquals(1, p.getIntValue());
            assertEquals(BigInteger.ONE, p.getBigIntegerValue());
        }

        try (AsyncReaderWrapper p = createParser("2.0")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            // double to BigInteger
            assertEquals(2.0, p.getDoubleValue());
            assertEquals(BigInteger.valueOf(2L), p.getBigIntegerValue());
        }

        try (AsyncReaderWrapper p = createParser(String.valueOf(Long.MAX_VALUE))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // long to BigInteger
            assertEquals(Long.MAX_VALUE, p.getLongValue());
            assertEquals(BigInteger.valueOf(Long.MAX_VALUE), p.getBigIntegerValue());
        }

        try (AsyncReaderWrapper p = createParser(" 200.0")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            // BigDecimal to BigInteger
            assertEquals(new BigDecimal("200.0"), p.getDecimalValue());
            assertEquals(BigInteger.valueOf(200L), p.getBigIntegerValue());
        }
    }

    /*
    /**********************************************************
    /* Numeric coercions, floating point
    /**********************************************************
     */

    public void testToDoubleCoercion() throws Exception
    {
        // BigDecimal->double
        try (AsyncReaderWrapper p = createParser("100.5")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(new BigDecimal("100.5"), p.getDecimalValue());
            assertEquals(100.5, p.getDoubleValue());
        }

        try (AsyncReaderWrapper p = createParser("10")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.TEN, p.getBigIntegerValue());
            assertEquals(10.0, p.getDoubleValue());
        }
    }

    public void testToBigDecimalCoercion() throws Exception
    {
        try (AsyncReaderWrapper p = createParser("1")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // int to BigDecimal
            assertEquals(1, p.getIntValue());
            assertEquals(BigDecimal.ONE, p.getDecimalValue());
        }

        try (AsyncReaderWrapper p = createParser(String.valueOf(Long.MAX_VALUE))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // long to BigDecimal
            assertEquals(Long.MAX_VALUE, p.getLongValue());
            assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), p.getDecimalValue());
        }

        BigInteger biggie = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN);
        try (AsyncReaderWrapper p = createParser(String.valueOf(biggie))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // BigInteger to BigDecimal
            assertEquals(biggie, p.getBigIntegerValue());
            assertEquals(new BigDecimal(biggie), p.getDecimalValue());
        }
    }

    private AsyncReaderWrapper createParser(String doc) throws IOException
    {
        return asyncForBytes(JSON_F, 1, _jsonDoc(doc), 1);
    }
}
