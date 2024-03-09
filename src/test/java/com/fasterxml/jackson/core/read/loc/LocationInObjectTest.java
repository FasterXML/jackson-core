package com.fasterxml.jackson.core.read.loc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// tests for [core#37]
public class LocationInObjectTest extends BaseTest
{
    @Test
    public void testOffsetWithObjectFieldsUsingUTF8() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        byte[] b = "{\"f1\":\"v1\",\"f2\":{\"f3\":\"v3\"},\"f4\":[true,false],\"f5\":5}".getBytes("UTF-8");
        //            1      6      11    16 17    22      28    33 34 39      46    51
        JsonParser p = f.createParser(b);

        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(1L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(6L, p.currentTokenLocation().getByteOffset());

        assertEquals("f2", p.nextFieldName());
        assertEquals(11L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.START_OBJECT, p.nextValue());
        assertEquals(16L, p.currentTokenLocation().getByteOffset());

        assertEquals("f3", p.nextFieldName());
        assertEquals(17L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextValue());
        assertEquals(22L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals("f4", p.nextFieldName());
        assertEquals(28L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.START_ARRAY, p.nextValue());
        assertEquals(33L, p.currentTokenLocation().getByteOffset());

        assertEquals(JsonToken.VALUE_TRUE, p.nextValue());
        assertEquals(34L, p.currentTokenLocation().getByteOffset());

        assertEquals(JsonToken.VALUE_FALSE, p.nextValue());
        assertEquals(39L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());

        assertEquals("f5", p.nextFieldName());
        assertEquals(46L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(51L, p.currentTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        p.close();
    }

    @Test
    public void testOffsetWithObjectFieldsUsingReader() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        char[] c = "{\"f1\":\"v1\",\"f2\":{\"f3\":\"v3\"},\"f4\":[true,false],\"f5\":5}".toCharArray();
        //            1      6      11    16 17    22      28    33 34 39      46    51
        JsonParser p = f.createParser(c);

        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(1L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(6L, p.currentTokenLocation().getCharOffset());

        assertEquals("f2", p.nextFieldName());
        assertEquals(11L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.START_OBJECT, p.nextValue());
        assertEquals(16L, p.currentTokenLocation().getCharOffset());

        assertEquals("f3", p.nextFieldName());
        assertEquals(17L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextValue());
        assertEquals(22L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals("f4", p.nextFieldName());
        assertEquals(28L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.START_ARRAY, p.nextValue());
        assertEquals(33L, p.currentTokenLocation().getCharOffset());

        assertEquals(JsonToken.VALUE_TRUE, p.nextValue());
        assertEquals(34L, p.currentTokenLocation().getCharOffset());

        assertEquals(JsonToken.VALUE_FALSE, p.nextValue());
        assertEquals(39L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());

        assertEquals("f5", p.nextFieldName());
        assertEquals(46L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(51L, p.currentTokenLocation().getCharOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        p.close();
    }
}
