package com.fasterxml.jackson.core.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// for [core#1418]
class ParserFilterEmpty1418Test extends JUnit5TestBase
{
    // Custom TokenFilter that only includes the "one" property
    // and returns true for includeEmptyArray
    static class OnePropertyFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            if ("one".equals(name)) {
                return this;
            }
            return null;
        }

        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return true;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    // [core#1418]: case #1: failing
    @JacksonTestFailureExpected
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty1() throws Exception
    {
        final String json = "[{\"one\":1},{\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        // Second object has no "one" property, should be empty object
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #2: passing
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty2() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        // Second object has "one" property
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #3: passing
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty3() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2},{\"one\":1}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        // First object
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        // Second object
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        // Third object
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
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
