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
    // 22-Jun-2026, [core#1622]: bumped default base64 codec buffer (index
    //   BYTE_BASE64_CODEC_BUFFER) from 2000 to 4000 to reduce InputStream
    //   reads when encoding binary content of unknown/large length.

    private final static int[] BYTE_BUFFER_LENGTHS = new int[] { 8000, 8000, 2000, 4000 };
    private final static int[] CHAR_BUFFER_LENGTHS = new int[] { 4000, 4000, 200, 200 };

    /**
     * Number of unused slots at each end of {@link #_slots}.
     * A pool keeps a recycler for a long time, and each use of the recycler
     * writes its slots. Without padding, these slots share cache lines with
     * other objects that other threads read, such as serializers and the
     * values that they write. Each write then takes those lines away from
     * the other cores (false sharing). 32 references are 128 bytes with
     * compressed oops (256 bytes without), the default padding of the JDK's
     * {@code @Contended}. This covers 128-byte cache lines (as on Apple
     * silicon) and the x86 adjacent-line prefetcher, which fetches 64-byte
     * lines in pairs.
     */
    private final static int SLOT_PADDING = 32;

    private final static int POOL_SLOT = SLOT_PADDING;

    private final static int FIRST_BYTE_SLOT = POOL_SLOT + 1;

    private final static VarHandle SLOTS = MethodHandles.arrayElementVarHandle(Object[].class);

    /**
     * Pool link, then the byte buffers, then the char buffers, with
     * {@link #SLOT_PADDING} unused slots before and after them.
     */
    private final Object[] _slots;

    private final int _byteBufferCount;

    private final int _charBufferCount;

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
        if (bbCount < 0 || cbCount < 0) {
            throw new IllegalArgumentException("Negative buffer count: bbCount="+bbCount+", cbCount="+cbCount);
        }
        _slots = new Object[FIRST_BYTE_SLOT + bbCount + cbCount + SLOT_PADDING];
        _byteBufferCount = bbCount;
        _charBufferCount = cbCount;
    }

    /**
     * @return True if this recycler is linked to pool and may be released
     *   with {@link #releaseToPool()}; false if no linkage exists.
     *
     * @since 2.17
     */
    public boolean isLinkedWithPool() {
        return _slots[POOL_SLOT] != null;
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
        byte[] buffer = (byte[]) SLOTS.getAndSet(_slots, _byteSlot(ix), null);
        if (buffer == null || buffer.length < minSize) {
            buffer = balloc(minSize);
        }
        return buffer;
    }

    public void releaseByteBuffer(int ix, byte[] buffer) {
        // 13-Jan-2024, tatu: [core#1186] Replace only if beneficial.
        // [core#1598] Update the slot atomically, avoiding the TOCTOU race
        // of a non-atomic get+conditional-set sequence.
        final int slot = _byteSlot(ix);
        Object current;
        do {
            current = SLOTS.getVolatile(_slots, slot);
            if (current != null && ((byte[]) current).length >= buffer.length) {
                return;
            }
        } while (!SLOTS.compareAndSet(_slots, slot, current, buffer));
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
        char[] buffer = (char[]) SLOTS.getAndSet(_slots, _charSlot(ix), null);
        if (buffer == null || buffer.length < minSize) {
            buffer = calloc(minSize);
        }
        return buffer;
    }

    public void releaseCharBuffer(int ix, char[] buffer) {
        // 13-Jan-2024, tatu: [core#1186] Replace only if beneficial.
        // [core#1598] Update the slot atomically, avoiding the TOCTOU race
        // of a non-atomic get+conditional-set sequence.
        final int slot = _charSlot(ix);
        Object current;
        do {
            current = SLOTS.getVolatile(_slots, slot);
            if (current != null && ((char[]) current).length >= buffer.length) {
                return;
            }
        } while (!SLOTS.compareAndSet(_slots, slot, current, buffer));
    }

    private int _byteSlot(int ix) {
        return FIRST_BYTE_SLOT + Objects.checkIndex(ix, _byteBufferCount);
    }

    private int _charSlot(int ix) {
        return FIRST_BYTE_SLOT + _byteBufferCount + Objects.checkIndex(ix, _charBufferCount);
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
        if (_slots[POOL_SLOT] != null) {
            throw new IllegalStateException("BufferRecycler already linked to pool: "+pool);
        }
        // assign to pool to which this BufferRecycler belongs in order to release it
        // to the same pool when the work will be completed
        _slots[POOL_SLOT] = Objects.requireNonNull(pool);
        return this;
    }

    /**
     * Method called when owner of this recycler no longer wishes use it; this should
     * return it to pool passed via {@code withPool()} (if any).
     *
     * @since 2.16
     */
    @Override
    @SuppressWarnings("unchecked")
    public void releaseToPool() {
        RecyclerPool<BufferRecycler> tmpPool = (RecyclerPool<BufferRecycler>) _slots[POOL_SLOT];
        if (tmpPool != null) {
            // nullify the reference to the pool in order to avoid the risk of releasing
            // the same BufferRecycler more than once, thus compromising the pool integrity
            _slots[POOL_SLOT] = null;
            tmpPool.releasePooled(this);
        }
    }
}
