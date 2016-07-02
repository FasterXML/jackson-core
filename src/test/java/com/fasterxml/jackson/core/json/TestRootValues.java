package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestRootValues
    extends com.fasterxml.jackson.core.BaseTest
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

    public void testBrokeanNumber() throws Exception
    {
    	_testBrokeanNumber(false);
    	_testBrokeanNumber(true);
    }

    private void _testBrokeanNumber(boolean useStream) throws Exception
    {
    	JsonFactory f = new JsonFactory();
        final String DOC = "14:89:FD:D3:E7:8C";
        JsonParser p = useStream ?
                createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);
        // Should fail, right away
        try {
        	p.nextToken();
        	fail("Ought to fail! Instead, got token: "+p.currentToken());
        } catch (JsonParseException e) {
        	verifyException(e, "unexpected character");
        }
        p.close();
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

    public void testSimpleWrites() throws Exception
    {
        _testSimpleWrites(false);
        _testSimpleWrites(true);
    }

    public void _testSimpleWrites(boolean useStream) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringWriter w = new StringWriter();
        JsonGenerator gen;

        if (useStream) {
            gen = JSON_F.createGenerator(out, JsonEncoding.UTF8);
        } else {
            gen = JSON_F.createGenerator(w);
        }
        gen.writeNumber(123);
        gen.writeString("abc");
        gen.writeBoolean(true);
        
        gen.close();
        out.close();
        w.close();

        // and verify
        String json = useStream ? out.toString("UTF-8") : w.toString();
        assertEquals("123 \"abc\" true", json);
    }
}
