package com.fasterxml.jackson.core.json;

import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * An adapter implementation from {@link CharBuffer} to {@link Reader}.
 * This is used by {@link ByteSourceJsonBootstrapper#constructReader()} when processing small inputs to
 * avoid the 8KiB {@link java.nio.ByteBuffer} allocated by {@link java.io.InputStreamReader}, see [jackson-core#488].
 */
final class CharBufferReader extends Reader {
    private final CharBuffer charBuffer;

    CharBufferReader(CharBuffer buffer) {
        this.charBuffer = buffer;
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
        int skipped = Math.min(this.charBuffer.remaining(), (int) Math.min(n, Integer.MAX_VALUE));
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
        if (readAheadLimit < 0L) {
            throw new IllegalArgumentException("read ahead limit cannot be negative");
        }
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
