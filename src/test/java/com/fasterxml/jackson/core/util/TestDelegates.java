package com.fasterxml.jackson.core.util;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestDelegates extends com.fasterxml.jackson.core.BaseTest
{
    /**
     * Test default, non-overridden parser delegate.
     */
    public void testParserDelegate() throws IOException
    {
        JsonParser jp = new JsonFactory().createParser("[ 1, true ]");
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals("[", jp.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertTrue(jp.getBooleanValue());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
        assertTrue(jp.isClosed());
    }

    /**
     * Test default, non-overridden generator delegate.
     */
    public void testGeneratorDelegate() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonFactory().createGenerator(sw);
        jg.writeStartArray();
        jg.writeNumber(13);
        jg.writeNull();
        jg.writeBoolean(false);
        jg.writeEndArray();
        jg.close();
        assertTrue(jg.isClosed());        
        assertEquals("[13,null,false]", sw.toString());
    }

    public void testNotDelegateCopyMethods() throws IOException
    {
        JsonParser jp = new JsonFactory().createParser("[{\"a\":[1,2,{\"b\":3}],\"c\":\"d\"},{\"e\":false},null]");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonGeneratorDelegate(new JsonFactory().createGenerator(sw), false) {
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
