package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;

/* Additional testing for {@link java.io.DataInput} specific
 * challenges for parsing.
 */
public class DataInputTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testEOFAfterArray() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "[ 1 ]  ");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    public void testEOFAfterObject() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "{ \"value\" : true }");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    public void testEOFAfterScalar() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "\"foobar\" ");
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foobar", p.getText());
        assertNull(p.nextToken());
        p.close();
    }
}
