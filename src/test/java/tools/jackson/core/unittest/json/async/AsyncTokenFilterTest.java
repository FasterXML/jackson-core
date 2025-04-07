package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.JsonPointerBasedFilter;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.filter.TokenFilter.Inclusion;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.async.NonBlockingByteArrayJsonParser;
import tools.jackson.core.unittest.async.AsyncTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [core#462], [core#463]
public class AsyncTokenFilterTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    private final static String INPUT_STRING = a2q("{'a': 1, 'b': [2, {'c': 3}]}");
    private final static byte[] INPUT_BYTES = utf8Bytes(INPUT_STRING);
    private final static TokenFilter TOKEN_FILTER = new TokenFilter() {
        @Override
        public TokenFilter includeProperty(String name) {
            return name.equals("a") ? TokenFilter.INCLUDE_ALL : null;
        }
    };

    @Test
    void filteredNonBlockingParserNotExplicitlyAllowed() throws Exception {
        NonBlockingByteArrayJsonParser nbParser = _nonBlockingParser();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new FilteringParserDelegate(nbParser, TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true)
        );

        verifyException(exception, "NonBlockingByteArrayJsonParser");
        verifyException(exception, "(canParseAsync() == true)");
    }

    @Test
    void filteringNonBlockingParserWithoutInputFed() throws Exception
    {
        NonBlockingByteArrayJsonParser nbParser = _nonBlockingParser();
        try (JsonParser filteringParser = new FilteringParserDelegate(nbParser,
                new JsonPointerBasedFilter("/second"),
                TokenFilter.Inclusion.ONLY_INCLUDE_ALL, false, true)) {
            StreamReadException e = assertThrows(StreamReadException.class, filteringParser::nextToken);
            verifyException(e, "JsonToken.NOT_AVAILABLE");
        }
    }

    // Passes if (but only if) all content is actually available
    @Test
    public void testFilteredNonBlockingParserAllContent()
    {
        NonBlockingByteArrayJsonParser nbParser = _nonBlockingParser();
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true, true);
        nbParser.feedInput(INPUT_BYTES, 0, INPUT_BYTES.length);

        assertToken(JsonToken.START_OBJECT, filteredParser.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, filteredParser.nextToken());
        assertEquals("a", nbParser.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, filteredParser.nextToken());
        assertEquals(1, nbParser.getIntValue());
        assertToken(JsonToken.END_OBJECT, filteredParser.nextToken());

        filteredParser.close();
        nbParser.close();
    }

    @Test
    public void testSkipChildrenFailOnSplit()
    {
        NonBlockingByteArrayJsonParser nbParser = _nonBlockingParser();
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true, true);
        nbParser.feedInput(INPUT_BYTES, 0, 5);

        assertToken(JsonToken.START_OBJECT, nbParser.nextToken());
        try {
            nbParser.skipChildren();
            fail("Should not pass!");
        } catch (StreamReadException e) {
            verifyException(e, "not enough content available");
            verifyException(e, "skipChildren()");
        }
        filteredParser.close();
        nbParser.close();
    }

    private NonBlockingByteArrayJsonParser _nonBlockingParser() {
        return (NonBlockingByteArrayJsonParser) JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty());
    }
}
