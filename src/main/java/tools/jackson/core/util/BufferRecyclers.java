package tools.jackson.core.util;

import java.lang.ref.SoftReference;

/**
 * Helper entity used to control access to simple buffer recycling scheme used for
 * some encoding, decoding tasks.
 *
 * @see BufferRecycler
 */
public class BufferRecyclers
{
    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef
        = new ThreadLocal<SoftReference<BufferRecycler>>();

    /**
     * Main accessor to call for accessing possibly recycled {@link BufferRecycler} instance.
     *
     * @return {@link BufferRecycler} to use
     *
     * @deprecated Since 2.16 should use {@link BufferRecyclerPool} abstraction instead
     *   of calling static methods of this class
     */
    @Deprecated // since 2.16
    public static BufferRecycler getBufferRecycler()
    {
        SoftReference<BufferRecycler> ref = _recyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new BufferRecycler();
            ref = new SoftReference<BufferRecycler>(br);
            _recyclerRef.set(ref);
        }
        return br;
    }
}
