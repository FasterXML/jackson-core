package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonFactory;

public class FastParserNumberParsingTest extends NumberParsingTest
{
    private final JsonFactory fastFactory = JsonFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
