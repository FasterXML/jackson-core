package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

// Test(s) to see that limited amount of recovery is possible over
// content: specifically, most single-character problems.
class ParserErrorRecovery1173Test
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void recoverNumberBytes() throws Exception {
        _testRecoverNumber(MODE_INPUT_STREAM);
        _testRecoverNumber(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void recoverNumberDataInput() throws Exception {
        _testRecoverNumber(MODE_DATA_INPUT);
    }

    @Test
    void recoverNumberChars() throws Exception {
        _testRecoverNumber(MODE_READER);
        _testRecoverNumber(MODE_READER_THROTTLED);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testRecoverNumber(int mode) throws Exception
    {
        try (JsonParser p = createParser(JSON_F, mode, "1\n[ , ]\n3 ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                JsonToken t = p.nextToken();
                fail("Should have gotten an exception; instead got token: "+t);
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character (','");
            }

            // But should essentially "skip" problematic character
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(3, p.getIntValue());
            assertNull(p.nextToken());
        }
    }
}
