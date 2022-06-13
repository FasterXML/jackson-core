package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class FastParserNumberParsingTest extends NumberParsingTest {

    private final JsonFactory fastFactory =
            JsonFactory.builder()
                    .enable(JsonReadFeature.USE_FAST_DOUBLE_PARSER)
                    .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
