package com.fasterxml.jackson.core.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.fasterxml.jackson.core.*;

// [core#763] (and [databind#3455]
public class InputStreamInitTest
    extends com.fasterxml.jackson.core.BaseTest
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

    public void testForFile() throws Exception
    {
        final FailingJsonFactory jsonF = new FailingJsonFactory();
        try {
            /*JsonParser p =*/ jsonF.createParser(new File("/tmp/test.json"));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Will not read");
        }
        assertNotNull(jsonF.lastStream);
        assertTrue(jsonF.lastStream.closed);
    }

    public void testForURL() throws Exception
    {
        final FailingJsonFactory jsonF = new FailingJsonFactory();
        try {
            /*JsonParser p =*/ jsonF.createParser(new URL("http://localhost:80/"));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Will not read");
        }
        assertNotNull(jsonF.lastStream);
        assertTrue(jsonF.lastStream.closed);
    }
}
