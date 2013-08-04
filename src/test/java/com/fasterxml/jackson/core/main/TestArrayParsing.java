package com.fasterxml.jackson.core.main;

import com.fasterxml.jackson.core.*;

/**
 * Set of additional unit for verifying array parsing, specifically
 * edge cases.
 */
public class TestArrayParsing
    extends com.fasterxml.jackson.test.BaseTest
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
}
