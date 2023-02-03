package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardAposQuotedNamesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory STD_F = sharedStreamFactory();

    private final JsonFactory APOS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    public void testSingleQuotesDefault() throws Exception
    {
        _testSingleQuotesDefault(MODE_INPUT_STREAM);
        _testSingleQuotesDefault(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesDefault(MODE_DATA_INPUT);
        _testSingleQuotesDefault(MODE_READER);
    }

    public void testSingleQuotesEnabled() throws Exception
    {
        _testSingleQuotesEnabled(MODE_INPUT_STREAM);
        _testSingleQuotesEnabled(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesEnabled(MODE_DATA_INPUT);
        _testSingleQuotesEnabled(MODE_READER);
    }

    public void testSingleQuotesEscaped() throws Exception
    {
        _testSingleQuotesEscaped(MODE_INPUT_STREAM);
        _testSingleQuotesEscaped(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesEscaped(MODE_DATA_INPUT);
        _testSingleQuotesEscaped(MODE_READER);
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    /**
     * Test to verify that the default parser settings do not
     * accept single-quotes for String values (field names,
     * textual values)
     */
    private void _testSingleQuotesDefault(int mode) throws Exception
    {
        // First, let's see that by default they are not allowed
        String JSON = "[ 'text' ]";
        JsonParser p = createParser(STD_F, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }

        JSON = "{ 'a':1 }";
        p = createParser(STD_F, mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }
    }

    /**
     * Test to verify optional handling of single quotes,
     * to allow handling invalid (but, alas, common) JSON.
     */
    private void _testSingleQuotesEnabled(int mode) throws Exception
    {
        String JSON = "{ 'a' : 1, \"foobar\": 'b', '_abc\\u00A0e\\'23\\'':'d\\'foo\\'', '\"' : '\"\"', '':'' }";
        JsonParser p = createParser(APOS_F, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("foobar", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_abc\u00A0e'23'", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("d'foo'", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("\"", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        //assertEquals("\"\"", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("", p.getText());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();


        JSON = "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':3,'a':false }}";
        p = createParser(APOS_F, mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("x", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("y", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    // test to verify that we implicitly allow escaping of apostrophe
    private void _testSingleQuotesEscaped(int mode) throws Exception
    {
        String JSON = "[ '16\\'' ]";
        JsonParser p = createParser(APOS_F, mode, JSON);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("16'", p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    // [core#721]: specific issue with enclosed unescaped double quotes
    public void testSingleQuotedKeys721() throws Exception
    {
        // empty
        _testSingleQuotedKeys721("{ '\"\"': 'value'}", "\"\"");
        // non-empty
        _testSingleQuotedKeys721("{ '\"key\"': 'value'}", "\"key\"");
    }

    private void _testSingleQuotedKeys721(String doc, String expKey) throws Exception
    {
        _testSingleQuotedKeys721(MODE_READER, doc, expKey);
        _testSingleQuotedKeys721(MODE_INPUT_STREAM, doc, expKey);
        _testSingleQuotedKeys721(MODE_INPUT_STREAM_THROTTLED, doc, expKey);
        _testSingleQuotedKeys721(MODE_DATA_INPUT, doc, expKey);
    }

    private void _testSingleQuotedKeys721(int mode, String doc, String expKey) throws Exception
    {
        JsonParser p = createParser(APOS_F, mode, doc);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(expKey, p.nextFieldName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("value", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    // [core#721]: specific issue with enclosed unescaped double quotes
    public void testSingleQuotedValues721() throws Exception
    {
        // empty
        _testSingleQuotedValues721("{ \"bar\": '\"\"'}", "\"\"");
        // non-empty
        _testSingleQuotedValues721("{ \"bar\": '\"stuff\"'}", "\"stuff\"");
    }

    private void _testSingleQuotedValues721(String doc, String expValue) throws Exception
    {
        _testSingleQuotedValues721(MODE_READER, doc, expValue);
        _testSingleQuotedValues721(MODE_INPUT_STREAM, doc, expValue);
        _testSingleQuotedValues721(MODE_INPUT_STREAM_THROTTLED, doc, expValue);
        _testSingleQuotedValues721(MODE_DATA_INPUT, doc, expValue);
    }

    private void _testSingleQuotedValues721(int mode, String doc, String expValue) throws Exception
    {
        JsonParser p = createParser(APOS_F, mode, doc);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("bar", p.nextFieldName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(expValue, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
