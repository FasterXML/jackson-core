package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class ValueConversionsTest
    extends JacksonCoreTestBase
{
    @Test
    void asInt() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testAsInt(mode);
        }
    }

    private void _testAsInt(int mode) throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17\", \"foo\" ]";
        JsonParser p = createParser(mode, input);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(0, p.getValueAsLong());
        assertEquals(9, p.getValueAsLong(9));

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getValueAsLong());
        assertEquals(1, p.getValueAsLong(-99));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-3, p.getValueAsLong());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(4, p.getValueAsLong());
        assertEquals(4, p.getValueAsLong(99));
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(1, p.getValueAsLong());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(0, p.getValueAsLong());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals(0, p.getValueAsLong());
        assertEquals(0, p.getValueAsLong(27));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(-17, p.getValueAsLong());
        assertEquals(-17, p.getValueAsLong(3));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(0, p.getValueAsLong());
        assertEquals(9, p.getValueAsLong(9));

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(0, p.getValueAsLong());
        assertEquals(9, p.getValueAsLong(9));

        p.close();
    }

    @Test
    void asBoolean() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testAsBoolean(mode);
        }
    }

    private void _testAsBoolean(int mode) throws Exception
    {
        final String input = "[ true, false, null, 1, 0, \"true\", \"false\", \"foo\" ]";
        JsonParser p = createParser(mode, input);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertFalse(p.getValueAsBoolean());
        assertTrue(p.getValueAsBoolean(true));

        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertTrue(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertFalse(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertFalse(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertTrue(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertFalse(p.getValueAsBoolean());

        assertToken(JsonToken.VALUE_STRING, p.nextToken()); // "true"
        assertTrue(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertFalse(p.getValueAsBoolean());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertFalse(p.getValueAsBoolean());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertFalse(p.getValueAsBoolean());
        assertTrue(p.getValueAsBoolean(true));

        p.close();
    }

    @Test
    void asLong() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testAsLong(mode);
        }
    }

    public void _testAsLong(int mode) throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17\", \"foo\" ]";
        JsonParser p = createParser(mode, input);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(0L, p.getValueAsLong());
        assertEquals(9L, p.getValueAsLong(9L));

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1L, p.getValueAsLong());
        assertEquals(1L, p.getValueAsLong(-99L));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-3L, p.getValueAsLong());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(4L, p.getValueAsLong());
        assertEquals(4L, p.getValueAsLong(99L));
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(1L, p.getValueAsLong());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(0L, p.getValueAsLong());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals(0L, p.getValueAsLong());
        assertEquals(0L, p.getValueAsLong(27L));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(-17L, p.getValueAsLong());
        assertEquals(-17L, p.getValueAsLong(3L));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(0L, p.getValueAsLong());
        assertEquals(9L, p.getValueAsLong(9L));

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(0L, p.getValueAsLong());
        assertEquals(9L, p.getValueAsLong(9L));

        p.close();
    }

    @Test
    void asDouble() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testAsDouble(mode);
        }
    }

    private void _testAsDouble(int mode) throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17.25\", \"foo\" ]";
        JsonParser p = createParser(mode, input);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(0.0, p.getValueAsDouble());
        assertEquals(9.0, p.getValueAsDouble(9.0));

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1., p.getValueAsDouble());
        assertEquals(1., p.getValueAsDouble(-99.0));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-3., p.getValueAsDouble());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(4.98, p.getValueAsDouble());
        assertEquals(4.98, p.getValueAsDouble(12.5));
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(1.0, p.getValueAsDouble());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(0.0, p.getValueAsDouble());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals(0.0, p.getValueAsDouble());
        assertEquals(0.0, p.getValueAsDouble(27.8));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(-17.25, p.getValueAsDouble());
        assertEquals(-17.25, p.getValueAsDouble(1.9));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(0.0, p.getValueAsDouble());
        assertEquals(1.25, p.getValueAsDouble(1.25));

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(0.0, p.getValueAsDouble());
        assertEquals(7.5, p.getValueAsDouble(7.5));

        p.close();
    }

}
