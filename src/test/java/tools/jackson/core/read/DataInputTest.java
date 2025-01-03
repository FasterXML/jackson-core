package tools.jackson.core.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/* Additional testing for {@link java.io.DataInput} specific
 * challenges for parsing.
 */
class DataInputTest
    extends JUnit5TestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void eofAfterArray() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "[ 1 ]  ");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    void eofAfterObject() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "{ \"value\" : true }");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    void eofAfterScalar() throws Exception
    {
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "\"foobar\" ");
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foobar", p.getString());
        assertNull(p.nextToken());
        p.close();
    }
}
