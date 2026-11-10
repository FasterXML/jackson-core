package tools.jackson.core.unittest.read;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [core#707]: JSON5-style hexadecimal integer literals enabled via
 * {@link JsonReadFeature#ALLOW_HEXADECIMAL_NUMBERS}.
 */
class NonStandardHexNumbers707Test extends JacksonCoreTestBase
{
    private final JsonFactory HEX_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
            .build();

    private final JsonFactory HEX_AND_PLUS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    @Test
    void unsignedHexLowercase() throws Exception {
        _expectInt(HEX_F, "0xc0ffee", "0xc0ffee", 0xC0FFEE);
    }

    @Test
    void unsignedHexUppercaseXAndDigits() throws Exception {
        _expectInt(HEX_F, "0XC0FFEE", "0XC0FFEE", 0xC0FFEE);
    }

    @Test
    void unsignedHexZero() throws Exception {
        _expectInt(HEX_F, "0x0", "0x0", 0);
    }

    @Test
    void unsignedHexWithLeadingZeros() throws Exception {
        // [core#707]: leading zeros are always permitted in hex digits, regardless
        // of ALLOW_LEADING_ZEROS_FOR_NUMBERS.
        _expectInt(HEX_F, "0x007F", "0x007F", 0x7F);
    }

    @Test
    void negativeHex() throws Exception {
        _expectInt(HEX_F, "-0x10", "-0x10", -16);
    }

    @Test
    void plusSignHexRequiresLeadingPlusFeature() throws Exception {
        // Without ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS, +0xff must still fail
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(HEX_F, mode, " +0xff ")) {
                p.nextToken();
                fail("Should not pass when ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS is disabled");
            } catch (StreamReadException e) {
                verifyException(e, "plus sign");
            }
        }
    }

    @Test
    void plusSignHexWithPlusFeature() throws Exception {
        _expectInt(HEX_AND_PLUS_F, "+0xff", "+0xff", 0xFF);
    }

    @Test
    void hexLongRange() throws Exception {
        // Just over Integer.MAX_VALUE (0x7fffffff = 2147483647) -> 0x80000000 = 2147483648L
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(HEX_F, mode, " 0x80000000 ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals("0x80000000", p.getString());
                assertEquals(2147483648L, p.getLongValue());
            }
        }
    }

    @Test
    void hexBigIntegerRange() throws Exception {
        // 17 hex digits -> must promote to BigInteger
        final String literal = "0x1ffffffffffffffff";
        final BigInteger expected = new BigInteger("1ffffffffffffffff", 16);
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(HEX_F, mode, " " + literal + " ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(literal, p.getString());
                assertEquals(expected, p.getBigIntegerValue());
            }
        }
    }

    @Test
    void hexInsideArray() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(HEX_F, mode, "[0x1, 0x2, -0xA]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(1, p.getIntValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(2, p.getIntValue());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(-10, p.getIntValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    @Test
    void hexRejectedWhenFeatureDisabled() throws Exception {
        // With the feature OFF, 0x... must still fail like in vanilla JSON,
        // and the error message must point at the feature to enable.
        JsonFactory plainF = new JsonFactory();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(plainF, mode, " 0xc0ffee ")) {
                p.nextToken();
                fail("Should not pass when ALLOW_HEXADECIMAL_NUMBERS is disabled");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('x'");
                verifyException(e, "ALLOW_HEXADECIMAL_NUMBERS");
            }
        }
    }

    @Test
    void hexPrefixWithoutDigitsFails() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(HEX_F, mode, " 0x ")) {
                p.nextToken();
                fail("Should not pass: prefix without any hex digit");
            } catch (StreamReadException e) {
                verifyException(e, "hex digit");
            }
        }
    }

    @Test
    void hexNumberLengthConstraint() throws Exception {
        // Build a hex literal long enough to cross several TextBuffer segment
        // boundaries (default first segment is 500 chars; using 8000 digits to
        // ensure we definitely span at least one boundary on every backend).
        StringBuilder sb = new StringBuilder("0x");
        for (int i = 0; i < 8000; i++) {
            sb.append('f');
        }
        final String hugeHex = sb.toString();
        JsonFactory cappedF = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS)
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(100).build())
                .build();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(cappedF, mode, " " + hugeHex + " ")) {
                p.nextToken();
                fail("Should not pass: hex literal exceeds maxNumberLength (mode " + mode + ")");
            } catch (StreamConstraintsException e) {
                verifyException(e, "exceeds the maximum");
            }
        }
    }

    private void _expectInt(JsonFactory factory, String literal, String expectedText, long expectedValue)
            throws Exception
    {
        String input = " " + literal + " ";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(factory, mode, input)) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(expectedText, p.getString(),
                        "getString() literal mismatch for " + literal + " (mode " + mode + ")");
                assertEquals(expectedValue, p.getLongValue(),
                        "long value mismatch for " + literal + " (mode " + mode + ")");
                if (expectedValue >= Integer.MIN_VALUE && expectedValue <= Integer.MAX_VALUE) {
                    assertEquals((int) expectedValue, p.getIntValue(),
                            "int value mismatch for " + literal + " (mode " + mode + ")");
                }
            }
        }
    }
}
