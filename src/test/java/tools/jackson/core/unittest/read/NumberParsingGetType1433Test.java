package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class NumberParsingGetType1433Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    protected JsonFactory jsonFactory() {
        return sharedStreamFactory();
    }

    @Test
    void getNumberType() throws Exception
    {
        _getNumberType(MODE_INPUT_STREAM);
        _getNumberType(MODE_INPUT_STREAM_THROTTLED);
        _getNumberType(MODE_READER);
        _getNumberType(MODE_DATA_INPUT);
    }

    private void _getNumberType(int mode) throws Exception
    {
        JsonParser p;

        p = createParser(jsonFactory(), mode, " 123 ");
        _verifyGetNumberTypeFail(p, "null");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertNull(p.nextToken());
        _verifyGetNumberTypeFail(p, "null");
        p.close();
        _verifyGetNumberTypeFail(p, "null");

        p = createParser(jsonFactory(), mode, " -9 false ");
        _verifyGetNumberTypeFail(p, "null");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        _verifyGetNumberTypeFail(p, "VALUE_FALSE");
        assertNull(p.nextToken());
        _verifyGetNumberTypeFail(p, "null");
        p.close();
        _verifyGetNumberTypeFail(p, "null");

        p = createParser(jsonFactory(), mode, "[123, true]");
        _verifyGetNumberTypeFail(p, "null");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _verifyGetNumberTypeFail(p, "START_ARRAY");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        _verifyGetNumberTypeFail(p, "VALUE_TRUE");
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        _verifyGetNumberTypeFail(p, "END_ARRAY");
        assertNull(p.nextToken());
        _verifyGetNumberTypeFail(p, "null");
        p.close();
        _verifyGetNumberTypeFail(p, "null");
    }

    private void _verifyGetNumberTypeFail(JsonParser p, String token) throws Exception
    {
        try {
            p.getNumberType();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Current token ("+token+") not numeric, cannot use numeric");
        }
    }
}
