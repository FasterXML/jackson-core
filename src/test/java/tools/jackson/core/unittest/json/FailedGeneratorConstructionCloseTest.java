package tools.jackson.core.unittest.json;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that streams Jackson itself opens for {@link File} /
 * {@link java.nio.file.Path} targets get closed if construction of generator
 * fails after opening.
 */
class FailedGeneratorConstructionCloseTest extends JacksonCoreTestBase
{
    static class CloseTrackingOutputStream extends FilterOutputStream {
        public boolean closed;

        CloseTrackingOutputStream(OutputStream out) { super(out); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /**
     * Factory that fails the way format backends can (say, when schema
     * validation fails), that is, after source/target has been opened.
     */
    static class FailingFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingOutputStream> outputs = new ArrayList<>();

        @Override
        protected OutputStream _fileOutputStream(File f) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._fileOutputStream(f));
            outputs.add(out);
            return out;
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, Writer out) throws JacksonException {
            throw new IllegalStateException("Test-induced construction failure");
        }

        @Override
        protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) throws JacksonException {
            throw new IllegalStateException("Test-induced construction failure");
        }
    }

    private File _tempFile() throws IOException {
        File f = File.createTempFile("jackson-core-test", ".json");
        f.deleteOnExit();
        Files.write(f.toPath(), utf8Bytes("{\"a\":1}"));
        return f;
    }

    @Test
    void closesFileOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        for (JsonEncoding enc : new JsonEncoding[] { JsonEncoding.UTF8, JsonEncoding.UTF16_BE }) {
            FailingFactory f = new FailingFactory();
            File dst = _tempFile();
            assertThrows(IllegalStateException.class,
                    () -> f.createGenerator(ObjectWriteContext.empty(), dst, enc));
            assertEquals(1, f.outputs.size(), enc.toString());
            assertTrue(f.outputs.get(0).closed,
                    "OutputStream Jackson opened should have been closed, encoding "+enc);
        }
    }
}
