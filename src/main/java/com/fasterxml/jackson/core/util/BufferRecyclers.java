package com.fasterxml.jackson.core.util;

import java.lang.ref.SoftReference;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

/**
 * Helper entity used to further 
 *
 * @since 2.9.2
 */
public class BufferRecyclers
{
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

    public static BufferRecycler getBufferRecycler()
    {
        SoftReference<BufferRecycler> ref = _recyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new BufferRecycler();
            _recyclerRef.set(new SoftReference<BufferRecycler>(br));
        }
        return br;
    }

    /*
    /**********************************************************
    /* JsonStringEncoder
    /**********************************************************
     */

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<JsonStringEncoder>> _encoderRef
        = new ThreadLocal<SoftReference<JsonStringEncoder>>();

    public static JsonStringEncoder getJsonStringEncoder() {
        SoftReference<JsonStringEncoder> ref = _encoderRef.get();
        JsonStringEncoder enc = (ref == null) ? null : ref.get();

        if (enc == null) {
            enc = new JsonStringEncoder();
            _encoderRef.set(new SoftReference<JsonStringEncoder>(enc));
        }
        return enc;
    }
}
