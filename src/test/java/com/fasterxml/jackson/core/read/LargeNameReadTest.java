package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LargeNameReadTest extends BaseTest {

    public void testLargeName() throws Exception
    {
        final String doc = generateJSON(1000);
        final JsonFactory jsonFactory = JsonFactory.builder().build();
        try (JsonParser jp = createParserUsingStream(jsonFactory, doc, "UTF-8")) {
            consumeTokens(jp);
        }
    }

    public void testLargeNameWithSmallLimit() throws Exception
    {
        final String doc = generateJSON(1000);
        final JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
                .build();
        try (JsonParser jp = createParserUsingStream(jsonFactory, doc, "UTF-8")) {
            consumeTokens(jp);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("Name value length"));
        }
    }

    public void testAsyncLargeNameWithSmallLimit() throws Exception
    {
        final byte[] doc = generateJSON(1000).getBytes(StandardCharsets.UTF_8);
        final JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
                .build();

        try (NonBlockingJsonParser jp = (NonBlockingJsonParser) jsonFactory.createNonBlockingByteArrayParser()) {
            jp.feedInput(doc, 0, doc.length);
            consumeTokens(jp);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("Name value length"));
        }
    }

    public void testReaderLargeNameWithSmallLimit() throws Exception
    {
        final String doc = generateJSON(1000);
        final JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
                .build();
        try (JsonParser jp = createParserUsingReader(jsonFactory, doc)) {
            consumeTokens(jp);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("Name value length"));
        }
    }

    private void consumeTokens(JsonParser jp) throws IOException {
        JsonToken jsonToken;
        while ((jsonToken = jp.nextToken()) != null) {

        }
    }

    private String generateJSON(final int nameLen) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"");
        for (int i = 0; i < nameLen; i++) {
            sb.append("a");
        }
        sb.append("\":\"value\"}");
        return sb.toString();
    }
}
