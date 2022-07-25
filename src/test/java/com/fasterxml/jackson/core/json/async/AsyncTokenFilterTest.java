package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

// [core#462], [core#463]
public class AsyncTokenFilterTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final static String INPUT_STRING = a2q("{'a': 1, 'b': [2, {'c': 3}]}");
    private final static byte[] INPUT_BYTES = utf8Bytes(INPUT_STRING);
    private final static TokenFilter TOKEN_FILTER = new TokenFilter() {
        @Override
        public TokenFilter includeProperty(String name) {
            return name == "a" ? TokenFilter.INCLUDE_ALL : null;
        }
    };

    private final static JsonToken[] EXPECTED_TOKENS = new JsonToken[]{
        JsonToken.START_OBJECT,
        JsonToken.FIELD_NAME,
        JsonToken.VALUE_NUMBER_INT,
        JsonToken.END_OBJECT
    };

    // Passes if (but only if) all content is actually available
    public void testFilteredNonBlockingParserAllContent() throws IOException
    {
        NonBlockingJsonParser nonBlockingParser = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser();
        assertNotNull(nonBlockingParser.getNonBlockingInputFeeder());

        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nonBlockingParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true);
        nonBlockingParser.feedInput(INPUT_BYTES, 0, INPUT_BYTES.length);
        int expectedIdx = 0;
        while (expectedIdx < EXPECTED_TOKENS.length) {
            // grab next token
            JsonToken actual = filteredParser.nextToken();

            // make sure it's the right one and mark it as seen.
            assertToken(EXPECTED_TOKENS[expectedIdx], actual);
            expectedIdx++;
        }

        filteredParser.close();
        nonBlockingParser.close();
    }

    public void testFilteredNonBlockingByteBufferParserAllContent() throws IOException
    {
        NonBlockingByteBufferJsonParser nonBlockingParser =
                (NonBlockingByteBufferJsonParser) JSON_F.createNonBlockingByteBufferParser();
        assertNotNull(nonBlockingParser.getNonBlockingInputFeeder());
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nonBlockingParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true);
        ByteBuffer byteBuffer = ByteBuffer.wrap(INPUT_BYTES);
        nonBlockingParser.feedInput(byteBuffer);
        int expectedIdx = 0;
        while (expectedIdx < EXPECTED_TOKENS.length) {
            // grab next token
            JsonToken actual = filteredParser.nextToken();

            // make sure it's the right one and mark it as seen.
            assertToken(EXPECTED_TOKENS[expectedIdx], actual);
            expectedIdx++;
        }

        filteredParser.close();
        nonBlockingParser.close();
    }

    public void testSkipChildrenFailOnSplit() throws IOException
    {
        NonBlockingJsonParser nbParser = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser();
        @SuppressWarnings("resource")
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true);
        nbParser.feedInput(INPUT_BYTES, 0, 5);

        assertToken(JsonToken.START_OBJECT, nbParser.nextToken());
        try {
            nbParser.skipChildren();
            fail("Should not pass!");
        } catch (JsonParseException e) {
            verifyException(e, "not enough content available");
            verifyException(e, "skipChildren()");
        }
        nbParser.close();
        filteredParser.close();
    }

    public void testSkipChildrenFailOnSplitByteBuffer() throws IOException
    {
        NonBlockingByteBufferJsonParser nbParser =
                (NonBlockingByteBufferJsonParser) JSON_F.createNonBlockingByteBufferParser();
        @SuppressWarnings("resource")
        FilteringParserDelegate filteredParser = new FilteringParserDelegate(nbParser,
                TOKEN_FILTER, Inclusion.INCLUDE_ALL_AND_PATH, true);
        ByteBuffer byteBuffer = ByteBuffer.wrap(INPUT_BYTES, 0, 5);
        nbParser.feedInput(byteBuffer);

        assertToken(JsonToken.START_OBJECT, nbParser.nextToken());
        try {
            nbParser.skipChildren();
            fail("Should not pass!");
        } catch (JsonParseException e) {
            verifyException(e, "not enough content available");
            verifyException(e, "skipChildren()");
        }
        nbParser.close();
        filteredParser.close();
    }
}
