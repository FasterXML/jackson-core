package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

@SuppressWarnings("resource")
public class ParserFiltering700Test extends BaseTest
{
    static class NoTypeFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            if ("@type".equals(name)) {
                return null;
            }
            return this;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, [core#700]
    /**********************************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    // [core#700], simplified
    public void testSkippingRootLevel() throws Exception
    {
        final String json = a2q("{'@type':'yyy','value':12}");
        // should become: {"value":12}
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.currentName());
        // 19-Jul-2021, tatu: while not ideal, existing contract is that "getText()"
        //    ought to return property name as well...
        assertEquals("value", p.getText());

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
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("value", p.currentName());
        // as earlier, this needs to hold true too
        assertEquals("value", p.getText());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertEquals("a", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(12, p.getIntValue());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    // [core#700], full test
    public void testSkippingForSingleWithPath() throws Exception
    {
        _testSkippingForSingleWithPath(false);
        _testSkippingForSingleWithPath(true);
    }

    private void _testSkippingForSingleWithPath(boolean useNextName) throws Exception
    {
        final String json = a2q("{'@type':'xxx','value':{'@type':'yyy','a':99}}");
        // should become: {"value":{"a":99}}

        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new NoTypeFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertTrue(p.isExpectedStartObjectToken());

        if (useNextName) {
            assertEquals("value", p.nextFieldName());
            // as earlier, this needs to hold true too
            assertEquals("value", p.getText());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertTrue(p.isExpectedStartObjectToken());
            assertEquals("a", p.nextFieldName());
            assertEquals("a", p.getText());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(99, p.getIntValue());
            assertNull(p.nextFieldName());
            assertEquals(JsonToken.END_OBJECT, p.currentToken());
        } else {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertEquals("value", p.getText());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertTrue(p.isExpectedStartObjectToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("a", p.currentName());
            assertEquals("a", p.getText());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(99, p.getIntValue());
            assertEquals(JsonToken.END_OBJECT, p.nextToken());
        }

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private JsonParser _createParser(TokenStreamFactory f, String json) throws Exception {
        return f.createParser(json);
    }
}
