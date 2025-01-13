package tools.jackson.core.unittest.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

//[core#764] (and [databind#3508]
class OutputStreamInitTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    static class FailingOutputStream extends OutputStream {
        public int written = 0;
        public boolean failWrites = false;
        public boolean closed = false;

        public void startFailingWrites() {
            failWrites = true;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void write(int b) throws IOException {
            ++written;
            if (failWrites) {
                throw new IOException("No writes!");
            }
        }
    }

    static class FailingJsonFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public FailingOutputStream lastStream;

        @Override
        protected OutputStream _fileOutputStream(File f) {
            return (lastStream = new FailingOutputStream());
        }
    }

    @Test
    void forFile() throws Exception
    {
        final FailingJsonFactory jsonF = new FailingJsonFactory();
        try {
            JsonGenerator g = jsonF.createGenerator(ObjectWriteContext.empty(),
                    new File("/tmp/test.json"),
                    JsonEncoding.UTF8);
            g.writeString("foo");
            jsonF.lastStream.startFailingWrites();
            g.close();
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "No writes");
        }
        assertNotNull(jsonF.lastStream);
        assertTrue(jsonF.lastStream.closed);
    }

}
