package com.fasterxml.jackson.core.write;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Set of basic unit tests for verifying that the Object write methods
 * of {@link JsonGenerator} work as expected.
 */
class ObjectWriteTest
        extends JUnit5TestBase
{
    private final JsonFactory FACTORY = new JsonFactory();

    protected JsonFactory jsonFactory() {
        return FACTORY;
    }

    @Test
    void emptyObjectWrite()
            throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(sw);

        JsonStreamContext ctxt = gen.getOutputContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeStartObject();

        ctxt = gen.getOutputContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeEndObject();

        ctxt = gen.getOutputContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(1, ctxt.getEntryCount());
        // Index won't yet move
        assertEquals(0, ctxt.getCurrentIndex());

        gen.close();

        String docStr = sw.toString();
        JsonParser jp = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    @Test
    void invalidObjectWrite()
            throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(sw);
        gen.writeStartObject();
        // Mismatch:
        try {
            gen.writeEndArray();
            fail("Expected an exception for mismatched array/object write");
        } catch (JsonGenerationException e) {
            verifyException(e, "Current context not Array");
        }
        gen.close();
    }

    @Test
    void simpleObjectWrite()
            throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(sw);
        gen.writeStartObject();
        gen.writeFieldName("first");
        gen.writeNumber(-901);
        gen.writeFieldName("sec");
        gen.writeBoolean(false);
        gen.writeFieldName("3rd!"); // JSON field names are just strings, not ids with restrictions
        gen.writeString("yee-haw");
        gen.writeEndObject();
        gen.close();
        String docStr = sw.toString();
        JsonParser jp = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("first", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-901, jp.getIntValue());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("sec", jp.getText());
        assertEquals(JsonToken.VALUE_FALSE, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("3rd!", jp.getText());
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("yee-haw", jp.getText());
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    /**
     * Methods to test functionality added for [JACKSON-26]
     */
    @Test
    void convenienceMethods()
            throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(sw);
        gen.writeStartObject();

        final String TEXT = "\"some\nString!\"";

        gen.writeNullField("null");
        gen.writeBooleanField("bt", true);
        gen.writeBooleanField("bf", false);
        gen.writeNumberField("short", (short) -12345);
        gen.writeNumberField("int", Integer.MIN_VALUE + 1707);
        gen.writeNumberField("long", Integer.MIN_VALUE - 1707L);
        gen.writeNumberField("big", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1707)));
        gen.writeNumberField("float", 17.07F);
        gen.writeNumberField("double", 17.07);
        gen.writeNumberField("dec", new BigDecimal("0.1"));

        gen.writeObjectFieldStart("ob");
        gen.writeStringField("str", TEXT);
        gen.writeEndObject();

        gen.writeArrayFieldStart("arr");
        gen.writeEndArray();

        gen.writeEndObject();
        gen.close();

        String docStr = sw.toString();
        JsonParser jp = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("null", jp.getText());
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("bt", jp.getText());
        assertEquals(JsonToken.VALUE_TRUE, jp.nextToken());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("bf", jp.getText());
        assertEquals(JsonToken.VALUE_FALSE, jp.nextToken());

        //Short parsed as int
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("short", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(JsonParser.NumberType.INT, jp.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("int", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(JsonParser.NumberType.INT, jp.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("long", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("big", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("float", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, jp.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("double", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, jp.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("dec", jp.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, jp.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("ob", jp.getText());
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());

        assertEquals("str", jp.getText());
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(TEXT, getAndVerifyText(jp));
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("arr", jp.getText());
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());

        assertEquals(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    /**
     * Tests to cover [JACKSON-164]
     */
    @Test
    void convenienceMethodsWithNulls()
            throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(sw);
        gen.writeStartObject();

        gen.writeStringField("str", null);
        gen.writeNumberField("big", (BigInteger) null);
        gen.writeNumberField("dec", (BigDecimal) null);
        gen.writeObjectField("obj", null);
        gen.writeBinaryField("bin", new byte[] { 1, 2 });

        gen.writeEndObject();
        gen.close();

        String docStr = sw.toString();
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("str", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());

        assertEquals("big", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("dec", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("obj", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bin", p.currentName());
        // no native binary indicator in JSON, so:
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("AQI=", p.getText());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
