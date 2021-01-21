package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;

public class TrailingCommas616Test extends BaseTest
{
    private final JsonFactory JSON_F_ALLOW_MISSING = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
            .build();

    // [core#616]
    public void testRootLevelComma616()
    {
        _testRootLevel616(MODE_READER);
    }

    public void testRootLevelComma616Bytes()
    {
        _testRootLevel616(MODE_INPUT_STREAM);
        _testRootLevel616(MODE_INPUT_STREAM_THROTTLED);
    }

    public void testRootLevelComma616DataInput()
    {
        _testRootLevel616(MODE_DATA_INPUT);
    }

    private void _testRootLevel616(int mode)
    {
        JsonParser p = createParser(JSON_F_ALLOW_MISSING, mode, ",");
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character (','");
        }
        p.close();
    }
}
