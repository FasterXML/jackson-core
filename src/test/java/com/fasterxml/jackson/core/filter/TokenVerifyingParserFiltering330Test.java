package com.fasterxml.jackson.core.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;

// Tests for [core#330]
public class TokenVerifyingParserFiltering330Test extends BaseTest
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    private final String SIMPLE = aposToQuotes("{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}");
    
    @SuppressWarnings("resource")
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
    
    @SuppressWarnings("resource")
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
        assertEquals(JsonTokenId.ID_NO_TOKEN, p.getCurrentTokenId());
        assertFalse(p.isExpectedStartObjectToken());
        assertFalse(p.isExpectedStartArrayToken());
        
// {'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}
//      String result = readAndWrite(JSON_F, p);
//      assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.START_OBJECT, p.getCurrentToken());
        assertTrue(p.isExpectedStartObjectToken());
        assertFalse(p.isExpectedStartArrayToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.getCurrentToken());
        assertEquals("ob", p.getCurrentName());
//        assertEquals("ob", p.getText());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("ob", p.getCurrentName());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.getCurrentName());
        assertEquals("value", p.getText());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.getCurrentToken());
        assertEquals(NumberType.INT, p.getNumberType());
        assertEquals(3, p.getIntValue());
        assertEquals("value", p.getCurrentName());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());

        p.clearCurrentToken();
        assertNull(p.getCurrentToken());
        
        p.close();
    }

    @SuppressWarnings("resource")
    public void testSkippingForSingleWithPath() throws Exception
    {
        JsonParser p0 = JSON_F.createParser(SIMPLE);
        JsonParser p = new FilteringParserDelegate(p0,
               new NameMatchFilter("value"),
                   true, // includePath
                   false // multipleMatches
                );

//        assertEquals(aposToQuotes("{'ob':{'value':3}}"), result);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        p.skipChildren();
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextToken());
    }
}
