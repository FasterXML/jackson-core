package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class NumberParsingGetType1433Test
    extends JUnit5TestBase
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
        _verifyGetNumberTypeFail(p);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertNull(p.nextToken());
        _verifyGetNumberTypeFail(p);
        p.close();
        _verifyGetNumberTypeFail(p);

        p = createParser(jsonFactory(), mode, "[123]");
        _verifyGetNumberTypeFail(p);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _verifyGetNumberTypeFail(p);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        _verifyGetNumberTypeFail(p);
        p.close();
        _verifyGetNumberTypeFail(p);
    }

    private void _verifyGetNumberTypeFail(JsonParser p) throws Exception
    {
        try {
            p.getNumberType();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Current token (");
            verifyException(e, ") not numeric, can not use numeric");
        }
    }
    
}
