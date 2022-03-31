package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

public class FastParserNumberParsingTest extends NumberParsingTest {

    private final JsonFactory fastFactory = new JsonFactory().enable(JsonParser.Feature.USE_FAST_DOUBLE_PARSER);

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
