package tools.jackson.core.unittest.write;


import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.OutputDecorator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.unittest.testutil.ByteOutputStreamForTesting;
import tools.jackson.core.unittest.testutil.StringWriterForTesting;

import static org.junit.jupiter.api.Assertions.*;

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
class GeneratorCloseTest extends JacksonCoreTestBase
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    @Test
    void noAutoCloseGenerator() throws Exception
    {
        JsonFactory f = new JsonFactory();

        // Check the default settings
        assertTrue(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        // then change
        f = f.rebuild().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        assertFalse(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        @SuppressWarnings("resource")
        StringWriterForTesting output = new StringWriterForTesting();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        g.writeNumber(39);
        // regular close won't close it either:
        g.close();
        assertFalse(output.isClosed());
    }

    @Test
    void closeGenerator() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        StringWriterForTesting output = new StringWriterForTesting();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        g.writeNumber(39);
        // but close() should now close the writer
        g.close();
        assertTrue(output.isClosed());
    }

    @Test
    void noAutoCloseOutputStream() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        ByteOutputStreamForTesting output = new ByteOutputStreamForTesting();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output, JsonEncoding.UTF8);

        assertFalse(output.isClosed());
        g.writeNumber(39);
        g.close();
        assertFalse(output.isClosed());
    }

    @Test
    void autoCloseArraysAndObjects()
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

    @Test
    void noAutoCloseArraysAndObjects()
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

    @SuppressWarnings("resource")
    @Test
    void autoFlushOrNot() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        StringWriterForTesting sw = new StringWriterForTesting();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, sw.flushCount);
        g.flush();
        assertEquals(1, sw.flushCount);
        g.close();

        // ditto with stream
        ByteOutputStreamForTesting bytes = new ByteOutputStreamForTesting();
        g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, bytes.flushCount);
        g.flush();
        assertEquals(1, bytes.flushCount);
        assertEquals(2, bytes.toByteArray().length);
        g.close();

        // then disable and we should not see flushing again...
        f = f.rebuild()
            .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
            .build();
        // first with a Writer
        sw = new StringWriterForTesting();
        g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, sw.flushCount);
        g.flush();
        assertEquals(0, sw.flushCount);
        g.close();
        assertEquals("[]", sw.toString());

        // and then with OutputStream
        bytes = new ByteOutputStreamForTesting();
        g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        assertEquals(0, bytes.flushCount);
        g.flush();
        assertEquals(0, bytes.flushCount);
        g.close();
        assertEquals(2, bytes.toByteArray().length);
    }

    // Content buffered by the encoding Writer Jackson creates for non-UTF-8
    // OutputStream targets must not be lost, even when generator is configured
    // to neither close nor flush the caller-owned stream
    @Test
    void nonUtf8OutputStreamNotLosingContent() throws Exception
    {
        for (JsonEncoding enc : new JsonEncoding[] {
                JsonEncoding.UTF16_BE, JsonEncoding.UTF16_LE,
                JsonEncoding.UTF32_BE, JsonEncoding.UTF32_LE }) {
            for (boolean autoClose : new boolean[] { true, false }) {
                for (boolean flush : new boolean[] { true, false }) {
                    JsonFactory f = JsonFactory.builder()
                            .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                            .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flush)
                            .build();
                    ByteOutputStreamForTesting output = new ByteOutputStreamForTesting();
                    JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output, enc);
                    g.writeStartObject();
                    g.writeNumberProperty("a", 1);
                    g.writeEndObject();
                    g.close();

                    String desc = enc+", autoClose="+autoClose+", flush="+flush;
                    assertEquals(a2q("{'a':1}"),
                            new String(output.toByteArray(), enc.getJavaName()), desc);
                    assertEquals(autoClose, output.isClosed(), desc);
                }
            }
        }
    }

    // Failure to flush pending encoded content on close must not mask earlier
    // failure, and generator must still be marked as closed
    @Test
    void nonUtf8FailingTargetKeepsOriginalFailure() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, false)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, false)
                .outputDecorator(new FailingWriterDecorator())
                .build();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(),
                new ByteOutputStreamForTesting(), JsonEncoding.UTF16_BE);
        g.writeStartObject();
        g.writeEndObject();

        JacksonException e = assertThrows(JacksonException.class, g::close);
        assertEquals("write failed", e.getCause().getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("flush failed", e.getSuppressed()[0].getCause().getMessage());
        assertTrue(g.isClosed());
    }

    static class FailingWriterDecorator extends OutputDecorator
    {
        private static final long serialVersionUID = 1L;

        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) {
            return out;
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) {
            return new FilterWriter(w) {
                @Override
                public void write(int c) throws IOException {
                    throw new IOException("write failed");
                }

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    throw new IOException("write failed");
                }

                @Override
                public void write(String str, int off, int len) throws IOException {
                    throw new IOException("write failed");
                }

                @Override
                public void flush() throws IOException {
                    throw new IOException("flush failed");
                }
            };
        }
    }
}
