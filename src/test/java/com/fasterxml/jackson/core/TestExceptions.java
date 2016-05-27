package com.fasterxml.jackson.core;

import java.io.StringWriter;

import com.fasterxml.jackson.core.io.JsonEOFException;

public class TestExceptions extends BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();
    
    // For [core#10]
    public void testOriginalMesssage()
    {
        JsonProcessingException exc = new JsonParseException(null, "Foobar", JsonLocation.NA);
        String msg = exc.getMessage();
        String orig = exc.getOriginalMessage();
        assertEquals("Foobar", orig);
        assertTrue(msg.length() > orig.length());
    }

    // [core#198]
    public void testAccessToParser() throws Exception
    {
        JsonParser p = JSON_F.createParser("{}");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        JsonParseException e = new JsonParseException(p, "Test!");
        assertSame(p, e.getProcessor());
        assertEquals("Test!", e.getOriginalMessage());
        JsonLocation loc = e.getLocation();
        assertNotNull(loc);
        assertEquals(2, loc.getColumnNr());
        assertEquals(1, loc.getLineNr());
        p.close();
    }

    // [core#198]
    public void testAccessToGenerator() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator g = JSON_F.createGenerator(w);
        g.writeStartObject();
        JsonGenerationException e = new JsonGenerationException("Test!", g);
        assertSame(g, e.getProcessor());
        assertEquals("Test!", e.getOriginalMessage());
        g.close();
    }

    // [core#281]: new eof exception
    public void testEofExceptionsBytes() throws Exception {
        _testEofExceptions(MODE_INPUT_STREAM);
    }

    // [core#281]: new eof exception
    public void testEofExceptionsChars() throws Exception {
        _testEofExceptions(MODE_READER);
    }

    private void _testEofExceptions(int mode) throws Exception
    {
        JsonParser p = createParser(mode, "[ ");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (JsonEOFException e) {
            verifyException(e, "close marker for Array");
        }
        p.close();

        p = createParser(mode, "{ \"foo\" : [ ] ");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (JsonEOFException e) {
            verifyException(e, "close marker for Object");
        }
        p.close();

        p = createParser(mode, "{ \"fo");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (JsonEOFException e) {
            
            verifyException(e, "in field name");
            assertEquals(JsonToken.FIELD_NAME, e.getTokenBeingDecoded());
        }
        p.close();

        p = createParser(mode, "{ \"field\" : ");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (JsonEOFException e) {
            verifyException(e, "unexpected end-of-input");
            verifyException(e, "Object entries");
        }
        p.close();

        // any other cases we'd like to test?
    }
}
