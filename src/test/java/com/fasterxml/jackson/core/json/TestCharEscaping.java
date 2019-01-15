package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestCharEscaping
    extends com.fasterxml.jackson.core.BaseTest
{
    @SuppressWarnings("serial")
    private final static CharacterEscapes ESC_627 = new CharacterEscapes() {
        final int[] ascii = CharacterEscapes.standardAsciiEscapesForJSON();
        {
          ascii['<'] = CharacterEscapes.ESCAPE_STANDARD;
          ascii['>'] = CharacterEscapes.ESCAPE_STANDARD;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
          return ascii;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
          throw new UnsupportedOperationException("Not implemented for test");
        }
    };

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
      */

    public void testMissingEscaping()
        throws Exception
    {
        // Invalid: control chars, including lf, must be escaped
        final String DOC = "["
            +"\"Linefeed: \n.\""
            +"]";
        JsonParser jp = createParserUsingReader(DOC);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        try {
            // This may or may not trigger exception
            JsonToken t = jp.nextToken();
            assertToken(JsonToken.VALUE_STRING, t);
            // and if not, should get it here:
            jp.getText();
            fail("Expected an exception for un-escaped linefeed in string value");
        } catch (JsonParseException jex) {
            verifyException(jex, "has to be escaped");
        }
        jp.close();
    }

    public void testSimpleEscaping()
        throws Exception
    {
        String DOC = "["
            +"\"LF=\\n\""
            +"]";

        JsonParser jp = createParserUsingReader(DOC);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("LF=\n", jp.getText());
        jp.close();


        /* Note: must split Strings, so that javac won't try to handle
         * escape and inline null char
         */
        DOC = "[\"NULL:\\u0000!\"]";

        jp = createParserUsingReader(DOC);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("NULL:\0!", jp.getText());
        jp.close();

        // Then just a single char escaping
        jp = createParserUsingReader("[\"\\u0123\"]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("\u0123", jp.getText());
        jp.close();

        // And then double sequence
        jp = createParserUsingReader("[\"\\u0041\\u0043\"]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("AC", jp.getText());
        jp.close();
    }

    public void testInvalid()
        throws Exception
    {
        // 2-char sequences not allowed:
        String DOC = "[\"\\u41=A\"]";
        JsonParser jp = createParserUsingReader(DOC);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        try {
            jp.nextToken();
            jp.getText();
            fail("Expected an exception for unclosed ARRAY");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "for character escape");
        }
        jp.close();
    }

    /**
     * Test to verify that decoder does not allow 8-digit escapes
     * (non-BMP characters must be escaped using two 4-digit sequences)
     */
    public void test8DigitSequence()
        throws Exception
    {
        String DOC = "[\"\\u00411234\"]";
        JsonParser jp = createParserUsingReader(DOC);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("A1234", jp.getText());
        jp.close();
    }

    public void testWriteLongCustomEscapes() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        jf.setCharacterEscapes(ESC_627); // must set to trigger bug
        StringBuilder longString = new StringBuilder();
        while (longString.length() < 2000) {
          longString.append("\u65e5\u672c\u8a9e");
        }

        StringWriter writer = new StringWriter();
        // must call #createGenerator(Writer), #createGenerator(OutputStream) doesn't trigger bug
        JsonGenerator jgen = jf.createGenerator(writer);
        jgen.setHighestNonEscapedChar(127); // must set to trigger bug
        jgen.writeString(longString.toString());
        jgen.close();
    }

    // [jackson-core#116]
    public void testEscapesForCharArrays() throws Exception {
        JsonFactory jf = new JsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jgen = jf.createGenerator(writer);
        // must call #writeString(char[],int,int) and not #writeString(String)
        jgen.writeString(new char[] { '\0' }, 0, 1);
        jgen.close();
        assertEquals("\"\\u0000\"", writer.toString());
    }

    // [jackson-core#116]
    public void testEscapeNonLatin1Chars() throws Exception {
        _testEscapeNonLatin1ViaChars(false);
    }

    // [jackson-core#116]
    public void testEscapeNonLatin1Bytes() throws Exception {
        _testEscapeNonLatin1ViaChars(true);
    }

    private void _testEscapeNonLatin1ViaChars(boolean useBytes) throws Exception {
        // NOTE! First one is outside latin-1, so escape; second one within, do NOT escape:
        final String VALUE_IN = "Line\u2028feed, \u00D6l!";
        final String VALUE_ESCAPED = "Line\\u2028feed, \u00D6l!";
        final JsonFactory DEFAULT_F = new JsonFactory();

        // First: with default settings, no auto-escaping
        _testEscapeNonLatin1(DEFAULT_F, VALUE_IN, VALUE_IN, useBytes); // char

        // Second: with escaping beyond Latin-1 range
        final JsonFactory latinF = ((JsonFactoryBuilder)JsonFactory.builder())
                .highestNonEscapedChar(255)
                .build();
        _testEscapeNonLatin1(latinF, VALUE_IN, VALUE_ESCAPED, useBytes);
    }

    private void _testEscapeNonLatin1(JsonFactory f, String valueIn, String expEncoded,
            boolean useBytes) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        final JsonGenerator g = useBytes ? f.createGenerator(bytes, JsonEncoding.UTF8)
                : f.createGenerator(sw);
        g.writeStartArray();
        g.writeString(valueIn);
        g.writeEndArray();
        g.close();

        // Don't parse, as we want to verify actual escaping aspects

        final String doc = useBytes ? bytes.toString("UTF-8") : sw.toString();
        assertEquals("[\""+expEncoded+"\"]", doc);
    }
}
