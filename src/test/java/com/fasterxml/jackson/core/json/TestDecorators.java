package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.core.io.OutputDecorator;

/**
 * Unit tests to verify that input and output decorators work as
 * expected
 *
 * @since 1.8
 */
@SuppressWarnings("serial")
public class TestDecorators extends com.fasterxml.jackson.core.BaseTest
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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testInputDecoration() throws IOException
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

    public void testOutputDecoration() throws IOException
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
    public void testDeprecatedMethods() throws IOException
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
}
