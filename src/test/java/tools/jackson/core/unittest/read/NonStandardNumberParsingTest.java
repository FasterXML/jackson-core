package tools.jackson.core.unittest.read;

import java.math.BigDecimal;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.*;

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

    private final JsonFactory COMMENTS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
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

    // [core#1557]: number values inside containers (non-root) must be properly
    // terminated/separated too, not only at root level (cf. [core#105]); without
    // this such content would only fail lazily when accessing the following token.
    // NOTE: async (non-blocking) parser still handled lazily; see follow-up.
    @Test
    void nonRootMangledIntegers1557() throws Exception {
        for (int mode : ALL_MODES) {
            _verifyMangledNonRootNumber(mode, "[ 123true ]");
            _verifyMangledNonRootNumber(mode, "[ 100k ]");
            _verifyMangledNonRootNumber(mode, "[ 100/ ]");
        }
    }

    @Test
    void nonRootMangledFloats1557() throws Exception {
        for (int mode : ALL_MODES) {
            _verifyMangledNonRootNumber(mode, "[ 1.5false ]");
            _verifyMangledNonRootNumber(mode, "[ 1.5x ]");
        }
    }

    // [core#1557]: a number immediately followed by a comment (no separating
    // white space) must still parse when comments are enabled; this exercises the
    // '/' and '#' branches of the separator check that the plain cases never reach.
    @Test
    void nonRootNumberFollowedByComment1557() throws Exception {
        for (int mode : ALL_MODES) {
            _verifyTwoIntsAcrossComment(mode, "[1/* c */,2]"); // Java block comment
            _verifyTwoIntsAcrossComment(mode, "[1// c\n,2]");  // Java line comment
            _verifyTwoIntsAcrossComment(mode, "[1#c\n,2]");    // YAML/shell comment
        }
    }

    private void _verifyTwoIntsAcrossComment(int mode, String doc) throws Exception {
        try (JsonParser p = createParser(COMMENTS_F, mode, doc)) {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2, p.getIntValue());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    private void _verifyMangledNonRootNumber(int mode, String doc) throws Exception {
        try (JsonParser p = createParser(mode, doc)) {
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            // Should fail eagerly when decoding the number token (and, at the
            // latest, when forcing numeric value access); must NOT silently pass.
            JsonToken t = p.nextToken();
            p.getDoubleValue();
            fail("Should have failed for '"+doc+"', instead got token "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
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
