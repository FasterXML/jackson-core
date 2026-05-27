package tools.jackson.core.unittest.json.async;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [core#707]: JSON5-style hexadecimal integer literals via the
 * non-blocking (async) UTF-8 parser. Exercises the {@code 1}-byte and
 * {@code 3}-byte feed sizes to drive suspension at every position.
 */
class AsyncHexNumbers707Test extends AsyncTestBase
{
    private final JsonFactory HEX_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
            .build();

    private final JsonFactory HEX_AND_PLUS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    @Test
    void unsignedHexFullBuffer() throws Exception {
        _expectInt(HEX_F, "0xc0ffee", "0xc0ffee", 0xC0FFEE, 1000);
    }

    @Test
    void unsignedHexOneBytePerRead() throws Exception {
        // Forces suspension after every single byte
        _expectInt(HEX_F, "0xc0ffee", "0xc0ffee", 0xC0FFEE, 1);
    }

    @Test
    void uppercaseHexOneBytePerRead() throws Exception {
        _expectInt(HEX_F, "0XCAFE", "0XCAFE", 0xCAFE, 1);
    }

    @Test
    void negativeHexOneBytePerRead() throws Exception {
        _expectInt(HEX_F, "-0x10", "-0x10", -16, 1);
    }

    @Test
    void positiveHexWithPlusSignAndSplit() throws Exception {
        _expectInt(HEX_AND_PLUS_F, "+0xff", "+0xff", 0xFF, 1);
    }

    @Test
    void plainZeroStillWorks() throws Exception {
        // Make sure we didn't break the bare "0" path
        try (AsyncReaderWrapper r = asyncForBytes(HEX_F, 1, _jsonDoc(" 0 "), 1)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
            assertEquals(0, r.getIntValue());
        }
    }

    @Test
    void hexInsideArray() throws Exception {
        for (int readSize : new int[] {1, 3, 1000}) {
            try (AsyncReaderWrapper r = asyncForBytes(HEX_F, readSize,
                    _jsonDoc("[0x1, 0xFF, -0x10]"), 1)) {
                assertToken(JsonToken.START_ARRAY, r.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(1, r.getIntValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(255, r.getIntValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(-16, r.getIntValue());
                assertToken(JsonToken.END_ARRAY, r.nextToken());
            }
        }
    }

    @Test
    void hexBigIntegerRange() throws Exception {
        final String literal = "0x1ffffffffffffffff";
        final BigInteger expected = new BigInteger("1ffffffffffffffff", 16);
        for (int readSize : new int[] {1, 5, 1000}) {
            try (AsyncReaderWrapper r = asyncForBytes(HEX_F, readSize,
                    _jsonDoc(" " + literal + " "), 1)) {
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(literal, r.currentText());
                assertEquals(expected, r.getBigIntegerValue());
            }
        }
    }

    @Test
    void hexNegativeBigIntegerRange() throws Exception {
        // 17 hex digits with sign -> must promote to (negative) BigInteger via async resumption
        final String literal = "-0x1ffffffffffffffff";
        final BigInteger expected = new BigInteger("-1ffffffffffffffff", 16);
        for (int readSize : new int[] {1, 5, 1000}) {
            try (AsyncReaderWrapper r = asyncForBytes(HEX_F, readSize,
                    _jsonDoc(" " + literal + " "), 1)) {
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(literal, r.currentText());
                assertEquals(expected, r.getBigIntegerValue());
            }
        }
    }

    @Test
    void hex16DigitsLongMax() throws Exception {
        // 16 digits, top nibble == 7 -> stays on the long fast path (Long.MAX_VALUE).
        // Boundary case in _parseHexInt: hexLen == 16 && topNibble < 0x8.
        final String literal = "0x7fffffffffffffff";
        for (int readSize : new int[] {1, 5, 1000}) {
            try (AsyncReaderWrapper r = asyncForBytes(HEX_F, readSize,
                    _jsonDoc(" " + literal + " "), 1)) {
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(literal, r.currentText());
                assertEquals(Long.MAX_VALUE, r.getLongValue());
            }
        }
    }

    @Test
    void hex16DigitsOverflowsToBigInteger() throws Exception {
        // 16 digits, top nibble == 8 -> falls off the long fast path into the
        // BigInteger arm of _parseHexInt (value is 2^63, just past Long.MAX_VALUE).
        final String literal = "0x8000000000000000";
        final BigInteger expected = BigInteger.ONE.shiftLeft(63);
        for (int readSize : new int[] {1, 5, 1000}) {
            try (AsyncReaderWrapper r = asyncForBytes(HEX_F, readSize,
                    _jsonDoc(" " + literal + " "), 1)) {
                assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
                assertEquals(literal, r.currentText());
                assertEquals(expected, r.getBigIntegerValue());
            }
        }
    }

    @Test
    void hexRejectedWhenFeatureDisabled() throws Exception {
        JsonFactory plain = new JsonFactory();
        try (AsyncReaderWrapper r = asyncForBytes(plain, 1, _jsonDoc(" 0xff "), 1)) {
            r.nextToken();
            fail("Should not pass when ALLOW_HEXADECIMAL_NUMBERS is disabled");
        } catch (StreamReadException e) {
            // Error now names the feature that must be enabled (see _checkHexNumbersAllowed).
            verifyException(e, "Unexpected character ('x'");
            verifyException(e, "ALLOW_HEXADECIMAL_NUMBERS");
        }
    }

    @Test
    void hexPrefixWithoutDigitsFails() throws Exception {
        try (AsyncReaderWrapper r = asyncForBytes(HEX_F, 1, _jsonDoc(" 0x "), 1)) {
            r.nextToken();
            fail("Should not pass: prefix without any hex digit");
        } catch (StreamReadException e) {
            verifyException(e, "hex digit");
        }
    }

    private void _expectInt(JsonFactory factory, String literal, String expectedText,
            long expectedValue, int readSize) throws Exception
    {
        String input = " " + literal + " ";
        try (AsyncReaderWrapper r = asyncForBytes(factory, readSize, _jsonDoc(input), 1)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
            assertEquals(expectedText, r.currentText(),
                    "currentText() literal mismatch for " + literal + " (readSize " + readSize + ")");
            assertEquals(expectedValue, r.getLongValue(),
                    "long value mismatch for " + literal + " (readSize " + readSize + ")");
            if (expectedValue >= Integer.MIN_VALUE && expectedValue <= Integer.MAX_VALUE) {
                assertEquals((int) expectedValue, r.getIntValue(),
                        "int value mismatch for " + literal + " (readSize " + readSize + ")");
            }
        }
    }
}
