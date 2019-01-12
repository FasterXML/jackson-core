package com.fasterxml.jackson.core.testsupport;

import java.io.IOException;
import java.io.StringWriter;

public class StringWriterForTesting extends StringWriter
{
    public int closeCount = 0;
    public int flushCount = 0;

    public StringWriterForTesting() { }

    @Override
    public void close() throws IOException {
        ++closeCount;
        super.close();
    }

    @Override
    public void flush()
    {
        ++flushCount;
        super.flush();
    }

    public boolean isClosed() { return closeCount > 0; }
    public boolean isFlushed() { return flushCount > 0; }
}
