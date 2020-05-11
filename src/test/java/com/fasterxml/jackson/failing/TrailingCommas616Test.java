package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class TrailingCommas616Test extends BaseTest
{
    public void testRootLevel616() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .build();
        _testRootLevel616(f, MODE_INPUT_STREAM);
        _testRootLevel616(f, MODE_INPUT_STREAM_THROTTLED);
        _testRootLevel616(f, MODE_READER);
    }

    private void _testRootLevel616(JsonFactory f, int mode) throws Exception
    {
        JsonParser p = createParser(f, mode, ",");
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }
}
