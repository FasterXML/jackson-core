package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

public class LargeNameReadTest extends BaseTest {

    public void testLargeName() throws Exception
    {
        final String doc = generateJSON(1000);
        final JsonFactory jsonFactory = JsonFactory.builder().build();
        try (JsonParser jp = createParserUsingStream(jsonFactory, doc, "UTF-8")) {
            JsonToken jsonToken;
            while ((jsonToken = jp.nextToken()) != null) {

            }
        }
    }

    public void testLargeNameWithSmallLimit() throws Exception
    {
        final String doc = generateJSON(1000);
        final JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
                .build();
        try (JsonParser jp = createParserUsingStream(jsonFactory, doc, "UTF-8")) {
            JsonToken jsonToken;
            while ((jsonToken = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("Name value length"));
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
