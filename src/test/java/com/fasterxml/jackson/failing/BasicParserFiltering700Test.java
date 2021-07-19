package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

@SuppressWarnings("resource")
public class BasicParserFiltering700Test extends BaseTest
{
    static class NoTypeFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            if ("@type".equals(name)) {
                return null;
            }
            return this;
        }

        @Override
        protected boolean _includeScalar() {
            return true;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    // [core#700], simplified
    public void testSkippingRootLevel() throws Exception
    {
        final String json = a2q("{'@type':'yyy','value':12}");
        // should become: {"value":12}
        JsonParser p0 = JSON_F.createParser(json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(12, p.getIntValue());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    // [core#700], medium test
    public void testSkippingOneNested() throws Exception
    {
        final String json = a2q("{'value':{'@type':'yyy','a':12}}");
        // should become: {"value":{"a":12}}
        JsonParser p0 = JSON_F.createParser(json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.currentName());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(12, p.getIntValue());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    // [core#700], full test
/*
    public void testSkippingForSingleWithPath() throws Exception
    {
        final String json = a2q(
 //               "{'@type':'xxx','value':{'@type':'yyy','a':12}}");
    "{'value':{'@type':'yyy','a':12}}");
        // should become: {"value":{"a":12}}

        JsonParser p0 = JSON_F.createParser(json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

//        String filtered = readAndWrite(JSON_F, p);
//        System.out.println("->\n"+filtered);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.currentName());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(12, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(34, p.getIntValue());
        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());

        assertEquals(JsonToken.END_OBJECT, p.getCurrentToken());
        assertNull(p.nextToken());

        p.close();
    }
    */
}
