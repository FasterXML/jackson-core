package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

public class TestParserSymbols
    extends com.fasterxml.jackson.core.BaseTest
{
    // For [Issue#148]
    public void testSymbolsWithNullBytes() throws Exception {
        _testSymbolsWithNull(true);
    }

    // For [Issue#148]
    public void testSymbolsWithNullChars() throws Exception {
        _testSymbolsWithNull(false);
    }
    
    private void _testSymbolsWithNull(boolean useBytes) throws Exception
    {
        final JsonFactory f = new JsonFactory();
        final String INPUT = "{\"\\u0000abc\" : 1, \"abc\" : 2}";
        JsonParser parser = useBytes ? f.createParser(INPUT.getBytes("UTF-8"))
                : f.createParser(INPUT);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("\u0000abc", parser.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(1, parser.getIntValue());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("abc", parser.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(2, parser.getIntValue());
        
        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();
    }
}
