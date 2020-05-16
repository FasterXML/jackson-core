package com.fasterxml.jackson.core.filter;

import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;

@SuppressWarnings("resource")
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

    private final String SIMPLE = aposToQuotes(
            "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':0.25},'b':true}");

    public void testNonFiltering() throws Exception
    {
        JsonParser p = JSON_F.createParser(SIMPLE);
        String result = readAndWrite(JSON_F, p);
        assertEquals(SIMPLE, result);
    }

    public void testSingleMatchFilteringWithoutPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   false // multipleMatches
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath1() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("a"),
                true, // includePath
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'a':123}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath2() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{\"ob\":{\"value\":3}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath3() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'ob':{'value0':2,'value':3,'value2':4},'array':[1,2],'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("ob"),
                true, // includePath
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value0':2,'value':3,'value2':4}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithoutPath1() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   false // multipleMatches -false
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithoutPath2() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'array':[3,4],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                false, // includePath
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("2"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath1() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'array':[3,4],'ob':{'value':3,'array':[5,6],'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                true, // includePath
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{\"array\":[2]}"), result);
        assertEquals(1, p.getMatchCount());
    }


    public void testNotAllowMultipleMatchesWithPath2() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'ob':{'value':3,'array':[1,2],'value':{'value0':2}},'array':[3,4]}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                true, // includePath
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{\"ob\":{\"array\":[2]}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath3() throws Exception
    {
        String jsonString = aposToQuotes("{'ob':{'value':3,'ob':{'value':2}},'value':\"val\"}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath4() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value1':1},'ob2':{'ob':{'value2':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("ob"),
                true, // includePath
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value1':1}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithoutPath() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   false, // includePath
                   true // multipleMatches - true
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("3 {\"value0\":2} \"val\""), result);
        assertEquals(3, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithPath1() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                true // multipleMatches - true
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{\"ob\":{\"value\":3,\"value\":{\"value0\":2}},\"value\":\"val\"}"), result);
        assertEquals(3, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithPath2() throws Exception
    {
        String jsonString = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'array':[3,4],'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                true, // includePath
                true // multipleMatches - true
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{\"array\":[2],\"ob\":{\"array\":[4]}}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath1() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value0", "value2"),
                true, /* includePath */ true /* multipleMatches */ );
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value0':2,'value2':0.25}}"), result);
        assertEquals(2, p.getMatchCount());

    }

    public void testMultipleMatchFilteringWithPath2() throws Exception
    {
        String INPUT = aposToQuotes("{'a':123,'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(INPUT);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("b", "value"),
                true, true);

        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value':3},'b':true}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath3() throws Exception
    {
        final String JSON = aposToQuotes("{'root':{'a0':true,'a':{'value':3},'b':{'value':\"foo\"}},'b0':false}");
        JsonParser p0 = JSON_F.createParser(JSON);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'root':{'a':{'value':3},'b':{'value':\"foo\"}}}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testIndexMatchWithPath1() throws Exception
    {
        FilteringParserDelegate p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(1), true, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'array':[2]}"), result);
        assertEquals(1, p.getMatchCount());

        p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0), true, true);
        result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'array':[1]}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testIndexMatchWithPath2() throws Exception
    {
        FilteringParserDelegate p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0, 1), true, true);
        assertEquals(aposToQuotes("{'array':[1,2]}"), readAndWrite(JSON_F, p));
        assertEquals(2, p.getMatchCount());
    
        String JSON = aposToQuotes("{'a':123,'array':[1,2,3,4,5],'b':[1,2,3]}");
        p = new FilteringParserDelegate(JSON_F.createParser(JSON),
                new IndexMatchFilter(1, 3), true, true);
        assertEquals(aposToQuotes("{'array':[2,4],'b':[2]}"), readAndWrite(JSON_F, p));
        assertEquals(3, p.getMatchCount());
    }

    public void testBasicSingleMatchFilteringWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
        );

// {'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}
        String result = readAndWrite(JSON_F, p);
        assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);
    }

    public void testTokensSingleMatchWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
        );

        assertFalse(p.hasCurrentToken());
        assertNull(p.getCurrentToken());
        assertEquals(JsonTokenId.ID_NO_TOKEN, p.currentTokenId());
        assertFalse(p.isExpectedStartObjectToken());
        assertFalse(p.isExpectedStartArrayToken());

// {'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}
//      String result = readAndWrite(JSON_F, p);
//      assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.START_OBJECT, p.getCurrentToken());
        assertEquals(JsonToken.START_OBJECT, p.currentToken());
        assertEquals(JsonTokenId.ID_START_OBJECT, p.currentTokenId());
        assertEquals(JsonTokenId.ID_START_OBJECT, p.currentTokenId());
        assertTrue(p.isExpectedStartObjectToken());
        assertFalse(p.isExpectedStartArrayToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertTrue(p.hasToken(JsonToken.FIELD_NAME));
        assertTrue(p.hasTokenId(JsonTokenId.ID_FIELD_NAME));
        assertEquals("ob", p.getCurrentName());
//        assertEquals("ob", p.getText());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("ob", p.getCurrentName());

        assertEquals(p0.getCurrentLocation(), p.getCurrentLocation());
        
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.getCurrentName());
        assertEquals("value", p.getText());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.getCurrentToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals(3, p.getIntValue());
        assertEquals(3, p.getValueAsInt());
        assertEquals(3, p.getValueAsInt(7));
        assertEquals(3L, p.getLongValue());
        assertEquals(3L, p.getValueAsLong());
        assertEquals(3L, p.getValueAsLong(6L));
        assertEquals((double)3, p.getDoubleValue());
        assertEquals((double)3, p.getValueAsDouble());
        assertEquals((double)3, p.getValueAsDouble(0.5));
        assertEquals((short)3, p.getShortValue());
        assertEquals((byte)3, p.getByteValue());
        assertEquals((float)3, p.getFloatValue());
        assertEquals(BigInteger.valueOf(3L), p.getBigIntegerValue());
        assertEquals(Integer.valueOf(3), p.getNumberValue());
        assertTrue(p.getValueAsBoolean());
        assertTrue(p.getValueAsBoolean(false));

        assertEquals("value", p.currentName());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());

        p.clearCurrentToken();
        assertNull(p.getCurrentToken());

        p.close();
    }

    public void testSkippingForSingleWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                true, // includePath
                false // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        p.skipChildren();
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextToken());
    }
}
