package tools.jackson.core.unittest.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

// [core#763] (and [databind#3455]
class InputStreamInitTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    static class FailingInputStream extends InputStream {
        public boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public int read() throws IOException {
            throw new IOException("Will not read, ever!");
        }
    }

    static class FailingJsonFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public FailingInputStream lastStream;

        @Override
        protected InputStream _fileInputStream(File f) {
            return (lastStream = new FailingInputStream());
        }

        @Override
        protected InputStream _optimizedStreamFromURL(URL url) {
            return (lastStream = new FailingInputStream());
        }
    }

    @Test
    void forFile() throws Exception
    {
        final FailingJsonFactory jsonF = new FailingJsonFactory();
        try {
            /*JsonParser p =*/ jsonF.createParser(ObjectReadContext.empty(),
                    new File("/tmp/test.json"));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Will not read");
        }
        assertNotNull(jsonF.lastStream);
        assertTrue(jsonF.lastStream.closed);
    }
}
