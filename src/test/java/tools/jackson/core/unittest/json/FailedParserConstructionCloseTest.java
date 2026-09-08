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
 * {@link java.nio.file.Path} sources get closed if construction of parser
 * fails after opening.
 */
class FailedParserConstructionCloseTest extends JacksonCoreTestBase
{
    static class CloseTrackingInputStream extends FilterInputStream {
        public boolean closed;

        CloseTrackingInputStream(InputStream in) { super(in); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /**
     * Factory that fails the way format backends can (say, when schema
     * validation fails), that is, after source has been opened.
     */
    static class FailingFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingInputStream> inputs = new ArrayList<>();

        @Override
        protected InputStream _fileInputStream(File f) throws JacksonException {
            CloseTrackingInputStream in = new CloseTrackingInputStream(super._fileInputStream(f));
            inputs.add(in);
            return in;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                InputStream in) throws JacksonException {
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
    void closesFileInputStreamOnFailedParserConstruction() throws Exception
    {
        FailingFactory f = new FailingFactory();
        File src = _tempFile();
        assertThrows(IllegalStateException.class,
                () -> f.createParser(ObjectReadContext.empty(), src));
        assertEquals(1, f.inputs.size());
        assertTrue(f.inputs.get(0).closed, "InputStream Jackson opened should have been closed");
    }
}
