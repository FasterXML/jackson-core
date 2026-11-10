package tools.jackson.core.unittest.read;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.Named;

import static org.junit.jupiter.api.Assertions.*;

// easier to read on IDE
@TestMethodOrder(MethodName.class)
class NonStandardNumberParsingTest
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    protected JsonFactory jsonFactory() {
        return JSON_F;
    }

    /**
     * The format "0xc0ffee" is not valid JSON, so this should fail
     */
    @Test
    void hexadecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " 0xc0ffee ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    @Test
    void hexadecimalBigX() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " 0XC0FFEE ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    @Test
    void negativeHexadecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0xc0ffee ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('x'");
            }
        }
    }

    /**
     * Test for checking that Overflow for Double value does not lead
     * to bogus NaN information.
     */
    @Test
    void largeDecimal() throws Exception {
        final String biggerThanDouble = "7976931348623157e309";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " " + biggerThanDouble + " ")) {
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(new BigDecimal(biggerThanDouble), p.getDecimalValue());
                assertFalse(p.isNaN());
                assertEquals(Double.POSITIVE_INFINITY, p.getValueAsDouble());
                // 01-Dec-2023, tatu: [core#1137] NaN only from explicit value
                assertFalse(p.isNaN());
            }
        }
    }

    // JSON does not allow numbers to have f or d suffixes
    @Test
    void floatMarker() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123f ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('f'");
            }
        }
    }

    //JSON does not allow numbers to have f or d suffixes
    @Test
    void doubleMarker() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123d ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('d'");
            }
        }
    }

    @Test
    void test2DecimalPoints() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " -0.123.456 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('.'");
            }
        }
    }

    // [core#679]: second decimal point must be caught inside containers too
    @Test
    void test2DecimalPointsInArray() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, "[ 1.5.00 ]")) {
                assertEquals(JsonToken.START_ARRAY, p.nextToken());
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('.'");
                verifyException(e, "more than one decimal point");
            }
        }
    }

    /**
     * The format ".NNN" (as opposed to "0.NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingDotInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " .123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('.'");
            }
        }
    }

    /*
     * The format "+NNN" (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void leadingPlusSignInDecimalDefaultFail() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, " +123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('+' (code 43)) in numeric value: JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
            }
            try (JsonParser p = createParser(mode, " +0.123 ")) {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('+' (code 43)) in numeric value: JSON spec does not allow numbers to have plus signs: enable `JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` to allow");
            }
        }
    }

    /**
     * The format "NNN." (as opposed to "NNN") is not valid JSON, so this should fail
     */
    @Test
    void trailingDotInDecimal() throws Exception {
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, " 123. ");
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "Decimal point not followed by a digit");
            }
            p.close();
        }
    }

    @Test
    public void testLeadingDotInDecimalAllowedDataInput() {
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    public void testLeadingDotInDecimalAllowedBytes() {
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_INPUT_STREAM);
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    public void testLeadingDotInDecimalAllowedReader() {
        _testLeadingDotInDecimalAllowed(JSON_F, MODE_READER);
        _testLeadingDotInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    public void testTrailingDotInDecimalAllowedDataInput() {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    public void testTrailingDotInDecimalAllowedBytes() {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void trailingDotInDecimalAllowedReader() {
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_READER);
        _testTrailingDotInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void leadingPlusSignInDecimalAllowedDataInput() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingPlusSignInDecimalAllowedBytes() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingPlusSignInDecimalAllowedReader() throws Exception {
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_READER);
        _testLeadingPlusSignInDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    // [core#1630]: nextName() value-peeking must also honor
    // ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS (as nextToken() does)
    @Test
    void leadingDotAsFieldValueBytes() throws Exception {
        _testLeadingDotAsFieldValue(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingDotAsFieldValue(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingDotAsFieldValueReader() throws Exception {
        _testLeadingDotAsFieldValue(jsonFactory(), MODE_READER);
        _testLeadingDotAsFieldValue(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void leadingDotAsFieldValueDataInput() throws Exception {
        _testLeadingDotAsFieldValue(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void signedLeadingDotAsFieldValueBytes() throws Exception {
        _testSignedLeadingDotAsFieldValue(jsonFactory(), MODE_INPUT_STREAM);
        _testSignedLeadingDotAsFieldValue(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void signedLeadingDotAsFieldValueReader() throws Exception {
        _testSignedLeadingDotAsFieldValue(jsonFactory(), MODE_READER);
        _testSignedLeadingDotAsFieldValue(jsonFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void signedLeadingDotAsFieldValueDataInput() throws Exception {
        _testSignedLeadingDotAsFieldValue(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingDotAsFieldValueFailsWhenDisabledBytes() throws Exception {
        _testLeadingDotAsFieldValueFails(newStreamFactory(), MODE_INPUT_STREAM);
        _testLeadingDotAsFieldValueFails(newStreamFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingDotAsFieldValueFailsWhenDisabledReader() throws Exception {
        _testLeadingDotAsFieldValueFails(newStreamFactory(), MODE_READER);
        _testLeadingDotAsFieldValueFails(newStreamFactory(), MODE_READER_THROTTLED);
    }

    @Test
    void leadingDotAsFieldValueFailsWhenDisabledDataInput() throws Exception {
        _testLeadingDotAsFieldValueFails(newStreamFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedAsync() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_DATA_INPUT);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedBytes() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM);
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingDotInNegativeDecimalAllowedReader() throws Exception {
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_READER);
        _testLeadingDotInNegativeDecimalAllowed(jsonFactory(), MODE_READER_THROTTLED);
    }

    private void _testLeadingDotInDecimalAllowed(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, " .125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            assertEquals(".125", p.getString());
        }
    }

    private void _testLeadingPlusSignInDecimalAllowed(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, " +125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            // [core#784]: Leading plus sign should be included in textual representation
            assertEquals("+125", p.getString());
        }
        try (JsonParser p = createParser(f, mode, " +0.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            // [core#784]: Leading plus sign should be included in textual representation
            assertEquals("+0.125", p.getString());
        }
        try (JsonParser p = createParser(f, mode, " +.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("0.125", p.getDecimalValue().toString());
            // [core#784]: Leading plus sign should be included in textual representation
            assertEquals("+.125", p.getString());
        }
    }

    private void _testTrailingDotInDecimalAllowed(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, " 125. ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(125.0, p.getValueAsDouble());
            assertEquals("125", p.getDecimalValue().toString());
            assertEquals("125.", p.getString());
        }
    }

    // [core#1630]: leading decimal point in value peeked by nextName()
    private void _testLeadingDotAsFieldValue(JsonFactory f, int mode)
    {
        final String DOC = "{\"a\": .125}";

        // 1) nextName() (no-arg) peeks the value after resolving name
        try (JsonParser p = createParser(f, mode, DOC)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("a", p.nextName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals(".125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        // 2) nextName(SerializableString), matching name (the "Yes" fast path)
        try (JsonParser p = createParser(f, mode, DOC)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertTrue(p.nextName(new SerializedString("a")));
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals(".125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        // 3) nextName(SerializableString), non-matching name (the "Maybe" path)
        try (JsonParser p = createParser(f, mode, DOC)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertFalse(p.nextName(new SerializedString("b")));
            assertEquals("a", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals(".125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        // 4) nextNameMatch(), which peeks the value too
        final PropertyNameMatcher matcher = f.constructNameMatcher(List.of(Named.fromString("a")), false);
        try (JsonParser p = createParser(f, mode, DOC)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals(0, p.nextNameMatch(matcher));
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals(".125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    // [core#1630]: signed variants of leading decimal point, as peeked property value
    private void _testSignedLeadingDotAsFieldValue(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, "{\"a\": -.125}")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("a", p.nextName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-0.125, p.getValueAsDouble());
            assertEquals("-.125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
        try (JsonParser p = createParser(f, mode, "{\"a\": +.125}")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("a", p.nextName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.125, p.getValueAsDouble());
            assertEquals("+.125", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    // [core#1630]: with feature disabled, nextName() must still report the same
    // failure that nextToken() does -- the peek path must not silently allow it
    private void _testLeadingDotAsFieldValueFails(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, "{\"a\": .125}")) {
            p.nextToken();
            p.nextName();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character ('.'");
        }
    }

    private void _testLeadingDotInNegativeDecimalAllowed(JsonFactory f, int mode)
    {
        try (JsonParser p = createParser(f, mode, " -.125 ")) {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-0.125, p.getValueAsDouble());
            assertEquals("-0.125", p.getDecimalValue().toString());
            assertEquals("-.125", p.getString());
        }
    }
}
