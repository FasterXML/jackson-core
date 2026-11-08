package tools.jackson.core.unittest.read;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// [core#1815]: cached number values from previous token must not leak
class NumberStaleValue1815Test extends JacksonCoreTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

    private final static BigInteger BIG_INT = new BigInteger("12345678901234567890123");
    private final static String NAN_DOC = "[1.5, NaN, 2.5, 7, 3.5, 0]";
    private final static String BIG_INT_DOC = "[1.5, "+BIG_INT+", "+BIG_INT+", "+BIG_INT+"]";

    @Test
    void staleBigDecimal() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "[2.5, 7.5]")) {
                _testStaleBigDecimal(p, p::nextToken);
            }
        }
    }

    @Test
    void staleBigDecimalAsync() throws Exception
    {
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, 1, utf8Bytes("[2.5, 7.5]"), 0)) {
            _testStaleBigDecimal(w.parser(), w::nextToken);
        }
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F, 1, utf8Bytes("[2.5, 7.5]"), 0)) {
            _testStaleBigDecimal(w.parser(), w::nextToken);
        }
    }

    // Root-level values: last number completed at end-of-input
    @Test
    void staleValuesAtEOFAsync() throws Exception
    {
        for (int bytesPerRead : new int[] { 1, 99 }) {
            try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, bytesPerRead, utf8Bytes("2.5 7"), 0)) {
                JsonParser p = w.parser();
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
                assertToken(JsonToken.VALUE_NUMBER_INT, w.nextToken());
                assertEquals(7.0, p.getDoubleValue());
                assertEquals(7.0, p.getDoubleValue());
                assertNull(w.nextToken());
            }
            try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, bytesPerRead, utf8Bytes("1.5 NaN"), 0)) {
                JsonParser p = w.parser();
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                assertTrue(Double.isNaN(p.getDoubleValue()));
                assertNull(w.nextToken());
            }
        }
    }

    @Test
    void staleNumberStringBeforeNaN() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, NAN_DOC)) {
                _testStaleNumberStringBeforeNaN(p, p::nextToken);
            }
        }
    }

    @Test
    void staleNumberStringBeforeNaNAsync() throws Exception
    {
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, 1, utf8Bytes(NAN_DOC), 0)) {
            _testStaleNumberStringBeforeNaN(w.parser(), w::nextToken);
        }
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F, 1, utf8Bytes(NAN_DOC), 0)) {
            _testStaleNumberStringBeforeNaN(w.parser(), w::nextToken);
        }
    }

    @Test
    void deferredBigInteger() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, BIG_INT_DOC)) {
                _testDeferredBigInteger(p, p::nextToken);
            }
        }
    }

    @Test
    void deferredBigIntegerAsync() throws Exception
    {
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, 1, utf8Bytes(BIG_INT_DOC), 0)) {
            _testDeferredBigInteger(w.parser(), w::nextToken);
        }
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F, 1, utf8Bytes(BIG_INT_DOC), 0)) {
            _testDeferredBigInteger(w.parser(), w::nextToken);
        }
    }

    private void _testStaleBigDecimal(JsonParser p, TokenSource src)
    {
        assertToken(JsonToken.START_ARRAY, src.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(new BigDecimal("2.5"), p.getDecimalValue());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertEquals(BigInteger.valueOf(7), p.getBigIntegerValue());
        assertEquals(new BigDecimal("7.5"), p.getDecimalValue());
        assertEquals(7.5, p.getDoubleValue());
        assertToken(JsonToken.END_ARRAY, src.nextToken());
    }

    private void _testStaleNumberStringBeforeNaN(JsonParser p, TokenSource src)
    {
        assertToken(JsonToken.START_ARRAY, src.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertTrue(Double.isNaN(p.getDoubleValue()));
        // and same for regular ints
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertEquals(7.0, p.getDoubleValue());
        assertEquals(7.0, p.getDoubleValue());
        assertEquals(7.0f, p.getFloatValue());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertEquals(0.0, p.getDoubleValue());
        assertEquals(0.0, p.getDoubleValue());
        assertToken(JsonToken.END_ARRAY, src.nextToken());
    }

    private void _testDeferredBigInteger(JsonParser p, TokenSource src)
    {
        final double expD = BIG_INT.doubleValue();
        final float expF = BIG_INT.floatValue();

        assertToken(JsonToken.START_ARRAY, src.nextToken());
        // BigInteger from float value of previous token must not leak
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertEquals(BigInteger.ONE, p.getBigIntegerValue());
        assertEquals(1.5, p.getDoubleValue());

        // double/float accessed after type check (deferred BigInteger)
        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
        assertEquals(expD, p.getDoubleValue());
        assertEquals(expF, p.getFloatValue());
        assertEquals(expD, p.getDoubleValue());
        assertEquals(BIG_INT, p.getBigIntegerValue());
        assertEquals(expD, p.getDoubleValue());
        assertEquals(new BigDecimal(BIG_INT), p.getDecimalValue());

        // double/float accessed before anything else
        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertEquals(expD, p.getDoubleValue());
        assertEquals(BIG_INT, p.getBigIntegerValue());
        assertEquals(BIG_INT, p.getNumberValue());

        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertEquals(expF, p.getFloatValue());
        assertEquals(new BigDecimal(BIG_INT), p.getDecimalValue());
        assertEquals(BIG_INT, p.getBigIntegerValue());

        assertToken(JsonToken.END_ARRAY, src.nextToken());
    }

    @FunctionalInterface
    interface TokenSource {
        JsonToken nextToken();
    }
}
