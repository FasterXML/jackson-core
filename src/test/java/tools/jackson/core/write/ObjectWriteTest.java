package tools.jackson.core.write;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamWriteException;

/**
 * Set of basic unit tests for verifying that the Object write methods
 * of {@link JsonGenerator} work as expected.
 */
public class ObjectWriteTest
    extends BaseTest
{
    final TokenStreamFactory JSON_F = newStreamFactory();

    protected TokenStreamFactory jsonFactory() {
        return JSON_F;
    }

    public void testEmptyObjectWrite()
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(ObjectWriteContext.empty(), sw);

        TokenStreamContext ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeStartObject();

        ctxt = gen.streamWriteContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        gen.writeEndObject();

        ctxt = gen.streamWriteContext();
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
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartObject();
        // Mismatch:
        try {
            gen.writeEndArray();
            fail("Expected an exception for mismatched array/object write");
        } catch (StreamWriteException e) {
            verifyException(e, "Current context not Array");
        }
        gen.close();
    }

    public void testSimpleObjectWrite()
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartObject();
        gen.writeName("first");
        gen.writeNumber(-901);
        gen.writeName("sec");
        gen.writeBoolean(false);
        gen.writeName("3rd!"); // JSON field names are just strings, not ids with restrictions
        gen.writeString("yee-haw");
        gen.writeEndObject();
        gen.close();
        String docStr = sw.toString();
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("first", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-901, p.getIntValue());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("sec", p.getText());
        assertEquals(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("3rd!", p.getText());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yee-haw", p.getText());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }

    public void testConvenienceMethods()
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartObject();

        final String TEXT = "\"some\nString!\"";

        gen.writeNullProperty("null");
        gen.writeBooleanProperty("bt", true);
        gen.writeBooleanProperty("bf", false);
        gen.writeNumberProperty("short", (short) -12345);
        gen.writeNumberProperty("int", Integer.MIN_VALUE + 1707);
        gen.writeNumberProperty("long", Integer.MIN_VALUE - 1707L);
        gen.writeNumberProperty("big", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1707)));
        gen.writeNumberProperty("float", 17.07F);
        gen.writeNumberProperty("double", 17.07);
        gen.writeNumberProperty("dec", new BigDecimal("0.1"));

        gen.writeObjectPropertyStart("ob");
        gen.writeStringProperty("str", TEXT);
        gen.writeEndObject();

        gen.writeArrayPropertyStart("arr");
        gen.writeEndArray();

        gen.writeEndObject();
        gen.close();

        String docStr = sw.toString();
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("null", p.getText());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bt", p.getText());
        assertEquals(JsonToken.VALUE_TRUE, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bf", p.getText());
        assertEquals(JsonToken.VALUE_FALSE, p.nextToken());

        //Short parsed as int
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("short", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("long", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("big", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("float", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("double", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        //All floating point types parsed as double
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("dec", p.getText());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("ob", p.getText());
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());

        assertEquals("str", p.getText());
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(TEXT, getAndVerifyText(p));
        assertEquals(JsonToken.END_OBJECT, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("arr", p.getText());
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(null, p.nextToken());
        p.close();
    }

    public void testConvenienceMethodsWithNulls()
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jsonFactory().createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartObject();

        gen.writeStringProperty("str", null);
        gen.writeNumberProperty("big", (BigInteger) null);
        gen.writeNumberProperty("dec", (BigDecimal) null);
        gen.writePOJOProperty("obj", null);
        gen.writeBinaryProperty("bin", new byte[] { 1, 2 });

        gen.writeEndObject();
        gen.close();

        String docStr = sw.toString();
        JsonParser p = createParserUsingReader(docStr);
        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("str", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("big", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("dec", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj", p.currentName());
        assertEquals(JsonToken.VALUE_NULL, p.nextToken());

        assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bin", p.currentName());
        // no native binary indicator in JSON, so:
        assertEquals(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("AQI=", p.getText());

        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
