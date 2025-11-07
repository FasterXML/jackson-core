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
