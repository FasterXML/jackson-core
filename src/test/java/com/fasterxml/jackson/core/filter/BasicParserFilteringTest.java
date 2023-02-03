package com.fasterxml.jackson.core.filter;

import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

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

    static class StrictNameMatchFilter extends TokenFilter
    {
        private final Set<String> _names;

        public StrictNameMatchFilter(String... names) {
            _names = new HashSet<String>(Arrays.asList(names));
        }

        @Override
        public TokenFilter includeProperty(String name) {
            if (_names.contains(name)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return null;
        }
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

    static class NoArraysFilter extends TokenFilter
    {
        @Override
        public TokenFilter filterStartArray() {
            return null;
        }
    }

    static class NoObjectsFilter extends TokenFilter
    {
        @Override
        public TokenFilter filterStartObject() {
            return null;
        }
    }

    static final TokenFilter INCLUDE_EMPTY_IF_NOT_FILTERED = new TokenFilter() {
        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return !contentsFiltered;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return !contentsFiltered;
        }

        @Override
        public boolean _includeScalar() {
            return false;
        }
    };

    static final TokenFilter INCLUDE_EMPTY = new TokenFilter() {
        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return true;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return true;
        }

        @Override
        public boolean _includeScalar() {
            return false;
        }
    };

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    private final String SIMPLE = a2q(
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
                   Inclusion.ONLY_INCLUDE_ALL,
                   false // multipleMatches
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("3"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("a"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'a':123}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath2() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{\"ob\":{\"value\":3}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testSingleMatchFilteringWithPath3() throws Exception
    {
        String jsonString = a2q("{'a':123,'ob':{'value0':2,'value':3,'value2':4},'array':[1,2],'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("ob"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value0':2,'value':3,'value2':4}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithoutPath1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   Inclusion.ONLY_INCLUDE_ALL,
                   false // multipleMatches -false
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("3"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithoutPath2() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'array':[3,4],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                Inclusion.ONLY_INCLUDE_ALL,
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("2"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'array':[3,4],'ob':{'value':3,'array':[5,6],'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{\"array\":[2]}"), result);
        assertEquals(1, p.getMatchCount());
    }


    public void testNotAllowMultipleMatchesWithPath2() throws Exception
    {
        String jsonString = a2q("{'a':123,'ob':{'value':3,'array':[1,2],'value':{'value0':2}},'array':[3,4]}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{\"ob\":{\"array\":[2]}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath3() throws Exception
    {
        String jsonString = a2q("{'ob':{'value':3,'ob':{'value':2}},'value':\"val\"}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value':3}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testNotAllowMultipleMatchesWithPath4() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value1':1},'ob2':{'ob':{'value2':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("ob"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches -false
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value1':1}}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithoutPath() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   Inclusion.ONLY_INCLUDE_ALL,
                   true // multipleMatches - true
                );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("3 {\"value0\":2} \"val\""), result);
        assertEquals(3, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithPath1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4,'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches - true
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{\"ob\":{\"value\":3,\"value\":{\"value0\":2}},\"value\":\"val\"}"), result);
        assertEquals(3, p.getMatchCount());
    }

    public void testAllowMultipleMatchesWithPath2() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'array':[3,4],'value':{'value0':2}},'value':\"val\",'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new IndexMatchFilter(1),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches - true
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{\"array\":[2],\"ob\":{\"array\":[4]}}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath1() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value0", "value2"),
                Inclusion.INCLUDE_ALL_AND_PATH, true /* multipleMatches */ );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value0':2,'value2':0.25}}"), result);
        assertEquals(2, p.getMatchCount());

    }

    public void testMultipleMatchFilteringWithPath2() throws Exception
    {
        String INPUT = a2q("{'a':123,'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(INPUT);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("b", "value"),
                Inclusion.INCLUDE_ALL_AND_PATH, true);

        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value':3},'b':true}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath3() throws Exception
    {
        final String JSON = a2q("{'root':{'a0':true,'a':{'value':3},'b':{'value':\"foo\"}},'b0':false}");
        JsonParser p0 = JSON_F.createParser(JSON);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'root':{'a':{'value':3},'b':{'value':\"foo\"}}}"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testNoMatchFiltering1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new NameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'array':[],'ob':{}}"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testNoMatchFiltering2() throws Exception
    {
        String object = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        String jsonString = String.format("[%s,%s,%s]", object, object, object);
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new NameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("[{'array':[],'ob':{}},{'array':[],'ob':{}},{'array':[],'ob':{}}]"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testNoMatchFiltering3() throws Exception
    {
        String object = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        String jsonString = String.format("[[%s],[%s],[%s]]", object, object, object);
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new NameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("[[{'array':[],'ob':{}}],[{'array':[],'ob':{}}],[{'array':[],'ob':{}}]]"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testNoMatchFiltering4() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new StrictNameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{}"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testNoMatchFiltering5() throws Exception
    {
        String object = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        String jsonString = String.format("[%s,%s,%s]", object, object, object);
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new StrictNameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("[{},{},{}]"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testNoMatchFiltering6() throws Exception
    {
        String object = a2q("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
        String jsonString = String.format("[[%s],[%s],[%s]]", object, object, object);
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new StrictNameMatchFilter("invalid"),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("[[{}],[{}],[{}]]"), result);
        assertEquals(0, p.getMatchCount());
    }

    public void testValueOmitsFieldName1() throws Exception
    {
        String jsonString = a2q("{'a':123,'array':[1,2]}");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new NoArraysFilter(),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'a':123}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testValueOmitsFieldName2() throws Exception
    {
        String jsonString = a2q("['a',{'value0':3,'b':{'value':4}},123]");
        JsonParser p0 = JSON_F.createParser(jsonString);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
            new NoObjectsFilter(),
            Inclusion.INCLUDE_NON_NULL,
            true // multipleMatches
        );
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("['a',123]"), result);
        assertEquals(2, p.getMatchCount());
    }

    public void testIndexMatchWithPath1() throws Exception
    {
        FilteringParserDelegate p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(1), Inclusion.INCLUDE_ALL_AND_PATH, true);
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'array':[2]}"), result);
        assertEquals(1, p.getMatchCount());

        p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0), Inclusion.INCLUDE_ALL_AND_PATH, true);
        result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'array':[1]}"), result);
        assertEquals(1, p.getMatchCount());
    }

    public void testIndexMatchWithPath2() throws Exception
    {
        FilteringParserDelegate p = new FilteringParserDelegate(JSON_F.createParser(SIMPLE),
                new IndexMatchFilter(0, 1), Inclusion.INCLUDE_ALL_AND_PATH, true);
        assertEquals(a2q("{'array':[1,2]}"), readAndWrite(JSON_F, p));
        assertEquals(2, p.getMatchCount());

        String JSON = a2q("{'a':123,'array':[1,2,3,4,5],'b':[1,2,3]}");
        p = new FilteringParserDelegate(JSON_F.createParser(JSON),
                new IndexMatchFilter(1, 3), Inclusion.INCLUDE_ALL_AND_PATH, true);
        assertEquals(a2q("{'array':[2,4],'b':[2]}"), readAndWrite(JSON_F, p));
        assertEquals(3, p.getMatchCount());
    }

    public void testBasicSingleMatchFilteringWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );

// {'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}
        String result = readAndWrite(JSON_F, p);
        assertEquals(a2q("{'ob':{'value':3}}"), result);
    }

    public void testTokensSingleMatchWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );

        assertFalse(p.hasCurrentToken());
        assertNull(p.getCurrentToken());
        assertEquals(JsonTokenId.ID_NO_TOKEN, p.currentTokenId());
        assertFalse(p.isExpectedStartObjectToken());
        assertFalse(p.isExpectedStartArrayToken());

// {'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}
//      String result = readAndWrite(JSON_F, p);
//      assertEquals(a2q("{'ob':{'value':3}}"), result);

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
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        p.skipChildren();
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextToken());
    }

    public void testIncludeEmptyArrayIfNotFiltered() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'empty_array':[],'filtered_array':[5]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'empty_array':[]}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'empty_array':[],'filtered_array':[5]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'empty_array':[],'filtered_array':[]}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyObjectIfNotFiltered() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'empty_object':{},'filtered_object':{'foo':5}}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'empty_object':{}}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyObject() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'empty_object':{},'filtered_object':{'foo':5}}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'empty_object':{},'filtered_object':{}}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyArrayInObjectIfNotFiltered() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'object_with_empty_array':{'foo':[]},'object_with_filtered_array':{'foo':[5]}}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'object_with_empty_array':{'foo':[]}}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyArrayInObject() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'object_with_empty_array':{'foo':[]},'object_with_filtered_array':{'foo':[5]}}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(
                a2q("{'object_with_empty_array':{'foo':[]},'object_with_filtered_array':{'foo':[]}}"),
                readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyObjectInArrayIfNotFiltered() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'array_with_empty_object':[{}],'array_with_filtered_object':[{'foo':5}]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'array_with_empty_object':[{}]}"), readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyObjectInArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'array_with_empty_object':[{}],'array_with_filtered_object':[{'foo':5}]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(
                a2q("{'array_with_empty_object':[{}],'array_with_filtered_object':[{}]}"),
                readAndWrite(JSON_F, p));
    }

    public void testIncludeEmptyArrayIfNotFilteredAfterFiltered() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "[5, {'empty_array':[],'filtered_array':[5]}]"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("[{'empty_array':[]}]"), readAndWrite(JSON_F, p));
    }

    public void testExcludeObjectAtTheBeginningOfArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'parent':[{'exclude':false},{'include':true}]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter(new String[] { "include" } ),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'parent':[{'include':true}]}"), readAndWrite(JSON_F, p));
    }

    public void testExcludeObjectAtTheEndOfArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'parent':[{'include':true},{'exclude':false}]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter(new String[] { "include" } ),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );
        assertEquals(a2q("{'parent':[{'include':true}]}"), readAndWrite(JSON_F, p));
    }

    public void testExcludeObjectInMiddleOfArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "{'parent':[{'include-1':1},{'skip':0},{'include-2':2}]}"));
        JsonParser p = new FilteringParserDelegate(p0,
                new NameMatchFilter(new String[]{"include-1", "include-2"}),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );
        assertEquals(a2q("{'parent':[{'include-1':1},{'include-2':2}]}"), readAndWrite(JSON_F, p));
    }

    public void testExcludeLastArrayInsideArray() throws Exception {
        JsonParser p0 = JSON_F.createParser(a2q(
                "['skipped', [], ['skipped']]"));
        JsonParser p = new FilteringParserDelegate(p0,
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );
        assertEquals(a2q("[[]]"), readAndWrite(JSON_F, p));
    }
}
