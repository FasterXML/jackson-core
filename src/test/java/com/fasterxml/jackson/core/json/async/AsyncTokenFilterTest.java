package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.json.JsonFactory;

// [core#462], [core#463]
public class AsyncTokenFilterTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final static String INPUT_STRING = aposToQuotes("{'a': 1, 'b': [2, {'c': 3}]}");
    private final static byte[] INPUT_BYTES = utf8Bytes(INPUT_STRING);
    private final static TokenFilter TOKEN_FILTER = new TokenFilter() {
        @Override
        public TokenFilter includeProperty(String name) {
            return name.equals("a") ? TokenFilter.INCLUDE_ALL : null;
        }
    };

    // Passes if (but only if) all content is actually available
    public void testFilteredNonBlockingParserAllContent() throws IOException
    {
        NonBlockingJsonParser nbParser = _nonBlockingParser();
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, true, true);
        nbParser.feedInput(INPUT_BYTES, 0, INPUT_BYTES.length);

        assertToken(JsonToken.START_OBJECT, filteredParser.nextToken());
        assertToken(JsonToken.FIELD_NAME, filteredParser.nextToken());
        assertEquals("a", nbParser.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, filteredParser.nextToken());
        assertEquals(1, nbParser.getIntValue());
        assertToken(JsonToken.END_OBJECT, filteredParser.nextToken());

        filteredParser.close();
        nbParser.close();
    }

    public void testSkipChildrenFailOnSplit() throws IOException
    {
        NonBlockingJsonParser nbParser = _nonBlockingParser();
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, true, true);
        nbParser.feedInput(INPUT_BYTES, 0, 5);

        assertToken(JsonToken.START_OBJECT, nbParser.nextToken());
        try {
            nbParser.skipChildren();
            fail("Should not pass!");
        } catch (JsonParseException e) {
            verifyException(e, "not enough content available");
            verifyException(e, "skipChildren()");
        }
        filteredParser.close();
        nbParser.close();
    }

    private NonBlockingJsonParser _nonBlockingParser() throws IOException {
        return (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty());
    }
}
