package tools.jackson.core.unittest.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.util.MinimalPrettyPrinter;

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

    // SerializableString names/values must also escape the custom quote char
    @Test
    void aposQuotingWithSerializableString() throws Exception
    {
        final String EXP = "{'it\\'s':'x\\', \\'admin\\': \\'true'}";
        final ObjectWriteContext ppCtxt = new ObjectWriteContext.Base() {
            @Override
            public PrettyPrinter getPrettyPrinter() { return new MinimalPrettyPrinter(); }
        };
        for (ObjectWriteContext ctxt : new ObjectWriteContext[] { ObjectWriteContext.empty(), ppCtxt }) {
            StringWriter w = new StringWriter();
            try (JsonGenerator g = JSON_F.createGenerator(ctxt, w)) {
                _writeSerializableObject(g, "it's", "x', 'admin': 'true");
            }
            assertEquals(EXP, w.toString());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator g = JSON_F.createGenerator(ctxt, out)) {
                _writeSerializableObject(g, "it's", "x', 'admin': 'true");
            }
            assertEquals(EXP, out.toString("UTF-8"));
        }
    }

    // Same with unquoted property names: escaping must match the String overloads
    @Test
    void aposQuotingWithSerializableStringUnquotedNames() throws Exception
    {
        final JsonFactory f = streamFactoryBuilder()
                .quoteChar('\'')
                .disable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .build();
        final String EXP = "{it\\'s:'x\\', \\'admin\\': \\'true'}";

        StringWriter w = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), w)) {
            _writeSerializableObject(g, "it's", "x', 'admin': 'true");
        }
        assertEquals(EXP, w.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), out)) {
            _writeSerializableObject(g, "it's", "x', 'admin': 'true");
        }
        assertEquals(EXP, out.toString("UTF-8"));

        // and verify String overloads produce identical output
        w = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), w)) {
            g.writeStartObject();
            g.writeName("it's");
            g.writeString("x', 'admin': 'true");
            g.writeEndObject();
        }
        assertEquals(EXP, w.toString());
    }

    private void _writeSerializableObject(JsonGenerator g, String key, String value) throws Exception {
        g.writeStartObject();
        g.writeName(new SerializedString(key));
        g.writeString(new SerializedString(value));
        g.writeEndObject();
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
