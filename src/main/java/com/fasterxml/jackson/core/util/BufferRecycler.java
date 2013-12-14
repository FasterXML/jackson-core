package com.fasterxml.jackson.core.util;

/**
 * This is a small utility class, whose main functionality is to allow
 * simple reuse of raw byte/char buffers. It is usually used through
 * <code>ThreadLocal</code> member of the owning class pointing to
 * instance of this class through a <code>SoftReference</code>. The
 * end result is a low-overhead GC-cleanable recycling: hopefully
 * ideal for use by stream readers.
 */
public class BufferRecycler
{
//    public final static int DEFAULT_WRITE_CONCAT_BUFFER_LEN = 2000;

    /**
     * Buffer used for reading byte-based input.
     */
    public final static int BYTE_READ_IO_BUFFER = 0;

    /**
     * Buffer used for temporarily storing encoded content; used
     * for example by UTF-8 encoding writer
     */
    public final static int BYTE_WRITE_ENCODING_BUFFER = 1;

    /**
     * Buffer used for temporarily concatenating output; used for
     * example when requesting output as byte array.
     */
    public final static int BYTE_WRITE_CONCAT_BUFFER = 2;

    /**
     * Buffer used for concatenating binary data that is either being
     * encoded as base64 output, or decoded from base64 input.
     * 
     * @since 2.1
     */
    public final static int BYTE_BASE64_CODEC_BUFFER = 3;

    public final static int CHAR_TOKEN_BUFFER = 0;  // Tokenizable input
    public final static int CHAR_CONCAT_BUFFER = 1; // concatenated output
    public final static int CHAR_TEXT_BUFFER = 2; // Text content from input
    public final static int CHAR_NAME_COPY_BUFFER = 3; // Temporary buffer for getting name characters

    private final static int[] BYTE_BUFFER_LENGTHS = new int[] { 4000, 4000, 2000, 2000 };
    private final static int[] CHAR_BUFFER_LENGTHS = new int[] { 2000, 2000, 200, 200 };
    
    final protected byte[][] _byteBuffers = new byte[4][];
    final protected char[][] _charBuffers = new char[4][];

    public BufferRecycler() { }

    /**
     * @param ix One of <code>READ_IO_BUFFER</code> constants.
     */
    public final byte[] allocByteBuffer(int ix)
    {
        byte[] buffer = _byteBuffers[ix];
        if (buffer == null) {
            buffer = balloc(BYTE_BUFFER_LENGTHS[ix]);
        } else {
            _byteBuffers[ix] = null;
        }
        return buffer;
    }

    public final void releaseByteBuffer(int ix, byte[] buffer) {
        _byteBuffers[ix] = buffer;
    }

    public final char[] allocCharBuffer(int ix) {
        return allocCharBuffer(ix, 0);
    }

    public final char[] allocCharBuffer(int ix, int minSize)
    {
        final int DEF_SIZE = CHAR_BUFFER_LENGTHS[ix];
        if (minSize < DEF_SIZE) {
            minSize = DEF_SIZE;
        }
        char[] buffer = _charBuffers[ix];
        if (buffer == null || buffer.length < minSize) {
            buffer = calloc(minSize);
        } else {
            _charBuffers[ix] = null;
        }
        return buffer;
    }

    public final void releaseCharBuffer(int ix, char[] buffer) {
        _charBuffers[ix] = buffer;
    }

    /*
    /**********************************************************
    /* Actual allocations separated for easier debugging/profiling
    /**********************************************************
     */

    private byte[] balloc(int size) { return new byte[size]; }
    private char[] calloc(int size) { return new char[size]; }
}
