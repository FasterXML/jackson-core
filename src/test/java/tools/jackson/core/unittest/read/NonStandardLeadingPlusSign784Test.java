package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce issue #784: Leading Plus Sign Inconsistency
 */
class NonStandardLeadingPlusSign784Test extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    @Test
    void testLeadingPlusSignConsistency() throws Exception {
        // Test various number formats with leading plus sign
        // [core#784]: All should consistently INCLUDE the '+' sign in getText()/getString()
        _testNumber("+125", JsonToken.VALUE_NUMBER_INT);
        _testNumber("+0.125", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+1.25e2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+125.0", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+0", JsonToken.VALUE_NUMBER_INT);
        _testNumber("+1", JsonToken.VALUE_NUMBER_INT);

        // Special case: numbers starting with decimal point (issue #784)
        _testNumber("+.125", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+.5", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+.0", JsonToken.VALUE_NUMBER_FLOAT);

        // With exponents
        _testNumber("+1e2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+1e+2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber("+1e-2", JsonToken.VALUE_NUMBER_FLOAT);
    }

    // With '+' retained in text, values must still be decoded correctly; 19-digit
    // values use a separate decoding path
    @Test
    void testLeadingPlusSignLongValues() throws Exception {
        _testLong("+999999999999999999", 999999999999999999L);
        _testLong("+1000000000000000000", 1000000000000000000L);
        _testLong("+9223372036854775807", Long.MAX_VALUE);
        _testLong("+2147483648", 2147483648L);
        _testLong("+1234567890", 1234567890L);
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, " +9223372036854775808 ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
                assertEquals(new java.math.BigInteger("9223372036854775808"), p.getBigIntegerValue());
            }
        }
    }

    private void _testLong(String numberString, long expected) throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, " " + numberString + " ")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(numberString, p.getString());
                assertEquals(expected, p.getLongValue(),
                        "wrong value for " + numberString + " in mode " + mode);
            }
        }
    }

    private void _testNumber(String numberString, JsonToken expectedToken) throws Exception {
        String input = " " + numberString + " ";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, input)) {
                assertToken(expectedToken, p.nextToken());
                String text = p.getString();
                assertEquals(numberString, text,
                    "getText() returned wrong value for number: " + numberString + " in mode " + mode +
                    " - got: '" + text + "'");
            }
        }
    }
}
