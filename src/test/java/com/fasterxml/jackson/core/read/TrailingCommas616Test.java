package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.fail;

public class TrailingCommas616Test extends TestBase
{
    private final JsonFactory JSON_F_ALLOW_MISSING = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
            .build();

    // [core#616]
    @Test
    public void testRootLevelComma616() throws Exception
    {
        _testRootLevel616(MODE_READER);
    }

    @Test
    public void testRootLevelComma616Bytes() throws Exception
    {
        _testRootLevel616(MODE_INPUT_STREAM);
        _testRootLevel616(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    public void testRootLevelComma616DataInput() throws Exception
    {
        _testRootLevel616(MODE_DATA_INPUT);
    }

    private void _testRootLevel616(int mode) throws Exception
    {
        JsonParser p = createParser(JSON_F_ALLOW_MISSING, mode, ",");
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character (','");
        }
        p.close();
    }
}
