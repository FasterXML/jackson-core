package tools.jackson.core.json;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.InputDecorator;
import tools.jackson.core.io.OutputDecorator;

/**
 * Unit tests to verify that input and output decorators work as
 * expected
 */
@SuppressWarnings("serial")
public class TestDecorators extends tools.jackson.core.BaseTest
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
            throws JacksonException
        {
            try {
                return new ByteArrayInputStream("123".getBytes("UTF-8"));
            } catch (IOException e) {
                throw WrappedIOException.construct(e, null);
            }
        }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length)
            throws JacksonException
        {
            try {
                return new ByteArrayInputStream("456".getBytes("UTF-8"));
            } catch (IOException e) {
                throw WrappedIOException.construct(e, null);
            }
        }

        @Override
        public Reader decorate(IOContext ctxt, Reader src) {
            return new StringReader("789");
        }
    }

    static class SimpleOutputDecorator extends OutputDecorator
    {
        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) throws JacksonException
        {
            try {
                out.write("123".getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                throw WrappedIOException.construct(e, null);
            }
            return new ByteArrayOutputStream();
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) throws JacksonException
        {
            try {
                w.write("567");
                w.flush();
            } catch (IOException e) {
                throw WrappedIOException.construct(e, null);
            }
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
        p = f.createParser(ObjectReadContext.empty(), new StringReader("{ }"));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(789, p.getIntValue());
        p.close();

        // similarly with InputStream
        p = f.createParser(ObjectReadContext.empty(), new ByteArrayInputStream("[ ]".getBytes("UTF-8")));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());
        p.close();

        // and with raw bytes
        final byte[] bytes = "[ ]".getBytes("UTF-8");
        p = f.createParser(ObjectReadContext.empty(), bytes);
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(456, p.getIntValue());
        p.close();

        // also diff method has diff code path so try both:
        p = f.createParser(ObjectReadContext.empty(), bytes, 0, bytes.length);
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(456, p.getIntValue());
        p.close();

        // and also char[]
        final char[] chars = "  [ ]".toCharArray();
        p = f.createParser(ObjectReadContext.empty(), chars, 0, chars.length);
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(789, p.getIntValue());
        p.close();
    }

    public void testOutputDecoration() throws IOException
    {
        JsonFactory f = JsonFactory.builder()
                .outputDecorator(new SimpleOutputDecorator())
                .build();
        JsonGenerator jg;

        StringWriter sw = new StringWriter();
        jg = f.createGenerator(ObjectWriteContext.empty(), sw);
        jg.close();
        assertEquals("567", sw.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jg = f.createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8);
        jg.close();
        assertEquals("123", out.toString("UTF-8"));
    }
}
