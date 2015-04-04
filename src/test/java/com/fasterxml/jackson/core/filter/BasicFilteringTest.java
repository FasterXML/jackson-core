package com.fasterxml.jackson.core.filter;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class BasicFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    static class NameMatchFilter extends TokenFilter
    {
        
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    private final JsonFactory JSON_F = new JsonFactory();
    
    public void testNonFiltering() throws Exception
    {
        // First, verify non-filtering
        StringWriter w = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(w);
        _writeSimpleDoc(gen);
        gen.close();
        assertEquals(aposToQuotes("{'a':123,'array':[1,2],'ob':{'value':3},'b':true}"),
                w.toString());
    }

    public void testSingleMatchFiltering() throws Exception
    {
        // First, verify non-filtering
        StringWriter w = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(w);
        _writeSimpleDoc(gen);
        gen.close();
        assertEquals(aposToQuotes("{'a':123,'array':[1,2],'ob':{'value':3},'b':true}"),
                w.toString());
    }

    protected void _writeSimpleDoc(JsonGenerator gen) throws IOException
    {
        // { "a" : 123,
        //   "array" : [ 1, 2 ],
        //   "ob" : { "value" : 3 },
        //   "b" : true
        // }

        gen.writeStartObject();

        gen.writeFieldName("a");
        gen.writeNumber(123);
        
        gen.writeFieldName("array");
        gen.writeStartArray();
        gen.writeNumber(1);
        gen.writeNumber(2);
        gen.writeEndArray();

        gen.writeFieldName("ob");
        gen.writeStartObject();
        gen.writeFieldName("value");
        gen.writeNumber(3);
        gen.writeEndObject();

        gen.writeFieldName("b");
        gen.writeBoolean(true);

        gen.writeEndObject();
    }
}
