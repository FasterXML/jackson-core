package tools.jackson.core.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * This is a small utility class, whose main functionality is to allow
 * simple reuse of raw byte/char buffers. It is usually allocated through
 * {@link RecyclerPool} (starting with 2.16): multiple pool
 * implementations exists.
 * The default pool implementation uses
 * {@code ThreadLocal} combined with {@code SoftReference}.
 * The end result is a low-overhead GC-cleanable recycling: hopefully
 * ideal for use by stream readers.
 *<p>
 * Rewritten in 2.10 to be thread-safe (see [jackson-core#479] for details),
 * to not rely on {@code ThreadLocal} access.<br>
 * Rewritten in 2.16 to work with {@link RecyclerPool} abstraction.
 */
public class BufferRecycler
        implements RecyclerPool.WithPool<BufferRecycler>
{
    /**
     * VarHandle for atomic acquire/release access to Object[] array elements.
     * Used for the byte and char buffer slot arrays to apply minimal required
     * memory ordering semantics (acquire on alloc, release on release) rather
     * than the unconditional volatile barriers that AtomicReferenceArray imposes.
     */
    private static final VarHandle BUFFER_VH;

    static {
        try {
            BUFFER_VH = MethodHandles.arrayElementVarHandle(Object[].class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Tag-on interface to allow various other types to expose {@link BufferRecycler}
     * they are constructed with.
     *
     * @since 2.17
     */
    public interface Gettable {
        /**
         * @return Buffer recycler instance object is configured with, if any;
         *    whether this can be {@code null} depends on type of object
         */
        public BufferRecycler bufferRecycler();
    }

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
     */
    public final static int BYTE_BASE64_CODEC_BUFFER = 3;

    /**
     * Buffer used as input buffer for tokenization for character-based parsers.
     */
    public final static int CHAR_TOKEN_BUFFER = 0;

    /**
     * Buffer used by generators; for byte-backed generators for buffering of
     * {@link String} values to output (before encoding into UTF-8),
     * and for char-backed generators as actual concatenation buffer.
     */
    public final static int CHAR_CONCAT_BUFFER = 1;

    /**
     * Used through {@link TextBuffer}: directly by parsers (to concatenate
     * String values)
     *  and indirectly via
     * {@link tools.jackson.core.io.SegmentedStringWriter}
     * when serializing (databind level {@code ObjectMapper} and
     * {@code ObjectWriter}). In both cases used as segments (and not for whole value),
     * but may result in retention of larger chunks for big content
     * (long text values during parsing; bigger output documents for generation).
     */
    public final static int CHAR_TEXT_BUFFER = 2;

    /**
     * For parsers, temporary buffer into which {@code char[]} for names is copied
     * when requested as such; for {@code WriterBasedGenerator} used for buffering
     * during {@code writeString(Reader)} operation (not commonly used).
     */
    public final static int CHAR_NAME_COPY_BUFFER = 3;

    // Buffer lengths

    private final static int[] BYTE_BUFFER_LENGTHS = new int[] { 8000, 8000, 2000, 2000 };
    private final static int[] CHAR_BUFFER_LENGTHS = new int[] { 4000, 4000, 200, 200 };

    // Note: changed from AtomicReferenceArray in 3.2 to Object[] + VarHandle for
    // finer-grained memory ordering control (acquire/release instead of full volatile).
    protected final Object[] _byteBuffers;

    // Note: changed from AtomicReferenceArray in 3.2 to Object[] + VarHandle for
    // finer-grained memory ordering control (acquire/release instead of full volatile).
    protected final Object[] _charBuffers;

    private RecyclerPool<BufferRecycler> _pool;

    /*
    /**********************************************************
    /* Life-cycle
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
     * @param bbCount Number of {@code byte[]} buffers to allocate
     * @param cbCount Number of {@code char[]} buffers to allocate
     */
    protected BufferRecycler(int bbCount, int cbCount) {
        _byteBuffers = new Object[bbCount];
        _charBuffers = new Object[cbCount];
    }

    /**
     * @return True if this recycler is linked to pool and may be released
     *   with {@link #releaseToPool()}; false if no linkage exists.
     *
     * @since 2.17
     */
    public boolean isLinkedWithPool() {
        return _pool != null;
    }

    /*
    /**********************************************************
    /* Public API, byte buffers
    /**********************************************************
     */

    /**
     * @param ix One of <code>READ_IO_BUFFER</code> constants.
     *
     * @return Buffer allocated (possibly recycled)
     */
    public final byte[] allocByteBuffer(int ix) {
        return allocByteBuffer(ix, 0);
    }

    public byte[] allocByteBuffer(int ix, int minSize) {
        final int DEF_SIZE = byteBufferLength(ix);
        if (minSize < DEF_SIZE) {
            minSize = DEF_SIZE;
        }
        // getAndSetAcquire: atomically claims the slot and applies acquire semantics so
        // that all writes made by the thread that stored this buffer are visible to us.
        byte[] buffer = (byte[]) BUFFER_VH.getAndSetAcquire(_byteBuffers, ix, null);
        if (buffer == null || buffer.length < minSize) {
            buffer = balloc(minSize);
        }
        return buffer;
    }

    public void releaseByteBuffer(int ix, byte[] buffer) {
        // CAS loop fixes the TOCTOU race in the original get+set pattern.
        // compareAndSet with release semantics publishes all our writes to the next
        // thread that acquires this buffer.
        // Modified to do a max number of attempts to recycle (hardcoded to 3)
        for (int i = 0; i < 3; i++) {
            byte[] current = (byte[]) BUFFER_VH.getAcquire(_byteBuffers, ix);
            if (current != null && current.length >= buffer.length) {
                // Slot already holds a buffer at least as large; no benefit in replacing it.
                return;
            }
            if (BUFFER_VH.compareAndSet(_byteBuffers, ix, current, buffer)) {
                return;
            }
            // CAS lost the race; retry with the newly observed value.
        }
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
        // getAndSetAcquire: atomically claims the slot with acquire semantics.
        char[] buffer = (char[]) BUFFER_VH.getAndSetAcquire(_charBuffers, ix, null);
        if (buffer == null || buffer.length < minSize) {
            buffer = calloc(minSize);
        }
        return buffer;
    }

    public void releaseCharBuffer(int ix, char[] buffer) {
        // CAS loop fixes the TOCTOU race in the original get+set pattern.
        // compareAndSet with release semantics publishes our writes to the next thread.
        while (true) {
            char[] current = (char[]) BUFFER_VH.getAcquire(_charBuffers, ix);
            if (current != null && current.length >= buffer.length) {
                return;
            }
            if (BUFFER_VH.compareAndSet(_charBuffers, ix, current, buffer)) {
                return;
            }
        }
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

    /*
    /**********************************************************
    /* WithPool implementation
    /**********************************************************
     */

    /**
     * Method called by owner of this recycler instance, to provide reference to
     * {@link RecyclerPool} into which instance is to be released (if any)
     *
     * @since 2.16
     */
    @Override
    public BufferRecycler withPool(RecyclerPool<BufferRecycler> pool) {
        if (_pool != null) {
            throw new IllegalStateException("BufferRecycler already linked to pool: "+pool);
        }
        // assign to pool to which this BufferRecycler belongs in order to release it
        // to the same pool when the work will be completed
        _pool = Objects.requireNonNull(pool);
        return this;
    }

    /**
     * Method called when owner of this recycler no longer wishes use it; this should
     * return it to pool passed via {@code withPool()} (if any).
     *
     * @since 2.16
     */
    @Override
    public void releaseToPool() {
        if (_pool != null) {
            RecyclerPool<BufferRecycler> tmpPool = _pool;
            // nullify the reference to the pool in order to avoid the risk of releasing
            // the same BufferRecycler more than once, thus compromising the pool integrity
            _pool = null;
            tmpPool.releasePooled(this);
        }
    }
}
