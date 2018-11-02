package com.fasterxml.jackson.core.main;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;

import java.io.*;

/**
 * Set of basic unit tests that verify aspect of closing a
 * {@link JsonGenerator} instance. This includes both closing
 * of physical resources (target), and logical content
 * (json content tree)
 *<p>
 * Specifically, features
 * <code>JsonGenerator.Feature#AUTO_CLOSE_TARGET</code>
 * and
 * <code>JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT</code>
 * are tested.
 */
public class TestGeneratorClosing extends BaseTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    final static class MyWriter extends StringWriter
    {
        boolean _isClosed = false;

        public MyWriter() { }

        @Override
        public void close() throws IOException {
            _isClosed = true;
            super.close();
        }
        public boolean isClosed() { return _isClosed; }
    }

    final static class MyStream extends ByteArrayOutputStream
    {
        boolean _isClosed = false;

        public MyStream() { }

        @Override
        public void close() throws IOException {
            _isClosed = true;
            super.close();
        }
        public boolean isClosed() { return _isClosed; }
    }

    static class MyBytes extends ByteArrayOutputStream
    {
        public int flushed = 0;

        @Override
        public void flush() throws IOException
        {
            ++flushed;
            super.flush();
        }
    }

    static class MyChars extends StringWriter
    {
        public int flushed = 0;

        @Override
        public void flush()
        {
            ++flushed;
            super.flush();
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseGenerator() throws Exception
    {
        JsonFactory f = new JsonFactory();

        // Check the default settings
        assertTrue(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        // then change
        f = f.rebuild().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        assertFalse(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        @SuppressWarnings("resource")
        MyWriter output = new MyWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        g.writeNumber(39);
        // regular close won't close it either:
        g.close();
        assertFalse(output.isClosed());
    }

    public void testCloseGenerator() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        MyWriter output = new MyWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        g.writeNumber(39);
        // but close() should now close the writer
        g.close();
        assertTrue(output.isClosed());
    }

    public void testNoAutoCloseOutputStream() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        MyStream output = new MyStream();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output, JsonEncoding.UTF8);

        assertFalse(output.isClosed());
        g.writeNumber(39);
        g.close();
        assertFalse(output.isClosed());
    }

    public void testAutoCloseArraysAndObjects()
        throws Exception
    {
        JsonFactory f = new JsonFactory();
        // let's verify default setting, first:
        assertTrue(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_CONTENT));
        StringWriter sw = new StringWriter();

        // First, test arrays:
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.close();
        assertEquals("[]", sw.toString());

        // Then objects
        sw = new StringWriter();
        g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartObject();
        g.close();
        assertEquals("{}", sw.toString());
    }

    public void testNoAutoCloseArraysAndObjects()
        throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamWriteFeature.AUTO_CLOSE_CONTENT)
                .build();
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.close();
        // shouldn't close
        assertEquals("[", sw.toString());

        // Then objects
        sw = new StringWriter();
        g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartObject();
        g.close();
        assertEquals("{", sw.toString());
    }

    // [JACKSON-401]
    @SuppressWarnings("resource")
    public void testAutoFlushOrNot() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        MyChars sw = new MyChars();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, sw.flushed);
        g.flush();
        assertEquals(1, sw.flushed);
        g.close();
        
        // ditto with stream
        MyBytes bytes = new MyBytes();
        g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, bytes.flushed);
        g.flush();
        assertEquals(1, bytes.flushed);
        assertEquals(2, bytes.toByteArray().length);
        g.close();

        // then disable and we should not see flushing again...
        f = f.rebuild()
            .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
            .build();
        // first with a Writer
        sw = new MyChars();
        g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, sw.flushed);
        g.flush();
        assertEquals(0, sw.flushed);
        g.close();
        assertEquals("[]", sw.toString());

        // and then with OutputStream
        bytes = new MyBytes();
        g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, bytes.flushed);
        g.flush();
        assertEquals(0, bytes.flushed);
        g.close();
        assertEquals(2, bytes.toByteArray().length);
    }
}
