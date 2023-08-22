package tools.jackson.core.util;

import java.lang.ref.SoftReference;

import tools.jackson.core.TokenStreamFactory;

/**
 * Helper entity used to control access to simple buffer recycling scheme used for
 * some encoding, decoding tasks.
 *
 * @see BufferRecycler
 */
public class BufferRecyclers
{
    /**
     * System property that is checked to see if recycled buffers (see {@link BufferRecycler})
     * should be tracked, for purpose of forcing release of all such buffers, typically
     * during major classloading.
     */
    public final static String SYSTEM_PROPERTY_TRACK_REUSABLE_BUFFERS
        = "tools.jackson.core.util.BufferRecyclers.trackReusableBuffers";

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Flag that indicates whether {@link BufferRecycler} instances should be tracked.
     */
    private final static ThreadLocalBufferManager _bufferRecyclerTracker;
    static {
        boolean trackReusableBuffers = false;
        try {
            trackReusableBuffers = "true".equals(System.getProperty(SYSTEM_PROPERTY_TRACK_REUSABLE_BUFFERS));
        } catch (SecurityException e) { }

        _bufferRecyclerTracker = trackReusableBuffers ? ThreadLocalBufferManager.instance() : null;
    }

    /*
    /**********************************************************
    /* BufferRecyclers for parsers, generators
    /**********************************************************
     */

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
            if (_bufferRecyclerTracker != null) {
                ref = _bufferRecyclerTracker.wrapAndTrack(br);
            } else {
                ref = new SoftReference<BufferRecycler>(br);
            }
            _recyclerRef.set(ref);
        }
        return br;
    }

    /**
     * Specialized method that will release all recycled {@link BufferRecycler} if
     * (and only if) recycler tracking has been enabled
     * (see {@link #SYSTEM_PROPERTY_TRACK_REUSABLE_BUFFERS}).
     * This method is usually called on shutdown of the container like Application Server
     * to ensure that no references are reachable via {@link ThreadLocal}s as this may cause
     * unintentional retention of sizable amounts of memory. It may also be called regularly
     * if GC for some reason does not clear up {@link SoftReference}s aggressively enough.
     *
     * @return Number of buffers released, if tracking enabled (zero or more); -1 if tracking not enabled.
     *
     * @since 2.9.6
     */
    public static int releaseBuffers() {
        if (_bufferRecyclerTracker != null) {
            return _bufferRecyclerTracker.releaseBuffers();
        }
        return -1;
    }

    /*
    /**********************************************************************
    /* Default BufferRecyclerPool implementations
    /**********************************************************************
     */

    public static BufferRecyclerPool defaultRecyclerPool() {
        return ThreadLocalRecyclerPool.INSTANCE;
    }

    public static BufferRecyclerPool nopRecyclerPool() {
        return NonRecyclingRecyclerPool.INSTANCE;
    }

    /**
     * Default {@link BufferRecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances.
     */
    public static class ThreadLocalRecyclerPool
        implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;
        public final static ThreadLocalRecyclerPool INSTANCE = new ThreadLocalRecyclerPool();

        @Override
        public BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory) {
            return getBufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that does not use
     * any pool but simply creates new instances when necessary.
     */
    public static class NonRecyclingRecyclerPool
        implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        public final static ThreadLocalRecyclerPool INSTANCE = new ThreadLocalRecyclerPool();

        @Override
        public BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory) {
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }
}
