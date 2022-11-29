package tools.jackson.core.read;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

public class FastParserNumberParsingTest extends NumberParsingTest
{
    private final JsonFactory fastFactory = JsonFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            // 28-Nov-2022, tatu: Uses rather long numbers, need to tweak
            .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(5000).build())
            .build();

    @Override
    protected JsonFactory jsonFactory() {
        return fastFactory;
    }
}
