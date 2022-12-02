package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;

public class FastParserNumberParsingTest extends NumberParsingTest
{
    private final JsonFactory fastFactory = JsonFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
            .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
