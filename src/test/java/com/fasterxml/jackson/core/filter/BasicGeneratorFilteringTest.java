package com.fasterxml.jackson.core.filter;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Low-level tests for explicit, hand-written tests for generator-side
 * filtering.
 */
@SuppressWarnings("resource")
public class BasicGeneratorFilteringTest extends BaseTest
{
    static class NameMatchFilter extends TokenFilter
    {
        private final Set<String> _names;
        
        public NameMatchFilter(String... names) {
            _names = new HashSet<String>(Arrays.asList(names));
        }

        @Override
        public TokenFilter includeElement(int index) {
            return this;
        }

        @Override
        public TokenFilter includeProperty(String name) {
            if (_names.contains(name)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return this;
        }

        @Override
        protected boolean _includeScalar() { return false; }
    }

    static class IndexMatchFilter extends TokenFilter
    {
        private final BitSet _indices;
        
        public IndexMatchFilter(int... ixs) {
            _indices = new BitSet();
            for (int ix : ixs) {
                _indices.set(ix);
            }
        }

        @Override
        public TokenFilter includeProperty(String name) {
            return this;
        }
        
        @Override
        public TokenFilter includeElement(int index) {
            if (_indices.get(index)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return null;
        }

        @Override
        protected boolean _includeScalar() { return false; }
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
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes(
                "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}"),
                w.toString());
    }

    public void testSingleMatchFilteringWithoutPath() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("value"),
                false, // includePath
                false // multipleMatches
                );
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);

        // 21-Apr-2015, tatu: note that there were plans to actually
        //     allow "immediate parent inclusion" for matches on property
        //    names. This behavior was NOT included in release however, so:
//        assertEquals(aposToQuotes("{'value':3}"), w.toString());

        assertEquals(aposToQuotes("3"), w.toString());
    }

    public void testSingleMatchFilteringWithPath() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator origGen = JSON_F.createGenerator(w);
        NameMatchFilter filter = new NameMatchFilter("value");
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(origGen,
                filter,
                true, // includePath
                false // multipleMatches
                );

        // Hmmh. Should we get access to eventual target?
        assertSame(w, gen.getOutputTarget());
        assertNotNull(gen.getFilterContext());
        assertSame(filter, gen.getFilter());

        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'ob':{'value':3}}"), w.toString());

        assertEquals(1, gen.getMatchCount());
    }

    public void testSingleMatchFilteringWithPathSkippedArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator origGen = JSON_F.createGenerator(w);
        NameMatchFilter filter = new NameMatchFilter("value");
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(origGen,
                filter,
                true, // includePath
                false // multipleMatches
                );

        // Hmmh. Should we get access to eventual target?
        assertSame(w, gen.getOutputTarget());
        assertNotNull(gen.getFilterContext());
        assertSame(filter, gen.getFilter());

        final String JSON = "{'array':[1,[2,3]],'ob':[{'value':'bar'}],'b':{'foo':[1,'foo']}}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'ob':[{'value':'bar'}]}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    // Alternative take, using slightly different calls for FIELD_NAME, START_ARRAY
    public void testSingleMatchFilteringWithPathAlternate1() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
                );
        //final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':[3],'value2':'foo'},'b':true}";

        gen.writeStartObject();
        gen.writeFieldName(new SerializedString("a"));
        gen.writeNumber(123);

        gen.writeFieldName("array");
        gen.writeStartArray(2);
        gen.writeNumber("1");
        gen.writeNumber((short) 2);
        gen.writeEndArray();

        gen.writeFieldName(new SerializedString("ob"));
        gen.writeStartObject();
        gen.writeNumberField("value0", 2);
        gen.writeFieldName(new SerializedString("value"));
        gen.writeStartArray(1);
        gen.writeString(new SerializedString("x")); // just to vary generation method
        gen.writeEndArray();
        gen.writeStringField("value2", "foo");

        gen.writeEndObject();

        gen.writeBooleanField("b", true);
        
        gen.writeEndObject();
        gen.close();

        assertEquals(aposToQuotes("{'ob':{'value':['x']}}"), w.toString());
    }

    public void testSingleMatchFilteringWithPathRawBinary() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("array"),
                true, // includePath
                false // multipleMatches
                );
        //final String JSON = "{'header':['ENCODED',raw],'array':['base64stuff',1,2,3,4,5,6.25,7.5],'extra':[1,2,3,4,5,6.25,7.5]}";

        gen.writeStartObject();

        gen.writeFieldName("header");
        gen.writeStartArray();
        gen.writeBinary(new byte[] { 1 });
        gen.writeRawValue(new SerializedString("1"));
        gen.writeRawValue("2");
        gen.writeEndArray();
        
        gen.writeFieldName("array");

        gen.writeStartArray();
        gen.writeBinary(new byte[] { 1 });
        gen.writeNumber((short) 1);
        gen.writeNumber((int) 2);
        gen.writeNumber((long) 3);
        gen.writeNumber(BigInteger.valueOf(4));
        gen.writeRaw(" ");
        gen.writeNumber(new BigDecimal("5.0"));
        gen.writeRaw(new SerializedString(" /*x*/"));
        gen.writeNumber(6.25f);
        gen.writeNumber(7.5);
        gen.writeEndArray();

        gen.writeArrayFieldStart("extra");
        gen.writeNumber((short) 1);
        gen.writeNumber((int) 2);
        gen.writeNumber((long) 3);
        gen.writeNumber(BigInteger.valueOf(4));
        gen.writeNumber(new BigDecimal("5.0"));
        gen.writeNumber(6.25f);
        gen.writeNumber(7.5);
        gen.writeEndArray();
        
        gen.writeEndObject();
        gen.close();

        assertEquals(aposToQuotes("{'array':['AQ==',1,2,3,4 ,5.0 /*x*/,6.25,7.5]}"), w.toString());
    }
    
    public void testMultipleMatchFilteringWithPath1() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("value0", "value2"),
                true, /* includePath */ true /* multipleMatches */ );
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'ob':{'value0':2,'value2':4}}"), w.toString());
    }

    public void testMultipleMatchFilteringWithPath2() throws Exception
    {
        StringWriter w = new StringWriter();
        
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("array", "b", "value"),
                true, true);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'array':[1,2],'ob':{'value':3},'b':true}"), w.toString());
    }

    public void testMultipleMatchFilteringWithPath3() throws Exception
    {
        StringWriter w = new StringWriter();
        
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("value"),
                true, true);
        final String JSON = "{'root':{'a0':true,'a':{'value':3},'b':{'value':4}},'b0':false}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'root':{'a':{'value':3},'b':{'value':4}}}"), w.toString());
    }

    public void testIndexMatchWithPath1() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new IndexMatchFilter(1),
                true, true);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'array':[2]}"), w.toString());

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new IndexMatchFilter(0),
                true, true);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'array':[1]}"), w.toString());
    }

    public void testIndexMatchWithPath2() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new IndexMatchFilter(0,1),
                true, true);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'array':[1,2]}"), w.toString());
    }
}
