package tools.jackson.core.json;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.util.JsonpCharacterEscapes;

public class TestCustomEscaping extends tools.jackson.core.BaseTest
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
        private final SerializedString _customRepl;

        public MyEscapes(String custom) {
            _customRepl = new SerializedString(custom);
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A"
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // to force "\u0062"
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
                return _customRepl;
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
        _testEscapeAboveAscii(false, false); // reader
        _testEscapeAboveAscii(false, true);
    }

    public void testAboveAsciiEscapeWithUTF8Stream() throws Exception
    {
        _testEscapeAboveAscii(true, false); // stream (utf-8)
        _testEscapeAboveAscii(true, true);
    }

    // // // Tests for [JACKSON-106]

    public void testEscapeCustomWithReader() throws Exception
    {
        _testEscapeCustom(false, false, "[x]"); // reader
        _testEscapeCustom(false, true, "[x]");

        // and with longer (above 6 characters)
        _testEscapeCustom(false, false, "[abcde]");
        _testEscapeCustom(false, true, "[12345]");
        _testEscapeCustom(false, false, "[xxyyzz4321]");
        _testEscapeCustom(false, true, "[zzyyxx1234]");
    }

    public void testEscapeCustomWithUTF8Stream() throws Exception
    {
        _testEscapeCustom(true, false, "[x]"); // stream (utf-8)
        _testEscapeCustom(true, true, "[x]");

        // and with longer (above 6 characters)
        _testEscapeCustom(true, false, "[12345]");
        _testEscapeCustom(true, true, "[abcde]");
        _testEscapeCustom(true, false, "[abcdefghiz]");
        _testEscapeCustom(true, true, "[123456789ABCDEF]");
    }

    public void testJsonpEscapes() throws Exception {
        _testJsonpEscapes(false, false);
        _testJsonpEscapes(false, true);
        _testJsonpEscapes(true, false);
        _testJsonpEscapes(true, true);
    }

    @SuppressWarnings("resource")
    private void _testJsonpEscapes(boolean useStream, boolean stringAsChars) throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .characterEscapes(JsonpCharacterEscapes.instance())
                .build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;

        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bytes, "UTF-8"));
        }
        final String VALUE_TEMPLATE = "String with JS 'linefeeds': %s and %s...";
        final String INPUT_VALUE = String.format(VALUE_TEMPLATE, "\u2028", "\u2029");

        g.writeStartArray();
        _writeString(g, INPUT_VALUE, stringAsChars);
        g.writeEndArray();
        g.close();

        String json = bytes.toString("UTF-8");
        assertEquals(String.format("[%s]",
                q(String.format(VALUE_TEMPLATE, "\\u2028", "\\u2029"))),
                json);
    }

    /*
    /********************************************************
    /* Secondary test methods
    /********************************************************
     */

    @SuppressWarnings({ "resource" })
    private void _testEscapeAboveAscii(boolean useStream, boolean stringAsChars) throws Exception
    {
        JsonFactory f = new JsonFactory();
        final String VALUE = "chars: [\u00A0]-[\u1234]";
        final String KEY = "fun:\u0088:\u3456";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;

        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartArray();
        _writeString(g, VALUE, stringAsChars);
        g.writeEndArray();
        g.close();
        String json = bytes.toString("UTF-8");

        assertEquals("["+q(VALUE)+"]", json);

        // And then with forced ASCII; first, values
        f = f.rebuild()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();

        bytes = new ByteArrayOutputStream();
        if (useStream) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(),new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartArray();
        _writeString(g, VALUE+"\\", stringAsChars);
        g.writeEndArray();
        g.close();
        json = bytes.toString("UTF-8");
        assertEquals("["+q("chars: [\\u00A0]-[\\u1234]\\\\")+"]", json);

        // and then keys
        f = f.rebuild()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();
        bytes = new ByteArrayOutputStream();
        if (useStream) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartObject();
        g.writeName(KEY+"\\");
        g.writeBoolean(true);
        g.writeEndObject();
        g.close();
        json = bytes.toString("UTF-8");
        assertEquals("{"+q("fun:\\u0088:\\u3456\\\\")+":true}", json);
    }

    @SuppressWarnings("resource")
    private void _testEscapeCustom(boolean useStream, boolean stringAsChars,
            String customRepl) throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .characterEscapes(new MyEscapes(customRepl))
                .build();
        final String STR_IN = "[abcd/"+((char) TWO_BYTE_ESCAPED)+"/"+((char) THREE_BYTE_ESCAPED)+"]";
        final String STR_OUT = "[\\A\\u0062c"+customRepl+"/"+TWO_BYTE_ESCAPED_STRING+"/"+THREE_BYTE_ESCAPED_STRING+"]";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g;

        // First: output normally; should not add escaping
        if (useStream) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bytes, "UTF-8"));
        }
        g.writeStartObject();
        g.writeName(STR_IN);
        _writeString(g, STR_IN, stringAsChars);
        g.writeEndObject();
        g.close();
        String json = bytes.toString("UTF-8");
        assertEquals("{"+q(STR_OUT)+":"+q(STR_OUT)+"}", json);
    }

    private void _writeString(JsonGenerator g, String str, boolean stringAsChars) throws Exception
    {
        if (stringAsChars) {
            g.writeString(str);
        } else {
            char[] ch = str.toCharArray();
            g.writeString(ch, 0, ch.length);
        }
    }
}
