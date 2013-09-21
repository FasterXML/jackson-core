package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TestRootValueParsing
    extends com.fasterxml.jackson.test.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testSimpleNumbers() throws Exception
    {
        _testSimpleNumbers(false);
        _testSimpleNumbers(true);
    }

    private void _testSimpleNumbers(boolean useStream) throws Exception
    {
        final String DOC = "1 2\t3\r4\n5\r\n6\r\n   7";
        JsonParser jp = useStream ?
                createParserUsingStream(JSON_F, DOC, "UTF-8")
                : createParserUsingReader(JSON_F, DOC);
        for (int i = 1; i <= 7; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(i, jp.getIntValue());
        }
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testSimpleBooleans() throws Exception
    {
        _testSimpleBooleans(false);
        _testSimpleBooleans(true);
    }

    private void _testSimpleBooleans(boolean useStream) throws Exception
    {
        final String DOC = "true false\ttrue\rfalse\ntrue\r\nfalse\r\n   true";
        JsonParser jp = useStream ?
                createParserUsingStream(JSON_F, DOC, "UTF-8")
                : createParserUsingReader(JSON_F, DOC);
        boolean exp = true;
        for (int i = 1; i <= 7; ++i) {
            assertToken(exp ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, jp.nextToken());
            exp = !exp;
        }
        assertNull(jp.nextToken());
        jp.close();
    }
}
