package com.fasterxml.jackson.core.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

import static org.junit.jupiter.api.Assertions.assertNull;

// for [core#708]
class ParserFilterEmpty708Test extends JUnit5TestBase
{
    // Could actually just return basic TokenFilter but...
    static class IncludeAllFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            return TokenFilter.INCLUDE_ALL;
        }

        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return true;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return true;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    // [core#708]
    @Test
    void emptyArray() throws Exception
    {
        final String json = "[ ]";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new IncludeAllFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // [core#708]
    @Test
    void emptyObject() throws Exception
    {
        final String json = "{ }";
        JsonParser p0 = _createParser(JSON_F, json);
        JsonParser p = new FilteringParserDelegate(p0,
                new IncludeAllFilter(),
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
        );

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
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
