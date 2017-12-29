package com.fasterxml.jackson.core.read;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.util.Named;

public class NextNameWithMatcherTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final List<String> NAMES_1 = Arrays.asList("enabled", "a", "longerName", "otherStuff3");
    private final List<String> NAMES_1_CASE_MISMATCH = Arrays.asList("ENABLED", "A", "LongerName", "otherStuff3");

    private final List<Named> FIELDS_1 = namedFromStrings(NAMES_1);

    private final FieldNameMatcher MATCHER_CS_1 = JSON_F.constructFieldNameMatcher(FIELDS_1, true);
    private final FieldNameMatcher MATCHER_CI_1 = JSON_F.constructCIFieldNameMatcher(FIELDS_1, true);

    private final String DOC_1 = aposToQuotes(
            "{ 'a' : 4, 'enabled' : true, 'longerName' : 'Billy-Bob Burger', 'extra' : [ 0], 'otherStuff3' : 0.25 }"
            );
    
    private final String DOC_1_CASE_MISMATCH = aposToQuotes(
            "{ 'A' : 4, 'ENABLED' : true, 'LongerName' : 'Billy-Bob Burger', 'extra' : [0 ], 'otherStuff3' : 0.25 }");

    public void testSimpleCaseSensitive() throws Exception
    {
        _testSimpleCaseSensitive(MODE_INPUT_STREAM);
        _testSimpleCaseSensitive(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleCaseSensitive(MODE_DATA_INPUT);
        _testSimpleCaseSensitive(MODE_READER);
    }

    private void _testSimpleCaseSensitive(int mode) throws Exception
    {
        _verifyDoc1(createParser(mode, DOC_1), MATCHER_CS_1, NAMES_1);
    }

    public void testSimpleCaseInsensitive() throws Exception
    {
        _testSimpleCaseInsensitive(MODE_INPUT_STREAM);
        _testSimpleCaseInsensitive(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleCaseInsensitive(MODE_DATA_INPUT);
        _testSimpleCaseInsensitive(MODE_READER);
    }

    public void _testSimpleCaseInsensitive(int mode) throws Exception
    {
        // First, should still pass regular case-matching doc
        _verifyDoc1(createParser(mode, DOC_1), MATCHER_CI_1, NAMES_1);
        // but then mis-cased one too:
        _verifyDoc1(createParser(mode, DOC_1_CASE_MISMATCH), MATCHER_CI_1, NAMES_1_CASE_MISMATCH);
    }

    private void _verifyDoc1(JsonParser p, FieldNameMatcher matcher,
            List<String> names) throws Exception
    {
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.START_OBJECT, p.currentToken());

        assertEquals(1, p.nextFieldName(matcher)); // ("enabled", "a", "longerName", "otherStuff3")
        assertEquals(names.get(1), p.currentName());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        assertEquals(4, p.getIntValue());

        assertEquals(0, p.nextFieldName(matcher)); // ("enabled", "a", "longerName", "otherStuff3")
        assertEquals(names.get(0), p.currentName());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.VALUE_TRUE, p.currentToken());

        assertEquals(2, p.nextFieldName(matcher)); // ("enabled", "a", "longerName", "otherStuff3")
        assertEquals(names.get(2), p.currentName());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.VALUE_STRING, p.currentToken());
        assertEquals("Billy-Bob Burger", p.getText());

        assertEquals(FieldNameMatcher.MATCH_UNKNOWN_NAME, p.nextFieldName(matcher));
        assertEquals("extra", p.currentName());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.START_ARRAY, p.currentToken());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        assertEquals(0, p.getIntValue());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.END_ARRAY, p.currentToken());

        assertEquals(3, p.nextFieldName(matcher)); // ("enabled", "a", "longerName", "otherStuff3")
        assertEquals(names.get(3), p.currentName());
        assertEquals(FieldNameMatcher.MATCH_ODD_TOKEN, p.nextFieldName(matcher));
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.currentToken());
        assertEquals(0.25, p.getDoubleValue());

        assertEquals(FieldNameMatcher.MATCH_END_OBJECT, p.nextFieldName(matcher));
        assertToken(JsonToken.END_OBJECT, p.currentToken());

        p.close();
    }
}
