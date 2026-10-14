package tools.jackson.core.unittest.json;

import java.io.ByteArrayInputStream;
import java.io.Reader;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.ByteSourceJsonBootstrapper;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [core#1768]: read buffer must be released even if no buffered content to merge
class ByteSourceBootstrapper1768Test extends JacksonCoreTestBase
{
    @Test
    void releaseBufferWithNoBufferedContent() throws Exception
    {
        IOContext ctxt = testIOContext();
        ctxt.setEncoding(JsonEncoding.UTF16_BE);
        ByteSourceJsonBootstrapper bs = new ByteSourceJsonBootstrapper(ctxt,
                new ByteArrayInputStream(new byte[] { 0, '1' }));
        // Skip encoding detection so that nothing gets buffered
        try (Reader r = bs.constructReader()) {
            assertEquals('1', r.read());
            assertEquals(-1, r.read());
        }
        // would fail with IllegalStateException if buffer was not released
        assertNotNull(ctxt.allocReadIOBuffer());
        ctxt.close();
    }
}
