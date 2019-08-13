package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

// For [core#549], ability to use alternate quote characters
public class CustomQuoteCharTest
    extends com.fasterxml.jackson.core.BaseTest
{
    final JsonFactory JSON_F = streamFactoryBuilder()
            .quoteChar('\'')
            .build();

    public void testBasicAposWithCharBased() throws Exception
    {
        StringWriter w;
        JsonGenerator g;

        // with Object
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        g.writeStartObject();
        g.writeStringField("question", "answer");
        g.writeEndObject();
        g.close();
        assertEquals("{'question':'answer'}", w.toString());

        // with Array
        w = new StringWriter();
        g = createGenerator(JSON_F, w);
        g.writeStartArray();
        g.writeString("hello world");
        g.writeEndArray();
        g.close();
        assertEquals("['hello world']", w.toString());
    }

    public void testBasicAposWithByteBased() throws Exception
    {
        ByteArrayOutputStream out;
        JsonGenerator g;

        // with Object
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        g.writeStartObject();
        g.writeStringField("question", "answer");
        g.writeEndObject();
        g.close();
        assertEquals("{'question':'answer'}", out.toString("UTF-8"));

        // with Array
        out = new ByteArrayOutputStream();
        g = createGenerator(JSON_F, out);
        g.writeStartArray();
        g.writeString("hello world");
        g.writeEndArray();
        g.close();
        assertEquals("['hello world']", out.toString("UTF-8"));
    }
}
