package com.fasterxml.jackson.core.dos;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.*;

// For [core#967]
public class PerfBigDecimalParser967Test
{
    private final JsonFactory JSON_F = new JsonFactory();

    // For [core#967]: shouldn't take multiple seconds
    @Test(timeout = 3000)
    public void bigDecimalFromString() throws Exception {
        // Jackson's BigDecimalParser seems to be slower than JDK's;
        // won't fail if using latter.
        StringBuilder sb = new StringBuilder(900);
        for (int i = 0; i < 500; ++i) {
            sb.append('1');
        }
        sb.append("1e10000000");
        final String DOC = sb.toString();

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            Assert.assertNotNull(p.getDecimalValue());
        }
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            Assert.fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
