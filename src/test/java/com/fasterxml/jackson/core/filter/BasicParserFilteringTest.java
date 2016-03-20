package com.fasterxml.jackson.core.filter;

import java.util.*;

import com.fasterxml.jackson.core.*;

public class BasicParserFilteringTest extends BaseTest
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

    private final String SIMPLE = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");

    @SuppressWarnings("resource")
    public void testNonFiltering() throws Exception
    {
        JsonParser p = JSON_F.createParser(SIMPLE);
        String result = readAndWrite(JSON_F, p);
        assertEquals(SIMPLE, result);
    }

    @SuppressWarnings("resource")
    public void testSingleMatchFilteringWithoutPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   false // multipleMatches
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3"), result);
    }

    @SuppressWarnings("resource")
    public void testSingleMatchFilteringWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   true, // includePath
                   false // multipleMatches
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);
    }

    
    @SuppressWarnings("resource")
    public void testNotAllowMultipleMatches() throws Exception
    {
    	String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'value':4,'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        JsonParser p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   false // multipleMatches -false
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3"), result);
    }
    
    @SuppressWarnings("resource")
    public void testAllowMultipleMatches() throws Exception
    {
    	String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'value':4,'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        JsonParser p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   true // multipleMatches - true
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3 4"), result);
    }

    @SuppressWarnings("resource")
    public void testMultipleMatchFilteringWithPath1() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value0", "value2"),
                true, /* includePath */ true /* multipleMatches */ );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value0':2,'value2':4}}"), result);
    }

    @SuppressWarnings("resource")
    public void testMultipleMatchFilteringWithPath2() throws Exception
    {
        String INPUT = aposToQuotes("{'a':123,'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(INPUT);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("b", "value"),
                true, true);

        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value':3},'b':true}"), result);
    }

    @SuppressWarnings("resource")
    public void testMultipleMatchFilteringWithPath3() throws Exception
    {
        final String JSON = aposToQuotes("{'root':{'a0':true,'a':{'value':3},'b':{'value':4}},'b0':false}");
        JsonParser p0 = JSON_F.createParser(JSON);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'root':{'a':{'value':3},'b':{'value':4}}}"), result);
    }

    @SuppressWarnings("resource")
    public void testIndexMatchWithPath1() throws Exception
    {
        JsonParser p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(1), true, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'array':[2]}"), result);

        p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0), true, true);
        result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'array':[1]}"), result);
    }

    @SuppressWarnings("resource")
    public void testIndexMatchWithPath2() throws Exception
    {
        JsonParser p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0, 1), true, true);
        assertEquals(aposToQuotes("{'array':[1,2]}"), readAndWrite(JSON_F, p));
    
        String JSON = aposToQuotes("{'a':123,'array':[1,2,3,4,5],'b':[1,2,3]}");
        p = new FilteringParserDelegate(JSON_F.createParser(JSON),
                new IndexMatchFilter(1, 3), true, true);
        assertEquals(aposToQuotes("{'array':[2,4],'b':[2]}"), readAndWrite(JSON_F, p));
    }
}
