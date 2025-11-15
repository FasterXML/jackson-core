package com.fasterxml.jackson.core.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// for [core#1418]
class ParserFilterEmpty1418Test extends JUnit5TestBase
{
    // Custom TokenFilter that only includes the "one" property
    // and returns true for includeEmptyArray
    static class OnePropertyFilter1418Orig extends TokenFilter {
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

    // And then Filter like it was probably intended
    static class OnePropertyFilter1418Fixed extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            if ("one".equals(name)) {
                return this;
            }
            return null;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return true;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, original
    /**********************************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    // [core#1418]: case #1
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty1() throws Exception
    {
        final String json = "[{\"one\":1},{\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Orig(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertOneObject(p);
        // Second object has no "one" property, should be filtered out
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #2
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty2() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Orig(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertOneObject(p);
        _assertOneObject(p);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #3
    @Test
    void filterArrayWithObjectsEndingWithFilteredProperty3() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2},{\"one\":1}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Orig(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        _assertOneObject(p);
        _assertOneObject(p);
        _assertOneObject(p);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // One additional test, for excluding all properties
    @Test
    void filterWithEmptyArray() throws Exception
    {
        final String json = "[{\"two\":2},{\"three\":3}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Orig(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: []
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }
    
    /*
    /**********************************************************************
    /* Test methods, with corrected "empty Object" filtering
    /**********************************************************************
     */
    
    // [core#1418]: case #1 / corrected
    @Test
    void filterArray1Corrected() throws Exception
    {
        final String json = "[{\"one\":1},{\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Fixed(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertOneObject(p);
        // Second object has no "one" property, should be included as empty
        _assertEmptyObject(p);
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #2 / corrected
    @Test
    void filterArray2Corrected() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Fixed(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertOneObject(p);
        _assertOneObject(p);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#1418]: case #3 / corrected
    @Test
    void filterArray3Corrected() throws Exception
    {
        final String json = "[{\"one\":1},{\"one\":1,\"two\":2},{\"one\":1}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Fixed(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{"one":1},{"one":1},{"one":1}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        _assertOneObject(p);
        _assertOneObject(p);
        _assertOneObject(p);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // Case #4, extra
    @Test
    void filterArray4Corrected() throws Exception
    {
        final String json = "[{\"two\":2},{\"three\":3}]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new OnePropertyFilter1418Fixed(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        // Expected output: [{},{}]
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertEmptyObject(p);
        _assertEmptyObject(p);
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

    private void _assertOneObject(JsonParser p) throws Exception {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("one", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
    }

    private void _assertEmptyObject(JsonParser p) throws Exception {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
    }
}
