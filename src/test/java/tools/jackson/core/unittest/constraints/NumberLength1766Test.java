package tools.jackson.core.unittest.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [core#1766]: `ReaderBasedJsonParser` undercounted number length when
// fraction or exponent was absent (throttled Reader forces fallback path)
class NumberLength1766Test extends JacksonCoreTestBase
{
    @Test
    void exactLimitAccepted() throws Exception
    {
        JsonFactory f = factory(5);
        for (String doc : new String[] { "123.45", "123e45", "12.3e45", "-123.45" }) {
            assertParses(f, doc);
        }
        f = factory(1000);
        assertParses(f, "1." + "7".repeat(999));
        assertParses(f, "1e" + "7".repeat(999));
    }

    @Test
    void oneOverLimitRejected() throws Exception
    {
        JsonFactory f = factory(5);
        for (String doc : new String[] { "123.456", "12345e1", "12.34e56", "-12345e1" }) {
            assertTooLong(f, doc);
        }
        // long enough to span TextBuffer segments
        f = factory(1000);
        assertTooLong(f, "1." + "7".repeat(1000));
        assertTooLong(f, "1e" + "7".repeat(1000));
    }

    @Test
    void nonStandardDecimalPoint() throws Exception
    {
        JsonFactory f = factory(5).rebuild()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        assertParses(f, ".12345");
        assertTooLong(f, ".123456");
        assertParses(f, "12345.");
        assertTooLong(f, "123456.");
    }

    private void assertParses(JsonFactory f, String doc) throws Exception
    {
        try (JsonParser p = createParser(f, MODE_READER_THROTTLED, doc + " ")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(doc, p.getString());
            assertNull(p.nextToken());
        }
    }

    private void assertTooLong(JsonFactory f, String doc) throws Exception
    {
        try (JsonParser p = createParser(f, MODE_READER_THROTTLED, doc + " ")) {
            StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                    p::nextToken, "Should exceed maxNumberLength: " + doc);
            verifyException(e, "Number value length (");
        }
    }

    private JsonFactory factory(int maxNumberLength) {
        return JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNumberLength(maxNumberLength).build())
                .build();
    }
}
