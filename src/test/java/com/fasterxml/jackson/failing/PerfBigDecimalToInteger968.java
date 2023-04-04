package com.fasterxml.jackson.failing;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.*;

// For [core#968]]
public class PerfBigDecimalToInteger968
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    // For [core#968]: shouldn't take multiple seconds
    @Test(timeout = 2000)
    public void bigIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            BigInteger value = p.getBigIntegerValue();
            Assert.assertNotNull(value);
        }
    }

    @Test(timeout = 2000)
    public void tinyIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e-25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            BigInteger value = p.getBigIntegerValue();
            Assert.assertNotNull(value);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            Assert.fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
