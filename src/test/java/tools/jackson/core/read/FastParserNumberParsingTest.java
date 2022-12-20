package tools.jackson.core.read;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

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
