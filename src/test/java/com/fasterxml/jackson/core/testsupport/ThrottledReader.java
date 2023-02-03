package com.fasterxml.jackson.core.testsupport;

import java.io.*;

public class ThrottledReader extends FilterReader
{
    protected final int _maxChars;

    public ThrottledReader(String doc, int maxChars) {
        this(new StringReader(doc), maxChars);
    }

    public ThrottledReader(char[] data, int maxChars) {
        this(new CharArrayReader(data), maxChars);
    }

    public ThrottledReader(Reader r, int maxChars)
    {
        super(r);
        _maxChars = maxChars;
    }

    @Override
    public int read(char[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read(char[] buf, int offset, int len) throws IOException {
        return in.read(buf, offset, Math.min(_maxChars, len));
    }
}
