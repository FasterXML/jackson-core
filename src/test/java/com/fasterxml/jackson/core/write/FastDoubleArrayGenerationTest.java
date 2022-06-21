package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamWriteFeature;

public class FastDoubleArrayGenerationTest extends ArrayGenerationTest {
    private final JsonFactory FACTORY = JsonFactory.builder().enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER).build();

    protected JsonFactory jsonFactory() {
        return FACTORY;
    }
}
