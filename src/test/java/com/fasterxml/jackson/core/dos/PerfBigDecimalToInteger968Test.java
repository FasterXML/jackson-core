package com.fasterxml.jackson.core.dos;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

// For [core#968]]
public class PerfBigDecimalToInteger968Test
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    // For [core#968]: shouldn't take multiple seconds
    @Test(timeout = 2000)
    public void bigIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                Assert.fail("Should not pass");
            } catch (StreamConstraintsException e) {
                Assert.assertEquals("BigDecimal scale (-25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }

    @Test(timeout = 2000)
    public void tinyIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e-25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                Assert.fail("Should not pass");
            } catch (StreamConstraintsException e) {
                Assert.assertEquals("BigDecimal scale (25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            Assert.fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
