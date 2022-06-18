package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

public class FastDoubleObjectWriteTest extends ObjectWriteTest {
    private final JsonFactory FACTORY = JsonFactory.builder().enable(JsonWriteFeature.USE_FAST_DOUBLE_WRITER).build();

    protected JsonFactory jsonFactory() {
        return FACTORY;
    }
}
