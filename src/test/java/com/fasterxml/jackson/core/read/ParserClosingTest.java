package com.fasterxml.jackson.core.read;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.*;

import java.io.*;

/**
 * Set of basic unit tests that verify that the closing (or not) of
 * the underlying source occurs as expected and specified
 * by documentation.
 */
public class ParserClosingTest
    extends BaseTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseReader()
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
        {
            assertFalse(f.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        }
        @SuppressWarnings("resource")
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createParser(input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());
    }

    @SuppressWarnings("resource")
    public void testAutoCloseReader() throws Exception
    {
        final String DOC = "[ 1 ]";

        JsonFactory f = JsonFactory.builder()
                .enable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createParser(input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // but can close half-way through
        jp.close();
        assertTrue(input.isClosed());

        // And then let's test implicit close at the end too:
        input = new MyReader(DOC);
        jp = f.createParser(input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        assertTrue(input.isClosed());
    }

    @SuppressWarnings("resource")
    public void testNoAutoCloseInputStream() throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        MyStream input = new MyStream(DOC.getBytes("UTF-8"));
        JsonParser jp = f.createParser(input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());
    }

    // [JACKSON-287]
    public void testReleaseContentBytes() throws Exception
    {
        byte[] input = "[1]foobar".getBytes("UTF-8");
        JsonParser jp = sharedStreamFactory().createParser(input);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(6, jp.releaseBuffered(out));
        assertArrayEquals("foobar".getBytes("UTF-8"), out.toByteArray());
        // also will "drain" so can not release twice
        assertEquals(0, jp.releaseBuffered(out));
        jp.close();
    }

    public void testReleaseContentChars() throws Exception
    {
        JsonParser jp = sharedStreamFactory().createParser("[true]xyz");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        StringWriter sw = new StringWriter();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(3, jp.releaseBuffered(sw));
        assertEquals("xyz", sw.toString());
        // also will "drain" so can not release twice
        assertEquals(0, jp.releaseBuffered(sw));
        jp.close();
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
