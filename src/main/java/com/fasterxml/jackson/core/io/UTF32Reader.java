package com.fasterxml.jackson.core.io;

import java.io.*;


/**
 * Since JDK does not come with UTF-32/UCS-4, let's implement a simple
 * decoder to use.
 */
public class UTF32Reader extends Reader
{
    /**
     * JSON actually limits available Unicode range in the high end
     * to the same as xml (to basically limit UTF-8 max byte sequence
     * length to 4)
     */
    final protected static int LAST_VALID_UNICODE_CHAR = 0x10FFFF;

    final protected static char NC = (char) 0;

    final protected IOContext _context;

    protected InputStream _in;

    protected byte[] _buffer;

    protected int _ptr;
    protected int _length;

    protected final boolean _bigEndian;

    /**
     * Although input is fine with full Unicode set, Java still uses
     * 16-bit chars, so we may have to split high-order chars into
     * surrogate pairs.
     */
    protected char _surrogate = NC;

    /**
     * Total read character count; used for error reporting purposes
     */
    protected int _charCount;

    /**
     * Total read byte count; used for error reporting purposes
     */
    protected int _byteCount;

    protected final boolean _managedBuffers;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public UTF32Reader(IOContext ctxt, InputStream in, byte[] buf, int ptr, int len, boolean isBigEndian) {
        _context = ctxt;
        _in = in;
        _buffer = buf;
        _ptr = ptr;
        _length = len;
        _bigEndian = isBigEndian;
        _managedBuffers = (in != null);
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    @Override
    public void close() throws IOException {
        InputStream in = _in;

        if (in != null) {
            _in = null;
            freeBuffers();
            in.close();
        }
    }

    protected char[] _tmpBuf;

    /**
     * Although this method is implemented by the base class, AND it should
     * never be called by main code, let's still implement it bit more
     * efficiently just in case
     */
    @Override
    public int read() throws IOException {
        if (_tmpBuf == null) {
            _tmpBuf = new char[1];
        }
        if (read(_tmpBuf, 0, 1) < 1) {
            return -1;
        }
        return _tmpBuf[0];
    }
    
    @Override
    public int read(char[] cbuf, int start, int len) throws IOException {
        // Already EOF?
        if (_buffer == null) { return -1; }
        if (len < 1) { return len; }
        // Let's then ensure there's enough room...
        if (start < 0 || (start+len) > cbuf.length) {
            reportBounds(cbuf, start, len);
        }

        len += start;
        int outPtr = start;

        // Ok, first; do we have a surrogate from last round?
        if (_surrogate != NC) {
            cbuf[outPtr++] = _surrogate;
            _surrogate = NC;
            // No need to load more, already got one char
        } else {
            /* Note: we'll try to avoid blocking as much as possible. As a
             * result, we only need to get 4 bytes for a full char.
             */
            int left = (_length - _ptr);
            if (left < 4) {
                if (!loadMore(left)) { // (legal) EOF?
                    return -1;
                }
            }
        }

        main_loop:
        while (outPtr < len) {
            int ptr = _ptr;
            int ch;

            if (_buffer.length < ptr + 4) {
                reportUnexpectedEOF(_buffer.length - ptr, 4);
            }

            if (_bigEndian) {
                ch = (_buffer[ptr] << 24) | ((_buffer[ptr+1] & 0xFF) << 16)
                    | ((_buffer[ptr+2] & 0xFF) << 8) | (_buffer[ptr+3] & 0xFF);
            } else {
                ch = (_buffer[ptr] & 0xFF) | ((_buffer[ptr+1] & 0xFF) << 8)
                    | ((_buffer[ptr+2] & 0xFF) << 16) | (_buffer[ptr+3] << 24);
            }
            _ptr += 4;

            // Does it need to be split to surrogates?
            // (also, we can and need to verify illegal chars)
            if (ch > 0xFFFF) { // need to split into surrogates?
                if (ch > LAST_VALID_UNICODE_CHAR) {
                    reportInvalid(ch, outPtr-start,
                                  "(above "+Integer.toHexString(LAST_VALID_UNICODE_CHAR)+") ");
                }
                ch -= 0x10000; // to normalize it starting with 0x0
                cbuf[outPtr++] = (char) (0xD800 + (ch >> 10));
                // hmmh. can this ever be 0? (not legal, at least?)
                ch = (0xDC00 | (ch & 0x03FF));
                // Room for second part?
                if (outPtr >= len) { // nope
                    _surrogate = (char) ch;
                    break main_loop;
                }
            }
            cbuf[outPtr++] = (char) ch;
            if (_ptr >= _length) {
                break main_loop;
            }
        }

        len = outPtr - start;
        _charCount += len;
        return len;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void reportUnexpectedEOF(int gotBytes, int needed) throws IOException {
        int bytePos = _byteCount + gotBytes, charPos = _charCount;

        throw new CharConversionException("Unexpected EOF in the middle of a 4-byte UTF-32 char: got "+gotBytes+", needed "+needed+", at char #"+charPos+", byte #"+bytePos+")");
    }

    private void reportInvalid(int value, int offset, String msg) throws IOException {
        int bytePos = _byteCount + _ptr - 1, charPos = _charCount + offset;

        throw new CharConversionException("Invalid UTF-32 character 0x"+Integer.toHexString(value)+msg+" at char #"+charPos+", byte #"+bytePos+")");
    }

    /**
     * @param available Number of "unused" bytes in the input buffer
     *
     * @return True, if enough bytes were read to allow decoding of at least
     *   one full character; false if EOF was encountered instead.
     */
    private boolean loadMore(int available) throws IOException {
        _byteCount += (_length - available);

        // Bytes that need to be moved to the beginning of buffer?
        if (available > 0) {
            if (_ptr > 0) {
                System.arraycopy(_buffer, _ptr, _buffer, 0, available);
                _ptr = 0;
            }
            _length = available;
        } else {
            /* Ok; here we can actually reasonably expect an EOF,
             * so let's do a separate read right away:
             */
            _ptr = 0;
            int count = (_in == null) ? -1 : _in.read(_buffer);
            if (count < 1) {
                _length = 0;
                if (count < 0) { // -1
                    if (_managedBuffers) {
                        freeBuffers(); // to help GC?
                    }
                    return false;
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            _length = count;
        }

        /* Need at least 4 bytes; if we don't get that many, it's an
         * error.
         */
        while (_length < 4) {
            int count = (_in == null) ? -1 : _in.read(_buffer, _length, _buffer.length - _length);
            if (count < 1) {
                if (count < 0) { // -1, EOF... no good!
                    if (_managedBuffers) {
                        freeBuffers(); // to help GC?
                    }
                    reportUnexpectedEOF(_length, 4);
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            _length += count;
        }
        return true;
    }

    /**
     * This method should be called along with (or instead of) normal
     * close. After calling this method, no further reads should be tried.
     * Method will try to recycle read buffers (if any).
     */
    private void freeBuffers() {
        byte[] buf = _buffer;
        if (buf != null) {
            _buffer = null;
            _context.releaseReadIOBuffer(buf);
        }
    }

    private void reportBounds(char[] cbuf, int start, int len) throws IOException {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    private void reportStrangeStream() throws IOException {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }
}
