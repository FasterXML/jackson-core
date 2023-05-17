package tools.jackson.core.read;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;

/**
 * Set of additional unit for verifying array parsing, specifically
 * edge cases.
 */
public class ArrayParsingTest
    extends tools.jackson.core.BaseTest
{
    public void testValidEmpty() throws Exception
    {
        final String DOC = "[   \n  ]";

        JsonParser p = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    public void testInvalidEmptyMissingClose() throws Exception
    {
        final String DOC = "[ ";

        JsonParser p = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        try {
            p.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (StreamReadException jex) {
            verifyException(jex, "expected close marker for ARRAY");
        }
        p.close();
    }

    public void testInvalidMissingFieldName() throws Exception
    {
        final String DOC = "[  : 3 ] ";

        JsonParser p = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        try {
            p.nextToken();
            fail("Expected a parsing error for odd character");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unexpected character");
        }
        p.close();
    }

    public void testInvalidExtraComma() throws Exception
    {
        final String DOC = "[ 24, ] ";

        JsonParser p = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(24, p.getIntValue());

        try {
            p.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (StreamReadException jex) {
            verifyException(jex, "expected a value");
        }
        p.close();
    }

    /**
     * Tests the missing value as 'null' in an array
     * This needs enabling of the Feature.ALLOW_MISSING_VALUES in JsonParser
     * This tests both Stream based parsing and the Reader based parsing
     * @throws Exception
     */
    public void testMissingValueAsNullByEnablingFeature() throws Exception
    {
    	_testMissingValueByEnablingFeature(true);
    	_testMissingValueByEnablingFeature(false);
    }

    /**
     * Tests the missing value in an array by not enabling
     * the Feature.ALLOW_MISSING_VALUES
     * @throws Exception
     */
    public void testMissingValueAsNullByNotEnablingFeature() throws Exception
    {
    	_testMissingValueNotEnablingFeature(true);
    	_testMissingValueNotEnablingFeature(false);
    }

    /**
     * Tests the not missing any value in an array by enabling the
     * Feature.ALLOW_MISSING_VALUES in JsonParser
     * This tests both Stream based parsing and the Reader based parsing for not missing any value
     * @throws Exception
     */
    public void testNotMissingValueByEnablingFeature() throws Exception
    {
        _testNotMissingValueByEnablingFeature(true);
        _testNotMissingValueByEnablingFeature(false);
    }

    private void _testMissingValueByEnablingFeature(boolean useStream) throws Exception {
        String DOC = "[ \"a\",,,,\"abc\", ] ";
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES).build();

        JsonParser p = useStream ? createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getValueAsString());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());

        p.close();

        // And another take
        DOC = "[,] ";
        p = useStream ? createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    private void _testMissingValueNotEnablingFeature(boolean useStream) throws Exception {
    	final String DOC = "[ \"a\",,\"abc\"] ";

    	JsonFactory f = new JsonFactory();

        JsonParser p = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getValueAsString());
        try {
	        assertToken(JsonToken.VALUE_STRING, p.nextToken());
	        fail("Expecting exception here");
        }
        catch (StreamReadException ex){
        	verifyException(ex, "expected a valid value", "expected a value");
        }
        p.close();
    }

    private void _testNotMissingValueByEnablingFeature(boolean useStream) throws Exception {
        final String DOC = "[ \"a\",\"abc\"] ";

        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES).build();
        JsonParser p = useStream ? createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getValueAsString());

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }
}
