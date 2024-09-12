package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NonStandardNumberParsing4694Test
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
