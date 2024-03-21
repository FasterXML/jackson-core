package tools.jackson.core.dos;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

// For [core#967]
class PerfBigDecimalParser967Test
{
    private final JsonFactory JSON_F = new JsonFactory();

    // For [core#967]: shouldn't take multiple seconds
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS)
    void bigDecimalFromString() throws Exception {
        // Jackson's BigDecimalParser seems to be slower than JDK's;
        // won't fail if using latter.
        StringBuilder sb = new StringBuilder(900);
        for (int i = 0; i < 500; ++i) {
            sb.append('1');
        }
        sb.append("1e10000000");
        final String DOC = sb.toString();

        try (JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertNotNull(p.getDecimalValue());
        }
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
