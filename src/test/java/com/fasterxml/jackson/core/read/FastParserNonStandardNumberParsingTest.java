package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class FastParserNonStandardNumberParsingTest extends NonStandardNumberParsingTest {
    private final JsonFactory fastFactory =
            JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                    .enable(JsonReadFeature.USE_FAST_DOUBLE_PARSER)
                    .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
