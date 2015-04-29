package com.fasterxml.jackson.core.filter;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.*;

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
        JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
                );
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(aposToQuotes("{'ob':{'value':3}}"), w.toString());
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
