package com.fasterxml.jackson.core;

import java.io.StringWriter;

import com.fasterxml.jackson.core.exc.UnexpectedEndOfInputException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.json.JsonFactory;

public class TestExceptions extends BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();
    
    // For [core#10]
    public void testOriginalMesssage()
    {
        final JsonLocation loc = new JsonLocation(null, -1L, 1, 1);
        StreamReadException exc = new StreamReadException(null, "Foobar", loc);
        String msg = exc.getMessage();
        String orig = exc.getOriginalMessage();
        assertEquals("Foobar", orig);
        assertTrue(msg.length() > orig.length());

        // and another
        StreamReadException exc2 = new StreamReadException("Second",
                loc, exc);
        assertSame(exc, exc2.getCause());
        exc2.clearLocation();
        assertNull(exc2.getLocation());

        // and yet with null
        StreamReadException exc3 = new StreamReadException((JsonParser) null, null, exc);
        assertNull(exc3.getOriginalMessage());
        assertEquals("N/A\n at [No location information]", exc3.getMessage());
        assertTrue(exc3.toString().startsWith(StreamReadException.class.getName()+": N/A"));
    }

    // [core#198]
    public void testAccessToParser() throws Exception
    {
        JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), "{}");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        StreamReadException e = new StreamReadException(p, "Test!");
        assertSame(p, e.processor());
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
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), w);
        g.writeStartObject();
        StreamWriteException e = new StreamWriteException(g, "Test!");
        assertSame(g, e.processor());
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
        } catch (UnexpectedEndOfInputException e) {
            verifyException(e, "close marker for Array");
        }
        p.close();

        p = createParser(mode, "{ \"foo\" : [ ] ");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (UnexpectedEndOfInputException e) {
            verifyException(e, "close marker for Object");
        }
        p.close();

        p = createParser(mode, "{ \"fo");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (UnexpectedEndOfInputException e) {
            
            verifyException(e, "in property name");
            assertEquals(JsonToken.PROPERTY_NAME, e.getTokenBeingDecoded());
        }
        p.close();

        p = createParser(mode, "{ \"field\" : ");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Should get exception");
        } catch (UnexpectedEndOfInputException e) {
            verifyException(e, "unexpected end-of-input");
            verifyException(e, "Object entries");
        }
        p.close();

        // any other cases we'd like to test?
    }
}
