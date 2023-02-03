package com.fasterxml.jackson.core.write;

import java.io.*;
import java.util.Random;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;

public class SerializedStringWriteTest
    extends com.fasterxml.jackson.core.BaseTest
{
    final static String NAME_WITH_QUOTES = "\"name\"";
    final static String NAME_WITH_LATIN1 = "P\u00f6ll\u00f6";

    final static String VALUE_WITH_QUOTES = "\"Value\"";
    final static String VALUE2 = _generateLongName(9000);

    private final JsonFactory JSON_F = new JsonFactory();

    private final SerializedString quotedName = new SerializedString(NAME_WITH_QUOTES);
    private final SerializedString latin1Name = new SerializedString(NAME_WITH_LATIN1);

    public void testSimpleFieldNames() throws Exception
    {
        // First using char-backed generator
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        _writeSimple(gen);
        gen.close();
        String json = sw.toString();
        _verifySimple(JSON_F.createParser(json));

        // then using UTF-8
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gen = JSON_F.createGenerator(out, JsonEncoding.UTF8);
        _writeSimple(gen);
        gen.close();
        byte[] jsonB = out.toByteArray();
        _verifySimple(JSON_F.createParser(jsonB));
    }

    public void testSimpleValues() throws Exception
    {
        // First using char-backed generator
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        _writeSimpleValues(gen);
        gen.close();
        _verifySimpleValues(JSON_F.createParser(new StringReader(sw.toString())));

        // then using UTF-8
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gen = JSON_F.createGenerator(out, JsonEncoding.UTF8);
        _writeSimpleValues(gen);
        gen.close();
        _verifySimpleValues(JSON_F.createParser(new ByteArrayInputStream(out.toByteArray())));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _writeSimple(JsonGenerator gen) throws Exception
    {
        // Let's just write array of 2 objects
        gen.writeStartArray();

        gen.writeStartObject();
        gen.writeFieldName(quotedName);
        gen.writeString("a");
        gen.writeFieldName(latin1Name);
        gen.writeString("b");
        gen.writeEndObject();

        gen.writeStartObject();
        gen.writeFieldName(latin1Name);
        gen.writeString("c");
        gen.writeFieldName(quotedName);
        gen.writeString("d");
        gen.writeEndObject();

        gen.writeEndArray();
    }

    private void _writeSimpleValues(JsonGenerator gen) throws Exception
    {
        // Let's just write an array of 2 objects
        gen.writeStartArray();
        gen.writeStartObject();
        gen.writeFieldName(NAME_WITH_QUOTES);
        gen.writeString(new SerializedString(VALUE_WITH_QUOTES));
        gen.writeFieldName(NAME_WITH_LATIN1);
        gen.writeString(VALUE2);
        gen.writeEndObject();

        gen.writeStartObject();
        gen.writeFieldName(NAME_WITH_LATIN1);
        gen.writeString(VALUE_WITH_QUOTES);
        gen.writeFieldName(NAME_WITH_QUOTES);
        gen.writeString(new SerializedString(VALUE2));
        gen.writeEndObject();

        gen.writeEndArray();
    }

    private void _verifySimple(JsonParser p) throws Exception
    {
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_QUOTES, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_LATIN1, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_LATIN1, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("c", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_QUOTES, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("d", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
    }

    private void _verifySimpleValues(JsonParser p) throws Exception
    {
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_QUOTES, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE_WITH_QUOTES, p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_LATIN1, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE2, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_LATIN1, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE_WITH_QUOTES, p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(NAME_WITH_QUOTES, p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(VALUE2, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
    }

    private static String _generateLongName(int minLen)
    {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random(123);
        while (sb.length() < minLen) {
            int ch = rnd.nextInt(96);
            if (ch < 32) { // ascii (single byte)
                sb.append((char) (48 + ch));
            } else if (ch < 64) { // 2 byte
                sb.append((char) (128 + ch));
            } else { // 3 byte
                sb.append((char) (4000 + ch));
            }
        }
        return sb.toString();
    }
}
