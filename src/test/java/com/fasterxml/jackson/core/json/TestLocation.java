package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

// NOTE: just a stub so for, fill me!
public class TestLocation extends com.fasterxml.jackson.core.BaseTest
{
    // Trivially simple unit test for basics wrt offsets
    public void testSimpleInitialOffsets() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        JsonLocation loc;
        JsonParser p;
        final String DOC = "{ }";

        // first, char based:
        p = f.createParser(DOC);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(0L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();

        // then byte-based

        p = f.createParser(DOC.getBytes("UTF-8"));
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    // for [Issue#111]
    public void testOffsetWithInputOffset() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        JsonLocation loc;
        JsonParser p;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "   { }  ".getBytes("UTF-8");

        // and then peel them off
        p = f.createParser(b, 3, b.length-5);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    public void testOffsetWithObjectFields() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        JsonLocation loc;
        JsonParser p;
        JsonToken t;
        String s;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "{\"f1\":\"v1\",\"f2\":{\"f3\":\"v3\"},\"f4\":[true,false],\"f5\":5}".getBytes("UTF-8");
        //            1      6      11    16 17    22      28    33 34 39      46    51
        // and then peel them off
        p = f.createParser(b);

        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(1L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(6L, p.getTokenLocation().getByteOffset());

        assertEquals("f2", p.nextFieldName());
        assertEquals(11L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.START_OBJECT, p.nextValue());
        assertEquals(16L, p.getTokenLocation().getByteOffset());

        assertEquals("f3", p.nextFieldName());
        assertEquals(17L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_STRING, p.nextValue());
        assertEquals(22L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals("f4", p.nextFieldName());
        assertEquals(28L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.START_ARRAY, p.nextValue());
        assertEquals(33L, p.getTokenLocation().getByteOffset());

        assertEquals(JsonToken.VALUE_TRUE, p.nextValue());
        assertEquals(34L, p.getTokenLocation().getByteOffset());

        assertEquals(JsonToken.VALUE_FALSE, p.nextValue());
        assertEquals(39L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());

        assertEquals("f5", p.nextFieldName());
        assertEquals(46L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(51L, p.getTokenLocation().getByteOffset());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        p.close();
    }
}
