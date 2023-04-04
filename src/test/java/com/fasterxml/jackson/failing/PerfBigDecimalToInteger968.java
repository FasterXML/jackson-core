package com.fasterxml.jackson.failing;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.*;

// For [core#968]]
public class PerfBigDecimalToInteger968
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    // For [core#968]: shouldn't take multiple seconds
    @Test(timeout = 3000)
    public void bigIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e20000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            Assert.assertNotNull(p.getBigIntegerValue());
        }
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            Assert.fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
