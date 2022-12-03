package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class FastParserNonStandardNumberParsingTest
    extends NonStandardNumberParsingTest
{
    private final JsonFactory fastFactory =
            JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                    .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                    .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                    .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
                    .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
                    .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
