package com.fasterxml.jackson.core.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Helper class for verifying that {@link java.io.OutputStream} is (or is not)
 * closed and/or flushed.
 */
public class ByteOutputStreamForTesting extends ByteArrayOutputStream
{
    public int closeCount = 0;
    public int flushCount = 0;

    public ByteOutputStreamForTesting() { }

    @Override
    public void close() throws IOException {
        ++closeCount;
        super.close();
    }

    @Override
    public void flush() throws IOException
    {
        ++flushCount;
        super.flush();
    }

    public boolean isClosed() { return closeCount > 0; }
    public boolean isFlushed() { return flushCount > 0; }
}
