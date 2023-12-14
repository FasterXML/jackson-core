package com.fasterxml.jackson.core.fuzz;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

// For
//
// * [core#1169],
// * https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=61198
public class Fuzz61198_1169_NumberParseTest extends BaseTest
{
    // NOTE! Not enough to enable just first, but both it seem
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    public void testLeadingPlusSignMalformedBytes() throws Exception {
        _testLeadingPlusMalformed(JSON_F, MODE_INPUT_STREAM);
        _testLeadingPlusMalformed(JSON_F, MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLeadingPlusSignMalformedReader() throws Exception {
        _testLeadingPlusMalformed(JSON_F, MODE_READER);
        _testLeadingPlusMalformed(JSON_F, MODE_READER_THROTTLED);
    }

    public void testLeadingPlusSignMalformedOther() throws Exception {
        _testLeadingPlusMalformed(JSON_F, MODE_DATA_INPUT);
    }

    private void _testLeadingPlusMalformed(JsonFactory f, int mode) throws Exception
    {
        // But also, invalid case:
        try (JsonParser p = createParser(f, mode, "[ +X  1 ")) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                JsonToken t = p.nextToken();
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                // Either one works:
//                p.getNumberType();
                p.getIntValue();
                fail("Should not pass, got: "+t);
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('X' (code 88");
            }
        }
    }
    
}
