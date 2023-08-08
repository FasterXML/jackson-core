package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class LargeNameReadTest extends BaseTest {

    public void testLargeName() throws Exception
    {
        final String doc = generateJSON(1000);
        try (JsonParser jp = createParserUsingStream(doc, "UTF-8")) {
            JsonToken jsonToken;
            while ((jsonToken = jp.nextToken()) != null) {

            }
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
