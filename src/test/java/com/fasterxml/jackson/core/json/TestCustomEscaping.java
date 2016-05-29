package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class TestCustomEscaping extends com.fasterxml.jackson.core.BaseTest
{
    final static int TWO_BYTE_ESCAPED = 0x111;
    final static int THREE_BYTE_ESCAPED = 0x1111;

    final static SerializedString TWO_BYTE_ESCAPED_STRING = new SerializedString("&111;");
    final static SerializedString THREE_BYTE_ESCAPED_STRING = new SerializedString("&1111;");
    
    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    /**
     * Trivial simple custom escape definition set.
     */
    @SuppressWarnings("serial")
    static class MyEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes;

        public MyEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A"
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // too force "\u0062"
            _asciiEscapes['d'] = CharacterEscapes.ESCAPE_CUSTOM;
        }
        
        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch)
        {
            if (ch == 'd') {
                return new SerializedString("[D]");
            }
            if (ch == TWO_BYTE_ESCAPED) {
                return TWO_BYTE_ESCAPED_STRING;
            }
            if (ch == THREE_BYTE_ESCAPED) {
                return THREE_BYTE_ESCAPED_STRING;
            }
            return null;
        }
    }
    
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    /**
     * Test to ensure that it is possible to force escaping
     * of non-ASCII characters.
     * Related to [JACKSON-102]
     */
    public void testAboveAsciiEscapeWithReader() throws Exception
    {
        _testEscapeAboveAscii(false); // reader
    }

    public void testAboveAsciiEscapeWithUTF8Stream() throws Exception
    {
        _testEscapeAboveAscii(true); // stream (utf-8)
    }

    // // // Tests for [JACKSON-106]

    public void testEscapeCustomWithReader() throws Exception
    {
        _testEscapeCustom(false); // reader
    }

    public void testEscapeCustomWithUTF8Stream() throws Exception
    {
        _testEscapeCustom(true); // stream (utf-8)
    }

    public void testJsonpEscapes() throws Exception {
        _testJsonpEscapes(false);
        _testJsonpEscapes(true);
    }

    @SuppressWarnings("resource")
    private void _testJsonpEscapes(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.setCharacterEscapes(JsonpCharacterEscapes.instance());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;

        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        final String VALUE_TEMPLATE = "String with JS 'linefeeds': %s and %s...";
        final String INPUT_VALUE = String.format(VALUE_TEMPLATE, "\u2028", "\u2029");

        g.writeStartArray();
        g.writeString(INPUT_VALUE);
        g.writeEndArray();
        g.close();

        String json = bytes.toString("UTF-8");
        assertEquals(String.format("[%s]",
                quote(String.format(VALUE_TEMPLATE, "\\u2028", "\\u2029"))),
                json);
    }

    /*
    /********************************************************
    /* Secondary test methods
    /********************************************************
     */

    @SuppressWarnings("resource")
    private void _testEscapeAboveAscii(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        final String VALUE = "chars: [\u00A0]/[\u1234]";
        final String KEY = "fun:\u0088:\u3456";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;

        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartArray();
        g.writeString(VALUE);
        g.writeEndArray();
        g.close();
        String json = bytes.toString("UTF-8");
        
        assertEquals("["+quote(VALUE)+"]", json);

        // And then with forced ASCII; first, values

        bytes = new ByteArrayOutputStream();
        if (useStream) {
            g = f.createGenerator(bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        g.writeStartArray();
        g.writeString(VALUE);
        g.writeEndArray();
        g.close();
        json = bytes.toString("UTF-8");
        assertEquals("["+quote("chars: [\\u00A0]/[\\u1234]")+"]", json);

        // and then keys
        bytes = new ByteArrayOutputStream();
        if (useStream) {
            g = f.createGenerator(bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        g.writeStartObject();
        g.writeFieldName(KEY);
        g.writeBoolean(true);
        g.writeEndObject();
        g.close();
        json = bytes.toString("UTF-8");
        assertEquals("{"+quote("fun:\\u0088:\\u3456")+":true}", json);
    }

    @SuppressWarnings("resource")
    private void _testEscapeCustom(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory().setCharacterEscapes(new MyEscapes());
        final String STR_IN = "[abcd/"+((char) TWO_BYTE_ESCAPED)+"/"+((char) THREE_BYTE_ESCAPED)+"]";
        final String STR_OUT = "[\\A\\u0062c[D]/"+TWO_BYTE_ESCAPED_STRING+"/"+THREE_BYTE_ESCAPED_STRING+"]";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;
        
        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartObject();
        g.writeStringField(STR_IN, STR_IN);
        g.writeEndObject();
        g.close();
        String json = bytes.toString("UTF-8");
        assertEquals("{"+quote(STR_OUT)+":"+quote(STR_OUT)+"}", json);
    }
}
