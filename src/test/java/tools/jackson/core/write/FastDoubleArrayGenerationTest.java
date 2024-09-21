package tools.jackson.core.write;

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;

public class FastDoubleArrayGenerationTest extends ArrayGenerationTest {
    private final JsonFactory FACTORY = streamFactoryBuilder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            // 17-Sep-2024, tatu: [core#223] change to surrogates, let's use old behavior
            //   for now for simpler testing
            .disable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    @Override
    protected JsonFactory jsonFactory() {
        return FACTORY;
    }
}
