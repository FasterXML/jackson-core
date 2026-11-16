package tools.jackson.core.unittest.read;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// [core#1828]: accessing number as `BigDecimal` (or `BigInteger`) must not throw
// raw `NumberFormatException` for exponents outside `BigDecimal` scale range,
// or for NaN/Infinity
class NumberBigDecimalOverflow1828Test extends JacksonCoreTestBase
{
    // Ways to get to failing `BigDecimal` conversion
    enum Access {
        DECIMAL,
        DOUBLE_THEN_DECIMAL,
        NUMBER_EXACT,
        BIG_INTEGER;
    }

    private final JsonFactory F_DEFAULT = newStreamFactory();

    private final JsonFactory F_FAST = JsonFactory.builder()
            .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
            .build();

    private final JsonFactory F_NAN = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

    private final static String[] FAIL_ALWAYS = {
            "35e222222222222222222222222"
    };

    // JDK `BigDecimal` accepts "1e2147483648" (scale == Integer.MIN_VALUE),
    // so only fails with fast parser
    private final static String[] FAIL_FAST = _append(FAIL_ALWAYS, "1e2147483648");

    private final static String[] NON_NUMERIC = { "NaN", "Infinity", "-Infinity" };

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

    @Test
    void nonNumericToBigNumber() throws Exception
    {
        for (String value : NON_NUMERIC) {
            final String doc = "[" + value + "]";
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(F_NAN, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    _verifyNonNumericFail(p, value);
                }
            }
            try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F_NAN, 1, utf8Bytes(doc), 0)) {
                assertToken(JsonToken.START_ARRAY, w.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                _verifyNonNumericFail(w.parser(), value);
            }
        }
    }

    // NaN flag must not leak to following number tokens
    @Test
    void nonNumericThenNumber() throws Exception
    {
        final String doc = "[NaN, 1.25, 3]";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F_NAN, mode, doc)) {
                _verifyNonNumericThenNumber(p, p::nextToken);
            }
        }
        for (int bytesPerRead : new int[] { 1, 99 }) {
            try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(F_NAN, bytesPerRead, utf8Bytes(doc), 0)) {
                _verifyNonNumericThenNumber(w.parser(), w::nextToken);
            }
            try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(F_NAN, bytesPerRead, utf8Bytes(doc), 0)) {
                _verifyNonNumericThenNumber(w.parser(), w::nextToken);
            }
        }
    }

    interface TokenSource {
        JsonToken nextToken() throws Exception;
    }

    private void _verifyNonNumericThenNumber(JsonParser p, TokenSource src) throws Exception
    {
        assertToken(JsonToken.START_ARRAY, src.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertTrue(p.isNaN());
        assertThrows(StreamReadException.class, () -> p.getDecimalValue());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, src.nextToken());
        assertFalse(p.isNaN());
        assertEquals(new BigDecimal("1.25"), p.getDecimalValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, src.nextToken());
        assertFalse(p.isNaN());
        assertEquals(BigInteger.valueOf(3), p.getBigIntegerValue());
        assertToken(JsonToken.END_ARRAY, src.nextToken());
    }

    private void _testDecimalOverflow(JsonFactory f, String[] values) throws Exception
    {
        for (String value : values) {
            final String doc = "[" + value + "]";
            for (Access access : Access.values()) {
                for (int mode : ALL_MODES) {
                    try (JsonParser p = createParser(f, mode, doc)) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                        _verifyOverflowFail(p, value, access);
                    }
                }
            }
        }
    }

    private void _testDecimalOverflowAsync(JsonFactory f, String[] values) throws Exception
    {
        for (String value : values) {
            final byte[] doc = utf8Bytes("[" + value + "]");
            for (Access access : Access.values()) {
                try (AsyncReaderWrapper w = AsyncTestBase.asyncForBytes(f, 1, doc, 0)) {
                    assertToken(JsonToken.START_ARRAY, w.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                    _verifyOverflowFail(w.parser(), value, access);
                }
                try (AsyncReaderWrapper w = AsyncTestBase.asyncForByteBuffer(f, 1, doc, 0)) {
                    assertToken(JsonToken.START_ARRAY, w.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, w.nextToken());
                    _verifyOverflowFail(w.parser(), value, access);
                }
            }
        }
    }

    private void _verifyOverflowFail(JsonParser p, String value, Access access) throws Exception
    {
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> _access(p, access), "Access "+access);
        verifyException(e, "cannot be deserialized as `java.math.BigDecimal`");
        verifyException(e, value);
        assertInstanceOf(NumberFormatException.class, e.getCause());
    }

    private void _verifyNonNumericFail(JsonParser p, String value) throws Exception
    {
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> p.getDecimalValue());
        verifyException(e, "Cannot convert non-numeric value ("+value+") to `java.math.BigDecimal`");
        e = assertThrows(StreamReadException.class,
                () -> p.getBigIntegerValue());
        verifyException(e, "Cannot convert non-numeric value ("+value+") to `java.math.BigInteger`");
    }

    private Object _access(JsonParser p, Access access) throws Exception
    {
        switch (access) {
        case DECIMAL:
            return p.getDecimalValue();
        case DOUBLE_THEN_DECIMAL:
            p.getDoubleValue();
            return p.getDecimalValue();
        case NUMBER_EXACT:
            return p.getNumberValueExact();
        case BIG_INTEGER:
            return p.getBigIntegerValue();
        }
        throw new IllegalStateException();
    }

    private static String[] _append(String[] base, String value) {
        String[] result = Arrays.copyOf(base, base.length + 1);
        result[base.length] = value;
        return result;
    }
}
