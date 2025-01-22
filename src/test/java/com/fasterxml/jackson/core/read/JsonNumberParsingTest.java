package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TokenStreamFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonNumberParsingTest extends JUnit5TestBase
{
    // [jackson-databind#4917]
    @Test
    public void bigDecimal4917() throws Exception
    {
        final String json = a2q("{'decimalHolder':100.00,'number':50}");
        TokenStreamFactory jsonF = newStreamFactory();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(jsonF, MODE_READER, json)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertEquals("decimalHolder", p.currentName());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(new BigDecimal("100.00"), p.getDecimalValue());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertEquals("number", p.currentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(50.0, p.getDoubleValue());
                assertEquals(50, p.getIntValue());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }
}
