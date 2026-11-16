package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// [core#1828]: `getDecimalValue()` must not throw raw `NumberFormatException`
// for exponents outside `BigDecimal` scale range
class NumberBigDecimalOverflow1828Test extends JUnit5TestBase
{
    private final JsonFactory F_DEFAULT = newStreamFactory();

    private final JsonFactory F_FAST = JsonFactory.builder()
            .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
            .build();

    // JDK `BigDecimal` accepts "1e2147483648" (scale == Integer.MIN_VALUE),
    // so only use it with fast parser
    private final static String[] FAIL_ALWAYS = {
            "35e222222222222222222222222"
    };

    private final static String[] FAIL_FAST = {
            "35e222222222222222222222222",
            "1e2147483648"
    };

    @Test
    void decimalOverflowDefault() throws Exception
    {
        _testDecimalOverflow(F_DEFAULT, FAIL_ALWAYS);
    }

    @Test
    void decimalOverflowFast() throws Exception
    {
        _testDecimalOverflow(F_FAST, FAIL_FAST);
    }

    @Test
    void decimalOverflowAsync() throws Exception
    {
        _testDecimalOverflowAsync(F_DEFAULT, FAIL_ALWAYS);
        _testDecimalOverflowAsync(F_FAST, FAIL_FAST);
    }

    private void _testDecimalOverflow(JsonFactory f, String[] values) throws Exception
    {
        for (String value : values) {
            final String doc = "[" + value + "]";
            for (boolean doubleFirst : new boolean[] { false, true }) {
                for (int mode : ALL_MODES) {
                    try (JsonParser p = createParser(f, mode, doc)) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                        _verifyFail(p, value, doubleFirst);
                    }
                }
            }
        }
    }

    private void _testDecimalOverflowAsync(JsonFactory f, String[] values) throws Exception
    {
        for (String value : values) {
            final byte[] doc = utf8Bytes("[" + value + "]");
            for (boolean doubleFirst : new boolean[] { false, true }) {
                try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(f, 1, doc, 0)) {
                    assertToken(JsonToken.START_ARRAY, w.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                    _verifyFail(w.parser(), value, doubleFirst);
                }
                try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(f, 1, doc, 0)) {
                    assertToken(JsonToken.START_ARRAY, w.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                    _verifyFail(w.parser(), value, doubleFirst);
                }
            }
        }
    }

    private void _verifyFail(JsonParser p, String value, boolean doubleFirst) throws Exception
    {
        if (doubleFirst) {
            p.getDoubleValue();
        }
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> p.getDecimalValue());
        verifyException(e, "Malformed numeric value");
        verifyException(e, value);
        assertInstanceOf(NumberFormatException.class, e.getCause());
    }
}
