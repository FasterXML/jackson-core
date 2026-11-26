package tools.jackson.core.unittest.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;

import static org.junit.jupiter.api.Assertions.*;

// [core#1827]: Custom CharacterEscapes must not drop escaping of quote char
// (or forward slash, if ESCAPE_FORWARD_SLASHES enabled)
class CustomEscapesQuoteChar1827Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
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

    @Test
    void aposQuoting() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .quoteChar('\'')
                .characterEscapes(new HtmlEscapes())
                .build();
        _verifyAllOutputs(f, "it's", "x', 'admin': 'true",
                "{'it\\'s':'x\\', \\'admin\\': \\'true'}");
    }

    // Quote chars without "natural" escape get standard escape
    @Test
    void backtickQuoting() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .quoteChar('`')
                .characterEscapes(new HtmlEscapes())
                .build();
        _verifyAllOutputs(f, "a`b", "x`y",
                "{`a\\u0060b`:`x\\u0060y`}");
    }

    @Test
    void escapeForwardSlash() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .characterEscapes(new HtmlEscapes())
                .build();
        _verifyAllOutputs(f, "url", "http://example.com",
                "{\"url\":\"http:\\/\\/example.com\"}");
    }

    @Test
    void dontEscapeForwardSlash() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .characterEscapes(new HtmlEscapes())
                .build();
        _verifyAllOutputs(f, "url", "http://example.com",
                "{\"url\":\"http://example.com\"}");
    }

    // Derived escape table is only copied if needed, and then cached
    @Test
    void derivedEscapesCached()
    {
        final CharacterEscapes esc = new HtmlEscapes();
        final int[] base = esc.getEscapeCodesForAscii();

        // No custom escapes: standard tables
        assertSame(CharTypes.get7BitOutputEscapes('"', true),
                CharTypes.get7BitOutputEscapes(null, '"', true));

        // Already escapes '"', no slash: no copy needed
        assertSame(base, CharTypes.get7BitOutputEscapes(esc, '"', false));

        // Slash escaping needed: copy, then reused
        int[] withSlash = CharTypes.get7BitOutputEscapes(esc, '"', true);
        assertNotSame(base, withSlash);
        assertEquals('/', withSlash['/']);
        assertEquals(0, base['/']);
        assertSame(withSlash, CharTypes.get7BitOutputEscapes(esc, '"', true));

        // Different settings: new table
        int[] apos = CharTypes.get7BitOutputEscapes(esc, '\'', false);
        assertNotSame(withSlash, apos);
        assertEquals('\'', apos['\'']);
        assertEquals(0, apos['/']);
        assertSame(apos, CharTypes.get7BitOutputEscapes(esc, '\'', false));
    }

    // Verify output via Writer, OutputStream and DataOutput backed generators
    private void _verifyAllOutputs(JsonFactory f, String key, String value, String exp)
        throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = createGenerator(f, w)) {
            _writeObject(g, key, value);
        }
        assertEquals(exp, w.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator g = createGenerator(f, out)) {
            _writeObject(g, key, value);
        }
        assertEquals(exp, out.toString("UTF-8"));

        out = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(),
                (DataOutput) new DataOutputStream(out))) {
            _writeObject(g, key, value);
        }
        assertEquals(exp, out.toString("UTF-8"));
    }

    private void _writeObject(JsonGenerator g, String key, String value) throws Exception {
        g.writeStartObject();
        g.writeStringProperty(key, value);
        g.writeEndObject();
    }
}
