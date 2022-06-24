package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.json.JsonFactory;

public class FastDoubleArrayGenerationTest extends ArrayGenerationTest {
    private final JsonFactory FACTORY = JsonFactory.builder().enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER).build();

    @Override
    protected JsonFactory jsonFactory() {
        return FACTORY;
    }
}
