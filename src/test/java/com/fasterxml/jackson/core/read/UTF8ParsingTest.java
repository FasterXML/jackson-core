package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TokenStreamFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UTF8ParsingTest extends JUnit5TestBase
{
    private TokenStreamFactory JSON_F = newStreamFactory();

    final String testValue = createTestString();
    final String INPUT_JSON = a2q("{ 'value': '" + testValue +"' }");

    // [jackson-core#1397]
    @Test
    public void issue1397() throws Exception
    {
        for (int mode : ALL_MODES) {
            testIssue(JSON_F, mode, INPUT_JSON);
        }
    }
    
    private void testIssue(final TokenStreamFactory jsonF,
                           final int mode,
                           final String json) throws Exception
    {
        try (JsonParser p = createParser(jsonF, mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(testValue, p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private static String createTestString() {
        StringBuilder sb = new StringBuilder(4001);
        for (int i = 0; i < 4000; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        return sb.toString();
    }
}
