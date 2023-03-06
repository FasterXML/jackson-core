package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

/**
 * Set of additional unit for verifying array parsing, specifically
 * edge cases.
 */
public class ArrayParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testValidEmpty() throws Exception
    {
        final String DOC = "[   \n  ]";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testInvalidEmptyMissingClose() throws Exception
    {
        final String DOC = "[ ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (JsonParseException jex) {
            verifyException(jex, "expected close marker for ARRAY");
        }
        jp.close();
    }

    public void testInvalidMissingFieldName() throws Exception
    {
        final String DOC = "[  : 3 ] ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected a parsing error for odd character");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unexpected character");
        }
        jp.close();
    }

    public void testInvalidExtraComma() throws Exception
    {
        final String DOC = "[ 24, ] ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(24, jp.getIntValue());

        try {
            jp.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (JsonParseException jex) {
            verifyException(jex, "expected a value");
        }
        jp.close();
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
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .build();
        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();

        // And another take
        DOC = "[,] ";
        jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }

    private void _testMissingValueNotEnablingFeature(boolean useStream) throws Exception {
    	final String DOC = "[ \"a\",,\"abc\"] ";

    	JsonFactory f = new JsonFactory();

        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());
        try {
	        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
	        fail("Expecting exception here");
        }
        catch(JsonParseException ex){
        	verifyException(ex, "expected a valid value", "expected a value");
        }
        jp.close();
    }

    private void _testNotMissingValueByEnablingFeature(boolean useStream) throws Exception {
        final String DOC = "[ \"a\",\"abc\"] ";

        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .build();
        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());

        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        jp.close();
    }
}
