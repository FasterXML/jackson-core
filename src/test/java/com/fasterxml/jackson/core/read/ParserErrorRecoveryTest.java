package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

// Test(s) to see that limited amount of recovery is possible over
// content: specifically, most single-character problems.
public class ParserErrorRecoveryTest
    extends JUnit5TestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    public void testRecoverNumberBytes() throws Exception {
        _testRecoverNumber(MODE_INPUT_STREAM);
        _testRecoverNumber(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    public void testRecoverNumberDataInput() throws Exception {
        _testRecoverNumber(MODE_DATA_INPUT);
    }

    @Test
    public void testRecoverNumberChars() throws Exception {
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
        try (JsonParser p = createParser(JSON_F, mode, "1\n[ , ]\n3")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                JsonToken t = p.nextToken();
                fail("Should have gotten an exception; instead got token: "+t);
            } catch (JsonParseException e) {
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
