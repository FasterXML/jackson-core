package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that a number must have at least one digit, even with both
 * leading and trailing decimal point allowed.
 */
class NumberDotWithoutDigitsTest extends JacksonCoreTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    @Test
    void dotWithoutDigitsInArray() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String value : new String[] { ".", ".e5", "-.", "+.", "-.e5" }) {
                final String json = "[1," + value + "]";
                try (JsonParser p = createParser(F, mode, json)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    p.nextToken();
                    fail("Should not pass for: " + json + " (mode " + mode + ")");
                } catch (StreamReadException e) {
                    verifyException(e, "Decimal point not followed by a digit");
                }
            }
        }
    }

    @Test
    void dotWithoutDigitsAfterColon() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "{\"a\":.}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                p.nextToken();
                fail("Should not pass (mode " + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "Decimal point not followed by a digit");
            }
        }
    }

    @Test
    void dotWithoutDigitsAtRoot() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, ". ")) {
                p.nextToken();
                fail("Should not pass (mode " + mode + ")");
            } catch (StreamReadException e) {
                verifyException(e, "Decimal point not followed by a digit");
            }
        }
    }

    // Single digit on either side of decimal point still fine
    @Test
    void singleDigitAllowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(F, mode, "[1.,.5,-.5,-1.,1.e2]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.0, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(-0.5, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(-1.0, p.getDoubleValue());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(100.0, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }
}
