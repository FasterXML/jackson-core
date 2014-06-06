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

    // Buffer lengths, defined in 2.4 (smaller before that)

    private final static int[] BYTE_BUFFER_LENGTHS = new int[] { 8000, 8000, 2000, 2000 };
    private final static int[] CHAR_BUFFER_LENGTHS = new int[] { 4000, 4000, 200, 200 };
    
    final protected byte[][] _byteBuffers;
    final protected char[][] _charBuffers;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    /**
     * Default constructor used for creating instances of this default
     * implementation.
     */
    public BufferRecycler() {
        this(4, 4);
    }

    /**
     * Alternate constructor to be used by sub-classes, to allow customization
     * of number of low-level buffers in use.
     * 
     * @since 2.4
     */
    protected BufferRecycler(int bbCount, int cbCount) {
        _byteBuffers = new byte[bbCount][];
        _charBuffers = new char[cbCount][];
    }

    /*
    /**********************************************************
    /* Public API, byte buffers
    /**********************************************************
     */
    
    /**
     * @param ix One of <code>READ_IO_BUFFER</code> constants.
     */
    public final byte[] allocByteBuffer(int ix) {
        return allocByteBuffer(ix, 0);
    }

    public byte[] allocByteBuffer(int ix, int minSize) {
        final int DEF_SIZE = byteBufferLength(ix);
        if (minSize < DEF_SIZE) {
            minSize = DEF_SIZE;
        }
        byte[] buffer = _byteBuffers[ix];
        if (buffer == null || buffer.length < minSize) {
            buffer = balloc(minSize);
        } else {
            _byteBuffers[ix] = null;
        }
        return buffer;
    }

    public final void releaseByteBuffer(int ix, byte[] buffer) {
        _byteBuffers[ix] = buffer;
    }

    /*
    /**********************************************************
    /* Public API, char buffers
    /**********************************************************
     */
    
    public final char[] allocCharBuffer(int ix) {
        return allocCharBuffer(ix, 0);
    }
    
    public char[] allocCharBuffer(int ix, int minSize) {
        final int DEF_SIZE = charBufferLength(ix);
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

    public void releaseCharBuffer(int ix, char[] buffer) {
        _charBuffers[ix] = buffer;
    }

    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    protected int byteBufferLength(int ix) {
        return BYTE_BUFFER_LENGTHS[ix];
    }

    protected int charBufferLength(int ix) {
        return CHAR_BUFFER_LENGTHS[ix];
    }
    
    /*
    /**********************************************************
    /* Actual allocations separated for easier debugging/profiling
    /**********************************************************
     */

    protected byte[] balloc(int size) { return new byte[size]; }
    protected char[] calloc(int size) { return new char[size]; }
}
