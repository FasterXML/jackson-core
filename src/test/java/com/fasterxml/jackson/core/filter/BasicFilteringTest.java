package com.fasterxml.jackson.core.filter;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class BasicFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    static class NameMatchFilter extends TokenFilter
    {
        private final String _name;
        
        public NameMatchFilter(String n) { _name = n; }
        
        @Override
        public int includeProperty(String name) {
//System.err.println("Include? "+name);
            if (name.equals(_name)) {
//System.err.println(" -> true");
                return TokenFilter.FILTER_INCLUDE;
            }
//System.err.println(" -> false");
            return TokenFilter.FILTER_CHECK;
        }
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

    @SuppressWarnings("resource")
    public void testSingleMatchFiltering() throws Exception
    {
        // First, verify non-filtering
        StringWriter w = new StringWriter();
        JsonGenerator gen0 = JSON_F.createGenerator(w);
        JsonGenerator gen = new FilteringGeneratorDelegate(gen0, new NameMatchFilter("value"));
        
        _writeSimpleDoc(gen);
        gen.close();

//System.out.println("JSON -> "+w.toString());
        
        assertEquals(aposToQuotes("{'value':3}"),
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
