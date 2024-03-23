package tools.jackson.core.write;


import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.ByteOutputStreamForTesting;
import tools.jackson.core.testsupport.StringWriterForTesting;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
class GeneratorCloseTest extends JUnit5TestBase
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
}
