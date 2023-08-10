package com.fasterxml.jackson.core.io;

import java.io.Reader;
import java.nio.CharBuffer;

public class CharBufferReader extends Reader {
    private final CharBuffer charBuffer;

    public CharBufferReader(CharBuffer buffer) {
        this.charBuffer = buffer.duplicate();
    }

    @Override
    public int read(char[] chars, int off, int len) {
        int remaining = this.charBuffer.remaining();
        if (remaining <= 0) {
            return -1;
        }
        int length = Math.min(len, remaining);
        this.charBuffer.get(chars, off, length);
        return length;
    }

    @Override
    public int read() {
        if (this.charBuffer.hasRemaining()) {
            return this.charBuffer.get();
        }
        return -1;
    }

    @Override
    public long skip(long n) {
        if (n < 0L) {
            throw new IllegalArgumentException("number of characters to skip cannot be negative");
        }
        int skipped = Math.min((int) n, this.charBuffer.remaining());
        this.charBuffer.position(this.charBuffer.position() + skipped);
        return skipped;
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) {
        this.charBuffer.mark();
    }

    @Override
    public void reset() {
        this.charBuffer.reset();
    }

    @Override
    public void close() {
        this.charBuffer.position(this.charBuffer.limit());
    }
}