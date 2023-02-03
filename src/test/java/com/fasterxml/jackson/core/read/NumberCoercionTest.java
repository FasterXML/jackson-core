package com.fasterxml.jackson.core.read;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.InputCoercionException;

public class NumberCoercionTest extends BaseTest
{
    /*
    /**********************************************************
    /* Numeric coercions, integral
    /**********************************************************
     */

    public void testToIntCoercion() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            // long->int
            p = createParser(mode, "1");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1L, p.getLongValue());
            assertEquals(1, p.getIntValue());
            p.close();

            // BigInteger->int
            p = createParser(mode, "10");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.TEN, p.getBigIntegerValue());
            assertEquals(10, p.getIntValue());
            p.close();

            // double->int
            p = createParser(mode, "2");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2.0, p.getDoubleValue());
            assertEquals(2, p.getIntValue());
            p.close();

            // BigDecimal->int
            p = createParser(mode, "10");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigDecimal.TEN, p.getDecimalValue());
            assertEquals(10, p.getIntValue());
            p.close();
        }
    }

    @SuppressWarnings("resource")
    public void testToIntFailing() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            // long -> error
            long big = 1L + Integer.MAX_VALUE;
            p = createParser(mode, String.valueOf(big));
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
            long small = -1L + Integer.MIN_VALUE;
            p = createParser(mode, String.valueOf(small));
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

            // double -> error
            p = createParser(mode, String.valueOf(big)+".0");
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
            p = createParser(mode, String.valueOf(small)+".0");
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

            // BigInteger -> error
            p = createParser(mode, String.valueOf(big));
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
            p = createParser(mode, String.valueOf(small));
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
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            // int->long
            p = createParser(mode, "1");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertEquals(1L, p.getLongValue());
            p.close();

            // BigInteger->long
            long biggish = 12345678901L;
            p = createParser(mode, String.valueOf(biggish));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.valueOf(biggish), p.getBigIntegerValue());
            assertEquals(biggish, p.getLongValue());
            p.close();

            // double->long
            p = createParser(mode, "2");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2.0, p.getDoubleValue());
            assertEquals(2L, p.getLongValue());
            p.close();

            // BigDecimal->long
            p = createParser(mode, "10");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigDecimal.TEN, p.getDecimalValue());
            assertEquals(10, p.getLongValue());
            p.close();
        }
    }

    @SuppressWarnings("resource")
    public void testToLongFailing() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            // BigInteger -> error
            BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN);
            p = createParser(mode, String.valueOf(big));
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
            BigInteger small = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.TEN);
            p = createParser(mode, String.valueOf(small));
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
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            p = createParser(mode, "1");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // int to BigInteger
            assertEquals(1, p.getIntValue());
            assertEquals(BigInteger.ONE, p.getBigIntegerValue());
            p.close();

            p = createParser(mode, "2.0");
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            // double to BigInteger
            assertEquals(2.0, p.getDoubleValue());
            assertEquals(BigInteger.valueOf(2L), p.getBigIntegerValue());
            p.close();

            p = createParser(mode, String.valueOf(Long.MAX_VALUE));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // long to BigInteger
            assertEquals(Long.MAX_VALUE, p.getLongValue());
            assertEquals(BigInteger.valueOf(Long.MAX_VALUE), p.getBigIntegerValue());
            p.close();

            p = createParser(mode, " 200.0");
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            // BigDecimal to BigInteger
            assertEquals(new BigDecimal("200.0"), p.getDecimalValue());
            assertEquals(BigInteger.valueOf(200L), p.getBigIntegerValue());
            p.close();
        }
    }

    /*
    /**********************************************************
    /* Numeric coercions, floating point
    /**********************************************************
     */

    public void testToDoubleCoercion() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            // BigDecimal->double
            p = createParser(mode, "100.5");
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(new BigDecimal("100.5"), p.getDecimalValue());
            assertEquals(100.5, p.getDoubleValue());
            p.close();

            p = createParser(mode, "10");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BigInteger.TEN, p.getBigIntegerValue());
            assertEquals(10.0, p.getDoubleValue());
            p.close();
        }
    }

    public void testToBigDecimalCoercion() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            JsonParser p;

            p = createParser(mode, "1");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // int to BigDecimal
            assertEquals(1, p.getIntValue());
            assertEquals(BigDecimal.ONE, p.getDecimalValue());
            p.close();

            p = createParser(mode, String.valueOf(Long.MAX_VALUE));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // long to BigDecimal
            assertEquals(Long.MAX_VALUE, p.getLongValue());
            assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), p.getDecimalValue());
            p.close();

            BigInteger biggie = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN);
            p = createParser(mode, String.valueOf(biggie));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            // BigInteger to BigDecimal
            assertEquals(biggie, p.getBigIntegerValue());
            assertEquals(new BigDecimal(biggie), p.getDecimalValue());
            p.close();

        }
    }
}
