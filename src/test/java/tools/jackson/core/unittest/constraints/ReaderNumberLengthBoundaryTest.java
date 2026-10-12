package tools.jackson.core.unittest.constraints;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderNumberLengthBoundaryTest
    extends JacksonCoreTestBase
{
    private static final int READER_FALLBACK_MODE = MODE_READER_THROTTLED;

    @Test
    void readerFallbackAcceptsExactDigitBoundary() throws Exception
    {
        assertParsesWithMaxDigits("123.45", 5);
        assertParsesWithMaxDigits("123e45", 5);
        assertParsesWithMaxDigits("12.3e45", 5);
    }

    @Test
    void readerFallbackRejectsFractionOnlyOneDigitOverLimit() throws Exception
    {
        assertTooLongForMaxDigits("123.456", 5);
    }

    @Test
    void readerFallbackRejectsExponentOnlyOneDigitOverLimit() throws Exception
    {
        assertTooLongForMaxDigits("12345e1", 5);
    }

    @Test
    void readerFallbackRejectsFractionAndExponentOneDigitOverLimit() throws Exception
    {
        assertTooLongForMaxDigits("12.34e56", 5);
    }

    @Test
    void readerFallbackRejectsLongFractionAcrossTextBufferSegments() throws Exception
    {
        assertTooLongForMaxDigits("1." + repeatDigit('7', 1000), 1000);
    }

    @Test
    void readerFallbackRejectsLongExponentAcrossTextBufferSegments() throws Exception
    {
        assertTooLongForMaxDigits("1e" + repeatDigit('7', 1000), 1000);
    }

    @Test
    void readerFallbackAcceptsLongExactBoundaryAcrossTextBufferSegments() throws Exception
    {
        assertParsesWithMaxDigits("1." + repeatDigit('7', 999), 1000);
        assertParsesWithMaxDigits("1e" + repeatDigit('7', 999), 1000);
    }

    @Test
    void readerFallbackRejectsLeadingDecimalOneDigitOverLimit() throws Exception
    {
        JsonFactory f = factoryWithMaxDigits(5)
                .rebuild()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        assertParses(f, ".12345");
        assertTooLong(f, ".123456");
    }

    @Test
    void readerFallbackRejectsTrailingDecimalOneDigitOverLimit() throws Exception
    {
        JsonFactory f = factoryWithMaxDigits(4)
                .rebuild()
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        assertParses(f, "1234.");
        assertTooLong(f, "12345.");
    }

    @Test
    void readerFallbackRecordsIntermediateFractionLengthChecks() throws Exception
    {
        RecordingStreamReadConstraints constraints =
                new RecordingStreamReadConstraints(Integer.MAX_VALUE);
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(constraints)
                .build();
        final String value = "1." + repeatDigit('7', 2000);

        assertParses(f, value);

        assertTrue(constraints.fpLengths.size() > 1,
                "Expected intermediate and final floating-point length checks");
        assertTrue(constraints.fpLengths.stream().anyMatch(len -> len < 2001),
                "Expected at least one intermediate check before the final digit count");
        assertEquals(2001, constraints.fpLengths.get(constraints.fpLengths.size() - 1).intValue(),
                "Expected final check to use int + fraction digit length");
    }

    @Test
    void readerFallbackRecordsIntermediateExponentLengthChecks() throws Exception
    {
        RecordingStreamReadConstraints constraints =
                new RecordingStreamReadConstraints(Integer.MAX_VALUE);
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(constraints)
                .build();
        final String value = "1e" + repeatDigit('7', 2000);

        assertParses(f, value);

        assertTrue(constraints.fpLengths.size() > 1,
                "Expected intermediate and final floating-point length checks");
        assertTrue(constraints.fpLengths.stream().anyMatch(len -> len < 2001),
                "Expected at least one intermediate check before the final digit count");
        assertEquals(2001, constraints.fpLengths.get(constraints.fpLengths.size() - 1).intValue(),
                "Expected final check to use int + exponent digit length");
    }

    private void assertParsesWithMaxDigits(String value, int maxNumberLength) throws Exception
    {
        assertParses(factoryWithMaxDigits(maxNumberLength), value);
    }

    private void assertParses(JsonFactory f, String value) throws Exception
    {
        try (JsonParser p = createParser(f, READER_FALLBACK_MODE, value + " ")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(value, p.getString());
            assertNull(p.nextToken());
        }
    }

    private void assertTooLongForMaxDigits(String value, int maxNumberLength) throws Exception
    {
        assertTooLong(factoryWithMaxDigits(maxNumberLength), value);
    }

    private void assertTooLong(JsonFactory f, String value) throws Exception
    {
        try (JsonParser p = createParser(f, READER_FALLBACK_MODE, value + " ")) {
            StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                    p::nextToken, "Input should exceed maxNumberLength: " + value);
            verifyException(e, "Number value length (");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    private JsonFactory factoryWithMaxDigits(int maxNumberLength)
    {
        return JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNumberLength(maxNumberLength)
                        .build())
                .build();
    }

    private static String repeatDigit(char digit, int count)
    {
        char[] chars = new char[count];
        for (int i = 0; i < count; ++i) {
            chars[i] = digit;
        }
        return new String(chars);
    }

    private static final class RecordingStreamReadConstraints
        extends StreamReadConstraints
    {
        final List<Integer> fpLengths = new ArrayList<>();

        RecordingStreamReadConstraints(int maxNumLen)
        {
            super(DEFAULT_MAX_DEPTH, DEFAULT_MAX_DOC_LEN, DEFAULT_MAX_TOKEN_COUNT,
                    maxNumLen, DEFAULT_MAX_STRING_LEN, DEFAULT_MAX_NAME_LEN);
        }

        @Override
        public void validateFPLength(int length) throws StreamConstraintsException
        {
            fpLengths.add(length);
            super.validateFPLength(length);
        }
    }
}
