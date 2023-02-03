package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class NonStandardUnquotedNamesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory UNQUOTED_FIELDS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

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

    // [core#510]: ArrayIndexOutOfBounds
    public void testUnquotedIssue510() throws Exception
    {
        // NOTE! Requires longer input buffer to trigger longer codepath
        char[] fullChars = new char[4001];
        for (int i = 0; i < 3998; i++) {
             fullChars[i] = ' ';
        }
        fullChars[3998] = '{';
        fullChars[3999] = 'a';
        fullChars[4000] = 256;

        JsonParser p = UNQUOTED_FIELDS_F.createParser(new java.io.StringReader(new String(fullChars)));
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            ; // should fail here
        }
        p.close();
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    public void testNonStandardNameChars() throws Exception
    {
        _testNonStandardNameChars(MODE_INPUT_STREAM);
        _testNonStandardNameChars(MODE_INPUT_STREAM_THROTTLED);
        _testNonStandardNameChars(MODE_DATA_INPUT);
        _testNonStandardNameChars(MODE_READER);
    }

    private void _testNonStandardNameChars(int mode) throws Exception
    {
        String JSON = "{ @type : \"mytype\", #color : 123, *error* : true, "
            +" hyphen-ated : \"yes\", me+my : null"
            +"}";
        JsonParser p = createParser(UNQUOTED_FIELDS_F, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("@type", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("mytype", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("#color", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("*error*", p.getText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("hyphen-ated", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yes", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("me+my", p.getText());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

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
        JsonParser p = createParser(UNQUOTED_FIELDS_F, mode, JSON);
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
        String JSON = "{ a : 1, _foo:true, $:\"money!\", \" \":null }";
        JsonParser p = createParser(UNQUOTED_FIELDS_F, mode, JSON);

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
        p = createParser(UNQUOTED_FIELDS_F, mode, JSON);

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
