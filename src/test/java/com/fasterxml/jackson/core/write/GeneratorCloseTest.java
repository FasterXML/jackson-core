package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.testsupport.ByteOutputStreamForTesting;
import com.fasterxml.jackson.core.testsupport.StringWriterForTesting;

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
public class GeneratorCloseTest extends BaseTest
{
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
        assertTrue(f.isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        // then change
        f.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        assertFalse(f.isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        @SuppressWarnings("resource")
        ByteOutputStreamForTesting output = new ByteOutputStreamForTesting();
        JsonGenerator jg = f.createGenerator(output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        jg.writeNumber(39);
        // regular close won't close it either:
        jg.close();
        assertFalse(output.isClosed());
    }

    public void testCloseGenerator() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        @SuppressWarnings("resource")
        ByteOutputStreamForTesting output = new ByteOutputStreamForTesting();
        JsonGenerator jg = f.createGenerator(output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        jg.writeNumber(39);
        // but close() should now close the writer
        jg.close();
        assertTrue(output.isClosed());
    }

    public void testNoAutoCloseOutputStream() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        @SuppressWarnings("resource")
        ByteOutputStreamForTesting output = new ByteOutputStreamForTesting();
        JsonGenerator jg = f.createGenerator(output, JsonEncoding.UTF8);

        assertFalse(output.isClosed());
        jg.writeNumber(39);
        jg.close();
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
        JsonGenerator jg = f.createGenerator(sw);
        jg.writeStartArray();
        jg.close();
        assertEquals("[]", sw.toString());

        // Then objects
        sw = new StringWriter();
        jg = f.createGenerator(sw);
        jg.writeStartObject();
        jg.close();
        assertEquals("{}", sw.toString());
    }

    public void testNoAutoCloseArraysAndObjects()
        throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamWriteFeature.AUTO_CLOSE_CONTENT)
                .build();
        StringWriter sw = new StringWriter();
        JsonGenerator jg = f.createGenerator(sw);
        jg.writeStartArray();
        jg.close();
        // shouldn't close
        assertEquals("[", sw.toString());

        // Then objects
        sw = new StringWriter();
        jg = f.createGenerator(sw);
        jg.writeStartObject();
        jg.close();
        assertEquals("{", sw.toString());
    }

    @SuppressWarnings("resource")
    public void testAutoFlushOrNot() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        StringWriterForTesting sw = new StringWriterForTesting();
        JsonGenerator jg = f.createGenerator(sw);
        jg.writeStartArray();
        jg.writeEndArray();
        assertEquals(0, sw.flushCount);
        jg.flush();
        assertEquals(1, sw.flushCount);
        jg.close();

        // ditto with stream
        ByteOutputStreamForTesting bytes = new ByteOutputStreamForTesting();
        jg = f.createGenerator(bytes, JsonEncoding.UTF8);
        jg.writeStartArray();
        jg.writeEndArray();
        assertEquals(0, bytes.flushCount);
        jg.flush();
        assertEquals(1, bytes.flushCount);
        assertEquals(2, bytes.toByteArray().length);
        jg.close();

        // then disable and we should not see flushing again...
        f = JsonFactory.builder()
                .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                .build();
        // first with a Writer
        sw = new StringWriterForTesting();
        jg = f.createGenerator(sw);
        jg.writeStartArray();
        jg.writeEndArray();
        assertEquals(0, sw.flushCount);
        jg.flush();
        assertEquals(0, sw.flushCount);
        jg.close();
        assertEquals("[]", sw.toString());

        // and then with OutputStream
        bytes = new ByteOutputStreamForTesting();
        jg = f.createGenerator(bytes, JsonEncoding.UTF8);
        jg.writeStartArray();
        jg.writeEndArray();
        assertEquals(0, bytes.flushCount);
        jg.flush();
        assertEquals(0, bytes.flushCount);
        jg.close();
        assertEquals(2, bytes.toByteArray().length);
    }
}
