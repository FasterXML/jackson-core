package tools.jackson.core.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.filter.TokenFilter.Inclusion;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNull;

// for [core#708]
class ParserFilterEmpty708Test extends JUnit5TestBase
{
    // Could actually just return basic TokenFilter but...
    static class IncludeAllFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            return this;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final JsonFactory JSON_F = newStreamFactory();

    // [core#708]
    @JacksonTestFailureExpected
    @Test
    void emptyArray() throws Exception
    {
        final String json = "[ ]";
        // should become: {"value":12}
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
    @JacksonTestFailureExpected
    @Test
    void emptyObject() throws Exception
    {
        final String json = "{ }";
        // should become: {"value":12}
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
        return f.createParser(ObjectReadContext.empty(), json);
    }
}
