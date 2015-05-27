package com.fasterxml.jackson.core.util;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestDelegates extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    /**
     * Test default, non-overridden parser delegate.
     */
    public void testParserDelegate() throws IOException
    {
        JsonParser jp0 = JSON_F.createParser("[ 1, true, null, { } ]");
        JsonParserDelegate jp = new JsonParserDelegate(jp0);
        
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals("[", jp.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());

        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertTrue(jp.getBooleanValue());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        jp.close();
        assertTrue(jp.isClosed());
        assertTrue(jp0.isClosed());

        jp0.close();
    }

    /**
     * Test default, non-overridden generator delegate.
     */
    public void testGeneratorDelegate() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(sw);
        JsonGeneratorDelegate jg = new JsonGeneratorDelegate(g0);
        jg.writeStartArray();

        assertEquals(1, jg.getOutputBuffered());
        
        jg.writeNumber(13);
        jg.writeNull();
        jg.writeBoolean(false);
        jg.writeString("foo");
        jg.writeStartObject();
        jg.writeEndObject();

        jg.writeStartArray(0);
        jg.writeEndArray();

        jg.writeEndArray();
        
        jg.flush();
        jg.close();
        assertTrue(jg.isClosed());        
        assertTrue(g0.isClosed());        
        assertEquals("[13,null,false,\"foo\",{},[]]", sw.toString());

        g0.close();
    }

    public void testNotDelegateCopyMethods() throws IOException
    {
        JsonParser jp = JSON_F.createParser("[{\"a\":[1,2,{\"b\":3}],\"c\":\"d\"},{\"e\":false},null]");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonGeneratorDelegate(JSON_F.createGenerator(sw), false) {
            @Override
            public void writeFieldName(String name) throws IOException, JsonGenerationException {
                super.writeFieldName(name+"-test");
                super.writeBoolean(true);
                super.writeFieldName(name);
            }
        };
        jp.nextToken();
        jg.copyCurrentStructure(jp);
        jg.flush();
        assertEquals("[{\"a-test\":true,\"a\":[1,2,{\"b-test\":true,\"b\":3}],\"c-test\":true,\"c\":\"d\"},{\"e-test\":true,\"e\":false},null]", sw.toString());
        jp.close();
        jg.close();
    }
}
