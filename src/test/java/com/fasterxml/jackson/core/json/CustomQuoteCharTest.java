package com.fasterxml.jackson.core.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// For [core#549], ability to use alternate quote characters
class CustomQuoteCharTest
        extends com.fasterxml.jackson.core.JUnit5TestBase
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
        assertEquals("{'key':'It\\u0027s \\\"fun\\\"'}", w.toString());

        // with Array
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        _writeArray(g, "It's a sin");
        g.close();
        assertEquals("['It\\u0027s a sin']", w.toString());
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
        assertEquals("{'key':'It\\u0027s \\\"fun\\\"'}", out.toString("UTF-8"));

        // with Array
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        _writeArray(g, "It's a sin");
        g.close();
        assertEquals("['It\\u0027s a sin']", out.toString("UTF-8"));
    }

    private void _writeObject(JsonGenerator g, String key, String value) throws Exception {
        g.writeStartObject();
        g.writeStringField(key, value);
        g.writeEndObject();
    }

    private void _writeArray(JsonGenerator g, String value) throws Exception {
        g.writeStartArray();
        g.writeString(value);
        g.writeEndArray();
    }
}
