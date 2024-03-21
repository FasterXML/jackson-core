package tools.jackson.core.dos;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;

import static org.assertj.core.api.Assertions.assertThat;
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

        try (JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                assertThat(e.getMessage())
                    .startsWith("BigDecimal scale (-25000000) magnitude exceeds the maximum allowed (100000)");
            }
        }
    }

    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    void tinyIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e-25000000";

        try (JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                assertThat(e.getMessage())
                    .startsWith("BigDecimal scale (25000000) magnitude exceeds the maximum allowed (100000)");
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
