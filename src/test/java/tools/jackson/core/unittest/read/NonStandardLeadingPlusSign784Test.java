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
        _testNumber(" +125 ", "+125", JsonToken.VALUE_NUMBER_INT);
        _testNumber(" +0.125 ", "+0.125", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +1.25e2 ", "+1.25e2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +125.0 ", "+125.0", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +0 ", "+0", JsonToken.VALUE_NUMBER_INT);
        _testNumber(" +1 ", "+1", JsonToken.VALUE_NUMBER_INT);

        // Special case: numbers starting with decimal point (issue #784)
        _testNumber(" +.125 ", "+.125", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +.5 ", "+.5", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +.0 ", "+.0", JsonToken.VALUE_NUMBER_FLOAT);

        // With exponents
        _testNumber(" +1e2 ", "+1e2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +1e+2 ", "+1e+2", JsonToken.VALUE_NUMBER_FLOAT);
        _testNumber(" +1e-2 ", "+1e-2", JsonToken.VALUE_NUMBER_FLOAT);
    }

    private void _testNumber(String input, String expectedText, JsonToken expectedToken) throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, input)) {
                assertEquals(expectedToken, p.nextToken(),
                    "Wrong token for input: " + input + " in mode " + mode);
                String text = p.getText();
                assertEquals(expectedText, text,
                    "getText() returned wrong value for input: " + input + " in mode " + mode +
                    " - got: '" + text + "'");

                // Also verify getString() is consistent
                String str = p.getString();
                assertEquals(expectedText, str,
                    "getString() returned wrong value for input: " + input + " in mode " + mode +
                    " - got: '" + str + "'");
            }
        }
    }
}
