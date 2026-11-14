package tools.jackson.core.unittest.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// For [core#549], ability to use alternate quote characters
class CustomQuoteCharTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    final JsonFactory JSON_F = streamFactoryBuilder()
            .quoteChar('\'')
            .build();

    // Only ASCII range supported as of 2.10
    @Test
    void invalidQuote() throws Exception
    {
        try {
            streamFactoryBuilder()
                .quoteChar('\u00A0');
            fail("Should not allow quote character outside ASCII range");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Can only use Unicode characters up to 0x7F");
        }
    }

    @Test
    void basicAposWithCharBased() throws Exception
    {
        StringWriter w;
        JsonGenerator g;

        // with Object
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        _writeObject(g, "question", "answer");
        g.close();
        assertEquals("{'question':'answer'}", w.toString());

        // with Array
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        _writeArray(g, "hello world");
        g.close();
        assertEquals("['hello world']", w.toString());
    }

    @Test
    void basicAposWithByteBased() throws Exception
    {
        ByteArrayOutputStream out;
        JsonGenerator g;

        // with Object
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        _writeObject(g, "question", "answer");
        g.close();
        assertEquals("{'question':'answer'}", out.toString("UTF-8"));

        // with Array
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        _writeArray(g, "hello world");
        g.close();
        assertEquals("['hello world']", out.toString("UTF-8"));
    }

    @Test
    void aposQuotingWithCharBased() throws Exception
    {
        StringWriter w;
        JsonGenerator g;

        // with Object
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        _writeObject(g, "key", "It's \"fun\"");
        g.close();
        // should escape apostrophes but not quotes?
        assertEquals("{'key':'It\\'s \\\"fun\\\"'}", w.toString());

        // with Array
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        _writeArray(g, "It's a sin");
        g.close();
        assertEquals("['It\\'s a sin']", w.toString());
    }

    @Test
    void aposQuotingWithByteBased() throws Exception
    {
        ByteArrayOutputStream out;
        JsonGenerator g;

        // with Object
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        _writeObject(g, "key", "It's \"fun\"");
        g.close();
        // should escape apostrophes but not quotes?
        assertEquals("{'key':'It\\'s \\\"fun\\\"'}", out.toString("UTF-8"));

        // with Array
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        _writeArray(g, "It's a sin");
        g.close();
        assertEquals("['It\\'s a sin']", out.toString("UTF-8"));
    }

    // Custom CharacterEscapes must not drop escaping of the quote character
    @Test
    void aposQuotingWithCustomEscapes() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .quoteChar('\'')
                .characterEscapes(new HtmlEscapes())
                .build();

        StringWriter w = new StringWriter();
        JsonGenerator g = createGenerator(f, w);
        _writeObject(g, "it's", "x', 'admin': 'true");
        g.close();
        assertEquals("{'it\\'s':'x\\', \\'admin\\': \\'true'}", w.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        g = createGenerator(f, out);
        _writeObject(g, "it's", "x', 'admin': 'true");
        g.close();
        assertEquals("{'it\\'s':'x\\', \\'admin\\': \\'true'}", out.toString("UTF-8"));
    }

    @SuppressWarnings("serial")
    static class HtmlEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes;

        HtmlEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['<'] = CharacterEscapes.ESCAPE_STANDARD;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return null;
        }
    }

    private void _writeObject(JsonGenerator g, String key, String value) throws Exception {
        g.writeStartObject();
        g.writeStringProperty(key, value);
        g.writeEndObject();
    }

    private void _writeArray(JsonGenerator g, String value) throws Exception {
        g.writeStartArray();
        g.writeString(value);
        g.writeEndArray();
    }
}
