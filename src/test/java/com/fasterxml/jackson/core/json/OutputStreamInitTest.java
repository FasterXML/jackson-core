package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

//[core#764] (and [databind#3508]
public class OutputStreamInitTest
    extends com.fasterxml.jackson.core.BaseTest
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

    public void testForFile() throws Exception
    {
        final FailingJsonFactory jsonF = new FailingJsonFactory();
        try {
            JsonGenerator g = jsonF.createGenerator(new File("/tmp/test.json"),
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
