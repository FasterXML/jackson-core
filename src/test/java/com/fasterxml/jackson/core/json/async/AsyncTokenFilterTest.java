package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

public class AsyncTokenFilterTest extends AsyncTestBase {
    private final JsonFactory JSON_F = new JsonFactory();

    private final static String INPUT_STRING = "{'a': 1, 'b': [2, {'c': 3}]}".replace('\'', '"');
    private final static byte[] INPUT_BYTES = INPUT_STRING.getBytes();
    private final static TokenFilter TOKEN_FILTER = new TokenFilter() {
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

    public void testFilteredNonBlockingParser() throws IOException {
        // Start by feeding all the bytes at once, since that works fine, and gradually reduce the chunk size.
        for (int chunkSize = INPUT_BYTES.length; chunkSize > 0; chunkSize--) {
            System.out.println("Starting chunkSize " + chunkSize);
            // Create the parsers
            NonBlockingJsonParser nonBlockingParser = (NonBlockingJsonParser) JSON_F.createNonBlockingByteArrayParser();
            FilteringParserDelegate filteredParser = new FilteringParserDelegate(nonBlockingParser, TOKEN_FILTER, true, true);

            int expectedIdx = 0; // tracks EXPECTED_TOKENS seen
            int inputIdx = 0; // tracks INPUT_BYTES fed to the parser

            while (expectedIdx < EXPECTED_TOKENS.length) {
                // grab next token
                JsonToken actual = filteredParser.nextToken();

                if (actual == JsonToken.NOT_AVAILABLE) {
                    // feed it.
                    int chunkLen = Math.min(inputIdx + chunkSize, INPUT_BYTES.length - inputIdx);
                    assertNotEquals(0, chunkLen); // sanity check.
                    nonBlockingParser.feedInput(INPUT_BYTES, inputIdx, inputIdx + chunkLen);
                    inputIdx += chunkLen;
                } else {
                    // make sure it's the right one and mark it as seen.
                    JsonToken expected = EXPECTED_TOKENS[expectedIdx];
                    assertEquals(expected, actual);
                    expectedIdx++;
                }
            }
        }
    }
}
