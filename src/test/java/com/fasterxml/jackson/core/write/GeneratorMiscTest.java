package com.fasterxml.jackson.core.write;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.core.*;

/**
 * Set of basic unit tests for verifying basic generator
 * features.
 */
@SuppressWarnings("resource")
public class GeneratorMiscTest
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
            JsonGenerator jg = stream ?
                    JSON_F.createGenerator(new StringWriter())
                : JSON_F.createGenerator(new ByteArrayOutputStream(), JsonEncoding.UTF8)
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

    // Also, "very simple" objects are supported even without Codec:
    public void testSimpleWriteObject() throws IOException
    {
        // note: NOT mapping factory, for this test
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        gen.writeStartArray();

        // simple wrappers first
        gen.writeObject(1);
        gen.writeObject((short) -2);
        gen.writeObject((long) 3);
        gen.writeObject((byte) -4);
        gen.writeObject(0.25);
        gen.writeObject(-0.125f);
        gen.writeObject(Boolean.TRUE);
        gen.close();
        String act = sw.toString().trim();
        assertEquals("[1,-2,3,-4,0.25,-0.125,true]", act);

        // then other basic types
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeStartArray();
        gen.writeObject(BigInteger.valueOf(1234));
        gen.writeObject(new BigDecimal(0.5));
        gen.writeEndArray();
        gen.close();
        act = sw.toString().trim();
        assertEquals("[1234,0.5]", act);

        // then Atomic types
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeStartArray();
        gen.writeObject(new AtomicBoolean(false));
        gen.writeObject(new AtomicInteger(13));
        gen.writeObject(new AtomicLong(-127L));
        gen.writeEndArray();
        gen.close();
        act = sw.toString().trim();
        assertEquals("[false,13,-127]", act);
    }

    /*
    /**********************************************************
    /* Tests for raw output
    /**********************************************************
     */

    public void testRaw() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
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
        JsonGenerator gen = JSON_F.createGenerator(sw);
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

        switch (mode) {
        case 0:
            g = jf.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
            break;
        case 1:
            g = jf.createGenerator(bout, JsonEncoding.UTF8);
            break;
        case 2:
            {
                DataOutputStream dout = new DataOutputStream(bout);
                g = jf.createGenerator((DataOutput) dout);
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
        JsonParser jp = jf.createParser(json);
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
                assertEquals(name, jp.getCurrentName());
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
        g = JSON_F.createGenerator(sw);
        g.writeEmbeddedObject(null);
        g.close();
        assertEquals("null", sw.toString());

        ByteArrayOutputStream bytes =  new ByteArrayOutputStream(100);
        g = JSON_F.createGenerator(bytes);
        g.writeEmbeddedObject(null);
        g.close();
        assertEquals("null", utf8String(bytes));

        // also, for fun, try illegal unknown thingy

        try {
            g = JSON_F.createGenerator(bytes);
            // try writing a Class object
            g.writeEmbeddedObject(getClass());
            fail("Expected an exception");
            g.close(); // never gets here
        } catch (JsonGenerationException e) {
            verifyException(e, "No native support for");
        }
    }
}
