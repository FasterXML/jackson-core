package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// [core#1815]: cached number values from previous token must not leak
class NumberStaleValue1815Test extends JUnit5TestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

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

    @Test
    void staleNumberStringBeforeNaN() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "[1.5, NaN]")) {
                _testStaleNumberStringBeforeNaN(p, p::nextToken);
            }
        }
    }

    @Test
    void staleNumberStringBeforeNaNAsync() throws Exception
    {
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F, 1, utf8Bytes("[1.5, NaN]"), 0)) {
            _testStaleNumberStringBeforeNaN(w.parser(), w::nextToken);
        }
        try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F, 1, utf8Bytes("[1.5, NaN]"), 0)) {
            _testStaleNumberStringBeforeNaN(w.parser(), w::nextToken);
        }
    }

    private void _testStaleBigDecimal(JsonParser p, TokenSource src) throws IOException
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

    private void _testStaleNumberStringBeforeNaN(JsonParser p, TokenSource src) throws IOException
    {
        assertToken(JsonToken.START_ARRAY, src.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertEquals(NumberType.DOUBLE, p.getNumberType());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertTrue(Double.isNaN(p.getDoubleValue()));
        assertToken(JsonToken.END_ARRAY, src.nextToken());
    }

    @FunctionalInterface
    interface TokenSource {
        JsonToken nextToken() throws IOException;
    }
}
