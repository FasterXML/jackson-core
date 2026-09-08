package tools.jackson.core.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.ReadConstrainedTextBuffer;
import tools.jackson.core.util.TextBuffer;

/**
 * To limit number of configuration and state objects to pass, all
 * contextual objects that need to be passed by the factory to
 * readers and writers are combined under this object. One instance
 * is created for each reader and writer.
 */
public class IOContext implements AutoCloseable
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Reference to the source object, which can be used for displaying
     * location information
     */
    protected final ContentReference _contentReference;

    /**
     * Encoding used by the underlying stream, if known.
     */
    protected JsonEncoding _encoding;

    /**
     * Flag that indicates whether underlying input/output source/target
     * object is fully managed by the owner of this context (parser or
     * generator). If true, it is, and is to be closed by parser/generator;
     * if false, calling application has to do closing (unless auto-closing
     * feature is enabled for the parser/generator in question; in which
     * case it acts like the owner).
     */
    protected final boolean _managedResource;

    /*
    /**********************************************************************
    /* Buffer handling, recycling
    /**********************************************************************
     */

    /**
     * Recycler used for actual allocation/deallocation/reuse
     */
    protected final BufferRecycler _bufferRecycler;

    /**
     * Flag that indicates whether this context instance should release
     * configured {@code _bufferRecycler} or not: if it does, it needs to call
     * (via {@link BufferRecycler#releaseToPool()} when closed; if not,
     * should do nothing (recycler life-cycle is externally managed)
     */
    protected boolean _releaseRecycler = true;

    protected final StreamReadConstraints _streamReadConstraints;

    protected final StreamWriteConstraints _streamWriteConstraints;

    protected final ErrorReportConfiguration _errorReportConfiguration;

    /**
     * Reference to the allocated I/O buffer for low-level input reading,
     * if any allocated.
     */
    protected byte[] _readIOBuffer;

    /**
     * Reference to the allocated I/O buffer used for low-level
     * encoding-related buffering.
     */
    protected byte[] _writeEncodingBuffer;

    /**
     * Reference to the buffer allocated for temporary use with
     * base64 encoding or decoding.
     */
    protected byte[] _base64Buffer;

    /**
     * Reference to the buffer allocated for tokenization purposes,
     * in which character input is read, and from which it can be
     * further returned.
     */
    protected char[] _tokenCBuffer;

    /**
     * Reference to the buffer allocated for buffering it for
     * output, before being encoded: generally this means concatenating
     * output, then encoding when buffer fills up.
     */
    protected char[] _concatCBuffer;

    /**
     * Reference temporary buffer Parser instances need if calling
     * app decides it wants to access name via 'getTextCharacters' method.
     * Regular text buffer cannot be used as it may contain textual
     * representation of the value token.
     */
    protected char[] _nameCopyBuffer;

    /**
     * Intermediate encoding {@link Writer} Jackson constructed itself over
     * caller-supplied {@link java.io.OutputStream}, if any. Unlike caller-provided
     * targets it has to be drained when this context is closed: caller has no
     * reference with which to flush content buffered in it.
     */
    protected EncodingWriter _encodingWriter;

    private boolean _closed = false;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * Main constructor to use.
     *
     * @param src Constraints for streaming reads
     * @param swc Constraints for streaming writes
     * @param erc Configuration for error reporting
     * @param br BufferRecycler to use, if any ({@code null} if none)
     * @param contentRef Input source reference for location reporting
     * @param managedResource Whether input source is managed (owned) by Jackson library
     * @param enc Encoding in use
     */
    public IOContext(StreamReadConstraints src, StreamWriteConstraints swc,
            ErrorReportConfiguration erc,
            BufferRecycler br, ContentReference contentRef, boolean managedResource,
            JsonEncoding enc)
    {
        _streamReadConstraints = Objects.requireNonNull(src);
        _streamWriteConstraints = Objects.requireNonNull(swc);
        _errorReportConfiguration = Objects.requireNonNull(erc);
        _bufferRecycler = br;
        _contentReference = contentRef;
        _managedResource = managedResource;
        _encoding = enc;
    }

    /**
     * Method to call to prevent {@link #_bufferRecycler} release upon
     * {@link #close()}: called when {@link #_bufferRecycler} life-cycle is
     * externally managed.
     *
     * @since 2.17
     */
    public IOContext markBufferRecyclerReleased() {
        _releaseRecycler = false;
        return this;
    }

    /**
     * Method factories call to register intermediate encoding {@link Writer}
     * they constructed themselves to wrap caller-supplied
     * {@link java.io.OutputStream}; returns Writer to actually pass to generator.
     *<p>
     * Content buffered by such Writer is flushed when this context is closed
     * (that is, when generator using it gets closed), regardless of whether
     * generator is configured to close or flush the caller-owned target:
     * otherwise buffered output would simply be lost.
     *
     * @param w Encoding Writer Jackson constructed
     *
     * @return Writer to pass to generator
     *
     * @since 3.1.7
     */
    public Writer encodingWriter(Writer w) {
        EncodingWriter ew = new EncodingWriter(w);
        _encodingWriter = ew;
        return ew;
    }

    /*
    /**********************************************************************
    /* Public API, accessors
    /**********************************************************************
     */

    /**
     * @return constraints for streaming reads
     */
    public StreamReadConstraints streamReadConstraints() {
        return _streamReadConstraints;
    }

    /**
     * @return constraints for streaming writes
     */
    public StreamWriteConstraints streamWriteConstraints() {
        return _streamWriteConstraints;
    }

    /**
     * @return Configured {@link ErrorReportConfiguration}, containing configured values for
     *   handling error reporting.
     */
    public ErrorReportConfiguration errorReportConfiguration() {
        return _errorReportConfiguration;
    }

    public IOContext setEncoding(JsonEncoding enc) {
        _encoding = enc;
        return this;
    }

    public JsonEncoding getEncoding() { return _encoding; }
    public boolean isResourceManaged() { return _managedResource; }

    /**
     * Accessor for getting (some) information about input source, mostly
     * usable for error reporting purposes.
     *
     * @return Reference to input source
     */
    public ContentReference contentReference() {
        return _contentReference;
    }

    // @since 2.17
    public BufferRecycler bufferRecycler() {
        return _bufferRecycler;
    }

    /*
    /**********************************************************************
    /* Public API, buffer management
    /**********************************************************************
     */

    public TextBuffer constructTextBuffer() {
        return new TextBuffer(_bufferRecycler);
    }

    public TextBuffer constructReadConstrainedTextBuffer() {
        return new ReadConstrainedTextBuffer(_streamReadConstraints, _bufferRecycler);
    }

    /**
     * Method for recycling or allocation byte buffer of "read I/O" type.
     *<p>
     * Note: the method can only be called once during its life cycle.
     * This is to protect against accidental sharing.
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocReadIOBuffer() {
        _verifyAlloc(_readIOBuffer);
        return (_readIOBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER));
    }

    /**
     * Variant of {@link #allocReadIOBuffer()} that specifies smallest acceptable
     * buffer size.
     *
     * @param minSize Minimum size of the buffer to recycle or allocate
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocReadIOBuffer(int minSize) {
        _verifyAlloc(_readIOBuffer);
        return (_readIOBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, minSize));
    }

    /**
     * Method for recycling or allocation byte buffer of "write encoding" type.
     *<p>
     * Note: the method can only be called once during its life cycle.
     * This is to protect against accidental sharing.
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocWriteEncodingBuffer() {
        _verifyAlloc(_writeEncodingBuffer);
        return (_writeEncodingBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_WRITE_ENCODING_BUFFER));
    }

    /**
     * Variant of {@link #allocWriteEncodingBuffer()} that specifies smallest acceptable
     * buffer size.
     *
     * @param minSize Minimum size of the buffer to recycle or allocate
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocWriteEncodingBuffer(int minSize) {
        _verifyAlloc(_writeEncodingBuffer);
        return (_writeEncodingBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_WRITE_ENCODING_BUFFER, minSize));
    }

    /**
     * Method for recycling or allocation byte buffer of "base 64 encode/decode" type.
     *<p>
     * Note: the method can only be called once during its life cycle.
     * This is to protect against accidental sharing.
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocBase64Buffer() {
        _verifyAlloc(_base64Buffer);
        return (_base64Buffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_BASE64_CODEC_BUFFER));
    }

    /**
     * Variant of {@link #allocBase64Buffer()} that specifies smallest acceptable
     * buffer size.
     *
     * @param minSize Minimum size of the buffer to recycle or allocate
     *
     * @return Allocated or recycled byte buffer
     */
    public byte[] allocBase64Buffer(int minSize) {
        _verifyAlloc(_base64Buffer);
        return (_base64Buffer = _bufferRecycler.allocByteBuffer(BufferRecycler.BYTE_BASE64_CODEC_BUFFER, minSize));
    }

    public char[] allocTokenBuffer() {
        _verifyAlloc(_tokenCBuffer);
        return (_tokenCBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER));
    }

    public char[] allocTokenBuffer(int minSize) {
        _verifyAlloc(_tokenCBuffer);
        return (_tokenCBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, minSize));
    }

    public char[] allocConcatBuffer() {
        _verifyAlloc(_concatCBuffer);
        return (_concatCBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CHAR_CONCAT_BUFFER));
    }

    public char[] allocNameCopyBuffer(int minSize) {
        _verifyAlloc(_nameCopyBuffer);
        return (_nameCopyBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, minSize));
    }

    /**
     * Method to call when all the processing buffers can be safely
     * recycled.
     *
     * @param buf Buffer instance to release (return for recycling)
     */
    public void releaseReadIOBuffer(byte[] buf) {
        if (buf != null) {
            // Let's do sanity checks to ensure once-and-only-once release,
            // as well as avoiding trying to release buffers not owned
            _verifyRelease(buf, _readIOBuffer);
            _readIOBuffer = null;
            _bufferRecycler.releaseByteBuffer(BufferRecycler.BYTE_READ_IO_BUFFER, buf);
        }
    }

    public void releaseWriteEncodingBuffer(byte[] buf) {
        if (buf != null) {
            // Let's do sanity checks to ensure once-and-only-once release,
            // as well as avoiding trying to release buffers not owned
            _verifyRelease(buf, _writeEncodingBuffer);
            _writeEncodingBuffer = null;
            _bufferRecycler.releaseByteBuffer(BufferRecycler.BYTE_WRITE_ENCODING_BUFFER, buf);
        }
    }

    public void releaseBase64Buffer(byte[] buf) {
        if (buf != null) { // sanity checks, release once-and-only-once, must be one owned
            _verifyRelease(buf, _base64Buffer);
            _base64Buffer = null;
            _bufferRecycler.releaseByteBuffer(BufferRecycler.BYTE_BASE64_CODEC_BUFFER, buf);
        }
    }

    public void releaseTokenBuffer(char[] buf) {
        if (buf != null) {
            _verifyRelease(buf, _tokenCBuffer);
            _tokenCBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CHAR_TOKEN_BUFFER, buf);
        }
    }

    public void releaseConcatBuffer(char[] buf) {
        if (buf != null) {
            // 14-Jan-2014, tatu: Let's actually allow upgrade of the original buffer.
            _verifyRelease(buf, _concatCBuffer);
            _concatCBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CHAR_CONCAT_BUFFER, buf);
        }
    }

    public void releaseNameCopyBuffer(char[] buf) {
        if (buf != null) {
            // 14-Jan-2014, tatu: Let's actually allow upgrade of the original buffer.
            _verifyRelease(buf, _nameCopyBuffer);
            _nameCopyBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CHAR_NAME_COPY_BUFFER, buf);
        }
    }

    /*
    /**********************************************************************
    /* Internal helpers
    /**********************************************************************
     */

    protected final void _verifyAlloc(Object buffer) {
        if (buffer != null) { throw new IllegalStateException("Trying to call same allocXxx() method second time"); }
    }

    protected final void _verifyRelease(byte[] toRelease, byte[] src) {
        // 07-Mar-2016, tatu: As per [core#255], only prevent shrinking of buffer
        if ((toRelease != src) && (toRelease.length < src.length)) { throw wrongBuf(); }
    }

    protected final void _verifyRelease(char[] toRelease, char[] src) {
        // 07-Mar-2016, tatu: As per [core#255], only prevent shrinking of buffer
        if ((toRelease != src) && (toRelease.length < src.length)) { throw wrongBuf(); }
    }

    private IllegalArgumentException wrongBuf() {
        // sanity check failed; trying to return different, smaller buffer.
        return new IllegalArgumentException("Trying to release buffer smaller than original");
    }

    @Override
    public void close() {
        if (!_closed) {
            _closed = true;
            try {
                EncodingWriter w = _encodingWriter;
                if (w != null) {
                    _encodingWriter = null;
                    w.flushPending();
                }
            } finally {
                if (_releaseRecycler) {
                    _releaseRecycler = false;
                    _bufferRecycler.releaseToPool();
                }
            }
        }
    }

    /**
     * Wrapper for encoding {@link Writer} Jackson constructed itself, tracking
     * whether it holds content not yet pushed to the underlying target, so that
     * {@link IOContext#close} can drain it exactly once.
     */
    private final static class EncodingWriter extends FilterWriter
    {
        private boolean _pending;

        EncodingWriter(Writer w) { super(w); }

        @Override
        public void write(int c) throws IOException {
            _pending = true;
            super.write(c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            _pending = true;
            super.write(cbuf, off, len);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            _pending = true;
            super.write(str, off, len);
        }

        @Override
        public void flush() throws IOException {
            _pending = false;
            super.flush();
        }

        @Override
        public void close() throws IOException {
            // clear before delegating: close() may lead back to IOContext.close()
            _pending = false;
            super.close();
        }

        public void flushPending() {
            if (_pending) {
                _pending = false;
                try {
                    out.flush();
                } catch (IOException e) {
                    throw JacksonIOException.construct(e);
                }
            }
        }
    }
}
