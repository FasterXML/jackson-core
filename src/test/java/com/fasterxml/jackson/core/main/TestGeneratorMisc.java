package com.fasterxml.jackson.core.main;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;

/**
 * Set of basic unit tests for verifying basic generator
 * features.
 */
@SuppressWarnings("resource")
public class TestGeneratorMisc
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    /*
    /**********************************************************
    /* Tests for closing, status
    /**********************************************************
     */

    public void testIsClosed() throws IOException
    {
        for (int i = 0; i < 2; ++i) {
            boolean stream = ((i & 1) == 0);
            ObjectWriteContext writeCtxt = ObjectWriteContext.empty();
            JsonGenerator jg = stream ?
                    JSON_F.createGenerator(writeCtxt, new StringWriter())
                : JSON_F.createGenerator(writeCtxt, new ByteArrayOutputStream(), JsonEncoding.UTF8)
                ;
            assertFalse(jg.isClosed());
            jg.writeStartArray();
            jg.writeNumber(-1);
            jg.writeEndArray();
            assertFalse(jg.isClosed());
            jg.close();
            assertTrue(jg.isClosed());
            jg.close();
            assertTrue(jg.isClosed());
        }
    }

    /*
    /**********************************************************
    /* Tests for raw output
    /**********************************************************
     */

    public void testRaw() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartArray();
        gen.writeRaw("-123, true");
        gen.writeRaw(", \"x\"  ");
        gen.writeEndArray();
        gen.close();

                
        JsonParser jp = createParserUsingReader(sw.toString());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-123, jp.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("x", jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    public void testRawValue() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartArray();
        gen.writeRawValue("7");
        gen.writeRawValue("[ null ]");
        gen.writeRawValue("false");
        gen.writeEndArray();
        gen.close();

        JsonParser jp = createParserUsingReader(sw.toString());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(7, jp.getIntValue());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    /*
    /**********************************************************
    /* Tests for object writing
    /**********************************************************
     */

    /**
     * Unit test that tries to trigger buffer-boundary conditions
     */
    public void testLongerObjects() throws Exception
    {
        _testLongerObjects(JSON_F, 0);
        _testLongerObjects(JSON_F, 1);
        _testLongerObjects(JSON_F, 2);
    }

    public void _testLongerObjects(JsonFactory jf, int mode) throws Exception
    {
        JsonGenerator g;
        ByteArrayOutputStream bout = new ByteArrayOutputStream(200);
        final ObjectWriteContext writeCtxt = ObjectWriteContext.empty();

        switch (mode) {
        case 0:
            g = jf.createGenerator(writeCtxt, new OutputStreamWriter(bout, "UTF-8"));
            break;
        case 1:
            g = jf.createGenerator(writeCtxt, bout, JsonEncoding.UTF8);
            break;
        case 2:
            {
                DataOutputStream dout = new DataOutputStream(bout);
                g = jf.createGenerator(writeCtxt, (DataOutput) dout);
            }
        
            break;
        default:
            fail("Unknown mode "+mode);
            g = null;
        }

        g.writeStartObject();

        for (int rounds = 0; rounds < 1500; ++rounds) {
            for (int letter = 'a'; letter <= 'z'; ++letter) {
                for (int index = 0; index < 20; ++index) {
                    String name;
                    if (letter > 'f') {
                        name = "X"+letter+index;
                    } else if (letter > 'p') {
                        name = ""+letter+index;
                    } else {
                        name = "__"+index+letter;
                    }
                    g.writeFieldName(name);
                    g.writeNumber(index-1);
                }
                g.writeRaw('\n');
            }
        }
        g.writeEndObject();
        g.close();

        byte[] json = bout.toByteArray();
        JsonParser jp = jf.createParser(ObjectReadContext.empty(), json);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        for (int rounds = 0; rounds < 1500; ++rounds) {
        for (int letter = 'a'; letter <= 'z'; ++letter) {
            for (int index = 0; index < 20; ++index) {
                assertToken(JsonToken.FIELD_NAME, jp.nextToken());
                String name;
                if (letter > 'f') {
                    name = "X"+letter+index;
                } else if (letter > 'p') {
                    name = ""+letter+index;
                } else {
                    name = "__"+index+letter;
                }
                assertEquals(name, jp.currentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(index-1, jp.getIntValue());
            }
        }
        }
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }

    /*
    /**********************************************************
    /* Tests, other
    /**********************************************************
     */

    // NOTE: test for binary data under `base64/` tests
    public void testAsEmbedded() throws Exception
    {
        JsonGenerator g;

        StringWriter sw = new StringWriter();
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeEmbeddedObject(null);
        g.close();
        assertEquals("null", sw.toString());

        ByteArrayOutputStream bytes =  new ByteArrayOutputStream(100);
        g = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
        g.writeEmbeddedObject(null);
        g.close();
        assertEquals("null", bytes.toString("UTF-8"));

        // also, for fun, try illegal unknown thingy

        try {
            g = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
            // try writing a Class object
            g.writeEmbeddedObject(getClass());
            fail("Expected an exception");
            g.close(); // never gets here
        } catch (JsonGenerationException e) {
            verifyException(e, "No native support for");
        }
    }
}
