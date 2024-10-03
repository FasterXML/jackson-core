package com.fasterxml.jackson.core.dos;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// For [core#968]]
class PerfBigDecimalToInteger968Test
{
    private final JsonFactory JSON_F = new JsonFactory();

    // For [core#968]: shouldn't take multiple seconds
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    void bigIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                assertEquals("BigDecimal scale (-25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }

    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    void tinyIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e-25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                assertEquals("BigDecimal scale (25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
