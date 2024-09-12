package tools.jackson.failing;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JUnit5TestBase;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NumberParsing4694Test
        extends JUnit5TestBase
{
    // https://github.com/FasterXML/jackson-databind/issues/4694
    @Test
    void databind4694() throws Exception {
        final String str = "-11000.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        final BigDecimal expected = new BigDecimal(str);
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(mode, String.format(" %s ", str))) {
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(expected, p.getDecimalValue());
                assertFalse(p.isNaN());
            }
        }
    }
}
