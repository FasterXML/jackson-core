package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// [core#1814]: number accessor results must not depend on order of access
class NumberDeferredAccess1814Test extends JUnit5TestBase
{
    private final static String BIG_INT_STR = "123456789012345678901234567890";
    private final static BigInteger BIG_INT = new BigInteger(BIG_INT_STR);

    // Floating-point values exactly representable as float, to avoid
    // differences from float-to-double widening
    private final static String[] VALUES = {
            "1.5", "-2.5e2", "0.25", "7", "-3000000000", BIG_INT_STR
    };

    private final static String[] OPS = {
            "type", "int", "long", "bigInteger", "float", "double", "bigDecimal"
    };

    private final JsonFactory F = newStreamFactory();

    @FunctionalInterface
    interface NumberSource {
        // Parser positioned at the number token
        JsonParser open() throws IOException;
    }

    @Test
    void bigIntegerAfterDouble() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, BIG_INT_STR+" ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                assertEquals(BIG_INT.doubleValue(), p.getDoubleValue());
                assertEquals(BIG_INT, p.getBigIntegerValue());
            }
            try (JsonParser p = createParser(F, mode, BIG_INT_STR+" ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                assertEquals(BIG_INT.floatValue(), p.getFloatValue());
                assertEquals(BIG_INT, p.getBigIntegerValue());
            }
        }
    }

    @Test
    void doubleAfterBigInteger() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "1.5 ")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
                assertEquals(BigInteger.ONE, p.getBigIntegerValue());
                assertEquals(1.5, p.getDoubleValue());
            }
        }
    }

    @Test
    void floatAfterIntConversion() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "1.5 ")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.5, p.getDoubleValue());
                assertEquals(1, p.getIntValue());
                assertEquals(1.5f, p.getFloatValue());
            }
            // conversions from (non-deferred) float
            try (JsonParser p = createParser(F, mode, "1.5 ")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.5f, p.getFloatValue());
                assertEquals(1, p.getIntValue());
                assertEquals(1L, p.getLongValue());
                assertEquals(BigInteger.ONE, p.getBigIntegerValue());
                assertEquals(new BigDecimal("1.5"), p.getDecimalValue());
                assertEquals(1.5, p.getDoubleValue());
            }
        }
    }

    // Every accessor sequence of length 3 must give same results as a fresh parser
    @Test
    void accessOrderIndependence() throws Exception
    {
        for (String value : VALUES) {
            for (int mode : ALL_MODES) {
                _verifyAccessOrder(value, () -> {
                    JsonParser p = createParser(F, mode, value+" ");
                    p.nextToken();
                    return p;
                });
            }
        }
    }

    @Test
    void accessOrderIndependenceAsync() throws Exception
    {
        for (String value : VALUES) {
            final byte[] doc = utf8Bytes("["+value+"]");
            _verifyAccessOrder(value, () -> {
                AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, 1, doc, 0);
                w.nextToken();
                w.nextToken();
                return w.parser();
            });
            _verifyAccessOrder(value, () -> {
                AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F, 1, doc, 0);
                w.nextToken();
                w.nextToken();
                return w.parser();
            });
        }
    }

    private void _verifyAccessOrder(String value, NumberSource src) throws IOException
    {
        Map<String, Object> expected = new HashMap<>();
        for (String op : OPS) {
            try (JsonParser p = src.open()) {
                expected.put(op, _access(p, op));
            }
        }
        for (String op1 : OPS) {
            for (String op2 : OPS) {
                for (String op3 : OPS) {
                    final List<String> seq = Arrays.asList(op1, op2, op3);
                    try (JsonParser p = src.open()) {
                        for (String op : seq) {
                            // type may legitimately change (BigDecimal accessed)
                            if ("type".equals(op)) {
                                p.getNumberType();
                                continue;
                            }
                            assertEquals(expected.get(op), _access(p, op),
                                    "Value '"+value+"', accessors "+seq+": wrong result for '"+op+"'");
                        }
                    }
                }
            }
        }
    }

    private Object _access(JsonParser p, String op) {
        try {
            switch (op) {
            case "type":
                return p.getNumberType();
            case "int":
                return p.getIntValue();
            case "long":
                return p.getLongValue();
            case "bigInteger":
                return p.getBigIntegerValue();
            case "float":
                return p.getFloatValue();
            case "double":
                return p.getDoubleValue();
            case "bigDecimal":
                return p.getDecimalValue();
            }
        } catch (Exception e) {
            return e.getClass();
        }
        throw new IllegalArgumentException(op);
    }
}
