package tools.jackson.core.unittest.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Set of basic unit tests for verifying that the Array write methods
 * of {@link JsonGenerator} work as expected.
 */
class ArrayWriteTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void emptyArrayWrite() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);

        TokenStreamContext ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeStartArray();

        ctxt = gen.streamWriteContext();
        assertFalse(ctxt.inRoot());
        assertTrue(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeEndArray();

        ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inRoot(), "Should be in root, was "+ctxt.typeDesc());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(1, ctxt.getEntryCount());
        // Index won't yet move
        assertEquals(0, ctxt.getCurrentIndex());

        gen.close();
        String docStr = sw.toString();
        JsonParser jp = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();

        // Ok, then array with nested empty array
        sw = new StringWriter();
        gen = new JsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartArray();
        gen.writeStartArray();
        gen.writeEndArray();
        gen.writeEndArray();
        gen.close();
        docStr = sw.toString();
        jp = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    @Test
    void invalidArrayWrite() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartArray();
        // Mismatch:
        try {
            gen.writeEndObject();
            fail("Expected an exception for mismatched array/object write");
        } catch (StreamWriteException e) {
            verifyException(e, "Current context not Object");
        }
        gen.close();
    }

    @Test
    void simpleArrayWrite() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartArray();
        gen.writeNumber(13);
        gen.writeBoolean(true);
        gen.writeString("foobar");
        gen.writeEndArray();
        gen.close();
        String docStr = sw.toString();
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(13, p.getIntValue());
        assertEquals(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foobar", p.getString());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }
}
