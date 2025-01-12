package tools.jackson.core.fuzz;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.fail;

// For
//
// * [core#1169],
// * https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=61198
class Fuzz61198_1169_NumberParseTest extends JacksonCoreTestBase
{
    // NOTE! Not enough to enable just first, but both it seem
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .build();

    @Test
    void leadingPlusSignMalformedBytes() throws Exception {
        _testLeadingPlusMalformed(JSON_F, MODE_INPUT_STREAM);
        _testLeadingPlusMalformed(JSON_F, MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void leadingPlusSignMalformedReader() throws Exception {
        _testLeadingPlusMalformed(JSON_F, MODE_READER);
        _testLeadingPlusMalformed(JSON_F, MODE_READER_THROTTLED);
    }

    @Test
    void leadingPlusSignMalformedOther() throws Exception {
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
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character ('X' (code 88");
            }
        }
    }
    
}
