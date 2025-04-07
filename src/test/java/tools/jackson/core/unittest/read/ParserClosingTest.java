package tools.jackson.core.unittest.read;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Set of basic unit tests that verify that the closing (or not) of
 * the underlying source occurs as expected and specified
 * by documentation.
 */
class ParserClosingTest
        extends JacksonCoreTestBase
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    @Test
    void noAutoCloseReader()
            throws Exception
    {
        final String DOC = "[ 1 ]";

        // Check the default settings
        assertTrue(sharedStreamFactory().isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        // then change
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        assertFalse(f.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        @SuppressWarnings("resource")
        MyReader input = new MyReader(DOC);
        JsonParser p = f.createParser(ObjectReadContext.empty(), input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        p.close();
        assertFalse(input.isClosed());
    }

    @SuppressWarnings("resource")
    @Test
    void autoCloseReader() throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = JsonFactory.builder()
                .enable(StreamReadFeature.AUTO_CLOSE_SOURCE).build();
        assertTrue(f.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        MyReader input = new MyReader(DOC);
        JsonParser p = f.createParser(ObjectReadContext.empty(), input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        // but can close half-way through
        p.close();
        assertTrue(input.isClosed());

        // And then let's test implicit close at the end too:
        input = new MyReader(DOC);
        p = f.createParser(ObjectReadContext.empty(), input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        assertTrue(input.isClosed());
    }

    @SuppressWarnings("resource")
    @Test
    void noAutoCloseInputStream() throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build();
        MyStream input = new MyStream(DOC.getBytes("UTF-8"));
        JsonParser p = f.createParser(ObjectReadContext.empty(), input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        p.close();
        assertFalse(input.isClosed());
    }

    // [JACKSON-287]
    @Test
    void releaseContentBytes() throws Exception
    {
        byte[] input = "[1]foobar".getBytes("UTF-8");
        JsonParser p = sharedStreamFactory().createParser(ObjectReadContext.empty(), input);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(6, p.releaseBuffered(out));
        assertArrayEquals("foobar".getBytes("UTF-8"), out.toByteArray());
        // also will "drain" so cannot release twice
        assertEquals(0, p.releaseBuffered(out));
        p.close();
    }

    @Test
    void releaseContentChars() throws Exception
    {
        JsonParser p = sharedStreamFactory().createParser(ObjectReadContext.empty(), "[true]xyz");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        StringWriter sw = new StringWriter();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(3, p.releaseBuffered(sw));
        assertEquals("xyz", sw.toString());
        // also will "drain" so cannot release twice
        assertEquals(0, p.releaseBuffered(sw));
        p.close();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    final static class MyReader extends StringReader
    {
        boolean mIsClosed = false;

        public MyReader(String contents) {
            super(contents);
        }

        @Override
        public void close() {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyStream extends ByteArrayInputStream
    {
        boolean mIsClosed = false;

        public MyStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

}
