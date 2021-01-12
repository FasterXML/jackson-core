package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Set of basic unit tests for verifying that the Object write methods
 * of {@link JsonGenerator} work as expected.
 */
public class ObjectWriteTest
    extends BaseTest
{
    final TokenStreamFactory JSON_F = newStreamFactory();

    public void testEmptyObjectWrite()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);

        TokenStreamContext ctxt = gen.getOutputContext();
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
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }

    public void testInvalidObjectWrite()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
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

    public void testSimpleObjectWrite()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
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
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("first", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-901, p.getIntValue());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("sec", p.getText());
        assertEquals(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("3rd!", p.getText());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yee-haw", p.getText());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }

    /**
     * Methods to test functionality added for [JACKSON-26]
     */
    public void testConvenienceMethods()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
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
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("null", p.getText());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bt", p.getText());
        assertEquals(JsonToken.VALUE_TRUE, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bf", p.getText());
        assertEquals(JsonToken.VALUE_FALSE, p.nextToken());

        //Short parsed as int
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("short", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("int", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("long", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("big", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("float", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("double", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("dec", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("ob", p.getText());
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());

        assertEquals("str", p.getText());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(TEXT, getAndVerifyText(p));
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("arr", p.getText());
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }

    /**
     * Tests to cover [JACKSON-164]
     */
    public void testConvenienceMethodsWithNulls()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
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
