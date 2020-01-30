package com.fasterxml.jackson.core.util;

import java.lang.ref.SoftReference;

/**
 * Helper entity used to control access to simple buffer recyling scheme used for
 * some encoding, decoding tasks.
 *
 * @see BufferRecycler
 *
 * @since 2.9.2
 */
public class BufferRecyclers
{
    /**
     * System property that is checked to see if recycled buffers (see {@link BufferRecycler})
     * should be tracked, for purpose of forcing release of all such buffers, typically
     * during major classloading.
     *
     * @since 2.9.6
     */
    public final static String SYSTEM_PROPERTY_TRACK_REUSABLE_BUFFERS
        = "com.fasterxml.jackson.core.util.BufferRecyclers.trackReusableBuffers";

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
     */
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
}
