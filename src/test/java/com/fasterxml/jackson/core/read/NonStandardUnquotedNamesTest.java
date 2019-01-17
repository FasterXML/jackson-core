package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class NonStandardUnquotedNamesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleUnquotedBytes() throws Exception {
        _testSimpleUnquoted(MODE_INPUT_STREAM);
        _testSimpleUnquoted(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleUnquoted(MODE_DATA_INPUT);
    }

    public void testSimpleUnquotedChars() throws Exception {
        _testSimpleUnquoted(MODE_READER);
    }
    
    public void testLargeUnquoted() throws Exception
    {
        _testLargeUnquoted(MODE_INPUT_STREAM);
        _testLargeUnquoted(MODE_INPUT_STREAM_THROTTLED);
        _testLargeUnquoted(MODE_DATA_INPUT);
        _testLargeUnquoted(MODE_READER);
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    private void _testLargeUnquoted(int mode) throws Exception
    {
        StringBuilder sb = new StringBuilder(5000);
        sb.append("[\n");
        //final int REPS = 2000;
        final int REPS = 1050;
        for (int i = 0; i < REPS; ++i) {
            if (i > 0) {
                sb.append(',');
                if ((i & 7) == 0) {
                    sb.append('\n');
                }
            }
            sb.append("{");
            sb.append("abc").append(i&127).append(':');
            sb.append((i & 1) != 0);
            sb.append("}\n");
        }
        sb.append("]");
        String JSON = sb.toString();
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JsonParser p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        for (int i = 0; i < REPS; ++i) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("abc"+(i&127), p.getCurrentName());
            assertToken(((i&1) != 0) ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testSimpleUnquoted(int mode) throws Exception
    {
        final JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        String JSON = "{ a : 1, _foo:true, $:\"money!\", \" \":null }";
        JsonParser p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_foo", p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("$", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("money!", p.getText());

        // and then regular quoted one should still work too:
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(" ", p.getCurrentName());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();

        // Another thing, as per [Issue#102]: numbers

        JSON = "{ 123:true,4:false }";
        p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("123", p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("4", p.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
