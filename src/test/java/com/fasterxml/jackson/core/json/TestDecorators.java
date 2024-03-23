package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.core.io.OutputDecorator;
import com.fasterxml.jackson.core.util.JsonGeneratorDecorator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify that input and output decorators work as
 * expected
 *
 * @since 1.8
 */
@SuppressWarnings("serial")
class TestDecorators extends com.fasterxml.jackson.core.JUnit5TestBase
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class SimpleInputDecorator extends InputDecorator
    {
        @Override
        public InputStream decorate(IOContext ctxt, InputStream in)
            throws IOException
        {
            return new ByteArrayInputStream("123".getBytes("UTF-8"));
        }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length)
            throws IOException
        {
            return new ByteArrayInputStream("456".getBytes("UTF-8"));
        }

        @Override
        public Reader decorate(IOContext ctxt, Reader src) {
            return new StringReader("789");
        }
    }

    static class SimpleOutputDecorator extends OutputDecorator
    {
        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) throws IOException
        {
            out.write("123".getBytes("UTF-8"));
            out.flush();
            return new ByteArrayOutputStream();
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) throws IOException
        {
            w.write("567");
            w.flush();
            return new StringWriter();
        }
    }

    static class SimpleGeneratorDecorator implements JsonGeneratorDecorator
    {
        @Override
        public JsonGenerator decorate(JsonFactory factory, JsonGenerator generator) {
            return new TextHider(generator);
        }

        static class TextHider extends JsonGeneratorDelegate {
            public TextHider(JsonGenerator g) {
                super(g);
            }

            @Override
            public void writeString(String text) throws IOException {
                delegate.writeString("***");
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests: input/output decoration
    /**********************************************************
     */

    @Test
    void inputDecoration() throws IOException
    {
        JsonFactory f = JsonFactory.builder()
                .inputDecorator(new SimpleInputDecorator())
                .build();
        JsonParser p;
        // first test with Reader
        p = f.createParser(new StringReader("{ }"));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(789, p.getIntValue());
        p.close();

        // similarly with InputStream
        p = f.createParser(new ByteArrayInputStream("[ ]".getBytes("UTF-8")));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());
        p.close();

        // and with raw bytes
        final byte[] bytes = "[ ]".getBytes("UTF-8");
        p = f.createParser(bytes);
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(456, p.getIntValue());
        p.close();

        // also diff method has diff code path so try both:
        p = f.createParser(bytes, 0, bytes.length);
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(456, p.getIntValue());
        p.close();

        // and also char[]
        final char[] chars = "  [ ]".toCharArray();
        p = f.createParser(chars, 0, chars.length);
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(789, p.getIntValue());
        p.close();
    }

    @Test
    void outputDecoration() throws IOException
    {
        JsonFactory f = JsonFactory.builder()
                .outputDecorator(new SimpleOutputDecorator())
                .build();
        JsonGenerator g;

        StringWriter sw = new StringWriter();
        g = f.createGenerator(sw);
        g.close();
        assertEquals("567", sw.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        g = f.createGenerator(out, JsonEncoding.UTF8);
        g.close();
        assertEquals("123", out.toString("UTF-8"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void deprecatedMethods() throws IOException
    {
        JsonFactory f = new JsonFactory();
        assertNull(f.getInputDecorator());
        assertNull(f.getOutputDecorator());
        final SimpleInputDecorator inDec = new SimpleInputDecorator();
        final SimpleOutputDecorator outDec = new SimpleOutputDecorator();
        f.setInputDecorator(inDec);
        assertSame(inDec, f.getInputDecorator());
        f.setOutputDecorator(outDec);
        assertSame(outDec, f.getOutputDecorator());
    }

    /*
    /**********************************************************
    /* Unit tests: input/output decoration
    /**********************************************************
     */

    @Test
    void generatorDecoration() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .addDecorator(new SimpleGeneratorDecorator())
                .build();
        final String EXP = a2q("{'password':'***'}");

        // First, test with newly constructed factory
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = f.createGenerator(sw)) {
            _generateForDecorator(g);
        }
        assertEquals(EXP, sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(bytes)) {
            _generateForDecorator(g);
        }
        assertEquals(EXP, utf8String(bytes));

        // Second do the same to a copy
        JsonFactory f2 = f.copy();
        sw = new StringWriter();
        try (JsonGenerator g = f2.createGenerator(sw)) {
            _generateForDecorator(g);
        }
        assertEquals(EXP, sw.toString());

        // And re-built instance
        JsonFactory f3 = f2.rebuild().build();
        sw = new StringWriter();
        try (JsonGenerator g = f3.createGenerator(sw)) {
            _generateForDecorator(g);
        }
        assertEquals(EXP, sw.toString());
    }

    private void _generateForDecorator(JsonGenerator g) throws Exception
    {
        g.writeStartObject();
        g.writeStringField("password", "s3cr37x!!");
        g.writeEndObject();
    }
}
