package tools.jackson.core.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JUnit5TestBase;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NextNameParserTest
    extends JUnit5TestBase
{
    @Test
    void basicNextNameWithReader() throws Exception
    {
        _testBasicNextName(MODE_READER);
    }

    @Test
    void basicNextNameWithStream() throws Exception
    {
        _testBasicNextName(MODE_INPUT_STREAM);
        _testBasicNextName(MODE_INPUT_STREAM_THROTTLED);
    }

    @Test
    void basicNextNameWithDataInput() throws Exception
    {
        _testBasicNextName(MODE_DATA_INPUT);
    }

    private void _testBasicNextName(int mode) throws Exception
    {
        final String DOC = a2q(
"{ 'data' : { 'primary' : -15, 'vector' : [ 'yes', false ], 'misc' : null, 'name' : 'Bob'  },\n"
+"  'array' : [ true,   {'message':'hello', 'value' : 42, 'misc' : [1, 2] }, null, 0.25 ]\n"
+"}");

        JsonParser p = createParser(mode, DOC);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("data", p.currentName());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertEquals("primary", p.nextName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-15, p.getIntValue());

        assertEquals("vector", p.nextName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yes", p.getString());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertEquals("misc", p.nextName());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals("name", p.nextName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Bob", p.getString());

        assertNull(p.nextName());
        assertToken(JsonToken.END_OBJECT, p.currentToken());

        assertEquals("array", p.nextName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("message", p.nextName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("hello", p.getString());
        assertEquals("value", p.nextName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        assertEquals("misc", p.nextName());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        assertNull(p.nextName());
        assertToken(JsonToken.END_ARRAY, p.currentToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        p.close();
    }
}
