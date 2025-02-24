package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UTF8ParsingTest
    extends JacksonCoreTestBase
{
    private TokenStreamFactory JSON_F = newStreamFactory();

    final String testValue = createTestString();
    final String INPUT_JSON = a2q("{ 'value': '" + testValue + "' }");

    // https://github.com/FasterXML/jackson-dataformats-text/issues/497
    @Test
    public void utf8Char3Bytes() throws Exception
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
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(testValue, p.getString());
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
